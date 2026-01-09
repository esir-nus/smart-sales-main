# M1: Tingwu Completion

**Status:** IN PROGRESS  
**Started:** 2026-01-09  
**Target:** M1 Feature Complete milestone

## Objective

Complete Tingwu transcription pipeline per Orchestrator-V1.md §6.

**Entry point:** User uploads audio → transcription/chapters/summary → publish to chat

---

## Current State: ~60% Complete

| Component | Status |
|-----------|--------|
| Disector (batch split) | ✅ Done |
| Tingwu Runner (API calls) | ✅ Done |
| Sanitizer/Formatter | ✅ Done |
| TranscriptPublisher | ✅ Done |
| V1BatchIndexPrefixGate | ✅ Done |
| M2B Metadata (chapters/keyPoints) | ❌ Missing |
| LLM Parser for chapters | ❌ Missing |
| publishedPrefixBatchIndex persistence | ❌ Missing |
| State Recovery (§13) | ❌ Missing |
| Chapters/Summary UI | ✅ Done (Inline Markdown) |

---

## Phases

### Phase 1: E2E Verification
> "Can user upload audio → see transcription in chat?"

- [ ] End-to-end test: upload → transcribe → render
- [ ] Confirm happy path works
- [ ] Verify chapters/summary captured (even if not displayed)

### Phase 2: Chapters & Summary Display
> "User sees organized chapters, not just raw text"

- [x] Verify Tingwu returns chapters/summary — 2026-01-09
- [x] Create M2B TranscriptMetadata structure — (Deferred to Phase 3, using Inline Markdown for display)
- [x] Wire to UI (chapter timeline, summary card) — (Implemented via Inline Markdown)

### Phase 3: LLM Parser Integration
> "AI extracts structured insights from transcription"

- [ ] LLM Parser parses Tingwu chapters → M2B
- [ ] Source pointers (chapterId, timeRange)
- [ ] MetaHub integration

### Phase 4: Robustness
> "Handle failures gracefully"

- [ ] publishedPrefixBatchIndex persistence
- [ ] State recovery on app restart (§13)
- [ ] Partial failure handling

---

## Completed

- [x] Fixed Debug HUD Tingwu trace wiring — 2026-01-09
- [x] Removed XFyun from Debug HUD — 2026-01-09
- [x] **M1 Phase 2 Complete**: Enabled Chapters/Summary display (Inline Markdown) — 2026-01-09

---

## Reference

- Spec: [Orchestrator-V1.md §6](file:///home/cslh-frank/main_app/docs/Orchestrator-V1.md)
- Architecture: [RealizeTheArchi.md](file:///home/cslh-frank/main_app/docs/RealizeTheArchi.md)
