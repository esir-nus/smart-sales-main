import { common } from '@kit.AbilityKit';
import { fileIo as fs } from '@kit.CoreFileKit';
import util from '@ohos.util';
import {
  HarmonySchedulerTaskRecord,
  HarmonySchedulerTraceEvent
} from '../model/HarmonySchedulerModels';

const TASKS_FILE_NAME = 'harmony_scheduler_backend_tasks.json';
const EVENTS_FILE_NAME = 'harmony_scheduler_backend_events.json';
const TEXT_DECODER = new util.TextDecoder();
const TEXT_ENCODER = new util.TextEncoder();

class HarmonySchedulerStore {
  private tasksPath(context: common.UIAbilityContext): string {
    return `${context.filesDir}/${TASKS_FILE_NAME}`;
  }

  private eventsPath(context: common.UIAbilityContext): string {
    return `${context.filesDir}/${EVENTS_FILE_NAME}`;
  }

  loadTasks(context: common.UIAbilityContext): HarmonySchedulerTaskRecord[] {
    const raw = this.readTextIfExists(this.tasksPath(context));
    if (!raw) {
      return [];
    }
    try {
      const parsed: Object = JSON.parse(raw) as Object;
      return Array.isArray(parsed) ? parsed as HarmonySchedulerTaskRecord[] : [];
    } catch (_error) {
      console.error('Failed to parse harmony scheduler tasks.');
      return [];
    }
  }

  saveTasks(context: common.UIAbilityContext, tasks: HarmonySchedulerTaskRecord[]): void {
    this.writeText(this.tasksPath(context), JSON.stringify(tasks, null, 2));
  }

  loadEvents(context: common.UIAbilityContext): HarmonySchedulerTraceEvent[] {
    const raw = this.readTextIfExists(this.eventsPath(context));
    if (!raw) {
      return [];
    }
    try {
      const parsed: Object = JSON.parse(raw) as Object;
      return Array.isArray(parsed) ? parsed as HarmonySchedulerTraceEvent[] : [];
    } catch (_error) {
      console.error('Failed to parse harmony scheduler events.');
      return [];
    }
  }

  saveEvents(context: common.UIAbilityContext, events: HarmonySchedulerTraceEvent[]): void {
    this.writeText(this.eventsPath(context), JSON.stringify(events, null, 2));
  }

  clear(context: common.UIAbilityContext): void {
    this.saveTasks(context, []);
    this.saveEvents(context, []);
  }

  private readTextIfExists(path: string): string | undefined {
    try {
      return this.readText(path);
    } catch (_error) {
      return undefined;
    }
  }

  private readText(path: string): string {
    const file = fs.openSync(path, fs.OpenMode.READ_ONLY);
    try {
      const stat = fs.statSync(path);
      const buffer = new ArrayBuffer(Number(stat.size));
      const bytesRead = fs.readSync(file.fd, buffer);
      const payload = bytesRead === buffer.byteLength ? buffer : buffer.slice(0, bytesRead);
      return TEXT_DECODER.decode(new Uint8Array(payload));
    } finally {
      fs.closeSync(file);
    }
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

export const harmonySchedulerStore = new HarmonySchedulerStore();
