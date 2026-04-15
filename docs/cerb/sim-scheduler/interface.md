# SIM Scheduler Interface (Historical Redirect)

> **Blackbox contract** — Historical SIM scheduler contract retained in place for campaign memory.
> **Status**: Historical redirect
> **Current Reading Priority**: Historical context only; not current source of truth.
> **Current Active Truth**:
> - `docs/cerb/scheduler-path-a-spine/interface.md`
> - `docs/cerb/scheduler-path-a-uni-a/interface.md`
> - `docs/cerb/scheduler-path-a-uni-b/interface.md`
> - `docs/cerb/scheduler-path-a-uni-c/interface.md`
> - `docs/cerb/scheduler-path-a-uni-d/interface.md`
> - `docs/cerb-ui/scheduler/contract.md`
> - `docs/cerb/interface-map.md`
> - `docs/core-flow/sim-scheduler-path-a-flow.md`

Historical note:

- the remainder of this file is a frozen snapshot of the older SIM-scheduler interface framing
- it must not be used as active scheduler fallback ownership

---

## UI Surface

### Scheduler Drawer

```kotlin
@Composable
fun SchedulerDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    visualMode: SchedulerDrawerVisualMode = SchedulerDrawerVisualMode.STANDARD,
    onInspirationAskAi: ((String) -> Unit)? = null,
    enableInspirationMultiSelect: Boolean = true,
    viewModel: ISchedulerViewModel
)
```

SIM should reuse this UI surface.
For SIM wiring, `enableInspirationMultiSelect` must be passed as `false` so the deprecated bulk `问AI (N)` branch stays unreachable while the shelf-card launcher remains available.
For the approved scheduler-drawer transplant, SIM should also pass `visualMode = SchedulerDrawerVisualMode.SIM` so the shared drawer can adopt the dark top-anchored SIM slab while `AgentShell` keeps the standard presentation.
In debug builds, SIM may also surface a visible explicit `REC` control inside this drawer as a scheduler-local test aid, but that control must still route through the same scheduler `processAudio(...)` lane rather than audio-drawer ingestion.
That scheduler-local `REC` lane must only surface scheduler-owned product copy on terminal failure; raw extractor, classifier, or prompt-debug wording must not leak into the visible warning banner.

---

## Runtime Surface

### SIM Scheduler ViewModel

```kotlin
class SimSchedulerViewModel : ISchedulerViewModel
```

Required meaning:

- emits the state required by `SchedulerDrawer`
- implements only the scheduler slice approved for SIM
- does not require smart-agent runtime collaborators by default
- does not require connectivity contracts or badge connection state
- may expose a shell-safe ordered reminder projection without making the shell the owner of scheduler truth

---

## Required Behaviors

