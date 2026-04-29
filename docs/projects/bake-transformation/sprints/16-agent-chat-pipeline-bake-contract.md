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

- **Iteration 1 — 2026-04-29**
  - Read the repo tracker, BAKE project tracker, sprint-contract schema,
    project-structure spec, BAKE protocol, Sprint 14 DBM, Sprint 15 TCF
    closeout, interface map, existing audio/shell BAKE contracts, and scoped
    SIM audio-chat and shell docs.
  - Wrote `docs/bake-contracts/agent-chat-pipeline.md` as the verified BAKE
    implementation contract for the base agent-chat slice: blank/general SIM
    chat, persona/user metadata context, local session history, SIM-only
    persistence, durable message types, composer send, FunASR voice draft,
    scheduler-shaped pre-route, badge scheduler follow-up hosting, telemetry
    gaps, runtime evidence gaps, provider/network gaps, installed UI gaps, and
    logcat gaps.
  - Updated `docs/cerb/interface-map.md` to add the agent-chat-pipeline BAKE
    overlay and route SIM Audio Chat Lane implementation authority through
    agent-chat-pipeline for chat/session/voice/follow-up, audio-pipeline for
    audio-specific behavior, and shell-routing for shell route arbitration.
  - Added supporting-reference BAKE notices to scoped historical/supporting
    SIM audio-chat and shell docs without removing existing shell-routing or
    audio-pipeline authority.
  - Updated the BAKE tracker to Sprint 16 `done` and agent-chat-pipeline
    `bake-contract-written`.
  - Evaluator result: static acceptance checks passed. No Kotlin/API/schema,
    runtime/L3, installed UI, provider/network, live voice draft, FunASR,
    microphone, scheduler side-effect, follow-up prompt/action strip, logcat,
    Cerb archive, or CHANGELOG work was performed.

## 10. Closeout

**Status**: success

**Tracker summary**: Wrote the agent-chat-pipeline BAKE contract, synced
interface-map authority, and demoted scoped historical/supporting docs to
supporting reference.

**Changed files**:

