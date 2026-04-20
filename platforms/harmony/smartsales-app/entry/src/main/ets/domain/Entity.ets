// File: platforms/harmony/smartsales-app/entry/src/main/ets/domain/Entity.ets
// Module: smartsales-app/entry
// Summary: 实体/CRM 领域模型 — 翻译自 domain/crm/ Kotlin 层
// Author: created on 2026-04-16
// 翻译来源: domain/crm/src/main/java/com/smartsales/prism/domain/memory/

// Phase 2D 实现时完善，当前为占位符定义核心类型

// 实体条目（翻译自 EntityModels.kt 核心字段）
export interface EntityEntry {
  id: string;
  displayName: string;
  aliases: string[];
  category: string;
  notes: string;
  createdAt: number;
  updatedAt: number;
}

// 实体仓库快照
export interface EntitySnapshot {
  entities: EntityEntry[];
  isLoading: boolean;
  lastErrorMessage?: string;
}

export function createEmptyEntitySnapshot(): EntitySnapshot {
  return {
    entities: [],
    isLoading: true
  };
}
