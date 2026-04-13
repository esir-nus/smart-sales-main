import { common } from '@kit.AbilityKit';
import {
  createEmptyHarmonySchedulerSnapshot,
  HarmonySchedulerCommandEnvelope,
  HarmonySchedulerCommandKind,
  HarmonySchedulerPathACommitRecord,
  HarmonySchedulerPathBState,
  HarmonySchedulerReminderState,
  HarmonySchedulerSnapshot,
  HarmonySchedulerTaskRecord,
  HarmonySchedulerTaskState,
  HarmonySchedulerTaskTimingKind,
  HarmonySchedulerTraceEvent,
  HarmonySchedulerTraceStage
} from '../model/HarmonySchedulerModels';
import { harmonySchedulerStore } from './HarmonySchedulerStore';
import { harmonySchedulerTelemetry } from './HarmonySchedulerTelemetry';

type HarmonySchedulerListener = (snapshot: HarmonySchedulerSnapshot) => void;
type HarmonySchedulerError = Error | Object | string | number | boolean | undefined | null;
type HarmonySchedulerMetadata = Object;

interface HarmonyIngressMetadata {
  scenario: string;
  inputLength: string;
  ingressOwner: string;
}

interface HarmonyCommandMetadata {
  commandKind: string;
  conflictDetected?: string;
}

interface HarmonyCommitSummaryMetadata {
  committedLineageGuid: string;
  taskState: string;
  ownerChain: string;
}

interface HarmonyReasonMetadata {
  reason: string;
  result?: string;
}

interface HarmonyTargetMetadata {
  rootGuid: string;
  lineageGuid: string;
  targetQuery: string;
}

interface HarmonyConflictMetadata {
  rootGuid: string;
  dueAtIso: string;
}

interface HarmonyRescheduleCommitMetadata {
  rootGuid: string;
  lineageGuid: string;
  replaces: string;
  ownerChain: string;
  targetQuery: string;
}

interface HarmonyPathCommitMetadata {
  rootGuid: string;
  lineageGuid: string;
  ownerChain: string;
}

interface HarmonySchedulerPendingCommit {
  rootGuid: string;
  lineageGuid: string;
  commandKind: HarmonySchedulerCommandKind;
  taskState: HarmonySchedulerTaskState;
  message: string;
}

class HarmonySchedulerRepository {
  private context?: common.UIAbilityContext;
  private tasks: HarmonySchedulerTaskRecord[] = [];
  private events: HarmonySchedulerTraceEvent[] = [];
  private listeners: HarmonySchedulerListener[] = [];
  private snapshot: HarmonySchedulerSnapshot = createEmptyHarmonySchedulerSnapshot();

  async initialize(context: common.UIAbilityContext): Promise<void> {
    if (this.context?.filesDir === context.filesDir && !this.snapshot.isLoading) {
      return;
    }
    this.context = context;
    this.tasks = harmonySchedulerStore.loadTasks(context)
      .sort((left: HarmonySchedulerTaskRecord, right: HarmonySchedulerTaskRecord) => right.updatedAt - left.updatedAt);
    this.events = harmonySchedulerStore.loadEvents(context)
      .sort((left: HarmonySchedulerTraceEvent, right: HarmonySchedulerTraceEvent) => right.occurredAt - left.occurredAt);

    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.tasks = this.sortedTasks();
    nextSnapshot.events = this.sortedEvents();
    nextSnapshot.isLoading = false;
    nextSnapshot.isBusy = false;
    nextSnapshot.busyMessage = '';
    this.snapshot = nextSnapshot;
    this.emit();
  }

  getSnapshot(): HarmonySchedulerSnapshot {
    return this.snapshot;
  }

  subscribe(listener: HarmonySchedulerListener): () => void {
    this.listeners.push(listener);
    listener(this.snapshot);
    return () => {
      this.listeners = this.listeners.filter((candidate: HarmonySchedulerListener) => candidate !== listener);
    };
  }

