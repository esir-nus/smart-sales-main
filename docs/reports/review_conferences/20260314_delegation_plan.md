# 📋 Delegation Enforcement Plan: Hardware Badge Scheduling

**Subject**: Enforcing the architectural constraint that scheduling is physically delegated to the Hardware Badge, and the software LLM should NOT generate JSON scheduling data.
**Targeted Specs**: `docs/cerb/scheduler/spec.md`, `docs/cerb-ui/agent-intelligence/spec.md`
**Code Targets**: `PromptCompiler.kt`

---

## Stage 1: `/cerb-check` Audit 

**Target**: `docs/cerb/scheduler/spec.md`
**Classification**: Feature

### Universal Rules
- [x] Link Purity (No spec-to-spec logic chains)
- [x] Self-containment (Models/Edge cases inline)
- [x] Wave Plan is present.

### Domain-Specific Checks (Feature)
- [x] **OS Layer**: Declared as `RAM Application (reads entity context from SessionWorkingSet Section 1)`.
- [x] **Interface Clarity**: `ScheduledTaskRepository`, `SchedulerLinter`, `AlarmScheduler` explicitly defined.
- [x] **Boundary Guard**: "You Should NOT" section is present and explicitly forbids manual parsing of JSON and reading data sources directly.

**Verdict**: ✅ **PASS**. The Scheduler spec is highly compliant. However, it requires a structural update to reflect the new **Hardware Badge Delegation**.

---

## Stage 2: `/01-senior-reviewr` Approach Assessment

### 🔴 The Current Risk (Prompt Hallucination)
Currently, `PromptCompiler.kt` (Lines 191-201) explicitly instructs the LLM on how to generate scheduling JSON:
> "如果用户意图包含日程安排... 必须提供 tasks 数组。推断合理 urgency... duration推断..."

If the user says "Schedule a meeting with Frank tomorrow", the LLM will try to generate a `UnifiedMutation` with tasks. 
**The Goal is to stop this.** We want the LLM to simply output `classification: "schedulable"`, but *without* the `tasks` JSON block, and instead let the Mascot UI hint the user to tap the badge.

### 💡 The Pragmatic Path (What we should actually do)

1. **Modify `PromptCompiler.kt`**:
   - Change the `crm_task` or `query_quality` rules. When the user asks to schedule something verbally to the phone, the LLM should identify the intent as `badge_delegation` (or similar).
   - The LLM should return a lightweight response telling the Mascot to hint the user: "Sure, please tap your physical badge to record the schedule."
   - **Crucially**: Remove the instructions that force the LLM to generate `tasks` arrays from normal phone audio.

2. **Update `docs/cerb/scheduler/spec.md`**:
   - Explicitly document the "Hardware Badge Constraint" in the **Voice Command Scope** section.
   - Define that phone-based voice scheduling is explicitly rejected in favor of the badge.

3. **Update `docs/cerb-ui/agent-intelligence/spec.md`**:
   - Add a new `UiState.BadgeHint` (or similar) to Section 2 (Core Visual Mechanics).
   - Document that when the Pipeline returns a delegation result, the Presentation layer renders the Mascot pointing to the badge.

---

## Stage 3: Implementation Strategy

### Step 1: Interface & State Updates
Create the `UiState` representing the hardware handoff.

**File:** `docs/cerb-ui/agent-intelligence/spec.md`
```markdown
### 2.7 Hardware Handoff Bubble (`UiState.BadgeDelegationHint`)
Triggered when the user attempts a complex spatial/temporal task (like scheduling) without using the dedicated hardware badge.
- **Visuals**: A highly visible, pulsating Mascot hint: "Please tap your Smart Badge to schedule this."
```

### Step 2: PromptCompiler Overhaul
Modify `PromptCompiler.kt` to intercept scheduling attempts.

**File:** `core/pipeline/src/main/java/com/smartsales/core/pipeline/PromptCompiler.kt`
*Change the Intent Rules:*
```text
7. `badge_delegation`: 如果用户试图安排日程、开会、设置提醒（如"明天下午2点开会"），你必须拦截该意图。
不要生成任何 JSON 任务数据。你应该在 response 中回复："好的，请长按您的智能工牌专属按键来录入此日程。" 
并将 query_quality 设为 badge_delegation。
```

### Step 3: Pipeline & Router Wiring
To ensure the system doesn't just treat `badge_delegation` as a generic `VAGUE` intent, we must wire the pipeline:
1. **`LightningRouter.kt`**: Add `BADGE_DELEGATION` to the `QueryQuality` enum.
2. **`RealLightningRouter.kt`**: Parse `"badge_delegation"` into `QueryQuality.BADGE_DELEGATION`.
3. **`docs/cerb/unified-pipeline/interface.md`**: Add `BadgeDelegationIntercepted` to the `PipelineResult` sealed class.
4. **`IntentOrchestrator.kt`**: Add a new `when` branch for `QueryQuality.BADGE_DELEGATION` to short-circuit the pipeline and emit the new `PipelineResult.BadgeDelegationIntercepted`.

### Step 4: Spec Sync
Ensure `scheduler/spec.md` reflects this so future agents don't try to "fix" the lack of JSON scheduling.

**File:** `docs/cerb/scheduler/spec.md`
```markdown
> [!CAUTION]
> **HARDWARE DELEGATION STRICT POLICY**
> Scheduling intent detected via standard Phone App Mic is strictly intercepted by the LightningRouter at Phase 0.
> The LLM will intentionally refuse to generate `tasks` JSON, and will instead trigger a Mascot hint to use the physical ESP32 Badge.
> Only audio routed through the `BadgeAudioPipeline` is permitted to generate actual TimelineItem records.
```

---
## Phase 3: Implementation Plan (Feature Dev Planner)

### Feature: Hardware Badge Delegation
### Owning Specs: `docs/cerb/lightning-router/spec.md`, `docs/cerb/scheduler/spec.md`
### Target Wave: Wave 6 (Crucible)

### Anti-Drift Audit
- [x] Cerb Scope Declaration filled (Exception applied: Cross-cutting architectural boundary between Router & Scheduler).
- [x] ALL business logic quotes from owning spec ONLY.
- [x] OS Layer classified (RAM Application layer).
- [x] LLM vs Kotlin justified (LLM for intent extraction -> Kotlin for hardcoded UI loop and `IntentOrchestrator` short-circuiting).
- [x] Fakes clean (no skeleton data) - N/A for this specific routing change.
- [x] L2 scenario planned (`AgentViewModelTest` will be updated to inject `UiState.BadgeDelegationHint` directly).
- [x] Tagged `Log.d` calls added to trace data flow (`adb logcat -s Tag:D` for `LightningRouter` and `IntentOrchestrator`).
- [x] **Mandatory Mechanical Verification:** Run Gradle assembleDebug after wiring new `QueryQuality` Enum.

