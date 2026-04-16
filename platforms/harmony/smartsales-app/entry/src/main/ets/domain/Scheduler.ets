// File: platforms/harmony/smartsales-app/entry/src/main/ets/domain/Scheduler.ets
// Module: smartsales-app/entry
// Summary: 日程领域模型 — 翻译自 domain/scheduler/ Kotlin 层
// Author: created on 2026-04-16
// 翻译来源: domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/

// Phase 2B 实现时完善，当前为占位符定义核心类型

// 紧急程度枚举（翻译自 UrgencyLevel.kt）
export enum UrgencyLevel {
  CRITICAL = 'CRITICAL',
  HIGH = 'HIGH',
  NORMAL = 'NORMAL',
  LOW = 'LOW',
  DEFERRED = 'DEFERRED'
}

// 日程任务数据模型（翻译自 ScheduledTask.kt 核心字段）
export interface ScheduledTask {
  id: string;
  title: string;
  description: string;
  startTime: number; // Unix 时间戳（毫秒）
  endTime?: number;
  urgencyLevel: UrgencyLevel;
  isCompleted: boolean;
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
    isLoading: true
  };
}
