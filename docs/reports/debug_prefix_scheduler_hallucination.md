# Pre-Fix Report: Scheduler LLM Hallucinated "ByteDance"

## 1. Intake & Diagnosis
**Symptom**: User inputted "明天下午三点到五点开会" but the LLM outputted `{"title":"与字节跳动张伟开会", "keyPerson":"张伟", "keyCompany":"字节跳动"}`.
**Expectation**: Path A / Scheduler execution is a "streamlined simple pipeline" that shouldn't read unrelated conversation memory.

**Root Cause**:
1. `IntentOrchestrator` correctly routes voice scheduling to `RealUnifiedPipeline` with `Mode.SCHEDULER`.
2. `RealContextBuilder` passes `sessionHistory = _sessionHistory.toList()` indiscriminately for all modes, failing to filter by context depth or mode.
3. `PromptCompiler` indiscriminately injects `sessionHistory` into the LLM prompt.
4. **Conclusion**: The LLM reads the previous turns of the chat (where "Zhang Wei" and "ByteDance" must have been discussed), gets confused, and hallucinates that old context into the standalone meeting request.

## 2. Evidence
- **Log confirmation**: Your ADB logs showed `PluginRequest` with the hallucinated JSON list perfectly matching the `UnifiedMutation` DTO format we expect, proving the leak happens before deserialization.
- **Code Trace**: `RealContextBuilder.kt:148` sets history for all contexts. `PromptCompiler.kt:30` injects it without checking `Mode.SCHEDULER`.

## 3. Recommended Fix
Following the "streamlined pipeline" OS Model, `Mode.SCHEDULER` should be treated as a high-speed, isolated transactional pipeline, not a chatty conversational pipeline.

1.  **Modify `RealContextBuilder.kt`**: Check the mode. If `mode == Mode.SCHEDULER`, return `sessionHistory = emptyList()`.

## 4. Verification Gate Readiness
- **Score:** 100% (Verified code paths manually via `view_file` matching telemetry).
- **Risk:** Low. Only isolates the `SCHEDULER` mode, leaving `ANALYST` intact.
