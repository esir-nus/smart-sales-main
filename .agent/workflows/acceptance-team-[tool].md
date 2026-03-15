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
*Ensures architectural integrity, ownership rules, and strict Brain/Body data contracts via MECHANICAL validation.*

- **Input**: `docs/cerb/interface-map.md` + `docs/plans/telemetry/pipeline-valves.md` + Changed Files
- **Command**: MUST run a result-oriented mechanical script. **"Vibe checking" by reading code is strictly FORBIDDEN.**
  ```bash
  # 1. OS Layer Isolation (Forbidden Imports)
  grep -r "import com.smartsales.prism.data" [feature_domain_path]
  grep -r "import android." [core_pipeline_path]
  
  # 2. Strict Mathematical Alignment (Anti-Hallucination Guardrails)
  # MUST explicitly run these two project-level Contract Verify scripts if the domain or UI changed:
  # A) Brain/Body Contract (LLM Schema vs Data Class)
  ./gradlew :app-core:testDebugUnitTest --tests "*PromptCompilerBadgeTest*"
  # B) UI/Skin Contract (Markdown Docs vs Kotlin UiState)
  ./gradlew :app-core:testDebugUnitTest --tests "*UiSpecAlignmentTest*"

  # 3. Telemetry Alignment (Pipeline Valves)
  grep -rn "PipelineValve.tag" [feature_path]
  ```
- **Rubric**:
  - Did the mechanical `grep` script return 0 forbidden imports?
  - Did `PromptCompilerBadgeTest` pass? (Proves Brain/Body schema alignment)
  - Did `UiSpecAlignmentTest` pass? (Proves Docs-First UI protocol)
  - Are "Writes" going through the owner module as defined in `interface-map.md`? (Verified via grep, not guessing)
  - If a core pipeline boundary was crossed, is there a `PipelineValve.tag` matching `pipeline-valves.md`?

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
- [x] Writes go via `EntityWriter` (matches `interface-map.md`)
- [x] Target feature emits `PipelineValve.tag[PLUGIN_DISPATCH_RECEIVED]`

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
