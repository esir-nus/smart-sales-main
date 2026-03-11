> **Senior Engineer (`/01-senior-reviewr`) & `/03-explainer`**

### 1. Did I find drift between the spec and the code?

**Yes, but it was *positive* drift (Code was better than Spec).**

While auditing `RealEntityWriter.kt` and `entity-writer/spec.md`, I found a critical architectural improvement in the code that the spec had marked as "KILLED":
*   **The Spec** (`entity-writer/spec.md L254`) says "Wave 3 Conflict Merge - KILLED". It claims that deterministic "latest-write-wins" is sufficient and history is tracked via `recordActivity()`.
*   **The Code** (`RealEntityWriter.kt L164-219`) actually implements a highly sophisticated `ProfileUpdateResult` system that tracks field-level changes (`val oldValue = when (field) ...`) and *appends* the old canonical name into the curated `aliasesJson` string via FIFO before overwriting it. It doesn't just "win"; it preserves the history inside the lookup index itself.

The code evolved to be much smarter about resolving homophones and preserving aliases than the original spec designed. We need to respect the code as the Source of Truth here.

---

### 2. The Intelligence Pipeline & Lifecycle (Explainer)

Here is the exact journey of a memory, from the user's mouth to the permanent database, and back into the AI's brain.

#### You Say: *"Follow up with Dingsheng about the 5% discount"*

