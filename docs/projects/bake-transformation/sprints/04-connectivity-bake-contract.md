# Sprint Contract: 04-connectivity-bake-contract

## 1. Header

- **Slug**: connectivity-bake-contract
- **Project**: bake-transformation
- **Date authored**: 2026-04-28
- **Author**: Claude-authored contract lineage; Codex refined from Sprint 03 target-flow close
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
   Verify:
   `test -f docs/bake-contracts/connectivity-badge-session.md`
   `rg -n "^protocol: BAKE$|^version: 1.0$|^domain: connectivity-badge-session$|^runtime: base-runtime-active$|^last-verified:" docs/bake-contracts/connectivity-badge-session.md`
   `rg -n "^## Pipeline Contract$|^## Telemetry Joints$|^## UI Docking Surface$|^## Core-Flow Gap$|^## Test Contract$|^## Cross-Platform Notes$" docs/bake-contracts/connectivity-badge-session.md`

2. The BAKE contract records the Sprint 02 gaps rather than silently treating
   delivered behavior as complete.
   Verify:
   `rg -n "WifiProvisionedHttpDelayed" docs/bake-contracts/connectivity-badge-session.md`
   `rg -n "audioRecordingNotifications\\(\\)|wifiRepairEvents\\(\\)" docs/bake-contracts/connectivity-badge-session.md`
   `rg -n "queuedBadgeDownloads|SimBadgeAudioAutoDownloader|hardware evidence" docs/bake-contracts/connectivity-badge-session.md`

3. `docs/cerb/interface-map.md` cites the new BAKE contract for the
   ConnectivityBridge / badge-session corridor.
   Verify: `rg -n "connectivity-badge-session|BAKE" docs/cerb/interface-map.md`

4. `docs/cerb/connectivity-bridge/spec.md` and
   `docs/cerb/connectivity-bridge/interface.md` clearly mark themselves as
   supporting/reference docs beneath the BAKE contract without deleting
   historical content.
   Verify: `rg -n "BAKE|reference" docs/cerb/connectivity-bridge/spec.md docs/cerb/connectivity-bridge/interface.md`

5. Tracker row for sprint 04 is updated to `done`, `stopped`, or `blocked` with
   a factual one-line summary.
   Verify: `rg -n "\| 04 \| connectivity-bake-contract \| (done|stopped|blocked)" docs/projects/bake-transformation/tracker.md`

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
2. `rg -n "^protocol: BAKE$|^## Core-Flow Gap$|^## Test Contract$|^## Cross-Platform Notes$" docs/bake-contracts/connectivity-badge-session.md`
3. `rg -n "connectivity-badge-session|BAKE" docs/cerb/interface-map.md docs/cerb/connectivity-bridge/spec.md docs/cerb/connectivity-bridge/interface.md`
4. `git diff --stat -- docs/bake-contracts/connectivity-badge-session.md docs/cerb/interface-map.md docs/cerb/connectivity-bridge/spec.md docs/cerb/connectivity-bridge/interface.md docs/projects/bake-transformation/`

## 9. Iteration Ledger

- Iteration 1 — Read the sprint contract, repo rules, BAKE protocol, sprint and
  project structure specs, lessons index/details for architecture and
  cross-platform triggers, Sprint 02 delivered-behavior map, Sprint 03 closeout,
  both core-flow docs, connectivity Cerb spec/interface, interface map, and
  referenced connectivity/audio code paths. Applied the pre-execution review
  findings by tightening BAKE verification checks, allowing stopped tracker
  close states, and correcting the author line. Wrote the first BAKE contract at
  `docs/bake-contracts/connectivity-badge-session.md`, synced the interface map,
  demoted connectivity Cerb docs to supporting/reference status, added the
  missing `audioRecordingNotifications()` and `wifiRepairEvents()` reference
  entries, updated the project tracker, and ran the required evidence commands.

## 10. Closeout

- **Status**: success
- **Tracker summary**: Wrote the connectivity-badge-session BAKE contract,
  synced interface-map authority, and demoted connectivity Cerb docs to
  supporting reference.
