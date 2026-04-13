import {
  HarmonySchedulerCommandEnvelope,
  HarmonySchedulerIngressKind,
  HarmonySchedulerScenarioKind
} from '../model/HarmonySchedulerModels';
import { harmonySchedulerRepository } from './HarmonySchedulerRepository';
import { harmonySchedulerTelemetry } from './HarmonySchedulerTelemetry';

const OPERATOR_OWNER_CHAIN: string[] = [
  'HarmonySchedulerIngressCoordinator',
  'HarmonySchedulerRepository'
];

class HarmonySchedulerIngressCoordinator {
  async runOperatorScenario(scenario: HarmonySchedulerScenarioKind): Promise<void> {
    const envelope: HarmonySchedulerCommandEnvelope = {
      traceId: harmonySchedulerTelemetry.createTraceId(scenario),
      scenario,
      sourceInput: operatorScenarioInput(scenario),
      ingressKind: HarmonySchedulerIngressKind.OPERATOR_SCENARIO,
      ingressOwner: 'HarmonySchedulerIngressCoordinator',
      ownerChain: [...OPERATOR_OWNER_CHAIN],
      rescheduleTargetQuery: scenario === HarmonySchedulerScenarioKind.RESCHEDULE
        ? 'Meet the client tomorrow morning'
        : undefined,
      rescheduleNewDueAtIso: scenario === HarmonySchedulerScenarioKind.RESCHEDULE
        ? '2026-04-11T11:00:00+08:00'
        : undefined
    };
    await harmonySchedulerRepository.executeEnvelope(envelope);
  }
}

function operatorScenarioInput(scenario: HarmonySchedulerScenarioKind): string {
  switch (scenario) {
    case HarmonySchedulerScenarioKind.EXACT_CREATE:
      return '明天上午九点和客户开会';
    case HarmonySchedulerScenarioKind.VAGUE_CREATE:
      return '下周提醒我跟进报价';
    case HarmonySchedulerScenarioKind.CONFLICT_CREATE:
      return '后天下午三点提交合同';
    case HarmonySchedulerScenarioKind.RESCHEDULE:
      return '把 Meet the client tomorrow morning 改到明天上午十一点';
    case HarmonySchedulerScenarioKind.NULL_FAIL:
      return '';
    default:
      return '';
  }
}

export const harmonySchedulerIngressCoordinator = new HarmonySchedulerIngressCoordinator();
