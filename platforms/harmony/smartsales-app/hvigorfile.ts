// File: platforms/harmony/smartsales-app/hvigorfile.ts
// Module: smartsales-app
// Summary: 构建脚本 — 从 local.properties 生成运行时配置
// Author: created on 2026-04-16
// 模式来源: tingwu-container/hvigorfile.ts

import fs from 'node:fs';
import { createRequire } from 'node:module';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

type AppRuntimeConfigShape = {
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
  AI_API_BASE_URL: string;
  AI_API_KEY: string;
};

const PROJECT_DIR = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(PROJECT_DIR, '..', '..', '..');
const LOCAL_PROPERTIES_PATH = path.resolve(REPO_ROOT, 'local.properties');
const GENERATED_CONFIG_PATH = path.resolve(
  PROJECT_DIR,
  'entry',
  'src',
  'main',
  'ets',
  'config',
  'AppConfig.local.ets'
);
const require = createRequire(import.meta.url);

// 构建时必需的键（AI 键在 Phase 2C 前是可选的）
const REQUIRED_KEYS: Array<keyof AppRuntimeConfigShape> = [
  'TINGWU_BASE_URL',
  'TINGWU_APP_KEY',
  'TINGWU_ACCESS_KEY_ID',
  'TINGWU_ACCESS_KEY_SECRET',
  'OSS_ACCESS_KEY_ID',
  'OSS_ACCESS_KEY_SECRET',
  'OSS_BUCKET_NAME',
  'OSS_ENDPOINT'
];

generateAppConfig();

export function generateAppConfig(): void {
  const localProperties = loadLocalProperties(LOCAL_PROPERTIES_PATH);
  const config = resolveRuntimeConfig(localProperties);
  const missing = REQUIRED_KEYS.filter((key: keyof AppRuntimeConfigShape) => {
    return config[key].trim().length === 0;
  });

  if (missing.length > 0) {
    throw new Error(
      `[AppConfig] Missing required keys in local.properties: ${missing.join(', ')}`
    );
  }

  fs.mkdirSync(path.dirname(GENERATED_CONFIG_PATH), { recursive: true });
  const nextContents = buildGeneratedModule(config);
  const currentContents = fs.existsSync(GENERATED_CONFIG_PATH)
    ? fs.readFileSync(GENERATED_CONFIG_PATH, 'utf8')
    : '';

  if (currentContents !== nextContents) {
    fs.writeFileSync(GENERATED_CONFIG_PATH, nextContents, 'utf8');
  }
}

export function loadLocalProperties(filePath: string): Record<string, string> {
  if (!fs.existsSync(filePath)) {
    throw new Error(
      `[AppConfig] Missing repo-root local.properties at ${filePath}`
    );
  }
  return parsePropertiesText(fs.readFileSync(filePath, 'utf8'));
}

export function parsePropertiesText(contents: string): Record<string, string> {
  const properties: Record<string, string> = {};
  toLogicalLines(contents).forEach((rawLine: string) => {
    const trimmedLeft = rawLine.replace(/^[\t\f ]+/, '');
    if (!trimmedLeft || trimmedLeft.startsWith('#') || trimmedLeft.startsWith('!')) {
      return;
    }
    const { key, value } = splitPropertyLine(rawLine);
    properties[decodePropertySegment(key)] = decodePropertySegment(value);
  });
  return properties;
}

function resolveRuntimeConfig(
  properties: Record<string, string>
): AppRuntimeConfigShape {
  const tingwuAppKey = pickProperty(properties, ['TINGWU_APP_KEY']);
  const tingwuApiKey = pickProperty(properties, ['TINGWU_API_KEY'], tingwuAppKey);
  const tingwuAccessKeyId = pickProperty(
    properties,
    ['TINGWU_ACCESS_KEY_ID', 'ALIBABA_CLOUD_ACCESS_KEY_ID']
  );
  const tingwuAccessKeySecret = pickProperty(
    properties,
    ['TINGWU_ACCESS_KEY_SECRET', 'ALIBABA_CLOUD_ACCESS_KEY_SECRET']
  );
  const ossAccessKeyId = pickProperty(
    properties,
    ['OSS_ACCESS_KEY_ID', 'ALIBABA_CLOUD_ACCESS_KEY_ID']
  );
  const ossAccessKeySecret = pickProperty(
    properties,
    ['OSS_ACCESS_KEY_SECRET', 'ALIBABA_CLOUD_ACCESS_KEY_SECRET']
  );

  return {
    TINGWU_BASE_URL: normalizeTingwuBaseUrl(pickProperty(properties, ['TINGWU_BASE_URL'])),
    TINGWU_API_KEY: tingwuApiKey,
    TINGWU_APP_KEY: tingwuAppKey,
    TINGWU_ACCESS_KEY_ID: tingwuAccessKeyId,
    TINGWU_ACCESS_KEY_SECRET: tingwuAccessKeySecret,
    TINGWU_SECURITY_TOKEN: pickProperty(properties, ['TINGWU_SECURITY_TOKEN']),
    OSS_ACCESS_KEY_ID: ossAccessKeyId,
    OSS_ACCESS_KEY_SECRET: ossAccessKeySecret,
    OSS_BUCKET_NAME: pickProperty(properties, ['OSS_BUCKET_NAME']),
    OSS_ENDPOINT: normalizeOssEndpoint(
      pickProperty(properties, ['OSS_ENDPOINT'], 'oss-cn-beijing.aliyuncs.com')
    ),
    AI_API_BASE_URL: pickProperty(properties, ['AI_API_BASE_URL']),
    AI_API_KEY: pickProperty(properties, ['AI_API_KEY'])
  };
}

