# Lightning Fast-Track E2E Spec

> **OS Layer**: Testing Environment (App / RAM)

## Execution Scenarios

### 1. The Mascot Short-Circuit (NOISE)
- **Inject**: Unintelligible ASR artifact ("嗯嗯").
- **State**: Minimal Context.
- **Expect**: `IntentOrchestrator` routes immediately to `MascotService` without invoking `UnifiedPipeline`. Emits nothing.

### 2. The Mascot Short-Circuit (GREETING)
- **Inject**: Pure social greeting ("你好啊").
- **State**: Minimal Context.
- **Expect**: Routed specifically to `MascotService`. Emits nothing.

### 3. The Clarification Trap (VAGUE)
- **Inject**: Ambiguous feature request ("那个功能").
- **State**: Minimal Context.
- **Expect**: `LightningRouter` returns a vague clarification. `IntentOrchestrator` intercepts and emits `PipelineResult.ConversationalReply` directly.

### 4. The Unified Pipeline Handoff (DEEP_ANALYSIS & CRM_TASK)
- **Inject**: Multi-step reasoning request ("帮我分析一下华为的最新进展").
- **State**: Minimal Context.
- **Expect**: `IntentOrchestrator` passes intent to `UnifiedPipeline` and forwards its emission.

### 5. *BLOCKED*: The Lightning Answer (SIMPLE_QA)
- **Inject**: Factual query soluble from RAM.
- **State**: Minimal Context.
- **Expect**: Fast-tracked by the router returning an immediate answer. 
- **Current Status**: ❌ Spec Drift (Currently falls through to `UnifiedPipeline`).

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Anti-Drift Remediation | 🔲 PLANNED | Align `IntentOrchestrator` to handle `SIMPLE_QA` per spec. |
| **2** | Mock Eviction Structure | 🔲 PLANNED | Setup `RealIntentOrchestratorTest` with injected Fakes. |
| **3** | Scenario Executions | 🔲 PLANNED | All 5 Context Branches written and passing L2 simulation. |
