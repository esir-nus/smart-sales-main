# 🛡️ Code Audit: Pipeline Explainer Ghost & Drift Analysis

## 1. Context & Scope
- **Target**: `docs/artifacts/pipeline-explainer.md`
- **Goal**: Perform an evidence-based code audit to identify Drift (code diverges from spec) and Ghost Implementations (spec claims things the code doesn't do) across the L3 Unified Pipeline.

## 2. Existence Verification
- [x] `RealInputParserService.kt`: Found at `core/pipeline/src/main/java/com/smartsales/core/pipeline/RealInputParserService.kt`
- [x] `RealEntityDisambiguationService.kt`: Found at `core/pipeline/src/main/java/com/smartsales/core/pipeline/RealEntityDisambiguationService.kt`
- [x] `RealContextBuilder.kt`: Found at `core/context/src/main/java/com/smartsales/core/context/RealContextBuilder.kt`
- [x] `RealUnifiedPipeline.kt`: Found at `core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt`
- [x] `RealEntityWriter.kt`: Found at `data/crm/src/main/java/com/smartsales/data/crm/writer/RealEntityWriter.kt`

## 3. Logic Analysis vs Explainer Claims

### Claim 1: Step 1 (Fast Parse) launches the Habit Learner
- **Explainer (`pipeline-explainer.md` L22)**: *"The Turbo model instantly extracts the physical clues... (Note: The architecture plans for a background job to start learning habits here, but it is currently [WIP]/Deferred)."*
- **Actual Code**: The habit listener (`habitListener.analyzeAsync`) is **NOT** launched in Step 1 (`RealInputParserService`). It is actually launched deep in Step 4 of `RealUnifiedPipeline.kt` (Line 177) *after* the entire Context Assembly is finished.
- **Verdict**: ⚠️ **DRIFT**. The background job placement is misrepresented in the explainer. It fires much later in the pipeline than Step 1.

### Claim 2: The LLM outputs Habits in its JSON payload
- **Explainer (`pipeline-explainer.md` L36-39)**: *"The heavy LLM reads the PipelineContext package. It figures out the best response and creates a structured JSON output (including the response text, proposed calendar events, and observed habits)."*
- **Actual Code**: The foreground LLM execution (`RealUnifiedPipeline.kt` L186) passes its result solely to `schedulerLinter.lint()`, which extracts Scheduling Data (CRM Task, MultiTask, Reschedule, Deletion). The foreground JSON payload **does not** contain observed habits. Habits are extracted by a completely separate, concurrent LLM execution happening inside `habitListener.analyzeAsync`.
- **Verdict**: 👻 **GHOST IMPLEMENTATION**. The explainer assumes a monolithic JSON payload from a single LLM call. The reality is Dual-LLM executions (Foreground CRM Scheduler + Background RL Listener).

### Claim 3: Disambiguator Halts Pipeline
- **Explainer (`pipeline-explainer.md` L27)**: *"If it's confused, it stops the pipeline and asks the user to clarify."*
- **Actual Code**: `RealUnifiedPipeline.kt` L68-73 correctly intercepts `DisambiguationResult.Intercepted`, emits `PipelineResult.DisambiguationIntercepted` to the UI, and issues a `return@flow`. 
- **Verdict**: ✅ **ALIGNED**.

### Claim 4: Context Assembly doesn't read Raw Chat Logs
- **Explainer (`pipeline-explainer.md` L32)**: *"It pulls the distilled Entity Profile... fetches your global habits, and checks your schedule. It packages this into a strict JSON format. It does not read raw chat logs."*
- **Actual Code**: `RealContextBuilder.kt` L125 explicitly includes `sessionHistory = _sessionHistory.toList()`. This represents the current conversation's turns, not global historical chat logs from SSD. The SSD loads distilled `EntityEntry` objects (L94-100).
- **Verdict**: ✅ **ALIGNED (Semantically)**. It relies on Distilled Entity Profiles from DB, not raw historic vector text matches.

### Claim 5: Twin Engines Write-Through
- **Explainer (`pipeline-explainer.md` L45-46)**: *"Both engines write-through to updating the RAM session and saving to the SSD."*
- **Actual Code**: `RealEntityWriter.kt` L221-222 calls `entityRepository.save(finalEntry)` (SSD) immediately followed by `writeThrough(finalEntry)` (RAM).
- **Verdict**: ✅ **ALIGNED**.

## 4. Conclusion & Next Steps
- **Ready to Proceed?**: **CAUTION**
- **Action Required**: `docs/artifacts/pipeline-explainer.md` contains significant architectural drift regarding how the Dual-Engine (CRM vs RL) actually schedules its LLM calls. It needs to be rewritten to clarify that the RL Engine is an asynchronous fork in Step 4, not a single monolithic JSON payload from Step 4. 

---

## 5. Literal Spec Alignment Audit

| Explainer Line # | Explainer Says (VERBATIM) | Code Says (VERBATIM) | Match? |
|-------------|----------------------|----------------------|--------|
| L22 | `plans for a background job to start learning habits here` (in Step 1) | `habitListener.analyzeAsync(...)` is called in `RealUnifiedPipeline.kt` (Step 4) | ❌ NO |
| L37 | `creates a structured JSON output (including the response text, proposed calendar events, and observed habits)` | `val lintResult = ... schedulerLinter.lint(llmResult.content)` — No habits in this payload. | ❌ NO |
| L45 | `write-through to updating the RAM session and saving to the SSD` | `entityRepository.save(finalEntry)` \n `writeThrough(finalEntry)` | ✅ YES |
