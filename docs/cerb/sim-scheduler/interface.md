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
    viewModel: ISchedulerViewModel
)
```

SIM should reuse this UI surface.

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

---

## Required Behaviors

- timeline observation
- day selection
- conflict state projection
- delete/reschedule handling within approved SIM scope
- Path A-triggered creation flow

---

## Optional for V1

- inspiration shelf
- smart tips
- voice-triggered entry
- completed-memory merge

These may be omitted if they force unrelated dependencies into SIM.

---

## You Should NOT

- ❌ inject the current `SchedulerViewModel` directly into SIM just because the UI compiles
- ❌ depend on Path B or CRM enrichment for baseline scheduler operation
- ❌ treat smart-app memory merging as required SIM truth unless a later SIM spec adds it

---

## When to Read Full Spec

Read `spec.md` if:

- you are deciding which current scheduler behaviors survive into SIM
- you are implementing `SimSchedulerViewModel`
- you are deciding whether inspiration/tips/completed-memory behavior belongs in SIM V1
