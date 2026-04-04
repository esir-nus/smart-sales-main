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
    this.snapshot = {
      ...this.snapshot,
      items: [...this.items],
      configMissingKeys: getHarmonyRuntimeConfigMissingKeys(),
      isLoading: false,
      isBusy: false,
      busyMessage: ''
    };
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
      this.items = [item, ...this.items];
      this.persist();
      this.setBusy(false, '');
      return item;
    } catch (error) {
      this.failGlobal(error);
      throw error;
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
      this.patchItem(audioId, {
        status: HarmonyJobStatus.UPLOADING,
        progressPercent: 8,
        lastErrorMessage: undefined,
        updatedAt: Date.now()
      });

      const objectKey = buildObjectKey(item.localFileName);
      const upload = await harmonyOssService.uploadAudio(
        item.localPath,
        objectKey,
        guessMimeType(item.extension)
      );

      this.patchItem(audioId, {
        status: HarmonyJobStatus.PROCESSING,
        progressPercent: 24,
        objectKey: upload.objectKey,
        publicUrl: upload.publicUrl,
        updatedAt: Date.now()
      });

      const submit = await harmonyTingwuService.submitTask(upload.publicUrl, item.localFileName);
      this.patchItem(audioId, {
        status: HarmonyJobStatus.PROCESSING,
        progressPercent: 30,
        jobId: submit.jobId,
        updatedAt: Date.now()
      });

      const completed = await harmonyTingwuService.observeUntilComplete(
        submit.jobId,
        (progress: number) => {
          const mappedProgress = Math.max(30, Math.min(95, progress));
          this.patchItem(audioId, {
            status: HarmonyJobStatus.PROCESSING,
            progressPercent: mappedProgress,
            updatedAt: Date.now()
          });
        }
      );

      harmonyFileStore.saveArtifacts(context, audioId, completed.artifacts);
      this.patchItem(audioId, {
        status: HarmonyJobStatus.COMPLETED,
        progressPercent: 100,
        summary: completed.summary,
        transcriptPreview: completed.transcriptPreview,
        completedAt: Date.now(),
        lastErrorMessage: undefined,
        keywords: completed.artifacts.keywords,
        jobId: undefined,
        updatedAt: Date.now()
      });
    } catch (error) {
      const message = this.errorMessage(error);
      this.patchItem(audioId, {
        status: HarmonyJobStatus.FAILED,
        progressPercent: 0,
        lastErrorMessage: message,
        updatedAt: Date.now()
      });
      this.snapshot = {
        ...this.snapshot,
        lastErrorMessage: message
      };
      this.emit();
      throw error;
    } finally {
      delete this.activeJobs[audioId];
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
        this.resumeObservation(item.id, item.jobId!).catch((error: Error) => {
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
          this.patchItem(audioId, {
            status: HarmonyJobStatus.PROCESSING,
            progressPercent: mappedProgress,
            updatedAt: Date.now()
          });
        }
      );
      harmonyFileStore.saveArtifacts(this.requireContext(), audioId, completed.artifacts);
      this.patchItem(audioId, {
        status: HarmonyJobStatus.COMPLETED,
        progressPercent: 100,
        summary: completed.summary,
        transcriptPreview: completed.transcriptPreview,
        completedAt: Date.now(),
        lastErrorMessage: undefined,
        keywords: completed.artifacts.keywords,
        jobId: undefined,
        updatedAt: Date.now()
      });
    } catch (error) {
      this.patchItem(audioId, {
        status: HarmonyJobStatus.FAILED,
        progressPercent: 0,
        lastErrorMessage: this.errorMessage(error),
        updatedAt: Date.now()
      });
    } finally {
      delete this.activeJobs[audioId];
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

  private patchItem(audioId: string, patch: Partial<HarmonyTingwuAudioItem>): void {
    this.items = this.items.map((item: HarmonyTingwuAudioItem) => item.id === audioId ? {
      ...item,
      ...patch
    } : item);
    this.persist();
  }

  private persist(): void {
    const context = this.requireContext();
    harmonyFileStore.saveMetadata(context, this.items);
    this.snapshot = {
      ...this.snapshot,
      items: [...this.items].sort((left: HarmonyTingwuAudioItem, right: HarmonyTingwuAudioItem) => right.importedAt - left.importedAt),
      configMissingKeys: getHarmonyRuntimeConfigMissingKeys(),
      isLoading: false
    };
    this.emit();
  }

  private setBusy(isBusy: boolean, busyMessage: string): void {
    this.snapshot = {
      ...this.snapshot,
      isBusy,
      busyMessage,
      lastErrorMessage: undefined,
      configMissingKeys: getHarmonyRuntimeConfigMissingKeys()
    };
    this.emit();
  }

  private failGlobal(error: unknown): void {
    this.snapshot = {
      ...this.snapshot,
      isBusy: false,
      busyMessage: '',
      lastErrorMessage: this.errorMessage(error),
      configMissingKeys: getHarmonyRuntimeConfigMissingKeys()
    };
    this.emit();
  }

  private errorMessage(error: unknown): string {
    if (error instanceof Error) {
      return error.message;
    }
    return `${error}`;
  }

  private emit(): void {
    this.listeners.forEach((listener: HarmonySnapshotListener) => listener(this.snapshot));
  }
}

export const harmonyContainerRepository = new HarmonyContainerRepository();
