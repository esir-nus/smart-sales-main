# Scheduler Flows

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md §3

---

## 3.1 ESP32 Voice → Transcribe → Schedule
**Scenario:** User records "帮我约张总下周二下午3点的会" (Schedule meeting with Zhang) via badge.

```
USER (Audio)     SYSTEM (Background)                USER INTERFACE
────────────     ───────────────────                ──────────────
Records Voice -> Badge Uploads WAV -> Syncs ->      Audio Drawer [Cards: +1]
                 Tingwu Transcribes ->              Coach Chat (Hidden)
                                                    ↓
                 Orchestrator Detects Intent ->     Toast: "Detected Schedule Intent"
                 Extacts <Schedule> Tag ->          
                                                    ↓
                 Scheduler Adds Pending Task ->     Scheduler Drawer Opens (Auto)
                                                    Card Appears:
                                                    ┌───────────────────────────────┐
                                                    │ ☐ 与张总会面          [待确认] │
                                                    │   Tue, 3:00 PM                │
                                                    │   [Confirm]  [Edit]           │
                                                    └───────────────────────────────┘
                                                    
User Taps [Confirm] -> Task Active (Alarm Set) ->   Card State: [⏰] Normal
```

## 3.1.1 Voice Correction (MODIFY Intent)
**Scenario:** User records follow-up voice note to fix a mistake (mispronunciation or poor audio).

**Trigger Phrases:**

| Phrase | Example |
|--------|---------|
| "不是X，是Y" | "不是三点，是四点" |
| "改成" | "改成周三" |
| "错了" | "时间错了，应该是下午" |
| "重说" | "我重说一遍" |

**Matching Rule:** Most recent PENDING task (< 5 min old).

```
USER (Audio)     SYSTEM (Background)                USER INTERFACE
────────────     ───────────────────                ──────────────
Records Fix ->   Tingwu Transcribes ->              
                 Detects MODIFY Intent ->           
                 Finds Pending Task (< 5min) ->     
                 Override with New Values ->        
                 Re-query Relevancy Library ->      (Conflict Check)
                         ↓
           ┌─────────────┴──────────────┐
           ▼                            ▼
      No Conflict                  Conflict Found
           ↓                            ↓
  ┌─────────────────────────┐   ┌─────────────────────────────┐
  │ ☐ Meeting Zhang [Updated]│   │ ⚠️ Meeting Zhang  [Conflict]│
  │   Tue, 4:00 PM           │   │   Tue, 4:00 PM              │
  │   📝 3:00 PM (Start)     │   │   ──────────────────────────│
  │   [Confirm] [Edit]       │   │   ⚠️ 与李总电话冲突 (4:00 PM)│
  └─────────────────────────┘   │   [解决冲突] [仍然保留]      │
                                 └─────────────────────────────┘
```

**Conflict Resolution:** Same as §3.4 (Rethink flow applies).

**Fallback:** If no pending task found, create as new task with toast: "未找到待确认任务，已创建新日程".


## 3.2 Inspiration → Ask AI → Coach
**Scenario:** User wants to develop an idea from a note.

```
USER ACTION                          SYSTEM ACTION
───────────                          ─────────────
Open Scheduler Drawer                Displays Inspiration Card
Tap [问AI] on "Competitor Analysis"  Opens Coach Chat
                                     Context: "Competitor Analysis note..."
                                     Prompt: "What do you want to do with this?"
                                     
User types: "Make a plan"            Coach responds with analysis/plan
```

## 3.3 Inspiration Multi-Select (Combined Context)
**Scenario:** User combines two cards to form a larger context.

```
USER ACTION                          INTERFACE STATE
───────────                          ───────────────
Tap [问AI] on Card A                 Multi-select Mode Active
                                     Card A: (●)
                                     Bottom: [ 问AI (1) ]

Tap Card B Body                      Card B: (●)
                                     Bottom: [ 问AI (2) ]

Tap [问AI (2)]                       Opens Coach Chat
                                     Context: Note A + Note B
                                     Prompt: "I've loaded 2 notes. How can I help?"
```

