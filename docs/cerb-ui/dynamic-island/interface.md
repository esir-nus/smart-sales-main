# Dynamic Island UI Interface

> **Context Boundary**: `docs/cerb-ui/dynamic-island/`
> **OS Layer**: Shell-level one-line projection with scheduler and connectivity entry actions
> **Status**: Active

## 1. Responsibility

This interface defines the minimal UI contract for the one-line Dynamic Island surface.

The contract is intentionally small:

- one hidden state
- one visible state
- one one-line visible item payload
- shell-owned lane and visual-state metadata
- two allowed tap actions with optional scheduler target metadata

## 2. State Contract

- `DynamicIslandUiState.Hidden`: the shell should not render the island.
- `DynamicIslandUiState.Visible`: the shell renders one sticky one-line island item.

## 3. Data Contract

```kotlin
sealed interface DynamicIslandUiState {
    data object Hidden : DynamicIslandUiState
    data class Visible(val item: DynamicIslandItem) : DynamicIslandUiState
}

enum class DynamicIslandLane {
    SCHEDULER,
    CONNECTIVITY
}

enum class DynamicIslandVisualState {
    SCHEDULER_UPCOMING,
    SCHEDULER_CONFLICT,
    SCHEDULER_IDLE,
    SESSION_TITLE_HIGHLIGHT,
    CONNECTIVITY_CONNECTED,
    CONNECTIVITY_DISCONNECTED,
    CONNECTIVITY_RECONNECTING,
    CONNECTIVITY_NEEDS_SETUP
}

data class DynamicIslandItem(
    val sessionTitle: String = "",
    val displayText: String,
    val lane: DynamicIslandLane = DynamicIslandLane.SCHEDULER,
    val visualState: DynamicIslandVisualState = DynamicIslandVisualState.SCHEDULER_UPCOMING,
    val showsAudioIndicator: Boolean = false,
    val batteryPercentage: Int? = null,
    val tapAction: DynamicIslandTapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
)

data class DynamicIslandSchedulerTarget(
    val date: LocalDate? = null,
    val taskId: String? = null,
    val isConflict: Boolean = false
)

sealed interface DynamicIslandTapAction {
    data class OpenSchedulerDrawer(
        val target: DynamicIslandSchedulerTarget? = null
    ) : DynamicIslandTapAction

    data object OpenConnectivityEntry : DynamicIslandTapAction
}
```

`batteryPercentage` remains provisional connected-lane data. It is consumed by shell-owned connected ambient chrome and must not be rendered as inline battery UI inside the one-line island body.

`showsAudioIndicator` is a shell-owned presentation hint. In the current SIM slice it prepends a fixed audio glyph on rendered session-title surfaces when the session has ever carried audio context.

## 4. Invariants

- scheduler remains the default lane when no connectivity takeover is active
- only one item may be visible at a time
- the item stays one line even when content is long
- overflow truncates rather than wrapping or marquee-scrolling
- `SESSION_TITLE_HIGHLIGHT` remains a scheduler-lane presentation variant, not a new tap lane
- the current SIM shell may dwell `SESSION_TITLE_HIGHLIGHT` for `3s` while scheduler-backed items keep the local `5s` dwell
- visible-lane tap must follow the currently rendered item
- downward drag is scheduler-only and is lawful only when the scheduler lane is visible
- the RuntimeShell/SIM connectivity lane may reuse the same renderer without widening the surrounding header contract
- connectivity takeover must use transport-truth `connectionState`, not manager-only refinement state
- connected battery display is provisional and currently sourced from the shell/viewmodel mock value until a bridge-backed battery contract lands
- any connected battery presentation stays shell-owned ambient chrome outside the island body rather than inline island UI

## 5. You Should NOT

- do not pass raw scheduler repositories or drawer state machines into the island component
- do not let the island own scheduler task selection or mutation logic
- do not derive connectivity island behavior from `effectiveState`, manager-only BLE/Wi-Fi diagnostics, update-check branches, or Wi-Fi mismatch copy
- do not add second-line subtitles, stacked lanes, or inline buttons without updating this interface first
