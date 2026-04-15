import {
  HarmonyUiTingwuAdapterSnapshot
} from '../model/HarmonyUiModels';

class HarmonyUiTingwuSeedRepository {
  loadSnapshot(): HarmonyUiTingwuAdapterSnapshot {
    return {
      items: [
        {
          id: 'tingwu-completed-01',
          displayName: 'Customer interview.m4a',
          status: 'COMPLETED',
          progressPercent: 100,
          summary: 'Transcript, summary, and chapters are present in the repository-shaped adapter input.',
          transcriptPreview: 'Key objections and follow-up commitments are already visible from the Tingwu artifact preview.',
          keywords: ['Objections', 'Follow-up']
        },
        {
          id: 'tingwu-processing-01',
          displayName: 'Weekly pipeline recap.m4a',
          status: 'PROCESSING',
          progressPercent: 46,
          summary: 'Polling is still active, so the page keeps the incomplete state visible.',
          keywords: ['Upload']
        }
      ],
      artifactsById: {
        'tingwu-completed-01': {
          hasTranscript: true,
          hasSummary: true,
          hasChapters: true
        },
        'tingwu-processing-01': {
          hasTranscript: false,
          hasSummary: false,
          hasChapters: false
        }
      },
      configMissingKeys: [],
      isLoading: false,
      isBusy: false,
      busyMessage: '',
      lastErrorMessage: undefined
    };
  }
}

export const harmonyUiTingwuSeedRepository = new HarmonyUiTingwuSeedRepository();