  async executeEnvelope(envelope: HarmonySchedulerCommandEnvelope): Promise<void> {
    this.setBusy(true, `Running ${envelope.scenario} verification...`);

    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.lastInput = envelope.sourceInput;
    nextSnapshot.lastScenario = envelope.scenario;
    nextSnapshot.lastIngressKind = envelope.ingressKind;
    nextSnapshot.lastIngressOwner = envelope.ingressOwner;
    nextSnapshot.ownerChain = this.copyStringArray(envelope.ownerChain);
    nextSnapshot.lastErrorMessage = undefined;
    nextSnapshot.lastSafeFailReason = undefined;
    this.snapshot = nextSnapshot;
    this.emit();

    try {
      const ingressMetadata: HarmonyIngressMetadata = {
        scenario: envelope.scenario,
        inputLength: `${envelope.sourceInput.length}`,
        ingressOwner: envelope.ingressOwner
      };
      this.record(
        envelope.traceId,
        HarmonySchedulerTraceStage.INGRESS_ACCEPTED,
        'Harmony scheduler backend ingress accepted',
        ingressMetadata
      );

      switch (envelope.scenario) {
        case 'EXACT_CREATE':
          this.handleExactCreate(envelope, false);
          break;
        case 'VAGUE_CREATE':
          this.handleVagueCreate(envelope);
          break;
        case 'CONFLICT_CREATE':
          this.handleExactCreate(envelope, true);
          break;
        case 'RESCHEDULE':
          this.handleReschedule(envelope);
          break;
        case 'NULL_FAIL':
          this.handleNullFail(envelope);
          break;
        default:
          this.handleNullFail(envelope);
          break;
      }
    } catch (error) {
      const message = this.errorMessage(error);
      const failedSnapshot = this.copySnapshot(this.snapshot);
      failedSnapshot.lastErrorMessage = message;
      this.snapshot = failedSnapshot;
      this.emit();
      throw new Error(message);
    } finally {
      this.setBusy(false, '');
    }
  }

  async resetSandbox(): Promise<void> {
    const context = this.requireContext();
    harmonySchedulerStore.clear(context);
    this.tasks = [];
    this.events = [];
    const nextSnapshot = createEmptyHarmonySchedulerSnapshot();
    nextSnapshot.isLoading = false;
    this.snapshot = nextSnapshot;
    this.emit();
  }

