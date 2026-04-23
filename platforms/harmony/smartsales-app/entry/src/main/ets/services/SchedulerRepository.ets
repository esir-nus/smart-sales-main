// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/SchedulerRepository.ets
// Module: smartsales-app/entry
// Summary: 日程仓库管理层 — Phase 2B 手动创建/列表/完成/删除
// Author: created on 2026-04-20

import { common } from '@kit.AbilityKit';
import {
  ScheduledTask,
  SchedulerSnapshot,
  UrgencyLevel,
  DurationSource,
  ConflictPolicy,
  buildCascade,
  createEmptySchedulerSnapshot
} from '../domain/Scheduler';
import { fileStore } from './FileStore';

type SchedulerSnapshotListener = (snapshot: SchedulerSnapshot) => void;

export interface SchedulerCreateInput {
  title: string;
  startTime: number;
  durationMinutes: number;
  urgencyLevel: UrgencyLevel;
  notes?: string;
}

interface SchedulerTaskPatch {
  isDone?: boolean;
  startTime?: number;
  endTime?: number;
  durationMinutes?: number;
  urgencyLevel?: UrgencyLevel;
  alarmCascade?: string[];
  timeDisplay?: string;
  dateRange?: string;
  hasConflict?: boolean;
  conflictWithTaskId?: string;
  conflictSummary?: string;
  updatedAt?: number;
}

class SchedulerRepository {
  private context?: common.UIAbilityContext;
  private tasks: ScheduledTask[] = [];
  private listeners: SchedulerSnapshotListener[] = [];
  private snapshot: SchedulerSnapshot = createEmptySchedulerSnapshot();

  async initialize(context: common.UIAbilityContext): Promise<void> {
    if (this.context?.filesDir === context.filesDir && !this.snapshot.isLoading) {
      return;
    }
    this.context = context;
    this.tasks = this.sortTasks(this.evaluateConflicts(fileStore.loadTasks(context)));
    this.snapshot = this.buildSnapshot(this.cloneTasks(this.tasks), false, this.snapshot.lastErrorMessage);
    console.info(`[HS-006][SchedulerRepository] initialize restored count=${this.tasks.length}`);
    this.emit();
  }

  getSnapshot(): SchedulerSnapshot {
    return this.snapshot;
  }

  subscribe(listener: SchedulerSnapshotListener): () => void {
    this.listeners.push(listener);
    listener(this.snapshot);
    return () => {
      this.listeners = this.listeners.filter((candidate: SchedulerSnapshotListener) => candidate !== listener);
    };
  }

  async addTask(input: SchedulerCreateInput): Promise<ScheduledTask> {
    const now = Date.now();
    const task = this.buildTask(input, now);
    this.tasks = [task, ...this.tasks];
    console.info(`[HS-006][scheduler] addTask id=${task.id} title=${task.title} startTime=${task.startTime}`);
    this.persist();
    const persisted = this.findTask(task.id);
    if (!persisted) {
      throw new Error(`Task missing after add: ${task.id}`);
    }
    this.publishReminderOrDefer(persisted, 'create');
    return persisted;
  }

  async rescheduleTask(
    taskId: string,
    newStart: number,
    newDurationMinutes: number,
    newUrgencyLevel?: UrgencyLevel
  ): Promise<ScheduledTask> {
    const task = this.findTask(taskId);
    if (!task) {
      throw new Error(`Task not found: ${taskId}`);
    }
    const nextUrgency = newUrgencyLevel !== undefined ? newUrgencyLevel : task.urgencyLevel;
    const nextEndTime = this.computeEndTime(newStart, newDurationMinutes);
    const nextUpdatedAt = Date.now();
    console.info(
      `[HS-006][scheduler] rescheduleTask id=${taskId} oldStart=${task.startTime} newStart=${newStart} oldDuration=${task.durationMinutes} newDuration=${newDurationMinutes}`
    );
    this.cancelReminderOrDefer(taskId, 'reschedule-old', task.startTime);
    this.patchTask(taskId, {
      startTime: newStart,
      endTime: nextEndTime,
      durationMinutes: newDurationMinutes,
      urgencyLevel: nextUrgency,
      alarmCascade: buildCascade(nextUrgency),
      timeDisplay: this.formatTimeDisplay(newStart),
      dateRange: this.formatDateRange(newStart, nextEndTime),
      updatedAt: nextUpdatedAt
    });
    const updated = this.findTask(taskId);
    if (!updated) {
      throw new Error(`Task missing after reschedule: ${taskId}`);
    }
    this.publishReminderOrDefer(updated, 'reschedule-new');
    return updated;
  }

