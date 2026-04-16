// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/AudioRepository.ets
// Module: smartsales-app/entry
// Summary: 音频仓库管理层 — 迁移自 tingwu-container HarmonyContainerRepository.ets
// Author: created on 2026-04-16

import { common } from '@kit.AbilityKit';
import { ensureAppRuntimeConfigLoaded, getAppRuntimeConfigMissingKeys } from '../config/AppRuntimeConfig';
import {
  AudioItem,
  AudioJobStatus,
  AudioArtifacts,
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

class AudioRepository {
  private context?: common.UIAbilityContext;
  private items: AudioItem[] = [];
  private listeners: AudioSnapshotListener[] = [];
  private snapshot: AudioSnapshot = createEmptyAudioSnapshot();
  private activeJobs: Record<string, boolean> = {};

  async initialize(context: common.UIAbilityContext): Promise<void> {
    if (this.context?.filesDir === context.filesDir && !this.snapshot.isLoading) {
      return;
    }
    await ensureAppRuntimeConfigLoaded();
    this.context = context;
    this.items = fileStore.loadMetadata(context)
      .sort((left: AudioItem, right: AudioItem) => right.importedAt - left.importedAt);
    this.snapshot = {
      ...this.snapshot,
      items: [...this.items],
      configMissingKeys: getAppRuntimeConfigMissingKeys(),
      isLoading: false,
      isBusy: false,
      busyMessage: ''
    };
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
      throw error;
    }
  }

  async startTingwu(audioId: string): Promise<void> {
    await ensureAppRuntimeConfigLoaded();
    const context = this.requireContext();
    const item = this.findItem(audioId);
    if (!item || isProcessingStatus(item.status) || this.activeJobs[audioId]) {
      return;
    }
    this.activeJobs[audioId] = true;
    try {
      this.patchItem(audioId, {
        status: AudioJobStatus.UPLOADING,
        progressPercent: 8,
        lastErrorMessage: '',
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
        keywords: completed.artifacts.keywords,
        jobId: '',
        updatedAt: Date.now()
      });
    } catch (error) {
      const message = this.errorMessage(error);
      this.patchItem(audioId, {
        status: AudioJobStatus.FAILED,
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
    if (this.activeJobs[audioId]) {
      return;
    }
    this.activeJobs[audioId] = true;
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
        keywords: completed.artifacts.keywords,
        jobId: '',
        updatedAt: Date.now()
      });
    } catch (error) {
      this.patchItem(audioId, {
        status: AudioJobStatus.FAILED,
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
      throw new Error('AudioRepository context not initialized');
    }
    return this.context;
  }

  private findItem(audioId: string): AudioItem | undefined {
    return this.items.find((item: AudioItem) => item.id === audioId);
  }

  private patchItem(audioId: string, patch: Partial<AudioItem>): void {
    this.items = this.items.map((item: AudioItem) => item.id === audioId ? {
      ...item,
      ...patch
    } : item);
    this.persist();
  }

  private persist(): void {
    const context = this.requireContext();
    fileStore.saveMetadata(context, this.items);
    this.snapshot = {
      ...this.snapshot,
      items: [...this.items].sort((left: AudioItem, right: AudioItem) => right.importedAt - left.importedAt),
      configMissingKeys: getAppRuntimeConfigMissingKeys(),
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
      configMissingKeys: getAppRuntimeConfigMissingKeys()
    };
    this.emit();
  }

  private failGlobal(error: unknown): void {
    this.snapshot = {
      ...this.snapshot,
      isBusy: false,
      busyMessage: '',
      lastErrorMessage: this.errorMessage(error),
      configMissingKeys: getAppRuntimeConfigMissingKeys()
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
    this.listeners.forEach((listener: AudioSnapshotListener) => listener(this.snapshot));
  }
}

export const audioRepository = new AudioRepository();
