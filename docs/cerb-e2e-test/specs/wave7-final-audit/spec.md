# Wave 7 Final Audit Specification: The 6 Core Pillars

> **OS Layer**: L3 On-Device End-to-End Application Testing

## Execution Scenarios (Mandatory)

> **Rule**: Every test must prove both Cold Start (Zero Context) and Warm Start (Chaos Context) to prevent Testing Illusions.

### Pillar 1 & 2: Lightning Fast-Track & Dual-Engine Bridge
*Objective: Prove System I (Fast) and System II (Slow) operate independently without blocking each other.*

#### Scenario 1: Out-of-Band Noise
- **State**: `WorldStateSeeder` is EMPTY.
- **Inject**: User Voice/Text: `"嗯好先这样吧。"` (Yeah ok, that's it for now.)
- **Expect**: `LightningRouter` classifies as `NOISE`. 
- **Proof**: Mascot UI disappears or acknowledges instantly (< 500ms). ContextBuilder is NEVER invoked. No heavy SSD or LLM pipeline logs appear.

#### Scenario 2: Deep Analysis Interleaved
- **State**: `WorldStateSeeder` is loaded with full `dataset.md` chaos.
- **Inject**: `"总结一下咱们跟字节跳动第一季度的所有互动，并且给我一条跟进建议。"`
- **Expect**: `LightningRouter` assigns `DEEP_ANALYSIS`. 
- **Proof**: `ContextBuilder` triggers `ContextDepth.FULL`. `UiState.Thinking` actively pulses. LLM generates a localized Markdown response. Mascot Engine is completely bypassed.

### Pillar 3 & 4: Strict Interface Integrity & Adaptive Habit Loop
*Objective: Prove the LLM data class mappings (The "One Currency" Rule) and background RL operations.*

#### Scenario 3: The Background Learner
- **State**: `WorldStateSeeder` is running a simulated multi-turn session.
- **Inject**: `"张伟说他们对私有化部署的报价有点敏感。"` (Zhang Wei said they are sensitive to the on-premise pricing.)
- **Expect**: The Chat Pipeline updates the timeline instantly with this Clue.
- **Proof**: The asynchronous RL Module (`:domain:habit`) passively observes the trace, successfully extracting the user's habit (Price Sensitivity) via the `RlPayload` schema without blocking the UI. `RlPayloadSchemaTest` verifies the JSON mapping exactly matches the data class.

### Pillar 5 & 6: Efficiency Overload & Transparent Mind
*Objective: Prove that RAM loading is contextual and that UI visibility accurately reflects the pipeline state.*

#### Scenario 4: Fast Factual Retrieval (Amnesia Check)
- **State**: `WorldStateSeeder` chaos loaded.
- **Inject**: `"今天下午3点我要跟张总碰一下，他们之前的核心顾虑是啥？" `
- **Expect**: `LightningRouter` assigns `SIMPLE_QA`.
- **Proof**: Context depth is `MINIMAL`, bypassing `UserHabitDao` and `ScheduleBoard` entirely to save tokens. The `UiState.Thinking` explicitly hints "正在查询最近记录...". The Entity Pointer is successfully resolved by the `AliasCache` and carried through to the answer without dropping.

#### Scenario 5: Intent Routing Overload Defense (Safety Red Line)
- **State**: `WorldStateSeeder` chaos loaded.
- **Inject**: `"张伟那边说审批差不多了，把阶段往后挪一下，改成已经赢单，金额是120万"`
- **Expect**: The system halts at an Open-Loop `PROPOSAL` boundary.
- **Proof**: `RealUnifiedPipeline` does NOT allow a physical write to the DB. A UI Confirmation Card asks the user for explicit permission to update the `amount` and `stage`. System does NOT fall back to creating a Scheduler note.

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Architecture Sanity Check | 🔲 PLANNED | Validate boundaries and OS Layer conformance |
| **2** | System I Validation | 🔲 PLANNED | Scenarios 1 & 2 (Lightning Router / Noise / QA) |
| **3** | System II Execution | 🔲 PLANNED | Scenarios 3, 4, 5 (Context Assembly, Writes, RL) |
