// File: platforms/harmony/smartsales-app/entry/src/main/ets/domain/Scheduler.ets
// Module: smartsales-app/entry
// Summary: 日程领域模型 — 翻译自 domain/scheduler/ Kotlin 层
// Author: created on 2026-04-16
// 翻译来源: domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/

// 紧急程度枚举（翻译自 UrgencyLevel.kt）
export enum UrgencyLevel {
  L1_CRITICAL = 'L1_CRITICAL',
  L2_IMPORTANT = 'L2_IMPORTANT',
  L3_NORMAL = 'L3_NORMAL',
  FIRE_OFF = 'FIRE_OFF'
}

// 持续时间来源（翻译自 DurationSource）
export enum DurationSource {
  USER_SET = 'USER_SET',
  INFERRED = 'INFERRED',
  FOLLOW_UP = 'FOLLOW_UP',
  LEARNED = 'LEARNED',
  DEFAULT = 'DEFAULT'
}

// 冲突策略（翻译自 ConflictPolicy）
export enum ConflictPolicy {
  EXCLUSIVE = 'EXCLUSIVE',
  COEXISTING = 'COEXISTING',
  BACKGROUND = 'BACKGROUND'
}

// 澄清候选人
export interface ClarificationPersonCandidate {
  entityId: string;
  displayName: string;
  description?: string;
}

// 澄清状态（翻译自 ClarificationState）
export type ClarificationState = ClarificationAmbiguousPerson | ClarificationMissingInformation;

export interface ClarificationAmbiguousPerson {
  kind: 'AMBIGUOUS_PERSON';
  question: string;
  candidates: ClarificationPersonCandidate[];
}

export interface ClarificationMissingInformation {
  kind: 'MISSING_INFORMATION';
  question: string;
}

// 日程任务数据模型（翻译自 ScheduledTask.kt 语义字段，并补充 Harmony 本地元数据）
export interface ScheduledTask {
  id: string;
  timeDisplay: string;
  title: string;
  urgencyLevel: UrgencyLevel;
  isDone: boolean;
  hasAlarm: boolean;
  isSmartAlarm: boolean;
  startTime: number; // Unix 时间戳（毫秒）
  endTime?: number;
  durationMinutes: number;
  durationSource: DurationSource;
  conflictPolicy: ConflictPolicy;
  dateRange: string;
  location?: string;
  notes?: string;
  keyPerson?: string;
  keyPersonEntityId?: string;
  highlights?: string;
  alarmCascade: string[];
  hasConflict: boolean;
  conflictWithTaskId?: string;
  conflictSummary?: string;
  isVague: boolean;
  clarificationState?: ClarificationState;
  createdAt: number;
  updatedAt: number;
}

// 日程仓库快照
export interface SchedulerSnapshot {
  tasks: ScheduledTask[];
  isLoading: boolean;
  lastErrorMessage?: string;
}

export function createEmptySchedulerSnapshot(): SchedulerSnapshot {
  return {
    tasks: [],
    isLoading: true,
    lastErrorMessage: ''
  };
}

export function buildAlarmCascade(level: UrgencyLevel): string[] {
  switch (level) {
    case UrgencyLevel.L1_CRITICAL:
      return ['-1h', '-10m', '0m'];
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
