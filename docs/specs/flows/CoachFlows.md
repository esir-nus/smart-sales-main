# Coach Flows

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md §3 (Coach Flows)

---

## 3.7 Simple Chat
**Scenario:** User chats about sales techniques.
Top-layer chat UI. No special cards.

## 3.8 Memory Search Trigger
**Scenario:** User asks vague question "What about that price issue?"
**System:**
1. LLM identifies ambiguity.
2. Triggers `MemorySearch`.
3. UI shows Thinking Box:
   `Note: > Searching "price issue" in Relevancy Library...`

## 3.9 Suggest Analyst Switch
**Scenario:** User asks complex data question.
**System:**
1. LLM (Check A) identifies "Deep Analysis Needed".
2. Appends suggestion block to response.
3. UI renders: `[ 💡 Deep analysis suggested... Switch to Analyst ]`
**User:** Taps button → Mode toggles to Analyst (Blue theme).

## 3.10 Conflict Resolution (In Chat)
**Scenario:** Coach detects schedule conflict during conversation.
**System:** Renders `ConflictCard` inline in chat stream (same component as Scheduler).
