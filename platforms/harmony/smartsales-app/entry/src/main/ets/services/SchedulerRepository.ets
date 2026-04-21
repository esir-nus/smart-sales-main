// File: platforms/harmony/smartsales-app/entry/src/main/ets/services/SchedulerRepository.ets
// Module: smartsales-app/entry
// Summary: 日程仓库管理层 — Phase 2C 手动创建、冲突检测与重排
// Author: created on 2026-04-21

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
    this.snapshot = this.buildSnapshot(this.cloneTasks(this.tasks), false, '');
    console.info(`[HS-006][SchedulerRepository] initialize restored count=${this.tasks.length}`);
    this.emit();
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
      updatedAt: Date.now()
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
      return;
    }
    if (task.isDone) {
      return;
    }
    this.patchTask(taskId, {
      isDone: true,
      updatedAt: Date.now()
    });
  }

  async deleteTask(taskId: string): Promise<void> {
    const beforeCount = this.tasks.length;
    this.tasks = this.tasks.filter((task: ScheduledTask) => task.id !== taskId);
    if (beforeCount === this.tasks.length) {
      return;
    }
    this.cancelReminderOrDefer(taskId, 'delete');
    this.persist();
  }

  async loadConflictScenarioPreset(): Promise<void> {
    const firstStart = new Date(2030, 0, 15, 9, 30, 0, 0).getTime();
    const secondStart = new Date(2030, 0, 15, 10, 0, 0, 0).getTime();
    const createdAt = Date.now();
    this.tasks = [
      this.buildTask({
        title: 'HS-006 conflict anchor',
        startTime: firstStart,
        durationMinutes: 60,
        urgencyLevel: UrgencyLevel.L2_IMPORTANT,
        notes: 'Scenario A anchor task at 09:30 for 60 minutes.'
      }, createdAt - 2_000),
      this.buildTask({
        title: 'HS-006 conflict candidate',
        startTime: secondStart,
        durationMinutes: 60,
        urgencyLevel: UrgencyLevel.L2_IMPORTANT,
        notes: 'Scenario A overlapping task at 10:00 for 60 minutes.'
      }, createdAt - 1_000)
    ];
    console.info('[HS-006][scheduler] loadConflictScenarioPreset seeded overlapping future tasks');
    this.persist();
  }

  private buildTask(input: SchedulerCreateInput, now: number): ScheduledTask {
    const endTime = this.computeEndTime(input.startTime, input.durationMinutes);
    return {
      id: this.createTaskId(),
      timeDisplay: this.formatTimeDisplay(input.startTime),
      title: input.title.trim(),
      urgencyLevel: input.urgencyLevel,
      isDone: false,
      hasAlarm: true,
      isSmartAlarm: false,
      startTime: input.startTime,
      endTime,
      durationMinutes: input.durationMinutes,
      durationSource: input.durationMinutes > 0 ? DurationSource.USER_SET : DurationSource.DEFAULT,
      conflictPolicy: ConflictPolicy.EXCLUSIVE,
      dateRange: this.formatDateRange(input.startTime, endTime),
      notes: this.cleanNotes(input.notes),
      alarmCascade: buildCascade(input.urgencyLevel),
      hasConflict: false,
      conflictWithTaskId: undefined,
      conflictSummary: undefined,
      createdAt: now,
      updatedAt: now
    };
  }

  private findTask(taskId: string): ScheduledTask | undefined {
    return this.tasks.find((task: ScheduledTask) => task.id === taskId);
  }

  private patchTask(taskId: string, patch: SchedulerTaskPatch): void {
    this.tasks = this.tasks.map((task: ScheduledTask) => {
      if (task.id !== taskId) {
        return task;
      }
      return {
        ...task,
        isDone: patch.isDone !== undefined ? patch.isDone : task.isDone,
        startTime: patch.startTime !== undefined ? patch.startTime : task.startTime,
        endTime: patch.endTime !== undefined ? patch.endTime : task.endTime,
        durationMinutes: patch.durationMinutes !== undefined ? patch.durationMinutes : task.durationMinutes,
        durationSource: patch.durationMinutes !== undefined ? (patch.durationMinutes > 0 ? DurationSource.USER_SET : DurationSource.DEFAULT) : task.durationSource,
        urgencyLevel: patch.urgencyLevel !== undefined ? patch.urgencyLevel : task.urgencyLevel,
        alarmCascade: patch.alarmCascade !== undefined ? patch.alarmCascade.slice() : task.alarmCascade.slice(),
        timeDisplay: patch.timeDisplay !== undefined ? patch.timeDisplay : task.timeDisplay,
        dateRange: patch.dateRange !== undefined ? patch.dateRange : task.dateRange,
        updatedAt: patch.updatedAt !== undefined ? patch.updatedAt : task.updatedAt
      };
    });
    this.persist();
  }

  private persist(): void {
    const context = this.requireContext();
    this.tasks = this.sortTasks(this.evaluateConflicts(this.tasks));
    fileStore.saveTasks(context, this.tasks);
    this.snapshot = this.buildSnapshot(this.cloneTasks(this.tasks), false, '');
    this.emit();
  }

  private evaluateConflicts(tasks: ScheduledTask[]): ScheduledTask[] {
    const next = this.cloneTasks(tasks);
    next.forEach((task: ScheduledTask) => {
      task.hasConflict = false;
      task.conflictWithTaskId = undefined;
      task.conflictSummary = undefined;
    });
    const contenders = next
      .filter((task: ScheduledTask) => !task.isDone && task.urgencyLevel !== UrgencyLevel.FIRE_OFF && task.durationMinutes > 0)
      .sort((left: ScheduledTask, right: ScheduledTask) => {
        if (left.createdAt !== right.createdAt) {
          return left.createdAt - right.createdAt;
        }
        return left.startTime - right.startTime;
      });
    contenders.forEach((task: ScheduledTask, index: number) => {
      const taskEnd = task.startTime + task.durationMinutes * 60_000;
      for (let cursor = 0; cursor < index; cursor += 1) {
        const earlier = contenders[cursor];
        const earlierEnd = earlier.startTime + earlier.durationMinutes * 60_000;
        if (task.startTime < earlierEnd && earlier.startTime < taskEnd) {
          task.hasConflict = true;
          task.conflictWithTaskId = earlier.id;
          task.conflictSummary = `与「${earlier.title}」时间冲突`;
          console.info(`[HS-006][scheduler] evaluateConflicts conflict taskId=${task.id} withTaskId=${earlier.id}`);
          return;
        }
      }
    });
    return next;
  }

  private buildSnapshot(tasks: ScheduledTask[], isLoading: boolean, lastErrorMessage?: string): SchedulerSnapshot {
    return {
      tasks,
      isLoading,
      lastErrorMessage
    };
  }

  private cloneTasks(tasks: ScheduledTask[]): ScheduledTask[] {
    return tasks.map((task: ScheduledTask) => ({
      ...task,
      alarmCascade: task.alarmCascade.slice()
    }));
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
      return left.createdAt - right.createdAt;
    });
    return copy;
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
    return `${`${date.getHours()}`.padStart(2, '0')}:${`${date.getMinutes()}`.padStart(2, '0')}`;
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

  private cleanNotes(notes?: string): string | undefined {
    if (!notes) {
      return undefined;
    }
    const normalized = notes.trim();
    return normalized.length > 0 ? normalized : undefined;
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
