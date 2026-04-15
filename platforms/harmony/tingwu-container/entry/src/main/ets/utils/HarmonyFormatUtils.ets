import {
  HarmonyDiarizedSegment,
  HarmonyJobStatus,
  HarmonyQuestionAnswer,
  HarmonySmartSummary,
  HarmonySpeakerSummary
} from '../model/HarmonyTingwuModels';

type HarmonyLooseValue = Object | string | number | boolean | undefined | null;
type HarmonyLooseObject = Record<string, Object>;

export function createHarmonyAudioId(): string {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;
}

export function guessExtension(name: string): string {
  const normalized = name.trim().toLowerCase();
  const index = normalized.lastIndexOf('.');
  if (index < 0 || index === normalized.length - 1) {
    return 'm4a';
  }
  return normalized.slice(index + 1).replace(/[^a-z0-9]/g, '') || 'm4a';
}

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

export function formatBytes(sizeBytes: number): string {
  if (sizeBytes < 1024) {
    return `${sizeBytes} B`;
  }
  if (sizeBytes < 1024 * 1024) {
    return `${(sizeBytes / 1024).toFixed(1)} KB`;
  }
  return `${(sizeBytes / (1024 * 1024)).toFixed(1)} MB`;
}

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

export function formatDuration(ms: number): string {
  const seconds = Math.max(0, Math.floor(ms / 1000));
  const minutesPart = `${Math.floor(seconds / 60)}`.padStart(2, '0');
  const secondsPart = `${seconds % 60}`.padStart(2, '0');
  return `${minutesPart}:${secondsPart}`;
}

export function buildObjectKey(fileName: string): string {
  const safeName = fileName.replace(/[^a-zA-Z0-9_.-]/g, '_');
  return `smartsales/harmony/audio/${Date.now()}/${safeName}`;
}

export function buildTranscriptMarkdown(
  segments: HarmonyDiarizedSegment[],
  speakerLabels: Record<string, string>,
  transcriptText?: string
): string {
  if (segments.length === 0) {
    return transcriptText?.trim() || '';
  }
  return segments.map((segment: HarmonyDiarizedSegment) => {
    const speakerId = segment.speakerId || `Speaker ${segment.speakerIndex + 1}`;
    const speakerLabel = speakerLabels[speakerId] || speakerId;
    return `**${speakerLabel}** ${formatDuration(segment.startMs)}-${formatDuration(segment.endMs)}\n${segment.text}`;
  }).join('\n\n');
}

export function summarizeArtifacts(summary?: HarmonySmartSummary, transcriptMarkdown?: string): string {
  if (summary?.summary && summary.summary.trim().length > 0) {
    return summary.summary.trim();
  }
  if (summary && summary.keyPoints.length > 0) {
    return summary.keyPoints.join(' ').trim();
  }
  return transcriptMarkdown?.slice(0, 180).trim() || 'Ready for reopen.';
}

export function transcriptPreview(transcriptMarkdown?: string): string {
  if (!transcriptMarkdown) {
    return '';
  }
  return transcriptMarkdown.replace(/\*\*/g, '').replace(/\n+/g, ' ').slice(0, 220).trim();
}

export function statusLabel(status: HarmonyJobStatus): string {
  switch (status) {
    case HarmonyJobStatus.IMPORTED:
      return 'Imported';
    case HarmonyJobStatus.UPLOADING:
      return 'Uploading';
    case HarmonyJobStatus.PROCESSING:
      return 'Processing';
    case HarmonyJobStatus.COMPLETED:
      return 'Completed';
    case HarmonyJobStatus.FAILED:
      return 'Needs retry';
    default:
      return 'Unknown';
  }
}

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

export function cleanString(value: HarmonyLooseValue): string | undefined {
  if (typeof value !== 'string') {
    return undefined;
  }
  const normalized = value.trim();
  return normalized.length > 0 ? normalized : undefined;
}

export function arrayOfObjects(
  value: HarmonyLooseValue | Array<HarmonyLooseValue>
): HarmonyLooseObject[] {
  if (!Array.isArray(value)) {
    return [];
  }
  const result: HarmonyLooseObject[] = [];
  value.forEach((item: HarmonyLooseValue) => {
    if (typeof item === 'object' && item !== null && !Array.isArray(item)) {
      result.push(item as HarmonyLooseObject);
    }
  });
  return result;
}

export function safeObject(value: HarmonyLooseValue): HarmonyLooseObject | undefined {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) {
    return undefined;
  }
  return value as HarmonyLooseObject;
}

export function safeStringArray(
  value: HarmonyLooseValue | Array<HarmonyLooseValue>
): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  const result: string[] = [];
  value.forEach((item: HarmonyLooseValue) => {
    const nextValue = cleanString(item);
    if (nextValue !== undefined) {
      result.push(nextValue);
    }
  });
  return result;
}

export function makeSpeakerSummaries(items: HarmonyLooseObject[]): HarmonySpeakerSummary[] {
  const result: HarmonySpeakerSummary[] = [];
  items.forEach((item: HarmonyLooseObject) => {
    const summary = cleanString(item.Summary) || cleanString(item.summary);
    if (!summary) {
      return;
    }
    const nextItem: HarmonySpeakerSummary = {
      name: cleanString(item.Name) || cleanString(item.name),
      summary
    };
    result.push(nextItem);
  });
  return result;
}

export function makeQuestionAnswers(items: HarmonyLooseObject[]): HarmonyQuestionAnswer[] {
  const result: HarmonyQuestionAnswer[] = [];
  items.forEach((item: HarmonyLooseObject) => {
    const question = cleanString(item.Question) || cleanString(item.question);
    const answer = cleanString(item.Answer) || cleanString(item.answer);
    if (!question || !answer) {
      return;
    }
    const nextItem: HarmonyQuestionAnswer = {
      question,
      answer
    };
    result.push(nextItem);
  });
  return result;
}
