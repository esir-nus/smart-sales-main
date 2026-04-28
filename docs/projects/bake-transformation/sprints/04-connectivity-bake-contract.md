# Sprint Contract: 04-connectivity-bake-contract

## 1. Header

- **Slug**: connectivity-bake-contract
- **Project**: bake-transformation
- **Date authored**: 2026-04-28
- **Author**: Claude-authored contract lineage; Codex authored from Sprint 03 target-flow close
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Write the first BAKE implementation contract after the
connectivity delivered-behavior map and target core-flow direction are aligned.

**Codex interpretation**: This sprint writes the verified BAKE contract for the
cluster-1 connectivity corridor: connectivity-bridge plus badge-session. It
must ground the contract in Sprint 02 delivered behavior and Sprint 03 target
flow, record gaps explicitly, and update discovery docs so future platform work
uses the BAKE contract as the implementation record.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md`
- `docs/bake-contracts/connectivity-badge-session.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`

No files outside this list may be written. Cerb archival is out of scope for
this sprint; this sprint may mark Cerb docs as reference-supporting only and
leave physical archive moves to a later governance/archival sprint.

## 4. References

- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/projects/bake-transformation/evidence/02-connectivity-dbm/delivered-behavior-map.md`
- `docs/core-flow/badge-connectivity-lifecycle.md`
- `docs/core-flow/badge-session-lifecycle.md`
- `docs/cerb/connectivity-bridge/spec.md`
- `docs/cerb/connectivity-bridge/interface.md`
- `docs/cerb/interface-map.md`
- `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`

## 5. Success Exit Criteria

1. BAKE contract exists at
   `docs/bake-contracts/connectivity-badge-session.md` and includes the
   protocol frontmatter plus required BAKE sections.
   Verify: `rg -n "protocol: BAKE|## Pipeline Contract|## Telemetry Joints|## UI Docking Surface|## Core-Flow Gap|## Test Contract|## Cross-Platform Notes" docs/bake-contracts/connectivity-badge-session.md`

2. The BAKE contract records the Sprint 02 gaps rather than silently treating
   delivered behavior as complete.
   Verify: `rg -n "WifiProvisionedHttpDelayed|audioRecordingNotifications|wifiRepairEvents|queuedBadgeDownloads|SimBadgeAudioAutoDownloader|hardware evidence" docs/bake-contracts/connectivity-badge-session.md`

3. `docs/cerb/interface-map.md` cites the new BAKE contract for the
   ConnectivityBridge / badge-session corridor.
   Verify: `rg -n "connectivity-badge-session|BAKE" docs/cerb/interface-map.md`

4. `docs/cerb/connectivity-bridge/spec.md` and
   `docs/cerb/connectivity-bridge/interface.md` clearly mark themselves as
   supporting/reference docs beneath the BAKE contract without deleting
   historical content.
   Verify: `rg -n "BAKE|reference" docs/cerb/connectivity-bridge/spec.md docs/cerb/connectivity-bridge/interface.md`

5. Tracker row for sprint 04 is updated to `done` or `blocked` with a factual
   one-line summary.
   Verify: `rg -n "\| 04 \| connectivity-bake-contract \| (done|blocked)" docs/projects/bake-transformation/tracker.md`

## 6. Stop Exit Criteria

- **Missing target-flow input**: Sprint 03 did not close successfully or either
  core-flow doc lacks the Sprint 03 target/delivered/gap alignment notes.
- **Contract ambiguity blocker**: The operator cannot decide whether a branch is
  delivered behavior, target behavior, or a core-flow gap from Sprint 02/03 docs
  without a product decision.
- **Scope creep**: Any pull toward code edits, physical Cerb archival moves,
  or runtime/hardware claims without fresh adb/logcat evidence.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `wc -l docs/bake-contracts/connectivity-badge-session.md`
2. `rg -n "protocol: BAKE|Core-Flow Gap|Test Contract|Cross-Platform Notes" docs/bake-contracts/connectivity-badge-session.md`
3. `rg -n "connectivity-badge-session|BAKE" docs/cerb/interface-map.md docs/cerb/connectivity-bridge/spec.md docs/cerb/connectivity-bridge/interface.md`
4. `git diff --stat -- docs/bake-contracts/connectivity-badge-session.md docs/cerb/interface-map.md docs/cerb/connectivity-bridge/spec.md docs/cerb/connectivity-bridge/interface.md docs/projects/bake-transformation/`

## 9. Iteration Ledger

## 10. Closeout
