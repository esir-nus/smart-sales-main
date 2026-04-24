// File: platforms/harmony/smartsales-app/entry/src/main/ets/state/AppState.ets
// Module: smartsales-app/entry
// Summary: 应用级状态类型定义
// Author: created on 2026-04-16

// 应用全局状态（各功能仓库的聚合视图）
export interface AppState {
  isInitialized: boolean;
  configMissingKeys: string[];
  activeFeature: FeatureTab;
}

// 功能标签枚举
export enum FeatureTab {
  AUDIO = 'audio',
  SCHEDULER = 'scheduler',
  CHAT = 'chat',
  CONTACTS = 'contacts',
  SETTINGS = 'settings'
}

export function createInitialAppState(): AppState {
  return {
    isInitialized: false,
    configMissingKeys: [],
    activeFeature: FeatureTab.AUDIO
  };
}
