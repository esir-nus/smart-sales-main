// File: platforms/harmony/smartsales-app/entry/src/main/ets/utils/FormatUtils.ets
// Module: smartsales-app/entry
// Summary: 格式化工具 — 日期、字节、状态标签、转写渲染、摘要工具
// Author: created on 2026-04-16
// 迁移来源: platforms/harmony/tingwu-container/entry/src/main/ets/utils/HarmonyFormatUtils.ets

import {
  AudioJobStatus,
  DiarizedSegment,
  SmartSummary,
  SpeakerSummary,
  QuestionAnswer
} from '../domain/Audio';

type JsonValue = Object | string | number | boolean | null | undefined;

// 生成唯一音频 ID
export function createAudioId(): string {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;
}

// 从文件名猜测扩展名
export function guessExtension(name: string): string {
  const normalized = name.trim().toLowerCase();
  const index = normalized.lastIndexOf('.');
  if (index < 0 || index === normalized.length - 1) {
    return 'm4a';
  }
  return normalized.slice(index + 1).replace(/[^a-z0-9]/g, '') || 'm4a';
}

// 猜测 MIME 类型
export function guessMimeType(extension: string): string {
  const normalized = extension.toLowerCase();
  switch (normalized) {
    case 'wav':
      return 'audio/wav';
    case 'mp3':
      return 'audio/mpeg';
    case 'm4a':
      return 'audio/mp4';
    case 'aac':
      return 'audio/aac';
    case 'ogg':
      return 'audio/ogg';
    case 'flac':
      return 'audio/flac';
    default:
      return 'application/octet-stream';
  }
}

// 格式化字节大小
export function formatBytes(sizeBytes: number): string {
  if (sizeBytes < 1024) {
    return `${sizeBytes} B`;
  }
  if (sizeBytes < 1024 * 1024) {
    return `${(sizeBytes / 1024).toFixed(1)} KB`;
  }
  return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
}

// 格式化时间戳为可读日期
export function formatDateTime(epochMillis?: number): string {
  if (!epochMillis) {
    return '--';
  }
  const date = new Date(epochMillis);
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  const hour = `${date.getHours()}`.padStart(2, '0');
  const minute = `${date.getMinutes()}`.padStart(2, '0');
  return `${year}-${month}-${day} ${hour}:${minute}`;
}

// 格式化持续时间（毫秒 -> MM:SS）
export function formatDuration(ms: number): string {
  const seconds = Math.max(0, Math.floor(ms / 1000));
  const minutesPart = `${Math.floor(seconds / 60)}`.padStart(2, '0');
  const secondsPart = `${seconds % 60}`.padStart(2, '0');
  return `${minutesPart}:${secondsPart}`;
}

// 构建 OSS 对象键
export function buildObjectKey(fileName: string): string {
  const safeName = fileName.replace(/[^a-zA-Z0-9_.-]/g, '_');
  return `smartsales/harmony/audio/${Date.now()}/${safeName}`;
}

// 构建转写 Markdown（带说话人标签）
export function buildTranscriptMarkdown(
  segments: DiarizedSegment[],
  speakerLabels: Record<string, string>,
  transcriptText?: string
): string {
  if (segments.length === 0) {
    return transcriptText?.trim() || '';
  }
  return segments.map((segment: DiarizedSegment) => {
    const speakerId = segment.speakerId || `Speaker ${segment.speakerIndex + 1}`;
    const speakerLabel = speakerLabels[speakerId] || speakerId;
    return `**${speakerLabel}** ${formatDuration(segment.startMs)}-${formatDuration(segment.endMs)}\n${segment.text}`;
  }).join('\n\n');
}

// 生成摘要文本（优先使用结构化摘要，降级到关键点，再降级到转写预览）
export function summarizeArtifacts(summary?: SmartSummary, transcriptMarkdown?: string): string {
  if (summary?.summary && summary.summary.trim().length > 0) {
    return summary.summary.trim();
  }
  if (summary && summary.keyPoints.length > 0) {
    return summary.keyPoints.join(' ').trim();
  }
  return transcriptMarkdown?.slice(0, 180).trim() || 'Ready for reopen.';
}

// 转写文本预览（去除 Markdown 标记，截断）
export function transcriptPreview(transcriptMarkdown?: string): string {
  if (!transcriptMarkdown) {
    return '';
  }
  return transcriptMarkdown.replace(/\*\*/g, '').replace(/\n+/g, ' ').slice(0, 220).trim();
}

// 任务状态显示标签
export function statusLabel(status: AudioJobStatus): string {
  switch (status) {
    case AudioJobStatus.IMPORTED:
      return 'Imported';
    case AudioJobStatus.UPLOADING:
      return 'Uploading';
    case AudioJobStatus.PROCESSING:
      return 'Processing';
    case AudioJobStatus.COMPLETED:
      return 'Completed';
    case AudioJobStatus.FAILED:
      return 'Needs retry';
    default:
      return 'Unknown';
  }
}

// 去重字符串数组
export function uniqueStrings(values: string[]): string[] {
  const seen: Record<string, boolean> = {};
  const result: string[] = [];
  values.forEach((value: string) => {
    const normalized = value.trim();
    if (normalized.length === 0 || seen[normalized]) {
      return;
    }
    seen[normalized] = true;
    result.push(normalized);
  });
  return result;
}

// 安全字符串提取（非字符串或空白返回 undefined）
export function cleanString(value: JsonValue): string | undefined {
  if (typeof value !== 'string') {
    return undefined;
  }
  const normalized = value.trim();
  return normalized.length > 0 ? normalized : undefined;
}

// 安全对象数组提取
export function arrayOfObjects(value: JsonValue): Array<Record<string, Object>> {
  if (!Array.isArray(value)) {
    return [];
  }
  const result: Array<Record<string, Object>> = [];
  (value as Array<JsonValue>).forEach((item: JsonValue) => {
    if (typeof item === 'object' && item !== null && !Array.isArray(item)) {
      result.push(item as Record<string, Object>);
    }
  });
  return result;
}

// 安全 object 提取
export function safeObject(value: JsonValue): Record<string, Object> | undefined {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    return undefined;
  }
  return value as Record<string, Object>;
}

// 安全字符串数组提取
export function safeStringArray(value: JsonValue): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  const result: string[] = [];
  (value as Array<JsonValue>).forEach((item: JsonValue) => {
    const normalized = cleanString(item);
    if (normalized !== undefined) {
      result.push(normalized);
    }
  });
  return result;
}

// 从 JSON 解析说话人摘要列表
export function makeSpeakerSummaries(items: Array<Record<string, Object>>): SpeakerSummary[] {
  const result: SpeakerSummary[] = [];
  items.forEach((item: Record<string, Object>) => {
    const summary = cleanString(item['Summary']) || cleanString(item['summary']);
    if (!summary) {
      return;
    }
    result.push({
      name: cleanString(item['Name']) || cleanString(item['name']),
      summary
    });
  });
  return result;
}

// 从 JSON 解析问答对列表
export function makeQuestionAnswers(items: Array<Record<string, Object>>): QuestionAnswer[] {
  const result: QuestionAnswer[] = [];
  items.forEach((item: Record<string, Object>) => {
    const question = cleanString(item['Question']) || cleanString(item['question']);
    const answer = cleanString(item['Answer']) || cleanString(item['answer']);
    if (!question || !answer) {
      return;
    }
    result.push({ question, answer });
  });
  return result;
}
