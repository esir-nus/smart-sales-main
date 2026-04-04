import {
  emptyHarmonyRuntimeConfig,
  getMissingConfigKeys,
  HarmonyRuntimeConfig
} from './HarmonyContainerConfig';

type HarmonyRuntimeConfigModule = {
  harmonyRuntimeConfig?: HarmonyRuntimeConfig;
};

const LOCAL_CONFIG_MODULE = './HarmonyContainerConfig.local';

let runtimeConfig: HarmonyRuntimeConfig = {
  ...emptyHarmonyRuntimeConfig
};
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
  } catch (error) {
    console.warn(`Harmony local config unavailable, falling back to empty config: ${error}`);
    runtimeConfig = {
      ...emptyHarmonyRuntimeConfig
    };
  }
  return runtimeConfig;
}

function sanitizeRuntimeConfig(
  candidate?: HarmonyRuntimeConfig
): HarmonyRuntimeConfig {
  const nextConfig: HarmonyRuntimeConfig = {
    ...emptyHarmonyRuntimeConfig,
    ...(candidate || emptyHarmonyRuntimeConfig)
  };

  return {
    ...nextConfig,
    TINGWU_BASE_URL: nextConfig.TINGWU_BASE_URL.trim(),
    TINGWU_API_KEY: nextConfig.TINGWU_API_KEY.trim(),
    TINGWU_APP_KEY: nextConfig.TINGWU_APP_KEY.trim(),
    TINGWU_ACCESS_KEY_ID: nextConfig.TINGWU_ACCESS_KEY_ID.trim(),
    TINGWU_ACCESS_KEY_SECRET: nextConfig.TINGWU_ACCESS_KEY_SECRET.trim(),
    TINGWU_SECURITY_TOKEN: nextConfig.TINGWU_SECURITY_TOKEN.trim(),
    OSS_ACCESS_KEY_ID: nextConfig.OSS_ACCESS_KEY_ID.trim(),
    OSS_ACCESS_KEY_SECRET: nextConfig.OSS_ACCESS_KEY_SECRET.trim(),
    OSS_BUCKET_NAME: nextConfig.OSS_BUCKET_NAME.trim(),
    OSS_ENDPOINT: nextConfig.OSS_ENDPOINT.trim()
  };
}

function createModuleLoader(): (
  specifier: string
) => Promise<HarmonyRuntimeConfigModule> {
  return (specifier: string) => {
    return import(specifier) as Promise<HarmonyRuntimeConfigModule>;
  };
}
