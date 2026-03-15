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
| [§6 Memory System](#6-os-mental-model--relevancy-library) | OS Layers (RAM/SSD), RelevancyEntry schema, Entity Disambiguation, User Habits | 334-700 |
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

**Prism** is the architectural standard for the Intelligent Personal Assistant system. It replaces complex, stateful coordination with a **Dual-Engine Unified Pipeline** that rigidly separates fast User Interface interactions from complex background data mutations.

The name "Prism" reflects the core pattern: a single stream of user intent enters the system and is "refracted" into specific execution paths (General Chat vs Hardware Interrogation vs System Tools) by the Lightning Router, while sharing a common foundation of strict SSD-backed reality. 

### Core Principles
1.  **Dual-Engine Architecture (CQRS)**: Strict separation between the high-speed "Sync/Query" loop and the slow, complex "Async/Command" background loops.
2.  **Zero Ghosting**: The LLM operates purely on strictly typed Kotlin `data classes` (the "One Currency"); untyped string outputs for database mutations are strictly forbidden.
3.  **No Manual Modes**: The user does not manually toggle between "Coach" and "Analyst" modes. The Lightning Router automatically evaluates intent and routes the payload to the correct internal system (Clarification, Disambiguation, or Execution).
4.  **Streaming First**: The Chat interface is built around native streaming for minimal latency. Structured data parsing happens invisibly in the Async layer.

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
│       │               │                                             │
│       │               ▼                                             │
│       │        [Memory Writer] ──fire & forget──▶ [OS: SSD]        │
│       │                                                             │
└───────┴─────────────────────────────────────────────────────────────┘
```

### 3.2 Core Components

#### 1. Context Builder (Shared)
The foundation of intelligence. Runs for **every** request.

**Role:** Normalize heterogeneous inputs into a unified context payload for the Executor LLM.

**Input Normalization Tools:**

All user inputs are processed via specialized tools before reaching the main prompt builder:

| Input Type | Tool | Output | Notes |
|------------|------|--------|-------|
| **Text** | — (passthrough) | Raw text | Direct injection |
| **Audio** | `tingwu.transcribe` | Transcript + metadata | Speaker diarization, timestamps |
| **Image** | `qwen_vl.analyze` | OCR text / scene description | Supports batch (up to 5) |

> **Architectural Note:** In the old architecture, Tingwu was a standalone workflow. In Prism, it is **demoted to a tool** that feeds the Context Builder. All modalities are now unified input sources, not destinations.

#### 1b. Session Cache (In-Task Memory)

Lightweight in-memory cache for fast context access during task execution. Avoids heavy push/pull on persistent memory layers.

| Layer | Role | Lifetime |
|-------|------|----------|
| **Session Cache** | Fast in-memory store for ongoing task context | Current task execution |
| **Relevancy Library (SSD)** | Persistent structured memory index | Cross-session |

**Write Pattern:**
```
Output ──▶ Session Cache (sync, fast)
               │
               └── Async Delta Push ──▶ OS: SSD (Memory / Relevancy)
```

#### 2. Lightning Router (Gatekeeper)
Stateless router. Evaluates intent at the beginning of the Sync Loop to determine the correct pathway.
Replaces the old "Mode Toggle" system.

#### 3. LLM Executor
The generation engine. Driven dynamically by the Model Router depending on the pipeline phase (Sync/Async).

#### 4. The Linter (decodeFromString)
Validates ALL AI-generated structured outputs for background mutations.

**Failure Behavior:**
1. If the LLM generates an invalid payload, `decodeFromString` throws.
2. The exception is caught cleanly.
3. The background mutation halts. The system DOES NOT crash the UI thread.

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
-   Async observer on OS: SSD writes.
-   Does not block main pipeline.
-   Snackbar auto-dismisses after 3 seconds (tappable to expand details).
-   `<content>` is truncated to 20 characters max.

### 3.3 Model Router

Central routing for LLM model selection based on **task type**.

> **Location**: `domain/config/ModelRouter.kt`

#### Task-Based Routing (`forContext`)

| Input Condition        | Model          | Context Window |
|-----------------------|----------------|----------------|
| Image/Video present    | `qwen-vl-plus` | Vision         |
| Default (fast chat)    | `qwen-plus`    | 1M tokens      |

#### Memory Layer Routing (`forMemoryLayer`)

| Layer      | Model       | Rationale                                |
|------------|-------------|------------------------------------------|
| RELEVANCY  | `qwen3-max` | Advanced parsing for entity extraction   |
| HOT        | `qwen-plus` | Fast index navigation (14 days context)  |
| CEMENT     | `qwen-long` | Deep history retrieval (10M tokens)      |

---

## 4. Data Flow & Consistency

### 4.1 Sync Loop Output Flow (Native Streaming)
In Project Mono, because we strictly decouple the structured JSON data mutations (Async) from the user conversation (Sync), **The Chat UI can use true native flow streaming**. 

We do not buffer tokens waiting to see if there is JSON inside. The prompt restricts the LLM to pure markdown in the Sync loop.

### 4.2 Consistency Model

| Dimension | Guarantee | Implication |
|-----------|-----------|-------------|
| **UI Responsiveness** | **Immediate** | UI streams token-by-token. Zero blocking. |
| **Memory Persistence** | **Eventual** | Writes happen in background (Async Loop). |
| **Cross-Session** | **Strong** | Next session guaranteed to see previous session's commits. |
| **Intra-Session** | **Read-Your-Writes** | *Best effort.* In ultra-fast conversational turns, memory context bounding *might* be slightly stale (missing the immediately preceding turn). |

---

## 5. Agent Visibility System

> **Philosophy**: The user should *feel* the agent is smart. This section defines how intelligence is made visible during the Sync/Async lifecycles.

The UI avoids "Boolean Soup" (`isThinking`, `isParsing`) scattered across a ViewModel, instead using a dedicated `AgentActivityController` to surface background work to a top Activity Banner without blocking the chat.

#### 5.1 Visibility Mechanism: AgentActivityController

The **AgentActivityController** is the PRIMARY visibility mechanism — it makes agent cognition visible to users via the **AgentActivityBanner**.

> **Note**: This replaces the old legacy 3-layer Analyst UI (Planner Table / Sticky Board). Activity relies solely on the top-level banner and inline streaming.

**State Hierarchy:**
```kotlin
enum class ActivityPhase {
    DISAMBIGUATING, // 📝 正在理解您的意图...
    PARSING,        // 📄 解析附件中...
    TRANSCRIBING,   // 🎙️ 音频转写中...
    RETRIEVING,     // 📚 检索记忆图谱...
    EXECUTING,      // ⚙️ 执行特殊工具...
}
```

#### 5.2 Conflict Resolution (Rethink Model)

Conflicts encountered during Async Mutations are treated as **creative prompts**, not merge decisions. 
Because the Sync Loop already responded to the user, if the Async Loop hits a primary key conflict or date conflict:

**Detection → Suggestion:**
1. **Detection**: Linter or DB layer catches a conflict.
2. **Alert**: Pushes a structured warning card into the chat timeline automatically.
3. **Rethink Suggestion**: Agent proposes a "Third Option" (e.g., "Merge these", "Override", "Cancel").



## 6. OS Mental Model & Relevancy Library

Prism explicitly replaces the old "Two-Zone Model" (Hot/Cement) with the **OS Mental Model**, mapped to standard computer hardware concepts. This enforces strict separation between session state (RAM) and persistent world knowledge (SSD).

### 6.1 The SSD vs. RAM Metaphor

| OS Layer | Scope | Persistence | Module Path | Role |
|----------|-------|-------------|-------------|------|
| **OS: Kernel** | System | Core | `app-core` | Native lifecycle, Dagger setup. |
| **OS: RAM** | Session | Volatile | `domain:session` | The Active Workspace. Ephemeral, session-scoped intent and context. |
| **OS: SSD** | Cross-Session | Persistent | `domain:crm`, `data:*` | The Knowledge Base. Stable domain data (CRM, Habits, Memory). |
| **OS: App** | Bridge | Orchestrator| `app-core` | Orchestrates reads/writes across the SSD-RAM divide. |

> **Boundary Rule**: The foundation (SSD) MUST be oblivious to the transient workspace (RAM). No module in the `OS: SSD` layer is permitted to depend on or import classes from the `OS: RAM` layer. All coordination must happen in the `OS: App` bridge.

### 6.2 Relevancy Library Schema (SSD)

The **Relevancy Library** resides in the OS: SSD layer. It is a persistent, write-through index for O(1) entity lookup and conflict detection.

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

> **Viz Integration:** Report Center can generate charts directly from this data: "Show Zhang's budget trend over the last 6 months."

### 6.3 Field Update Policies

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

### 6.4 Entity Disambiguation (Counter-Based Reinforcement)

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

### 6.5 Supporting Types

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

### 6.6 Memory Entry Schema

All memory entries share a unified Room entity with workflow-specific payloads.

```kotlin
@Entity(tableName = "memory_entries")
data class MemoryEntryEntity(
    @PrimaryKey val id: String,
    
    // BaseEntry (indexed)
    val workflow: String,        // CHAT, COMMAND, SCHEDULE
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
| **Chat** | `messages: List<ChatMessage>`, `topic: String?` |
| **Command** | `commands: List<CommandAction>`, `targets: List<String>` |
| **Scheduler** | `scheduledAt: Instant?`, `priority: Priority`, `status: TaskStatus` |

### 6.7 User Profile (Static — User Center)

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
| `experienceLevel` | User Center | Adjust response depth |
| `industry` | User Center | Domain-specific context |
| `role` | User Center | Seniority-appropriate suggestions |

---

### 6.8 User Habit (Growing — Learned Patterns)

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
| **Analysis Focus** | `analysis_focus` | `data_heavy`, `painpoint_focus`, `psychology_insight` | Tailor responses |
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

## 7. Crash Recovery & Checkpoints

### 7.1 Recovery Philosophy

The system uses a **minimal checkpoint strategy**: cache only the `EnhancedContext` (the expensive artifact). Everything else is either already persisted (chat history) or cheap to rebuild.

### 7.2 Checkpoint Strategy

```
┌─────────────────────────────────────────────────────────────────────┐
│                    CHECKPOINT STRATEGY                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  User Input ──▶ [ChatHistory.insert()] ── CHECKPOINT 0             │
│       │                                                             │
│       ▼                                                             │
│  [Context Builder] ── OS: SSD query ~~~ $$$                        │
│       │                                                             │
│       ▼                                                             │
│  Enhanced Context ──▶ [ContextCache.save()] ── CHECKPOINT 1        │
│       │                                                             │
│       ▼                                                             │
│  [LLM Streaming] ~~~ $$$                                            │
│       │                                                             │
│       ▼                                                             │
│  [UI Stream Complete] ──▶ [ChatHistory.update()] ── CHECKPOINT 2   │
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

### 7.3 Checkpoint Data Model

| Checkpoint | What's Saved | When Cleared | Blocking? |
|------------|--------------|--------------|-----------|
| 0 | User message | Never (history) | ✅ Yes |
| 1 | Enhanced Context | On completion | ✅ Yes |
| 2 | AI response | Never (history) | ✅ Yes |

### 7.4 Recovery Behavior

| Crash Point | Recovery Action | Cost |
|-------------|-----------------|------|
| Before Checkpoint 1 | Rebuild context from scratch | High (LLM call) |
| After Checkpoint 1 | Load cached context, resume LLM | Low (skip context) |
| After Checkpoint 2 | No recovery needed | None |

### 7.5 Context Cache Hygiene

-   **TTL:** Stale checkpoints (older than 24h) are purged on app start.
-   **Scope:** One checkpoint per active turn. Overwritten on new turn.
-   **Storage:** Single SQLite table with `turnId`, `contextJson`, `createdAt`.

---

## 8. Testability Contract (Lattice Compliance)

Prism inherits and extends **Lattice Architecture** principles. While Lattice focuses on modularity and testability through black-box isolation, Prism focuses on intelligent agent patterns (Memory System, RAG, streaming). They are complementary layers.

### 8.1 Required Interfaces & Fakes

Every box MUST have an interface and a Fake for testing.

| Interface | Fake | Purpose |
|-----------|------|---------|
| `LlmProvider` | `FakeLlmProvider` | Returns canned responses |
| `ContextBuilder` | `FakeContextBuilder` | Returns preset EnhancedContext |
| `ContextCache` | `FakeContextCache` | In-memory map |
| `MemoryRepository` | `FakeMemoryRepository` | In-memory store (OS: SSD access) |
| `MemoryWriter` | `FakeMemoryWriter` | No-op or capture |
| `ToolAgent` | `FakeToolAgent` | Returns preset results |

### 8.2 Dependency Rules

| Rule | Enforcement |
|------|-------------|
| **No Box-to-Box imports** | All inter-box communication goes through Orchestrator |
| **No Android imports in domain** | Domain layer is pure Kotlin (KMP-ready) |
| **Orchestrator is thin** | Delegates to boxes, no inline business logic |

### 8.3 Test Strategy

| Layer | Test Type | Dependencies |
|-------|-----------|--------------|
| Orchestrator | Unit | All Fakes injected |
| Each Box | Unit | Own Fakes + real logic |
| Integration | Instrumented | Real DB, Fake LLM |
| E2E | Manual/UI | Real everything |

---

## 9. Extension Guidelines

1.  **New Tools**: Implement `ToolAgent` interface. Add to relevant Gateway permissions.
3.  **Platform Expansion**:
    -   Keep Orchestrator and Context Builder pure Kotlin (KMP-ready).
    -   `MemoryRepository` is the boundary for Room (Android) vs CoreData/SQLDelight (iOS).

---

## 10. Relationship to Lattice

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

## Appendix B: OS Mental Model Overview

Prism's architecture represents a physical transition from monolithic processing to an Operating System metaphor:

```
┌─────────────────────────────────────────────────────────────────┐
│                    THE OS MENTAL MODEL                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   ┌─────────────────┐       ┌─────────────────┐                 │
│   │    OS: RAM      │◄──────│    OS: SSD      │                 │
│   │ (Active Session)│       │ (Knowledge Base)│                 │
│   └────────┬────────┘       └─────────────────┘                 │
│            │                         ▲                          │
│            │ Write (Async)           │                          │
│            └─────────────────────────┘                          │
│                                                                 │
│   + OS: Kernel (Core Services, DI)                              │
│   + OS: App (User Shell & Bridge)                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

| Layer | Responsibility | 
|-------|----------------|
| **RAM** | `SessionWorkingSet`: Holds current context, active entities. Volatile. |
| **SSD** | `MemoryCenter`/`EntityWriter`: Permanent storage of facts across zones. |
| **Kernel/App** | Drives the lifecycle and bridges the RAM/SSD divide. |

> **Note**: The legacy "Two-Zone Model" (Hot/Cement) terminology is **deprecated**. Use the OS layers above.

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

