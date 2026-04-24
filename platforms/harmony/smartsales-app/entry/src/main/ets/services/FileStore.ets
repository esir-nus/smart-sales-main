// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/FileStore.ets
// Module: smartsales-app/entry
// Summary: 本地文件存储服务 — 迁移自 tingwu-container HarmonyFileStore.ets
// Author: created on 2026-04-16

import { common } from '@kit.AbilityKit';
import { util } from '@kit.ArkTS';
import { fileIo as fs } from '@kit.CoreFileKit';
import { AudioItem, AudioArtifacts, ChapterSegment } from '../domain/Audio';
import {
  ScheduledTask,
  ClarificationState,
  ClarificationPersonCandidate,
  UrgencyLevel,
  buildAlarmCascade,
  parseConflictPolicy,
  parseDurationSource,
  parseUrgencyLevel
} from '../domain/Scheduler';
import { cleanString, guessExtension, safeObject } from '../utils/FormatUtils';

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

  audioFileName(audioId: string, extension: string): string {
    return `audio_${audioId}.${extension}`;
  }

  audioPath(context: common.UIAbilityContext, audioId: string, extension: string): string {
    return `${context.filesDir}/${this.audioFileName(audioId, extension)}`;
  }

  artifactPath(context: common.UIAbilityContext, audioId: string): string {
    return `${context.filesDir}/smartsales_${audioId}_artifacts.json`;
  }

  taskPath(context: common.UIAbilityContext): string {
    return `${context.filesDir}/${TASK_FILE_NAME}`;
  }

  loadMetadata(context: common.UIAbilityContext): AudioItem[] {
    const raw = this.readTextIfExists(this.metadataPath(context));
    if (!raw) {
      return [];
    }
    try {
      const parsed = JSON.parse(raw) as Object;
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
      const parsed = JSON.parse(raw) as Object;
      const record = safeObject(parsed);
      if (!record) {
        return undefined;
      }
      const artifacts = parsed as AudioArtifacts;
      artifacts.chapters = this.normalizeChapters(record['chapters']);
      return artifacts;
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
      const parsed = JSON.parse(raw) as Object;
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

  private normalizeChapters(value: Object | null | undefined): ChapterSegment[] {
    if (!Array.isArray(value)) {
      return [];
    }
    const chapters: ChapterSegment[] = [];
    value.forEach((entry: Object | string) => {
      if (typeof entry === 'string') {
        const title = cleanString(entry) || 'Chapter';
        chapters.push({
          title,
          startMs: 0,
          endMs: 0,
          summary: undefined
        });
        return;
      }
      const record = safeObject(entry);
      if (!record) {
        return;
      }
      const title = cleanString(record['title']) || cleanString(record['Title']) || 'Chapter';
      const startMs = this.readMillis(record['startMs'], record['StartMs'], record['Start']);
      const endMs = this.readMillis(record['endMs'], record['EndMs'], record['End']);
      const summary = cleanString(record['summary']) || cleanString(record['Summary']);
      chapters.push({
        title,
        startMs,
        endMs,
        summary
      });
    });
    return chapters;
  }

  private readMillis(
    primary: Object | null | undefined,
    secondary: Object | null | undefined,
    tertiary: Object | null | undefined
  ): number {
    const primaryValue = Number(primary);
    if (Number.isFinite(primaryValue) && primaryValue >= 0) {
      return Math.floor(primaryValue);
    }
    const secondaryValue = Number(secondary);
    if (Number.isFinite(secondaryValue) && secondaryValue >= 0) {
      return Math.floor(secondaryValue);
    }
    const tertiaryValue = Number(tertiary);
    if (Number.isFinite(tertiaryValue) && tertiaryValue >= 0) {
      return Math.floor(tertiaryValue);
    }
    return 0;
  }

  private normalizeTask(value: Object | null | undefined): ScheduledTask | undefined {
    const record = safeObject(value);
    if (!record) {
      return undefined;
    }
    const id = cleanString(record['id']);
    const title = cleanString(record['title']);
    if (!id || !title) {
      return undefined;
    }
    const startTime = this.readMillis(record['startTime'], record['startAt'], record['scheduledAt']);
    if (startTime <= 0) {
      return undefined;
    }
    const urgencyLevel = parseUrgencyLevel(record['urgencyLevel']);
    const createdAt = this.readMillis(record['createdAt'], record['created_at'], record['startTime']);
    const updatedAt = this.readMillis(record['updatedAt'], record['updated_at'], record['createdAt']);
    const endTime = this.readOptionalMillis(record['endTime']);
    const alarmCascade = this.readAlarmCascade(record['alarmCascade'], urgencyLevel);
    const clarificationState = this.readClarificationState(record['clarificationState']);
    const timeDisplay = cleanString(record['timeDisplay']) || this.formatTimeDisplay(startTime);
    const dateRange = cleanString(record['dateRange']) || this.formatDateRange(startTime, endTime);
    const isDone = this.readBoolean(record['isDone']) || this.readBoolean(record['isCompleted']);
    return {
      id,
      timeDisplay,
      title,
      urgencyLevel,
      isDone,
      hasAlarm: this.readBoolean(record['hasAlarm'], true),
      isSmartAlarm: this.readBoolean(record['isSmartAlarm']),
      startTime,
      endTime,
      durationMinutes: this.readInteger(record['durationMinutes']),
      durationSource: parseDurationSource(record['durationSource']),
      conflictPolicy: parseConflictPolicy(record['conflictPolicy']),
      dateRange,
      location: cleanString(record['location']),
      notes: cleanString(record['notes']) || cleanString(record['description']),
      keyPerson: cleanString(record['keyPerson']),
      keyPersonEntityId: cleanString(record['keyPersonEntityId']),
      highlights: cleanString(record['highlights']),
      alarmCascade,
      hasConflict: this.readBoolean(record['hasConflict']),
      conflictWithTaskId: cleanString(record['conflictWithTaskId']),
      conflictSummary: cleanString(record['conflictSummary']),
      isVague: this.readBoolean(record['isVague']),
      clarificationState,
      createdAt: createdAt > 0 ? createdAt : startTime,
      updatedAt: updatedAt > 0 ? updatedAt : createdAt > 0 ? createdAt : startTime
    };
  }

  private readOptionalMillis(value: Object | null | undefined): number | undefined {
    const millis = Number(value);
    if (Number.isFinite(millis) && millis > 0) {
      return Math.floor(millis);
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
        const normalized = cleanString(entry);
        if (normalized) {
          cascade.push(normalized);
        }
      });
      if (cascade.length > 0) {
        return cascade;
      }
    }
    return buildAlarmCascade(urgencyLevel);
  }

  private readClarificationState(value: Object | null | undefined): ClarificationState | undefined {
    const record = safeObject(value);
    if (!record) {
      return undefined;
    }
    const kind = cleanString(record['kind']);
    const question = cleanString(record['question']);
    if (!kind || !question) {
      return undefined;
    }
    if (kind === 'AMBIGUOUS_PERSON') {
      const candidates = this.readClarificationCandidates(record['candidates']);
      return {
        kind: 'AMBIGUOUS_PERSON',
        question,
        candidates
      };
    }
    if (kind === 'MISSING_INFORMATION') {
      return {
        kind: 'MISSING_INFORMATION',
        question
      };
    }
    return undefined;
  }

  private readClarificationCandidates(value: Object | null | undefined): ClarificationPersonCandidate[] {
    if (!Array.isArray(value)) {
      return [];
    }
    const candidates: ClarificationPersonCandidate[] = [];
    value.forEach((entry: Object) => {
      const record = safeObject(entry);
      if (!record) {
        return;
      }
      const entityId = cleanString(record['entityId']);
      const displayName = cleanString(record['displayName']);
      if (!entityId || !displayName) {
        return;
      }
      candidates.push({
        entityId,
        displayName,
        description: cleanString(record['description'])
      });
    });
    return candidates;
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

  private readTextIfExists(path: string): string | undefined {
    try {
      return this.readText(path);
    } catch (error) {
      return undefined;
    }
  }

  private readText(path: string): string {
    const bytes = new Uint8Array(this.readBinary(path));
    return util.TextDecoder.create('utf-8').decodeToString(bytes);
  }

  private writeText(path: string, contents: string): void {
    const file = fs.openSync(path, fs.OpenMode.CREATE | fs.OpenMode.READ_WRITE | fs.OpenMode.TRUNC);
    try {
      const bytes = new util.TextEncoder().encodeInto(contents);
      fs.writeSync(file.fd, bytes.buffer);
    } finally {
      fs.closeSync(file);
    }
  }
}

export const fileStore = new FileStore();
