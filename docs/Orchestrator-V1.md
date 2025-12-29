<!-- File path: docs/Orchestrator-V1.md -->
<!-- Purpose: Orchestrator-V1 architecture spec (single authority / AI coding SoT) -->
<!-- Notes:
  - V7 is deprecated and archived (ARCHIVED) and must not be used as behavior/contract basis.
  - role-contract.md and orchestrator-sample-response.md remain unchanged as collaboration norms/examples.
  - This file is the single source of truth (SoT) for the new architecture.
-->

# Orchestrator-V1 Architecture Spec (CURRENT)

Version: 1.1.0  
Status: CURRENT (active spec)  
V7: ARCHIVED (historical reference only; not an implementation basis)

---

## 0) Goals and Non-goals

### 0.1 Goals

- **Deliver smart outcomes first**: prioritize SmartAnalysis (L3) and result analysis over perfect verbatim transcripts.
- **Leak prevention + renderability**: all user-visible content must be published by Publisher; Chat renders only `<visible2user>` human-readable text.
- **Determinism and replayability**: key pipelines (batch split, publish prefix, de-dup) must be deterministic, idempotent, reproducible, and replayable.
- **AI coding friendly**: clear module boundaries, explicit contracts, unambiguous failure semantics; docs consumable by coding agents.

### 0.2 Non-goals (must explicitly refuse)

- **Do not** perform LLM-driven transcript polishing/correction (e.g., suspicious boundary/gap fixes).
- **Do not** do fine-grained per-word timestamp polishing (V1 is chapter-level timeline; word-level may be missing).
- **Do not** rely on chain-of-thought as a correctness source or storage object (may be optional display/optimization later, not SoT).
- **Do not** default to automatic long-term cross-session reasoning (future extension; currently explicit links/pointers only).

---

## 1) System Overview (User Action Entry Points)

There are only two entry actions (implicit routing; no separate Router module):

1. **User sends a chat message** -> **General Chat Pipeline**
2. **User uploads audio and requests transcription** -> **Tingwu Transcription Pipeline**

> Important: The transcription pipeline is strictly defined as "upload audio -> get transcription/chapters/summary and publish."  
> Subsequent discussion around transcription content (Q&A, summarization, review) belongs to **General Chat Pipeline**.

---

## 2) Core Invariants (all implementations must satisfy)

### 2.1 Publisher owns truth (publish = truth)

- UI shows **only Publisher output**, never raw vendor JSON.
- Chat: UI **renders only** text inside `<visible2user> ... </visible2user>`.
- Transcript: UI **renders only** `PublishedTranscript` (continuous prefix b1..bk).

### 2.2 LLM Parser cannot mutate transcript truth

- LLM Parser can only generate/update **structured metadata** (Metadata Hub), must not modify transcript truth.
- Display-layer transforms (bolding/truncation/segmentation/pseudo-stream rhythm) can only happen in Publisher (deterministic).

### 2.3 Failures must be controlled

- MachineArtifact fails parse/validation -> **retry policy** (see Section 8); no infinite loops.
- Any retry/degrade must emit Trace events for HUD/Thinking Trace display.

---

## 3) Modules (AI coding responsibility boundaries)

### 3.1 LLM modules (LLM-driven)

1) **AI Chatter**
- Input: user message + context (from Metadata Hub aggregation) + system prompts (role/style/language).
- Output: dual-channel response (HumanDraft + MachineArtifact).
- Decision: prompt-driven; AI Chatter self-selects `mode = L1/L2/L3` each reply.

2) **SmartAnalysis (L3 only)**
- Not a separate router; it is the structured content within MachineArtifact in L3 mode.
- Goal: actionable, reusable, persistable (M2/M2B/M3).

3) **LLM Parser (standalone)**
- Inputs:
  - Chat: L3 MachineArtifact
  - Transcription: Tingwu chapters & summary (plus required runtime context)
  - Metadata Hub: to improve parse consistency (e.g., existing tags/people/places)
- Output: structured updates to Metadata Hub (M2 / M2B / M3 etc).
- Constraint: must not write/modify PublishedTranscript truth.

### 3.2 Deterministic modules (non-LLM)

1) **Disector (batch splitter)**
- Input: audio total duration totalMs
- Output: DisectorPlan (macro windows + capture window + overlap)
- Constraint: deterministic, persistable, reproducible

2) **Tingwu Runner (vendor calls)**
- Input: DisectorPlan + audio reference (URL/asset)
- Output: per-batch Tingwu artifacts (three pillars: transcription / chapters / summary)

3) **Sanitizer (transcription display cleanup)**
- Input: Tingwu raw transcription (plus required batch context)
- Output: Publisher-ready display transcription data (no semantic change)
- Note: Sanitizer ensures display safety and consistency; no language polishing.

