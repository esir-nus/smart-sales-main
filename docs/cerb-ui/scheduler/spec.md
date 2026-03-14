# Scheduler UI Specification

> **Context Boundary**: `docs/cerb-ui/scheduler/`
> **OS Layer**: RAM Application (UI Presentation)
> **Status**: PARTIAL

## Component Topology

- `SchedulerDrawer`: The top-level stateful container (Bottom sheet or side drawer).
- `SchedulerContent`: The stateless presentation layer that consumes `SchedulerUiState`.
- `TaskPreviewCard`: Reusable layout chunk showing the proposed task details.
- `ConflictAlert`: Reusable layout chunk showing scheduling overlap warnings.

## Interaction Rules & Gestures

- **Scrim & Drawer**: Must follow the "Scrim + Drawer Pattern" (Scrim fades, Drawer slides, separate AnimatedVisibility).
- **Empty State**: `Idle` shows nothing or a generic "Ready to Schedule" placeholder.
- **Loading State**: `ScanningTimeline` and `Executing` use shimmer effects on the `TaskPreviewCard` outline.
- **Error State**: Displays inline Amber Card preventing confirmation until resolved.
- **Confirmation Gestures**: Requires explicit tap on "Confirm". Swipe-to-dismiss implies Cancellation/Abort.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Scaffold & State | ✅ SHIPPED | `contract.md` + basic Compose outlines |
| **2** | Visual Polish | ✅ SHIPPED | Exact styling, padding, theme integration via `FakeAgentViewModel` previews |
| **3** | Interaction & Gestures | ✅ SHIPPED | Animations, Scrim + Drawer rules, edge cases |
| **4** | Backend Integration | 🔲 PLANNED | Connect to actual `SchedulerViewModel` and `UnifiedPipeline` |
