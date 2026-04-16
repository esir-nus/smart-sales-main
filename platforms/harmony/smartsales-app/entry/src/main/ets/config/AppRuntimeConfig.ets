// File: platforms/harmony/smartsales-app/entry/src/main/ets/config/AppRuntimeConfig.ets
// Module: smartsales-app/entry
// Summary: 运行时配置加载器 — 动态导入构建生成的配置，缺失时优雅回退
// Author: created on 2026-04-16

import { AppRuntimeConfigShape, REQUIRED_CONFIG_KEYS, createEmptyConfig } from './AppConfig';

let cachedConfig: AppRuntimeConfigShape | undefined = undefined;
let loadPromise: Promise<void> | undefined = undefined;

// 确保运行时配置已加载（只加载一次）
export async function ensureAppRuntimeConfigLoaded(): Promise<void> {
  if (cachedConfig !== undefined) {
    return;
  }
  if (loadPromise !== undefined) {
    return loadPromise;
  }
  loadPromise = doLoad();
  return loadPromise;
}

async function doLoad(): Promise<void> {
  try {
    const module = await import('./AppConfig.local');
    cachedConfig = module.appRuntimeConfig as AppRuntimeConfigShape;
  } catch {
    // 构建产物缺失时回退到空配置（全新克隆不会崩溃）
    cachedConfig = createEmptyConfig();
  }
}

// 获取当前配置
export function getAppRuntimeConfig(): AppRuntimeConfigShape {
  return cachedConfig ?? createEmptyConfig();
}

// 获取缺失的必需配置键
export function getAppRuntimeConfigMissingKeys(): string[] {
  const config = getAppRuntimeConfig();
  return REQUIRED_CONFIG_KEYS.filter((key: keyof AppRuntimeConfigShape) => {
    const value = config[key];
    return value === undefined || value === null || value.trim().length === 0;
  });
}
