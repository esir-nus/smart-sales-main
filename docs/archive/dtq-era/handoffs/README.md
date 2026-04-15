# Handoff Registry Contract

> **Purpose**: Standardize paused or transferred lane handoffs for the dirty-tree quarantine workflow and future parallel-agent delivery.
> **Primary Tracker**: `docs/plans/dirty-tree-quarantine.md`
> **Last Updated**: 2026-04-11

---

## Core Rule

If a lane is paused, blocked, transferred, or waiting for integration review, it must have a current handoff file in `handoffs/`.

One handoff file belongs to one lane.

Do not use a handoff as a generic scratchpad for multiple unrelated features.

---

## Required Sections

Each active handoff should include:

1. **Lane ID**
2. **Registry Lane ID**
3. **Evidence Class**
4. **Branch**
5. **Recommended Worktree**
6. **Scope**
7. **Owned Paths**
8. **Current Repo State / Implementation Truth**
9. **What Is Finished**
10. **What Is Still Open**
11. **Doc-Code Alignment**
12. **Required Evidence / Verification**
13. **Safe Next Actions**
14. **Do Not Touch / Collision Notes**

Suggested title pattern:

- `# Handoff: <Lane ID> <Short Theme>`

---

## Relationship To Trackers

- `docs/plans/tracker.md` stays the campaign index
- `docs/plans/dirty-tree-quarantine.md` owns human lane state
- `ops/lane-registry.json` owns machine lane state
- `handoffs/*.md` own paused/transferred lane context

Rule:

- if the tracker row says a lane is paused, blocked, or in review, the row should link the current handoff
- if the registry says a lane is `paused`, its `handoff_path` must point to the same current handoff
- if no handoff exists yet, the lane should not pretend to be safely transferable
- if a handoff omits doc-code alignment state, the lane should not pretend to be safely resumable

## Doc-Code Alignment Section

Every future handoff should state:

- owning source-of-truth docs
- whether the lane is `Aligned`, `Doc update required`, `Code update required`, `Both pending`, or `Deferred`
- which exact docs still need sync if the lane is not aligned

Rule:

- handoff readers must be able to tell whether code is ahead, docs are ahead, or the lane is actually aligned
- do not force the next agent to rediscover the drift state from scratch

---

## Existing Files

The current live handoffs were backfilled to this contract on 2026-04-04:

- `handoffs/audio_drawer_compose_handoff.md`
- `handoffs/notification_compose_handoff.md`
- `handoffs/oem_permission_diagnosis.md`
- `handoffs/schedule_quick_start_compose_handoff.md`

Future handoffs should follow the same section contract from the start instead of relying on later cleanup.
