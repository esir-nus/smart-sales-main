---
description: Audit or extend the Prism FSM Registry - evolve state machines incrementally
---

# FSM Review Workflow

Use this workflow when adding new features that require state management, or when auditing existing FSMs.

---

## 1. Discovery

```bash
# turbo
grep -rn "FlowController" app-prism/src/main/java/
```

List all registered `FlowMode` types:
```bash
# turbo
grep -rn "FlowMode" app-prism/src/main/java/
```

---

## 2. Gap Analysis

For a new feature, ask:

| Question | If YES | If NO |
|----------|--------|-------|
| Does an existing FSM cover this trigger? | Extend existing states | Design new `FlowMode` + `Controller` |
| Is this a UI state or a background process? | UI → Check `ui_element_registry.md` | Background → Check `Prism-V1.md` |
| Does it emit to the Unified Output? | Good | Add `state: StateFlow<FlowState>` |

---

## 3. Design New FSM (Template)

When creating a new FSM:

```kotlin
// 1. Define the Mode
sealed interface FlowMode {
    // Existing modes...
    data object NewFeature : FlowMode  // <-- Add here
}

// 2. Define the State Hierarchy
sealed interface NewFeatureState {
    data object Idle : NewFeatureState
    data class Active(val data: String) : NewFeatureState
    data object Done : NewFeatureState
}

// 3. Implement the Controller
class NewFeatureFlowController @Inject constructor() : FlowController {
    private val _state = MutableStateFlow<NewFeatureState>(NewFeatureState.Idle)
    override val state: StateFlow<FlowState> = _state.map { it as FlowState }
    // ...
}

// 4. Register
registry.register(FlowMode.NewFeature, newFeatureController)
```

---

## 4. Spec Updates

After designing:
1. **`Prism-V1.md`**: Add §4.6.X for new Controller contract.
2. **`ui_element_registry.md`**: Add states to relevant section.
3. **`prism-ui-ux-contract.md`**: Update ASCII if UI affected.

---

## 5. Implementation Handoff

Run:
```
/10-feature-build-mode
```
With context: "Implement [NewFeature]FlowController per §4.6.X"

---

## Examples: When to Use This

| Scenario | Action |
|----------|--------|
| Adding "Memory Updated" snackbar | `/fsm-review` → Design `SystemNotificationController` |
| Adding "Client Preference Updated" | `/fsm-review` → Extend `SystemNotificationController` |
| Auditing Analyst flow | `/fsm-review` → Check `AnalystFlowController` states |
| Adding new Scheduler state | `/fsm-review` → Extend `SchedulerFlowController` |
