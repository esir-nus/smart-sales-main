# T-Task: V7 Dev Trace (Orchestrator–MetaHub)

Doc version: 1.0  
Target spec: `docs/Orchestrator-MetadataHub-V7.md` (CURRENT)  
Purpose: Track V7 engineering workstreams, decisions, and verification evidence.

---

## 0) Scope & Outcomes (Expected Results Under Control)

V7 targets:

- Tingwu + OSS as default transcription lane; XFyun lane present but disabled by default.
- M1/M2/M3/M4 model documented and enforceable:
  - M1: UserMetadata (static JSON from onboarding/user-center)
  - M2: ConversationDerivedState (dynamic session state; provenance + uiSignals reserved)
  - M3: SessionState + RenamingMetadata (accepted vs candidate; export gating)
  - M4: External Knowledge & Style placeholder (API + pack versioning + HUD trace)
- HUD has 3 copy/paste sections:
  1) Effective run snapshot
  2) Raw transcription output
  3) Preprocessed snapshot

---

## 1) Workstreams

### WS1 — Docs & Contracts
- V7 spec doc (Orchestrator–MetaHub)
- V7 schema (metahub-schema-v7.json)
- api-contracts + ux-contract alignment
- AGENTS.md updated to set V7 as CURRENT
- Archived banners added to V6/V5/V4

### WS2 — Provider Lanes (Integration)
- Tingwu + OSS lane (default) verification
- XFyun lane (disabled by default) wiring audit + guardrails
- Third-party integration registry (docs/source-repo.json + schema) kept 3rd-party only

### WS3 — Multi-agent Pipeline (Design -> Implementation)
- Agent1 (Memory Keeper + Smart Analysis) patch model for M2
- Agent2 (Polisher) bounded edits model
- Pseudo-streaming batch-by-batch contract

### WS4 — Debug/HUD Observability
- Snapshot composition rules (3 sections)
- Redaction rules: no secrets, no raw HTTP bodies, copy-only JSON blocks

### WS5 — Export (Gated by Smart Analysis)
- Export gating model
- Export naming via RenamingMetadata

### WS6 — External Knowledge & Style (M4 placeholder)
- Pack versioning fields, retrieval trace, consent/feature flags (design only)

---

## 2) Task List (Status Tracking)

Legend: TODO / DOING / DONE / BLOCKED

### T7-001 Doc sync to V7 (spec + contracts + schema)
- Status: DONE
- Evidence:
  - `docs/Orchestrator-MetadataHub-V7.md`
  - `docs/metahub-schema-v7.json`
  - `docs/api-contracts.md`
  - `docs/ux-contract.md`
  - `docs/AGENTS.md`

### T7-002 Archive banners (V6/V5/V4)
- Status: DONE
- Evidence:
  - banner inserted at top of archived docs

### T7-003 Third-party registry refactor (OSS/Tingwu/XFyun/Dashscope only)
- Status: DONE
- Evidence:
  - `docs/source-repo.json`
  - `docs/source-repo.schema.json`

### T7-004 Implementation: MetaHub storage for M2/M3 (patch-based)
- Status: TODO
- Definition of done:
  - M2 writes are patch-based with provenance
  - M3 renaming accepted vs candidate works and is stable after user override

### T7-005 Implementation: provider lane selection (default Tingwu+OSS; XFyun disabled)
- Status: DONE
- Definition of done:
  - Effective lane selection visible in HUD Section 1
  - XFyun cannot be used unless explicitly enabled and preflight validated
  - Evidence:
    - `data/ai-core/src/main/java/com/smartsales/data/aicore/params/AiParaSettings.kt` (default provider -> TINGWU; XFyun has `enabled=false` gate)
    - `app/src/main/java/com/smartsales/aitest/audio/SwitchableAudioTranscriptionCoordinator.kt` (refuses XFyun unless enabled)
    - `data/ai-core/src/test/java/com/smartsales/data/aicore/params/InMemoryAiParaSettingsRepositoryTest.kt` updated to match new defaults
    - Tests: `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`

### T7-006 Implementation: HUD 3-block copy snapshot
- Status: TODO
- Definition of done:
  - One-click copy per section
  - No secrets; no raw HTTP bodies; JSON copy-only

### T7-007 Implementation: pseudo-streaming transcript batches
- Status: TODO
- Definition of done:
  - BatchReleased events in UI
  - Speaker labels freeze on completion

### T7-008 Implementation: Smart Analysis gating for export
- Status: TODO
- Definition of done:
  - Export button disabled until smart analysis ready
  - Export naming uses accepted/candidate fallback rules

### T7-009 Placeholder: External Knowledge & Style (M4) API interface
- Status: TODO
- Definition of done:
  - feature flag + consent gates defined
  - HUD Section 1 logs pack versions + traceId (when enabled)

---

## 3) Decision Log

- D7-001: Third-party integration registry is limited to OSS/Tingwu/XFyun/Dashscope + placeholders; it must not contain product specs or M1/M2/M3 schema.
- D7-002: XFyun REST parameter tables remain only in `docs/xfyun-asr-rest-api.md` (no duplication).
- D7-003: UI is allowed to read M2 for future playful UX; stabilized uiSignals reserved as future upgrade.

---

## 4) Open Questions / Risks

- R7-001: Tingwu+OSS lane specifics (OSS vendor/API shape) not documented here; must be discovered from code + tests.
- R7-002: M2 stabilization (hysteresis) is not implemented in V7 docs; reserved for future upgrade.
- R7-003: External memory / M4 governance: pack update process and permissions are TBD.

---

## 5) Verification Checklist (when implementation starts)

- [ ] Default lane is Tingwu+OSS
- [ ] XFyun disabled by default; cannot “try it and see”
- [ ] HUD 3 sections present and copyable
- [ ] No raw JSON visible in normal UI bubbles
- [ ] Export gated by Smart Analysis
- [ ] M4 placeholder does not change behavior unless enabled
