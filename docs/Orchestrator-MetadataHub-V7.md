# Orchestrator–MetaHub V7 (Docs-Driven Spec)

Version: 7.0.0  
Status: CURRENT  
Audience: Operator / Orchestrator / Codex / Android engineers  
Scope: Orchestrator + MetaHub + multi-agent pipeline + provider lanes + HUD contracts + schemas

## 0. Goals

V7 upgrades the Orchestrator–MetaHub system to:

1) Support a multi-agent pipeline for long audio and chat with strict scope enforcement and fail-soft behavior.
2) Define a clean, enforceable metadata model with stable ownership rules:
   - M1: UserMetadata (static)
   - M2: ConversationDerivedState (dynamic, session-scoped)
   - M3: SessionState (session mgmt + renaming/export naming)
   - M4: External Knowledge & Style (agent-friendly, optional, API)
3) Restore Tingwu + OSS as the default transcription provider lane.
4) Keep XFyun as an optional lane, disabled by default, and never “try it and see.”
5) Provide a 3-block HUD (copy/paste) for debugging:
   1) Effective run snapshot (settings + key states + prompts)
   2) Raw transcription output (provider-native)
   3) Preprocessed transcript snapshot (before polish/pseudo-streaming)
6) Leave a formal placeholder for External Memory / M4 via API without committing to a backend.

## 1. Non-goals

- No vector DB / embedding / RAG backend mandates in V7.
- No provider REST parameter tables in this doc. XFyun REST SoT is `docs/xfyun-asr-rest-api.md`.
- No token streaming requirement. Transcript updates use pseudo-streaming (batch-by-batch).

## 2. Terminology

- Provider lane: A provider-specific ingestion path producing raw transcription outputs (e.g., Tingwu+OSS, XFyun).
- Normalization: Provider output -> canonical utterance lines.
- Preprocess: Deterministic batch planning and suspicious boundary hints.
- Pseudo-streaming: Emitting incremental transcript updates per batch (not token deltas).
- Smart Analysis: Agent1-produced structured analysis artifact used for UI, exports, and derived state.

## 3. Provider Policy (V7)

### 3.1 Default provider lane
- Default transcription lane: TINGWU_OSS
- XFyun lane: present but disabled by default unless explicitly enabled and validated available.

### 3.2 Transfer-only guardrail (critical)
- Provider-layer translate/predict/analysis capabilities are treated as unavailable unless explicitly enabled and proven available.
- Do not “try it and see.” Fail early with a clear debug reason.

### 3.3 OSS policy
- Tingwu lane may depend on OSS to obtain a stable FileUrl.
- XFyun lane (if enabled) uses direct file-stream HTTP body (no OSS dependency in the active path).

## 4. Metadata Model (M1–M4)

V7 defines four logical metadata layers with strict ownership.

### 4.1 M1 — UserMetadata (static JSON)
Definition: Structured JSON directly mapped from onboarding/user-center.  
Scope: Cross-session.  
Write policy: UI / profile sync only. Agents do not write.  
Read policy:
- Agents and UI should read projections, not raw M1.
- UserPolishContext is the safe projection for Agent2.

### 4.2 M2 — ConversationDerivedState (dynamic, session-scoped)
Definition: Dynamic derived state for an ongoing session (audio processing and/or chat).  
Scope: Session-scoped by default.  
Write policy:
- Deterministic parsers + Agent1 emit typed patches.
- Orchestrator validates, merges, versions, and stores.
Read policy:
- Agent2 reads M2 for polishing.
- UI may read M2 to drive UX (risk/progress bars, badges). Prefer a stabilized signal view.

Typical contents:
- Deal signals: deal health risk level, stage/progress, intent.
- Highlights, pain points, next steps.
- Speaker registry proposals (with evidence/confidence).
- Bounded memory bank for long audio coherence.
- Preprocess snapshot pointers.

Important: M2 fields are hypotheses with provenance (source, evidence refs, confidence).

### 4.3 M3 — SessionState (management + naming + export gating)
Definition: Session management and UX rendezvous state.  
Scope: Session-scoped.  
Write policy: Primarily UI. Orchestrator may append system events but must not override accepted naming.

M3 contains a standalone RenamingMetadata:

RenamingMetadata:
- accepted:
  - sessionTitleAccepted, exportTitleAccepted (user-accepted values)
- candidate:
  - sessionTitleCandidate, exportTitleCandidate (derived proposals)
- guardrails:
  - one-time auto-apply only if accepted is empty and user has not renamed
  - after user rename, accepted values do not change unless user edits

Export gating:
- CSV/PDF export requires a successful Smart Analysis result (Agent1).
- Export naming uses RenamingMetadata (and optionally a pointer/hash to the Smart Analysis artifact used).

