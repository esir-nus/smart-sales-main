// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/AudioRepository.ets
// Module: smartsales-app/entry
// Summary: 音频仓库管理层 — 迁移自 tingwu-container HarmonyContainerRepository.ets
// Author: created on 2026-04-16

import { common } from '@kit.AbilityKit';
import { ensureAppRuntimeConfigLoaded, getAppRuntimeConfigMissingKeys } from '../config/AppRuntimeConfig';
import {
  AudioItem,
  AudioJobStatus,
  AudioErrorReason,
  AudioArtifacts,
  AudioPipelineError,
  AudioSnapshot,
  isProcessingStatus,
  createEmptyAudioSnapshot
} from '../domain/Audio';
import { fileStore } from './FileStore';
import { ossService } from './OssService';
import { pickerService } from './Picker';
import { tingwuService } from './TingwuService';
import {
  buildObjectKey,
  createAudioId,
  guessMimeType
} from '../utils/FormatUtils';

type AudioSnapshotListener = (snapshot: AudioSnapshot) => void;

interface AudioItemPatch {
  id?: string;
  displayName?: string;
  status?: AudioJobStatus;
  progressPercent?: number;
  summary?: string;
  transcriptPreview?: string;
  lastErrorMessage?: string;
  lastErrorReason?: AudioErrorReason;
  clearLastErrorReason?: boolean;
  keywords?: string[];
  sourceUri?: string;
  localFileName?: string;
  localPath?: string;
  extension?: string;
  sizeBytes?: number;
  importedAt?: number;
  updatedAt?: number;
  completedAt?: number;
  objectKey?: string;
  publicUrl?: string;
  jobId?: string;
}

class AudioRepository {
  private context?: common.UIAbilityContext;
  private items: AudioItem[] = [];
  private listeners: AudioSnapshotListener[] = [];
  private snapshot: AudioSnapshot = createEmptyAudioSnapshot();
  private activeJobs: Map<string, boolean> = new Map();

  async initialize(context: common.UIAbilityContext): Promise<void> {
    if (this.context?.filesDir === context.filesDir && !this.snapshot.isLoading) {
      return;
    }
    await ensureAppRuntimeConfigLoaded();
    this.context = context;
    this.items = fileStore.loadMetadata(context)
      .sort((left: AudioItem, right: AudioItem) => right.importedAt - left.importedAt);
    this.snapshot = this.buildSnapshot(
      this.cloneItems(this.items),
      getAppRuntimeConfigMissingKeys(),
      false,
      false,
      '',
      this.snapshot.lastErrorMessage
    );
    this.emit();
    this.resumeProcessingJobs();
  }

  getSnapshot(): AudioSnapshot {
    return this.snapshot;
  }

  subscribe(listener: AudioSnapshotListener): () => void {
    this.listeners.push(listener);
    listener(this.snapshot);
    return () => {
      this.listeners = this.listeners.filter((candidate: AudioSnapshotListener) => candidate !== listener);
    };
  }

  async importAudio(): Promise<AudioItem | undefined> {
    const context = this.requireContext();
    this.setBusy(true, 'Selecting audio file...');
    try {
      const selectedUri = await pickerService.selectSingleAudioUri();
      if (!selectedUri) {
        this.setBusy(false, '');
        return undefined;
      }
      const audioId = createAudioId();
      const imported = fileStore.importFromUri(context, selectedUri, audioId);
      const item: AudioItem = {
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
        status: AudioJobStatus.IMPORTED,
        summary: '',
        transcriptPreview: '',
        lastErrorMessage: '',
        lastErrorReason: undefined,
        keywords: [],
        completedAt: 0,
        objectKey: '',
        publicUrl: '',
        jobId: ''
      };
      this.items = [item, ...this.items];
      this.persist();
      this.setBusy(false, '');
      return item;
    } catch (error) {
      this.failGlobal(error);
      throw this.asError(error);
    }
  }