- **Files changed**:
  - `docs/bake-contracts/connectivity-badge-session.md`
  - `docs/cerb/interface-map.md`
  - `docs/cerb/connectivity-bridge/spec.md`
  - `docs/cerb/connectivity-bridge/interface.md`
  - `docs/projects/bake-transformation/tracker.md`
  - `docs/projects/bake-transformation/sprints/04-connectivity-bake-contract.md`
- **Evidence**:

```text
$ wc -l docs/bake-contracts/connectivity-badge-session.md
158 docs/bake-contracts/connectivity-badge-session.md

$ rg -n "^protocol: BAKE$|^## Core-Flow Gap$|^## Test Contract$|^## Cross-Platform Notes$" docs/bake-contracts/connectivity-badge-session.md
2:protocol: BAKE
114:## Core-Flow Gap
135:## Test Contract
149:## Cross-Platform Notes

$ rg -n "connectivity-badge-session|BAKE" docs/cerb/interface-map.md docs/cerb/connectivity-bridge/spec.md docs/cerb/connectivity-bridge/interface.md
docs/cerb/connectivity-bridge/interface.md:4:> **Status**: Supporting reference beneath BAKE
docs/cerb/connectivity-bridge/interface.md:6:> **BAKE Implementation Contract**: [`docs/bake-contracts/connectivity-badge-session.md`](../../bake-contracts/connectivity-badge-session.md)
docs/cerb/interface-map.md:7:> **Last Updated**: 2026-04-28 (ConnectivityBridge plus badge-session BAKE contract is now the implementation record; Cerb connectivity bridge docs remain supporting/reference docs beneath it)
docs/cerb/interface-map.md:35:### BAKE implementation contract overlay (2026-04-28)
docs/cerb/interface-map.md:37:- `docs/bake-contracts/connectivity-badge-session.md` is the verified BAKE
docs/cerb/interface-map.md:41:  docs above the BAKE contract.
docs/cerb/interface-map.md:44:  beneath the BAKE contract until a later archival sprint moves historical Cerb
docs/cerb/interface-map.md:56:| **[ConnectivityBridge](./connectivity-bridge/spec.md)** | Hardware & Audio | BLE + HTTP device state, manager-only BLE/Wi‑Fi diagnostic state; BAKE implementation record: [`connectivity-badge-session`](../bake-contracts/connectivity-badge-session.md) | ... |
docs/cerb/connectivity-bridge/spec.md:5:> **Status**: Supporting reference beneath BAKE
docs/cerb/connectivity-bridge/spec.md:7:> **BAKE Implementation Contract**: [`docs/bake-contracts/connectivity-badge-session.md`](../../bake-contracts/connectivity-badge-session.md)
docs/cerb/connectivity-bridge/spec.md:15:Connectivity Bridge provides a **thin, Prism-compatible interface** to legacy `feature:connectivity` module. Preserves ESP32 rate limiting and business logic while exposing clean interfaces for Prism domain code. After Sprint 04 of the BAKE transformation, this Cerb document is a supporting/reference discovery doc; the BAKE contract is the authoritative implementation record for the connectivity-bridge plus badge-session corridor.

$ git diff --stat -- docs/bake-contracts/connectivity-badge-session.md docs/cerb/interface-map.md docs/cerb/connectivity-bridge/spec.md docs/cerb/connectivity-bridge/interface.md docs/projects/bake-transformation/
 docs/bake-contracts/connectivity-badge-session.md  | 158 +++++++++++++++++++++
 docs/cerb/connectivity-bridge/interface.md         |  32 ++++-
 docs/cerb/connectivity-bridge/spec.md              |   7 +-
 docs/cerb/interface-map.md                         |  16 ++-
 .../sprints/04-connectivity-bake-contract.md       |  83 ++++++++++-
 docs/projects/bake-transformation/tracker.md       |   6 +-
 6 files changed, 285 insertions(+), 17 deletions(-)
```

- **Hardware evidence**: not claimed. This sprint was docs-only and records the
  hardware evidence gap for future platform-runtime verification.
- **Lesson proposals**: none.
- **CHANGELOG line**: none; internal BAKE transformation docs only.
