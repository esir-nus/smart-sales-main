# Architecture Refactoring Tracker

**Goal**: Clean, production-grade Android architecture  
**Scope**: Eliminate god files, modularize, align with Orchestrator-V1 spec  
**Status**: Phase 1 Complete, Phase 2 Wave 1 In Progress  
**Last Updated**: 2026-01-05

---

## Decisions Made

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Domain layer | Package first, module later | Lower friction, faster iteration |
| Splitting approach | Conservative, incremental | Reduce risk, verify after each extraction |
| Migration order | RealTingwuCoordinator → HomeScreenViewModel | Aligns with spec, creates clean building blocks |

---

## God Files Status

| File | Original Lines | Current Lines | Target | Status |
|------|----------------|---------------|--------|--------|
| `RealTingwuCoordinator.kt` | ~1990 | ~1870 | <500 | Phase 1 ✅ |
| `HomeScreenViewModel.kt` | 3668 | ~3650 | <500 | Phase 2 🔄 |
| `ChatController.kt` | ~400 | ~400 | <200 | P2 |

---

## Target Architecture

```
feature/chat/src/main/java/com/smartsales/
├── domain/                              # Pure Kotlin UseCases
│   ├── chat/ChatMessageBuilder.kt       # ✅ Wave 1
│   ├── export/ExportCoordinator.kt      # ✅ Hilt fix
│   ├── sessions/SessionsManager.kt      # ✅ Hilt fix
│   ├── transcription/                   # ✅ Phase 1 + Hilt fix
│   │   ├── TranscriptPublisherUseCase.kt
│   │   ├── SanitizerUseCase.kt
│   │   ├── DisectorUseCase.kt
│   │   └── TranscriptionCoordinator.kt
│   └── debug/DebugCoordinator.kt        # ✅ Hilt fix
├── feature/                             # UI layer
│   └── chat/home/HomeScreenViewModel.kt
└── data/ai-core/                        # Repositories
    └── tingwu/runner/TingwuRunnerRepository.kt  # ✅ Phase 1
```

---

## Phase 0: Stabilize (COMPLETE ✅)
- [x] Complete ViewModel extractions (Export, Debug, Sessions, Transcription)
- [x] Verify build passes
- [x] Hilt DI violation discovered and fixed
  - Converted ViewModels to domain coordinators
  - Build verified successful
- [ ] Runtime verification (awaiting user test)

## Phase 1: Split RealTingwuCoordinator (COMPLETE ✅)
Aligned with Orchestrator-V1 Section 3 modules:

- [x] Extract `TingwuRunnerRepository` (polling, validation, error mapping)
- [x] Extract `TranscriptPublisherUseCase` (URL extraction, download helpers)
- [x] Create `data/ai-core/tingwu/` package
- [x] Update RealTingwuCoordinatorTest with new dependencies
- [x] Verify build + tests pass

**Result**: RealTingwuCoordinator reduced by ~120 lines

## Phase 2: Slim HomeScreenViewModel (IN PROGRESS 🔄)

### Wave 1: ChatMessageBuilder (COMPLETE ✅)
- [x] Create `domain/chat/ChatMessageBuilder.kt`
- [x] Extract pure helper functions:
  - `buildSmartAnalysisUserMessage()`
  - `buildTranscriptMarkdown()`
  - `wrapSmartAnalysisForExport()`
- [x] Wire into HomeScreenViewModel
- [ ] On-device sanity check (PDF export)

### Wave 2: InputClassifier (PLANNED)
- [ ] Extract classification logic
- [ ] Extract `findSmartAnalysisPrimaryContent()`
- [ ] Extract `isLowInfoGeneralChatInput()`

### Wave 3: SmartAnalysisUseCase (DEFERRED)
> **Note**: Deferred due to deep coupling (50+ references, interacts with export, streaming, MetaHub). Safer to extract smaller building blocks first.

## Phase 3: Feature Module Cleanup
- [ ] Create `:feature:transcription` sub-package
- [ ] Move transcription UI components
- [ ] Ensure feature isolation

## Phase 4: Final Polish
- [ ] Add unit tests for UseCases  
- [ ] Update this documentation
- [ ] Code review

---

## Hilt Fix: ViewModel → Coordinator Migration

During Phase 2, discovered pre-existing Hilt violation from Phase 0 ViewModel extractions.

**Fixed by converting to domain layer:**

| Original | New | Notes |
|----------|-----|-------|
| `ExportViewModel` | `ExportCoordinator` | @Singleton, domain/export/ |
| `DebugViewModel` | `DebugCoordinator` | @Singleton, domain/debug/ |
| `SessionsViewModel` | `SessionsManager` | @Singleton, domain/sessions/ |
| `TranscriptionViewModel` | `TranscriptionCoordinator` | Injected as `tingwuCoordinator` to avoid naming collision |

---

## Success Metrics

| Metric | Before | Current | Target |
|--------|--------|---------|--------|
| Largest file | ~2500 lines | ~3650 (HomeVM) | <500 lines |
| UseCase count | 0 | 7 | 5-7 ✅ |
| God files | 3 | 2 | 0 |
| Domain layer classes | 0 | 7 | 10+ |

---

## References

- [Orchestrator-V1.md](./Orchestrator-V1.md) - Core pipeline spec
- [orchestrator-v1.schema.json](./orchestrator-v1.schema.json) - Data contracts
