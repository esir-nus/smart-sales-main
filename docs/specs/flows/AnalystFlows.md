# Analyst Flows

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md §3 (Analyst Flows)

---

## 3.11 Full Analysis (Plan Card with Chat Hints)
**Scenario:** User: "Analyze Zhang's buying cycle."

```
USER ACTION                          SYSTEM RESPONSE
───────────                          ───────────────
Switch to Analyst Mode               Theme: Blue. Right Dock Visible.
Input: "Analyze Zhang..."            Thinking Box: Active (Streaming)
                                     
                                     Create Plan Card:
                                     ┌──────────────────────────────┐
                                     │ 📋 分析计划 (Analysis Plan)   │
                                     │ ☑ 1. 概况总结 (Summary) [完成] │
                                     │ ☐ 2. 周期计算         [等待] │
                                     │ ☐ 3. 策略生成         [等待] │
                                     └──────────────────────────────┘
                                     
                                     [AI - Analyst]:
                                     "计划已生成。第一步已完成。
                                     是否继续进行周期计算？"
                                     
Input: "Yes, go ahead."              Tool executes Step 2.
                                     Card updates: Step 2 [完成]
```

**Chat-Hint Interaction:**
*   Instead of `[Run]` buttons, the AI **verbally proposes** the next action.
*   User confirms via text/voice ("Yes", "Run next", "Do it").
*   **Why?** Maintains conversational flow & authority of the Analyst.

## 3.12 Follow-up Chat
**Scenario:** User asks question while Plan is active.
**UI:** Plan Card **remains visible** at top of chat stream (re-injected context). New Q&A happens below it.
