## You Traced the JSON Crash

### Step 1: LLM Generates Intent (The "Brain")
**What**: The AI correctly understands your request and tries to output the required JSON format.
**Where**: RealUnifiedPipeline

### Step 2: The Hallucination (Explicit Nulls)
**What**: Instead of leaving out a field it doesn't need (which lets our code use its own default value), the AI unexpectedly writes `"classification": null` directly into the JSON.
**Where**: RealUnifiedPipeline

### Step 3: The Strict Linter Crashes (The Bug)
**What**: Our code (the Linter) intercepts the JSON to convert it into a strictly typed Kotlin object. Our code expects a non-nullable String (which defaults to `"schedulable"`). Because the JSON literally said `null`, the Kotlin parser panics and crashes the whole process rather than guessing.
**Where**: [SchedulerLinter.kt L21](file:///home/cslh-frank/main_app/domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt#L21)
**Result**: UI shows "JSON解析失败" (JSON Parsing Failed) and drops the context.

### Step 4: The Hotfix (Coerce Input Values)
**What**: We updated the JSON parser's settings to say: "If the AI gives you a `null` but our Kotlin code requires a real value and has a default fallback, force (coerce) it back to our safe default value."
**Where**: [SchedulerLinter.kt L23](file:///home/cslh-frank/main_app/domain/scheduler/src/main/java/com/smartsales/prism/domain/scheduler/SchedulerLinter.kt#L23)
**Result**: The payload parses safely, ignoring the hallucinated null and using `"schedulable"`. The UI continues flawlessly.

## Visual Flow (The Deserialization Defense)

```text
┌─────────────────────────────────────┐
│ 1. LLM Outputs:                     │
│    { "classification": null }       │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│ 2. JSON Linter (Before Hotfix):     │
│    "Wait, classification can't be   │
│     null! CRASH!"                   │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│ 3. JSON Linter (After Hotfix):      │
│    "I see 'null'. Forcing default   │
│     value 'schedulable' instead."   │
└─────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────┐
│ 4. Success. Card displays on UI.    │
└─────────────────────────────────────┘
```

## 🔍 Debug Checkpoints

If parsing fails again, you can trace the exact JSON the LLM returned before the crash happens:

| Step | Tag to Look For | Expected Log |
|------|-----------------|--------------|
| 1 | RealHabitListener | `JSON input: { ... }` (This prints the exact corrupted JSON if it fails) |
| 2 | RealUnifiedPipeline | *(Look for standard Pipeline errors during parsing)* |

**Run**: `adb logcat -s RealHabitListener:W RealUnifiedPipeline:E`