### 4.4 M4 — External Knowledge & Style (agent-friendly, optional)
M4 provides organization-approved knowledge and style constraints usable by agents.

Conceptual sublayers:
- M4a OrgPolicyAndStyle (pack): stable, versioned, deterministic rules (tone, disclaimers, terminology).
- M4b OrgKnowledgeBase (retrieved): searchable snippets (FAQ, specs, objection handling).

M4 is accessed via an API client (placeholder), feature-flagged and permission-gated. Do not commit to storage backend in V7.

## 5. Agents (two-agent model)

### 5.1 Agent1 — Memory Keeper + Smart Analysis
Reads:
- M2 current state and first-20 snapshot (or relevant session window)
- M3 minimal session state (e.g., whether user renamed already)
- M4 (optional) for taxonomy/terminology normalization

Writes:
- typed patches into M2:
  - memory bank updates
  - speaker label proposals (with evidence)
  - deal signals (risk/stage/intent/highlights/pain points/next steps) with provenance
- Smart Analysis artifact (structured), referenced for exports and UI
- renaming candidates (proposals only; never overwrite accepted directly)

Does not:
- rewrite transcript lines directly

### 5.2 Agent2 — Polisher
Reads:
- M2 ConversationDerivedState
- UserPolishContext projection from M1
- M4 (optional): style/jargon pack + retrieved FAQ snippets

Writes:
- transcript edits only, strictly bounded to the editable window as enforced by Orchestrator

Does not:
- mutate M2/M3 directly

## 6. Canonical Transcript Model and Preprocess

### 6.1 Normalization invariant
All provider lanes must normalize into canonical utterance lines before preprocessing and agents.

### 6.2 Batch plan and suspicious hints (deterministic)
- Suspicious boundary detection is hint-only; do not deterministically mark left/right clauses.

### 6.3 Pseudo-streaming
- Transcript is released batch-by-batch (not token streaming).
- UI may show provisional text; speaker labels may update during processing; labels freeze at completion.

### 6.4 Future note: stabilized UI signals
For future scalability:
- Maintain rawSignals (fast-changing, evidence-heavy) and uiSignals (debounced, stable).
- Risk can rise quickly but should not drop without:
  - N confirmations, cooldown, or user-confirmed correction.

## 7. HUD (Debug) Contract — 3 Copy/Paste Blocks

HUD must be human-readable and copy-ready. No secrets, no raw provider HTTP bodies.

### 7.1 Section 1 — Effective Run Snapshot
Must include (as much key info as possible, redacted):
- provider lane selected + disabled reasons for other lanes
- AiParaSettings snapshot (sanitized) + derived effective config
- key session states:
  - LLM prompt packs loaded? versions/hashes?
  - provider feature evidence (e.g., diarization present/absent)
  - signature health summary (XFyun; redacted; reference `docs/xfyun-asr-rest-api.md`)
- self-contained prompts in effect (or hashes + expandable)
- M4 status (enabled, pack versions, retrieval trace ids)

### 7.2 Section 2 — Raw Transcription Output
- provider-native raw transcription output (Tingwu or XFyun)
- schema/version tags and trace ids
- copy-only; not shown in normal chat bubbles

### 7.3 Section 3 — Preprocessed Snapshot (before final polish/streaming)
- first 20 lines summary (rendered)
- suspicious boundaries list
- batch plan summary
- other deterministic preprocessing artifacts

## 8. API Surface (high-level)
Detailed request/response types live in `docs/api-contracts.md` (aligned to V7).

UI talks to a unified facade, which routes internally to:
- ChatOrchestrator
- TranscriptionOrchestrator
- AnalysisOrchestrator
- ExportOrchestrator
- DebugOrchestrator
- ExternalMemoryClient (M4 placeholder)

## 9. Schema and Versioning

- M1/M2/M3/M4 schemas are described by `docs/metahub-schema-v7.json`.
- Each layer includes:
  - schemaVersion
  - updatedAt
  - provenance (source/confidence/evidenceRefs) for derived fields
- MetaHub should store:
  - current snapshots and bounded patch history for debugging.

## 10. Migration Notes (V6 -> V7)
- Introduce M2 ConversationDerivedState with provenance and patch-based updates.
- Extract RenamingMetadata into M3.
- Set default transcription lane to Tingwu+OSS; XFyun disabled by default.
- Add HUD 3-block copy contract.
- Add M4 placeholder interface and trace fields, feature-flagged and permission-gated.

## 11. Changelog

### 7.0.0
- New M1/M2/M3/M4 model with explicit ownership and provenance.
- Default provider lane becomes Tingwu+OSS; XFyun disabled by default.
- Added RenamingMetadata (candidate vs accepted) and export gating via Smart Analysis.
- Added HUD 3-section debug contract.
- Added External Memory / M4 placeholder interface (query-only by default).

