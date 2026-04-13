export enum HarmonySchedulerScenarioKind {
  EXACT_CREATE = 'EXACT_CREATE',
  VAGUE_CREATE = 'VAGUE_CREATE',
  CONFLICT_CREATE = 'CONFLICT_CREATE',
  RESCHEDULE = 'RESCHEDULE',
  NULL_FAIL = 'NULL_FAIL'
}

export enum HarmonySchedulerIngressKind {
  OPERATOR_SCENARIO = 'OPERATOR_SCENARIO'
}

export enum HarmonySchedulerCommandKind {
  EXACT_CREATE = 'EXACT_CREATE',
  VAGUE_CREATE = 'VAGUE_CREATE',
  RESCHEDULE = 'RESCHEDULE',
  NO_MUTATION = 'NO_MUTATION'
}

export enum HarmonySchedulerTaskTimingKind {
  EXACT = 'EXACT',
  NEEDS_TIME = 'NEEDS_TIME'
}

export enum HarmonySchedulerTaskState {
  ACTIVE = 'ACTIVE',
  NEEDS_TIME = 'NEEDS_TIME',
  SUPERSEDED = 'SUPERSEDED'
}

export enum HarmonySchedulerReminderState {
  DEFERRED = 'DEFERRED',
  NOT_ARMED = 'NOT_ARMED'
}

export enum HarmonySchedulerPathBState {
  DEFERRED = 'DEFERRED',
  NOT_ELIGIBLE = 'NOT_ELIGIBLE'
}

export enum HarmonySchedulerTraceStage {
  INGRESS_ACCEPTED = 'INGRESS_ACCEPTED',
  COMMAND_CLASSIFIED = 'COMMAND_CLASSIFIED',
  TARGET_RESOLVED = 'TARGET_RESOLVED',
  CONFLICT_MARKED = 'CONFLICT_MARKED',
  PATH_A_COMMITTED = 'PATH_A_COMMITTED',
  SAFE_FAILED = 'SAFE_FAILED',
  SNAPSHOT_PERSISTED = 'SNAPSHOT_PERSISTED'
}

export interface HarmonySchedulerTaskRecord {
  rootGuid: string;
  revisionNo: number;
  lineageGuid: string;
  title: string;
  sourceInput: string;
  commandKind: HarmonySchedulerCommandKind;
  timingKind: HarmonySchedulerTaskTimingKind;
  urgencyLabel: string;
  state: HarmonySchedulerTaskState;
  reminderState: HarmonySchedulerReminderState;
  pathBState: HarmonySchedulerPathBState;
  dueAtIso?: string;
  conflictDetected: boolean;
  createdAt: number;
  updatedAt: number;
  supersededAt?: number;
}

export interface HarmonySchedulerCommandEnvelope {
  traceId: string;
  scenario: HarmonySchedulerScenarioKind;
  sourceInput: string;
  ingressKind: HarmonySchedulerIngressKind;
  ingressOwner: string;
  ownerChain: string[];
  rescheduleTargetQuery?: string;
  rescheduleNewDueAtIso?: string;
}

export interface HarmonySchedulerTraceEvent {
  traceId: string;
  stage: HarmonySchedulerTraceStage;
  message: string;
  metadata: Object;
  occurredAt: number;
}

export interface HarmonySchedulerPathACommitRecord {
  traceId: string;
  rootGuid: string;
  lineageGuid: string;
  commandKind: HarmonySchedulerCommandKind;
  taskState: HarmonySchedulerTaskState;
  committedAt: number;
  ownerChain: string[];
}

export interface HarmonySchedulerSnapshot {
  tasks: HarmonySchedulerTaskRecord[];
  events: HarmonySchedulerTraceEvent[];
  isLoading: boolean;
  isBusy: boolean;
  busyMessage: string;
  lastInput: string;
  lastScenario?: HarmonySchedulerScenarioKind;
  lastIngressKind?: HarmonySchedulerIngressKind;
  lastIngressOwner: string;
  ownerChain: string[];
  lastCommandKind: HarmonySchedulerCommandKind;
  lastSafeFailReason?: string;
  lastPathACommit?: HarmonySchedulerPathACommitRecord;
  lastErrorMessage?: string;
}

export function createEmptyHarmonySchedulerSnapshot(): HarmonySchedulerSnapshot {
  return {
    tasks: [],
    events: [],
    isLoading: true,
    isBusy: false,
    busyMessage: '',
    lastInput: '',
    lastIngressOwner: '',
    ownerChain: [],
    lastCommandKind: HarmonySchedulerCommandKind.NO_MUTATION
  };
}
