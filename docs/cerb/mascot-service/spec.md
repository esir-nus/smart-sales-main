# Mascot Service (System I)

> **OS Layer**: RAM Application (Operates out-of-band on EventBus streams)
> **State**: SHIPPED

## Overview

Mascot Service handles **System I** interactions in the Dual-Engine Architecture. It is stateless, ephemeral, and provides a playful, empathetic UI layer without polluting the formal `SessionHistory` used by the `IntentOrchestrator` (System II).

**Core Responsibilities**:
1.  **Casual Greetings & Small Talk**: Responds to "Hello", "Thanks", etc.
2.  **Noise Handling**: Gracefully ignores or deflects invalid ASR noise.
3.  **Proactive Engagement**: Surfaces Daily Recaps or Tips on app idle.

## Architecture

```text
┌────────────────────────────────────────────────────────────┐
│ USER INTERFACE                                              │
│ 🎭 Ephemeral Mascot Overlay (Out-of-band)                  │
└──────────────────────────┬─────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│ MASCOT SERVICE (System I)                                  │
│ 1. Listens to EventBus for state changes (Idle, Error)     │
│ 2. Receives low-info intents from Lightning Router         │
│ 3. Produces ephemeral `MascotState`                        │
└──────────┬───────────────────────────────┬─────────────────┘
           │                               │
   (Direct LLM or Hardcoded)        (EventBus Observer)
           │                               │
           ▼                               ▼
┌──────────────────────┐    ┌──────────────────────────────┐
│ LLM: qwen-turbo      │    │ System Event Bus             │
│ (Fast, cheap)        │    │ (Network, ASR states)        │
└──────────────────────┘    └──────────────────────────────┘
```

## Anti-Pattern Rules

1.  **No Persistence**: The Mascot must NEVER push chat turns to the `HistoryRepository`. Every UI mount is a blank slate.
2.  **No Critical Toasts**: The Mascot is NOT a replacement for OS Toasts. System completion events (e.g. "Audio done") **MUST** remain native OS `NotificationService` toasts. The Mascot is for *engagement*, not *confirmation*.
3.  **No Heavy Context**: Do not feed the Mascot the `EnhancedContext` DB loads. It only needs the User Profile (Name) and current Time of Day.
4.  **No Silent Gateway Drops**: The `IntentOrchestrator` MUST emit `PipelineResult.MascotIntercepted` when routing to the mascot asynchronously. Do not silently let the flow end, as this breaks the deterministic state machine of `AgentViewModel` and causes the 'Infinite Shimmer' bug.
5.  **No Permanent Overlays**: `RealMascotService` must actively mutate `_state.value` to `MascotState.Active` upon handling text intents, and is directly responsible for launching its own coroutine delay to revert the state back to `MascotState.Hidden`. UI layers (`AgentShell`) merely observe this via `collectAsStateWithLifecycle()`.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Interface & Fakes | ✅ SHIPPED | `MascotService` interface, `FakeMascotService` |
| **2** | Basic Routing | ✅ SHIPPED | Wire Lightning Router constraints to send NOISE/GREETING intents to Mascot |
| **3** | EventBus Integration | ✅ SHIPPED | Mascot observes App Idle state to trigger proactive prompts |
| **4** | UI Integration | ✅ SHIPPED | Overlay Compose UI binding |
