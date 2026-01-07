---
description: Comprehensive architecture alignment audit against RealizeTheArchi.md
---

# Architecture Alignment Check

Audit how well the current codebase aligns with the target architecture in `RealizeTheArchi.md`. 

**Two audit dimensions:**
1. **Structure** — Are files in the right folders?
2. **Purity** — Is code in the right layer?

---

# PART A: STRUCTURE CHECKS (50 points)

---

## Step 1: Load Target Architecture

Read the target architecture document:

```bash
cat docs/RealizeTheArchi.md
```

Extract:
1. **Target Tree** (Section 2) — Expected file structure
2. **V1 Module Mapping** (Section 3) — Expected module locations
3. **Layer Rules** — What belongs where

---

## Step 2: Verify File Structure (15 points)

For each file in the target tree, verify it exists:

```bash
ls -la feature/chat/src/main/java/com/smartsales/domain/
ls feature/chat/src/main/java/com/smartsales/domain/*/
```

**Scoring:**
- All expected dirs/files exist: 15/15
- 90%+ exist: 12/15
- 70-89% exist: 8/15
- <70% exist: 0/15

---

## Step 3: Verify Zero Android Imports in Domain (15 points)

```bash
grep -rn "import android\." feature/chat/src/main/java/com/smartsales/domain/
grep -rn "import androidx\." feature/chat/src/main/java/com/smartsales/domain/
```

**Scoring:**
- 0 Android imports: 15/15
- 1-5 imports: 8/15
- 6+ imports: 0/15

---

## Step 4: Verify Interface/Impl Separation (10 points)

```bash
ls feature/chat/src/main/java/com/smartsales/domain/*/
```

Expected pairs:
- `Disector.kt` + `DisectorImpl.kt`
- `Sanitizer.kt` + `SanitizerImpl.kt`
- `TranscriptionCoordinator.kt` + `TranscriptionCoordinatorImpl.kt`
- `DebugCoordinator.kt` + `DebugCoordinatorImpl.kt`
- `ExportCoordinator.kt` + `ExportCoordinatorImpl.kt`
- `SessionsManager.kt` + `SessionsManagerImpl.kt`

**Scoring:**
- All 6 pairs exist: 10/10
- 5 pairs: 8/10
- <5 pairs: 4/10

---

## Step 5: Verify V1 Module Mapping (10 points)

```bash
grep -l "class DashscopeAiChatService" data/ai-core/src/main/java/
grep -l "object SmartAnalysisParser" feature/chat/src/main/java/com/smartsales/domain/analysis/
grep -l "interface Disector" feature/chat/src/main/java/com/smartsales/domain/transcription/
grep -l "interface Sanitizer" feature/chat/src/main/java/com/smartsales/domain/transcription/
grep -l "object ChatPublisher" feature/chat/src/main/java/com/smartsales/domain/chat/
```

**Scoring:**
- All mapped: 10/10
- 80%+ mapped: 7/10
- <80% mapped: 3/10

---

# PART B: PURITY CHECKS (50 points)

---

## Step 6: Data Layer Purity (20 points)

**Rule:** Data layer (`data/`) should ONLY do I/O. No business logic, no MetaHub, no formatting.

### 6A: Data Layer Should NOT Import MetaHub (10 points)

```bash
grep -rn "import com.smartsales.core.metahub" data/
```

**Why:** MetaHub is domain state. Data layer should return raw data, let domain update MetaHub.

**Scoring:**
- 0 matches: 10/10
- 1-3 matches: 5/10
- 4+ matches: 0/10

### 6B: Data Layer Should NOT Do Formatting (10 points)

```bash
grep -rn "buildMarkdown\|formatTranscript\|compose.*Markdown\|toMarkdown" data/
```

**Why:** Formatting is presentation/domain logic, not I/O.

**Scoring:**
- 0 matches: 10/10
- 1-3 matches: 5/10
- 4+ matches: 0/10

---

## Step 7: Domain Layer Purity (15 points)

**Rule:** Domain layer should NOT do I/O. No HTTP, no file system, no Android.

### 7A: Domain Should NOT Import Platform Layer (10 points)

```bash
grep -rn "import com.smartsales.feature" feature/chat/src/main/java/com/smartsales/domain/
```

**Why:** Dependencies should point inward, not outward.

**Scoring:**
- 0 matches: 10/10
- 1-5 matches: 5/10
- 6+ matches: 0/10

### 7B: Domain Should NOT Do HTTP (5 points)

```bash
grep -rn "OkHttpClient\|Retrofit\|HttpUrl\|okhttp3" feature/chat/src/main/java/com/smartsales/domain/
```