4) **Publisher (deterministic publisher)**
- ChatPublisher: extracts and publishes HumanDraft (only `<visible2user>`), validates MachineArtifact, decides retry/degrade.
- TranscriptPublisher: range filtering, continuous prefix publish, renders Transcript View Model.
- Unified responsibility: **turn displayable content into stable renderable objects** (Published*), and emit Trace events.

---

## 4) Metadata Hub (Memory + Context)

Metadata Hub is **context and structured memory for LLM modules**. Recommended to store as 4 (+1) separate JSON files (easier maintenance, diff, incremental write):

- **M1 UserMetadata** (static, from onboarding/user center)
  - language, style, tone, preferences, goals, etc.
  - part of AI Chatter system prompt (read-time)
- **M2 ConversationDerivedState** (dynamic, derived within chat session)
  - tags, highlights, action items, key people/locations, etc.
- **M2B TranscriptionDerivedState** (dynamic, derived within transcription session)
  - parsed from Tingwu chapters/summary
  - **must include source pointers** (see 6.2)
- **M3 SessionState**
  - session naming/export, session links (chat <-> recording), visibility/archive state, etc.
- **M4 ExternalKnowledge & Style** (placeholder)
  - external knowledge/tools/style packs (future extension)

> Are versions/schema "required"?  
> V1 recommends minimalism: each JSON top-level includes `schemaVersion` (integer), only increment on breaking change; no complex migration system.

---

## 5) General Chat Pipeline (user chat)

### 5.1 Inputs and outputs

Inputs:
- user message
- aggregated context: Metadata Hub (read-time projection of M1 + M2 + M2B + M3 + M4)

Outputs:
- PublishedChatTurn (for UI)
- Optional: metadata updates (via LLM Parser write)

### 5.2 Dual-channel output contract (must)

AI Chatter must output both channels in every reply:

1) **HumanDraft (user-visible content)**
- **Must** be inside `<visible2user> ... </visible2user>`
- Publisher renders only this part

