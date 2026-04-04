// Harmony runtime values are generated into HarmonyContainerConfig.local.ets by hvigorfile.ts.
// HarmonyContainerRuntimeConfig.ets is the stable tracked entrypoint that loads those values when present.
// Keep this file tracked and secret-free so the Harmony root owns the contract without committing credentials.
export interface HarmonyRuntimeConfig {
  TINGWU_BASE_URL: string;
  TINGWU_API_KEY: string;
  TINGWU_APP_KEY: string;
  TINGWU_ACCESS_KEY_ID: string;
  TINGWU_ACCESS_KEY_SECRET: string;
  TINGWU_SECURITY_TOKEN: string;
  OSS_ACCESS_KEY_ID: string;
  OSS_ACCESS_KEY_SECRET: string;
  OSS_BUCKET_NAME: string;
  OSS_ENDPOINT: string;
}

export const emptyHarmonyRuntimeConfig: HarmonyRuntimeConfig = {
  TINGWU_BASE_URL: '',
  TINGWU_API_KEY: '',
  TINGWU_APP_KEY: '',
  TINGWU_ACCESS_KEY_ID: '',
  TINGWU_ACCESS_KEY_SECRET: '',
  TINGWU_SECURITY_TOKEN: '',
  OSS_ACCESS_KEY_ID: '',
  OSS_ACCESS_KEY_SECRET: '',
  OSS_BUCKET_NAME: '',
  OSS_ENDPOINT: ''
};

const REQUIRED_KEYS: Array<keyof HarmonyRuntimeConfig> = [
  'TINGWU_BASE_URL',
  'TINGWU_APP_KEY',
  'TINGWU_ACCESS_KEY_ID',
  'TINGWU_ACCESS_KEY_SECRET',
  'OSS_ACCESS_KEY_ID',
  'OSS_ACCESS_KEY_SECRET',
  'OSS_BUCKET_NAME',
  'OSS_ENDPOINT'
];

export function getMissingConfigKeys(
  config: HarmonyRuntimeConfig = emptyHarmonyRuntimeConfig
): string[] {
  const missing: string[] = [];
  REQUIRED_KEYS.forEach((key: keyof HarmonyRuntimeConfig) => {
    if (config[key].trim().length === 0) {
      missing.push(key);
    }
  });
  return missing;
}

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

export function normalizeOssEndpoint(raw: string): string {
  const trimmed = raw.trim().replace(/^https?:\/\//, '');
  return trimmed.replace(/\/+$/, '');
}
