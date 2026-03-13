---
description: Run the Verification Acceptance Team to validate work
---

# Acceptance Team

> **Purpose**: Automated, blackbox verification of work products.
> **Role**: The "Teacher grading the Multiple Choice exam." Does not care about effort, only correctness.
> **Philosophy**: Trust is good, proof is better.

## When to Use

- **Always** before marking a task as "Done"
- **Always** before asking for human review
- **Never** during early drafting (too noisy)

## The Examiners

The acceptance team consists of specialized examiners. You (the agent) act as the **Orchestrator**, invoking each examiner and aggregating results.

### 1. Spec Examiner 📜
*Ensures the code implements the written requirement.*

- **Input**: `docs/cerb/[feature]/spec.md` + Changed Files
- **Command**:
  ```bash
  # Check if code implements spec requirements literally
  grep -r "key_requirement" [file_path]
  ```
- **Rubric**:
  - Does every field in the spec model exist in the code?
  - Do state transitions in spec match `sealed class` or `enum`?
  - Are all "MUST" requirements locally verifiable?

### 2. Contract Examiner 🤝
*Ensures architectural integrity and ownership rules.*

- **Input**: `docs/cerb/interface-map.md` + Changed Files
- **Command**:
  ```bash
  # Check for forbidden imports
  grep -r "import com.smartsales.prism.data" [feature_domain_path]
  ```
- **Rubric**:
  - Does the code import only from allowed layers? (See `interface-map.md`)
  - Are "Writes" going through the owner module?
  - Are "Reads" going through the public interface?

### 3. Build Examiner 🏗️
*Ensures it actually works.*

- **Input**: The codebase
- **Command**:
  ```bash
  ./gradlew assembleDebug && ./gradlew testDebugUnitTest
  ```
- **Rubric**:
  - Build successful?
  - All unit tests passed?
  - No new lint warnings?

### 4. Break-It Examiner 🔨
*Tries to break the implementation with edge cases.*

- **Input**: Public Interface functions
- **Action**: You (the agent) write a temporary test or script to send:
  - `null` / `empty string` / `blank string`
  - Max length inputs
  - Special characters / Emoji
  - Concurrent calls (if applicable)
- **Rubric**:
  - Does it crash?
  - Does it return a sensible error?
  - Is the state corrupted?

---

## Workflow Steps

1. **Self-Select Examiners**:
   - If UI change: Spec + Break-It
   - If Logic change: Spec + Contract + Build + Break-It
   - If Refactor: Contract + Build

2. **Run Execution**:
   - Run the commands/checks for each selected examiner.
   - **DO NOT** skip steps because "it looks simple."

3. **Generate Report**:

```markdown
# Acceptance Report

## 1. Spec Examiner 📜
- [x] Field `keyPerson` exists in `ScheduledTask`
- [ ] Requirement "Sort by date" verified (MISSING)

## 2. Contract Examiner 🤝
- [x] No `android.*` imports in domain
- [x] Writes go via `EntityWriter`

## 3. Build Examiner 🏗️
- [x] Build Success
- [x] Tests: 45 passed, 0 failed

## 4. Break-It Examiner 🔨
- [x] Input `null`: Handled gracefully (returns default)
- [x] Input "   ": Throws IllegalArgumentException (Correct)

## Verdict
🛑 FAIL - Missing "Sort by date" implementation.
```

4. **Iterate**:
   - If FAIL: Fix code -> Re-run relevant examiner.
   - If PASS: Mark task as Done.
