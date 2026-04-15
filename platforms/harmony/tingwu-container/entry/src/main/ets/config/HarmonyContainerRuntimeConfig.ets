import {
  emptyHarmonyRuntimeConfig,
  getMissingConfigKeys,
  HarmonyRuntimeConfig
} from './HarmonyContainerConfig';

interface HarmonyRuntimeConfigModule {
  harmonyRuntimeConfig?: HarmonyRuntimeConfig;
}

const LOCAL_CONFIG_MODULE = './HarmonyContainerConfig.local';

let runtimeConfig: HarmonyRuntimeConfig = cloneRuntimeConfig(emptyHarmonyRuntimeConfig);
let loadPromise: Promise<HarmonyRuntimeConfig> | undefined = undefined;

export async function ensureHarmonyRuntimeConfigLoaded(): Promise<HarmonyRuntimeConfig> {
  if (!loadPromise) {
    loadPromise = loadHarmonyRuntimeConfig();
  }
  return loadPromise;
}

export function getHarmonyRuntimeConfig(): HarmonyRuntimeConfig {
  return runtimeConfig;
}

export function getHarmonyRuntimeConfigMissingKeys(): string[] {
  return getMissingConfigKeys(runtimeConfig);
}

async function loadHarmonyRuntimeConfig(): Promise<HarmonyRuntimeConfig> {
  try {
    const moduleLoader = createModuleLoader();
    const localModule = await moduleLoader(LOCAL_CONFIG_MODULE);
    runtimeConfig = sanitizeRuntimeConfig(localModule.harmonyRuntimeConfig);
  } catch (_error) {
    console.warn('Harmony local config unavailable, falling back to empty config.');
    runtimeConfig = cloneRuntimeConfig(emptyHarmonyRuntimeConfig);
  }
  return runtimeConfig;
}

function sanitizeRuntimeConfig(
  candidate?: HarmonyRuntimeConfig
): HarmonyRuntimeConfig {
  const source = candidate || emptyHarmonyRuntimeConfig;
  const nextConfig: HarmonyRuntimeConfig = {
    TINGWU_BASE_URL: source.TINGWU_BASE_URL.trim(),
    TINGWU_API_KEY: source.TINGWU_API_KEY.trim(),
    TINGWU_APP_KEY: source.TINGWU_APP_KEY.trim(),
    TINGWU_ACCESS_KEY_ID: source.TINGWU_ACCESS_KEY_ID.trim(),
    TINGWU_ACCESS_KEY_SECRET: source.TINGWU_ACCESS_KEY_SECRET.trim(),
    TINGWU_SECURITY_TOKEN: source.TINGWU_SECURITY_TOKEN.trim(),
    OSS_ACCESS_KEY_ID: source.OSS_ACCESS_KEY_ID.trim(),
    OSS_ACCESS_KEY_SECRET: source.OSS_ACCESS_KEY_SECRET.trim(),
    OSS_BUCKET_NAME: source.OSS_BUCKET_NAME.trim(),
    OSS_ENDPOINT: source.OSS_ENDPOINT.trim()
  };
  return nextConfig;
}

function createModuleLoader(): (
  specifier: string
) => Promise<HarmonyRuntimeConfigModule> {
  return (specifier: string) => {
    return import(specifier) as Promise<HarmonyRuntimeConfigModule>;
  };
}

function cloneRuntimeConfig(source: HarmonyRuntimeConfig): HarmonyRuntimeConfig {
  return {
    TINGWU_BASE_URL: source.TINGWU_BASE_URL,
    TINGWU_API_KEY: source.TINGWU_API_KEY,
    TINGWU_APP_KEY: source.TINGWU_APP_KEY,
    TINGWU_ACCESS_KEY_ID: source.TINGWU_ACCESS_KEY_ID,
    TINGWU_ACCESS_KEY_SECRET: source.TINGWU_ACCESS_KEY_SECRET,
    TINGWU_SECURITY_TOKEN: source.TINGWU_SECURITY_TOKEN,
    OSS_ACCESS_KEY_ID: source.OSS_ACCESS_KEY_ID,
    OSS_ACCESS_KEY_SECRET: source.OSS_ACCESS_KEY_SECRET,
    OSS_BUCKET_NAME: source.OSS_BUCKET_NAME,
    OSS_ENDPOINT: source.OSS_ENDPOINT
  };
}
