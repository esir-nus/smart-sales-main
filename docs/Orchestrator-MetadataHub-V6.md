> ARCHIVED: This document is historical reference only.  
> CURRENT spec is `docs/Orchestrator-MetadataHub-V7.md`.  
> Do not implement new behavior against this version.

# ⚠️ 已归档（ARCHIVED）

> 状态：ARCHIVED  
> 本文档为历史规范 V6（V6.0.0-alpha），已被 `docs/Orchestrator-MetadataHub-V7.md`（CURRENT）取代。  
> 请以 V7 为唯一现行规范；V6 仅用于追溯历史与审计对照。

# Orchestrator–MetaHub Spec (V6)

## Version

- Version: `V6.0.0-alpha`
- Status: `ARCHIVED`
- Effective date: `2025-12-18`
- Replaced by: `docs/Orchestrator-MetadataHub-V7.md` (CURRENT)
- Replaces: `docs/Orchestrator-MetadataHub-V5.md` (archived)

## SemVer + Changelog Discipline (Normative)

### SemVer rules

- **Major**: breaking changes to contracts, responsibility boundaries, or user-visible rendering rules.
- **Minor**: backward-compatible additive changes (new optional fields, additional trace artifacts, extra validation rules that remain fail-soft).
- **Patch**: clarifications, typos, examples; MUST NOT change behavior meaning.

### Changelog

- `V6.0.0-alpha` (2025-12-18)
  - Publishes V6 baseline: batch-aware pipeline, hint-only suspicious boundaries, bounded Tier-2 memory, evidence-based speaker relabeling, and batch-based pseudo streaming.
  - Breaking vs V5: transcript rendering contract changes to `MM:SS speaker: utterance`, and V6 introduces batch-scoped artifacts (BatchPlan, SuspiciousHint, Decision records, Memory/Label patches).

## 1) Overview

### Goals

- Define a **batch-aware** transcript processing architecture with strict scope boundaries and fail-soft behavior.
- Make every stage **observable** via trace/HUD while never exposing secrets or raw provider HTTP bodies.
- Support **multi-agent** processing without token streaming (batch-based pseudo streaming only).
- Provide canonical, stable data model shapes for Orchestrator/MetaHub/UI integration.

### Non-goals

- This document does **not** prescribe task sequencing, roadmaps, or implementation plans.
- This document does **not** duplicate any provider REST parameter tables.
  - XFyun REST details are sourced from: `docs/xfyun-asr-rest-api.md` (SoT). This spec only references it and states conclusions.

### Definitions

- **Utterance line**: a normalized transcript line with speaker identity, text, and timing metadata.
- **Batch**: a windowed slice of utterance lines processed together (editable subset + read-only context).
- **Suspicious hint**: deterministic “boundary looks suspicious” signal; hint-only, not a deterministic label.
- **Tier-2 memory bank**: bounded, adaptive, slot-based memory for people/topics (does not grow unbounded).
- **Speaker label registry**: mapping from stable `speakerKey` to a mutable display label and label state.

## 2) Layer Responsibilities (Normative)

### LLM Agents

- MUST operate on **batch-scoped inputs** and produce **structured outputs** that are validated before application.
- MUST be treated as unreliable: any parse/validation failure MUST fail-soft.
- MUST NOT request or emit secrets (keys, signatures) or raw provider HTTP response bodies.

### Orchestrator

- Owns the end-to-end pipeline and is responsible for:
  - computing BatchPlan and SuspiciousHints deterministically;
  - calling LLM agents with scoped inputs;
  - validating and applying edits within strict scope rules;
  - producing trace artifacts for HUD/diagnostics.
- MUST NOT “silently” change user-visible behavior without producing trace evidence.

### MetaHub

- Stores structured metadata artifacts (batch plans, hints, decisions, memory patches, label patches).
- MUST remain schema-driven and merge/update in a bounded, deterministic way.

### UI / ViewModel

- Renders the **user-visible transcript** using the rendering contract in this spec.
- Debug/HUD MUST be human-readable on-screen; JSON payloads are **copy-only**.
- MUST NOT inline-render raw JSON previews.

## 3) Canonical Data Models (Normative, Stable Shapes)

This section defines **stable shapes**. Names are illustrative, but the shape and field semantics are normative.

### 3.1 UtteranceLine

Fields (required unless noted):
- `globalIndex: Int` — monotonic, 0-based across the whole transcript.
- `speakerKey: String` — stable identity key (e.g., `s1`, `s2`); not a display name.
- `text: String` — non-empty utterance content (trimmed).
- `startMs: Long?` / `endMs: Long?` — optional timing bounds.
- `gapMsToNext: Long?` — optional computed gap to the next line.
- `timeQuality: "EXACT" | "ESTIMATED" | "UNKNOWN"` — never omitted; drives UI fallback rules.

### 3.2 BatchPlan

Fields:
- `batchId: String` — stable within a run (`b1`, `b2`, …).
- `editableStartIndex: Int`
- `editableEndIndexInclusive: Int` (exactly 6 lines in baseline)
- `examineStartIndex: Int`
- `examineEndIndexInclusive: Int` (20-line read-only baseline)
- `tailLookaheadIndex: Int?` — optional read-only +1 line if the last editable boundary is suspicious.

### 3.3 SuspiciousHint (Hint-only)

