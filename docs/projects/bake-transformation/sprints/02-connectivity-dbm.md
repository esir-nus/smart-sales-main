# Sprint Contract: 02-connectivity-dbm

## 1. Header

- **Slug**: connectivity-dbm
- **Project**: bake-transformation
- **Date authored**: 2026-04-28
- **Author**: Claude-authored contract lineage; Codex authored from sprint 01 triage close
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Produce the delivered-behavior map for the first BAKE cluster:
connectivity-bridge plus badge-session.

**Claude/Codex interpretation**: This sprint is a read-heavy docs sprint. The
operator records what current committed docs and code actually deliver for
badge connectivity, active-device lifecycle, registered-badge repair, and badge
audio sync fencing. It does not optimize behavior, update target core-flow, or
write the final BAKE contract.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/02-connectivity-dbm.md`
- `docs/projects/bake-transformation/evidence/02-connectivity-dbm/`

No files outside this list may be written.

## 4. References

- `docs/specs/bake-protocol.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/cerb/interface-map.md`
- `docs/projects/badge-session-lifecycle/tracker.md`
- Connectivity code inventory:
  - `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/`
  - `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/`
  - `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository*.kt`
  - `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`

## 5. Success Exit Criteria

1. Delivered-behavior map exists at
   `docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`.
   Verify: `test -f docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`

2. The map covers at least these sections: current inputs, current outputs,
   state transitions, active-device ownership, HTTP media readiness, Wi-Fi
   repair, audio sync/download fencing, telemetry/log evidence, and known gaps.
   Verify: `rg -n "## (Current Inputs|Current Outputs|State Transitions|Active-Device Ownership|HTTP Media Readiness|Wi-Fi Repair|Audio Sync|Telemetry|Known Gaps)" docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`

3. The map cites at least 8 concrete source files or docs.
   Verify: `rg -c "\`(docs|app-core|data)/" docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`

4. The tracker row for sprint 02 is updated to `done` or `blocked` with a
   one-line factual summary.
   Verify: `rg -n "\| 02 \| connectivity-dbm \| (done|blocked)" docs/projects/bake-transformation/tracker.md`

## 6. Stop Exit Criteria

- **Scope blocker**: Any required source lies outside the listed read references
  and cannot be classified without widening write scope.
- **Classification blocker**: The delivered code contradicts both core-flow docs
  so strongly that the operator cannot write a factual map without first asking
  for a product decision.
- **Hardware-evidence blocker**: Runtime claims require fresh adb/logcat proof.
  Record the missing proof as a gap; do not fake L3 evidence.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `wc -l docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`
2. `rg -n "Known Gaps|HTTP Media Readiness|Active-Device Ownership" docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`
3. `git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/02-connectivity-dbm.md docs/projects/bake-transformation/evidence/02-connectivity-dbm/`

## 9. Iteration Ledger

- Iteration 1 — Read the sprint contract, BAKE tracker, sprint schema,
  connectivity/session core-flow docs, connectivity Cerb spec/interface,
  interface map, and current connectivity/audio code. Produced the delivered
  behavior map at
  `docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`.
  Initial section/file/line checks passed after correcting shell quoting for
  the literal backtick citation regex. Next action: update tracker and closeout.

## 10. Closeout

- **Status**: success
- **Tracker summary**: Delivered behavior map created for connectivity-bridge
  plus badge-session, with HTTP readiness, active-device fencing, Wi-Fi repair,
  telemetry, and hardware-evidence gaps recorded.
- **Files changed**:
  - `docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`
  - `docs/projects/bake-transformation/sprints/02-connectivity-dbm.md`
  - `docs/projects/bake-transformation/tracker.md`
- **Evidence**:

```text
$ test -f docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md && echo exists
exists

$ rg -n "## (Current Inputs|Current Outputs|State Transitions|Active-Device Ownership|HTTP Media Readiness|Wi-Fi Repair|Audio Sync|Telemetry|Known Gaps)" docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md
9:## Current Inputs
19:## Current Outputs
30:## State Transitions
42:## Active-Device Ownership
54:## HTTP Media Readiness
65:## Wi-Fi Repair
76:## Audio Sync
87:## Telemetry
95:## Known Gaps

$ rg -c '`(docs|app-core|data)/' docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md
11

$ wc -l docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md
104 docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md

$ rg -n "Known Gaps|HTTP Media Readiness|Active-Device Ownership" docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md
42:## Active-Device Ownership
54:## HTTP Media Readiness
95:## Known Gaps

$ rg -n "\| 02 \| connectivity-dbm \| (done|blocked)" docs/projects/bake-transformation/tracker.md
26:| 02 | connectivity-dbm | done | Delivered behavior map created for connectivity-bridge plus badge-session, with HTTP readiness, active-device fencing, Wi-Fi repair, telemetry, and hardware-evidence gaps recorded. | [sprints/02-connectivity-dbm.md](sprints/02-connectivity-dbm.md) |

$ git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/02-connectivity-dbm.md docs/projects/bake-transformation/evidence/02-connectivity-dbm/
 .../sprints/02-connectivity-dbm.md                 | 59 ++++++++++++++++++++++
 docs/projects/bake-transformation/tracker.md       |  2 +-
 2 files changed, 60 insertions(+), 1 deletion(-)
```

- **Hardware evidence**: not claimed. Fresh `adb logcat` evidence remains a
  known gap for runtime reconnect, Wi-Fi repair, BLE notifications, isolation,
  and HTTP media readiness.
- **Lesson proposals**: none.
- **CHANGELOG line**: none; docs-only sprint evidence for an internal BAKE
  transformation project.
