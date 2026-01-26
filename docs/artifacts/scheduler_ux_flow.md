# Scheduler UX/Architecture Visualization

**Role**: UX Specialist & System Architect
**Purpose**: Visualize the Scheduler as the "Active Agent" Home Screen.

---

## 1. System Flow Visualization

```ascii
                                   ┌───────────────────────┐
                                   │   USER (Voice/Tap)    │
                                   └──────────┬────────────┘
                                              │
                                     [1] "Meeting with Chen at 3pm"
                                              │
                                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            PRISM SCHEDULER PIPELINE                         │
│                                                                             │
│  ┌──────────────┐     ┌──────────────┐     ┌─────────────────────────────┐  │
│  │ ASR Engine   │────▶│ Context      │────▶│ LLM (Qwen Structured)       │  │
│  │ (Cloud/Dev)  │     │ Builder      │     │ "Extract Intent & Conflicts"│  │
│  └──────────────┘     └──────┬───────┘     └──────────────┬──────────────┘  │
│                              │                            │                 │
│                      [2] Checks LTM                       │ [3] JSON Output │
│                      (Conflict Scan)                      ▼                 │
│                                                   ┌────────────────┐        │
│                                                   │ Schedule       │        │
│                                                   │ Publisher      │        │
│                                                   └───────┬────────┘        │
│                                                           │                 │
│                          ┌────────────────────┬───────────┴─────────┐       │
│                          ▼                    ▼                     ▼       │
│                  ┌──────────────┐     ┌──────────────┐      ┌─────────────┐ │
│                  │ LTM STROAGE  │     │ ALARM SYSTEM │      │ UI RENDERER │ │
│                  │ (SQL/Room)   │     │ (System Mgr) │      │ (Aurora)    │ │
│                  └──────────────┘     └──────────────┘      └──────┬──────┘ │
│                                                                    │        │
└────────────────────────────────────────────────────────────────────┼────────┘
                                                                     │
                                                              [4] Real-time
                                                                  Update
                                                                     ▼
┌─────────────────────────────── NAVIGATION MODEL ────────────────────────────┐
│                                                                             │
│      [ HANDLE ]  (Swipe Down for Scheduler Overlay)                         │
│   ┌──────────────┐                                                          │
│   │  SCHEDULER   │  ◀── Global Top Drawer (Agent Context)                   │
│   │              │      - Time Flow                                         │
│   │              │      - P0 Tasks                                          │
│   │              │      - Conflicts                                         │
│   └──────────────┘                                                          │
│          ▼                                                                  │
│                                                                             │
│   ┌──────────────┐                                                          │
│   │     CHAT     │  ◀── Base Layer (Home)                                   │
│   │ (Conversation│      - Main Interaction                                  │
│   │    Stream)   │                                                          │
│   └──────────────┘                                                          │
│          ▲                                                                  │
│                                                                             │
│   ┌──────────────┐                                                          │
│   │  AUDIO MGR   │  ◀── Bottom Drawer (Swipe Up)                            │
│   │              │      - Files / Assets                                    │
│   │              │      - Uploads                                           │
│   └──────────────┘                                                          │
│      [ HANDLE ]  (Swipe Up for Audio Files)                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Cross-Reference Flow (Chat Mode Awareness)

The Agent doesn't forget your schedule just because you're chatting.

```ascii
┌──────────────────────┐
│ USER (Chat Mode)     │
│ "Can I meet John?"   │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐         ┌─────────────────────────┐
│ Context Builder      │◀───────▶│ LTM (Schedule Table)    │
│ (Chat Strategy)      │         │ "Query: Schedule Today" │
└──────────┬───────────┘         └─────────────────────────┘
           │
           │ *Injects Schedule Summary into Prompt*
           ▼
┌──────────────────────┐
│ LLM (Qwen Ops)       │
│ "User asks availability. Schedule shows gap at 2pm."
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Chat Response        │
│ "Yes, you have a slot at 2pm, but keep in mind..."
└──────────────────────┘
```

---

## 3. UX State Inventory (Home Screen)

| State | Visual Trigger | User Experience | Microcopy / Feedback |
|-------|----------------|-----------------|----------------------|
| **Idle (Dashboard)** | App Launch | **"Time Flow"**: A clean, scrolling timeline. Current time marked by pulsing Aurora orb. | "Good Morning, Frank. 3 P0 tasks today." |
| **Listening** | Tap Mic / Badge | **Aurora Pulse**: Bottom sheet glows warmly, reacting to voice volume. | "Listening..." |
| **Processing** | Silence > 1s | **Shimmer**: Skeleton cards appear on timeline where the new item might go. | "Scheduling..." |
| **Confirmation** | Success | **Materialization**: Item snaps into place. Glass effect solidifies. | "Added: Meeting with Chen." |
| **Conflict** | Logic Check | **Soft Warn**: Item appears with yellow glow. Attached "Tip" card slides out. | "Conflict detected." |
| **Reminding** | Time Trigger | **Toast/Popup**: Non-intrusive top banner. | "10m to Meeting: Room 302" |

## 4. UI Director Notes (Visuals)

-   **Zero Chrome**: No "Month/Week/Day" tabs at top. Just an infinite vertical scroll starting at "Today".
-   **Glassmorphism**: Items are semi-transparent plates floating above the background.
-   **P0 Highlighting**: Critical tasks have a subtle *inner glow* (CSS: `box-shadow: inset`) using the Aurora Red/Orange token. They don't scream "Red Alert", they just feel "hot".
-   **Smart Tips**: These are NOT separate cards in the list. They are **child elements** attached to the schedule item, visually connected (like a sticky note on a folder).

---

## 5. Functional Invariants

1.  **Immediate Access**: Scheduler IS the Home Screen. Launch time < 200ms.
2.  **Voice Dominance**: All fields (Title, Time, Location, Participants) MUST be populate-able via a single voice command.
3.  **Conflict Non-Blocking**: The system warns about conflicts but *never* prevents saving. We assume the user knows best.
4.  **Privacy**: "Smart Tips" (LLM generated) are stored locally in LTM, not sent back to cloud logs.
