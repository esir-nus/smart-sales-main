import { common } from '@kit.AbilityKit';
import { fileIo as fs } from '@kit.CoreFileKit';
import util from '@ohos.util';
import {
  HarmonyTingwuArtifacts,
  HarmonyTingwuAudioItem
} from '../model/HarmonyTingwuModels';
import { guessExtension } from '../utils/HarmonyFormatUtils';

const METADATA_FILE_NAME = 'harmony_tingwu_metadata.json';
const TEXT_DECODER = new util.TextDecoder();
const TEXT_ENCODER = new util.TextEncoder();

export interface ImportedAudioRecord {
  displayName: string;
  localFileName: string;
  localPath: string;
  extension: string;
  sizeBytes: number;
}

class HarmonyFileStore {
  metadataPath(context: common.UIAbilityContext): string {
    return `${context.filesDir}/${METADATA_FILE_NAME}`;
  }

  audioFileName(audioId: string, extension: string): string {
    return `harmony_${audioId}.${extension}`;
  }

  audioPath(context: common.UIAbilityContext, audioId: string, extension: string): string {
    return `${context.filesDir}/${this.audioFileName(audioId, extension)}`;
  }

  artifactPath(context: common.UIAbilityContext, audioId: string): string {
    return `${context.filesDir}/harmony_${audioId}_artifacts.json`;
  }

  loadMetadata(context: common.UIAbilityContext): HarmonyTingwuAudioItem[] {
    const raw = this.readTextIfExists(this.metadataPath(context));
    if (!raw) {
      return [];
    }
    try {
      const parsed: Object = JSON.parse(raw) as Object;
      return Array.isArray(parsed) ? parsed as HarmonyTingwuAudioItem[] : [];
    } catch (_error) {
      console.error('Failed to parse harmony metadata.');
      return [];
    }
  }

  saveMetadata(context: common.UIAbilityContext, items: HarmonyTingwuAudioItem[]): void {
    this.writeText(this.metadataPath(context), JSON.stringify(items, null, 2));
  }

  loadArtifacts(context: common.UIAbilityContext, audioId: string): HarmonyTingwuArtifacts | undefined {
    const raw = this.readTextIfExists(this.artifactPath(context, audioId));
    if (!raw) {
      return undefined;
    }
    try {
      const parsed: Object = JSON.parse(raw) as Object;
      return parsed as HarmonyTingwuArtifacts;
    } catch (_error) {
      console.error('Failed to parse harmony artifacts.');
      return undefined;
    }
  }

  saveArtifacts(context: common.UIAbilityContext, audioId: string, artifacts: HarmonyTingwuArtifacts): void {
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
    } catch (_error) {
      return undefined;
    }
  }

  private readText(path: string): string {
    const bytes = new Uint8Array(this.readBinary(path));
    return TEXT_DECODER.decode(bytes);
  }

  private writeText(path: string, contents: string): void {
    const file = fs.openSync(path, fs.OpenMode.CREATE | fs.OpenMode.READ_WRITE | fs.OpenMode.TRUNC);
    try {
      const bytes = TEXT_ENCODER.encode(contents);
      fs.writeSync(file.fd, bytes.buffer);
    } finally {
      fs.closeSync(file);
    }
  }
}

export const harmonyFileStore = new HarmonyFileStore();
