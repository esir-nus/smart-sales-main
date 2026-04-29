# Sprint Contract: 16-agent-chat-pipeline-bake-contract

## 1. Header

- **Slug**: agent-chat-pipeline-bake-contract
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Codex-authored during Sprint 15 TCF close
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Author the follow-on
`16-agent-chat-pipeline-bake-contract` sprint after stable agent-chat target
alignment.

**Codex interpretation**: The operator writes the durable BAKE implementation
contract for the agent-chat-pipeline, syncs interface-map authority, and demotes
scoped historical/supporting docs to supporting reference. Sprint 16 is not a
runtime code change sprint unless the contract cannot be verified from current
evidence.

## 3. Scope

Docs-only unless a stop criterion forces user arbitration. Explicit write scope:

- `docs/bake-contracts/agent-chat-pipeline.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md`

Allowed supporting read scope includes current SIM audio/chat and shell
core-flow docs, Sprint 14 DBM, Sprint 15 TCF notes, existing audio/shell BAKE
contracts, interface map, SIM chat/session/follow-up/voice-draft source, and
relevant JVM/source tests.

Out of scope:

- Kotlin/API/schema/runtime behavior changes
- creating a new agent-chat core-flow file
- moving Cerb docs into archive unless the user explicitly expands the sprint
  from supporting-reference demotion to physical archive movement
- changing the audio-pipeline or shell-routing BAKE contracts except for
  cross-reference drift that blocks Sprint 16
- CHANGELOG line unless the user explicitly approves one at close
- Harmony-native overlay changes

## 4. References

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md`
- `docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md`
- `docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/bake-contracts/audio-pipeline.md`
- `docs/bake-contracts/shell-routing.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentChatCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentSessionCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentVoiceDraftCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentFollowUpCoordinator.kt`
- `app-core/src/main/java/com/smartsales/prism/data/session/SimSessionRepository.kt`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizer.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/RuntimeShellContent.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/session/SimSessionRepositoryTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizerConfigTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`

## 5. Success Exit Criteria

1. `docs/bake-contracts/agent-chat-pipeline.md` exists and follows the BAKE
   contract format from `docs/specs/bake-protocol.md`, including frontmatter,
   Pipeline Contract, Telemetry Joints, UI Docking Surface, Core-Flow Gap,
   MONO Extension Surface, Test Contract, and Cross-Platform Notes. Verify:
   `rg -n "protocol: BAKE|domain: agent-chat-pipeline|runtime: partially-shipped|## Pipeline Contract|## Telemetry Joints|## UI Docking Surface|## Core-Flow Gap|## MONO Extension Surface|## Test Contract|## Cross-Platform Notes" docs/bake-contracts/agent-chat-pipeline.md`

2. The BAKE contract explicitly covers blank/general SIM chat, persona, user
   metadata, local session history, SIM-only session persistence, durable
   message types, composer send, FunASR voice draft, scheduler-shaped pre-route,
   badge scheduler follow-up, Mono/smart-agent memory exclusion, telemetry
   gaps, runtime evidence gaps, provider/network gaps, installed UI gaps, and
   logcat gaps. Verify:
   `rg -n "blank/general SIM chat|persona|user metadata|session history|session persistence|durable|composer|FunASR|voice draft|scheduler-shaped|follow-up|Mono|smart-agent memory|telemetry|runtime evidence|provider/network|installed UI|logcat" docs/bake-contracts/agent-chat-pipeline.md`

3. `docs/cerb/interface-map.md` cites
   `docs/bake-contracts/agent-chat-pipeline.md` as the agent-chat-pipeline
   implementation authority and keeps scoped historical/supporting docs as
   supporting reference. Verify:
   `rg -n "agent-chat-pipeline|docs/bake-contracts/agent-chat-pipeline.md|supporting reference|docs/cerb/sim-audio-chat/spec.md|docs/cerb/sim-shell/spec.md" docs/cerb/interface-map.md`

4. Scoped historical/supporting docs are demoted in prose to supporting
   reference for the verified BAKE contract without deleting their historical
   context. Verify:
   `rg -n "supporting reference|BAKE contract|docs/bake-contracts/agent-chat-pipeline.md" docs/cerb/sim-audio-chat/spec.md docs/cerb/sim-audio-chat/interface.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md`

5. Tracker row marks Sprint 16 `done` on success and
   `agent-chat-pipeline` status `bake-contract-written`. Verify:
   `rg -n "\\| 16 \\| agent-chat-pipeline-bake-contract \\| done|agent-chat-pipeline \\| partially-shipped .* bake-contract-written" docs/projects/bake-transformation/tracker.md`

6. Sprint 16 contract has all ten required sections and includes closeout
   evidence. Verify:
   `rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md`

7. Static diff is clean. Verify:
   `git diff --check -- docs/bake-contracts/agent-chat-pipeline.md docs/cerb/interface-map.md docs/cerb/sim-audio-chat/spec.md docs/cerb/sim-audio-chat/interface.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md`

## 6. Stop Exit Criteria

- **Contract authority blocker**: Current docs/code/tests cannot support a
  factual BAKE implementation contract without a product decision.
- **Scope blocker**: Verification requires Kotlin/runtime changes, a new
  core-flow file, Cerb archive movement, Harmony overlay work, audio/shell BAKE
  contract rewrites, or CHANGELOG edits not approved for Sprint 16.
- **Runtime-evidence blocker**: The operator must not claim installed UI,
  provider/network general LLM behavior, live voice draft, FunASR, microphone,
  scheduler side effects, badge-origin follow-up prompt/action strip, or logcat
  behavior without fresh filtered `adb logcat` and relevant command evidence.
  If those claims are required for success, stop and ask for device/provider
  validation scope.
- **Interface-map blocker**: Existing interface-map ownership conflicts with
  agent-chat-pipeline authority in a way that cannot be resolved by docs-only
  sync.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. BAKE contract structure `rg` output.
2. BAKE contract agent-chat branch/gap `rg` output.
3. Interface-map authority `rg` output.
4. Scoped historical/supporting docs supporting-reference `rg` output.
5. Tracker row `rg` output.
6. Sprint 16 section schema `rg` output.
7. `git diff --check` over Sprint 16 scope.
8. `git diff --stat` over Sprint 16 scope.

Runtime/L3 validation is not required unless Sprint 16 expands beyond docs-only
authority sync. Any installed UI, provider/network, live voice draft, FunASR,
microphone, scheduler side-effect, follow-up prompt/action strip, or logcat
claim remains unproven unless fresh filtered `adb logcat` and relevant command
evidence are captured during the sprint.

## 9. Iteration Ledger

Operator fills during Sprint 16.

## 10. Closeout

Operator fills at Sprint 16 close.
