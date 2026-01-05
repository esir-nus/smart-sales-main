---
description: Architecture refactoring workflow - clean up god files, create domain layer
---

# Architecture Refactoring Workflow

## Pre-Flight Checklist
// turbo
1. Read the tracker: `cat docs/ArchitectureRefactoring.md`
// turbo
2. Check current phase status (look for `[ ]` incomplete items)
3. Read Orchestrator-V1.md if working on transcription modules: `docs/Orchestrator-V1.md`

---

## Guardrails (MUST FOLLOW)

### File Size Limits
- **Hard limit**: No file > 500 lines after refactoring
- **Soft target**: Prefer files < 300 lines

### Naming Conventions
- UseCases: `[Action][Domain]UseCase.kt` (e.g., `SendChatMessageUseCase.kt`)
- Repositories: `[Domain]Repository.kt` (e.g., `TingwuBatchRepository.kt`)
- Package: `domain/[feature]/` for UseCases

### Do NOT
- Create circular dependencies between domain and feature layers
- Put Android dependencies in domain layer
- Create "manager" or "helper" classes (use UseCases instead)
- Skip build verification after each extraction

### Do
- Verify build after EVERY extraction: `./gradlew :feature:chat:compileDebugKotlin`
- Update `docs/ArchitectureRefactoring.md` checkboxes as you complete items
- Keep existing functionality intact (no behavior changes)
- Follow Orchestrator-V1 module names for transcription-related code

---

## Phase 1: Split RealTingwuCoordinator

Target file: `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`

### Step 1.1: Create domain package
```bash
mkdir -p feature/chat/src/main/java/com/smartsales/domain/transcription
```

### Step 1.2: Extract DisectorUseCase
1. Find Disector logic in RealTingwuCoordinator (batch splitting)
2. Create `domain/transcription/DisectorUseCase.kt`
3. Move batch splitting logic (aligns with Orchestrator-V1 Appendix A)
// turbo
4. Verify build: `./gradlew :feature:chat:compileDebugKotlin`

### Step 1.3: Extract TingwuRunnerRepository
1. Find Tingwu API call logic
2. Create `data/ai-core/.../tingwu/TingwuRunnerRepository.kt`
3. Move API interaction code
// turbo
4. Verify build

### Step 1.4: Extract SanitizerUseCase
1. Find sanitization logic (display cleanup)
2. Create `domain/transcription/SanitizerUseCase.kt`
// turbo
3. Verify build

### Step 1.5: Extract TranscriptPublisherUseCase
1. Find continuous prefix publishing logic (Appendix B)
2. Create `domain/transcription/TranscriptPublisherUseCase.kt`
// turbo
3. Verify build

### Step 1.6: Update tracker
Mark Phase 1 items as [x] in docs/ArchitectureRefactoring.md

---

## Phase 2: Slim HomeScreenViewModel

Target file: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`

### Step 2.1: Create chat domain package
```bash
mkdir -p feature/chat/src/main/java/com/smartsales/domain/chat
```

### Step 2.2: Extract SendChatMessageUseCase
1. Find `sendMessageInternal` and related functions
2. Create `domain/chat/SendChatMessageUseCase.kt`
3. Move message sending logic
// turbo
4. Verify build

### Step 2.3: Extract StreamChatResponseUseCase
1. Find streaming/retry logic
2. Create `domain/chat/StreamChatResponseUseCase.kt`
// turbo
3. Verify build

### Step 2.4: Refactor ViewModel to routing
1. HomeScreenViewModel should only:
   - Hold UI state
   - Route user intents to UseCases
   - Observe UseCase results
2. Target: < 500 lines
// turbo
3. Verify build

---

## Post-Refactoring Checklist

// turbo
1. Run full build: `./gradlew assembleDebug`
// turbo
2. Run tests: `./gradlew testDebugUnitTest`
3. Update `docs/ArchitectureRefactoring.md` with completion status
4. Verify no file exceeds 500 lines: `wc -l <files>`

---

## Recovery: If Build Breaks

1. Check error message carefully
2. Common issues:
   - Missing imports → Add import statement
   - Unresolved reference → Check if function was moved, update call site
   - Circular dependency → Refactor interface to break cycle
3. If stuck, revert last change and try smaller extraction

---

## Reference Files

- `docs/ArchitectureRefactoring.md` - Tracker (source of truth for progress)
- `docs/Orchestrator-V1.md` - Module definitions for transcription
- `docs/orchestrator-v1.schema.json` - Data contracts