## 3.4 Scheduler Conflict Resolution
**Scenario:** New task conflicts with existing one.

**Multi-Channel Notification (Tiered):**

| Channel | Trigger | Behavior |
|---------|---------|----------|
| **Card Visual** (⚠️) | Always | Card shows `[Conflict]` badge, positioned at collision time |
| **Toast** | App foreground | "⚠️ 检测到日程冲突" (auto-dismiss 3s) |
| **Badge Vibration** | Conflict from badge voice input | 2× short vibration + audio: "检测到冲突" via BLE |
| **Pop-up Modal** | App backgrounded > 30s | Show on next app open (deferred) |

> **Design Principle:** Badge feedback only when user just used badge. Avoids random vibrations.

```
SYSTEM STATE                         INTERFACE STATE
────────────                         ───────────────
Conflict Detected                    1. Card Badge: ⚠️ [Conflict]
(Time collision)                     2. Toast: "检测到日程冲突"
                                     3. (If from badge) → BLE: vibrate + audio
                                     
USER ACTION                          
───────────                          
Tap [Expand] on Conflict Card        Card Expands:
                                     ┌───────────────────────────────────┐
                                     │ ⚠️ 冲突: 与张总午餐 vs 电话会议   │
                                     │ ○ Keep Lunch (Cancel Call)        │
                                     │ ○ Keep Call (Postpone Lunch)      │
                                     │ ○ Manual Reschedule...            │
                                     └───────────────────────────────────┘

Select Option A -> Tap [Confirm]     Orchestrator Executes Rethink (§4.7):
                                     1. Updates Schedule DB
                                     2. Conflict Card Removed
                                     3. Toast: "Schedule Updated"
```

**Deferred Pop-up (On Return):**
```
┌────────────────────────────────────────────┐
│  ⚠️ 您有 1 个日程冲突待处理                 │
│  ──────────────────────────────────────    │
│  Lunch with Zhang ↔ Call with Li (12:00)   │
│                                            │
│           [查看]    [稍后处理]              │
└────────────────────────────────────────────┘
```

| Button | Action |
|--------|--------|
| **[查看]** | Scheduler Drawer opens → Scroll to conflict time → Card auto-expanded |
| **[稍后处理]** | Dismiss pop-up, conflict card remains in scheduler (user handles later) |



## 3.5 Task Card Lifecycle
`Pending` (if from Voice) → `Active` → `Alarm Ringing` → `Done`

**Reminder Timing (No Picker UI):**

| User Input | Agent Action |
|------------|--------------|
| "提醒我提前5分钟" | Set single alarm at T-5min |
| "提前一小时提醒" | Set single alarm at T-60min |
| No preference | Apply Smart Cascade (agent-determined) |

**Smart Cascade (Agent-Determined, if no user preference):**

| Task Type | Cascade |
|-----------|---------|
| Meetings/Calls | T-30min (prep) → T-5min (join) |
| Deadlines | T-1h → T-15min |
| Personal/Other | T-10min |

> **Learning:** Agent observes User Habit (Prism §5.9). If user frequently snoozes first alarm, agent auto-adjusts offset.

**Card Visual States:**

| State | Card Shows |
|-------|-----------|
| User-specified | `[⏰ 5分钟前提醒]` |
| Agent-set | `[⏰ 智能提醒]` |

```
STATE: Active                        [⏰ 智能提醒] Visible
   ↓
(Time Reached) -> System Notification + Full Screen Pop-up
                  ┌────────────────────────┐
                  │ ⏰ 与张总会面          │
                  │    3:00 PM             │
                  │ [Dismiss] [Snooze 10m] │
                  └────────────────────────┘
   ↓
Tap [Dismiss] -> Task marked Done    Card State: Done (✓) Dimmed
```

## 3.6 Alarm Display (No Manual Picker)
Tap `[⏰]` on card → Shows current reminder setting (read-only). To change, user speaks: "改成提前1小时提醒".
