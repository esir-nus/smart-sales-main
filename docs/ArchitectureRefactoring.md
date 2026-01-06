# Architecture Evolution Guide

> **Purpose**: North Star for Smart Sales architecture evolution  
> **Spec Alignment**: Orchestrator-V1.md (v1.2.0)  
> **Approach**: Prescriptive with escape hatches

---

## 1. Vision

Transform from ViewModel-centric "god files" to **Clean Architecture** where:
- ViewModels are routing + state only (no business logic)
- Domain layer owns all business logic (testable, spec-aligned)
- Data layer handles I/O only

---

## 2. Target Architecture

```
┌─────────────────────────────────────────┐
│  UI Layer (Compose)                     │
│  • Stateless render of ViewModel state  │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  ViewModel Layer                        │
│  • Hold StateFlow                       │
│  • Route intents to domain              │
│  • Observe domain results               │
│  • NO business logic                    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  Domain Layer (pure Kotlin)             │
│  • Coordinators (stateful flows)        │
│  • Parsers/Transformers (pure funcs)    │
│  • No Android dependencies              │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│  Data Layer                             │
│  • Repositories (API/DB)                │
│  • No business logic                    │
└─────────────────────────────────────────┘
```

---

## 3. Target File Structure

### Domain Layer (`domain/`)

```
domain/
├── chat/                        # General Chat Pipeline (V1 §5)
│   ├── ChatPublisher.kt         ✅ EXISTS
│   ├── ChatMessageBuilder.kt    ✅ EXISTS
│   ├── InputClassifier.kt       ✅ EXISTS
│   ├── MetadataParser.kt        ✅ EXISTS
│   └── SmartAnalysisParser.kt   ✅ EXISTS
│
├── transcription/               # Tingwu Pipeline (V1 §6)
│   ├── DisectorUseCase.kt       ✅ EXISTS
│   ├── SanitizerUseCase.kt      ✅ EXISTS
│   ├── TranscriptPublisher.kt   🔲 PLANNED
│   └── TranscriptionCoordinator.kt ✅ EXISTS
│
├── debug/
│   └── DebugCoordinator.kt      ✅ EXISTS
│
├── export/
│   └── ExportCoordinator.kt     ✅ EXISTS
│
└── sessions/
    └── SessionsRepository.kt    🔲 FUTURE
```

### Feature Layer (`feature/chat/`)

```
feature/chat/
├── home/
│   ├── HomeScreenViewModel.kt   ✅ EXISTS (target: routing only)
│   ├── HomeScreen.kt            ✅ EXISTS
│   ├── HomeUiState.kt           ✅ EXISTS
│   ├── orchestrator/
│   │   └── HomeOrchestratorImpl.kt ✅ EXISTS (target: slim)
│   ├── debug/DebugViewModel.kt  ✅ EXISTS
│   ├── export/ExportViewModel.kt ✅ EXISTS
│   └── transcription/TranscriptionViewModel.kt ✅ EXISTS
│
└── core/
    └── stream/ChatStreamCoordinator.kt ✅ EXISTS
```

### Data Layer (`data/ai-core/`)

```
data/ai-core/
├── tingwu/
│   ├── TingwuRunnerRepository.kt ✅ EXISTS
│   └── TingwuBatchRepository.kt 🔲 PLANNED
├── dashscope/                   ✅ EXISTS
└── oss/                         ✅ EXISTS
```

---

## 4. V1 Module → File Mapping

| V1 Module | File | Status |
|-----------|------|--------|
| AI Chatter (§3.1.1) | `HomeOrchestratorImpl` | ✅ Slim |
| SmartAnalysis (§3.1.2) | `SmartAnalysisParser` | ✅ Done |
| LLM Parser (§3.1.3) | `MetadataParser` | ✅ Done |
| Disector (§3.2.1) | `DisectorUseCase` | ✅ Done |
| Tingwu Runner (§3.2.2) | `TingwuRunnerRepository` | ✅ Done |
| Sanitizer (§3.2.3) | `SanitizerUseCase` | ✅ Done |
| ChatPublisher (§3.2.4) | `ChatPublisher` | ✅ Done |
| TranscriptPublisher (§3.2.4) | `TranscriptPublisher` | 🔲 Create |

---

## 5. Milestones

### M1: Domain Completeness ✅
**Criteria:**
- [x] All V1 modules have corresponding domain files
- [x] `SmartAnalysisParser` extracted from orchestrator
- [x] `DisectorUseCase` implements V1 Appendix A rules

**Verification:** Each domain file has unit tests (26 new tests)

---

### M2: ViewModel Purity ⏳
**Criteria:**
- [ ] HomeScreenViewModel contains no `if/when` business logic
- [ ] All business decisions delegated to domain layer
- [ ] ViewModel < 800 lines

**Verification:** Code review passes "can I test this without mocking Android?"

---

### M3: Full V1 Alignment ⏳
**Criteria:**
- [ ] All module mappings show ✅
- [ ] Data contracts match `orchestrator-v1.schema.json`
- [ ] Unit test coverage > 80% for domain layer

---

## 6. Escape Hatches

- **Combine if too granular** — Don't create 10-line files
- **Package names advisory** — Can rename if clearer
- **Priority order flexible** — Business needs may reorder

---

## 7. Constraints (Hard Rules)

1. **Build must pass after every change**
2. **No behavior changes during refactoring**
3. **Domain layer has no Android imports**
4. **Update this doc when structure changes**

---

## 8. References

- [Orchestrator-V1.md](./Orchestrator-V1.md) — Module definitions
- [orchestrator-v1.schema.json](./orchestrator-v1.schema.json) — Data contracts
- [CHANGELOG.md](./CHANGELOG.md) — Wave history and dev log
