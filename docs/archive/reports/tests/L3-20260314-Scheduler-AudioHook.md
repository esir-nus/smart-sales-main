# L2/L3 On-Device Test Record: Scheduler DEV Audio Hook

**Date**: 2026-03-14
**Tester**: User
**Target Build**: `:app-core:assembleDebug`

---

## 1. Test Context & Entry State
* **Objective**: Verify that the DEV Mic Audio Hook accurately bypasses the Wave 6 Hardware Delegation logic and invokes the UnifiedPipeline to schedule a task.
* **Testing Medium**: L3 Physical Device Test (Voice ASR -> Pipeline)
* **Initial Device State**: Scheduler UI open, tapping Mic and stating a valid task context.

## 2. Execution Plan
* **Trigger Action**: Tapped DEV Mic Button and spoke scheduling query.
* **Input Payload**: `明天下午三点跟李总开会`

## 3. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | "处理意图..." transitions to Proposal State. A task card appears in Timeline. | 15th Date button highlighted. No Proposal UI and no card appeared. | ❌ |
| **Log Evidence** | `RealUnifiedPipeline` logging pipeline progress. LLM emitting `tasks` array. | LLM `UnifiedMutation` parsed, but `tasks` array forced empty by prompt. | ❌ |
| **Negative Check**| Pipeline does not crash or silently swallow exceptions. | Pipeline ran, but generated empty tasks array due to compiler strictness. | ✅ |

---

## 4. Deviation & Resolution Log
* **Attempt 1 Failure**: The intent parsed strictly without task definitions.
  - **Root Cause**: While BugFix 4.3 permitted the traffic to pass the router (`isVoice=true`), the `PromptCompiler` utilizes a strict mapping rule: "If `isBadge==false`, forbid generation of `tasks` array". `isBadge` was defaulting to `false`.
  - **Resolution**: BugFix 4.4 applied in `IntentOrchestrator.kt`. `PipelineInput` is now explicitly configured with `isBadge = isVoice`, signaling the compiler to unlock task mutation capability for the Dev Audio Hook.

## 5. Final Verdict
**[❌ FAILED]**.

_Next steps: Compile successfully, attach updated payload, and re-test._
