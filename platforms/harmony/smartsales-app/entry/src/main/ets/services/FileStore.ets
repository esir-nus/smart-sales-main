// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/FileStore.ets
// Module: smartsales-app/entry
// Summary: 本地文件存储服务 — 音频与日程的 Harmony 本地持久化
// Author: updated on 2026-04-21

import { common } from '@kit.AbilityKit';
import { fileIo as fs } from '@kit.CoreFileKit';
import { AudioItem, AudioArtifacts } from '../domain/Audio';
import {
  ScheduledTask,
  UrgencyLevel,
  DurationSource,
  ConflictPolicy,
  buildCascade,
  parseConflictPolicy,
  parseDurationSource,
  parseUrgencyLevel
} from '../domain/Scheduler';
import { guessExtension } from '../utils/FormatUtils';

const METADATA_FILE_NAME = 'smartsales_audio_metadata.json';
const TASK_FILE_NAME = 'smartsales_scheduler_tasks.json';

export interface ImportedAudioRecord {
  displayName: string;
  localFileName: string;
  localPath: string;
  extension: string;
  sizeBytes: number;
}

class FileStore {
  metadataPath(context: common.UIAbilityContext): string {
    return `${context.filesDir}/${METADATA_FILE_NAME}`;
  }

  taskPath(context: common.UIAbilityContext): string {
    return `${context.filesDir}/${TASK_FILE_NAME}`;
  }

  audioFileName(audioId: string, extension: string): string {
    return `audio_${audioId}.${extension}`;
  }

  audioPath(context: common.UIAbilityContext, audioId: string, extension: string): string {
    return `${context.filesDir}/${this.audioFileName(audioId, extension)}`;
  }

  artifactPath(context: common.UIAbilityContext, audioId: string): string {
    return `${context.filesDir}/smartsales_${audioId}_artifacts.json`;
  }

