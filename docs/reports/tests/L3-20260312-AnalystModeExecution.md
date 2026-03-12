# L2/L3 On-Device Test Record: Analyst Mode LLM Execution (SIMPLE_QA)

**Date**: 2026-03-12
**Tester**: CSLH-Frank / Agent
**Target Build**: `app-core:assembleDebug`

---

## 1. Test Context & Entry State
* **Objective**: Verify that `SIMPLE_QA` and `DEEP_ANALYSIS` intents successfully execute the LLM Analyst prompt and return valid text responses to the UI, bypassing the previous "early-exit ETL debug log" bug.
* **Testing Medium**: L3 Physical Device Test (Real LLM)
* **Initial Device State**: Agent timeline active, `ent_201` (张伟) profile context loaded in RAM/SSD with recent updates to his job title.

## 2. Execution Plan
* **Trigger Action**: User input for factual queries.
* **Input Payload 1**: "张伟现在的职务是什么？"
* **Input Payload 2**: "总结一下张伟目前的跟进记录"

## 3. Expected vs Actual Results

| Checkpoint | Expected Behavior | Actual Behavior | Result |
| :--- | :--- | :--- | :---: |
| **UI Literal** | AI answers factual queries based on database (e.g., "采购总监") | User screenshots explicitly verify natural language answers citing the correct database facts. | ✅ |
| **Log Evidence** | `RealUnifiedPipeline: 🤖 Executing LLM Analyst Prompt...` | Logs confirm exact execution trace traversing the new code block and delegating to `qwen-max`. | ✅ |
| **Negative Check**| Must NOT prematurely return "Unified Pipeline ETL assembled successfully" | Failed debug string no longer appears. | ✅ |

---

## 5. Final Verdict
**[✅ SHIPPED]**. 
