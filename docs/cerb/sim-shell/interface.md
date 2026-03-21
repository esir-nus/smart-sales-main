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
- own the badge scheduler follow-up continuity binding metadata
- own connectivity route state, overlay presentation, and SIM route telemetry only

Connectivity-specific non-responsibilities:

- do not own BLE/Wi-Fi truth
- do not own reconnect/disconnect/unpair behavior
- do not own setup runtime behavior
- do not own badge file operations for audio

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
    val activeConnectivitySurface: SimConnectivitySurface? = null,
    val showHistory: Boolean = false
)
```

Guarantees:

- only one drawer may be open at a time
- the shell may reopen the audio drawer from chat
- the shell may expose history/new-page/connectivity/settings as SIM support surfaces
- the shell owns connectivity route state and may distinguish bootstrap modal vs setup vs manager connectivity surfaces
- the shell does not expose smart-only drawer types or smart-runtime-only shell meaning
- the shell does not become the owner of connectivity backend truth just because it hosts connectivity surfaces

### Badge Follow-Up Continuity Binding

```kotlin
data class SimBadgeFollowUpState(
    val threadId: String,
    val origin: SimBadgeFollowUpOrigin,
    val lane: SimBadgeFollowUpLane,
    val boundSessionId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastActiveSurface: SimBadgeFollowUpSurface
)

class SimBadgeFollowUpOwner : ViewModel() {
    val activeFollowUp: StateFlow<SimBadgeFollowUpState?>

    fun startBadgeSchedulerFollowUp(
        boundSessionId: String,
        initialSurface: SimBadgeFollowUpSurface = SimBadgeFollowUpSurface.SHELL
    )

    fun markSurface(surface: SimBadgeFollowUpSurface)

    fun clear(reason: SimBadgeFollowUpClearReason)
}
```

Notes:

- this owner is shell-owned metadata only, not a second session-memory lane
- the bound SIM chat session remains the conversational source of truth
- `SimShell` starts this binding from `BadgeAudioPipeline.events`
- only `PipelineEvent.Complete` with `TaskCreated` or non-empty `MultiTaskCreated` may start or replace the binding
- shelf-card `Ask AI` and scheduler dev/test mic flows must not start or mutate the binding
- unrelated session switch, bound-session delete, and explicit new session must clear the binding

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
