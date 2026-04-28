# Sprint Contract: 01-triage

## 1. Header

- **Slug**: triage
- **Project**: bake-transformation
- **Date authored**: 2026-04-28
- **Author**: Claude
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Establish the bake-transformation project by triaging all domains
in the codebase — classifying each by BAKE runtime category, assessing
complexity, and producing the ordered sprint backlog. Author the delivered-
behavior map (DBM) and target core-flow (TCF) sprint contracts for the first
domain cluster.

**Claude's interpretation**: This sprint is entirely docs-only. No code is
touched. The operator reads the codebase and existing docs to classify every
domain against the BAKE protocol's runtime classification (base-runtime-active,
partially-shipped, mono-deferred), estimates complexity (to decide whether DBM
and TCF can be collapsed for L1 leaf modules), and fills the tracker's domain
backlog table. The sprint closes by authoring two ready-to-execute sprint
contracts for cluster 1 (connectivity-bridge + badge-session, the highest-
priority tier-1 domains), and by confirming and registering the
`docs/bake-contracts/` canonical location.

**Prerequisite check at sprint start**: Confirm `docs/specs/bake-protocol.md`
is committed (`git log --oneline docs/specs/bake-protocol.md | head -1`).
If not committed, surface as a blocker before proceeding.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md` — fill domain backlog table,
  add any decisions from triage
- `docs/projects/bake-transformation/sprints/01-triage.md` — this contract
  (iteration ledger + closeout)
- `docs/projects/bake-transformation/sprints/02-connectivity-dbm.md` — author
  (not start; awaits user review gate after sprint 02 closes)
- `docs/projects/bake-transformation/sprints/03-connectivity-tcf.md` — author
  (not start; awaits user review gate after sprint 02 closes)

No files outside this list may be written. No code, test, or build files.

## 4. References

- `docs/specs/bake-protocol.md` — northstar doctrine; classification rules
- `docs/specs/sprint-contract.md` — schema for sprint 02 and 03 contracts
- `docs/specs/project-structure.md` — tracker shape rules (lean, under 150 lines)
- `docs/cerb/interface-map.md` — domain inventory source
- `docs/core-flow/` — all 13 docs; existing north stars for each domain
- `docs/specs/base-runtime-unification.md` — BAKE/MONO feature boundary
- `docs/projects/badge-session-lifecycle/tracker.md` — sprint 08 context
- `docs/projects/bake-transformation/tracker.md` — domain backlog to fill

## 5. Success Exit Criteria

1. Domain backlog table in `tracker.md` is complete: every domain listed has
   `runtime`, `existing-doc` (path or `none`), `estimated-complexity`
   (low/medium/high), and `priority-tier` columns confirmed.
   Verify: `grep -c "|" docs/projects/bake-transformation/tracker.md`

2. Sprint 02 contract (`02-connectivity-dbm.md`) exists and passes the
   10-section harness schema check (sections 1-8 filled, 9-10 blank).
   Verify: `wc -l docs/projects/bake-transformation/sprints/02-connectivity-dbm.md`

3. Sprint 03 contract (`03-connectivity-tcf.md`) exists and passes the
   10-section harness schema check (sections 1-8 filled, 9-10 blank).
   Verify: `wc -l docs/projects/bake-transformation/sprints/03-connectivity-tcf.md`

4. `docs/bake-contracts/` location confirmed and registered: a Decisions entry
   in `tracker.md` states the canonical path, OR a note has been added to
   `docs/specs/bake-protocol.md` §5 or §6. One of these must be true.
   Verify: `grep -n "bake-contracts" docs/projects/bake-transformation/tracker.md`
   or `grep -n "bake-contracts" docs/specs/bake-protocol.md`

5. `docs/specs/bake-protocol.md` is in git history (prerequisite).
   Verify: `git log --oneline docs/specs/bake-protocol.md | head -1`

## 6. Stop Exit Criteria

- **Prerequisite blocker**: `bake-protocol.md` is not committed. Surface
  immediately; do not proceed with triage until resolved.
- **Classification blocker**: More than 3 domains cannot be classified due to
  insufficient source context (missing docs, no core-flow, no cerb spec, and
  code is too complex to assess in read-only mode). Record blocked domains
  explicitly; partial triage is a valid stop.
- **Scope creep**: Any pull toward editing code, modifying non-listed docs, or
  writing BAKE contracts prematurely. Stop and surface.

## 7. Iteration Bound

2 iterations. No wall-clock limit (docs-only sprint).

## 8. Required Evidence Format

Closeout must include:

1. `wc -l docs/projects/bake-transformation/tracker.md` — confirms tracker is
   under 150 lines
2. `ls -1 docs/projects/bake-transformation/sprints/` — confirms sprint 02 and
   03 contracts are on file
3. `git log --oneline docs/specs/bake-protocol.md | head -1` — confirms
   prerequisite committed
4. Source inventory summary: for the connectivity-bridge and badge-session
   domains (cluster 1), list the key files inspected and rg/grep commands run
   to confirm the domain boundary. This serves as the starting evidence trail
   for sprint 02.
5. One-line classification rationale for any domain that was reclassified from
   the triage seed (tracker domain backlog).

## 9. Iteration Ledger

- **Iteration 1**: Confirmed the prerequisite commit for
  `docs/specs/bake-protocol.md`, read the sprint contract, project tracker,
  sprint-contract schema, project-structure rules, BAKE protocol, interface map,
  base-runtime boundary, lessons index, all core-flow docs, and badge-session
  sprint 08 tracker context. Found the handoff is valid, with the operational
  correction that `docs/bake-contracts/` should be registered in the project
  tracker rather than the northstar doc because sprint 01 write scope excludes
  `docs/specs/bake-protocol.md`.
- **Iteration 2**: Confirmed the domain inventory from `docs/core-flow/**`,
  `docs/cerb/interface-map.md`, Cerb specs, and targeted code searches for
  `ConnectivityBridge`, `DeviceConnectionManager`, `DeviceRegistryManager`,
  `SimAudioRepository`, `SimBadgeAudioAutoDownloader`, and
  `RealBadgeAudioPipeline`. Updated the tracker backlog, authored sprint 02 and
  sprint 03 contracts, and prepared closeout evidence.

## 10. Closeout

- **Status**: success
- **Summary** (one line for tracker): Classified BAKE runtime domains, confirmed
  `docs/bake-contracts/`, and authored cluster-1 DBM/TCF contracts.
- **Evidence**:
  - `wc -l docs/projects/bake-transformation/tracker.md`:
    `74 docs/projects/bake-transformation/tracker.md`
  - `ls -1 docs/projects/bake-transformation/sprints/`:
    `01-triage.md`, `02-connectivity-dbm.md`,
    `03-connectivity-tcf.md`
  - `git log --oneline docs/specs/bake-protocol.md | head -1`:
    `24a087c03 docs(governance): establish BAKE protocol and transformation project`
  - Tracker completion checks:
    `grep -c "|" docs/projects/bake-transformation/tracker.md` -> `29`;
    `wc -l docs/projects/bake-transformation/sprints/02-connectivity-dbm.md docs/projects/bake-transformation/sprints/03-connectivity-tcf.md`
    -> `96`, `90`.
  - Cluster-1 source inventory commands:
    `rg -n "interface ConnectivityBridge|class RealConnectivityBridge|class DefaultDeviceConnectionManager|class RealDeviceRegistryManager|interface DeviceRegistryManager|class SimAudioRepository|class SimBadgeAudioAutoDownloader|class RealBadgeAudioPipeline|interface BadgeAudioPipeline" data/connectivity app-core/src/main/java app-core/src/test/java`
    and
    `rg -n "connectivity-bridge|badge-session|badge-connectivity|DeviceRegistryManager|ConnectivityBridge|SimAudioRepository" docs/core-flow docs/cerb/interface-map.md docs/projects/badge-session-lifecycle/tracker.md`.
  - Cluster-1 key files inspected:
    `data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt`,
    `app-core/src/main/java/com/smartsales/prism/data/connectivity/RealConnectivityBridge.kt`,
    `app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManager.kt`,
    `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/DeviceRegistryManager.kt`,
    `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt`,
    `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`,
    `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`,
    `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`,
    `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt`,
    `docs/core-flow/badge-connectivity-lifecycle.md`,
    `docs/core-flow/badge-session-lifecycle.md`,
    `docs/cerb/connectivity-bridge/spec.md`,
    `docs/cerb/connectivity-bridge/interface.md`,
    `docs/cerb/interface-map.md`.
  - Reclassification rationale: no domain runtime category changed from the
    triage seed; sprint 01 only confirmed existing-doc and complexity columns.
- **Lesson proposals** (optional): none
- **CHANGELOG line** (optional): none
