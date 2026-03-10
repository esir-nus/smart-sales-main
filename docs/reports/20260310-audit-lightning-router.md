# 🛡️ Code Audit: Lightning Router (Phase 0)

## 1. Context & Scope
- **Target**: `LightningRouter.kt`, `IntentOrchestrator.kt`, and associated tests.
- **Goal**: Establish baseline understanding and code-spec alignment before resuming Phase 3 E2E Device Tests for the Lightning Fast-Track.

## 2. Existence Verification
- [x] `LightningRouter.kt`: Found at `core/pipeline/src/main/java/com/smartsales/core/pipeline/LightningRouter.kt`
- [x] `IntentOrchestrator.kt`: Found at `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`
- [x] `IntentOrchestratorTest.kt`: Found at `core/pipeline/src/test/java/com/smartsales/core/pipeline/IntentOrchestratorTest.kt`
- [x] `LightningRouter` Spec: Found at `docs/cerb/lightning-router/spec.md`

## 3. Logic Analysis
- **Implementation**: `IntentOrchestrator` uses `LightningRouter` to evaluate the input against a `MINIMAL` context. It explicitly intercepts `NOISE` and `GREETING` and routes them to `MascotService`. It routes `VAGUE` to a clarification response. Any other `QueryQuality` falls through to the heavy-duty `UnifiedPipeline`.
- **Dependencies**: Depends on `ContextBuilder`, `MascotService`, `UnifiedPipeline`.
- **Gaps/Risks**: The `SIMPLE_QA` intent is completely missing from the `IntentOrchestrator` `when` block.

## 4. Test Coverage
- **Test File**: `IntentOrchestratorTest.kt` (Found)
- **Coverage**: L1 Unit Tests cover `NOISE`, `GREETING`, `VAGUE`, and `DEEP_ANALYSIS`. However, these tests use `FakeUnifiedPipeline` and `FakeLightningRouter`. For Phase 3 E2E Pillar Resumption, a new integration test mapping `RealLightningRouter` to `RealIntentOrchestrator` without Mockito is required.

---

## 5. Literal Spec Alignment Audit: `IntentOrchestrator` vs `spec.md §1`

| Spec Line # | Spec Says (VERBATIM) | Code Says (VERBATIM) | Match? |
|-------------|----------------------|----------------------|--------|
| L21 | `3. **SIMPLE_QA**: Factual queries solvable purely from the minimal loaded RAM without a structured plan.` | Missing from `IntentOrchestrator` explicit handling. | ❌ NO |
| L22 | `*Routing*: Fast-tracked by the Lightning Router itself using qwen-plus, returning an immediate answer.` | `else -> { val pipelineInput = ... unifiedPipeline.processInput... }` | ❌ NO |
| L32 | `If query_quality is NOISE or GREETINGS, UnifiedPipeline is never called.` | `QueryQuality.NOISE, QueryQuality.GREETING -> { mascotService.interact... }` | ✅ YES |

## 6. Conclusion
- **Ready to Proceed?**: **CAUTION / BLOCKED**
- **Missing Information**: We have a strict violation of the Literal Spec Alignment Gate. The code delegates `SIMPLE_QA` to the `UnifiedPipeline` instead of fast-tracking it, which violates Wave 4's explicit definition. Before writing E2E device tests, we must either update the `IntentOrchestrator` to implement the `SIMPLE_QA` fast-track, or downgrade the spec to remove it.
