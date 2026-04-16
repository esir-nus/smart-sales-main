// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/FileStore.ets
// Module: smartsales-app/entry
// Summary: 本地文件存储服务 — 迁移自 tingwu-container HarmonyFileStore.ets
// Author: created on 2026-04-16

import { common } from '@kit.AbilityKit';
import { fileIo as fs } from '@kit.CoreFileKit';
import { AudioItem, AudioArtifacts } from '../domain/Audio';
import { guessExtension } from '../utils/FormatUtils';

const METADATA_FILE_NAME = 'smartsales_audio_metadata.json';

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
}

export const fileStore = new FileStore();