Fields:
- `susId: String` — batch-aware ID, e.g. `b3-sus7`.
- `batchId: String`
- `globalBoundaryIndex: Int` — boundary between `globalBoundaryIndex` and `globalBoundaryIndex + 1`.
- `localBoundaryIndex: Int` — boundary index within the batch (derived deterministically).
- `gapMs: Long`
- `prevSpeakerKey: String?`
- `nextSpeakerKey: String?`

Normative constraints:
- Hints MUST be ordered by increasing `globalBoundaryIndex`.
- Hints MUST be capped:
  - `maxHintsPerBatch` and `maxHintsTotal` MUST be enforced deterministically.

### 3.4 Decision Records (per hint)

Fields:
- `susId: String`
- `batchId: String`
- `globalBoundaryIndex: Int`
- `applyStatus: "APPLIED" | "SKIPPED_NO_CHANGE" | "SKIPPED_POLICY" | "FAILED_PARSE" | "FAILED_VALIDATION" | "FAILED_MODEL"`
- `reason: String?` — short, safe diagnostic reason (no secrets).
- `safeExcerpts: { prevTail: String, nextHead: String }?` — truncated excerpts only.

Fail-soft rule:
- Any failure MUST fall back to the pre-decision transcript state for that scope.

### 3.5 Tier-2 MemoryBank (Bounded, Adaptive)

Fixed caps (normative baseline):
- People slots: `p1..p8`
- Topic slots: `t1..t20`

Memory record principles:
- Slot-based records with deterministic replacement policy when full.
- Evidence MUST be bounded and summarized; raw transcript dumps MUST NOT be stored here.

### 3.6 MemoryPatch

- A bounded patch that updates or replaces slot records.
- MUST NOT expand beyond the fixed caps.
- MUST be deterministic: the same inputs produce the same slot allocation decisions.

### 3.7 SpeakerLabelRegistry

Fields:
- `speakerKey: String`
- `displayLabel: String` — what UI shows (e.g., `Speaker 1`, `张总`).
- `labelState: "DEFAULT" | "CANDIDATE" | "CONFIRMED"`
- `confidence: Double?` — optional; semantics are implementation-defined but MUST be evidence-based.

### 3.8 LabelPatch

- Bounded updates to SpeakerLabelRegistry entries.
- Weak inference MUST keep default labels (do not “guess” names/titles).

## 4) Deterministic Pre-processing (Normative)

### 4.1 Micro-chopper Option B (Normative Baseline)

- Editable window size MUST be **6 lines**.
- Examination window size MUST be **20 lines** and is **read-only** baseline context.
- Tail lookahead rule (read-only):
  - If a suspicious boundary is exactly at `editableEndIndexInclusive`, include `tailLookaheadIndex = editableEndIndexInclusive + 1` as read-only context (if exists).

### 4.2 Suspicious Boundary Hinter (Hint-only)

- “Short gap suspicious”:
  - A boundary is suspicious **iff** `gapMs <= suspiciousGapThresholdMs`.
- Hint-only rule:
  - The hinter MUST NOT perform clause labeling, deterministic rewriting, or enrichment beyond the minimal hint fields.

## 5) Multi-agent Workflow (Normative)

### Agent 1: Segment Relocation

- Scope: batch-scoped.
- Input: BatchPlan (editable + examine context) + SuspiciousHints + relevant memory/labels.
- Output: structured relocation decision records that modify only editable indices.

### Agent 2: Polish

- Scope: batch-scoped and post-relocation.
- Input: relocated batch + hints + memory/labels.
- Output: structured polish decisions that modify only editable indices.

### Hard constraints (MUST)

- Agents MUST modify **only** editable indices (6 lines).
- Examination window and tail lookahead lines are **read-only**.
- Output MUST NOT create empty utterances.
- Output MUST NOT merge two speakers into one utterance line.
- `speakerKey` changes are NOT required in baseline V6.0:
  - If ever allowed, it MUST be explicitly enabled by a future spec update and validated with additional invariants.

## 6) Rendering Contract (User-visible, Normative)

- Final transcript MUST render as:
  - `MM:SS speaker: utterance`
- Timestamp rules:
  - Timestamp MUST never be blank.
  - If exact timing is unavailable, UI MUST render a fallback timestamp and record `timeQuality` accordingly.
- Speaker identity vs display labels:
  - Data uses stable `speakerKey`.
  - UI renders `displayLabel` from SpeakerLabelRegistry.
  - Weak inference keeps `Speaker N`.
  - Labels MAY update during processing; labels MUST freeze at completion.

## 7) Pseudo Streaming Model (Normative)

- Output is released **batch-by-batch** (pseudo streaming).
- Token streaming is out of scope.
- Previously released text MAY be provisional during feasibility, and:
  - display labels MAY update retroactively while processing is ongoing.
- Final transcript is finalized only when the whole audio completes.

## 8) Observability / Trace / HUD Policy (Normative)

### Required trace artifacts (high level)

- `batchPlans`
- `hintsByBatch`
- `decisionsByBatch`
- `memoryPatches`
- `labelPatches`
- `original/intermediate/final outputs` (stored safely; no raw provider HTTP bodies)

### HUD policy (MUST)

- On-screen debug MUST be human-readable (tables/lists), not JSON blocks.
- Copy-only actions MAY provide JSON payloads for debugging.
- HUD MUST NOT inline-render raw JSON previews.
- HUD/trace MUST NOT expose:
  - secrets (keys, tokens, signatures),
  - raw provider HTTP response bodies (e.g., XFyun raw JSON body).
