# UX Contract (Aligned with Orchestrator-V1)

## Version notes
- The only current spec is `docs/Orchestrator-V1.md`.
- V7 is archived; do not use V7 as UX basis.
- UI never shows raw JSON; JSON is allowed only in HUD copyable blocks.
- Stable data objects follow `docs/orchestrator-v1.schema.json`.

---

## 1) Chat UX (General Chat Pipeline)

### 1.1 Message rendering (hard constraints)
- UI renders only `PublishedChatTurn.displayMarkdown`.
- `<visible2user>` tags never appear in UI; Publisher extracts and sanitizes to `displayMarkdown`.
- Machine-side structure (`machineArtifact`) is not a primary render source; do not display it except in Debug/HUD.

### 1.2 L1/L2/L3 presentation
- L1: short answer/greeting -> normal bubble
- L2: needs clarification -> bubble + (optional) follow-up prompt styling
- L3 (SmartAnalysis):
  - Normal bubble (`displayMarkdown`) must be readable and complete
  - If `PublishedChatTurn.smartAnalysisCard` exists: UI shows a structured card (Highlights / Action items / Entities / Pointers)

### 1.3 Thinking Trace (optional panel)
- This is a display/debug panel, not a correctness source.
- No chain-of-thought display; only:
  - which modules ran, latency, cache hits, retries
  - which metadata layers updated (M2/M2B/M3)
  - pointers (turnId / chapterId / time range)

---

## 2) Recording UX (Tingwu Transcription Pipeline)

### 2.1 Transcript (verbatim view)
- Driven by `PublishedTranscript`.
- UI shows **continuous prefix** only (b1..bk), no out-of-order display.
- De-dup logic is unified in Publisher (range filtering); UI must not re-compute.
- If relative segment info missing: show as "batch blocks" and accept pre-roll duplication (V1 limitation).

### 2.2 Chapters & Summary (chapter/summary view)
- Driven by `PublishedAnalysis`.
- Chapter-level timeline only; V1 does not do per-line timestamp polishing.
- Allow "jump to chapter" interaction (chapterId or time range).

### 2.3 Speaker display
- Prefer `PublishedAnalysis.speakerMap`.
- If no mapping, fall back to stable placeholders: S1, S2...
- Mapping updates as batches advance, but do not back-write history.

---

## 3) Entry points and boundaries (must be consistent)

- User sends chat message -> General Chat Pipeline
- User uploads audio for transcription -> Tingwu Pipeline
- **Post-transcription discussion** (Q&A, summary, review) belongs to General Chat Pipeline (can reference M2B pointers)

---

## 4) Progressive publishing rules (Transcription)

- UI only consumes Publisher "prefix publish" results.
- UI must not assemble out-of-order batches.

---

## 5) HUD (Debug) three copy/paste blocks (mandatory)

HUD is a debug panel with three copyable sections:

1) **Section 1: Effective Run Snapshot**
   - Current config and key state
   - Cache hits, retry count, plan/version, batch progress

2) **Section 2: Raw Output**
   - Chat: raw LLM output (includes `<visible2user>` and MachineArtifact text)
   - Transcription: Tingwu raw output or reference

3) **Section 3: Publisher-ready / Published Snapshot**
   - Chat: extracted `displayMarkdown` + artifact validation summary
   - Transcription: DisectorPlan summary + published prefix state + chapter timeline summary

> Constraint: normal UI never shows raw JSON; JSON is allowed only in HUD.

---

## 6) Text cleaning boundary

- UI layer does not do complex cleanup or secondary LLM cleanup.
- Publisher may do display transforms (e.g., segmentation, truncation, bold/italic, Markdown safety), but must not do semantic rewriting.

---

## 7) Core UI Behavioral Invariants

The following behaviors are mandatory. Visual specifications are in `style-guide.md` at the referenced sections.

| Invariant | Rule | Visual Spec |
|-----------|------|-------------|
| **Home Hero visibility** | Hero appears ONLY when current session is empty (no user messages, no assistant messages, no imported transcripts). Never rendered as a chat bubble or in the scrollable message list. | §6.2 |
| **Quick Skill Row placement** | Under hero when session is empty; inside input area (above text field) when session is active. There is **never more than one quick skill row visible**. | §6.3 |
| **History Drawer layout** | Device-status placeholder card at top, profile entry at bottom. Both use full drawer width. | §6.6 |