- `docs/bake-contracts/agent-chat-pipeline.md`
- `docs/cerb/interface-map.md`
- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md`

**Evidence command outputs**:

```text
$ rg -n "protocol: BAKE|domain: agent-chat-pipeline|runtime: partially-shipped|## Pipeline Contract|## Telemetry Joints|## UI Docking Surface|## Core-Flow Gap|## MONO Extension Surface|## Test Contract|## Cross-Platform Notes" docs/bake-contracts/agent-chat-pipeline.md
2:protocol: BAKE
4:domain: agent-chat-pipeline
6:runtime: partially-shipped
26:## Pipeline Contract
122:## Telemetry Joints
150:## UI Docking Surface
159:## Core-Flow Gap
181:## MONO Extension Surface
200:## Test Contract
225:## Cross-Platform Notes
```

```text
$ rg -n "blank/general SIM chat|persona|user metadata|session history|session persistence|durable|composer|FunASR|voice draft|scheduler-shaped|follow-up|Mono|smart-agent memory|telemetry|runtime evidence|provider/network|installed UI|logcat" docs/bake-contracts/agent-chat-pipeline.md
21:slice: blank/general SIM chat, local session continuity, composer send, FunASR
35:- SIM-local session history from the active session, excluding transient UI
38:- FunASR realtime voice draft events for partial, final, cancel, no-match,
48:  require an audio item, Mono memory, or the smart-agent system.
51:- General chat prompts include the SIM persona, user metadata, and bounded
57:- SIM session persistence is SIM-only: session metadata is stored in
61:  artifacts, and AI error. Thinking, streaming, voice draft, input text,
70:- Badge scheduler follow-up is task-scoped. SIM may host the follow-up session,
94:- MUST require fresh runtime evidence before claiming installed UI,
95:  provider/network, live voice draft, microphone, FunASR auth/session,
96:  scheduler side effects, follow-up prompt/action strip, or logcat behavior.
141:Delivered telemetry is partial. Static evidence found scheduler shelf handoff,
147:agent-chat-pipeline valve family remain telemetry gaps. No fresh `adb logcat`
164:- Gap: installed UI behavior is unproven for blank chat, composer send, grouped
167:- Gap: provider/network behavior is unproven for general executor calls,
183:The base agent-chat contract is deliberately complete without Mono. Future Mono
213:  sections exist; prove blank/general SIM chat, persona, user metadata,
214:  session history, session persistence, durable message types, composer send,
215:  FunASR voice draft, scheduler-shaped pre-route, follow-up, Mono exclusion,
216:  telemetry gaps, runtime evidence gaps, provider/network gaps, installed UI
217:  gaps, and logcat gaps are recorded; prove interface-map and scoped supporting
```

```text
$ rg -n "agent-chat-pipeline|docs/bake-contracts/agent-chat-pipeline.md|supporting reference|docs/cerb/sim-audio-chat/spec.md|docs/cerb/sim-shell/spec.md" docs/cerb/interface-map.md
63:  `docs/cerb/sim-shell/spec.md`, and
80:- `docs/bake-contracts/agent-chat-pipeline.md` is the verified BAKE
81:  implementation contract for the base agent-chat-pipeline slice: blank/general
88:- `docs/cerb/sim-audio-chat/spec.md`,
90:  `docs/cerb/sim-shell/spec.md`, and
91:  `docs/cerb/sim-shell/interface.md` remain supporting reference beneath the
186:| **[SIM Audio Chat Lane](../core-flow/sim-audio-artifact-chat-flow.md)** | Hardware & Audio | SIM-local chat composer draft state, audio-grounded discussion continuity, FunASR realtime draft bridge; behavioral north star above [`audio-pipeline`](../bake-contracts/audio-pipeline.md) and [`agent-chat-pipeline`](../bake-contracts/agent-chat-pipeline.md) | SimAudioRepository, SimSessionRepository, SimRealtimeSpeechRecognizer, UserProfileRepository | `SimAgentViewModel`, durable chat/session projections; agent-chat implementation authority routes through the [`agent-chat-pipeline`](../bake-contracts/agent-chat-pipeline.md) BAKE record, audio-specific behavior routes through [`audio-pipeline`](../bake-contracts/audio-pipeline.md), and shell routing stays under [`shell-routing`](../bake-contracts/shell-routing.md); scoped audio/chat and shell Cerb docs remain supporting reference | OS: App | 🚧 |
```

```text
$ rg -n "supporting reference|BAKE contract|docs/bake-contracts/agent-chat-pipeline.md" docs/cerb/sim-audio-chat/spec.md docs/cerb/sim-audio-chat/interface.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md
docs/cerb/sim-shell/interface.md:10:> `docs/bake-contracts/agent-chat-pipeline.md` for base SIM chat/session/voice
docs/cerb/sim-shell/interface.md:16:> - `docs/bake-contracts/agent-chat-pipeline.md`
docs/cerb/sim-shell/spec.md:12:> `docs/bake-contracts/agent-chat-pipeline.md` for base SIM chat/session/voice
docs/cerb/sim-shell/spec.md:18:> - `docs/bake-contracts/agent-chat-pipeline.md`
docs/cerb/sim-shell/spec.md:45:the notes below are supporting reference only beneath current core-flow and
docs/cerb/sim-shell/spec.md:46:future BAKE contract authority:
docs/cerb/sim-audio-chat/spec.md:10:> beneath `docs/bake-contracts/agent-chat-pipeline.md` for blank/general SIM
docs/cerb/sim-audio-chat/spec.md:18:> - `docs/bake-contracts/agent-chat-pipeline.md`
docs/cerb/sim-audio-chat/interface.md:8:> beneath `docs/bake-contracts/agent-chat-pipeline.md` for blank/general SIM
docs/cerb/sim-audio-chat/interface.md:14:> - `docs/bake-contracts/agent-chat-pipeline.md`
```

```text
$ rg -n "\| 16 \| agent-chat-pipeline-bake-contract \| done|agent-chat-pipeline \| partially-shipped .* bake-contract-written" docs/projects/bake-transformation/tracker.md
40:| 16 | agent-chat-pipeline-bake-contract | done | Wrote the agent-chat-pipeline BAKE contract, synced interface-map authority, and demoted scoped historical/supporting docs to supporting reference. | [sprints/16-agent-chat-pipeline-bake-contract.md](sprints/16-agent-chat-pipeline-bake-contract.md) |
51:| agent-chat-pipeline | partially-shipped | `docs/cerb/sim-audio-chat/spec.md` | high | tier-2 | bake-contract-written |
```

```text
$ rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md
3:## 1. Header
14:## 2. Demand
26:## 3. Scope
55:## 4. References
88:## 5. Success Exit Criteria
126:## 6. Stop Exit Criteria
144:## 7. Iteration Bound
148:## 8. Required Evidence Format
167:## 9. Iteration Ledger
195:## 10. Closeout
```

```text
$ git diff --check -- docs/bake-contracts/agent-chat-pipeline.md docs/cerb/interface-map.md docs/cerb/sim-audio-chat/spec.md docs/cerb/sim-audio-chat/interface.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md
[no output]
```

```text
$ git diff --stat -- docs/cerb/interface-map.md docs/cerb/sim-audio-chat/spec.md docs/cerb/sim-audio-chat/interface.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md
 docs/cerb/interface-map.md                         |  18 ++-
 docs/cerb/sim-audio-chat/interface.md              |   9 ++
 docs/cerb/sim-audio-chat/spec.md                   |   9 ++
 docs/cerb/sim-shell/interface.md                   |   5 +
 docs/cerb/sim-shell/spec.md                        |   5 +
 .../16-agent-chat-pipeline-bake-contract.md        | 158 ++++++++++++++++++++-
 docs/projects/bake-transformation/tracker.md       |   4 +-
 7 files changed, 202 insertions(+), 6 deletions(-)

$ git diff --stat --no-index /dev/null docs/bake-contracts/agent-chat-pipeline.md || true
 docs/bake-contracts/agent-chat-pipeline.md | 240 ++++++++++++++++++++++++++++
 1 file changed, 240 insertions(+)
```

**Runtime evidence**: not run by sprint design. Installed UI,
provider/network, live voice draft, FunASR, microphone, scheduler side effects,
follow-up prompt/action strip, and logcat behavior remain unproven.

**Lesson proposals**: none.

**CHANGELOG line**: none.
