# Sprint Contract: 11-audio-pipeline-dbm

## 1. Header

- **Slug**: audio-pipeline-dbm
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Codex-authored from previous-agent Sprint 11 plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Create and execute Sprint 11 as a docs-only BAKE
delivered-behavior-map sprint for the audio-bound pipeline.

**Codex interpretation**: This sprint maps delivered behavior for badge/manual
audio ingress, SIM audio repository, Tingwu artifact processing, drawer UI,
`Ask AI`, audio reselect, pending audio selection, and audio-bound chat state.
It excludes general blank SIM chat, implementation changes, target core-flow
edits, Cerb demotion, BAKE implementation contracts, and changelog work.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md`
- `docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/`

No files outside this list may be written.

Out of scope:

- code edits
- target core-flow updates
- Cerb demotion, archive moves, or authority changes
- BAKE implementation contract writing
- CHANGELOG line
- Harmony-native or platform overlay changes
- general blank SIM chat mapping for `agent-chat-pipeline`
- runtime, device, BLE, provider-network, live Tingwu, adb, or L3 claims
  without explicit evidence captured during this sprint

## 4. References

Governance and project protocol:

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`

Audio north-star and active supporting docs:

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/audio-management/interface.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/cerb/tingwu-pipeline/interface.md`
- `docs/cerb/badge-audio-pipeline/spec.md`
- `docs/cerb/badge-audio-pipeline/interface.md`
- `docs/cerb/pipeline-telemetry/spec.md`
- `docs/cerb/interface-map.md`

Code and test inventory:

- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryStoreSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryTranscriptionSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryArtifactSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioPipelineIngestSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt`
- `app-core/src/main/java/com/smartsales/prism/data/tingwu/RealTingwuPipeline.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerCard.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`
- `app-core/src/test/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupportTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloaderTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/audio/RealBadgeAudioPipelineIngressTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModelTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/tingwu/RealTingwuPipelineTest.kt`

## 5. Success Exit Criteria

1. Delivered-behavior map exists at
   `docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`.
   Verify:
   `test -f docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`

2. The DBM covers the required audio-bound branches and terms: manual sync,
   `rec#`, `log#`, Tingwu, artifact, `Ask AI`, audio reselect, pending,
   local import, tombstone, and runtime evidence.
   Verify:
   `rg -n "manual sync|rec#|log#|Tingwu|artifact|Ask AI|audio reselect|pending|local import|tombstone|runtime evidence" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`

3. The DBM covers list-first placeholders, delete/tombstone behavior,
   empty-file filtering, targeted resume, drawer browse/select behavior,
   transcribe blocking, audio-bound chat binding, durable artifact chat history,
   telemetry/canonical valves, and missing runtime/provider proof.
   Verify:
   `rg -n "placeholder|delete|empty|targeted resume|Browse|Select|Transcribe Blocking|Audio-Bound Chat|durable|Telemetry|canonical|provider" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`

