---
description: Architecture refactoring workflow - evolve to Clean Architecture per V1 spec
---

# Architecture Refactoring Workflow

## Reference Documents

| Document | Purpose |
|----------|---------|
| `docs/ArchitectureRefactoring.md` | **North Star** - Vision, target structure, milestones |
| `docs/Orchestrator-V1.md` | Module definitions and contracts |
| `docs/CHANGELOG.md` | Wave history and dev log |

---

## Pre-Flight Checklist

// turbo
1. Read the North Star: `cat docs/ArchitectureRefactoring.md`
// turbo
2. Check current milestone progress (look for unchecked `[ ]` items)
3. Review V1 module mapping to identify next extraction target

---

## Guardrails (MUST FOLLOW)

### Quality Over Quantity
- Focus on clean separation, not line counts
- Only extract when it improves testability or clarity

### Layer Rules
- **Domain layer**: Pure Kotlin, no Android imports
- **ViewModel**: Routing + state only, no business logic
- **Data layer**: I/O only, no business logic

### Naming (V1 Aligned)
- Parsers: `[Domain]Parser.kt` (e.g., `SmartAnalysisParser.kt`)
- Coordinators: `[Domain]Coordinator.kt` (e.g., `TranscriptionCoordinator.kt`)
- UseCases: `[Action]UseCase.kt` (e.g., `DisectorUseCase.kt`)

### Hard Rules
- Build must pass after EVERY change: `./gradlew :feature:chat:compileDebugKotlin`
- No behavior changes during refactoring
- Update North Star when structure changes

---

## Execution Steps

### Step 1: Identify Target
1. Check V1 module mapping in North Star
2. Find first 🔲 PLANNED or ⚠️ Mixed item
3. Analyze current code for extraction opportunity

### Step 2: Extract
1. Create new file in appropriate `domain/` package
2. Move pure logic (no state dependencies)
3. Keep state management in ViewModel/Coordinator
// turbo
4. Verify build: `./gradlew :feature:chat:compileDebugKotlin`

### Step 3: Test
1. Create unit test in parallel `test/` directory
2. Cover happy path + edge cases
// turbo
3. Run tests: `./gradlew :feature:chat:testDebugUnitTest`

### Step 4: Document
1. Update North Star module mapping (🔲 → ✅)
2. Log in CHANGELOG.md
3. Commit with descriptive message

---

## Milestone Verification

### M1: Domain Completeness
```bash
# Check all domain files exist
ls -la feature/chat/src/main/java/com/smartsales/domain/*/
# Verify no Android imports in domain
grep -r "android\." feature/chat/src/main/java/com/smartsales/domain/ || echo "Clean!"
```

### M2: ViewModel Purity
```bash
# Check ViewModel line counts
wc -l feature/chat/src/main/java/com/smartsales/feature/chat/home/*ViewModel.kt
# Look for business logic (manual review)
```

---

## Recovery: If Build Breaks

1. Check error message for unresolved references
2. Common fixes:
   - Missing import → Add import
   - Circular dependency → Extract interface
   - Moved function → Update call sites
3. If stuck, revert: `git checkout -- <file>`

---

## Escape Hatches

- Combine files if extraction creates too much fragmentation
- Rename packages if clearer name emerges
- Reorder priorities based on business needs
