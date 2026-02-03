---
description: Explain complex code flows in plain language with step-by-step visual traces
---

# Explainer Tool

When invoked, trace a code flow or system behavior and explain it in **plain language** that a non-expert can follow.

---

## When to Use

| Scenario | Use This? |
|----------|-----------|
| "What happens when I tap this button?" | ✅ Yes |
| "Trace the data flow from X to Y" | ✅ Yes |
| "I don't understand how this works" | ✅ Yes |
| "Debug why X isn't working" | ⚠️ Start here, then `/06-audit` |
| Code review | ❌ Use `/01-senior-reviewr` |

---

## Output Format

Every explanation should include:

### 1. Entry Point Summary
```markdown
## You Do [Action]
What the user does to trigger the flow
```

### 2. Step-by-Step Trace
For each step:
```markdown
### Step N: [Plain Language Title]
**What**: One sentence explaining what happens  
**Where**: [File.kt LNN](file:///absolute/path#LNN)  
**Input**: What goes in  
**Result**: What comes out
```

### 3. Visual Flow Diagram
ASCII box diagram showing the journey:
```
┌─────────────────────────────────────┐
│  Step description                   │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│  Next step                          │
└─────────────────────────────────────┘
```

### 4. Debug Command
If applicable, provide a debug command:
```bash
adb logcat -s TagName:D
```

---

## Plain Language Rules

| ❌ Don't Say | ✅ Say Instead |
|-------------|----------------|
| "Invokes the coroutine dispatcher" | "Starts a background task" |
| "Emits to the StateFlow" | "Updates the UI" |
| "Delegates to the repository implementation" | "Saves data to storage" |
| "The DI container injects" | "The app provides" |
| "Processes the suspend function" | "Waits for the result" |

---

## Example Invocations

**User**: "What happens when I tap the record button?"

**Agent Response**:
```markdown
## You Tap the Record Button

### Step 1: Button Shows Recording State
**What**: The mic icon turns red to show it's listening
**Where**: RecordButton.kt L45
**Result**: UI updates to "recording" state

### Step 2: AudioRecorder Starts Capturing
**What**: Phone microphone starts picking up sound
**Where**: AudioRecorder.kt L88
**Result**: Audio bytes flow to the app

...
```

---

## Depth Guidelines

| User Says | Depth |
|-----------|-------|
| "Briefly explain" | 3-5 steps, high level |
| "Explain" | 5-10 steps, medium detail |
| "Deep dive" / "Comprehensive" | All steps, full code refs |

---

## Code Reference Style

Always include clickable file links:
```markdown
[FileName.kt L42](file:///absolute/path/to/FileName.kt#L42)
```

For ranges:
```markdown
[FileName.kt L42-L55](file:///absolute/path/to/FileName.kt#L42-L55)
```

---

## When Flow Breaks

If user reports "it's not working":

1. Create the expected flow diagram
2. Identify key checkpoints with debug tags
3. Provide `adb logcat` command to verify each checkpoint
4. Ask user to run and report which checkpoint fails

```markdown
## 🔍 Debug Checkpoints

| Step | Tag to Look For | Expected Log |
|------|-----------------|--------------|
| 1 | SchedulerVM | "🧪 Simulating transcript: ..." |
| 2 | SchedulerVM | "🧪 Pipeline result: ..." |
| 3 | CalendarRepo | "Inserting event: ..." |

Run: `adb logcat -s SchedulerVM:D,CalendarRepo:D`
```

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|-------|
| Use jargon without explanation | Define terms on first use |
| Show raw code without context | Add "What" before "Where" |
| Skip the visual diagram | Always include ASCII flow |
| Assume user knows the codebase | Start from user action |
| Dump 20 steps at once | Group into logical phases |