- timeline observation
- ordered active reminder observation for the shell dynamic island
- day selection
- date-attention acknowledgement via `onDateSelected(dayOffset)` / `acknowledgeDate(dayOffset)`
- conflict state projection
- delete/reschedule handling within approved SIM scope
- scheduler-drawer mic reschedule handling within approved SIM scope
- Path A-triggered creation flow
- multi-task create ingress may enter through `Uni-M` before the existing single-task Uni-A / Uni-B / Uni-C chain
- off-page exact create and off-page vague create must be representable as normal date attention through `unacknowledgedDates`
- off-page conflict-create must be representable as warning-priority date attention by reusing `rescheduledDates`
- multi-task create must aggregate attention per target date rather than choosing a single mutation-wide attention outcome
- multi-task fragments must execute as independent creates; the viewmodel must not require a batch persistence primitive
- standalone `N hours/minutes later` fragments must be allowed to resolve from `nowIso` as lawful exact creates
- standalone `明天/后天/tomorrow + clock` fragments inside a multi-task batch must be representable through deterministic now-day-offset anchoring instead of relying on model-computed absolute dates
- exact clock-relative fragments are valid only when a prior exact fragment exists in the same utterance chain
- clock-relative fragments after a vague date-only predecessor must downgrade to vague when the lawful day anchor remains available
- partial-success batch status must remain visible in `pipelineStatus` even when the existing conflict surface is also active
- same-page create must not emit redundant calendar attention
- visible inspiration shelf with shelf-card `Ask AI` acting as a plain new-chat launcher seeded by the card text
- SIM drawer keeps inspiration multi-select contract state inert/unreachable; the deprecated `问AI (N)` bulk action is not part of SIM V1
- created/rescheduled target dates must be representable through `unacknowledgedDates` / `rescheduledDates`
- reminder reliability prompting stays on this interface boundary when scheduler-owned task creation is in scope
- persisted exact tasks must schedule reminders through the shared `AlarmScheduler` / notification stack
- vague tasks must not schedule reminders
- conflict-persisted exact tasks must still schedule reminders
- delete and mark-done must cancel any existing reminder for that task
- restore-from-done must not reschedule reminders in T4.8
- reschedule must cancel the old reminder before scheduling the new exact-time cascade
- `FIRE_OFF` tasks must remain non-conflicting during SIM reschedule/follow-up execution; reschedule must not surface conflict state for them even if another exact task exists at the same instant
- every reschedule clause must contain an explicit target plus a new exact time; omitted-target mutation such as `改到3点` must safe-fail
- explicit day+clock reschedule phrasing such as `明天早上8点` must be interpreted through scheduler-owned deterministic parsing before any model-led exact-time fallback
- explicit delta reschedule phrasing such as `推迟1小时` / `提前半小时` is unsupported and must safe-fail
- reschedule target resolution must stay global across all non-done scheduler-owned task truth; current selected/opened task state and current visible page/date are not semantic authority
- the runtime may use a scheduler-owned active retrieval index derived from `ScheduledTask` rows to build a bounded shortlist context pack for extraction
- the delivered shortlist cap is top 8 candidates
- create ingress should persist optional `keyPerson` / `location` retrieval hints whenever Uni-A / Uni-B / Uni-M extraction supplies them
- notes may enter the retrieval context pack only as weak evidence
- selected-task follow-up reschedule may run a dedicated V2 shadow extractor for time semantics, but the shadow path must stay write-disabled and must not alter user-visible mutation results
- the follow-up V2 shadow path must emit explicit parity / mismatch / invalid / failure observability without widening scheduler authority
- task-scoped follow-up reschedule must resolve the target globally first; selected-task UI state may still support quick actions or prefill only
- scheduler-drawer voice reschedule target resolution must be confidence-gated; low-confidence and near-tie results must surface explicit failure and no write
- model-suggested task choice is advisory only and must be corroborated by scheduler-owned retrieval-index evidence before mutation
- reminder-reliability prompt emission must be process-lifetime gated so one batch does not repeatedly re-prompt
- successful scheduler-owned exact and vague creates may both emit the reminder-reliability prompt, while only exact tasks schedule reminders through `AlarmScheduler`
- the prompt content should adapt to current OEM risk rather than always showing a generic exact-alarm-only message
- reminder scheduling failure must degrade safely without rolling back the task mutation result

Reminder projection rule:

- the reminder projection must be scheduler-owned
- ordering should prefer conflict-visible items first, then urgency, then earlier scheduled time
- completed tasks must not appear
- the shell may rotate or collapse this projection for presentation, but it must not reorder task truth independently
- the shell may cap SIM rendering to the top 3 entries as a presentation detail
- each reminder entry used by the shell must remain targetable to its corresponding scheduler date page

Scheduler-drawer voice resolution rule:

- the scheduler drawer mic may request reschedule within scheduler-owned scope
- target resolution must not depend on SQL/exact-title equality alone
- one clearly dominant task may be resolved and mutated
- matching may use title plus persisted participant/location cues and weak notes digest context
- the extractor may only reason over a scheduler-owned bounded shortlist from the active retrieval index rather than the full task corpus
- current visible page/date and selected/opened task state must not become semantic authority for target choice
- the current clause must itself say which task is moving; follow-up selection/prefill cannot silently supply the target
- after target resolution, exact day+clock tails such as `改到明天早上8点` must remain valid even when the tail itself does not restate the task title
- delta-only tails must safe-fail rather than mutate from the resolved task's persisted start time
- ambiguous or weak matches must not mutate state
- multiple clean reschedule clauses in one utterance decompose into ordinary independent reschedules rather than a distinct batch mode
- this rule does not widen ordinary SIM chat or audio drawer routes into scheduler mutation ownership

---

## Optional for V1

- smart tips
- voice-triggered entry
- completed-memory merge

These may be omitted if they force unrelated dependencies into SIM.

---

## You Should NOT

- ❌ inject the current `SchedulerViewModel` directly into SIM just because the UI compiles
- ❌ depend on Path B or CRM enrichment for baseline scheduler operation
- ❌ depend on `ConnectivityBridge`, `ConnectivityService`, or badge connection state for baseline scheduler operation
- ❌ treat smart-app memory merging as required SIM truth unless a later SIM spec adds it

---

## When to Read Full Spec

Read `spec.md` if:

- you are deciding which current scheduler behaviors survive into SIM
- you are implementing `SimSchedulerViewModel`
- you are deciding which inspiration follow-on behavior beyond the base shelf-card `Ask AI` launcher belongs in SIM V1
- you are wiring scheduler attention states, reschedule motion, or reminder/alarm behavior into SIM
