# UX Contract (Canonical)

> **🔒 LOCKED DOCUMENT**
> 
> This file defines **WHAT** the system presents—data contracts, pipeline ownership, and feature boundaries.
> Changes require Product/Engineering approval. Do not modify without explicit sign-off.
>
> For **HOW** things are presented (states, microcopy, timing, layout), see [`ux-tracker.md`](../plans/ux-tracker.md).
>
> **Last verified against Orchestrator-V1.md**: 2026-01-08 ✅

---

## Version Notes

- The only current spec is `docs/specs/Orchestrator-V1.md`.
- V7 is archived; do not use V7 as UX basis.
- Stable data objects follow `docs/specs/orchestrator-v1.schema.json`.

---

## 1. Data Contracts

These define what data the UI consumes. Changing these affects implementation.

| Contract | Rule |
|----------|------|
| Chat rendering source | UI renders only `PublishedChatTurn.displayMarkdown` |
| Tag sanitization | `<visible2user>` tags never appear in UI; Publisher extracts to `displayMarkdown` |
| Machine artifacts | `machineArtifact` is not a render source; display only in Debug/HUD |
| Transcript source | UI consumes only Publisher "prefix publish" results |
| Transcript ordering | UI shows continuous prefix only (b1..bk), no out-of-order display |
| De-duplication | Unified in Publisher (range filtering); UI must not re-compute |

---

## 2. Pipeline Ownership

Defines which pipeline handles which user action. Changing this affects routing logic.

| User Action | Pipeline | Outcome |
|-------------|----------|---------|
| Send chat message | General Chat Pipeline | LLM response in chat |
| Upload audio for transcription | Tingwu Pipeline | Transcript appears in chat |
| Q&A on transcript (post-transcription) | General Chat Pipeline | Uses M2B pointers for context |

---

## 3. Response Classifications

The system classifies LLM responses. UI adapts based on classification.

| Level | Definition | UI Adaptation |
|-------|------------|---------------|
| L1 | Short answer/greeting | Normal bubble |
| L2 | Needs clarification | Bubble + follow-up prompt styling |
| L3 | SmartAnalysis | Bubble (`displayMarkdown`) + structured card if `smartAnalysisCard` exists |

> [!NOTE]
> **L1/L2/L3 classify output richness, not pipeline selection.**
> - All GENERAL chat first-replies (including L3-style 状态C responses) produce `<Rename>` for session naming
> - The SMART_ANALYSIS pipeline (`quickSkillId = "SMART_ANALYSIS"`) is separate: it uses JSON-only output and derives session title from `main_person` + `summary_title_6chars` fields instead of `<Rename>` tags

> **Experience spec**: See [ux-tracker.md#l3-smartanalysis-card](../plans/ux-tracker.md#l3-smartanalysis-card) for card layout details.

---

## 4. Transcription Data Structure

### 4.1 Transcript (Verbatim View)
- Driven by `PublishedTranscript`
- If relative segment info missing: show as "batch blocks" (V1 limitation)

### 4.2 Chapters & Summary
- Driven by `PublishedAnalysis`
- Chapter-level timeline only; V1 does not do per-line timestamp polishing
- "Jump to chapter" interaction allowed (chapterId or time range)

### 4.3 Speaker Display
- Prefer `PublishedAnalysis.speakerMap` (if available)
- Fallback: stable placeholders S1, S2...
- Mapping updates as batches advance; do not back-write history

> **Note**: `speakerMap` is an implementation pattern; not yet defined in V1 schema. This section documents current behavior.

---

## 5. HUD (Debug Panel) Requirements

The HUD is a debug panel for developers. It must contain three copyable sections:

| Section | Contents |
|---------|----------|
| **1. Effective Run Snapshot** | Current config, cache hits, retry count, plan/version, batch progress |
| **2. Raw Output** | Chat: raw LLM output (includes `<visible2user>` tags). Transcription: Tingwu raw output |
| **3. Published Snapshot** | Chat: extracted `displayMarkdown` + artifact validation. Transcription: DisectorPlan summary + prefix state |

> **Constraint**: Normal UI never shows raw JSON; JSON is allowed only in HUD.

---

## 6. Text Processing Boundaries

| Layer | Allowed | Not Allowed |
|-------|---------|-------------|
| UI Layer | Display transforms only | Complex cleanup, secondary LLM cleanup |
| Publisher | Segmentation, truncation, bold/italic, Markdown safety | Semantic rewriting |

---

## 7. Thinking Trace (Debug Panel)

This is a display/debug panel, not a correctness source. Shows:
- Which modules ran, latency, cache hits, retries
- Which metadata layers updated (M2/M2B/M3)
- Pointers (turnId / chapterId / time range)

No chain-of-thought display.

---

## Cross-References

| Topic | Experience Spec |
|-------|-----------------|
| Chat flow states | [ux-tracker.md#chat-flow](../plans/ux-tracker.md#chat-flow) |
| Audio upload flow states | [ux-tracker.md#audio-upload-flow](../plans/ux-tracker.md#audio-upload-flow) |
| Transcription flow states | [ux-tracker.md#transcription-flow](../plans/ux-tracker.md#transcription-flow) |
| Layout invariants | [ux-tracker.md#layout-invariants](../plans/ux-tracker.md#layout-invariants) |
| Timing rules | [ux-tracker.md#timing--feedback](../plans/ux-tracker.md#timing--feedback) |
