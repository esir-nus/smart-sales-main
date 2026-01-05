# Architecture Refactoring Tracker

**Goal**: Clean, production-grade Android architecture  
**Scope**: Eliminate god files, modularize, align with Orchestrator-V1 spec  
**Status**: Planning Complete, Ready for Execution

---

## Decisions Made

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Domain layer | Package first, module later | Lower friction, faster iteration |
| Splitting approach | Conservative (5-7 UseCases) | Reduce risk, split more where needed |
| Migration order | RealTingwuCoordinator first | Aligns with spec, creates clean building blocks |

---

## God Files to Eliminate

| File | Current Lines | Target | Priority |
|------|---------------|--------|----------|
| `RealTingwuCoordinator.kt` | ~2500 | <500 per module | P0 |
| `HomeScreenViewModel.kt` | 3668 | <500 | P1 |
| `ChatController.kt` | ~400 | <200 | P2 |

---

## Target Architecture

```
feature/           # UI layer (ViewModels <500 lines each)
domain/            # Pure Kotlin UseCases
data/              # Repositories, API clients
core/              # Shared utilities
```

---

## Phase 0: Stabilize (COMPLETE)
- [x] Complete ViewModel extractions (Export, Debug, Sessions, Transcription)
- [x] Verify build passes
- [ ] Runtime verification

## Phase 1: Split RealTingwuCoordinator
Align with Orchestrator-V1 Section 3 modules:

- [ ] Extract `DisectorUseCase` (batch splitting logic)
- [ ] Extract `TingwuRunnerRepository` (API calls)
- [ ] Extract `SanitizerUseCase` (display cleanup)
- [ ] Extract `TranscriptPublisherUseCase` (continuous prefix publishing)
- [ ] Create `domain/transcription/` package
- [ ] Verify build + basic functionality

## Phase 2: Slim HomeScreenViewModel
- [ ] Create `domain/chat/` package
- [ ] Extract `SendChatMessageUseCase`
- [ ] Extract `StreamChatResponseUseCase`
- [ ] Extract `SmartAnalysisUseCase`
- [ ] Reduce HomeScreenViewModel to routing only
- [ ] Verify build + chat flow

## Phase 3: Feature Module Cleanup
- [ ] Create `:feature:transcription` sub-package
- [ ] Move transcription UI components
- [ ] Ensure feature isolation

## Phase 4: Final Polish
- [ ] Add unit tests for UseCases
- [ ] Update documentation
- [ ] Code review

---

## Success Metrics

| Metric | Before | Target |
|--------|--------|--------|
| Largest file | ~2500 lines | <500 lines |
| UseCase count | 0 | 5-7 |
| God files | 3 | 0 |

---

## References

- [Orchestrator-V1.md](./Orchestrator-V1.md) - Core pipeline spec
- [orchestrator-v1.schema.json](./orchestrator-v1.schema.json) - Data contracts
