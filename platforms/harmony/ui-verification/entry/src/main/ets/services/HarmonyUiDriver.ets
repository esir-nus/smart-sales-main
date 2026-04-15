import {
  HarmonyUiPageCard,
  HarmonyUiPageId,
  HarmonyUiSchedulerPreviewState,
  HarmonyUiShellState,
  HarmonyUiTingwuState
} from '../model/HarmonyUiModels';
import { harmonyUiTingwuPageAdapter } from './HarmonyUiTingwuPageAdapter';
import { harmonyUiTingwuSeedRepository } from './HarmonyUiTingwuSeedRepository';

class HarmonyUiDriver {
  loadShellState(): HarmonyUiShellState {
    const routeCards: HarmonyUiPageCard[] = [
      {
        id: HarmonyUiPageId.SHELL,
        title: 'HUI-01 Shell Gate',
        summary: 'Owns the internal route scaffold and the honesty boundary for this package.',
        statusLabel: 'Active'
      },
      {
        id: HarmonyUiPageId.TINGWU,
        title: 'HUI-02 Tingwu Rewrite',
        summary: 'Rewrites the Tingwu surface through an adapter-backed page state instead of inline mock cards.',
        statusLabel: 'Active'
      },
      {
        id: HarmonyUiPageId.SCHEDULER,
        title: 'HUI-03 Scheduler Preview',
        summary: 'Hidden scheduler preview states stay internal until later docking is approved.',
        statusLabel: 'Internal'
      }
    ];

    return {
      pageId: HarmonyUiPageId.SHELL,
      title: 'Internal Harmony UI shell',
      summary: 'Separate ArkUI package for page-by-page native rewrite and later device checks.',
      routeCards
    };
  }

  loadTingwuState(): HarmonyUiTingwuState {
    return harmonyUiTingwuPageAdapter.buildState(
      harmonyUiTingwuSeedRepository.loadSnapshot()
    );
  }

  loadSchedulerPreviewState(): HarmonyUiSchedulerPreviewState {
    return {
      pageId: HarmonyUiPageId.SCHEDULER,
      title: 'Hidden scheduler preview',
      summary: 'Mock-only ArkUI preview of scheduler cards and summary states. This page must stay internal.',
      gateLabel: 'Internal gate required',
      cards: [
        {
          title: 'Meet the client tomorrow morning',
          summary: 'Exact-create preview card with visible due time and calm confirmation language.',
          dueLabel: '2026-04-12 09:00',
          stateLabel: 'Uni-A preview'
        },
        {
          title: 'Follow up on the quote next week',
          summary: 'Vague-create preview card that keeps timing unresolved instead of fabricating an exact due time.',
          dueLabel: 'Needs time',
          stateLabel: 'Uni-B preview'
        }
      ]
    };
  }
}

export const harmonyUiDriver = new HarmonyUiDriver();