4. The DBM cites concrete source docs plus Kotlin source/test files. It must
   cite at least 18 distinct repository paths, including at least 5 docs and at
   least 8 Kotlin source or test paths.
   Verify:
   `rg -o '`(docs|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md | sort -u | wc -l`

5. The DBM distinguishes delivered behavior, target behavior, gaps, historical
   references, and unknowns where docs, code, tests, and evidence class differ.
   Verify:
   `rg -n "Delivered behavior|Target behavior|Gap|Historical reference|Unknown" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`

6. The sprint contract has all ten required sections and includes a closeout
   with static evidence.
   Verify:
   `rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md`

7. Tracker row for Sprint 11 is updated to `done`, `stopped`, or `blocked`
   with a factual one-line summary, and `audio-pipeline` is updated only if the
   sprint actually closes successfully.
   Verify:
   `rg -n "\| 11 \| audio-pipeline-dbm \| (done|stopped|blocked)|audio-pipeline \| base-runtime-active .* (dbm-written|triaged)" docs/projects/bake-transformation/tracker.md`

8. No runtime/L3 claims are made without fresh device/provider evidence. Missing
   installed-device, BLE, provider-network, or live Tingwu proof is recorded as
   Gap or Unknown.
   Verify:
   `rg -n "adb logcat|runtime evidence|provider|live Tingwu|Gap|Unknown" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md`

## 6. Stop Exit Criteria

- **Scope blocker**: The delivered behavior cannot be mapped without writing
  outside the sprint scope.
- **Source ambiguity blocker**: Current core-flow, Cerb/reference docs, code,
  and tests conflict so strongly that the operator cannot write a factual DBM
  without a product decision.
- **Runtime-evidence blocker**: Installed-device, BLE, provider-network, live
  Tingwu, UI, lifecycle, networking, or device behavior would be required to
  claim success. Record missing proof as a gap; do not claim it.
- **Authority creep**: Any pull toward target core-flow edits, Cerb demotion,
  BAKE contract writing, CHANGELOG edits, platform overlay changes, or code
  changes.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `test -f docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`
2. `rg -n "manual sync|rec#|log#|Tingwu|artifact|Ask AI|audio reselect|pending|local import|tombstone|runtime evidence" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`
3. `rg -n "placeholder|delete|empty|targeted resume|Browse|Select|Transcribe Blocking|Audio-Bound Chat|durable|Telemetry|canonical|provider" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`
4. `rg -n "Delivered behavior|Target behavior|Gap|Historical reference|Unknown|adb logcat" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`
5. `rg -o '`(docs|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md | sort -u | wc -l`
6. `rg -n "\| 11 \| audio-pipeline-dbm \| (done|stopped|blocked)|audio-pipeline \| base-runtime-active .* (dbm-written|triaged)" docs/projects/bake-transformation/tracker.md`
7. `git diff --check -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/`
8. `git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/`

Do not run runtime/L3 validation for this sprint. Any installed-device, BLE,
provider-network, or live Tingwu claim must be recorded as unproven unless fresh
filtered `adb logcat` and relevant command evidence are captured.

## 9. Iteration Ledger

- **Iteration 1 — 2026-04-29**
  - Read the sprint plan, AGENTS repo contract, BAKE tracker, top-level tracker,
    sprint-contract schema, project-structure spec, audio core flow, active
    audio-management and Tingwu specs, interface map, and current audio/chat
    Kotlin source and test inventory.
  - Wrote
    `docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`
    with delivered-vs-target/gap/historical/unknown treatment for manual sync,
    `rec#`, `log#`, Tingwu, drawer UI, `Ask AI`, audio reselect, pending
    selection, durable audio-bound chat state, telemetry, and runtime/provider
    evidence gaps.
  - Updated the Sprint 11 tracker row to `done` and the `audio-pipeline` domain
    status to `dbm-written`.
  - Evaluator result: static acceptance checks passed. No runtime/L3 claims,
    BLE claims, provider-network claims, or live Tingwu claims were made without
    fresh `adb logcat` or command evidence.

## 10. Closeout

**Status**: success

**Tracker summary**: Delivered behavior map created for the audio pipeline,
covering badge/manual ingress, SIM repository, Tingwu artifacts, drawer UI,
Ask AI, audio reselect, audio-bound chat state, telemetry, and evidence gaps.

**Changed files**:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md`
- `docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`

**Evidence command outputs**:

```text
$ test -f docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md && echo exists
exists
```

```text
$ rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md
3:## 1. Header
14:## 2. Demand
25:## 3. Scope
46:## 4. References
94:## 5. Success Exit Criteria
144:## 6. Stop Exit Criteria
159:## 7. Iteration Bound
163:## 8. Required Evidence Format
179:## 9. Iteration Ledger
196:## 10. Closeout
```

```text
$ rg -n "manual sync|rec#|log#|Tingwu|artifact|Ask AI|audio reselect|pending|local import|tombstone|runtime evidence" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md
5:Evidence class: static DBM from docs, Kotlin source, and JVM tests. No fresh
83:manual badge sync, `rec#` audio auto-download, `log#` badge pipeline ingest into
86:`Ask AI`, audio reselect, pending audio selection, and audio-bound chat state.
139:Target behavior: the core flow requires placeholders to remain visible and
160:## `rec#` Auto-Download
174:## `log#` Badge Pipeline Ingest Boundary
202:## Tingwu Submit, Observe, Persist, Reuse
242:## Transcribe Blocking And Local Import
262:## `Ask AI`
284:## Audio Reselect And Pending Selection
316:`sim_session_metadata.json` and messages in `sim_session_<id>_messages.json`.
333:Delivered behavior: static telemetry for audio-bound pipeline is partial.
```

```text
$ rg -n "placeholder|delete|empty|targeted resume|Browse|Select|Transcribe Blocking|Audio-Bound Chat|durable|Telemetry|canonical|provider" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md
113:`QUEUED` placeholders, then processes one background download queue.
126:## Placeholders, Empty Files, Delete, And Tombstone
148:## Targeted Resume And Device Changes
235:## Drawer Browse And Artifact Surface
242:## Transcribe Blocking And Local Import
284:## Audio Reselect And Pending Selection
307:## Audio-Bound Chat State And Durable History
333:## Telemetry And Canonical Valves
```

```text
$ rg -n "Delivered behavior|Target behavior|Gap|Historical reference|Unknown|adb logcat" docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md
6:`adb logcat`, installed-device run, BLE trace, provider-network run, or live
83:Delivered behavior: this map covers audio-bound pipeline behavior only:
93:Target behavior: the core flow expects a single SIM-owned transcription/artifact
105:Delivered behavior: drawer sync is UI/manual through
116:Target behavior: `docs/core-flow/sim-audio-artifact-chat-flow.md` and
120:Gap: runtime evidence for physical badge `/list`, HTTP download timing,
124:Unknown: whether current installed-device BLE/Wi-Fi state transitions always
128:Delivered behavior: manual sync and `rec#` create SmartBadge placeholders with
139:Target behavior: the core flow requires placeholders to remain visible and
143:Gap: the manual sync `SimBadgeSyncOutcome` type has `skippedEmptyCount`, but the
148:Unknown: remote badge delete behavior and tombstone clearing require live badge
```

```text
$ rg -o '`(docs|app-core)/[^`]+`' docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md | sort -u | wc -l
31
```

```text
$ rg -n "\| 11 \| audio-pipeline-dbm \| (done|stopped|blocked)|audio-pipeline \| base-runtime-active .* (dbm-written|triaged)" docs/projects/bake-transformation/tracker.md
35:| 11 | audio-pipeline-dbm | done | Delivered behavior map created for the audio pipeline, covering badge/manual ingress, SIM repository, Tingwu artifacts, drawer UI, Ask AI, audio reselect, audio-bound chat state, telemetry, and evidence gaps. | [sprints/11-audio-pipeline-dbm.md](sprints/11-audio-pipeline-dbm.md) |
44:| audio-pipeline | base-runtime-active | `docs/core-flow/sim-audio-artifact-chat-flow.md` | high | tier-2 | dbm-written |
```

```text
$ git diff --cached --check -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/
```

```text
$ git diff --cached --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/
 .../delivered-behavior-map.md                      | 414 +++++++++++++++++++++
 .../sprints/11-audio-pipeline-dbm.md               | 303 +++++++++++++++
 docs/projects/bake-transformation/tracker.md       |   3 +-
 3 files changed, 719 insertions(+), 1 deletion(-)
```

**Lesson proposals**: none.

**CHANGELOG line**: none.
