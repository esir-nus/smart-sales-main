# Dynamic Island UI Interface

> **Context Boundary**: `docs/cerb-ui/dynamic-island/`
> **OS Layer**: Display-only shell projection with a single scheduler resume action
> **Status**: Active

## 1. Responsibility

This interface defines the minimal UI contract for the one-line Dynamic Island surface.

The contract is intentionally small:

- one hidden state
- one visible state
- one scheduler-backed item payload
- one allowed tap action with optional scheduler target metadata

## 2. State Contract

- `DynamicIslandUiState.Hidden`: the shell should not render the island.
- `DynamicIslandUiState.Visible`: the shell renders one sticky one-line island item.

## 3. Data Contract

```kotlin
sealed interface DynamicIslandUiState {
    data object Hidden : DynamicIslandUiState
    data class Visible(val item: DynamicIslandItem) : DynamicIslandUiState
}

data class DynamicIslandItem(
    val sessionTitle: String,
    val schedulerSummary: String,
    val isConflict: Boolean = false,
    val isIdleEntry: Boolean = false,
    val tapAction: DynamicIslandTapAction
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
}
```

## 4. Invariants

- v1 content is scheduler-only
- only one item may be visible at a time
- the item stays one line even when content is long
- overflow truncates rather than wrapping or marquee-scrolling
- tap action is limited to `OpenSchedulerDrawer`
- when a scheduler target is present, the shell should use it to land on the corresponding scheduler date page

## 5. You Should NOT

- do not pass raw scheduler repositories or drawer state machines into the island component
- do not let the island own scheduler task selection or mutation logic
- do not add second-line subtitles or stacked lanes without updating this interface first
