import { common } from '@kit.AbilityKit';
import {
  ensureHarmonyRuntimeConfigLoaded,
  getHarmonyRuntimeConfigMissingKeys
} from '../config/HarmonyContainerRuntimeConfig';
import {
  createEmptySnapshot,
  HarmonyContainerSnapshot,
  HarmonyJobStatus,
  HarmonyTingwuArtifacts,
  HarmonyTingwuAudioItem,
  isProcessingStatus
} from '../model/HarmonyTingwuModels';
import { harmonyFileStore } from './HarmonyFileStore';
import { harmonyOssService } from './HarmonyOssService';
import { harmonyPickerService } from './HarmonyPickerService';
import { harmonyTingwuService } from './HarmonyTingwuService';
import {
  buildObjectKey,
  createHarmonyAudioId,
  guessMimeType
} from '../utils/HarmonyFormatUtils';

type HarmonySnapshotListener = (snapshot: HarmonyContainerSnapshot) => void;
type HarmonyContainerError = Error | Object | string | number | boolean | undefined | null;

class HarmonyContainerRepository {
  private context?: common.UIAbilityContext;
  private items: HarmonyTingwuAudioItem[] = [];
  private listeners: HarmonySnapshotListener[] = [];
  private snapshot: HarmonyContainerSnapshot = createEmptySnapshot();
  private activeJobs: Record<string, boolean> = {};

  async initialize(context: common.UIAbilityContext): Promise<void> {
    if (this.context?.filesDir === context.filesDir && !this.snapshot.isLoading) {
      return;
    }
    await ensureHarmonyRuntimeConfigLoaded();
    this.context = context;
    this.items = harmonyFileStore.loadMetadata(context)
      .sort((left: HarmonyTingwuAudioItem, right: HarmonyTingwuAudioItem) => right.importedAt - left.importedAt);
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.items = this.sortedItems();
    nextSnapshot.configMissingKeys = this.copyStringArray(getHarmonyRuntimeConfigMissingKeys());
    nextSnapshot.isLoading = false;
    nextSnapshot.isBusy = false;
    nextSnapshot.busyMessage = '';
    this.snapshot = nextSnapshot;
    this.emit();
    this.resumeProcessingJobs();
  }

  getSnapshot(): HarmonyContainerSnapshot {
    return this.snapshot;
  }

  subscribe(listener: HarmonySnapshotListener): () => void {
    this.listeners.push(listener);
    listener(this.snapshot);
    return () => {
      this.listeners = this.listeners.filter((candidate: HarmonySnapshotListener) => candidate !== listener);
    };
  }

  async importAudio(): Promise<HarmonyTingwuAudioItem | undefined> {
    const context = this.requireContext();
    this.setBusy(true, 'Selecting Harmony local audio...');
    try {
      const selectedUri = await harmonyPickerService.selectSingleAudioUri();
      if (!selectedUri) {
        this.setBusy(false, '');
        return undefined;
      }
      const audioId = createHarmonyAudioId();
      const imported = harmonyFileStore.importFromUri(context, selectedUri, audioId);
      const item: HarmonyTingwuAudioItem = {
        id: audioId,
        displayName: imported.displayName,
        sourceUri: selectedUri,
        localFileName: imported.localFileName,
        localPath: imported.localPath,
        extension: imported.extension,
        sizeBytes: imported.sizeBytes,
        importedAt: Date.now(),
        updatedAt: Date.now(),
        progressPercent: 0,
        status: HarmonyJobStatus.IMPORTED,
        keywords: []
      };
      this.items = this.prependItem(item);
      this.persist();
      this.setBusy(false, '');
      return item;
    } catch (error) {
      const message = this.errorMessage(error);
      this.failGlobal(error);
      throw new Error(message);
    }
  }

  async startTingwu(audioId: string): Promise<void> {
    await ensureHarmonyRuntimeConfigLoaded();
    const context = this.requireContext();
    const item = this.findItem(audioId);
    if (!item || isProcessingStatus(item.status) || this.activeJobs[audioId]) {
      return;
    }
    this.activeJobs[audioId] = true;
    try {
      this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
        const nextItem = this.copyItem(itemToUpdate);
        nextItem.status = HarmonyJobStatus.UPLOADING;
        nextItem.progressPercent = 8;
        nextItem.lastErrorMessage = undefined;
        nextItem.updatedAt = Date.now();
        return nextItem;
      });

      const objectKey = buildObjectKey(item.localFileName);
      const upload = await harmonyOssService.uploadAudio(
        item.localPath,
        objectKey,
        guessMimeType(item.extension)
      );

