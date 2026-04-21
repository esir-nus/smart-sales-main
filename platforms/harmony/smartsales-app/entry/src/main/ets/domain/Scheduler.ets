// File: platforms/harmony/smartsales-app/entry/src/main/ets/domain/Scheduler.ets
// Module: smartsales-app/entry
// Summary: 日程领域模型 — 对齐 Android 调度契约的 Harmony 定义
// Author: updated on 2026-04-21

export enum UrgencyLevel {
  L1_CRITICAL = 'L1_CRITICAL',
  L2_IMPORTANT = 'L2_IMPORTANT',
  L3_NORMAL = 'L3_NORMAL',
  FIRE_OFF = 'FIRE_OFF'
}

export enum DurationSource {
  USER_SET = 'USER_SET',
  INFERRED = 'INFERRED',
  FOLLOW_UP = 'FOLLOW_UP',
  LEARNED = 'LEARNED',
  DEFAULT = 'DEFAULT'
}

export enum ConflictPolicy {
  EXCLUSIVE = 'EXCLUSIVE',
  COEXISTING = 'COEXISTING',
  BACKGROUND = 'BACKGROUND'
}

export interface ScheduledTask {
  id: string;
  timeDisplay: string;
  title: string;
  urgencyLevel: UrgencyLevel;
  isDone: boolean;
  hasAlarm: boolean;
  isSmartAlarm: boolean;
  startTime: number;
  endTime?: number;
  durationMinutes: number;
  durationSource: DurationSource;
  conflictPolicy: ConflictPolicy;
  dateRange: string;
  notes?: string;
  alarmCascade: string[];
  hasConflict: boolean;
  conflictWithTaskId?: string;
  conflictSummary?: string;
  createdAt: number;
  updatedAt: number;
}

export interface SchedulerSnapshot {
  tasks: ScheduledTask[];
  isLoading: boolean;
  lastErrorMessage?: string;
}

export interface SchedulerRescheduleInput {
  taskId: string;
  newStart: number;
  newDurationMinutes: number;
  newUrgencyLevel?: UrgencyLevel;
}

export function createEmptySchedulerSnapshot(): SchedulerSnapshot {
  return {
    tasks: [],
    isLoading: true,
    lastErrorMessage: ''
  };
}

export function buildCascade(level: UrgencyLevel): string[] {
  switch (level) {
    case UrgencyLevel.L1_CRITICAL:
      return ['-60m', '-10m', '0m'];
    case UrgencyLevel.L2_IMPORTANT:
      return ['-30m', '0m'];
    case UrgencyLevel.L3_NORMAL:
      return ['0m'];
    case UrgencyLevel.FIRE_OFF:
      return ['0m'];
    default:
      return ['0m'];
  }
}

export function parseUrgencyLevel(value: Object | null | undefined): UrgencyLevel {
  switch (`${value || ''}`.trim()) {
    case UrgencyLevel.L1_CRITICAL:
    case 'CRITICAL':
      return UrgencyLevel.L1_CRITICAL;
    case UrgencyLevel.L2_IMPORTANT:
    case 'HIGH':
      return UrgencyLevel.L2_IMPORTANT;
    case UrgencyLevel.FIRE_OFF:
    case 'LOW':
    case 'DEFERRED':
      return UrgencyLevel.FIRE_OFF;
    case UrgencyLevel.L3_NORMAL:
    case 'NORMAL':
    default:
      return UrgencyLevel.L3_NORMAL;
  }
}

export function parseDurationSource(value: Object | null | undefined): DurationSource {
  switch (`${value || ''}`.trim()) {
    case DurationSource.USER_SET:
      return DurationSource.USER_SET;
    case DurationSource.INFERRED:
      return DurationSource.INFERRED;
    case DurationSource.FOLLOW_UP:
      return DurationSource.FOLLOW_UP;
    case DurationSource.LEARNED:
      return DurationSource.LEARNED;
    case DurationSource.DEFAULT:
    default:
      return DurationSource.DEFAULT;
  }
}

export function parseConflictPolicy(value: Object | null | undefined): ConflictPolicy {
  switch (`${value || ''}`.trim()) {
    case ConflictPolicy.COEXISTING:
      return ConflictPolicy.COEXISTING;
    case ConflictPolicy.BACKGROUND:
      return ConflictPolicy.BACKGROUND;
    case ConflictPolicy.EXCLUSIVE:
    default:
      return ConflictPolicy.EXCLUSIVE;
  }
}