### Step 1: The Input & Fast Parse
**What**: The user speaks. The Turbo model instantly extracts the physical clues (Who, What, When). *(Note: The architecture plans for a background job to start learning habits here, but it is currently [WIP]/Deferred)*.
**Where**: [`RealInputParserService.kt`](file:///home/cslh-frank/main_app/core/pipeline/src/main/java/com/smartsales/core/pipeline/RealInputParserService.kt)
**Result**: Clues extracted: `{ intent: CRM_TASK, entities: ["Dingsheng"], action: "follow up" }`

### Step 2: The Gateway (Disambiguation)
**What**: The system pauses. It checks the database to see if it knows exactly who "Dingsheng" is. If it finds "鼎盛科技" (`a-101`), it proceeds. If it's confused, it stops the pipeline and asks the user to clarify.
**Where**: [`RealEntityDisambiguationService.kt`](file:///home/cslh-frank/main_app/core/pipeline/src/main/java/com/smartsales/core/pipeline/RealEntityDisambiguationService.kt)
**Result**: Raw name mapped to physical ID: `a-101`

### Step 3: The RAM Assembly (Kernel Context Build)
**What**: The Kernel gathers the workspace. It pulls the distilled *Entity Profile* for `a-101` from the SSD, fetches your global habits, and checks your schedule. It packages this into a strict JSON format. It **does not** read raw chat logs.
**Where**: [`RealContextBuilder.kt L94-117`](file:///home/cslh-frank/main_app/core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt#L94-L117)
**Result**: `PipelineContext` is built (The "Prompt").

### Step 4: LLM Brain Execution
**What**: The heavy LLM reads the `PipelineContext` package. It figures out the best response and creates a structured JSON output (including the response text, proposed calendar events, and observed habits).
**Where**: [`UnifiedPipeline.kt`](file:///home/cslh-frank/main_app/core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt)
**Result**: Structured AI Output.

### Step 5: The Write-Backs (Twin Engines)
**What**: The pipeline is finished, but the OS must save the work. 
- The **CRM Engine** updates the entity profile (e.g., changes deal stage).
- The **RL Engine** saves the new habits (e.g., "User likes follow-ups on Fridays").
- Both engines write-through to updating the RAM session *and* saving to the SSD.
**Where**: [`RealEntityWriter.kt L221-236`](file:///home/cslh-frank/main_app/data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt#L221-L236)
**Result**: Database updated. Session ready for Turn 2.

---

### The OS Model Flow Diagram

```ascii
                               The "Anti-Poison" OS Model Intelligence Pipeline
=================================================================================================================

 [ USER INPUT ]  "Follow up with Shen..." (Ambiguous: Shen Wei or Shen Lei?)
       │           ▲
       ▼           │ (Interrupt & Resume Loop)
┌──────────────────┴──────────────── L3 ORCHESTRATOR ─────────────────────────────────────┐
│ 0. Mascot / Lightning Router (Pre-Filter)                                               │
│    - Fast check for NOISE (ambient chatter) or GREETING.                                │
│    - Filters out trash. Only passes substantive intents to Unified Pipeline.            │
└─────────────────────────────────────────┬───────────────────────────────────────────────┘
                                          │ (Substantive CRM Intent)
                                          ▼
┌─────────────────────────────── KICKOFF ─────────────────────────────────────────────────┐
│                   │                                           │                         │
│             (Foreground Path)                           (Background Path)               │
│                   ▼                                           ▼                         │
│ ┌────────────────────────────────────────┐     ┌──────────────────────────────────────┐ │
│ │ 1. EntityDisambiguator (Gateway App)   │     │ RL Subsystem (Habit Listener) [WIP]  │ │
│ │ - FAILS exact match against DB and     │     │ - *Planned:* Fire-and-forget async   │ │
│ │   the `aliasesJson` library.           │     │   learning loop. Currently a fake    │ │
│ │ - Detects: "Shen" could be c-101/c-102.│     │   masquerade until Wave 4/5.         │ │
│ │ - ACTION: Halts pipeline. Yields back  │     └───────────────────┬──────────────────┘ │
│ │   to user ("Which Shen?").             │                         │                    │
│ │ - User clarifies: "At Microsoft".      │                         │                    │
│ │ - RESULT: Contextually maps to c-102.  │                         │                    │
│ │   (`info_sufficiency` is now met!)     │                         │                    │
│ └──────────────────────┬─────────────────┘                         │                    │
└────────────────────────┼───────────────────────────────────────────│────────────────────┘
                         │ (Canonical ID: c-102)                     │
                         ▼                                           │
┌────────────────────────────────── THE KERNEL (RAM) ────────────────│──────────────────┐
│ 2. ContextBuilder (SessionWorkingSet Assembly / ETL)               │                    │
│    - Fast SQL Query: Loads Distilled Profile for c-102 from SSD.   │                    │
│    - Fast SQL Query: Loads General User Habits from SSD.           │                    │
│    - Fast SQL Query: Loads Specific Client Habits for c-102.       │                    │
│    - Fast SQL Query: Loads Kanban/ScheduleBoard.                   │                    │
│    - Packages pure, clean JSON for the LLM.                        │                    │
└─────────────────────────────────────────┬──────────────────────────│────────────────────┘
                                          │ (Clean Context Payload)  │
                                          ▼                          │
┌───────────────────────────────── THE BRAIN (LLM) ──────────────────│──────────────────┐
│ 3. UnifiedPipeline                                                 │                    │
│    - [EXAMPLE OF LLM REASONING ON COMPILED CONTEXT]                │                    │
│    - CRM DECISION: "When should I schedule the follow-up?"         │                    │
│      ├─ [Profile]: c-102 is a VIP client (needs fast action).      │                    │
│      ├─ [Habit]: Client prefers Friday morning calls.              │                    │
│      └─ [Schedule]: I am already booked on Friday morning.         │                    │
│    - Outputs Structured Commands: "Schedule for Thursday 4PM."     │                    │
└─────────────────────────────────────────┬──────────────────────────│────────────────────┘
                                          │                          │ (Async writes sync)
                                          ▼                          ▼
┌───────────────────────────────── TWIN WRITERS (RAM Ops) ──────────────────────────────┐
│ 4a. RL Engine (Habit Writes) [WIP]     │ 4b. EntityWriter (CRM Writes)                    │
│ - *Planned:* Completes async learning  │ - Explicit state: Update `nextAction` field on   │
│   "Likes 5% discounts" & binds to ID.  │   c-102 to "Follow up Thursday 4PM".             │
│ - Write-Through Strategy:              │ - Timeless log: Emits `NEXT_ACTION_SET` activity │
│   ├─ Updates RAM immediately.          │   to the Kernel history timeline.                │
│   └─ Async writes to SSD.              │ - Write-Through Strategy:                        │
│                                        │   ├─ Updates RAM immediately.                    │
│                                        │   └─ Fast SQL update to SSD.                     │
└────────────────────────────────────────┴────────────────────────────────────────────────┘
                                          │ (Safe Persist)
                                          ▼
=================================================================================================================
                                  LONG TERM STORAGE (SSD)
                      EntityRegistry (No ASR Trash) | UserHabitRepository
=================================================================================================================
```

By using the `WorldStateSeeder` to inject fragmented memories, we verify that this entire 5-step engine—specifically the Disambiguator (Step 2) and exactly what the LLM reads during Context Assembly (Step 3)—works under pressure.
