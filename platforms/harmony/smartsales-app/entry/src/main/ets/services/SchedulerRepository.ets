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
  buildAlarmCascade,
  createEmptySchedulerSnapshot
} from '../domain/Scheduler';
import { fileStore } from './FileStore';

type SchedulerSnapshotListener = (snapshot: SchedulerSnapshot) => void;

export interface SchedulerCreateInput {
  title: string;
  startTime: number;
  urgencyLevel: UrgencyLevel;
  notes?: string;
}

interface SchedulerTaskPatch {
  isDone?: boolean;
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
    this.tasks = this.sortTasks(fileStore.loadTasks(context));
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
    this.publishReminderOrDefer(task);
    return task;
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
    this.cancelReminderOrDefer(taskId);
    this.persist();
  }

  private buildTask(input: SchedulerCreateInput, now: number): ScheduledTask {
    const startTime = input.startTime;
    const endTime = undefined;
    const urgencyLevel = input.urgencyLevel;
    const alarmCascade = buildAlarmCascade(urgencyLevel);
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
      durationMinutes: 0,
      durationSource: DurationSource.DEFAULT,
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
      timeDisplay: task.timeDisplay,
      title: task.title,
      urgencyLevel: task.urgencyLevel,
      isDone: patch.isDone !== undefined ? patch.isDone : task.isDone,
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
      alarmCascade: task.alarmCascade,
      hasConflict: task.hasConflict,
      conflictWithTaskId: task.conflictWithTaskId,
      conflictSummary: task.conflictSummary,
      isVague: task.isVague,
      clarificationState: task.clarificationState,
      createdAt: task.createdAt,
      updatedAt: patch.updatedAt !== undefined ? patch.updatedAt : task.updatedAt
    };
  }

  private persist(): void {
    const context = this.requireContext();
    this.tasks = this.sortTasks(this.tasks);
    fileStore.saveTasks(context, this.tasks);
    this.snapshot = this.buildSnapshot(this.cloneTasks(this.tasks), false, '');
    this.emit();
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

  private publishReminderOrDefer(task: ScheduledTask): void {
    if (task.startTime <= Date.now()) {
      return;
    }
    console.warn(`[HS-006][Reminder] deferred -> HS-011 publishReminder blocked taskId=${task.id}`);
  }

  private cancelReminderOrDefer(taskId: string): void {
    console.warn(`[HS-006][Reminder] deferred -> HS-011 cancelReminder blocked taskId=${taskId}`);
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