  async startTingwu(audioId: string): Promise<void> {
    await ensureAppRuntimeConfigLoaded();
    const context = this.requireContext();
    const item = this.findItem(audioId);
    if (!item) {
      console.warn(`[HS-010][AudioRepository] start skipped missing-item audioId=${audioId}`);
      return;
    }
    if (isProcessingStatus(item.status)) {
      console.info(`[HS-010][AudioRepository] start skipped processing-status audioId=${audioId} status=${item.status}`);
      return;
    }
    if (this.activeJobs.get(audioId) === true) {
      console.info(`[HS-010][AudioRepository] start skipped active-job audioId=${audioId}`);
      return;
    }
    this.activeJobs.set(audioId, true);
    try {
      this.patchItem(audioId, {
        status: AudioJobStatus.UPLOADING,
        progressPercent: 8,
        lastErrorMessage: '',
        clearLastErrorReason: true,
        updatedAt: Date.now()
      });

      const objectKey = buildObjectKey(item.localFileName);
      const upload = await ossService.uploadAudio(
        item.localPath,
        objectKey,
        guessMimeType(item.extension)
      );

      this.patchItem(audioId, {
        status: AudioJobStatus.PROCESSING,
        progressPercent: 24,
        objectKey: upload.objectKey,
        publicUrl: upload.publicUrl,
        updatedAt: Date.now()
      });

      const submit = await tingwuService.submitTask(upload.publicUrl, item.localFileName);
      this.patchItem(audioId, {
        status: AudioJobStatus.PROCESSING,
        progressPercent: 30,
        jobId: submit.jobId,
        updatedAt: Date.now()
      });

      const completed = await tingwuService.observeUntilComplete(
        submit.jobId,
        (progress: number) => {
          const mappedProgress = Math.max(30, Math.min(95, progress));
          this.patchItem(audioId, {
            status: AudioJobStatus.PROCESSING,
            progressPercent: mappedProgress,
            updatedAt: Date.now()
          });
        }
      );

      fileStore.saveArtifacts(context, audioId, completed.artifacts);
      this.patchItem(audioId, {
        status: AudioJobStatus.COMPLETED,
        progressPercent: 100,
        summary: completed.summary,
        transcriptPreview: completed.transcriptPreview,
        completedAt: Date.now(),
        lastErrorMessage: '',
        clearLastErrorReason: true,
        keywords: completed.artifacts.keywords,
        jobId: '',
        updatedAt: Date.now()
      });
    } catch (error) {
      const message = this.errorMessage(error);
      const reason = this.errorReason(error);
      this.patchItem(audioId, {
        status: AudioJobStatus.FAILED,
        progressPercent: 0,
        lastErrorMessage: message,
        lastErrorReason: reason,
        updatedAt: Date.now()
      });
      this.snapshot = this.buildSnapshot(
        this.snapshot.items,
        this.snapshot.configMissingKeys,
        this.snapshot.isLoading,
        this.snapshot.isBusy,
        this.snapshot.busyMessage,
        message
      );
      this.emit();
      throw this.asError(error);
    } finally {
      this.activeJobs.delete(audioId);
    }
  }

  async loadArtifacts(audioId: string): Promise<AudioArtifacts | undefined> {
    const context = this.requireContext();
    return fileStore.loadArtifacts(context, audioId);
  }

  private resumeProcessingJobs(): void {
    this.items
      .filter((item: AudioItem) => item.status === AudioJobStatus.PROCESSING && !!item.jobId)
      .forEach((item: AudioItem) => {
        this.resumeObservation(item.id, item.jobId).catch((error: Error) => {
          console.error(`Failed to resume job ${item.id}: ${error.message}`);
        });
      });
  }

  private async resumeObservation(audioId: string, jobId: string): Promise<void> {
    if (this.activeJobs.get(audioId) === true) {
      return;
    }
    this.activeJobs.set(audioId, true);
    try {
      const completed = await tingwuService.observeUntilComplete(
        jobId,
        (progress: number) => {
          const mappedProgress = Math.max(30, Math.min(95, progress));
          this.patchItem(audioId, {
            status: AudioJobStatus.PROCESSING,
            progressPercent: mappedProgress,
            updatedAt: Date.now()
          });
        }
      );
      fileStore.saveArtifacts(this.requireContext(), audioId, completed.artifacts);
      this.patchItem(audioId, {
        status: AudioJobStatus.COMPLETED,
        progressPercent: 100,
        summary: completed.summary,
        transcriptPreview: completed.transcriptPreview,
        completedAt: Date.now(),
        lastErrorMessage: '',
        clearLastErrorReason: true,
        keywords: completed.artifacts.keywords,
        jobId: '',
        updatedAt: Date.now()
      });
    } catch (error) {
      this.patchItem(audioId, {
        status: AudioJobStatus.FAILED,
        progressPercent: 0,
        lastErrorMessage: this.errorMessage(error),
        lastErrorReason: this.errorReason(error),
        updatedAt: Date.now()
      });
    } finally {
      this.activeJobs.delete(audioId);
    }
  }

  private requireContext(): common.UIAbilityContext {
    if (!this.context) {
      throw new Error('AudioRepository context not initialized');
    }
    return this.context;
  }

  private findItem(audioId: string): AudioItem | undefined {
    return this.items.find((item: AudioItem) => item.id === audioId);
  }

