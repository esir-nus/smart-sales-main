---
description: Create a standardized Cerb UI/UX specification (States + Interactions + Layout)
---

# Cerb UI Spec Scaffolder

> **Purpose**: Create a standardized Cerb specification for a User Interface component or screen.
> **Output**: `ui-spec.md` and `contract.kt` (or similar interface files) defining the visual bounds and states.
> **Next Step**: Once spec is created, use `/feature-dev-planner` or `/SOP-ui-building` to implement it.

---

## Step 1: Define the UI Contract (State & Intents)

Create `docs/cerb/ui/[feature-name]/contract.md`.

**Rule**: UI code must be driven by states. This defines the exact data the UI receives and the actions it can emit.

```markdown
# [Feature Name] UI Contract

> **Owner**: [Feature] / UI Layer
> **ViewModel**: [Feature]ViewModel

## UI State

\```kotlin
data class [Feature]UiState(
    val isLoading: Boolean = false,
    val data: [Feature]Data? = null,
    val error: String? = null
)
\```

## UI Intents (Actions)

\```kotlin
sealed interface [Feature]Intent {
    data class OnItemClick(val id: String) : [Feature]Intent()
    data object OnDismiss : [Feature]Intent()
}
\```
```

---

## Step 2: Define the Visual/Interaction Spec

Create `docs/cerb/ui/[feature-name]/spec.md`.

### 2.1 OS Layer Declaration (Mandatory)

> **OS Layer**: RAM Application (UI Presentation)

### 2.2 Layout & Component Structure

```markdown
## Component Topology

- `[Feature]Screen`: The top-level stateful container.
- `[Feature]Content`: The stateless presentation layer.
- `[Feature]Item`: Reusable layout chunk.
```

### 2.3 Interaction Rules & Gestures

Define the Anti-Illusion rules for this UI. How does it handle empty states? Loading states?

```markdown
## Interaction Rules

- **Empty State**: Must display [Placeholder].
- **Error State**: Must display [Snackbar|Amber Card].
- **Loading**: [Shimmer|Spinner].
```

### 2.4 Wave Plan

```markdown
## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Scaffold & State | 🔲 PLANNED | `contract.md` + basic Compose outlines |
| **2** | Visual Polish | 🔲 PLANNED | Exact styling, padding, theme integration |
| **3** | Interaction & Gestures | 🔲 PLANNED | Animations, swipe-to-dismiss, edge cases |
```

---

## Step 3: Register in Tracker

Update `docs/plans/tracker.md`:
1. Add new row to **Cerb Spec Index** under a UI section.
2. Set State to `SPEC_ONLY`.

---

## Checklist

- [ ] `contract.md` created (State & Intents)
- [ ] `spec.md` created (Layout, Gestures, OS Layer)
- [ ] Registered in `tracker.md` as `SPEC_ONLY`
