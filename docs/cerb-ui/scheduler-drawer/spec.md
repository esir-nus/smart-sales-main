> **OS Layer**: RAM Application (UI Presentation)

# Scheduler Drawer Spec

## Overview
This shard is the visual representation of the Scheduler. It binds the `TimelineItem` mappings to `@Composable` views. It acts purely as a Dumb UI rendering state and forwarding Intents.

## Component Topology
- `SchedulerDrawer`: Top-level modal wrapper. Contains the ViewModel lifecycle bindings. Assumes side-effect logic (Wave 14).
- `SchedulerCards`: Stateless visual representation of Timeline Items. Will be dismantled into atomic bits (`TaskCardHeader` and `TaskCardDetails`).
- `SchedulerTimeline`: Recycler equivalent.
- `SchedulerConflict`: Kanban alert visualizations.
- `CollapsibleInspirationShelf`: Inspiration notes visualization.

## Interaction Rules
- **Task Complete Toggle**: Tapping checkbox -> visual grey-out -> strike-through -> disappears gracefully.
- **Reschedule**: Breathing animation (`#FFA726` Amber glow, 0.3-0.8 alpha) on target scheduled date to signal update success.
- **Smart Tips Lazy Load**: 
   - Initial Expand -> UI shows Shimmer loading animation (2-4s)
   - Subsequently caches tips.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1-12** | Timeline, Cards, Tips | ✅ SHIPPED | Composables, Shimmers, Amber Glows. |
| **13** | UI Mapper | ✅ SHIPPED | `TimelineItemModel.toUiState()`. |
| **14** | Atomic UI Teardown & Clarification | 🔲 IN PROGRESS | Hoist Side-Effects (`PhoneAudioRecorder`) to accept `java.io.File`. Map `ScheduledTask.clarificationState` dynamically without global UI states. Dismantle 700-line `SchedulerCards.kt`. |
