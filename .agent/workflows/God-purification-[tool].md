---
description: Align code with tracker.md, purify god files, sync docs bidirectionally
---

# God Purification Protocol

**Goal**: Keep code and documentation in sync. Shrink god files. Resolve deviations. Maintain clean architecture.

**Philosophy**: "Reality and spec must converge. Fix whichever is wrong."

---

## When to Invoke

Use this workflow when:
- Starting work on a module that may have drifted from `tracker.md`
- Suspecting architecture debt in a file > 500 lines
- Proactively auditing a domain for tech debt cleanup
- **Any time** you want to ensure code matches target architecture

---

## The Protocol

### Phase 1: 🔍 Audit (Gather Evidence)

1. **Read the relevant tracker section**: Check `docs/plans/tracker.md` for target structure
2. **Audit reality**: Use `find`, `wc -l`, `grep` to see actual files
3. **Identify gaps**:
   - Files in tracker but missing in code
   - Files in code but missing from tracker
   - Files > 500 lines (god file candidates)
   - Naming/location mismatches

**Output**: Gap table with evidence

```markdown
| Tracker Says | Reality | Gap Type |
|--------------|---------|----------|
| `Foo.kt` | Missing | Code gap |
| - | `Bar.kt` (exists) | Tracker gap |
| `X/` (folder) | `Y/` (folder) | Location mismatch |
| `File.kt` | 800 lines | God file |
```

---

### Phase 2: 🤔 Decide (Which Way to Fix)

For each gap, ask:

```
┌─────────────────────────────────────────┐
│ Is tracker's design BETTER than reality?│
└───────────────┬─────────────────────────┘
                │
        ┌───────┴───────┐
        │               │
       YES              NO
        │               │
        ▼               ▼
  FIX THE CODE    FIX THE TRACKER
  (move/rename/   (update tracker
   rewrite)        to match reality)
```

**Heuristics**:
- Tracker follows V1 spec → Tracker wins
- Reality is tested and working, tracker is aspirational → Reality wins
- Both valid → Prefer simpler/cleaner structure

---

### Phase 3: 🏗️ Execute (Fix It)

**If fixing code**:
1. Move/rename files to target locations
2. Update imports at all call sites
3. Run build + tests
4. Commit: `refactor(module): align X with tracker.md`

**If fixing tracker**:
1. Update `tracker.md` to reflect reality
2. Add any missing files to the tree
3. Commit: `docs: sync tracker.md with module X reality`

**If purifying god file**:
1. Identify extractable concern
2. Create new file in correct location
3. Extract logic, add tests
4. Inject + delegate from god file
5. Delete old code from god file
6. Commit: `refactor(module): extract Y from god file X`

---

### Phase 4: ✅ Verify & Log

1. Run `./gradlew :module:testDebugUnitTest`
2. Verify build passes
3. Update `tracker.md` if code changed
4. Commit all changes

---

## Commit Message Conventions

| Change Type | Format |
|-------------|--------|
| Code → Tracker | `docs: sync tracker.md with {module} structure` |
| Tracker → Code | `refactor({module}): align with tracker.md` |
| God file extract | `refactor({module}): extract {component} from {godfile}` |
| Combined | `refactor({module}): purify {godfile}, sync docs` |

---

## Quick Checklist

- [ ] Audited tracker vs reality
- [ ] Identified all gaps
- [ ] Decided fix direction for each gap
- [ ] Executed fixes (code or docs)
- [ ] Tests pass
- [ ] Committed with proper message
- [ ] Updated tracker if code changed

---

## Anti-Patterns to Avoid

- ❌ Updating tracker without checking if code matches spec
- ❌ Spending 2 hours auditing when fix is 10 minutes
- ❌ Making code changes without running tests
- ❌ Big-bang refactor instead of incremental purification
- ❌ Assuming tracker is always right (it's often aspirational)

---

## Required Deliverables

At workflow completion, you MUST produce:

### 1. Gap Analysis Table
```markdown
| Item | Tracker | Reality | Decision | Status |
|------|---------|---------|----------|--------|
| ... | ... | ... | Fix Code / Fix Tracker | ✅/❌ |
```

### 2. Actions Taken
- List of files moved/renamed/created/deleted
- List of tracker.md sections updated
- God file line count before/after

### 3. Verification Evidence
```bash
# Build result
./gradlew :module:assembleDebug  # ✅ SUCCESS

# Test result  
./gradlew :module:testDebugUnitTest  # ✅ X tests passed
```

### 4. Commits Made
```
abc1234 refactor(module): extract X from Y
def5678 docs: sync tracker.md with module structure
```

**Exit Criteria**: Workflow not complete until all 4 deliverables are documented.
