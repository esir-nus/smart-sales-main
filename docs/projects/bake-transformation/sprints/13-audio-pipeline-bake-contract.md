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

- **Iteration 1 — 2026-04-29**
  - Read the repo tracker, BAKE tracker, Sprint 13 contract, BAKE protocol,
    audio core-flow, Sprint 11 DBM, Sprint 12 closeout, interface map, existing
    BAKE contracts, and scoped audio Cerb specs.
  - Wrote `docs/bake-contracts/audio-pipeline.md` as the verified
    implementation record for manual sync, `rec#`, `log#`, Tingwu, drawer
    artifacts, `Ask AI`, audio reselect, pending durable history, empty-file
    filtering, summary fallback, canonical valves, runtime evidence gaps, and
    provider gaps.
  - Synced `docs/cerb/interface-map.md` so the audio core-flow remains the
    behavioral north star, `docs/bake-contracts/audio-pipeline.md` is the
    implementation authority, and audio-management, Tingwu, badge-audio-pipeline,
    and pipeline-telemetry Cerb docs remain supporting/reference docs beneath it.
  - Demoted `docs/cerb/audio-management/spec.md` and
    `docs/cerb/tingwu-pipeline/spec.md` in prose to supporting reference under
    the verified BAKE contract without moving or archiving files.
  - Updated the BAKE tracker to Sprint 13 `done` and `audio-pipeline`
    `bake-contract-written`.
  - Evaluator result: static docs acceptance checks passed. No runtime/L3,
    provider-network, BLE, live Tingwu, installed-device, Cerb archive, or
    CHANGELOG work was performed.

## 10. Closeout

**Status**: success

**Tracker summary**: Wrote the audio-pipeline BAKE contract, synced interface-map
authority, and demoted scoped audio Cerb docs to supporting reference.

**Changed files**:

- `docs/bake-contracts/audio-pipeline.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md`

**Evidence command outputs**:

```text
$ rg -n "protocol: BAKE|domain: audio-pipeline|## Pipeline Contract|## Telemetry Joints|## UI Docking Surface|## Core-Flow Gap|## Test Contract|## Cross-Platform Notes" docs/bake-contracts/audio-pipeline.md
2:protocol: BAKE
4:domain: audio-pipeline
24:## Pipeline Contract
120:## Telemetry Joints
154:## UI Docking Surface
163:## Core-Flow Gap
184:## Test Contract
205:## Cross-Platform Notes
```

