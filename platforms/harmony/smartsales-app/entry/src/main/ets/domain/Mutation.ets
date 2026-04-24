// File: platforms/harmony/smartsales-app/entry/src/main/ets/domain/Mutation.ets
// Module: smartsales-app/entry
// Summary: 统一变更合约 — 翻译自 domain/core/UnifiedMutation.kt（"唯一货币"）
// Author: created on 2026-04-16
// 翻译来源: domain/core/src/main/java/com/smartsales/prism/domain/core/UnifiedMutation.kt

// Phase 2C 实现时完善，当前为占位符定义核心类型
// UnifiedMutation 是所有 LLM 结构化意图输出的绝对根类型

// 个人资料变更
export interface ProfileMutation {
  entityId: string;
  field: string;
  oldValue?: string;
  newValue: string;
}

// 任务变更
export interface TaskMutation {
  action: 'create' | 'update' | 'cancel';
  taskId?: string;
  title?: string;
  startTime?: number;
  urgencyLevel?: string;
}

// 插件调度指令
export interface PluginDispatchInstruction {
  pluginId: string;
  action: string;
  parameters: Record<string, string>;
}

// 统一变更（LLM -> 后端的唯一合约）
export interface UnifiedMutation {
  queryQuality: string;
  missingEntities: string[];
  response?: string;
  profileMutations: ProfileMutation[];
  tasks: TaskMutation[];
  pluginDispatch?: PluginDispatchInstruction;
}