      this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
        const nextItem = this.copyItem(itemToUpdate);
        nextItem.status = HarmonyJobStatus.PROCESSING;
        nextItem.progressPercent = 24;
        nextItem.objectKey = upload.objectKey;
        nextItem.publicUrl = upload.publicUrl;
        nextItem.updatedAt = Date.now();
        return nextItem;
      });

      const submit = await harmonyTingwuService.submitTask(upload.publicUrl, item.localFileName);
      this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
        const nextItem = this.copyItem(itemToUpdate);
        nextItem.status = HarmonyJobStatus.PROCESSING;
        nextItem.progressPercent = 30;
        nextItem.jobId = submit.jobId;
        nextItem.updatedAt = Date.now();
        return nextItem;
      });

      const completed = await harmonyTingwuService.observeUntilComplete(
        submit.jobId,
        (progress: number) => {
          const mappedProgress = Math.max(30, Math.min(95, progress));
          this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
            const nextItem = this.copyItem(itemToUpdate);
            nextItem.status = HarmonyJobStatus.PROCESSING;
            nextItem.progressPercent = mappedProgress;
            nextItem.updatedAt = Date.now();
            return nextItem;
          });
        }
      );

      harmonyFileStore.saveArtifacts(context, audioId, completed.artifacts);
      this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
        const nextItem = this.copyItem(itemToUpdate);
        nextItem.status = HarmonyJobStatus.COMPLETED;
        nextItem.progressPercent = 100;
        nextItem.summary = completed.summary;
        nextItem.transcriptPreview = completed.transcriptPreview;
        nextItem.completedAt = Date.now();
        nextItem.lastErrorMessage = undefined;
        nextItem.keywords = this.copyStringArray(completed.artifacts.keywords);
        nextItem.jobId = undefined;
        nextItem.updatedAt = Date.now();
        return nextItem;
      });
    } catch (error) {
      const message = this.errorMessage(error);
      this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
        const nextItem = this.copyItem(itemToUpdate);
        nextItem.status = HarmonyJobStatus.FAILED;
        nextItem.progressPercent = 0;
        nextItem.lastErrorMessage = message;
        nextItem.updatedAt = Date.now();
        return nextItem;
      });
      const nextSnapshot = this.copySnapshot(this.snapshot);
      nextSnapshot.lastErrorMessage = message;
      this.snapshot = nextSnapshot;
      this.emit();
      throw new Error(message);
    } finally {
      this.activeJobs[audioId] = false;
    }
  }

  async loadArtifacts(audioId: string): Promise<HarmonyTingwuArtifacts | undefined> {
    const context = this.requireContext();
    return harmonyFileStore.loadArtifacts(context, audioId);
  }

  private resumeProcessingJobs(): void {
    this.items
      .filter((item: HarmonyTingwuAudioItem) => item.status === HarmonyJobStatus.PROCESSING && !!item.jobId)
      .forEach((item: HarmonyTingwuAudioItem) => {
        if (!item.jobId) {
          return;
        }
        this.resumeObservation(item.id, item.jobId).catch((error: Error) => {
          console.error(`Failed to resume harmony job ${item.id}: ${error.message}`);
        });
      });
  }

  private async resumeObservation(audioId: string, jobId: string): Promise<void> {
    if (this.activeJobs[audioId]) {
      return;
    }
    this.activeJobs[audioId] = true;
    try {
      const completed = await harmonyTingwuService.observeUntilComplete(
        jobId,
        (progress: number) => {
          const mappedProgress = Math.max(30, Math.min(95, progress));
          this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
            const nextItem = this.copyItem(itemToUpdate);
            nextItem.status = HarmonyJobStatus.PROCESSING;
            nextItem.progressPercent = mappedProgress;
            nextItem.updatedAt = Date.now();
            return nextItem;
          });
        }
      );
      harmonyFileStore.saveArtifacts(this.requireContext(), audioId, completed.artifacts);
      this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
        const nextItem = this.copyItem(itemToUpdate);
        nextItem.status = HarmonyJobStatus.COMPLETED;
        nextItem.progressPercent = 100;
        nextItem.summary = completed.summary;
        nextItem.transcriptPreview = completed.transcriptPreview;
        nextItem.completedAt = Date.now();
        nextItem.lastErrorMessage = undefined;
        nextItem.keywords = this.copyStringArray(completed.artifacts.keywords);
        nextItem.jobId = undefined;
        nextItem.updatedAt = Date.now();
        return nextItem;
      });
    } catch (error) {
      const message = this.errorMessage(error);
      this.updateItem(audioId, (itemToUpdate: HarmonyTingwuAudioItem) => {
        const nextItem = this.copyItem(itemToUpdate);
        nextItem.status = HarmonyJobStatus.FAILED;
        nextItem.progressPercent = 0;
        nextItem.lastErrorMessage = message;
        nextItem.updatedAt = Date.now();
        return nextItem;
      });
    } finally {
      this.activeJobs[audioId] = false;
    }
  }

  private requireContext(): common.UIAbilityContext {
    if (!this.context) {
      throw new Error('Harmony container context not initialized');
    }
    return this.context;
  }

  private findItem(audioId: string): HarmonyTingwuAudioItem | undefined {
    return this.items.find((item: HarmonyTingwuAudioItem) => item.id === audioId);
  }

  private updateItem(
    audioId: string,
    updater: (item: HarmonyTingwuAudioItem) => HarmonyTingwuAudioItem
  ): void {
    const nextItems: HarmonyTingwuAudioItem[] = [];
    this.items.forEach((item: HarmonyTingwuAudioItem) => {
      if (item.id === audioId) {
        nextItems.push(updater(item));
      } else {
        nextItems.push(this.copyItem(item));
      }
    });
    this.items = nextItems;
    this.persist();
  }

  private persist(): void {
    const context = this.requireContext();
    harmonyFileStore.saveMetadata(context, this.items);
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.items = this.sortedItems();
    nextSnapshot.configMissingKeys = this.copyStringArray(getHarmonyRuntimeConfigMissingKeys());
    nextSnapshot.isLoading = false;
    this.snapshot = nextSnapshot;
    this.emit();
  }

  private setBusy(isBusy: boolean, busyMessage: string): void {
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.isBusy = isBusy;
    nextSnapshot.busyMessage = busyMessage;
    nextSnapshot.lastErrorMessage = undefined;
    nextSnapshot.configMissingKeys = this.copyStringArray(getHarmonyRuntimeConfigMissingKeys());
    this.snapshot = nextSnapshot;
    this.emit();
  }

  private failGlobal(error: HarmonyContainerError): void {
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.isBusy = false;
    nextSnapshot.busyMessage = '';
    nextSnapshot.lastErrorMessage = this.errorMessage(error);
    nextSnapshot.configMissingKeys = this.copyStringArray(getHarmonyRuntimeConfigMissingKeys());
    this.snapshot = nextSnapshot;
    this.emit();
  }

  private errorMessage(error: HarmonyContainerError): string {
    if (error instanceof Error) {
      return error.message;
    }
    return `${error}`;
  }

  private emit(): void {
    this.listeners.forEach((listener: HarmonySnapshotListener) => listener(this.snapshot));
  }

  private prependItem(item: HarmonyTingwuAudioItem): HarmonyTingwuAudioItem[] {
    const nextItems: HarmonyTingwuAudioItem[] = [this.copyItem(item)];
    this.items.forEach((candidate: HarmonyTingwuAudioItem) => {
      nextItems.push(this.copyItem(candidate));
    });
    return nextItems;
  }

  private sortedItems(): HarmonyTingwuAudioItem[] {
    const nextItems = this.copyItems(this.items);
    nextItems.sort((left: HarmonyTingwuAudioItem, right: HarmonyTingwuAudioItem) => right.importedAt - left.importedAt);
    return nextItems;
  }

  private copyItems(items: HarmonyTingwuAudioItem[]): HarmonyTingwuAudioItem[] {
    const copies: HarmonyTingwuAudioItem[] = [];
    items.forEach((item: HarmonyTingwuAudioItem) => {
      copies.push(this.copyItem(item));
    });
    return copies;
  }

  private copyItem(item: HarmonyTingwuAudioItem): HarmonyTingwuAudioItem {
    return {
      id: item.id,
      displayName: item.displayName,
      sourceUri: item.sourceUri,
      localFileName: item.localFileName,
      localPath: item.localPath,
      extension: item.extension,
      sizeBytes: item.sizeBytes,
      importedAt: item.importedAt,
      updatedAt: item.updatedAt,
      progressPercent: item.progressPercent,
      status: item.status,
      objectKey: item.objectKey,
      publicUrl: item.publicUrl,
      jobId: item.jobId,
      summary: item.summary,
      transcriptPreview: item.transcriptPreview,
      lastErrorMessage: item.lastErrorMessage,
      completedAt: item.completedAt,
      keywords: this.copyStringArray(item.keywords)
    };
  }

  private copySnapshot(snapshot: HarmonyContainerSnapshot): HarmonyContainerSnapshot {
    return {
      items: this.copyItems(snapshot.items),
      configMissingKeys: this.copyStringArray(snapshot.configMissingKeys),
      isLoading: snapshot.isLoading,
      isBusy: snapshot.isBusy,
      busyMessage: snapshot.busyMessage,
      lastErrorMessage: snapshot.lastErrorMessage
    };
  }

  private copyStringArray(values: string[]): string[] {
    const nextValues: string[] = [];
    values.forEach((value: string) => {
      nextValues.push(value);
    });
    return nextValues;
  }
}

export const harmonyContainerRepository = new HarmonyContainerRepository();
