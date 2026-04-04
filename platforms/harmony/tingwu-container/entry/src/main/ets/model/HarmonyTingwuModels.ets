export enum HarmonyJobStatus {
  IMPORTED = 'IMPORTED',
  UPLOADING = 'UPLOADING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

export interface HarmonyResultLink {
  label: string;
  url: string;
}

export interface HarmonyChapter {
  title: string;
  startMs: number;
  endMs: number;
  summary?: string;
}

export interface HarmonySpeakerSummary {
  name?: string;
  summary: string;
}

export interface HarmonyQuestionAnswer {
  question: string;
  answer: string;
}

export interface HarmonySmartSummary {
  summary?: string;
  keyPoints: string[];
  speakerSummaries: HarmonySpeakerSummary[];
  questionAnswers: HarmonyQuestionAnswer[];
}

export interface HarmonyDiarizedSegment {
  speakerId?: string;
  speakerIndex: number;
  startMs: number;
  endMs: number;
  text: string;
}

export interface HarmonyTingwuArtifacts {
  outputMp3Path?: string;
  outputMp4Path?: string;
  outputThumbnailPath?: string;
  outputSpectrumPath?: string;
  resultLinks: HarmonyResultLink[];
  transcriptionUrl?: string;
  autoChaptersUrl?: string;
  customPromptUrl?: string;
  extraResultUrls: Record<string, string>;
  meetingAssistanceRaw?: string;
  transcriptMarkdown?: string;
  chapters: HarmonyChapter[];
  smartSummary?: HarmonySmartSummary;
  diarizedSegments: HarmonyDiarizedSegment[];
  speakerLabels: Record<string, string>;
  keywords: string[];
  providerPayloads: Record<string, string>;
}

export interface HarmonyTingwuAudioItem {
  id: string;
  displayName: string;
  sourceUri: string;
  localFileName: string;
  localPath: string;
  extension: string;
  sizeBytes: number;
  importedAt: number;
  updatedAt: number;
  progressPercent: number;
  status: HarmonyJobStatus;
  objectKey?: string;
  publicUrl?: string;
  jobId?: string;
  summary?: string;
  transcriptPreview?: string;
  lastErrorMessage?: string;
  completedAt?: number;
  keywords: string[];
}

export interface HarmonyContainerSnapshot {
  items: HarmonyTingwuAudioItem[];
  configMissingKeys: string[];
  isLoading: boolean;
  isBusy: boolean;
  busyMessage: string;
  lastErrorMessage?: string;
}

export function createEmptySnapshot(): HarmonyContainerSnapshot {
  return {
    items: [],
    configMissingKeys: [],
    isLoading: true,
    isBusy: false,
    busyMessage: ''
  };
}

export function isProcessingStatus(status: HarmonyJobStatus): boolean {
  return status === HarmonyJobStatus.UPLOADING || status === HarmonyJobStatus.PROCESSING;
}
