---
description: Sync documentation after completing work - discovers relevant docs from context and proposes targeted updates
---

# DocSync Workflow

Synchronize documentation after completing development work. This workflow is **context-driven** - it discovers what needs updating based on the work done.

---

## Step 1: Understand What Changed

Review the recent work to understand context:

1. Check recent commits: `git log -n 5 --oneline`
2. Review conversation history for completed tasks
3. Identify: new files, modified files, deleted code, new tests

**Output:** A mental model of what was accomplished.

---

## Step 2: Discover Relevant Docs

Search the workspace for documentation that tracks the changed components:

```bash
# Find markdown docs
find docs/ -name "*.md" -type f

# Search for references to changed files/concepts
grep -r "<component_name>" docs/
```

**Common doc patterns to look for:**
- `docs/plans/tracker.md` (MUST be updated if feature implementation status changed and is NOT an index)
- `docs/cerb/interface-map.md` (MUST be updated if module ownership, new modules, or new interface methods were added)
- Architecture guides (track file structure, milestones)
- Changelogs (track waves, releases)
- Spec docs (track implementation status)

Read each potentially relevant doc to understand its purpose and current state.

---

## Step 2.5: Cross-Reference Validation (CRITICAL)

Before proposing updates, audit for broken references:

```bash
# Check for deprecated paths
grep -rn "docs/guides/" docs/ .agent/workflows/
grep -rn "RealizeTheArchi" docs/ .agent/workflows/
grep -rn "ux-experience\.md" docs/ .agent/workflows/
grep -rn "modules/" docs/ .agent/workflows/

# Check for any file references that don't exist
# Look for patterns like: [text](path) or file:///path
```

### Known Deprecated References

| Old Path | New Path |
|----------|----------|
| `docs/guides/` | `docs/specs/` or `docs/cerb/` |
| `docs/plans/RealizeTheArchi.md` | `docs/plans/tracker.md` |
| `docs/modules/` | `docs/cerb/` |
| `docs/specs/connectivity-spec.md` | `docs/cerb/connectivity-bridge/` |

### If Broken Refs Found

1. Add them to the sync proposal
2. Fix deprecated refs → new paths
3. Report: "Found N deprecated references, included in sync"

---

## Step 3: Analyze and Propose (USER CONFIRMATION REQUIRED)

Based on context, create a sync proposal:

```markdown
## DocSync Proposal

### Context
<Brief summary of work completed>

### Cross-Reference Audit
- [ ] No deprecated `docs/guides/` refs
- [ ] No `RealizeTheArchi` refs (now `tracker.md`)
- [ ] No `ux-tracker.md` refs (now `ux-experience.md`)
- [ ] All file links point to existing files
- [ ] `docs/plans/tracker.md` reflects current spec wave status accurately
- [ ] `docs/cerb/interface-map.md` includes any newly added interfaces or cross-module edges

### Docs to Update

#### 1. <doc_path>
**Why:** <reason this doc is relevant>
**Changes:**
- <specific change 1>
- <specific change 2>

#### 2. <doc_path>
**Why:** <reason>
**Changes:**
- <change>

### Docs NOT Updated (and why)
- <doc_path>: <reason to skip>
```

**⚠️ STOP HERE**: Present proposal to user. Do NOT proceed until user confirms.

---

## Step 4: Execute Sync (After User Approval)

// turbo-all
Apply the approved changes to each doc. For each doc:

1. Read the current content
2. Make targeted edits (preserve existing formatting)
3. Verify the edit makes sense in context

---

## Step 5: Verify and Commit

// turbo
```bash
git add <modified_docs>
git commit -m "docs: sync after <work_summary>"
```

---

## Principles

1. **Context-driven** - No hardcoded file lists; discover from workspace
2. **Minimal changes** - Only update what's actually affected
3. **User confirmation** - Always get approval before modifying docs
4. **Preserve style** - Match existing doc formatting
5. **Conservative** - When unsure if a doc needs updating, ask user
6. **Reference integrity** - Always validate cross-doc refs before syncing
