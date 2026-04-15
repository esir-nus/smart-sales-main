import {
  HarmonyUiArtifactCard,
  HarmonyUiPageId,
  HarmonyUiTingwuAdapterArtifacts,
  HarmonyUiTingwuAdapterItem,
  HarmonyUiTingwuAdapterSnapshot,
  HarmonyUiTingwuState
} from '../model/HarmonyUiModels';

class HarmonyUiTingwuPageAdapter {
  buildState(snapshot: HarmonyUiTingwuAdapterSnapshot): HarmonyUiTingwuState {
    return {
      pageId: HarmonyUiPageId.TINGWU,
      title: 'Tingwu page rewrite',
      summary: this.buildSummary(snapshot),
      sourceLabel: 'Adapter-driven Tingwu seed',
      notes: this.buildNotes(snapshot),
      items: snapshot.items.map((item: HarmonyUiTingwuAdapterItem) => {
        return this.buildCard(item, snapshot.artifactsById[item.id]);
      })
    };
  }

  private buildSummary(snapshot: HarmonyUiTingwuAdapterSnapshot): string {
    if (snapshot.configMissingKeys.length > 0) {
      return `Runtime config still blocks Tingwu bind: ${snapshot.configMissingKeys.join(', ')}`;
    }
    if (snapshot.isLoading) {
      return 'Page adapter is waiting for a real Tingwu snapshot instead of rendering inline mock cards.';
    }
    if (snapshot.isBusy && snapshot.busyMessage.length > 0) {
      return snapshot.busyMessage;
    }
    if (snapshot.lastErrorMessage && snapshot.lastErrorMessage.length > 0) {
      return snapshot.lastErrorMessage;
    }
    return 'Card structure now comes from a Tingwu page adapter seeded with repository-shaped state, not from handwritten mock cards.';
  }

  private buildNotes(snapshot: HarmonyUiTingwuAdapterSnapshot): string[] {
    const notes: string[] = [
      'HUI-02 now maps repository-shaped Tingwu state through a dedicated page adapter.',
      'Scheduler preview remains hidden and does not inherit this adapter lane.'
    ];

    if (snapshot.configMissingKeys.length > 0) {
      notes.push(`Config keys still missing: ${snapshot.configMissingKeys.join(', ')}`);
    }
    if (snapshot.lastErrorMessage && snapshot.lastErrorMessage.length > 0) {
      notes.push(`Last repository error: ${snapshot.lastErrorMessage}`);
    }
    return notes;
  }

  private buildCard(
    item: HarmonyUiTingwuAdapterItem,
    artifacts?: HarmonyUiTingwuAdapterArtifacts
  ): HarmonyUiArtifactCard {
    return {
      title: item.displayName,
      statusLabel: this.buildStatusLabel(item),
      summary: this.buildItemSummary(item),
      tags: this.buildTags(item, artifacts)
    };
  }

  private buildStatusLabel(item: HarmonyUiTingwuAdapterItem): string {
    if (item.status === 'UPLOADING' || item.status === 'PROCESSING') {
      return `${item.status} ${item.progressPercent}%`;
    }
    return item.status;
  }

  private buildItemSummary(item: HarmonyUiTingwuAdapterItem): string {
    if (item.lastErrorMessage && item.lastErrorMessage.length > 0) {
      return item.lastErrorMessage;
    }
    if (item.transcriptPreview && item.transcriptPreview.length > 0) {
      return item.transcriptPreview;
    }
    if (item.summary && item.summary.length > 0) {
      return item.summary;
    }
    switch (item.status) {
      case 'IMPORTED':
        return 'Imported locally and ready for Tingwu submission once the real bind is allowed.';
      case 'UPLOADING':
        return 'Upload stage is active and the UI keeps progress visible without inventing completion.';
      case 'PROCESSING':
        return 'Tingwu polling remains in progress and the page stays honest about incomplete artifacts.';
      case 'FAILED':
        return 'The last Tingwu attempt failed and the page keeps the failure state visible.';
      default:
        return 'Available artifacts are ready for native ArkUI verification.';
    }
  }

  private buildTags(
    item: HarmonyUiTingwuAdapterItem,
    artifacts?: HarmonyUiTingwuAdapterArtifacts
  ): string[] {
    const tags: string[] = [];
    if (artifacts?.hasTranscript) {
      tags.push('Transcript');
    }
    if (artifacts?.hasSummary) {
      tags.push('Summary');
    }
    if (artifacts?.hasChapters) {
      tags.push('Chapters');
    }
    if (item.status === 'UPLOADING') {
      tags.push('Upload');
    }
    if (item.status === 'PROCESSING') {
      tags.push('Polling');
    }
    if (item.status === 'FAILED') {
      tags.push('Retry');
    }
    if (item.status === 'IMPORTED') {
      tags.push('Ready');
    }
    item.keywords.forEach((keyword: string) => {
      if (tags.length < 5 && !tags.includes(keyword)) {
        tags.push(keyword);
      }
    });
    return tags;
  }
}

export const harmonyUiTingwuPageAdapter = new HarmonyUiTingwuPageAdapter();