  loadMetadata(context: common.UIAbilityContext): AudioItem[] {
    const raw = this.readTextIfExists(this.metadataPath(context));
    if (!raw) {
      return [];
    }
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed as AudioItem[] : [];
    } catch (error) {
      console.error(`Failed to parse metadata: ${JSON.stringify(error)}`);
      return [];
    }
  }

  saveMetadata(context: common.UIAbilityContext, items: AudioItem[]): void {
    this.writeText(this.metadataPath(context), JSON.stringify(items, null, 2));
  }

  loadArtifacts(context: common.UIAbilityContext, audioId: string): AudioArtifacts | undefined {
    const raw = this.readTextIfExists(this.artifactPath(context, audioId));
    if (!raw) {
      return undefined;
    }
    try {
      return JSON.parse(raw) as AudioArtifacts;
    } catch (error) {
      console.error(`Failed to parse artifacts: ${JSON.stringify(error)}`);
      return undefined;
    }
  }

  saveArtifacts(context: common.UIAbilityContext, audioId: string, artifacts: AudioArtifacts): void {
    this.writeText(this.artifactPath(context, audioId), JSON.stringify(artifacts, null, 2));
  }

  loadTasks(context: common.UIAbilityContext): ScheduledTask[] {
    const raw = this.readTextIfExists(this.taskPath(context));
    if (!raw) {
      console.info('[HS-006][FileStore] loadTasks no-file');
      return [];
    }
    try {
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        console.warn('[HS-006][FileStore] loadTasks invalid-root');
        return [];
      }
      const tasks: ScheduledTask[] = [];
      parsed.forEach((entry: Object) => {
        const task = this.normalizeTask(entry);
        if (task) {
          tasks.push(task);
        }
      });
      console.info(`[HS-006][FileStore] loadTasks success count=${tasks.length}`);
      return tasks;
    } catch (error) {
      console.error(`[HS-006][FileStore] loadTasks parse-failed error=${JSON.stringify(error)}`);
      return [];
    }
  }

  saveTasks(context: common.UIAbilityContext, tasks: ScheduledTask[]): void {
    this.writeText(this.taskPath(context), JSON.stringify(tasks, null, 2));
    console.info(`[HS-006][FileStore] saveTasks success count=${tasks.length}`);
  }

  importFromUri(
    context: common.UIAbilityContext,
    sourceUri: string,
    audioId: string
  ): ImportedAudioRecord {
    const source = fs.openSync(sourceUri, fs.OpenMode.READ_ONLY);
    const sourceName = source.name || sourceUri.split('/').pop() || `imported_${audioId}.m4a`;
    const extension = guessExtension(sourceName);
    const localFileName = this.audioFileName(audioId, extension);
    const localPath = this.audioPath(context, audioId, extension);
    const target = fs.openSync(localPath, fs.OpenMode.CREATE | fs.OpenMode.READ_WRITE | fs.OpenMode.TRUNC);
    let totalBytes = 0;
    try {
      let chunk = new ArrayBuffer(64 * 1024);
      let bytesRead = fs.readSync(source.fd, chunk);
      while (bytesRead > 0) {
        const payload = bytesRead === chunk.byteLength ? chunk : chunk.slice(0, bytesRead);
        fs.writeSync(target.fd, payload);
        totalBytes += bytesRead;
        chunk = new ArrayBuffer(64 * 1024);
        bytesRead = fs.readSync(source.fd, chunk);
      }
    } finally {
      fs.closeSync(source);
      fs.closeSync(target);
    }
    return {
      displayName: sourceName,
      localFileName,
      localPath,
      extension,
      sizeBytes: totalBytes
    };
  }

  readBinary(path: string): ArrayBuffer {
    const file = fs.openSync(path, fs.OpenMode.READ_ONLY);
    try {
      const stat = fs.statSync(path);
      const buffer = new ArrayBuffer(Number(stat.size));
      const bytesRead = fs.readSync(file.fd, buffer);
      return bytesRead === buffer.byteLength ? buffer : buffer.slice(0, bytesRead);
    } finally {
      fs.closeSync(file);
    }
  }

  private normalizeTask(value: Object): ScheduledTask | undefined {
    const record = this.safeObject(value);
    if (!record) {
      return undefined;
    }
    const id = this.cleanString(record['id']);
    const title = this.cleanString(record['title']);
    if (!id || !title) {
      return undefined;
    }
    const startTime = this.readMillis(record['startTime'], record['scheduledAt'], record['createdAt']);
    if (startTime <= 0) {
      return undefined;
    }
    const urgencyLevel = parseUrgencyLevel(record['urgencyLevel']);
    const endTime = this.readOptionalMillis(record['endTime']);
    const createdAt = this.readMillis(record['createdAt'], record['updatedAt'], record['startTime']);
    const updatedAt = this.readMillis(record['updatedAt'], record['createdAt'], record['startTime']);
    return {
      id,
      timeDisplay: this.cleanString(record['timeDisplay']) || this.formatTimeDisplay(startTime),
      title,
      urgencyLevel,
      isDone: this.readBoolean(record['isDone']) || this.readBoolean(record['isCompleted']),
      hasAlarm: this.readBoolean(record['hasAlarm'], true),
      isSmartAlarm: this.readBoolean(record['isSmartAlarm']),
      startTime,
      endTime,
      durationMinutes: this.readInteger(record['durationMinutes']),
      durationSource: parseDurationSource(record['durationSource']),
      conflictPolicy: parseConflictPolicy(record['conflictPolicy']),
      dateRange: this.cleanString(record['dateRange']) || this.formatDateRange(startTime, endTime),
      notes: this.cleanString(record['notes']) || this.cleanString(record['description']),
      alarmCascade: this.readAlarmCascade(record['alarmCascade'], urgencyLevel),
      hasConflict: this.readBoolean(record['hasConflict']),
      conflictWithTaskId: this.cleanString(record['conflictWithTaskId']),
      conflictSummary: this.cleanString(record['conflictSummary']),
      createdAt: createdAt > 0 ? createdAt : startTime,
      updatedAt: updatedAt > 0 ? updatedAt : createdAt > 0 ? createdAt : startTime
    };
  }

  private readTextIfExists(path: string): string | undefined {
    try {
      return this.readText(path);
    } catch (error) {
      return undefined;
    }
  }

  private readText(path: string): string {
    const bytes = new Uint8Array(this.readBinary(path));
    return new TextDecoder().decode(bytes);
  }

  private writeText(path: string, contents: string): void {
    const file = fs.openSync(path, fs.OpenMode.CREATE | fs.OpenMode.READ_WRITE | fs.OpenMode.TRUNC);
    try {
      const bytes = new TextEncoder().encode(contents);
      fs.writeSync(file.fd, bytes.buffer);
    } finally {
      fs.closeSync(file);
    }
  }

  private cleanString(value: Object | null | undefined): string | undefined {
    if (typeof value !== 'string') {
      return undefined;
    }
    const normalized = value.trim();
    return normalized.length > 0 ? normalized : undefined;
  }

  private safeObject(value: Object | null | undefined): Record<string, Object> | undefined {
    if (!value || Array.isArray(value) || typeof value !== 'object') {
      return undefined;
    }
    return value as Record<string, Object>;
  }

  private readMillis(primary: Object | null | undefined, secondary: Object | null | undefined, tertiary: Object | null | undefined): number {
    const candidates = [primary, secondary, tertiary];
    for (let index = 0; index < candidates.length; index += 1) {
      const parsed = Number(candidates[index]);
      if (Number.isFinite(parsed) && parsed > 0) {
        return Math.floor(parsed);
      }
    }
    return 0;
  }

  private readOptionalMillis(value: Object | null | undefined): number | undefined {
    const parsed = Number(value);
    if (Number.isFinite(parsed) && parsed > 0) {
      return Math.floor(parsed);
    }
    return undefined;
  }

  private readInteger(value: Object | null | undefined): number {
    const parsed = Number(value);
    if (Number.isFinite(parsed) && parsed >= 0) {
      return Math.floor(parsed);
    }
    return 0;
  }

  private readBoolean(value: Object | null | undefined, fallback: boolean = false): boolean {
    if (typeof value === 'boolean') {
      return value;
    }
    if (typeof value === 'string') {
      const normalized = value.trim().toLowerCase();
      if (normalized === 'true') {
        return true;
      }
      if (normalized === 'false') {
        return false;
      }
    }
    return fallback;
  }

  private readAlarmCascade(value: Object | null | undefined, urgencyLevel: UrgencyLevel): string[] {
    if (Array.isArray(value)) {
      const cascade: string[] = [];
      value.forEach((entry: Object | string) => {
        if (typeof entry === 'string') {
          const normalized = entry.trim();
          if (normalized.length > 0) {
            cascade.push(normalized);
          }
        }
      });
      if (cascade.length > 0) {
        return cascade;
      }
    }
    return buildCascade(urgencyLevel);
  }

  private formatTimeDisplay(epochMillis: number): string {
    const date = new Date(epochMillis);
    const hour = `${date.getHours()}`.padStart(2, '0');
    const minute = `${date.getMinutes()}`.padStart(2, '0');
    return `${hour}:${minute}`;
  }

  private formatDateRange(startTime: number, endTime?: number): string {
    const startDate = new Date(startTime);
    const year = startDate.getFullYear();
    const month = `${startDate.getMonth() + 1}`.padStart(2, '0');
    const day = `${startDate.getDate()}`.padStart(2, '0');
    if (!endTime) {
      return `${year}-${month}-${day}`;
    }
    const endDate = new Date(endTime);
    const endYear = endDate.getFullYear();
    const endMonth = `${endDate.getMonth() + 1}`.padStart(2, '0');
    const endDay = `${endDate.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day} ~ ${endYear}-${endMonth}-${endDay}`;
  }
}

export const fileStore = new FileStore();