function pickProperty(
  properties: Record<string, string>,
  keys: string[],
  fallback = ''
): string {
  for (const key of keys) {
    const value = properties[key]?.trim();
    if (value && value.length > 0) {
      return value;
    }
  }
  return fallback.trim();
}

function normalizeTingwuBaseUrl(raw: string): string {
  const trimmed = raw.trim();
  if (!trimmed) {
    return '';
  }
  const withoutTrailingSlash = trimmed.endsWith('/') ? trimmed.slice(0, -1) : trimmed;
  if (withoutTrailingSlash.includes('/openapi/tingwu/v2')) {
    return `${withoutTrailingSlash}/`;
  }
  return `${withoutTrailingSlash}/openapi/tingwu/v2/`;
}

function normalizeOssEndpoint(raw: string): string {
  return raw.trim().replace(/^https?:\/\//, '').replace(/\/+$/, '');
}

function buildGeneratedModule(config: AppRuntimeConfigShape): string {
  const rows = Object.entries(config).map(([key, value]) => {
    return `  ${key}: '${escapeForSingleQuotedString(value)}'`;
  });

  return `import type { AppRuntimeConfigShape } from './AppConfig';

// Auto-generated from repo-root local.properties by hvigorfile.ts.
// Do not edit or commit this file.
export const appRuntimeConfig: AppRuntimeConfigShape = {
${rows.join(',\n')}
};
`;
}

function escapeForSingleQuotedString(value: string): string {
  return value
    .replace(/\\/g, '\\\\')
    .replace(/'/g, "\\'")
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n');
}

function toLogicalLines(contents: string): string[] {
  const lines = contents.replace(/\r\n?/g, '\n').split('\n');
  const logicalLines: string[] = [];
  let currentLine = '';
  let isContinuation = false;

  lines.forEach((line: string) => {
    const nextSegment = isContinuation ? line.replace(/^[\t\f ]+/, '') : line;
    currentLine = `${currentLine}${nextSegment}`;
    if (endsWithOddBackslashCount(currentLine)) {
      currentLine = currentLine.slice(0, -1);
      isContinuation = true;
      return;
    }
    logicalLines.push(currentLine);
    currentLine = '';
    isContinuation = false;
  });

  if (currentLine.length > 0) {
    logicalLines.push(currentLine);
  }
  return logicalLines;
}

function splitPropertyLine(rawLine: string): { key: string; value: string } {
  const keyEnd = findKeyEnd(rawLine);
  if (keyEnd >= rawLine.length) {
    return { key: rawLine.trim(), value: '' };
  }

  let cursor = keyEnd;
  while (cursor < rawLine.length && isKeyValueWhitespace(rawLine[cursor])) {
    cursor += 1;
  }
  if (cursor < rawLine.length && (rawLine[cursor] === '=' || rawLine[cursor] === ':')) {
    cursor += 1;
  }
  while (cursor < rawLine.length && isKeyValueWhitespace(rawLine[cursor])) {
    cursor += 1;
  }

  return { key: rawLine.slice(0, keyEnd), value: rawLine.slice(cursor) };
}

function findKeyEnd(rawLine: string): number {
  let index = 0;
  while (index < rawLine.length) {
    const char = rawLine[index];
    if (char === '\\') {
      index += 2;
      continue;
    }
    if (char === '=' || char === ':' || isKeyValueWhitespace(char)) {
      return index;
    }
    index += 1;
  }
  return rawLine.length;
}

function decodePropertySegment(segment: string): string {
  let decoded = '';
  for (let index = 0; index < segment.length; index += 1) {
    const char = segment[index];
    if (char !== '\\') {
      decoded += char;
      continue;
    }
    index += 1;
    if (index >= segment.length) {
      decoded += '\\';
      break;
    }
    const escaped = segment[index];
    if (escaped === 'u' && index + 4 < segment.length) {
      const codePoint = segment.slice(index + 1, index + 5);
      if (/^[0-9a-fA-F]{4}$/.test(codePoint)) {
        decoded += String.fromCharCode(parseInt(codePoint, 16));
        index += 4;
        continue;
      }
    }
    decoded += decodeEscapedChar(escaped);
  }
  return decoded;
}

function decodeEscapedChar(char: string): string {
  switch (char) {
    case 't': return '\t';
    case 'n': return '\n';
    case 'r': return '\r';
    case 'f': return '\f';
    default: return char;
  }
}

function endsWithOddBackslashCount(line: string): boolean {
  let backslashCount = 0;
  for (let index = line.length - 1; index >= 0; index -= 1) {
    if (line[index] !== '\\') break;
    backslashCount += 1;
  }
  return backslashCount % 2 === 1;
}

function isKeyValueWhitespace(char: string): boolean {
  return char === ' ' || char === '\t' || char === '\f';
}

function resolveHvigorAppTasks(): unknown {
  try {
    return require('@ohos/hvigor-ohos-plugin').appTasks;
  } catch {
    return undefined;
  }
}

const appTasks = resolveHvigorAppTasks();

export default appTasks ? {
  system: appTasks,
  plugins: []
} : {
  plugins: []
};