  private patchItem(audioId: string, patch: AudioItemPatch): void {
    this.items = this.items.map((item: AudioItem) => {
      if (item.id !== audioId) {
        return item;
      }
      return this.mergeItem(item, patch);
    });
    this.persist();
  }

  private persist(): void {
    const context = this.requireContext();
    fileStore.saveMetadata(context, this.items);
    this.snapshot = this.buildSnapshot(
      this.sortedItems(),
      getAppRuntimeConfigMissingKeys(),
      false,
      this.snapshot.isBusy,
      this.snapshot.busyMessage,
      this.snapshot.lastErrorMessage
    );
    this.emit();
  }

  private setBusy(isBusy: boolean, busyMessage: string): void {
    this.snapshot = this.buildSnapshot(
      this.snapshot.items,
      getAppRuntimeConfigMissingKeys(),
      this.snapshot.isLoading,
      isBusy,
      busyMessage,
      undefined
    );
    this.emit();
  }

  private failGlobal(error: Object | null | undefined): void {
    this.snapshot = this.buildSnapshot(
      this.snapshot.items,
      getAppRuntimeConfigMissingKeys(),
      this.snapshot.isLoading,
      false,
      '',
      this.errorMessage(error)
    );
    this.emit();
  }

  private errorMessage(error: Object | null | undefined): string {
    if (error instanceof Error) {
      return error.message;
    }
    return `${error}`;
  }

  private errorReason(error: Object | null | undefined): AudioErrorReason {
    if (error instanceof AudioPipelineError) {
      return error.reason;
    }
    return AudioErrorReason.UNKNOWN;
  }

  private asError(error: Object | null | undefined): Error {
    if (error instanceof Error) {
      return error;
    }
    return new Error(`${error}`);
  }

  private sortedItems(): AudioItem[] {
    const items = this.cloneItems(this.items);
    items.sort((left: AudioItem, right: AudioItem) => right.importedAt - left.importedAt);
    return items;
  }

  private cloneItems(items: AudioItem[]): AudioItem[] {
    return items.map((item: AudioItem) => this.mergeItem(item, {}));
  }

  private buildSnapshot(
    items: AudioItem[],
    configMissingKeys: string[],
    isLoading: boolean,
    isBusy: boolean,
    busyMessage: string,
    lastErrorMessage?: string
  ): AudioSnapshot {
    return {
      items,
      configMissingKeys,
      isLoading,
      isBusy,
      busyMessage,
      lastErrorMessage
    };
  }

  private mergeItem(base: AudioItem, patch: AudioItemPatch): AudioItem {
    return {
      id: patch.id !== undefined ? patch.id : base.id,
      displayName: patch.displayName !== undefined ? patch.displayName : base.displayName,
      status: patch.status !== undefined ? patch.status : base.status,
      progressPercent: patch.progressPercent !== undefined ? patch.progressPercent : base.progressPercent,
      summary: patch.summary !== undefined ? patch.summary : base.summary,
      transcriptPreview: patch.transcriptPreview !== undefined ? patch.transcriptPreview : base.transcriptPreview,
      lastErrorMessage: patch.lastErrorMessage !== undefined ? patch.lastErrorMessage : base.lastErrorMessage,
      lastErrorReason: patch.lastErrorReason !== undefined
        ? patch.lastErrorReason
        : (patch.clearLastErrorReason === true ? undefined : base.lastErrorReason),
      keywords: patch.keywords !== undefined ? patch.keywords : base.keywords,
      sourceUri: patch.sourceUri !== undefined ? patch.sourceUri : base.sourceUri,
      localFileName: patch.localFileName !== undefined ? patch.localFileName : base.localFileName,
      localPath: patch.localPath !== undefined ? patch.localPath : base.localPath,
      extension: patch.extension !== undefined ? patch.extension : base.extension,
      sizeBytes: patch.sizeBytes !== undefined ? patch.sizeBytes : base.sizeBytes,
      importedAt: patch.importedAt !== undefined ? patch.importedAt : base.importedAt,
      updatedAt: patch.updatedAt !== undefined ? patch.updatedAt : base.updatedAt,
      completedAt: patch.completedAt !== undefined ? patch.completedAt : base.completedAt,
      objectKey: patch.objectKey !== undefined ? patch.objectKey : base.objectKey,
      publicUrl: patch.publicUrl !== undefined ? patch.publicUrl : base.publicUrl,
      jobId: patch.jobId !== undefined ? patch.jobId : base.jobId
    };
  }

  private emit(): void {
    this.listeners.forEach((listener: AudioSnapshotListener) => listener(this.snapshot));
  }
}

export const audioRepository = new AudioRepository();
