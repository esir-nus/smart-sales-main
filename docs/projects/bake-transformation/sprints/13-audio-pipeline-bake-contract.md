# Sprint Contract: 13-audio-pipeline-bake-contract

## 1. Header

- **Slug**: audio-pipeline-bake-contract
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Codex-authored during Sprint 12 TCF close
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Author the follow-on `13-audio-pipeline-bake-contract` sprint
after stable audio target-flow alignment.

**Codex interpretation**: The operator writes the durable BAKE implementation
contract for the audio pipeline, syncs interface-map authority, and demotes the
scoped audio Cerb docs to supporting reference. Sprint 13 is not a runtime code
change sprint unless the contract cannot be verified from current evidence.

## 3. Scope

Docs-only unless a stop criterion forces user arbitration. Explicit write scope:

- `docs/bake-contracts/audio-pipeline.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md`

Allowed supporting read scope includes current audio core-flow, Sprint 11 DBM,
Sprint 12 TCF notes, audio interface docs, and audio/Tingwu Kotlin source/tests.

Out of scope:

- Kotlin/API/schema/runtime behavior changes
- moving Cerb docs into archive unless the user explicitly expands the sprint
  from supporting-reference demotion to physical archive movement
- CHANGELOG line unless the user explicitly approves one at close
- Harmony-native overlay changes
- general blank SIM chat behavior outside the audio-bound pipeline

## 4. References

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md`
- `docs/projects/bake-transformation/sprints/12-audio-pipeline-tcf.md`
- `docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/audio-management/interface.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/cerb/tingwu-pipeline/interface.md`
- `docs/cerb/badge-audio-pipeline/spec.md`
- `docs/cerb/badge-audio-pipeline/interface.md`
- `docs/cerb/pipeline-telemetry/spec.md`
- `docs/cerb/interface-map.md`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryTranscriptionSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryArtifactSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioPipelineIngestSupport.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/RealBadgeAudioPipeline.kt`
- `app-core/src/main/java/com/smartsales/prism/data/tingwu/RealTingwuPipeline.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`

## 5. Success Exit Criteria

1. `docs/bake-contracts/audio-pipeline.md` exists and follows the BAKE contract
   format from `docs/specs/bake-protocol.md`, including frontmatter,
   Pipeline Contract, Telemetry Joints, UI Docking Surface, Core-Flow Gap,
   Test Contract, and Cross-Platform Notes. Verify:
   `rg -n "protocol: BAKE|domain: audio-pipeline|## Pipeline Contract|## Telemetry Joints|## UI Docking Surface|## Core-Flow Gap|## Test Contract|## Cross-Platform Notes" docs/bake-contracts/audio-pipeline.md`

2. The BAKE contract explicitly covers manual sync, `rec#`, `log#`, Tingwu,
   drawer artifacts, `Ask AI`, audio reselect, pending durable history,
   empty-file filtering, summary fallback, canonical valves, and runtime/provider
   evidence gaps. Verify:
   `rg -n "manual sync|rec#|log#|Tingwu|drawer|Ask AI|audio reselect|pending|durable|empty-file|summary fallback|canonical valves|runtime evidence|provider" docs/bake-contracts/audio-pipeline.md`

3. `docs/cerb/interface-map.md` cites `docs/bake-contracts/audio-pipeline.md`
   as the audio-pipeline implementation authority and keeps scoped Cerb docs as
   supporting reference. Verify:
   `rg -n "audio-pipeline|docs/bake-contracts/audio-pipeline.md|supporting reference|docs/cerb/audio-management/spec.md|docs/cerb/tingwu-pipeline/spec.md" docs/cerb/interface-map.md`

4. Scoped audio Cerb docs are demoted in prose to supporting reference for the
   verified BAKE contract without deleting their historical/spec context.
   Verify:
   `rg -n "supporting reference|BAKE contract|docs/bake-contracts/audio-pipeline.md" docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md`

5. Tracker row marks Sprint 13 `done` on success and `audio-pipeline` status
   `bake-contract-written`. Verify:
   `rg -n "\\| 13 \\| audio-pipeline-bake-contract \\| done|audio-pipeline \\| base-runtime-active .* bake-contract-written" docs/projects/bake-transformation/tracker.md`

6. Sprint 13 contract has all ten required sections and includes closeout
   evidence. Verify:
   `rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md`

7. Static diff is clean. Verify:
   `git diff --check -- docs/bake-contracts/audio-pipeline.md docs/cerb/interface-map.md docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md`

## 6. Stop Exit Criteria

- **Contract authority blocker**: Current docs/code/tests cannot support a
  factual BAKE implementation contract without a product decision.
- **Scope blocker**: Verification requires Kotlin/runtime changes, Cerb archive
  movement, Harmony overlay work, or CHANGELOG edits not approved for Sprint 13.
- **Runtime-evidence blocker**: The operator must not claim physical badge, BLE,
  provider-network, installed UI, or live Tingwu behavior without fresh filtered
  `adb logcat` and relevant command evidence. If those claims are required for
  success, stop and ask for device/provider validation scope.
- **Interface-map blocker**: Existing interface-map ownership conflicts with
  audio-pipeline authority in a way that cannot be resolved by docs-only sync.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. BAKE contract structure `rg` output.
2. BAKE contract audio-branch/gap `rg` output.
3. Interface-map authority `rg` output.
4. Scoped Cerb supporting-reference `rg` output.
5. Tracker row `rg` output.
6. Sprint 13 section schema `rg` output.
7. `git diff --check` over Sprint 13 scope.
8. `git diff --stat` over Sprint 13 scope.

Runtime/L3 validation is not required unless Sprint 13 expands beyond docs-only
authority sync. Any installed-device, BLE, provider-network, or live Tingwu
claim remains unproven unless fresh filtered `adb logcat` and relevant command
evidence are captured during the sprint.

## 9. Iteration Ledger

Operator fills during Sprint 13 execution.

## 10. Closeout

Operator fills during Sprint 13 execution.
