# SIM Scheduler Interface

> **Blackbox contract** — For the standalone SIM scheduler surface.

---

## UI Surface

### Scheduler Drawer

```kotlin
@Composable
fun SchedulerDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onInspirationAskAi: ((String) -> Unit)? = null,
    enableInspirationMultiSelect: Boolean = true,
    viewModel: ISchedulerViewModel
)
```

SIM should reuse this UI surface.
For SIM wiring, `enableInspirationMultiSelect` must be passed as `false` so the deprecated bulk `问AI (N)` branch stays unreachable while the shelf-card launcher remains available.

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

---

## Required Behaviors

- timeline observation
- day selection
- date-attention acknowledgement via `onDateSelected(dayOffset)` / `acknowledgeDate(dayOffset)`
- conflict state projection
- delete/reschedule handling within approved SIM scope
- Path A-triggered creation flow
- off-page exact create and off-page vague create must be representable as normal date attention through `unacknowledgedDates`
- off-page conflict-create must be representable as warning-priority date attention by reusing `rescheduledDates`
- multi-task create must aggregate attention per target date rather than choosing a single mutation-wide attention outcome
- same-page create must not emit redundant calendar attention
- visible inspiration shelf with shelf-card `Ask AI` acting as a plain new-chat launcher seeded by the card text
- SIM drawer keeps inspiration multi-select contract state inert/unreachable; the deprecated `问AI (N)` bulk action is not part of SIM V1
- created/rescheduled target dates must be representable through `unacknowledgedDates` / `rescheduledDates`
- exact alarm permission prompting stays on this interface boundary when reminder scheduling is in scope

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
