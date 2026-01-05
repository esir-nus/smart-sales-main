# Architecture Refactoring Tracker

**Goal**: Clean, production-grade Android architecture  
**Scope**: Align with Orchestrator-V1 spec, modularize god files  
**Status**: ✅ Waves 1-3 Complete - Consolidation Checkpoint  
**Last Updated**: 2026-01-05

---

## Progress Summary

| Metric | Before | After | Change |
|--------|--------|-------|--------|
| HomeScreenViewModel | 3668 lines | **2888 lines** | -780 (-21%) |
| RealTingwuCoordinator | ~1990 lines | ~1870 lines | -120 (-6%) |
| Domain layer classes | 4 | **11** | +7 |

---

## Target Architecture

```
feature/chat/src/main/java/com/smartsales/
├── domain/
│   ├── chat/
│   │   ├── ChatMessageBuilder.kt       ✅ Wave 1
│   │   ├── InputClassifier.kt          ✅ Wave 2
│   │   └── ChatPublisher.kt            ✅ Wave 3 (V1-aligned)
│   ├── export/ExportCoordinator.kt     ✅ Hilt fix
│   ├── sessions/SessionsManager.kt     ✅ Hilt fix
│   ├── transcription/
│   │   ├── DisectorUseCase.kt          ✅ Phase 1
│   │   ├── SanitizerUseCase.kt         ✅ Phase 1
│   │   └── TranscriptionCoordinator.kt ✅ Hilt fix
│   └── debug/DebugCoordinator.kt       ✅ Hilt fix
└── data/ai-core/tingwu/
    ├── runner/TingwuRunnerRepository.kt     ✅ Phase 1
    └── TranscriptPublisherUseCase.kt        ✅ Phase 1
```

---

## Completed Work

### Phase 1: RealTingwuCoordinator Split ✅
- Extracted `TingwuRunnerRepository`, `TranscriptPublisherUseCase`
- Aligned with V1 Section 3.2 (Deterministic Modules)
- Tests passing

### Hilt DI Fix ✅
Converted 4 ViewModels to domain coordinators:
- `ExportCoordinator`, `SessionsManager`, `TranscriptionCoordinator`, `DebugCoordinator`

### Wave 1: ChatMessageBuilder ✅
Extracted 3 pure helper functions (~65 lines)

### Wave 2: InputClassifier ✅
Extracted 5 classification functions (~145 lines)

### Wave 3: ChatPublisher ✅
**Architecture rebuild** per V1 Section 3.2.4:
- Created V1-aligned `ChatPublisher.kt` (~140 lines)
- **Removed 700 lines** of legacy heuristic sanitizer (V1 violations)
- Enforces V1 contract: extract `<visible2user>` only, no heuristic cleanup

---

## V1 Compliance Status

| V1 Module | Status |
|-----------|--------|
| Disector | ✅ |
| Tingwu Runner | ✅ |
| Sanitizer (Transcription) | ✅ |
| **ChatPublisher** | ✅ |
| TranscriptPublisher | ✅ |

---

## Next Steps

**Recommended**: Verify stability before continuing extraction
1. Manual testing on device
2. Verify chat flow, export, transcription
3. Commit checkpoint

**Future Waves**: Continue extracting cohesive modules from HomeScreenViewModel

---

## References

- [Orchestrator-V1.md](./Orchestrator-V1.md) - Architecture spec
- [orchestrator-v1.schema.json](./orchestrator-v1.schema.json) - Data contracts