  private handleExactCreate(envelope: HarmonySchedulerCommandEnvelope, conflictDetected: boolean): void {
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.lastCommandKind = HarmonySchedulerCommandKind.EXACT_CREATE;
    this.snapshot = nextSnapshot;

    const classifyMetadata: HarmonyCommandMetadata = {
      commandKind: HarmonySchedulerCommandKind.EXACT_CREATE,
      conflictDetected: `${conflictDetected}`
    };
    this.record(
      envelope.traceId,
      HarmonySchedulerTraceStage.COMMAND_CLASSIFIED,
      'Scheduler command classified',
      classifyMetadata
    );

    const rootGuid = envelope.traceId;
    const task: HarmonySchedulerTaskRecord = {
      rootGuid,
      revisionNo: 0,
      lineageGuid: this.buildLineageGuid(rootGuid, 0),
      title: conflictDetected ? 'Submit Harmony scheduler prototype' : 'Meet the client tomorrow morning',
      sourceInput: envelope.sourceInput,
      commandKind: HarmonySchedulerCommandKind.EXACT_CREATE,
      timingKind: HarmonySchedulerTaskTimingKind.EXACT,
      urgencyLabel: conflictDetected ? 'L2_IMPORTANT' : 'L3_NORMAL',
      state: HarmonySchedulerTaskState.ACTIVE,
      reminderState: HarmonySchedulerReminderState.DEFERRED,
      pathBState: HarmonySchedulerPathBState.DEFERRED,
      dueAtIso: conflictDetected ? '2026-04-10T15:00:00+08:00' : '2026-04-10T09:00:00+08:00',
      conflictDetected,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    if (conflictDetected) {
      const conflictMetadata: HarmonyConflictMetadata = {
        rootGuid,
        dueAtIso: task.dueAtIso || ''
      };
      this.record(
        envelope.traceId,
        HarmonySchedulerTraceStage.CONFLICT_MARKED,
        'Conflict-visible exact task branch selected',
        conflictMetadata
      );
    }

    this.tasks = this.prependTask(task, this.tasks);

    const persistedMetadata: HarmonyCommitSummaryMetadata = {
      committedLineageGuid: task.lineageGuid,
      taskState: task.state,
      ownerChain: this.snapshot.ownerChain.join(' -> ')
    };
    const pendingCommit: HarmonySchedulerPendingCommit = {
      rootGuid,
      lineageGuid: task.lineageGuid,
      commandKind: task.commandKind,
      taskState: task.state,
      message: conflictDetected
        ? 'Conflict-visible Path A commit emitted after local persistence'
        : 'Harmony Path A commit emitted after local persistence'
    };
    this.persist(envelope.traceId, persistedMetadata, pendingCommit);
  }

  private handleVagueCreate(envelope: HarmonySchedulerCommandEnvelope): void {
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.lastCommandKind = HarmonySchedulerCommandKind.VAGUE_CREATE;
    this.snapshot = nextSnapshot;

    const classifyMetadata: HarmonyCommandMetadata = {
      commandKind: HarmonySchedulerCommandKind.VAGUE_CREATE
    };
    this.record(
      envelope.traceId,
      HarmonySchedulerTraceStage.COMMAND_CLASSIFIED,
      'Scheduler command classified',
      classifyMetadata
    );

    const rootGuid = envelope.traceId;
    const task: HarmonySchedulerTaskRecord = {
      rootGuid,
      revisionNo: 0,
      lineageGuid: this.buildLineageGuid(rootGuid, 0),
      title: 'Follow up on the quote next week',
      sourceInput: envelope.sourceInput,
      commandKind: HarmonySchedulerCommandKind.VAGUE_CREATE,
      timingKind: HarmonySchedulerTaskTimingKind.NEEDS_TIME,
      urgencyLabel: 'L3_NORMAL',
      state: HarmonySchedulerTaskState.NEEDS_TIME,
      reminderState: HarmonySchedulerReminderState.NOT_ARMED,
      pathBState: HarmonySchedulerPathBState.NOT_ELIGIBLE,
      conflictDetected: false,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };

    this.tasks = this.prependTask(task, this.tasks);

    const persistedMetadata: HarmonyCommitSummaryMetadata = {
      committedLineageGuid: task.lineageGuid,
      taskState: task.state,
      ownerChain: this.snapshot.ownerChain.join(' -> ')
    };
    const pendingCommit: HarmonySchedulerPendingCommit = {
      rootGuid,
      lineageGuid: task.lineageGuid,
      commandKind: task.commandKind,
      taskState: task.state,
      message: 'Vague scheduler Path A commit emitted after local persistence'
    };
    this.persist(envelope.traceId, persistedMetadata, pendingCommit);
  }

  private handleReschedule(envelope: HarmonySchedulerCommandEnvelope): void {
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.lastCommandKind = HarmonySchedulerCommandKind.RESCHEDULE;
    this.snapshot = nextSnapshot;

    const classifyMetadata: HarmonyCommandMetadata = {
      commandKind: HarmonySchedulerCommandKind.RESCHEDULE
    };
    this.record(
      envelope.traceId,
      HarmonySchedulerTraceStage.COMMAND_CLASSIFIED,
      'Scheduler command classified',
      classifyMetadata
    );

    const targetQuery = envelope.rescheduleTargetQuery?.trim() || '';
    if (!targetQuery) {
      this.safeFailReschedule(envelope.traceId, 'TARGET_OMITTED', 'Reschedule safe-failed because the explicit target was omitted');
      return;
    }

    const newDueAtIso = envelope.rescheduleNewDueAtIso?.trim() || '';
    if (!this.isExactIsoTimestamp(newDueAtIso)) {
      this.safeFailReschedule(envelope.traceId, 'TIME_NOT_EXACT', 'Reschedule safe-failed because the new time is not exact');
      return;
    }

    const resolution = this.resolveExplicitRescheduleTarget(targetQuery);
    if (resolution.kind === 'no_match') {
      this.safeFailReschedule(envelope.traceId, 'TARGET_NO_MATCH', 'Reschedule safe-failed because no active exact task matched the explicit target');
      return;
    }
    if (resolution.kind === 'ambiguous') {
      this.safeFailReschedule(envelope.traceId, 'TARGET_AMBIGUOUS', 'Reschedule safe-failed because the explicit target matched multiple active exact tasks');
      return;
    }

    const target = resolution.task;
    const targetMetadata: HarmonyTargetMetadata = {
      rootGuid: target.rootGuid,
      lineageGuid: target.lineageGuid,
      targetQuery
    };
    this.record(
      envelope.traceId,
      HarmonySchedulerTraceStage.TARGET_RESOLVED,
      'Reschedule target resolved from local Harmony snapshot',
      targetMetadata
    );

    const revised = this.copyTask(target);
    revised.revisionNo = target.revisionNo + 1;
    revised.lineageGuid = this.buildLineageGuid(target.rootGuid, target.revisionNo + 1);
    revised.commandKind = HarmonySchedulerCommandKind.RESCHEDULE;
    revised.sourceInput = envelope.sourceInput;
    revised.timingKind = HarmonySchedulerTaskTimingKind.EXACT;
    revised.reminderState = HarmonySchedulerReminderState.DEFERRED;
    revised.pathBState = HarmonySchedulerPathBState.DEFERRED;
    revised.dueAtIso = newDueAtIso;
    revised.conflictDetected = false;
    revised.updatedAt = Date.now();

    const nextTasks: HarmonySchedulerTaskRecord[] = [];
    this.tasks.forEach((task: HarmonySchedulerTaskRecord) => {
      if (task.lineageGuid === target.lineageGuid) {
        const superseded = this.copyTask(task);
        superseded.state = HarmonySchedulerTaskState.SUPERSEDED;
        superseded.supersededAt = Date.now();
        superseded.updatedAt = Date.now();
        nextTasks.push(superseded);
      } else {
        nextTasks.push(this.copyTask(task));
      }
    });
    this.tasks = this.prependTask(revised, nextTasks);

    const persistedMetadata: HarmonyRescheduleCommitMetadata = {
      rootGuid: revised.rootGuid,
      lineageGuid: revised.lineageGuid,
      replaces: target.lineageGuid,
      ownerChain: this.snapshot.ownerChain.join(' -> '),
      targetQuery
    };
    const pendingCommit: HarmonySchedulerPendingCommit = {
      rootGuid: revised.rootGuid,
      lineageGuid: revised.lineageGuid,
      commandKind: revised.commandKind,
      taskState: revised.state,
      message: 'Reschedule replacement Path A commit emitted after local persistence'
    };
    this.persist(envelope.traceId, persistedMetadata, pendingCommit);
  }

  private safeFailReschedule(traceId: string, reason: string, message: string): void {
    const safeFailMetadata: HarmonyReasonMetadata = {
      reason
    };
    this.record(
      traceId,
      HarmonySchedulerTraceStage.SAFE_FAILED,
      message,
      safeFailMetadata
    );
    const safeFailSnapshot = this.copySnapshot(this.snapshot);
    safeFailSnapshot.lastSafeFailReason = reason;
    this.snapshot = safeFailSnapshot;
    const persistedMetadata: HarmonyReasonMetadata = {
      result: 'SAFE_FAILED',
      reason
    };
    this.persist(traceId, persistedMetadata);
  }

  private handleNullFail(envelope: HarmonySchedulerCommandEnvelope): void {
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.lastCommandKind = HarmonySchedulerCommandKind.NO_MUTATION;
    nextSnapshot.lastSafeFailReason = 'EMPTY_INPUT';
    this.snapshot = nextSnapshot;

    const classifyMetadata: HarmonyCommandMetadata = {
      commandKind: HarmonySchedulerCommandKind.NO_MUTATION
    };
    this.record(
      envelope.traceId,
      HarmonySchedulerTraceStage.COMMAND_CLASSIFIED,
      'Scheduler command classified',
      classifyMetadata
    );

    const safeFailMetadata: HarmonyReasonMetadata = {
      reason: 'EMPTY_INPUT'
    };
    this.record(
      envelope.traceId,
      HarmonySchedulerTraceStage.SAFE_FAILED,
      'Null input safely produced no task mutation',
      safeFailMetadata
    );

    const persistedMetadata: HarmonyReasonMetadata = {
      result: 'SAFE_FAILED',
      reason: 'EMPTY_INPUT'
    };
    this.persist(envelope.traceId, persistedMetadata);
  }

  private persist(
    traceId: string,
    metadata: HarmonySchedulerMetadata,
    pendingCommit?: HarmonySchedulerPendingCommit
  ): void {
    const context = this.requireContext();
    const persistedEvent = harmonySchedulerTelemetry.createEvent(
      traceId,
      HarmonySchedulerTraceStage.SNAPSHOT_PERSISTED,
      'Harmony scheduler backend snapshot saved locally',
      metadata
    );
    this.events = this.prependEvent(persistedEvent, this.events);
    this.events = harmonySchedulerTelemetry.trim(this.events);
    harmonySchedulerStore.saveTasks(context, this.tasks);
    harmonySchedulerStore.saveEvents(context, this.events);

    let lastPathACommit = this.snapshot.lastPathACommit;
    if (pendingCommit) {
      lastPathACommit = this.buildPathACommit(traceId, pendingCommit);
      const commitMetadata: HarmonyPathCommitMetadata = {
        rootGuid: pendingCommit.rootGuid,
        lineageGuid: pendingCommit.lineageGuid,
        ownerChain: this.snapshot.ownerChain.join(' -> ')
      };
      const commitEvent = harmonySchedulerTelemetry.createEvent(
        traceId,
        HarmonySchedulerTraceStage.PATH_A_COMMITTED,
        pendingCommit.message,
        commitMetadata
      );
      this.events = this.prependEvent(commitEvent, this.events);
      this.events = harmonySchedulerTelemetry.trim(this.events);
      harmonySchedulerStore.saveTasks(context, this.tasks);
      harmonySchedulerStore.saveEvents(context, this.events);
    }

    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.tasks = this.sortedTasks();
    nextSnapshot.events = this.copyEvents(this.events);
    nextSnapshot.lastPathACommit = lastPathACommit ? this.copyPathACommit(lastPathACommit) : undefined;
    this.snapshot = nextSnapshot;
    this.emit();
  }

  private buildPathACommit(
    traceId: string,
    pendingCommit: HarmonySchedulerPendingCommit
  ): HarmonySchedulerPathACommitRecord {
    return {
      traceId,
      rootGuid: pendingCommit.rootGuid,
      lineageGuid: pendingCommit.lineageGuid,
      commandKind: pendingCommit.commandKind,
      taskState: pendingCommit.taskState,
      committedAt: Date.now(),
      ownerChain: this.copyStringArray(this.snapshot.ownerChain)
    };
  }

  private record(
    traceId: string,
    stage: HarmonySchedulerTraceStage,
    message: string,
    metadata: HarmonySchedulerMetadata
  ): void {
    const event = harmonySchedulerTelemetry.createEvent(traceId, stage, message, metadata);
    this.events = this.prependEvent(event, this.events);
    this.events = harmonySchedulerTelemetry.trim(this.events);

    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.events = this.copyEvents(this.events);
    this.snapshot = nextSnapshot;
    this.emit();
  }

  private setBusy(isBusy: boolean, busyMessage: string): void {
    const nextSnapshot = this.copySnapshot(this.snapshot);
    nextSnapshot.isBusy = isBusy;
    nextSnapshot.busyMessage = busyMessage;
    this.snapshot = nextSnapshot;
    this.emit();
  }

  private sortedTasks(): HarmonySchedulerTaskRecord[] {
    const nextTasks = this.copyTasks(this.tasks);
    nextTasks.sort((left: HarmonySchedulerTaskRecord, right: HarmonySchedulerTaskRecord) => right.updatedAt - left.updatedAt);
    return nextTasks;
  }

  private sortedEvents(): HarmonySchedulerTraceEvent[] {
    const nextEvents = this.copyEvents(this.events);
    nextEvents.sort((left: HarmonySchedulerTraceEvent, right: HarmonySchedulerTraceEvent) => right.occurredAt - left.occurredAt);
    return nextEvents;
  }

  private resolveExplicitRescheduleTarget(
    targetQuery: string
  ): { kind: 'resolved'; task: HarmonySchedulerTaskRecord } | { kind: 'ambiguous' } | { kind: 'no_match' } {
    const normalizedTarget = this.normalizeTargetQuery(targetQuery);
    if (!normalizedTarget) {
      return { kind: 'no_match' };
    }
    const matches = this.tasks.filter((task: HarmonySchedulerTaskRecord) => {
      if (task.state !== HarmonySchedulerTaskState.ACTIVE || task.timingKind !== HarmonySchedulerTaskTimingKind.EXACT) {
        return false;
      }
      const normalizedTitle = this.normalizeTargetQuery(task.title);
      return normalizedTitle === normalizedTarget ||
        normalizedTitle.includes(normalizedTarget) ||
        normalizedTarget.includes(normalizedTitle);
    });
    if (matches.length === 0) {
      return { kind: 'no_match' };
    }
    if (matches.length > 1) {
      return { kind: 'ambiguous' };
    }
    return { kind: 'resolved', task: this.copyTask(matches[0]) };
  }

  private normalizeTargetQuery(raw: string): string {
    return raw.trim().toLowerCase().replace(/\s+/g, ' ');
  }

  private isExactIsoTimestamp(raw: string): boolean {
    if (!raw) {
      return false;
    }
    return !Number.isNaN(Date.parse(raw));
  }

  private emit(): void {
    this.listeners.forEach((listener: HarmonySchedulerListener) => {
      listener(this.snapshot);
    });
  }

  private requireContext(): common.UIAbilityContext {
    if (!this.context) {
      throw new Error('Harmony scheduler context not initialized');
    }
    return this.context;
  }

  private buildLineageGuid(rootGuid: string, revisionNo: number): string {
    return `${rootGuid}/${revisionNo}`;
  }

  private errorMessage(error: HarmonySchedulerError): string {
    if (error instanceof Error) {
      return error.message;
    }
    return `${error}`;
  }

  private prependTask(
    task: HarmonySchedulerTaskRecord,
    source: HarmonySchedulerTaskRecord[]
  ): HarmonySchedulerTaskRecord[] {
    const tasks: HarmonySchedulerTaskRecord[] = [this.copyTask(task)];
    source.forEach((item: HarmonySchedulerTaskRecord) => {
      tasks.push(this.copyTask(item));
    });
    return tasks;
  }

  private prependEvent(
    event: HarmonySchedulerTraceEvent,
    source: HarmonySchedulerTraceEvent[]
  ): HarmonySchedulerTraceEvent[] {
    const events: HarmonySchedulerTraceEvent[] = [this.copyEvent(event)];
    source.forEach((item: HarmonySchedulerTraceEvent) => {
      events.push(this.copyEvent(item));
    });
    return events;
  }

  private copyTasks(source: HarmonySchedulerTaskRecord[]): HarmonySchedulerTaskRecord[] {
    const tasks: HarmonySchedulerTaskRecord[] = [];
    source.forEach((task: HarmonySchedulerTaskRecord) => {
      tasks.push(this.copyTask(task));
    });
    return tasks;
  }

  private copyTask(task: HarmonySchedulerTaskRecord): HarmonySchedulerTaskRecord {
    return {
      rootGuid: task.rootGuid,
      revisionNo: task.revisionNo,
      lineageGuid: task.lineageGuid,
      title: task.title,
      sourceInput: task.sourceInput,
      commandKind: task.commandKind,
      timingKind: task.timingKind,
      urgencyLabel: task.urgencyLabel,
      state: task.state,
      reminderState: task.reminderState,
      pathBState: task.pathBState,
      dueAtIso: task.dueAtIso,
      conflictDetected: task.conflictDetected,
      createdAt: task.createdAt,
      updatedAt: task.updatedAt,
      supersededAt: task.supersededAt
    };
  }

  private copyEvents(source: HarmonySchedulerTraceEvent[]): HarmonySchedulerTraceEvent[] {
    const events: HarmonySchedulerTraceEvent[] = [];
    source.forEach((event: HarmonySchedulerTraceEvent) => {
      events.push(this.copyEvent(event));
    });
    return events;
  }

  private copyEvent(event: HarmonySchedulerTraceEvent): HarmonySchedulerTraceEvent {
    return {
      traceId: event.traceId,
      stage: event.stage,
      message: event.message,
      metadata: event.metadata,
      occurredAt: event.occurredAt
    };
  }

  private copySnapshot(snapshot: HarmonySchedulerSnapshot): HarmonySchedulerSnapshot {
    return {
      tasks: this.copyTasks(snapshot.tasks),
      events: this.copyEvents(snapshot.events),
      isLoading: snapshot.isLoading,
      isBusy: snapshot.isBusy,
      busyMessage: snapshot.busyMessage,
      lastInput: snapshot.lastInput,
      lastScenario: snapshot.lastScenario,
      lastIngressKind: snapshot.lastIngressKind,
      lastIngressOwner: snapshot.lastIngressOwner,
      ownerChain: this.copyStringArray(snapshot.ownerChain),
      lastCommandKind: snapshot.lastCommandKind,
      lastSafeFailReason: snapshot.lastSafeFailReason,
      lastPathACommit: snapshot.lastPathACommit ? this.copyPathACommit(snapshot.lastPathACommit) : undefined,
      lastErrorMessage: snapshot.lastErrorMessage
    };
  }

  private copyPathACommit(commit: HarmonySchedulerPathACommitRecord): HarmonySchedulerPathACommitRecord {
    return {
      traceId: commit.traceId,
      rootGuid: commit.rootGuid,
      lineageGuid: commit.lineageGuid,
      commandKind: commit.commandKind,
      taskState: commit.taskState,
      committedAt: commit.committedAt,
      ownerChain: this.copyStringArray(commit.ownerChain)
    };
  }

  private copyStringArray(values: string[]): string[] {
    const nextValues: string[] = [];
    values.forEach((value: string) => {
      nextValues.push(value);
    });
    return nextValues;
  }
}

export const harmonySchedulerRepository = new HarmonySchedulerRepository();
