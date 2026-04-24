// File: platforms/harmony/smartsales-app/entry/src/main/ets/domain/Session.ets
// Module: smartsales-app/entry
// Summary: 会话领域模型 — 翻译自 domain/session/ Kotlin 层
// Author: created on 2026-04-16
// 翻译来源: domain/session/src/main/java/com/smartsales/prism/domain/model/

// Phase 2C 实现时完善，当前为占位符定义核心类型

// 聊天消息（翻译自 ChatMessage.kt）
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: number;
}

// 会话预览（翻译自 SessionPreview.kt）
export interface SessionPreview {
  id: string;
  title: string;
  lastMessage: string;
  lastMessageAt: number;
  messageCount: number;
}

// 聊天仓库快照
export interface ChatSnapshot {
  messages: ChatMessage[];
  sessions: SessionPreview[];
  isLoading: boolean;
  isBusy: boolean;
  lastErrorMessage?: string;
}

export function createEmptyChatSnapshot(): ChatSnapshot {
  return {
    messages: [],
    sessions: [],
    isLoading: true,
    isBusy: false
  };
}
