# SIM Shell Interface

> **Blackbox contract** - For the standalone SIM app entry and adjacent prototype-only callers.

---

## Entry Surface

### SIM Activity

```kotlin
@AndroidEntryPoint
class SimMainActivity : ComponentActivity()
```

Responsibilities:

- boot the standalone SIM composition root
- request only the permissions needed by SIM
- mount `SimShell`

### SIM Shell

```kotlin
@Composable
fun SimShell()
```

Responsibilities:

- coordinate scheduler and audio drawers
- host the simple chat surface
- host SIM support surfaces such as history and connectivity entry
- route `Ask AI` and audio re-selection flows

---

## Required Collaborators

```kotlin
interface SimShellDependencies {
    val chatViewModel: IAgentViewModel
    val schedulerViewModel: ISchedulerViewModel
}
```

Notes:

- `IAgentViewModel` is reused as a UI seam, not as approval to use `AgentViewModel`
- `ISchedulerViewModel` is reused as a UI seam, not as approval to use `SchedulerViewModel`
- the current shared `AudioDrawer` does not yet expose an equivalent safe seam, so Wave 1 may use a SIM-owned drawer wrapper until that boundary is extracted

---

## Shell State Contract

```kotlin
enum class SimDrawerType {
    SCHEDULER,
    AUDIO
}

data class SimShellState(
    val activeDrawer: SimDrawerType? = null,
    val activeChatAudioId: String? = null,
    val showHistory: Boolean = false
)
```

Guarantees:

- only one drawer may be open at a time
- the shell may reopen the audio drawer from chat
- the shell may expose history/new-page/connectivity/settings as SIM support surfaces
- the shell does not expose smart-only drawer types or smart-runtime-only shell meaning

---

## You Should NOT

- ❌ mount `AgentShell` directly
- ❌ inject the smart app's full singleton graph by default
- ❌ expose mascot, debug HUD, or plugin task board as required SIM shell behavior
- ❌ treat the shell as permission to share storage blindly with the smart app

---

## When to Read Full Spec

Read `spec.md` if:

- you are defining the standalone entry path
- you are wiring the prototype DI root
- you are deciding which shell surfaces survive into SIM