  async completeTask(taskId: string): Promise<void> {
    const task = this.findTask(taskId);
    if (!task) {
      console.warn(`[HS-006][scheduler] complete skipped missing-task id=${taskId}`);
      return;
    }
    if (task.isDone) {
      console.info(`[HS-006][scheduler] complete skipped already-done id=${taskId}`);
      return;
    }
    this.patchTask(taskId, {
      isDone: true,
      updatedAt: Date.now()
    });
    console.info(`[HS-006][scheduler] completeTask id=${taskId}`);
  }

  async deleteTask(taskId: string): Promise<void> {
    const beforeCount = this.tasks.length;
    this.tasks = this.tasks.filter((task: ScheduledTask) => task.id !== taskId);
    if (this.tasks.length === beforeCount) {
      console.warn(`[HS-006][scheduler] delete skipped missing-task id=${taskId}`);
      return;
    }
    console.info(`[HS-006][scheduler] deleteTask id=${taskId}`);
    this.cancelReminderOrDefer(taskId, 'delete');
    this.persist();
  }

  async loadConflictScenarioPreset(): Promise<void> {
    const baseTime = new Date(2030, 0, 15, 9, 30, 0, 0).getTime();
    const firstCreatedAt = baseTime - 10_000;
    const secondCreatedAt = baseTime - 9_000;
    this.tasks = [
      this.buildTask({
        title: 'HS-006 conflict anchor',
        startTime: baseTime,
        durationMinutes: 60,
        urgencyLevel: UrgencyLevel.L2_IMPORTANT,
        notes: 'Scenario A anchor task at 09:30 for 60 minutes.'
      }, firstCreatedAt),
      this.buildTask({
        title: 'HS-006 conflict candidate',
        startTime: new Date(2030, 0, 15, 10, 0, 0, 0).getTime(),
        durationMinutes: 60,
        urgencyLevel: UrgencyLevel.L2_IMPORTANT,
        notes: 'Scenario A overlapping task at 10:00 for 60 minutes.'
      }, secondCreatedAt)
    ];
    console.info('[HS-006][scheduler] loadConflictScenarioPreset seeded overlapping future tasks');
    this.persist();
  }

  private buildTask(input: SchedulerCreateInput, now: number): ScheduledTask {
    const startTime = input.startTime;
    const endTime = this.computeEndTime(startTime, input.durationMinutes);
    const urgencyLevel = input.urgencyLevel;
    const alarmCascade = buildCascade(urgencyLevel);
    return {
      id: this.createTaskId(),
      timeDisplay: this.formatTimeDisplay(startTime),
      title: input.title.trim(),
      urgencyLevel,
      isDone: false,
      hasAlarm: true,
      isSmartAlarm: false,
      startTime,
      endTime,
      durationMinutes: input.durationMinutes,
      durationSource: input.durationMinutes > 0 ? DurationSource.USER_SET : DurationSource.DEFAULT,
      conflictPolicy: ConflictPolicy.EXCLUSIVE,
      dateRange: this.formatDateRange(startTime, endTime),
      location: undefined,
      notes: this.cleanNotes(input.notes),
      keyPerson: undefined,
      keyPersonEntityId: undefined,
      highlights: undefined,
      alarmCascade,
      hasConflict: false,
      conflictWithTaskId: undefined,
      conflictSummary: undefined,
      isVague: false,
      clarificationState: undefined,
      createdAt: now,
      updatedAt: now
    };
  }

