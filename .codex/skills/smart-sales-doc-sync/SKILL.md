---
name: smart-sales-doc-sync
description: Documentation synchronization for the Smart Sales repository. Use when Codex needs to update specs, trackers, interface maps, reports, or references after code or behavior changes, audit doc drift, repair broken references, or adapt the Antigravity doc-sync workflow into Codex behavior.
---

# Smart Sales Doc Sync

Use this skill when repo documentation must be brought back into alignment with current behavior or recent code changes.

## Core Rule

No meaningful code change is complete until the affected docs are synchronized in the same working session.

When a Core Flow doc exists, sync lower layers toward that north star unless the user explicitly decides the Core Flow should change.

## Sync Workflow

### 1. Understand what changed

- List the changed files or changed behavior.
- Separate user-visible behavior, data model changes, interface changes, and ownership changes.
- Ignore unrelated docs rather than broadening the sync unnecessarily.

### 2. Discover the affected docs

Start with repo search:

```bash
rg -n "FeatureName|TypeName|OldPath|NewPath" docs .agent README.md AGENTS.md
rg --files docs | rg "feature|module|tracker|interface|report|spec"
```

Typical targets:

- Owning `docs/core-flow/**`
- Owning `docs/cerb/**/spec.md`
- Owning `docs/cerb/**/interface.md`
- `docs/plans/tracker.md`
- `docs/cerb/interface-map.md`
- Relevant SOPs, reports, or README entries

### 3. Validate references before editing

Check for:

- Deprecated paths
- Broken markdown links
- Stale `file:///` references
- References to deleted modules or renamed types

### 4. Decide whether to propose first or execute directly

- If the user explicitly asked for doc sync, execute directly.
- If the sync is broad, ambiguous, or touches many docs, present a concise proposal first.
- Keep the existing documentation structure intact unless the user explicitly asks for reorganization.

### 5. Update the right layer

Use the narrowest valid update:

- Behavioral north star changed: update `docs/core-flow/**`
- Behavior or state changed: update the owning spec
- Interface signature changed: update `interface.md`
- Status or debt changed: update `docs/plans/tracker.md`
- Module edge changed: update `docs/cerb/interface-map.md`
- Navigation or discovery changed: update README or guidance docs

### 6. Verify the sync

- Re-read the edited docs.
- Search for stale names or paths.
- Make sure linked guidance does not contradict itself.

## Anti-Patterns

- Shipping code and postponing doc updates
- Updating tracker only while leaving the spec stale
- Broad “cleanup” edits unrelated to the actual change
- Reorganizing docs just because the runtime changed
- Leaving broken references behind after a rename

## Source References

Read `references/source-map.md` for the exact Antigravity workflow and supporting rules this skill adapts.
