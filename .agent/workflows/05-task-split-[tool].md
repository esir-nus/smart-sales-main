---
description: Diagnose and split tasks that fail due to context/execution limits
---

# Task Split Workflow

Use when a task stalls, times out, or exceeds limits. This workflow helps diagnose the root cause and create an executable split.

## Step 1: Diagnose the Issue

### Check symptoms and classify:

| Symptom | Root Cause | Solution |
|---------|------------|----------|
| Tool call returned truncated | **Over-reading** - too many lines | Split into line-range chunks |
| "UncompletedCoroutinesError" or test timeout | **Async complexity** | Defer to later, simplify scope |
| Build takes 3+ minutes without progress | **Large scope** | Extract smaller atomic units |
| Multiple tool calls pending | **Parallel overload** | Serialize operations |
| Context window exhausted mid-task | **Cumulative read** | Reset context, reference prior work |

## Step 2: Apply the Right Split Strategy

### A. Over-Reading Issues
**Cause**: Single file read exceeds 800-line limit or reading too many files.

**Split approach**:
1. Use `view_file_outline` first (never reads full content)
2. Read in 500-line chunks with `--StartLine` and `--EndLine`
3. Process each chunk before reading next

### B. Chunky Execution Issues
**Cause**: Too many non-trivial operations in one task (writes, builds, tests).

**Split approach**:
1. Break into atomic commits: `Extract A` → verify → `Extract B` → verify
2. Max 2-3 file writes per verification cycle
3. Commit and checkpoint after each successful verification

### C. Complexity Issues
**Cause**: Task requires deep state or multi-step async coordination.

**Split approach**:
1. Defer complex parts with TODO comments
2. Focus on "works but incomplete" over "complete but broken"
3. Return to user with analysis of remaining work

## Step 3: Report to User

When splitting is needed, report:

```markdown
## Task Split Required

**Original task**: [description]
**Issue type**: Over-reading / Chunky execution / Complexity

**Proposed split**:
1. [Subtask A] - [scope]
2. [Subtask B] - [scope]
3. [Remaining/deferred] - [reason]

**Proceed with subtask 1?**
```

## Example Splits

### File Extraction (2000+ lines)
```
Original: Extract all debug components from HomeScreen.kt
Split:
  1. Extract DebugHud (lines 1153-1260) → verify
  2. Extract TingwuTraceSection (lines 1304-1390) → verify
  3. Extract XfyunTraceSection (lines 1392-1501) → verify
```

### Test Coverage (10+ test cases)
```
Original: Add all unit tests for ComponentX
Split:
  1. Add happy-path tests (4 cases) → run
  2. Add edge-case tests (3 cases) → run
  3. Add error-handling tests (3 cases) → run
```
