---
description: Generate a session handoff prompt for seamless continuation in a new chat
---

# Handoff Workflow

Use this workflow when the current session is ending or context is getting long, and you need to continue work in a fresh chat session.

## Steps

1. **Update the handoff document** with current state:
   - File: `/home/cslh-frank/.gemini/antigravity/knowledge/smart_sales_architecture_rework/artifacts/handover_prompt.md`
   - Include: current task, recent changes, next steps, blockers

2. **Generate a resume prompt** for the user to copy into a new session:

```
Continue the Smart Sales architecture refactoring. Read the handoff document at:
/home/cslh-frank/.gemini/antigravity/knowledge/smart_sales_architecture_rework/artifacts/handover_prompt.md

Resume from where we left off.
```

3. **Provide the prompt** to the user, ready to paste into a new chat.

---

## Quick Resume Prompt (Copy This)

```
@handover_prompt.md - Resume the session from this handoff document.
```

Or if direct file reference doesn't work:

```
Read /home/cslh-frank/.gemini/antigravity/knowledge/smart_sales_architecture_rework/artifacts/handover_prompt.md and continue from where we left off.
```
