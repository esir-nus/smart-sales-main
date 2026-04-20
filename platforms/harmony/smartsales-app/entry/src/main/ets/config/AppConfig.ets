// File: platforms/harmony/smartsales-app/entry/src/main/ets/config/AppConfig.ets
// Module: smartsales-app/entry
// Summary: 应用运行时配置接口定义 — 所有平台密钥和端点
// Author: created on 2026-04-16

// 运行时配置接口，由 hvigorfile.ts 在构建时生成 AppConfig.local.ets 实现
export interface AppRuntimeConfigShape {
  // 听悟相关
  TINGWU_BASE_URL: string;
  TINGWU_API_KEY: string;
  TINGWU_APP_KEY: string;
  TINGWU_ACCESS_KEY_ID: string;
  TINGWU_ACCESS_KEY_SECRET: string;
  TINGWU_SECURITY_TOKEN: string;
  TINGWU_POLL_TIMEOUT_MS: string;
  TINGWU_FORCE_TASK_EXPIRED_JOB_ID: string;

  // OSS 存储
  OSS_ACCESS_KEY_ID: string;
  OSS_ACCESS_KEY_SECRET: string;
  OSS_BUCKET_NAME: string;
  OSS_ENDPOINT: string;

  // AI/LLM 服务（Phase 2C 启用）
  AI_API_BASE_URL: string;
  AI_API_KEY: string;
}

// 必需的配置键 — 构建时校验
export const REQUIRED_CONFIG_KEYS: Array<keyof AppRuntimeConfigShape> = [
  'TINGWU_BASE_URL',
  'TINGWU_APP_KEY',
  'TINGWU_ACCESS_KEY_ID',
  'TINGWU_ACCESS_KEY_SECRET',
  'OSS_ACCESS_KEY_ID',
  'OSS_ACCESS_KEY_SECRET',
  'OSS_BUCKET_NAME',
  'OSS_ENDPOINT'
];

// 空配置，用于初始化和回退
export function createEmptyConfig(): AppRuntimeConfigShape {
  return {
    TINGWU_BASE_URL: '',
    TINGWU_API_KEY: '',
    TINGWU_APP_KEY: '',
    TINGWU_ACCESS_KEY_ID: '',
    TINGWU_ACCESS_KEY_SECRET: '',
    TINGWU_SECURITY_TOKEN: '',
    TINGWU_POLL_TIMEOUT_MS: '',
    TINGWU_FORCE_TASK_EXPIRED_JOB_ID: '',
    OSS_ACCESS_KEY_ID: '',
    OSS_ACCESS_KEY_SECRET: '',
    OSS_BUCKET_NAME: '',
    OSS_ENDPOINT: '',
    AI_API_BASE_URL: '',
    AI_API_KEY: ''
  };
}

// 规范化 OSS 端点 URL
export function normalizeOssEndpoint(raw: string): string {
  const trimmed = raw.trim().replace(/^https?:\/\//, '');
  return trimmed.replace(/\/+$/, '');
}

// 规范化 Tingwu 基础 URL
export function normalizeTingwuBaseUrl(raw: string): string {
  const trimmed = raw.trim();
  if (trimmed.length === 0) {
    return 'https://tingwu.cn/openapi/tingwu/v2/';
  }
  const withoutTrailingSlash = trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed;
  if (withoutTrailingSlash.includes('/openapi/tingwu/v2')) {
    return `${withoutTrailingSlash}/`;
  }
  return `${withoutTrailingSlash}/openapi/tingwu/v2/`;
}
