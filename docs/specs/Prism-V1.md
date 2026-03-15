# Prism Architecture (V1)

**Status:** Draft / Active
**Date:** 2026-01-21
**Successor to:** Lattice Architecture

---

## Table of Contents

| Section | Description | Lines |
|---------|-------------|-------|
| [§1 Executive Summary](#1-executive-summary) | Core principles, unified pipeline overview | 9-22 |
| [§2 Project Mono Philosophy & Lifecycle](#2-project-mono-philosophy--lifecycle) | Brain/Body Disconnect, One Currency Rule, Validation Gates | 29-75 |
| [§3 Architecture Blueprint](#3-architecture-blueprint) | Pipeline diagram, Core Components, Model Router, Auto-Quote | 76-340 |
| [§4 Data Flow & Consistency](#4-data-flow--consistency) | Buffered streaming, consistency model | 341-390 |
| [§5 Mode Pipelines](#5-mode-pipelines-ascii-visualization) | Coach, Analyst, Scheduler mode flows | 391-487 |
| [§6 Memory System](#6-memory-system--relevancy-library) | Hot/Cement Zones, RelevancyEntry schema, Entity Disambiguation, User Habits | 488-863 |
| [§7 Crash Recovery](#7-crash-recovery--checkpoints) | Local Recovery Queue, checkpoints | 864-927 |
| [§8 Testability Contract](#8-testability-contract-lattice-compliance) | Lattice compliance rules | 928-964 |
| [§9 Extension Guidelines](#9-extension-guidelines) | Future extensibility | 965-974 |
| [§10 Pipeline Valve Protocol](#10-pipeline-valve-protocol-observable-architecture) | SOT for Data Observability | 1000-1030 |
| [§11 Relationship to Lattice](#11-relationship-to-lattice) | Architecture evolution | 975-987 |
| [Appendix A: Changelog](#appendix-a-changelog) | Version history | 988-1002 |
| [Appendix B: Memory Trinity](#appendix-b-memory-trinity-overview) | Memory layer diagram | 1003-1042 |
| [Appendix C: Hybrid RAG](#appendix-c-hybrid-rag-strategy) | RAG integration strategy | 1043-1061 |

---

## 1. Executive Summary

**Prism** is the architectural standard for the Intelligent Personal Assistant system. It replaces complex, stateful coordination with a **Unified Pipeline** that delegates specific behaviors to **Mode Strategies**.

The name "Prism" reflects the core pattern: a single stream of user intent enters the system and is "refracted" into specific execution paths (Chat, Analyze, Schedule) by the Orchestrator, while sharing a common foundation of Context, Memory, and Intelligence.

### Core Principles
1.  **Unified Pipeline**: All modes share the same high-level flow (Context → Execute → Publish → Persist).
2.  **Strategy Pattern**: Mode-specific logic (LLM selection, Rendering, Tools) is encapsulated in switchable strategies.
3.  **Fire-and-Forget Persistence**: Memory writes happen in the background. The User Interface never blocks on database operations.
4.  **Streaming First**: The architecture is built around native streaming (DashScope Flow<Token>) for minimal latency.

---

## 2. Project Mono Philosophy & Lifecycle

**Project Mono** resolves the "Brain/Body Disconnect" by fundamentally changing the contract between the AI Engine (The Brain) and the Android Application (The Body). This is the governing philosophy of the Prism architecture.

### 2.1 The "One Currency" Rule (The Illusion of Interfaces)
Previously, modules were connected by clean interfaces (e.g., `fun process(json: String)`), but they traded in "multiple currencies" (raw Strings, custom JSON, regex). Because the data was shapeless, the LLM was forced to act as the Currency Exchange, guessing what each module required, causing Ghosting (the LLM hallucinated fields a downstream module couldn't parse).

In Project Mono, **there is only one currency**: The strictly typed SSD Kotlin `data class`. 
- **Module B (Database)** defines the currency.
- **Module A (Prompt/Brain)** is forced to use that exact currency via schema generation.
- **The Linter (Teller)** verifies the currency via strict deserialization. This eliminates hallucination at the boundary.

### 2.2 The Strict Cerb Lifecycle
Every module migrating to Project Mono **MUST** follow this exact lifecycle. Bypassing these steps is an automatic failure.

1. **Docs/Specs**: The feature spec (`docs/cerb/[feature]/spec.md`) must be written/updated defining the exact Data Contract (the fields that mutate).
2. **Interface Map**: `docs/cerb/interface-map.md` must be updated to denote the new API boundary.
3. **Plan**: Execute the `/feature-dev-planner` workflow to generate the implementation plan based strictly on the Docs.
4. **Execute**: Write the code, ensuring pure Kotlin `data classes` live in the `:domain` layer without Android imports.
5. **E2E Test**: The migration must withstand a full end-to-end lifecycle test. If the full pipeline isn't built yet, use `WorldStateSeeder` to inject a simulated fragment. The code cannot be marked SHIPPED without a passing E2E log.

### 2.3 What to Check (Validation Gates)
When reviewing a Project Mono PR or Plan, verify the following:

- **No Hardcoded Schemas**: If you see `{ "deal_stage": "string" }` hardcoded inside a Prompt string, **Reject it**. It must say `json.dumps(QuoteMutation.model_json_schema())` or the Kotlin equivalent via `kotlinx.serialization`.
- **Domain Purity**: Are the Mutation Data Classes inside pure Kotlin `:domain` modules? If they contain `import android.*`, **Reject it**.
- **Linter Simplicity**: If the Linter contains regex (`Regex("date=.*")`) or manual string-parsing math to figure out the LLM's intent, **Reject it**. It should be a 1-line `decodeFromString()`.
- **JSON Coercion Resilience**: All `PrismJson` instances parsing LLM outputs MUST set `coerceInputValues = true`. The LLM frequently hallucinates explicit `null` tokens.
- **Defensive Deserialization (Enum Safety)**: Never use standard `enumValueOf<T>()` or `T.valueOf()` when mapping strings from the Room Database to Kotlin Enums. Use `safeEnumValueOf<T>(value, fallback)`.
- **Domain vs UI State Decoupling**: Pure Domain Kotlin `data classes` represent the factual SSD truth. They must NEVER be overloaded with UI-specific rendering flags (like `tipsLoading`, `isExpanded`, or `amberGlow`).

---

## 3. Architecture Blueprint

### 3.1 The CQRS Dual-Engine Pattern (Master UJM)

To understand Project Mono's "Intelligence," an agent must understand the **Dual-Loop Architecture** during a Chat Session. We strictly decouple the high-speed "Sync/Query" loop from the slow, complex "Async/Command" loops.

#### Architectural ASCII Map

```text
================================================================================================
                        SYNC LOOP (Fast Query & RAM Assembly)
================================================================================================
                                      
[User Input] 
      │
      ▼
┌───────────────┐     [NO NAME]     ┌──────────────────┐
│ Lightning     ├──────────────────►│ Clarification    │──► (Halt: Ask User "Who?")
│ Router        │                   │ Minor Loop       │
└───────┬───────┘                   └──────────────────┘
        │
   [CANDIDATE]
        │
        ▼
┌───────────────┐   [MULTIPLE]      ┌──────────────────┐
│ Alias Lib     ├──────────────────►│ Disambiguation   │──► (Halt: Yield Options to User)
│ (L1 Cache)    │   [MISSING]       │ Minor Loop       │
└───────┬───────┘                   └──────────────────┘
        │
    [EXACT ID]
        │
        ▼
┌───────────────┐
│ SSD Graph     │ (Heavy SQL Fetch)
│ Query         │
└───────┬───────┘
        │
        ▼
┌───────────────┐
│ Living RAM    │◄──────────┐ (Injected Context for Future Turns)
│ Assembly      │           │
└───────┬───────┘           │
        │                   │
        ▼                   │
┌───────────────┐           │
│ LLM Brain     │           │
│ Generation    │           │
└───────┬───────┘           │
        │                   │
        ▼                   │
 [Chat Response]            │
                            │
============================│===================================================================
                        ASYNC LOOPS (Background Mutations)
============================│===================================================================
                            │
   [Entity Mutated]         │ 
        │                   │
        ▼                   │
┌───────────────┐           │
│ decodeFromStr │ (Linter)  │
└───────┬───────┘           │
        │                   │
        ▼                   │
┌───────────────┐           │
│ SSD Write /   │───────────┘
│ Entity Merge  │
└───────────────┘

┌───────────────┐
│ RL Module     │ (Listens passively to user input for async habit updates)
└───────────────┘
```

#### A. The Sync Loop (Fast RAM Assembly & Query)
This is the main interaction pipeline. Its entire goal is to build context (RAM) from the SSD with zero LLM hallucination and sub-second latency *before* generation begins.

1. **Lightning Router (Gatekeeper)**: Does this input contain an "Entity Candidate"? If NO -> trigger the Clarification Minor Loop.
2. **Alias Lib (L1 Cache)**: Given the Entity Candidate, look up the `EntityID` in the fast, in-memory alias dictionary.
3. **Disambiguation Minor Loop**: If multiple hits -> Suspend pipeline. Yield to user.
4. **SSD Graph Fetch -> RAM**: Query the heavy SQL SSD. Pull the target's entire relationship graph into Living RAM.
5. **The Brain Acts (LLM)**: Generates the chat response using verified intelligence.

#### B. The Async Loops (Background Mutations & Commands)
The LLM does NOT directly write to the database in the critical UX path. Writes are asynchronous events.

1. **Decoupled Entity Writing & Merging**: If the LLM determines an Entity mutation occurred, it emits a strict Kotlin `data class`. The Background Writer runs the `decodeFromString` Linter, mutates SSD, and merges back into RAM.
2. **Decoupled Reinforcement Learning (RL)**: Every user turn is quietly copied to the RL Module to update Habits independently.

---

### 3.2 The Unified Pipeline

```
┌─────────────────────────────────────────────────────────────────────┐
│                    UNIFIED ORCHESTRATION PIPELINE                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────┐   ┌──────────────┐   ┌───────────────┐   ┌──────────┐ │
│  │ Context │──▶│   Executor   │──▶│   Publisher   │──▶│   UI     │ │
│  │ Builder │   │    (LLM)     │   │   (Per Mode)  │   │          │ │
│  └─────────┘   └──────────────┘   └───────────────┘   └──────────┘ │
│       │               │                   │                         │
│       │               │                   │      ┌──────────────┐  │
│       │               │                   └─────▶│ Tool Agents  │  │
│       │               │                          │ (If Needed)  │  │
│       │               │                          └──────────────┘  │
│       │               │                                             │
│       │               ▼                                             │
│       │        [Memory Writer] ──fire & forget──▶ [Hot Zone]       │
│       │                                                             │
│       ▼                                                             │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                    MODE STRATEGY MATRIX                      │   │
│  ├────────────┬───────────────┬──────────────────┬─────────────┤   │
│  │ Component  │    CHAT       │     ANALYZE      │  SCHEDULE   │   │
│  ├────────────┼───────────────┼──────────────────┼─────────────┤   │
│  │ Executor   │ Lightweight   │ Reasoning Model  │ Structured  │   │
│  │ (LLM)      │ (qwen-turbo)  │ (qwen-max3)      │ Output LLM  │   │
│  ├────────────┼───────────────┼──────────────────┼─────────────┤   │
│  │ Publisher  │ ChatPublisher │ AnalystPublisher │ SchedPub    │   │
│  │            │ (stream +     │ (format chapters │ (calendar   │   │
│  │            │  context      │  insights, call  │  entry +    │   │
│  │            │  aware)       │  chart tools)    │  smart tips)│   │
│  ├────────────┼───────────────┼──────────────────┼─────────────┤   │
│  │ Tools      │ None          │ Charts, Reports  │ Calendar,   │   │
│  │            │               │ Recommendations  │ Tip Writer  │   │
│  └────────────┴───────────────┴──────────────────┴─────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Core Components

#### 1. Context Builder (Shared)
The foundation of intelligence. Runs for **every** request regardless of mode.

**Role:** Normalize heterogeneous inputs into a unified context payload for the Executor LLM.

**Input Normalization Tools:**

All user inputs are processed via specialized tools before reaching the Planner/Executor:

| Input Type | Tool | Output | Notes |
|------------|------|--------|-------|
| **Text** | — (passthrough) | Raw text | Direct injection |
| **Audio** | `tingwu.transcribe` | Transcript + metadata | Speaker diarization, timestamps |
| **Image** | `qwen_vl.analyze` | OCR text / scene description | Supports batch (up to 5) |
| **Video** | `qwen_vl.video` | Scene analysis / key frames | Future capability |
| **URL** | `fetch.text` | Page content | Web content extraction |

> **Architectural Note:** In the old architecture, Tingwu was a standalone workflow. In Prism, it is **demoted to a tool** that feeds the Context Builder. All modalities are now unified input sources, not destinations.

> **Future Note:** Tingwu provides native structured outputs (mindmaps, summaries, chapters, speaker key points) via its API. These can be exposed as **structured tool outputs** for the Planner, enabling richer downstream tool composition. To be explored in a future iteration.

**Context Payload:**
```kotlin
data class EnhancedContext(
    val userText: String,
    val audioTranscripts: List<TranscriptBlock>,  // From Tingwu
    val imageAnalysis: List<VisionResult>,        // From Qwen-VL
    val memoryHits: List<MemoryEntry>,            // From Hot Zone
    val entityContext: Map<String, Entity>,       // From Relevancy Library
    val modeMetadata: ModeMetadata
)
```

**Inputs:** User query, current screen context, User Profile (§5.8), attached media.
**Memory Query:** Hot Zone (active entries) + Relevancy Library (entity context).
**Output:** `EnhancedContext` object for Planner/Executor.

#### 1b. Session Cache (In-Task Memory)

Lightweight in-memory cache for fast context access during task execution. Avoids heavy push/pull on persistent memory layers.

| Layer | Role | Lifetime |
|-------|------|----------|
| **Session Cache** | Fast in-memory store for ongoing task context | Current task execution |
| **Relevancy Library** | Persistent structured memory | Cross-session |

**Write Pattern:**
```
Tool Output ──▶ Session Cache (sync, fast)
                    │
                    └── Async Delta Push ──▶ Relevancy Library / Hot Zone
```

1. Tool outputs → Session Cache (synchronous, in-memory)
2. Cache → Relevancy Library (async delta update, on boundary triggers)

**Flush Triggers:**
| Trigger | Action |
|---------|--------|
| Plan Card completion | Flush entire cache to memory |
| User sends new message | Flush cache, rebuild for new turn |
| App backgrounded > 30s | Flush cache for session continuity |

**Benefits:**
- Task execution reads from cache (fast, no DB queries)
- Memory stays fresh via delta updates
- Lightweight: no heavy push/pull during tool execution

> **Consistency Note:** During task execution, Session Cache is the source of truth. A brief consistency window exists until the next flush trigger. This is accepted practice to ensure zero-latency tool execution.

#### 2. Orchestrator (The Prism)
Stateless router. Selects mode and invokes the pipeline.

**Mode Selection Rules:**

| Trigger | Mode | Control |
|---------|------|---------|
| **Toggle Switch** | Coach ↔ Analyst | User manual |
| **ESP32 Event** (`record#end` + filename) | Scheduler | System trigger (see [esp32-protocol.md](./esp32-protocol.md)) |

**No Auto-Switch Rule:**
-   Agent may **suggest** a mode switch based on parsed intent (e.g., "This looks like a scheduling request").
-   Agent **never** auto-switches modes without user confirmation.
-   User must manually switch via toggle or confirm the suggestion.

**Behavior:**
-   Maintains ephemeral in-turn state (scope).
-   Invokes the pipeline steps based on current mode.

#### 3. Executor (Strategy-Based)
The brain. Implementation varies by mode (`LlmProvider` interface).
-   **Chat:** Uses fast, conversational models (e.g., Qwen Turbo). Focus: Engagement.
-   **Analyze:** Uses reasoning-heavy models (e.g., Qwen Max). Focus: Depth, Tool Calling.
-   **Schedule:** Uses structured output models. Focus: JSON correctness for calendar actions.

> **Note:** Analyst mode uses a **Planner LLM** before the main Executor to generate a visible task list. See §4.5 Plan-Once Execution and §4.6 Analyst Planner-Centric Paradigm.

#### 4. Publisher (Strategy-Based)
The output shaper. Implementation varies by mode (`ModePublisher` interface).
-   **ChatPublisher:** Streams tokens directly to UI bubles.
-   **AnalystPublisher:** Accumulates tokens, formats into Chapters/Insights, renders Chart Cards.
-   **SchedulePublisher:** Parses JSON, calls Calendar API, renders "Smart Tips" (conflicts/context).

#### 5. Memory Writer (Background)
The memory system. Writes to Hot Zone and triggers Relevancy Library update.
-   **Pattern:** Fire-and-Forget with Retry.
-   **Write-Through:** After Hot Zone write, triggers Relevancy Writer to update Relevancy Library.
-   **Behavior:** Runs in a detached CoroutineScope (`GlobalScope` or application lifecycle scope).
-   **Retry:** Exponential backoff for SQLite/Room writes.
-   **Invariant:** UI **never** waits for memory writes.

#### 6. Schema Linter (Hardcoded Quality Gate)
Validates ALL AI-generated structured outputs before persistence. **No LLM in the validation loop** — purely deterministic.

**Multiple Linter Patterns:**

| Linter | Output Type | Validates |
|--------|-------------|-----------|
| `EntityLinter` | Chat/Analyst `structuredJson` | JSON valid, entity types, ID format, required fields |
| `PlanLinter` | ExecutionPlan | Required fields, valid enums, step structure |
| `SchedulerLinter` | Scheduler JSON | Schema compliance, date validity, conflict detection |
| `RelevancyLinter` | Relevancy updates | Entity type, field constraints, alias uniqueness |

**Failure Behavior:**
1. On lint failure → Return errors to agent
2. Agent regenerates/fixes the output
3. Retry until linter passes (max 3 attempts)
4. If still fails → Log error, skip persistence, notify user

```kotlin
interface LinterRegistry {
    fun getLinter(outputType: OutputType): Linter
}

sealed class OutputType {
    object EntityExtraction : OutputType()
    object ExecutionPlan : OutputType()
    object SchedulerCommand : OutputType()
    object RelevancyUpdate : OutputType()
}

interface Linter {
    fun validate(content: String): LintResult
}

sealed class LintResult {
    object Pass : LintResult()
    data class Fail(val errors: List<LintError>) : LintResult()
}
```

#### 7. Memory Center Notifier (Snackbar Updates)
Dedicated UI component for notifying users of significant memory updates. Covers 4 categories:

**Notification Format:** `[Category]已更新：<truncated content>`

| Category | Prefix | Example |
|----------|--------|---------|
| **User Habit** | `习惯已更新：` | `习惯已更新：<早上安排会议>` |
| **Client Profile** | `客户已更新：` | `客户已更新：<张总的偏好>` |
| **User Scheduler** | `日程已更新：` | `日程已更新：<周三 9:00 会面>` |
| **User Profile** | `资料已更新：` | `资料已更新：<语言偏好>` |

**Behavior:**
-   Async observer on Hot Zone + Relevancy Library writes.
-   Does not block main pipeline.
-   Snackbar auto-dismisses after 3 seconds (tappable to expand details).
-   `<content>` is truncated to 20 characters max.

### 2.3 Mode-Specific Context Strategies

Context Builder applies different retrieval strategies based on the active mode:

#### Scheduler Mode
```
User Input (transcribed audio)
    ↓
Step 1: Check current Inspirations + Scheduled Tasks (today's scope)
    ↓
Step 2: Query Relevancy Library for entity matches (fast O(1))
    ↓
Step 3: Apply 14-day rule:
    - If lastMentionedAt < 14 days → check Hot Zone
    - If lastMentionedAt ≥ 14 days → check Cement Zone
    ↓
Step 4: Build context with pointers from Relevancy Library
```

#### Coach Mode
```
User Input
    ↓
Step 1: In-session chat history (primary context)
    ↓
Step 2: Context enrichment (deterministic):
    ├─ First turn of session → MemoryRepository.search() for relevant context
    ├─ Entity mentions detected → EntityRepository.findByAlias() for known entities
    └─ Subsequent turns → session context is sufficient (no re-search)
    ↓
Step 3: Analyst suggestion is an output flag from the main LLM call
        (not a pre-execution LLM check)
```

**Coach Prompt Checks:**

| Check | Purpose | User Control |
|-------|---------|--------------|
| **Analyst Suggestion** | Detects analysis-heavy requests | Output flag from response (User confirms) |
| **Memory Search** | Provides historical context | Triggered on first turn or entity state change |

#### Analyst Mode
```
User Input
    ↓
Step 1: Query Relevancy Library (entity context, decision history)
    ↓
Step 2: Query Hot Zone (recent relevant entries)
    ↓
Step 3: Generate ExecutionPlan via Planner LLM (§4.5)
    ↓
(Rest follows Plan-Once paradigm)
```

### 2.3 Model Router

Central routing for LLM model selection based on **task type**, not mode.

> **Location**: `domain/config/ModelRouter.kt`

#### Task-Based Routing (`forContext`)

| Input Condition        | Model          | Context Window |
|-----------------------|----------------|----------------|
| Image/Video present    | `qwen-vl-plus` | Vision         |
| Tool-calling required  | `qwen3-max`    | 32k tokens     |
| Default (fast chat)    | `qwen-plus`    | 1M tokens      |

#### Memory Layer Routing (`forMemoryLayer`)

| Layer      | Model       | Rationale                                |
|------------|-------------|------------------------------------------|
| RELEVANCY  | `qwen3-max` | Tool-calling for entity search           |
| HOT        | `qwen-plus` | Fast index navigation (14 days context)  |
| CEMENT     | `qwen-long` | Deep history retrieval (10M tokens)      |

> **Pattern**: Blackboxes call `ModelRouter` to get model string. Routing logic is centralized, not scattered across implementations.

---

### 2.4 Auto-Quote Module

> **[FUTURE]** Universal module for automatic excerpt quoting from historical context.

When context retrieval returns relevant entries, this module:
- Extracts key excerpt from matched entry
- Formats as inline quote in agent response
- Links to source entry for user navigation

*Implementation deferred. Applies to all modes.*

---

## 3. Data Flow & Consistency

### 3.1 Buffered Streaming Flow

**Trade-off:** We cannot use true native streaming because the Linter must validate structured output BEFORE displaying to user. Instead, we use **buffered streaming with simulated animation**.

```
Orchestrator ──▶ DashScope API (stream=true)
                       │
                       ▼
               [Response Buffer] ──accumulate tokens──▶ Complete Response
                       │
                       ▼
               [Linter] ──validate structured section──▶ Pass/Fail?
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
       Pass                        Fail
         │                           │
         ▼                           ▼
[Publisher]                    [Retry] → agent regenerates
  │                            (max 3 attempts)
  ├─▶ Simulated streaming animation (displayContent)
  └─▶ [Memory Writer] → Hot Zone (async)
```

**Simulated Streaming:**
```kotlin
// After linter passes, animate displayContent at 20ms per character
suspend fun simulateStreaming(content: String, charDelayMs: Long = 20) {
    content.forEach { char ->
        emit(char)
        delay(charDelayMs)
    }
}
```

**Why this works:**
- User still sees "typing" animation — feels responsive
- All structured data is validated before any display
- Retry happens invisibly if linter fails

### 3.2 Consistency Model

| Dimension | Guarantee | Implication |
|-----------|-----------|-------------|
| **UI Responsiveness** | **Evaluate-Now** | UI always shows immediate AI response. |
| **Memory Persistence** | **Eventual** | Writes happen milliseconds/seconds after response. |
| **Cross-Session** | **Strong** | Next session guaranteed to see previous session's commits. |
| **Intra-Session** | **Read-Your-Writes** | *Best effort.* Fast follow-up turns *may* run before previous turn's write completes. |

**Accepted Tradeoff:** In ultra-fast conversational turns, the memory context *might* be slightly stale (missing the immediately preceding turn). This is accepted industrial practice to ensure zero UI blocking.

---

## 4. Mode Pipelines (ASCII Visualization)

### 4.1 Chat Mode (The Coach)
Lightweight conversational path with optional memory search. See **§2.3 Coach Mode** for context strategy details.

```
User Input
    │
    ▼
[Context Builder: Session History]
    │
    ├─▶ LLM Check A: Suggest Analyst switch? ──(if yes)──▶ Prompt user
    │
    ├─▶ LLM Check B: Memory threshold score
    │       │
    │       ├─▶ Score > threshold: Search Relevancy Library
    │       │                      (emit "Searching memory...")
    │       └─▶ Score ≤ threshold: Stay with session context
    │
    ▼
[Qwen Turbo] ──stream──▶ [ChatPublisher] ──▶ UI
                              │
                              └─async─▶ [Memory Writer] ──▶ Hot Zone
```

### 4.2 Analyze Mode (The Analyst)
Heavy reasoning path with Thinking Trace and Planner Table. See **§2.3 Analyst Mode** and **§4.5-4.6** for planning details.

```
User Input
    │
    ▼
[Context Builder: Relevancy Library + Hot Zone]
    │
    ▼
[Qwen Max (Thinking)] ──stream──▶ [Thinking Trace UI]
    │
    ▼
[Qwen Max (Structured)] ──stream──▶ [Planner Table (Chat Bubble)]
    │                                  │
    │                            [Tool Execution]
    │                                  │
    └─async─▶ [Memory Writer] ──▶ Hot Zone
```

### 4.3 Schedule Mode (Global Top Drawer)
Structured command path with multi-step context. See **§2.3 Scheduler Mode** for context strategy and [scheduler-v1.md](./scheduler-v1.md) for full spec.

```
Voice Note (ESP32 trigger)
    │
    ▼
[Context Builder]
    ├─▶ Step 1: Check Inspirations + Scheduled Tasks
    ├─▶ Step 2: Query Relevancy Library (entity match)
    └─▶ Step 3: Apply 14-day rule (Hot vs Cement)
    │
    ▼
[Qwen Structured] ──json──▶ [SchedulePublisher]
    │                              │
    │                       [Calendar API]
    │                       [Tips Renderer]
    │                              │
    └─async─▶ [Memory Writer] ─────┴──▶ UI
```

### 4.5 Plan-Once Execution Model

Prism minimizes latency and token costs with a **Plan-Once** model: one comprehensive planning call per turn.

**The 3-Step Lifecycle:**
1. **Pre-Fetch (Deterministic)**: Load Hot Zone Snapshot + invoke Relevancy Checker (Radar) for O(1) context.
2. **Planning (LLM)**: Planner LLM outputs structured `ExecutionPlan`.
3. **Execute & Respond**: Execute plan, generate output with native structured tags.

```kotlin
data class ExecutionPlan(
    val retrievalScope: RetrievalScope,  // NONE, HOT_ONLY, HOT_AND_CEMENT, DEEP
    val toolsToInvoke: List<ToolCall>,
    val deliverables: List<DeliverableType>,
    val workflowSuggestion: WorkflowType?,
    val responseType: ResponseType
)

enum class RetrievalScope { NONE, HOT_ONLY, HOT_AND_CEMENT, DEEP }
```

### 4.6 Agent Visibility System

> **Philosophy**: The user should *feel* the agent is smart. This section defines how intelligence is made visible.
>
> For detailed audit protocols, see `/agent-visibility` workflow.

The Analyst workflow uses a **three-layer visibility architecture** to showcase agent cognition:

| Layer | Component | Purpose |
|-------|-----------|---------|
| **1. Cognition** | **Thinking Trace** | Inline raw reasoning streamed before response. |
| **2. Structure** | **Planner Table** | Self-updating markdown table within a chat bubble. |
| **3. Access** | **Task Board** | Sticky top row of shortcut buttons. |

```
┌─────────────────────────────────────────┐
│  📊 Sales   📈 Competitor   [+] Custom  │ ← Task Board (Sticky)
├─────────────────────────────────────────┤
│  [AI]                                   │
│  ┌── Thinking ───────────────────────┐  │ ← Thinking Trace
│  │ Checking Q4 data...               │  │
│  └───────────────────────────────────┘  │
│                                         │
│  | Step | Task | Status |               │ ← Planner Table (Bubble)
│  |------|------|--------|               │
│  | 1    | Data | ✅     |               │
│  | 2    | Viz  | ⏳     |               │
└─────────────────────────────────────────┘
```

#### 4.6.1 Analyst Pipeline Orchestration

The Analyst mode uses a **stateful controller** to orchestrate multi-step analysis flows. This is a **pipeline mechanism**, not a visibility concern — the visibility is handled by `AgentActivityController` (§4.6.2).

**Design Rationale**: Instead of "Boolean Soup" (`isThinking`, `isParsing`, `isRunning`) scattered across a ViewModel, we use a sealed hierarchy managed by a Controller.
- **Debuggability**: Log every state change in one place.
- **Testability**: Unit test the Controller without mounting UI.
- **Reusability**: Decoupled from `ChatViewModel`.

**State Hierarchy:**
```kotlin
sealed interface AnalystState {
    data object Idle : AnalystState
    data class Parsing(val currentTask: String, val progress: Float) : AnalystState
    data class Planning(val trace: List<String>) : AnalystState
    data class Proposal(val plan: AnalystPlan, val queue: List<String> = emptyList()) : AnalystState
    data class Executing(val plan: AnalystPlan, val currentStepId: String) : AnalystState
    data class Result(val artifact: PlanArtifact) : AnalystState
}
```

**Controller Contract:**
```kotlin
class AnalystFlowController @Inject constructor() {
    val state: StateFlow<AnalystState>
    suspend fun startAnalysis(input: String)
    suspend fun confirmPlan()
    fun handleInterruption(msg: String)
}
```

**Interruption Handling (Queueing):**
If user sends input during `Planning` or `Parsing` state, the message is queued in the `Proposal.queue` and processed after the current flow settles. This avoids race conditions.

#### 4.6.2 Visibility Mechanism: AgentActivityController

The **AgentActivityController** is the PRIMARY visibility mechanism — it makes agent cognition visible to users via the **AgentActivityBanner**.

> **Hierarchy**: Visibility channels are the **first debugging entry point**. If the app "feels dumb", audit visibility first, then pipeline logic.

**Two-Tier Structure:**

| Layer | Role | Example |
|-------|------|---------|
| **Phase** | High-level task (always visible) | "📝 规划分析步骤", "⚙️ 执行工具: PDF生成" |
| **Action** | Specific operation (optional) | "🧠 思考中...", "📚 检索记忆..." |
| **Trace** | Streaming content (optional) | Native CoT, transcript, memory hits |

**State Hierarchy:**
```kotlin
enum class ActivityPhase {
    PLANNING,      // 📝 规划...
    EXECUTING,     // ⚙️ 执行工具...
    RESPONDING,    // 💬 生成回复...
    COMPLETED,     // ✅ 思考完成（持久化展示）
    ERROR          // ⚠️ 发生错误
}

enum class ActivityAction {
    THINKING,      // 🧠 思考中... (Qwen3-Max CoT)
    PARSING,       // 📄 解析中... (Qwen-VL)
    TRANSCRIBING,  // 🎙️ 转写中... (Tingwu)
    RETRIEVING,    // 📚 检索记忆... (Relevancy)
    ASSEMBLING,    // 📋 整理上下文...
    STREAMING      // ✨ 生成中...
}

data class AgentActivity(
    val phase: ActivityPhase,
    val action: ActivityAction? = null,
    val trace: String? = null
)
```

**Trace Sources:**

| Source | Type | Details |
|--------|------|---------|
| **Qwen3-Max CoT** | Native | `enable_thinking` returns real reasoning trace |
| **Qwen-VL** | Native | Vision model streaming output |
| **Tingwu** | Native | Real-time transcript (pseudo-thinking) |
| **Relevancy Library** | Synthetic | Show matched entities/memories |
| **Context Assembly** | Synthetic | Show assembled sources |

**Controller Contract:**
```kotlin
class AgentActivityController @Inject constructor() {
    val activity: StateFlow<AgentActivity?>
    fun startPhase(phase: ActivityPhase, action: ActivityAction? = null)
    fun updateTrace(line: String)
    fun complete()
}
```

> **UI Reference**: See [AgentActivityBanner.md](./components/AgentActivityBanner.md) for rendering rules.

#### 4.6.3 ThinkingPolicy

**All Qwen models use `enable_thinking=true`**. The policy controls UI display truncation based on mode:

```kotlin
object ThinkingPolicy {
    fun maxTraceLines(mode: Mode): Int = when (mode) {
        Mode.COACH -> 3       // Quick, truncated — "feels understood"
        Mode.ANALYST -> 20    // Full trace — transparency matters
        Mode.SCHEDULER -> 5   // Moderate
    }
}
```

**Model Thinking Behavior:**

| Model | API Param | Trace Field | Use Case |
|-------|-----------|-------------|----------|
| Qwen3-Max | `enable_thinking=true` | `reasoning_content` | Analyst (full CoT) |
| Qwen-Plus | `enable_thinking=true` | `reasoning_content` | Coach (truncated) |
| Qwen-VL | `enable_thinking=true` | `reasoning_content` | Vision parsing |

> **Note**: All models return thinking traces. UI truncates per `ThinkingPolicy`, not the API.

#### 4.6.4 Simulated Streaming Architecture

**Native Qwen streaming is NOT used.** The system uses a **linter-first** pattern:

```
┌─────────────────────────────────────────────────────────┐
│ 1. LLM Call (non-streaming)                             │
│    → Returns complete response + thinking trace         │
└───────────────────────┬─────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Linter validates response                            │
│    → Structured output check, toxicity, etc.            │
└───────────────────────┬─────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────┐
│ 3. AgentActivityController.complete()                   │
│    → Thinking phase ends, banner collapses              │
└───────────────────────┬─────────────────────────────────┘
                        ▼
┌─────────────────────────────────────────────────────────┐
│ 4. SimulatedStreamer                                    │
│    → Emit chars at 20ms/char for "typing" effect        │
│    → UI shows progressive text rendering                │
└─────────────────────────────────────────────────────────┘
```

**Benefits:**
- **Reliability**: Full response validated before display
- **Consistency**: Same UX whether LLM streams or not
- **Control**: Exact typing speed tuned for UX



### 4.7 Conflict Resolution (Rethink Model)

Conflicts are treated as **creative prompts**, not merge decisions.

**Detection → Suggestion → Resolution:**
1. **Detection**: Relevancy Checker scans for time/resource overlaps.
2. **Rethink Suggestion**: Agent proposes a "Third Option" (e.g., "Combine into joint meeting").
3. **User Choice**: Override, Merge, or type a **Rethink** instruction.

| Choice | Action |
|--------|--------|
| **Override** | Replace conflicting entry |
| **Merge** | Combine both entries |
| **Rethink** | User types custom resolution → creates new entry |

**Rethink decisions are logged** to `decisionLogJson` in the Relevancy Library for future learning.

---

## 5. Memory System & Relevancy Library

Prism uses a simplified **Two-Zone Model** with an indexed **Relevancy Library** for client-centric operations.

### 5.1 Two-Zone Model

**Hot Zone Definition:**
- All **active entries** (`isArchived = false`)
- All **scheduled items** within 14 days (past or future)
- **Excludes:** Inspirations (standalone notes, not time-bound)

| Zone | Criteria | Contents | Notes |
|------|----------|----------|-------|
| **Hot** | `isArchived = false` OR `scheduledAt` within 14 days | Active entries, upcoming/recent tasks | Frequency is observable but doesn't determine hotness |
| **Cement** | `isArchived = true` AND `scheduledAt` > 14 days ago | Completed entries, old schedules | Archive/history browsing |

**Same schema, different flag.** Entries age from Hot → Cement via background compaction.

### 5.2 Relevancy Library Schema

The **Relevancy Library** is a persistent, write-through index for O(1) entity lookup and conflict detection.

```kotlin
@Entity(tableName = "relevancy_library")
data class RelevancyEntry(
    @PrimaryKey val entityId: String,   // e.g., "z-001" (Person), "p-042" (Product)
    val entityType: String,             // PERSON, PRODUCT, LOCATION, EVENT
    val displayName: String,            // Canonical name ("张伟")
    val aliasesJson: String,            // ["张总", "张董事长", "老张"]
    val demeanorJson: String,           // {"communication_style": "formal"}
    val attributesJson: String,         // {"budget": "2M", "preferred_time": "morning"} (latest snapshot)
    val metricsHistoryJson: String,     // Time-series data for visualization (see below)
    val relatedEntitiesJson: String,    // [{"id": "p-001", "relation": "interested_in"}]
    val decisionLogJson: String,        // List<DecisionRecord> for Rethink history
    val lastUpdatedAt: Long,
    val createdAt: Long
)
```

#### metricsHistoryJson Structure (Time-Series for Viz)

Stores historical changes for key sales metrics. Enables turnkey data visualization.

**Typing Rules:**
- **Currency:** Store in **minor units** (分/cents) as integer. Required `unit` field: `CNY`, `USD`, `HKD`, `EUR`.
- **Quantity:** Integer value. Required `unit` field: `pcs`, `carton`, `pallet`, `kg`, `ton`.
- **Duration:** Integer value. Required `unit` field: `days`, `weeks`, `months`, `quarters`.
- **Stage/Enum:** String value from predefined enum.
- **Date:** ISO 8601 format (`YYYY-MM-DD`).

```json
{
  "budget": [
    {"date": "2026-01-15", "value": 200000000, "unit": "CNY", "source": "session-abc"},
    {"date": "2026-02-10", "value": 250000000, "unit": "CNY", "source": "session-xyz"}
  ],
  "price_quoted": [
    {"date": "2026-01-20", "value": 45000000, "unit": "CNY", "source": "session-abc"},
    {"date": "2026-02-05", "value": 42000000, "unit": "CNY", "source": "session-def"}
  ],
  "quantity": [
    {"date": "2026-01-15", "value": 100, "unit": "pcs", "source": "session-abc"},
    {"date": "2026-02-01", "value": 150, "unit": "pcs", "source": "session-xyz"}
  ],
  "deal_cycle": [
    {"date": "2026-01-10", "value": 90, "unit": "days", "source": "session-abc"}
  ],
  "deal_stage": [
    {"date": "2026-01-10", "value": "qualification", "source": "session-abc"},
    {"date": "2026-02-01", "value": "proposal", "source": "session-xyz"}
  ]
}
```

| Metric Key | Value Type | Unit Required | Use Case |
|------------|------------|---------------|----------|
| `budget` | Integer (minor) | `CNY`, `USD`, `HKD`, `EUR` | Budget trend |
| `price_quoted` | Integer (minor) | `CNY`, `USD`, `HKD`, `EUR` | Price negotiation |
| `quantity` | Integer | `pcs`, `carton`, `pallet`, `kg`, `ton` | Volume tracking |
| `deal_cycle` | Integer | `days`, `weeks`, `months` | Deal velocity |
| `deal_stage` | Enum string | — | Funnel stage |
| `commitment` | Boolean | — | Follow-through score |

> **Linter Rule:** Agent MUST extract and specify `unit` for all numeric values. If user says "两百万", agent infers "CNY" from User Profile locale. If ambiguous, agent asks.

> **Viz Integration:** Analyst mode can generate charts directly from this data: "Show Zhang's budget trend over the last 6 months."

### 5.3 Field Update Policies

| Field | Update Policy | Rationale |
|-------|---------------|-----------|
| `displayName` | First-write-wins | Canonical name shouldn't change |
| `aliasesJson` | Append (dedupe) | Growing list, naturally bounded (3-8 items) |
| `demeanorJson` | Upsert per key | Latest observation wins per trait |
| `attributesJson` | Upsert per key | Facts can change (budget, preferences) — **latest snapshot** |
| `metricsHistoryJson` | **Append per metric key** | Time-series, never overwrite — **full history** |
| `relatedEntitiesJson` | Upsert (dedupe by id+relation) | Relationships evolve |
| `decisionLogJson` | Append-only | History matters for Rethink learning |

**Design Decision**: Field-based updates, not session-based. Ensures latest facts are always current regardless of which session established them.

**attributesJson vs metricsHistoryJson:**
- `attributesJson`: Current state (what's true *now*)
- `metricsHistoryJson`: Historical changes (how it *evolved*)

### 5.4 Entity Disambiguation (Counter-Based Reinforcement)

When voice input contains ambiguous references (e.g., "张总" matches multiple people), the system uses **counter-based reinforcement learning** through user confirmations.

#### Alias Mapping Schema

Each alias stores enriched metadata beyond the raw string:

```kotlin
data class AliasMapping(
    val alias: String,              // "张总"
    val entityId: String,           // "z-001"
    val confirmationCount: Int,     // User confirmations (reinforcement signal)
    val lastConfirmedAt: Long,      // Recency signal (epoch millis)
    val sessionContexts: List<String>  // ["A3项目", "华东区"] — contextual hints
)
```

Store in `aliasesJson` as enriched entries (replaces plain string list).

#### Disambiguation Scoring Algorithm

```
candidates = queryByAlias("张总")

if candidates.size == 1 → auto-resolve (unambiguous)

if candidates.size > 1:
    1. Score each candidate:
       score = (confirmationCount × 0.4)
             + (recencyDecay(lastConfirmedAt) × 0.3)
             + (contextMatch(currentSession) × 0.3)

    2. Sort disambiguation picker by score (highest first)

    3. If topScore > 0.85 AND secondScore < 0.3:
       → Auto-resolve WITH disclosure (see [prism-ui-ux-contract.md](./prism-ui-ux-contract.md) §3.16)
       → User can tap [更改] if system is wrong

    4. Else:
       → Show picker (ordered by score)
```

| Scoring Component | Weight | Notes |
|-------------------|--------|-------|
| `confirmationCount` | 40% | Bounded at 20 (caps influence) |
| `recencyDecay` | 30% | `1.0 - min(daysSinceLastConfirm / 90, 1.0)` |
| `contextMatch` | 30% | Jaccard similarity of session keywords vs `sessionContexts` |

#### Reinforcement Triggers

| Event | Action |
|-------|--------|
| User picks from disambiguation picker | `confirmationCount += 1`, `lastConfirmedAt = now()` |
| User overrides auto-resolution | `confirmationCount -= 1` for wrong candidate, `+1` for corrected one |
| 90+ days no confirmation | `confirmationCount` frozen (no decay, but recency hurts) |

#### Behavior Principles

| Principle | Implementation |
|-----------|----------------|
| **Never silent** | Auto-resolution ALWAYS shows disclosure + `[更改]` button |
| **User override accessible** | Override reachable in one tap |
| **Counter informs, doesn't dictate** | Counter is one input; context and recency also matter |
| **Transparent reasoning** | Agent may explain: "Based on recent A3 project discussions..." |

### 5.5 Supporting Types

```kotlin
data class EntryRef(
    val entryId: String,
    val date: Long,
    val workflow: String,
    val title: String,
    val snippet: String?
)

data class DecisionRecord(
    val timestamp: Long,
    val conflictDescription: String,  // "Wednesday 2pm overlapping with 李总"
    val userResolution: String,       // "Merge into joint prep session"
    val resultingEntryId: String?
)
```

### 5.6 Structured Output Model (Separated Sections)

LLM outputs are split into two distinct sections — no inline markup mixing.

> **Note:** Schema Linter (§2.2 #6) validates the structured section before persistence.

**Output Structure:**

```
┌─────────────────────────────────────────────────────────┐
│                   LLM OUTPUT                            │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  [STRUCTURED SECTION] (agent + linter only)             │
│  {                                                      │
│    "entities": [                                        │
│      {"type": "person", "id": "z-001", "name": "张总",   │
│       "context": "A3项目负责人"},                        │
│      {"type": "location", "id": "l-001", "name": "北京办公室"},│
│      {"type": "product", "id": "p-042", "name": "A3打印机"},│
│      {"type": "date", "raw": "周三", "resolved": "2026-01-29"}│
│    ]                                                    │
│  }                                                      │
│                                                         │
│  [USER CONTENT] (clean, user-visible)                   │
│  「张总希望在周三于北京办公室讨论A3打印机方案」            │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Storage Model:**

| Field | Content | Visibility |
|-------|---------|------------|
| `displayContent` | Clean transcription/response | User |
| `structuredJson` | Extracted entities (JSON) | Agent + Linter |

**Processing Flow:**
1. **LLM** outputs structured section + user content
2. **Schema Linter** validates `structuredJson`
3. **Publisher** displays `displayContent` to user
4. **Relevancy Writer** parses `structuredJson` → upserts to Relevancy Library

#### Schema Linter Rules

| Check | Rule | Failure Action |
|-------|------|----------------|
| **JSON Valid** | `structuredJson` must be valid JSON | Reject, retry |
| **Entity Type** | Must be one of: `person`, `product`, `location`, `date`, `event` | Reject, retry |
| **ID Format** | ID must match `[a-z]-[0-9]{3}` (e.g., `z-001`) | Reject, retry |
| **Required Fields** | Each entity must have `type`, `id`, `name` | Reject, retry |

```kotlin
data class ExtractedEntity(
    val type: String,      // "person", "product", "location", "date", "event"
    val id: String,        // "z-001"
    val name: String,      // "张总"
    val context: String?,  // Optional additional context
    val resolved: String?  // For dates: ISO format
)

interface StructuredOutputLinter {
    fun validate(json: String): LintResult
}
```

### 5.7 Memory Entry Schema

All memory entries share a unified Room entity with workflow-specific payloads.

```kotlin
@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    
    // BaseEntry (indexed)
    val workflow: String,        // COACH, ANALYST, SCHEDULER
    val title: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val sessionId: String,
    val outcomeStatus: String?,  // ONGOING, SUCCESS, PARTIAL, FAILED
    
    // Content & Structure
    val contentWithMarkup: String,  // Raw LLM output with tags
    val displayContent: String,     // Clean UI text
    val entitiesJson: String,       // Parsed entity IDs
    
    // JSON blobs
    val artifactsJson: String?,     // ArtifactMeta (file refs)
    val outcomeJson: String?,       // Outcome with deliverables
    val payloadJson: String         // WorkflowPayload (mode-specific)
)
```

**Workflow Payloads** (stored in `payloadJson`):

| Workflow | Key Fields |
|----------|------------|
| **Coach** | `messages: List<ChatMessage>`, `topic: String?` |
| **Analyst** | `chapters: List<AnalysisChapter>`, `keyInsights: List<String>` |
| **Scheduler** | `scheduledAt: Instant?`, `priority: Priority`, `status: TaskStatus` |

### 5.8 User Profile (Static — User Center)

User-level configuration from User Center. User sets these explicitly.

```kotlin
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 0,
    val displayName: String,
    val preferredLanguage: String,        // "zh-CN", "en-US"
    val experienceLevel: String,          // "beginner", "intermediate", "expert"
    val industry: String,                 // "technology", "manufacturing", "finance"
    val role: String,                     // "sales_rep", "manager", "executive"
    val updatedAt: Long
)
```

| Field | Source | Purpose |
|-------|--------|---------|
| `experienceLevel` | User Center | Adjust coaching depth |
| `industry` | User Center | Domain-specific context |
| `role` | User Center | Seniority-appropriate suggestions |

---

### 5.9 User Habit (Growing — Learned Patterns)

Behavior patterns learned automatically. Context Builder uses these for personalization.

```kotlin
@Entity(tableName = "user_habit")
data class UserHabit(
    @PrimaryKey val habitKey: String,
    val habitValue: String,
    val entityId: String?,                // For per-client habits (null = global)
    val isExplicit: Boolean,              // true = user-set, false = inferred
    val confidence: Float,                // 0.0-1.0
    val observationCount: Int,
    val rejectionCount: Int,
    val lastObservedAt: Long,
    val createdAt: Long
)
```

**Habit Categories:**

| Category | Key | Example Values | Agent Usage |
|----------|-----|----------------|-------------|
| **Meeting Time** | `meeting_time` | `morning`, `afternoon`, `evening` | Suggest slots |
| **Business Trip** | `business_trip` | `monthly_travel`, `regional_focus` | Scheduling context |
| **Follow-up Style** | `follow_up` | `immediate`, `next_day`, `weekly_batch` | Reminder timing |
| **Analysis Focus** | `analysis_focus` | `data_heavy`, `painpoint_focus`, `psychology_insight` | Tailor Analyst |
| **Client Tone** | `client_tone` | `formal`, `casual`, `technical` | Per-client (`entityId`) |
| **Verbosity** | `verbosity` | `concise`, `detailed`, `bullet_points` | Response length |

#### Learning Principles

**1. Observation Threshold:** Create habit only after 3+ consistent observations. Initial `confidence = 0.3`.

**2. Confidence Decay:** If not observed for 30+ days: `confidence -= 0.05` per week. Delete if `< 0.1`.

**3. Explicit vs Implicit:** `isExplicit = true` (user-set) never auto-decays. Explicit overrides implicit.

**4. Negative Signals:** User rejection → `rejectionCount += 1`, `confidence -= 0.2`. 3+ rejections → delete.

#### Update Rules

| Event | Action |
|-------|--------|
| Behavior observed (< 3x) | Increment shadow counter |
| Behavior observed (3x+) | Create habit, `confidence = 0.3` |
| Behavior repeated | `confidence += 0.1` (max 0.9) |
| 30+ days no observation | `confidence -= 0.05` per week |
| User rejects suggestion | `confidence -= 0.2`, `rejectionCount += 1` |
| 3+ rejections | Delete habit |
| User explicitly sets | `isExplicit = true`, `confidence = 1.0` |

**Notification:** Triggers snackbar: `习惯已更新：<meeting_time: 早上>`

---

## 6. Crash Recovery & Checkpoints

### 6.1 Recovery Philosophy

The system uses a **minimal checkpoint strategy**: cache only the `EnhancedContext` (the expensive artifact). Everything else is either already persisted (chat history) or cheap to rebuild.

### 6.2 Checkpoint Strategy

```
┌─────────────────────────────────────────────────────────────────────┐
│                    CHECKPOINT STRATEGY                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  User Input ──▶ [ChatHistory.insert()] ── CHECKPOINT 0             │
│       │                                                             │
│       ▼                                                             │
│  [Context Builder] ── Hot Zone query + LLM conflict detection ~~~ $$$   │
│       │                                                             │
│       ▼                                                             │
│  Enhanced Context ──▶ [ContextCache.save()] ── CHECKPOINT 1        │
│       │                                                             │
│       ▼                                                             │
│  [LLM Streaming] ~~~ $$$                                            │
│       │                                                             │
│       ▼                                                             │
│  [Publisher] ──▶ [ChatHistory.update()] ── CHECKPOINT 2 (done)     │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│  ON CRASH + RESTART:                                                │
│                                                                     │
│  if ContextCache.exists(sessionId, turnId):                         │
│      context = ContextCache.load()                                  │
│      → Resume from LLM call (skip expensive context building)       │
│  else:                                                              │
│      → Start from Context Builder                                   │
│                                                                     │
│  ContextCache.clear() after successful ChatHistory.update()        │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.3 Checkpoint Data Model

| Checkpoint | What's Saved | When Cleared | Blocking? |
|------------|--------------|--------------|-----------|
| 0 | User message | Never (history) | ✅ Yes |
| 1 | Enhanced Context | On completion | ✅ Yes |
| 2 | AI response | Never (history) | ✅ Yes |

### 6.4 Recovery Behavior

| Crash Point | Recovery Action | Cost |
|-------------|-----------------|------|
| Before Checkpoint 1 | Rebuild context from scratch | High (LLM call) |
| After Checkpoint 1 | Load cached context, resume LLM | Low (skip context) |
| After Checkpoint 2 | No recovery needed | None |

### 6.5 Context Cache Hygiene

-   **TTL:** Stale checkpoints (older than 24h) are purged on app start.
-   **Scope:** One checkpoint per active turn. Overwritten on new turn.
-   **Storage:** Single SQLite table with `turnId`, `contextJson`, `createdAt`.

---

## 7. Testability Contract (Lattice Compliance)

Prism inherits and extends **Lattice Architecture** principles. While Lattice focuses on modularity and testability through black-box isolation, Prism focuses on intelligent agent patterns (Memory System, RAG, streaming). They are complementary layers.

### 7.1 Required Interfaces & Fakes

Every box MUST have an interface and a Fake for testing.

| Interface | Fake | Purpose |
|-----------|------|---------|
| `LlmProvider` | `FakeLlmProvider` | Returns canned responses |
| `ModePublisher` | `FakeModePublisher` | Captures output for assertions |
| `ContextBuilder` | `FakeContextBuilder` | Returns preset EnhancedContext |
| `ContextCache` | `FakeContextCache` | In-memory map |
| `MemoryRepository` | `FakeMemoryRepository` | In-memory store (Hot Zone access) |
| `MemoryWriter` | `FakeMemoryWriter` | No-op or capture |
| `ToolAgent` | `FakeToolAgent` | Returns preset results |

### 7.2 Dependency Rules

| Rule | Enforcement |
|------|-------------|
| **No Box-to-Box imports** | All inter-box communication goes through Orchestrator |
| **No Android imports in domain** | Domain layer is pure Kotlin (KMP-ready) |
| **Orchestrator is thin** | Delegates to boxes, no inline business logic |

### 7.3 Test Strategy

| Layer | Test Type | Dependencies |
|-------|-----------|--------------|
| Orchestrator | Unit | All Fakes injected |
| Each Box | Unit | Own Fakes + real logic |
| Integration | Instrumented | Real DB, Fake LLM |
| E2E | Manual/UI | Real everything |

---

## 8. Extension Guidelines

1.  **New Modes**: Implement `ModeStrategy`, `LlmProvider`, and `ModePublisher`. Register in Orchestrator.
2.  **New Tools**: Implement `ToolAgent` interface. Add to relevant Mode Strategy tool list.
3.  **Platform Expansion**:
    -   Keep Orchestrator and Context Builder pure Kotlin (KMP-ready).
    -   `MemoryRepository` is the boundary for Room (Android) vs CoreData/SQLDelight (iOS).

---

## 9. Relationship to Lattice

| Aspect | Lattice | Prism |
|--------|---------|-------|
| **Focus** | Black-box modularity, testability | Intelligent agent patterns |
| **Scope** | All code organization | AI/LLM pipeline specifically |
| **Constraints** | Interface + Fake + Hilt | Unified Pipeline + Strategies |
| **Status** | **Subsumed** | **Active SOT** |

> **Note:** Prism is the **single source of truth** for architecture. Lattice principles (interfaces, fakes, dependency direction) are incorporated into Prism §7. The standalone Lattice spec is archived for historical reference.

---

## Appendix A: Changelog

| Version | Date | Changes |
|---------|------|---------|
| V1.7 | 2026-01-23 | Rewrote §5.8 User Profile (User Center fields), added §5.9 User Habit (6 categories, 4 learning principles). Expanded Schema Linter with 4 patterns. Updated §3.1 buffered streaming. |
| V1.6 | 2026-01-23 | Added §2.3 Mode-Specific Context Strategies (Scheduler, Coach with LLM-scored threshold, Analyst), §2.4 Auto-Quote Module stub. |
| V1.5 | 2026-01-23 | Added Core Components #6 Schema Linter (universal validation), #7 Relevancy Notifier (snackbar updates). Updated Orchestrator with mode selection rules. |
| V1.4 | 2026-01-23 | Legacy cleanup: replaced all "LTM" references with "Hot Zone/Memory System", renamed `LtmRepository` to `MemoryRepository`, updated Context Builder inputs to reference Trinity. |
| V1.3 | 2026-01-23 | Added §5.8 User Profile. Deprecated M1-M4 terminology. Replaced Appendix B with Memory Trinity Overview. |
| V1.2 | 2026-01-23 | Added §4.5-4.7 (Plan-Once, Planner-Centric, Rethink), §5.6-5.7 (Native Structured Output + Schema Linter, Memory Entry Schema), Appendix B-C. |
| V1.1 | 2026-01-23 | Added §5 Memory System & Relevancy Library: Two-Zone model, Relevancy Library schema, field-based update policies, disambiguation rules. |
| V1.0 | 2026-01-21 | Initial spec. Unified Pipeline, Mode Strategies, Fire-and-Forget, Checkpoint Recovery, Lattice compliance. |

---

## Appendix B: Memory Trinity Overview

Prism's memory system is built on three core stores:

```
┌─────────────────────────────────────────────────────────────────┐
│                    PRISM MEMORY TRINITY                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────┐    ┌─────────────────┐                   │
│   │    HOT ZONE     │───▶│  CEMENT ZONE    │                   │
│   │   (Active)      │    │  (Archived)     │                   │
│   └────────┬────────┘    └─────────────────┘                   │
│            │                                                    │
│            │ write-through                                      │
│            ▼                                                    │
│   ┌─────────────────────────────────────────┐                  │
│   │         RELEVANCY LIBRARY               │                  │
│   │  (Client Index + Decision History)      │                  │
│   └─────────────────────────────────────────┘                  │
│                                                                 │
│   + USER PROFILE (Static Preferences)  — see §5.8              │
│   + CLOUD RAG (External Knowledge)     — see Appendix C        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

| Store | Role | Contents |
|-------|------|----------|
| **Hot Zone** | Active work | Today's entries, ongoing sessions (`isArchived=false`) |
| **Cement Zone** | Archive | Completed entries >14 days (`isArchived=true`) |
| **Relevancy Library** | Client index | Aggregated entities, aliases, decision history |
| **User Profile** | Static settings | Experience level, industry, role (§5.8) |
| **User Habit** | Learned patterns | Meeting time, verbosity, client tone (§5.9) |
| **Cloud RAG** | External knowledge | Product catalogs, CMS docs (Appendix C) |

> **Note**: The legacy "M1/M2/M3/M4" terminology is **deprecated**. Use the intuitive names above.

---

## Appendix C: Hybrid RAG Strategy

> **[FUTURE]** This section is a docking point for future development.

**Planned Tiering:**

| Tier | Scope | Processing |
|------|-------|------------|
| **Tier 1** | Session Memory | 100% on-device (structured metadata, keyword index) |
| **Tier 2** | User Documents | Hybrid (small docs local, large docs → Aliyun Bailian) |
| **Tier 3** | Global Semantic | On-device quantized embeddings (bge-small-zh, NPU-accelerated) |

**Performance Targets (when implemented):**
- Vector search (1K vectors): <50ms
- Embedding generation: 200-500ms

*Implementation deferred. See tracker.md when ready.*

