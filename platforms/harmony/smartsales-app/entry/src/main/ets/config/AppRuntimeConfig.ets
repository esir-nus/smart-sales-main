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
    const value = readConfigValue(config, key);
    return value === undefined || value === null || value.trim().length === 0;
  });
}

function readConfigValue(config: AppRuntimeConfigShape, key: keyof AppRuntimeConfigShape): string {
  switch (key) {
    case 'TINGWU_BASE_URL':
      return config.TINGWU_BASE_URL;
    case 'TINGWU_API_KEY':
      return config.TINGWU_API_KEY;
    case 'TINGWU_APP_KEY':
      return config.TINGWU_APP_KEY;
    case 'TINGWU_ACCESS_KEY_ID':
      return config.TINGWU_ACCESS_KEY_ID;
    case 'TINGWU_ACCESS_KEY_SECRET':
      return config.TINGWU_ACCESS_KEY_SECRET;
    case 'TINGWU_SECURITY_TOKEN':
      return config.TINGWU_SECURITY_TOKEN;
    case 'TINGWU_POLL_TIMEOUT_MS':
      return config.TINGWU_POLL_TIMEOUT_MS;
    case 'TINGWU_FORCE_TASK_EXPIRED_JOB_ID':
      return config.TINGWU_FORCE_TASK_EXPIRED_JOB_ID;
    case 'OSS_ACCESS_KEY_ID':
      return config.OSS_ACCESS_KEY_ID;
    case 'OSS_ACCESS_KEY_SECRET':
      return config.OSS_ACCESS_KEY_SECRET;
    case 'OSS_BUCKET_NAME':
      return config.OSS_BUCKET_NAME;
    case 'OSS_ENDPOINT':
      return config.OSS_ENDPOINT;
    case 'AI_API_BASE_URL':
      return config.AI_API_BASE_URL;
    case 'AI_API_KEY':
      return config.AI_API_KEY;
    default:
      return '';
  }
}
