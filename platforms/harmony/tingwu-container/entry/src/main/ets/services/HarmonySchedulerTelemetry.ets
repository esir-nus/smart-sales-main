import {
  HarmonySchedulerScenarioKind,
  HarmonySchedulerTraceEvent,
  HarmonySchedulerTraceStage
} from '../model/HarmonySchedulerModels';

const MAX_TRACE_EVENTS = 48;

class HarmonyEmptyMetadata {}

const EMPTY_METADATA: Object = new HarmonyEmptyMetadata();

class HarmonySchedulerTelemetry {
  createTraceId(scenario: HarmonySchedulerScenarioKind): string {
    return `${scenario}_${Date.now()}`;
  }

  createEvent(
    traceId: string,
    stage: HarmonySchedulerTraceStage,
    message: string,
    metadata: Object = EMPTY_METADATA
  ): HarmonySchedulerTraceEvent {
    const event: HarmonySchedulerTraceEvent = {
      traceId,
      stage,
      message,
      metadata,
      occurredAt: Date.now()
    };
    console.info(`[HarmonyScheduler|${stage}] ${message} ${JSON.stringify(metadata)}`);
    return event;
  }

  trim(events: HarmonySchedulerTraceEvent[]): HarmonySchedulerTraceEvent[] {
    return events.slice(0, MAX_TRACE_EVENTS);
  }
}

export const harmonySchedulerTelemetry = new HarmonySchedulerTelemetry();
