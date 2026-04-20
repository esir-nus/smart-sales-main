// File: platforms/harmony/smartsales-app/entry/src/main/ets/domain/Audio.ets
// Module: smartsales-app/entry
// Summary: 音频领域模型 — 翻译自 tingwu-container HarmonyTingwuModels.ets
// Author: created on 2026-04-16
// 翻译来源: platforms/harmony/tingwu-container/entry/src/main/ets/model/HarmonyTingwuModels.ets

// 音频任务状态枚举
export enum AudioJobStatus {
  IMPORTED = 'IMPORTED',
  UPLOADING = 'UPLOADING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

// 音频失败原因枚举
export enum AudioErrorReason {
  AUTH_FAILED = 'AUTH_FAILED',
  NETWORK_FAILED = 'NETWORK_FAILED',
  POLL_TIMEOUT = 'POLL_TIMEOUT',
  TASK_EXPIRED = 'TASK_EXPIRED',
  UPLOAD_FAILED = 'UPLOAD_FAILED',
  UNKNOWN = 'UNKNOWN'
}

// 听悟章节时间片段
export interface ChapterSegment {
  title: string;
  startMs: number;
  endMs: number;
  summary?: string;
}

// Provider 原始载荷
export interface AudioProviderPayloads {
  transcription: string;
  autoChapters: string;
  summarization: string;
  meetingAssistance: string;
  identityRecognition: string;
}

// 带失败原因的错误对象，供服务层向仓库层传递可判定错误
export class AudioPipelineError extends Error {
  reason: AudioErrorReason;
  detailCode?: string;
  statusCode?: number;

  constructor(
    reason: AudioErrorReason,
    message: string,
    detailCode?: string,
    statusCode?: number
  ) {
    super(message);
    this.name = 'AudioPipelineError';
    this.reason = reason;
    this.detailCode = detailCode;
    this.statusCode = statusCode;
  }
}

// 音频项目数据模型（21 字段，与容器版本语义一致）
export interface AudioItem {
  id: string;
  displayName: string;
  status: AudioJobStatus;
  progressPercent: number;
  summary: string;
  transcriptPreview: string;
  lastErrorMessage: string;
  lastErrorReason?: AudioErrorReason;
  keywords: string[];
  sourceUri: string;
  localFileName: string;
  localPath: string;
  extension: string;
  sizeBytes: number;
  importedAt: number;
  updatedAt: number;
  completedAt: number;
  objectKey: string;
  publicUrl: string;
  jobId: string;
}

// 听悟处理产物（13 字段）
export interface AudioArtifacts {
  transcriptMarkdown: string;
  smartSummary: string;
  smartSummaryStructured?: SmartSummary;
  chapters: ChapterSegment[];
  outputMp3Path: string;
  outputMp4Path: string;
  outputThumbnailPath: string;
  outputSpectrumPath: string;
  resultLinks: Record<string, string>;
  transcriptionUrl: string;
  autoChaptersUrl: string;
  customPromptUrl: string;
  diarizedSegments: DiarizedSegment[];
  speakerLabels: Record<string, string>;
  speakerSummaries: SpeakerSummary[];
  questionAnswers: QuestionAnswer[];
  keywords: string[];
  providerPayloads: AudioProviderPayloads;
}

// 说话人分段
export interface DiarizedSegment {
  speakerId?: string;
  speakerIndex: number;
  startMs: number;
  endMs: number;
  text: string;
}

// 智能摘要结构（含说话人摘要和问答对，与 HarmonySmartSummary 语义一致）
export interface SmartSummary {
  summary?: string;
  keyPoints: string[];
  speakerSummaries: SpeakerSummary[];
  questionAnswers: QuestionAnswer[];
}

// 说话人摘要
export interface SpeakerSummary {
  name?: string;
  summary: string;
}

// 问答对
export interface QuestionAnswer {
  question: string;
  answer: string;
}

// 音频仓库快照
export interface AudioSnapshot {
  items: AudioItem[];
  configMissingKeys: string[];
  isLoading: boolean;
  isBusy: boolean;
  busyMessage: string;
  lastErrorMessage?: string;
}

// 判断是否为处理中状态
export function isProcessingStatus(status: AudioJobStatus): boolean {
  return status === AudioJobStatus.UPLOADING || status === AudioJobStatus.PROCESSING;
}

// 创建空快照
export function createEmptyAudioSnapshot(): AudioSnapshot {
  return {
    items: [],
    configMissingKeys: [],
    isLoading: true,
    isBusy: false,
    busyMessage: ''
  };
}
