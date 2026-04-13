export enum HarmonyUiPageId {
  SHELL = 'HUI-01',
  TINGWU = 'HUI-02',
  SCHEDULER = 'HUI-03'
}

export interface HarmonyUiPageCard {
  id: HarmonyUiPageId;
  title: string;
  summary: string;
  statusLabel: string;
}

export interface HarmonyUiShellState {
  pageId: HarmonyUiPageId;
  title: string;
  summary: string;
  routeCards: HarmonyUiPageCard[];
}

export interface HarmonyUiArtifactCard {
  title: string;
  statusLabel: string;
  summary: string;
  tags: string[];
}

export type HarmonyUiTingwuJobStatus =
  | 'IMPORTED'
  | 'UPLOADING'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED';

export interface HarmonyUiTingwuAdapterItem {
  id: string;
  displayName: string;
  status: HarmonyUiTingwuJobStatus;
  progressPercent: number;
  summary?: string;
  transcriptPreview?: string;
  lastErrorMessage?: string;
  keywords: string[];
}

export interface HarmonyUiTingwuAdapterArtifacts {
  hasTranscript: boolean;
  hasSummary: boolean;
  hasChapters: boolean;
}

export interface HarmonyUiTingwuAdapterSnapshot {
  items: HarmonyUiTingwuAdapterItem[];
  artifactsById: Record<string, HarmonyUiTingwuAdapterArtifacts>;
  configMissingKeys: string[];
  isLoading: boolean;
  isBusy: boolean;
  busyMessage: string;
  lastErrorMessage?: string;
}

export interface HarmonyUiTingwuState {
  pageId: HarmonyUiPageId;
  title: string;
  summary: string;
  sourceLabel: string;
  notes: string[];
  items: HarmonyUiArtifactCard[];
}

export interface HarmonyUiSchedulerPreviewCard {
  title: string;
  summary: string;
  dueLabel: string;
  stateLabel: string;
}

export interface HarmonyUiSchedulerPreviewState {
  pageId: HarmonyUiPageId;
  title: string;
  summary: string;
  gateLabel: string;
  cards: HarmonyUiSchedulerPreviewCard[];
}