**Scoring:**
- 0 matches: 5/5
- 1+ matches: 0/5

---

## Step 8: Presentation Layer Purity (15 points)

**Rule:** ViewModels should orchestrate, not contain business logic.

### 8A: ViewModel Delegates to Coordinators (8 points)

```bash
grep -c "tingwuCoordinator\." HomeViewModel.kt
grep -c "debugCoordinator\." HomeViewModel.kt
grep -c "chatCoordinator\." HomeViewModel.kt
grep -c "sessionsManager\." HomeViewModel.kt
```

**Scoring:**
- 4+ distinct coordinators used: 8/8
- 3 coordinators: 6/8
- <3 coordinators: 3/8

### 8B: ViewModel Anti-Pattern Check (7 points)

```bash
# Business logic leaking into ViewModel?
grep -cE "when\s*\(" HomeViewModel.kt     # Complex branching (expect <20)
wc -l HomeViewModel.kt                      # Size (expect <2000)
```

**Scoring:**
- <20 when statements AND <2000 lines: 7/7
- <30 when statements AND <2500 lines: 4/7
- Otherwise: 0/7

---

# PART C: REPORT GENERATION

---

## Step 9: Calculate Total Score

**Scoring Formula:**
```
STRUCTURE (50 points):
  - File Structure:      15
  - Zero Android:        15
  - Interface/Impl:      10
  - V1 Mapping:          10

PURITY (50 points):
  - Data Layer Purity:   20 (MetaHub 10 + Formatting 10)
  - Domain Layer Purity: 15 (Platform 10 + HTTP 5)
  - Presentation Purity: 15 (Delegation 8 + Anti-Pattern 7)

TOTAL: 100 points
```

**Alignment Levels:**
- **95-100%**: 🟢 EXCELLENT — Production-ready
- **85-94%**: 🟡 GOOD — Minor gaps
- **70-84%**: 🟠 FAIR — Notable gaps
- **<70%**: 🔴 POOR — Significant work needed

**Blocking Gates (auto-fail regardless of total):**
- Any Android imports in domain → 🔴 BLOCKED
- Data layer imports MetaHub → 🔴 BLOCKED for KMP

---

## Step 10: Generate Report

```markdown
# Architecture Alignment Report

**Date:** YYYY-MM-DD
**Target:** RealizeTheArchi.md
**Branch:** <branch name>

## Summary

### Structure Checks (X/50)

| Category | Score | Max |
|----------|-------|-----|
| File Structure | X | 15 |
| Zero Android Imports | X | 15 |
| Interface/Impl Separation | X | 10 |
| V1 Module Mapping | X | 10 |

### Purity Checks (X/50)

| Category | Score | Max |
|----------|-------|-----|
| Data Layer: No MetaHub | X | 10 |
| Data Layer: No Formatting | X | 10 |
| Domain: No Platform Imports | X | 10 |
| Domain: No HTTP | X | 5 |
| Presentation: Delegation | X | 8 |
| Presentation: Anti-Patterns | X | 7 |

### Total: X/100 (X%)

**Alignment Level:** 🟢/🟡/🟠/🔴

## Layer Blending Violations

| File | Violation | Should Be In |
|------|-----------|--------------|
| TingwuRunner.kt | metaHub.upsert() | TranscriptionCoordinator |
| TingwuRunner.kt | buildMarkdown() | TranscriptFormatter (domain) |

## Remediation Plan

| Priority | File | Violation | Action | Effort |
|----------|------|-----------|--------|--------|
| P0 | ... | ... | ... | ... |

## Evidence Log

<grep/find outputs>
```

---

## Principles

1. **Structure + Purity** — Both matter, check both
2. **Evidence-based** — Every claim backed by grep output
3. **Layer rules enforced** — Data=I/O, Domain=Logic, Presentation=Orchestration
4. **Blocking gates** — Some violations auto-fail
5. **Actionable** — Each gap has remediation

---

## Layer Rules Reference

| Layer | Location | Should Contain | Should NOT Contain |
|-------|----------|----------------|-------------------|
| **Data** | `data/`, `*Runner.kt` | HTTP, parsing, storage | MetaHub, formatting, business logic |
| **Domain** | `domain/` | Business rules, formatting, orchestration | HTTP, Android imports, platform layer |
| **Presentation** | `*ViewModel.kt` | UI state, event handling, delegation | Business logic, HTTP |

---

## When to Run

- After major refactoring waves
- Before declaring milestones complete
- When onboarding new team members
- Quarterly architecture health checks
- Before starting KMP migration