  private cleanNotes(notes?: string): string | undefined {
    if (!notes) {
      return undefined;
    }
    const normalized = notes.trim();
    return normalized.length > 0 ? normalized : undefined;
  }

  private createTaskId(): string {
    return `task_${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;
  }

  private computeEndTime(startTime: number, durationMinutes: number): number | undefined {
    if (durationMinutes <= 0) {
      return undefined;
    }
    return startTime + durationMinutes * 60_000;
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

  private findTask(taskId: string): ScheduledTask | undefined {
    return this.tasks.find((task: ScheduledTask) => task.id === taskId);
  }

  private patchTask(taskId: string, patch: SchedulerTaskPatch): void {
    this.tasks = this.tasks.map((task: ScheduledTask) => {
      if (task.id !== taskId) {
        return task;
      }
      return this.mergeTask(task, patch);
    });
    this.persist();
  }

  private mergeTask(task: ScheduledTask, patch: SchedulerTaskPatch): ScheduledTask {
    return {
      id: task.id,
      timeDisplay: patch.timeDisplay !== undefined ? patch.timeDisplay : task.timeDisplay,
      title: task.title,
      urgencyLevel: patch.urgencyLevel !== undefined ? patch.urgencyLevel : task.urgencyLevel,
      isDone: patch.isDone !== undefined ? patch.isDone : task.isDone,
      hasAlarm: task.hasAlarm,
      isSmartAlarm: task.isSmartAlarm,
      startTime: patch.startTime !== undefined ? patch.startTime : task.startTime,
      endTime: patch.endTime !== undefined ? patch.endTime : task.endTime,
      durationMinutes: patch.durationMinutes !== undefined ? patch.durationMinutes : task.durationMinutes,
      durationSource: patch.durationMinutes !== undefined ? (patch.durationMinutes > 0 ? DurationSource.USER_SET : DurationSource.DEFAULT) : task.durationSource,
      conflictPolicy: task.conflictPolicy,
      dateRange: patch.dateRange !== undefined ? patch.dateRange : task.dateRange,
      location: task.location,
      notes: task.notes,
      keyPerson: task.keyPerson,
      keyPersonEntityId: task.keyPersonEntityId,
      highlights: task.highlights,
      alarmCascade: patch.alarmCascade !== undefined ? patch.alarmCascade.slice() : task.alarmCascade,
      hasConflict: patch.hasConflict !== undefined ? patch.hasConflict : task.hasConflict,
      conflictWithTaskId: patch.conflictWithTaskId !== undefined ? patch.conflictWithTaskId : task.conflictWithTaskId,
      conflictSummary: patch.conflictSummary !== undefined ? patch.conflictSummary : task.conflictSummary,
      isVague: task.isVague,
      clarificationState: task.clarificationState,
      createdAt: task.createdAt,
      updatedAt: patch.updatedAt !== undefined ? patch.updatedAt : task.updatedAt
    };
  }

  private persist(): void {
    const context = this.requireContext();
    this.tasks = this.sortTasks(this.evaluateConflicts(this.tasks));
    fileStore.saveTasks(context, this.tasks);
    this.snapshot = this.buildSnapshot(this.cloneTasks(this.tasks), false, '');
    this.emit();
  }

  private evaluateConflicts(tasks: ScheduledTask[]): ScheduledTask[] {
    const next = this.cloneTasks(tasks).map((task: ScheduledTask) => ({
      ...task,
      hasConflict: false,
      conflictWithTaskId: undefined,
      conflictSummary: undefined
    }));
    const contenders = next
      .filter((task: ScheduledTask) => !task.isDone && task.urgencyLevel !== UrgencyLevel.FIRE_OFF)
      .sort((left: ScheduledTask, right: ScheduledTask) => {
        if (left.createdAt !== right.createdAt) {
          return left.createdAt - right.createdAt;
        }
        return left.startTime - right.startTime;
      });
    contenders.forEach((task: ScheduledTask, index: number) => {
      const taskWindow = this.taskWindow(task);
      if (!taskWindow) {
        return;
      }
      for (let cursor = 0; cursor < index; cursor += 1) {
        const earlier = contenders[cursor];
        const earlierWindow = this.taskWindow(earlier);
        if (!earlierWindow) {
          continue;
        }
        if (!this.windowsOverlap(taskWindow.start, taskWindow.end, earlierWindow.start, earlierWindow.end)) {
          continue;
        }
        task.hasConflict = true;
        task.conflictWithTaskId = earlier.id;
        task.conflictSummary = `与「${earlier.title}」时间冲突`;
        console.info(`[HS-006][scheduler] evaluateConflicts conflict taskId=${task.id} withTaskId=${earlier.id}`);
        return;
      }
    });
    return next;
  }

  private taskWindow(task: ScheduledTask): { start: number; end: number } | undefined {
    if (task.durationMinutes <= 0) {
      return undefined;
    }
    return {
      start: task.startTime,
      end: task.startTime + task.durationMinutes * 60_000
    };
  }

  private windowsOverlap(leftStart: number, leftEnd: number, rightStart: number, rightEnd: number): boolean {
    return leftStart < rightEnd && rightStart < leftEnd;
  }

  private buildSnapshot(tasks: ScheduledTask[], isLoading: boolean, lastErrorMessage?: string): SchedulerSnapshot {
    return {
      tasks,
      isLoading,
      lastErrorMessage
    };
  }

  private cloneTasks(tasks: ScheduledTask[]): ScheduledTask[] {
    return tasks.map((task: ScheduledTask) => {
      const clone: ScheduledTask = {
        id: task.id,
        timeDisplay: task.timeDisplay,
        title: task.title,
        urgencyLevel: task.urgencyLevel,
        isDone: task.isDone,
        hasAlarm: task.hasAlarm,
        isSmartAlarm: task.isSmartAlarm,
        startTime: task.startTime,
        endTime: task.endTime,
        durationMinutes: task.durationMinutes,
        durationSource: task.durationSource,
        conflictPolicy: task.conflictPolicy,
        dateRange: task.dateRange,
        location: task.location,
        notes: task.notes,
        keyPerson: task.keyPerson,
        keyPersonEntityId: task.keyPersonEntityId,
        highlights: task.highlights,
        alarmCascade: task.alarmCascade.slice(),
        hasConflict: task.hasConflict,
        conflictWithTaskId: task.conflictWithTaskId,
        conflictSummary: task.conflictSummary,
        isVague: task.isVague,
        clarificationState: task.clarificationState,
        createdAt: task.createdAt,
        updatedAt: task.updatedAt
      };
      return clone;
    });
  }

  private sortTasks(tasks: ScheduledTask[]): ScheduledTask[] {
    const copy = this.cloneTasks(tasks);
    copy.sort((left: ScheduledTask, right: ScheduledTask) => {
      if (left.isDone !== right.isDone) {
        return left.isDone ? 1 : -1;
      }
      if (left.startTime !== right.startTime) {
        return left.startTime - right.startTime;
      }
      return right.createdAt - left.createdAt;
    });
    return copy;
  }

  private publishReminderOrDefer(task: ScheduledTask, reason: string): void {
    if (task.startTime <= Date.now()) {
      return;
    }
    console.warn(
      `[HS-006][Reminder] deferred -> HS-011 publishReminder blocked taskId=${task.id} reason=${reason} startTime=${task.startTime}`
    );
  }

  private cancelReminderOrDefer(taskId: string, reason: string, oldStartTime?: number): void {
    console.warn(
      `[HS-006][Reminder] deferred -> HS-011 cancelReminder blocked taskId=${taskId} reason=${reason} oldStartTime=${oldStartTime || 0}`
    );
  }

  private requireContext(): common.UIAbilityContext {
    if (!this.context) {
      throw new Error('SchedulerRepository context not initialized');
    }
    return this.context;
  }

  private emit(): void {
    this.listeners.forEach((listener: SchedulerSnapshotListener) => listener(this.snapshot));
  }
}

export const schedulerRepository = new SchedulerRepository();