```text
$ rg -n "manual sync|rec#|log#|Tingwu|drawer|Ask AI|audio reselect|pending|durable|empty-file|summary fallback|canonical valves|runtime evidence|provider" docs/bake-contracts/audio-pipeline.md
28:- Manual sync from the audio drawer against the active badge recording list.
29:- Badge `rec#` audio-ready notifications routed to SIM audio auto-download.
30:- Badge `log#` scheduler-pipeline recordings that may be ingested into SIM
34:- Chat-origin audio reselect from the drawer, including already-transcribed
36:- `Ask AI` from an expanded transcribed drawer card.
37:- Tingwu provider submit, observe, result-link fetch, and artifact persistence
48:- `rec#` creates or resumes SIM SmartBadge placeholders, downloads WAV files,
50:- `log#` remains the scheduler-owned BadgeAudioPipeline path.
58:- Audio reselect reopens the audio drawer from chat.
64:- Summary fallback must be honest degradation.
85:- MUST preserve completed pending durable history once the final artifacts are
89:- MUST require fresh runtime evidence before claiming physical badge, BLE
95:- Badge not ready for manual sync: deny locally and do not invent auto-sync.
139:Target canonical valves from the core-flow include `AUDIO_DRAWER_VISIBLE`,
169:- Gap: manual sync empty-file outcome shape exists, but static Sprint 11
175:- Gap: summary fallback copy currently can imply a generated smart/provider
180:- Gap: runtime evidence is missing for physical badge manual sync, `rec#`,
195:  sections exist, prove manual sync, `rec#`, `log#`, Tingwu, drawer, `Ask AI`,
```

```text
$ rg -n "audio-pipeline|docs/bake-contracts/audio-pipeline.md|supporting reference|docs/cerb/audio-management/spec.md|docs/cerb/tingwu-pipeline/spec.md" docs/cerb/interface-map.md
67:- `docs/bake-contracts/audio-pipeline.md` is the verified BAKE implementation
68:  contract for the audio-pipeline corridor.
71:- `docs/cerb/audio-management/spec.md`,
73:  `docs/cerb/tingwu-pipeline/spec.md`,
94:| **[TingwuPipeline](./tingwu-pipeline/spec.md)** | Hardware & Audio | Transcription & Audio Intelligence; audio-pipeline BAKE implementation record: [`audio-pipeline`](../bake-contracts/audio-pipeline.md) | OSS (reads `fileUrl`) | `submit(TingwuRequest) -> Result<String>` | OS: SSD | ✅ |
95:| **[PipelineTelemetry](./pipeline-telemetry/spec.md)** | System II & Routing | Pipeline logs (to Logcat); audio canonical-valve target remains supporting reference under [`audio-pipeline`](../bake-contracts/audio-pipeline.md) | — | `recordEvent(PipelinePhase, String) -> Unit` | OS: RAM | ✅ |
170:| **[BadgeAudioPipeline](./badge-audio-pipeline/spec.md)** | Hardware & Audio | Audio recording lifecycle; `log#` boundary recorded by [`audio-pipeline`](../bake-contracts/audio-pipeline.md) | ASR, OSS, ConnectivityBridge | Uses `AsrService` for the scheduler fast path; automatic `log#` ingress executes inside `SchedulerPipelineForegroundService` via `SchedulerPipelineOrchestrator`; on successful completion also ingests the recording into SIM audio storage before badge cleanup | — | ✅ |
171:| **[AudioManagement](./audio-management/spec.md)** | Hardware & Audio | Drawer-visible audio inventory, manual sync/transcribe/delete states, persisted artifacts; audio-pipeline BAKE implementation record: [`audio-pipeline`](../bake-contracts/audio-pipeline.md) | ConnectivityBridge, TingwuPipeline | Receives completed badge recordings through the shared SIM audio namespace owned by `SimAudioRepository` | OS: App | ✅ |
172:| **[SIM Audio Chat Lane](../core-flow/sim-audio-artifact-chat-flow.md)** | Hardware & Audio | SIM-local chat composer draft state, audio-grounded discussion continuity, FunASR realtime draft bridge; behavioral north star above [`audio-pipeline`](../bake-contracts/audio-pipeline.md) | SimAudioRepository, SimSessionRepository, SimRealtimeSpeechRecognizer, UserProfileRepository | `SimAgentViewModel`, durable chat/session projections; implementation authority routes through the audio-pipeline BAKE record while scoped audio Cerb docs remain supporting reference | OS: App | 🚧 |
```

```text
$ rg -n "supporting reference|BAKE contract|docs/bake-contracts/audio-pipeline.md" docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md
docs/cerb/tingwu-pipeline/spec.md:4:> **Status**: Supporting reference under verified BAKE contract
docs/cerb/tingwu-pipeline/spec.md:8:> **Implementation Authority Above This Doc**: [`docs/bake-contracts/audio-pipeline.md`](../../bake-contracts/audio-pipeline.md)
docs/cerb/tingwu-pipeline/spec.md:15:Authority note: after Sprint 13, this Cerb spec is supporting reference under
docs/cerb/tingwu-pipeline/spec.md:16:the verified Audio Pipeline BAKE contract at
docs/cerb/tingwu-pipeline/spec.md:17:`docs/bake-contracts/audio-pipeline.md`.
docs/cerb/audio-management/spec.md:4:> **Status**: Supporting reference under verified BAKE contract
docs/cerb/audio-management/spec.md:7:> **Implementation Authority Above This Doc**: [`docs/bake-contracts/audio-pipeline.md`](../../bake-contracts/audio-pipeline.md)
docs/cerb/audio-management/spec.md:17:Authority note: after Sprint 13, this Cerb spec is supporting reference under
docs/cerb/audio-management/spec.md:18:the verified Audio Pipeline BAKE contract at
docs/cerb/audio-management/spec.md:19:`docs/bake-contracts/audio-pipeline.md`.
```

```text
$ rg -n "\| 13 \| audio-pipeline-bake-contract \| done|audio-pipeline \| base-runtime-active .* bake-contract-written" docs/projects/bake-transformation/tracker.md
37:| 13 | audio-pipeline-bake-contract | done | Wrote the audio-pipeline BAKE contract, synced interface-map authority, and demoted scoped audio Cerb docs to supporting reference. | [sprints/13-audio-pipeline-bake-contract.md](sprints/13-audio-pipeline-bake-contract.md) |
47:| audio-pipeline | base-runtime-active | `docs/core-flow/sim-audio-artifact-chat-flow.md` | high | tier-2 | bake-contract-written |
```

```text
$ rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md
3:## 1. Header
14:## 2. Demand
24:## 3. Scope
47:## 4. References
79:## 5. Success Exit Criteria
114:## 6. Stop Exit Criteria
128:## 7. Iteration Bound
132:## 8. Required Evidence Format
150:## 9. Iteration Ledger
174:## 10. Closeout
```

```text
$ git diff --check -- docs/bake-contracts/audio-pipeline.md docs/cerb/interface-map.md docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md
# no output
```

```text
$ git diff --cached --stat -- docs/bake-contracts/audio-pipeline.md docs/cerb/interface-map.md docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md
 docs/bake-contracts/audio-pipeline.md              | 219 +++++++++++++++++++++
 docs/cerb/audio-management/spec.md                 |  15 +-
 docs/cerb/interface-map.md                         |  25 ++-
 docs/cerb/tingwu-pipeline/spec.md                  |  13 +-
 .../sprints/13-audio-pipeline-bake-contract.md     | 142 ++++++++++++-
 docs/projects/bake-transformation/tracker.md       |   4 +-
 6 files changed, 403 insertions(+), 15 deletions(-)
```

**Runtime evidence**: not run by sprint design. Installed-device, BLE,
provider-network, physical badge, and live Tingwu behavior remain unproven.

**Lesson proposals**: none.

**CHANGELOG line**: none.