2) **MachineArtifact (structured for parser)**
- Must be strict JSON (or equivalent structure), placed **outside** `<visible2user>`
- UI never shows directly; only Parser/Trace/Debug
- **Extraction rules**: MachineArtifact must be inside a fenced code block: ```json ... ```
  1) Extract HumanDraft first: first complete `<visible2user>...</visible2user>` innerText as candidate displayMarkdown
  2) Extract MachineArtifact next: first ```json fenced block outside `<visible2user>`
  3) If fenced block missing or JSON parse/validation fails:
     - mark `artifactStatus = INVALID`
     - trigger retry (<= maxRetries)
  4) **Explicitly forbidden**: heuristic "guess JSON / regex extract JSON" from non-fenced text (leak prevention, drift prevention)
- UI never directly displays it; only Parser / Trace / Debug

### 5.3 L1/L2/L3 modes (chosen by AI Chatter)

- L1: greetings/noise/short answer
- L2: insufficient info; needs clarification/follow-up
- L3: rich content, clear task; output SmartAnalysis structured result (in MachineArtifact)

> V1 uses prompt engineering to lock mode logic; if it drifts, Publisher artifact validation + retry is the fallback.

---

## 6) Tingwu Transcription Pipeline (upload audio -> publish result)

### 6.0 Identifier Glossary

| Identifier | Type | Lifecycle | Purpose | Generated By | Links To |
|------------|------|-----------|---------|-------------|----------|
| `chatSessionId` | string | Per-chat-session | Chat session identity | Chat system | Chat turns, M2, M3 |
| `turnId` | string | Per-turn | Unique turn/message ID | Chat system | PublishedChatTurn, MachineArtifact |
| `audioAssetId` | string | Immutable | Content identity for dedupe/caching | Audio upload system (before Disector) | DisectorPlan, TingwuBatchArtifact, M2B |
| `recordingSessionId` | string | Per-processing-instance | Processing instance ID (may be new even if audioAssetId cached) | Orchestrator on transcription start | DisectorPlan, TingwuBatchArtifact, M2B, PublisherState |
| `disectorPlanId` | string | Per-plan-version | Plan identity (bound to rule version) | Disector | DisectorPlan |
| `batchAssetId` | string | Per-batch | Stable batch identity from DisectorPlan | Disector (derived from plan + batchIndex) | TingwuBatchArtifact, TingwuJobState |
| `batchIndex` | integer | Per-batch | Ordering primitive (1-based, numeric sort only) | Disector | DisectorBatch, TingwuBatchArtifact, PublishedUtterance |
| `publishedPrefixBatchIndex` | integer | Monotonic | Last published batch index (prefix boundary) | TranscriptPublisher | PublisherState, PublishedTranscript |
| `jobId` | string | Per-job | Provider job identifier (provider-agnostic; Tingwu calls it "tingwuJobId" internally, but schema uses `jobId`) | Provider | TingwuBatchArtifact |
| `chapterId` | string | Per-chapter | Chapter identifier within a recording session, generated by LLM Parser | LLM Parser | M2BChapter, RecordingSourcePointer |
| `artifactType` | string | Per-artifact | Discriminator for JSON object type (e.g., "DisectorPlan", "MachineArtifact") | System | All artifact objects |

### 6.1 Identity & Cache (strongly recommended)

To avoid duplicate Tingwu calls, distinguish "content identity" vs "plan identity":

- `audioAssetId`: audio content identity (dedupe/cache key)
- `recordingSessionId`: processing instance (may be new even if audioAssetId cached)
- `disectorPlanId`: batch plan identity (bound to rule version)
- `batchAssetId`: batch identity (from DisectorPlan; stable, reproducible)

> Disector owns plan and batch identity; **audioAssetId must not be set by Disector** (rule changes would break cache hit).

### 6.2 M2B TranscriptionDerivedState

M2B uses canonical structure: `chapters[]` + `keyPoints[]` (see `docs/orchestrator-v1.schema.json`).

**Hard rules**:
- LLM Parser writes M2B; Publisher must not modify transcript truth.
- M2B must include source pointers: each chapter and keyPoint must include:
  - `audioAssetId` + `recordingSessionId` (required)
  - anchor: `chapterId` and/or `[startMs, endMs]` time range
- **Time range semantics**: `timeRange.startMs/endMs` are **absolute milliseconds** on recording timeline:
  - `0ms` = recording start (aligned with PublishedTranscript timeline)
  - half-open interval: `[startMs, endMs)`

**Structure**:
- `chapters[]`: each includes `chapterId`, `title`, `startMs`/`endMs` (or `timeRange`), optional `summary`, plus required source pointers.
- `keyPoints[]`: each includes `text` plus required source pointers (`chapterId` and/or `timeRange`).

### 6.3 Pipeline steps

1) Disector generates DisectorPlan (see Appendix A)
2) Tingwu Runner executes batches (may complete out of order)
3) Sanitizer transforms raw transcription into display-ready data (no semantic change)
4) TranscriptPublisher range-filters and publishes continuous prefix (see Appendix B)
5) LLM Parser parses chapters/summary into M2B (with source pointers)
6) UI renders PublishedTranscript + Chapters/Summary (from Publisher output only)

---

## 7) Data Contracts (implementation-level structure)

> DTO/Schema are defined in `docs/orchestrator-v1.schema.json`. The following are key fields only (not exhaustive).

### 7.1 PublishedChatTurn (UI consumer object)

- `displayMarkdown`: extracted from `<visible2user>` and sanitized by Publisher
- `mode`: L1/L2/L3
- `machineArtifact`: structured JSON (stored, not displayed)
- `artifactStatus`: VALID / INVALID / RETRIED / FAILED (for Trace/HUD)

### 7.2 MachineArtifact (structured, for Parser)

Minimum fields recommended:

- `schemaVersion`: 1
- `mode`: L1/L2/L3
- `provenance`: `chatSessionId`, `turnId`, `createdAtMs`
- `smartAnalysis`: (L3 only) e.g., highlights / actionableTips / entityPointers
- `metadataPatch`: (optional) directly expresses intended updates to M2/M2B/M3 (Parser may use or ignore)
  - `target`: target metadata layer (M1/M2/M2B/M3/M4)
  - `ops`: array of operations; each includes:
    - `op`: operation type (string; use known ops)
    - `value`: operation value (any type)
    - `path`: optional target path

**Known operation types** (recommended):

| Operation | Semantics | Target layer |
|-----------|-----------|--------------|
| `addTag` | Add tag | M2 |
| `removeTag` | Remove tag | M2 |
| `setRiskLevel` | Set risk level | M2 |
| `setMainPerson` | Set main person | M2 |
| `setLocation` | Set location | M2 |
| `setStage` | Set stage | M2 |

> Unknown operations are ignored unless Parser explicitly implements them. Agents should prefer known ops.

> `additionalProperties` allowed for future extension (e.g., vendor CoT, external tool references).

### 7.3 DisectorPlan / TingwuBatchArtifact / PublishedTranscript / PublishedAnalysis

These objects follow V1 deterministic constraints (see Appendices A/B/C) and are defined in schema.

---

## 8) Failure Semantics and Retry (Chat)

When MachineArtifact fails parse/validation, Publisher must execute retry strategy (Strategy B):

- `maxRetries = 2` (default recommendation)
- Retry only the "structured part"; HumanDraft may be reused or rewritten, but must still be inside `<visible2user>`
- If retries exhausted and still failing:
  - allow publishing the last HumanDraft (if extractable)
  - set `artifactStatus = FAILED`
  - prohibit Metadata Hub writes (prevent contamination)
  - must write Trace events (for HUD diagnosis)

---

## 9) Debug / HUD / Thinking Trace

### 9.1 HUD (mandatory 3 copy/paste blocks)

HUD is a debug panel with three copyable sections (for both Chat and Transcription):

1) **Section 1: Effective Run Snapshot**
   - current config and key state (provider lane, retry count, plan version, cache hits)
2) **Section 2: Raw Output**
   - Chat: raw LLM output (including `<visible2user>` and MachineArtifact text)
   - Transcription: Tingwu raw output or reference
3) **Section 3: Publisher-ready / Published Snapshot**
   - Chat: extracted `displayMarkdown` + artifact validation summary
   - Transcription: DisectorPlan summary + published prefix state + chapter timeline summary

### 9.2 Thinking Trace (optional display)

- Thinking Trace is a display/debug panel, not a correctness source.
- Current phase does not show chain-of-thought; only shows:
  - which modules ran, duration, cache hits
  - which metadata layers updated (M2/M2B/M3)
  - pointers (chapter/time/turn id)
- **Future extension**: if vendor provides CoT, it can be optional trace, but must not replace MachineArtifact as SoT.

---

## 10) Appendix A: Disector Rules (batch split and micro overlap) [Deterministic]

### A.1 Trigger rules

- If recording duration `<= 20 minutes`: no split, single batch to Tingwu.
- If `> 20 minutes`: enter split flow.

### A.2 Batch length formula (must follow)

- `TEN_MIN_MS = 600_000`
- `full = floor(totalMs / TEN_MIN_MS)`
- `remMs = totalMs % TEN_MIN_MS`
- First generate `full` **10-minute** batches (macro timeline).
- Remainder rules:
  - If `remMs < 7 minutes`: merge into last batch (last batch length = 10min + rem).
  - If `remMs >= 7 minutes`: add a remainder batch (length = rem).

### A.3 Examples (must match)

- 21 minutes -> (10, 11)
- 26 minutes -> (10, 16) (rem 6 < 7, merge)
- 27 minutes -> (10, 10, 7)
- 36 minutes -> (10, 10, 16)
- 37 minutes -> (10, 10, 10, 7)
- 39 minutes -> (10, 10, 10, 9)

### A.4 Micro overlap strategy (pre-roll only, deterministic)

- `overlapMs = 10_000`
- **Macro window** (authoritative timeline): `absStartMs..absEndMs`, half-open interval: `abs in [absStartMs, absEndMs)`
- **Capture window** (submitted audio):
  - First batch: `captureStartMs = absStartMs`
  - Other batches: `captureStartMs = max(0, absStartMs - overlapMs)`
  - **All batches**: `captureEndMs = absEndMs` (no post-roll)
- Intent and constraints:
  - Provide pre-warm for later batches; no suffix overlap for previous batch.
  - Must not do "front+back 10s" (20s overlap).

---

## 11) Appendix B: TranscriptPublisher (deterministic publish and de-dup)

### B.1 Overlap de-dup (V1 strategy, deterministic)

- Default strategy uses **range filtering only** (no text similarity).
- Rules:
  - If `(absStart, absEnd)` known: publish iff `absEnd > batch.absStartMs && absStart < batch.absEndMs`
  - If only `absStart`: publish iff `absStart in [batch.absStartMs, batch.absEndMs)`
- If no relative segment info: publish as "batch block" (one block per batch), accept pre-roll duplication.

### B.2 Continuous prefix publish (hard invariant)

- Only publish continuous prefix `b1..bk`.
- Persist `publishedPrefixBatchIndex`, only allow monotonic increase.
- UI must not assemble out-of-order batches.

---

## 12) Appendix C: Time and Order Invariants

- All timestamps are absolute on recording timeline.
- Macro windows are half-open intervals `[absStartMs, absEndMs)` to avoid overlapping ownership.
- Ordering primitive is `batchIndex` (integer); do not sort by batchId string.
- Tingwu timestamps are relative hints and must be anchored to `captureStartMs` before filtering.

---

## 13) Future Extensions (reserved; not in V1.1 scope)

- Confidence/threshold-based cross-session retrieval and prompts (long-term cross-session memory).
- Publisher semantic transform boundaries and test strategy (currently display-only transforms allowed).
- Vendor CoT ingestion and display (trace only, not correctness source).
- M4 external knowledge and tool calls (ExternalMemory/Knowledge API).
