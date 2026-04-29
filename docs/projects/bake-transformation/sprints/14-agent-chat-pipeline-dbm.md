# Sprint Contract: 14-agent-chat-pipeline-dbm

## 1. Header

- **Slug**: agent-chat-pipeline-dbm
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Codex-authored from previous-agent Sprint 14 plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Execute Sprint 14 as a docs-only BAKE delivered-behavior-map
sprint for `agent-chat-pipeline` on `develop`.

**Codex interpretation**: This sprint maps current delivered behavior for
general SIM chat outside the audio-specific pipeline: blank chat, composer
send, persona/user metadata context, SIM-local session history and persistence,
voice draft, scheduler-shaped routing boundaries, follow-up hosting boundaries,
telemetry, tests, and runtime evidence gaps. It does not change code, target
core-flow docs, Cerb authority, BAKE contracts, or changelog entries.

## 3. Scope

Docs-only. No code changes. Explicit write scope:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md`
- `docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/`

No files outside this list may be written.

Out of scope:

- Kotlin/API/schema/runtime behavior changes
- target core-flow edits
- Cerb demotion, archive moves, or authority changes
- BAKE implementation contract writing
- CHANGELOG line
- Harmony-native or platform overlay changes
- audio-pipeline remapping except as boundary separation
- installed UI, live voice draft, provider/network, logcat, device, or L3
  claims without fresh evidence captured in a later sprint

## 4. References

Governance and project protocol:

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/projects/bake-transformation/tracker.md`

Authority and boundary docs:

- `docs/cerb/interface-map.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/sim-audio-chat/spec.md` historical reference only
- `docs/cerb/sim-audio-chat/interface.md` historical reference only
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`

Code and test inventory:

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
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimAgentViewModelStructureTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/session/SimSessionRepositoryTest.kt`
- `app-core/src/test/java/com/smartsales/prism/data/audio/SimRealtimeSpeechRecognizerConfigTest.kt`
- `app-core/src/test/java/com/smartsales/prism/ui/sim/SimShellHandoffTest.kt`

## 5. Success Exit Criteria

1. Delivered-behavior map exists at
   `docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`.
   Verify:
   `test -f docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`

2. The DBM covers required chat terms: blank chat, general SIM chat, persona,
   user metadata, session history, composer, voice draft, FunASR, session
   persistence, durable, scheduler, follow-up, audio boundary, Mono,
   telemetry, and runtime evidence. Verify:
   `rg -n "blank chat|general SIM chat|persona|user metadata|session history|composer|voice draft|FunASR|session persistence|durable|scheduler|follow-up|audio boundary|Mono|telemetry|runtime evidence" docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`

3. The DBM distinguishes evidence classes: Delivered behavior, Target
   behavior, Gap, Historical reference, and Unknown. Verify:
   `rg -n "Delivered behavior|Target behavior|Gap|Historical reference|Unknown" docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`

4. The DBM cites at least 15 distinct repo paths including docs and Kotlin/test
   paths. Verify:
   `rg -o '`(docs|app-core|core)/[^`]+`' docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md | sort -u | wc -l`

5. The sprint contract has all ten required sections and includes closeout
   static evidence. Verify:
   `rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md`

6. Tracker row for Sprint 14 is `done` on success, and `agent-chat-pipeline`
   domain status is `dbm-written`. Verify:
   `rg -n "\| 14 \| agent-chat-pipeline-dbm \| done|\| agent-chat-pipeline \| partially-shipped .* dbm-written" docs/projects/bake-transformation/tracker.md`

7. No runtime/L3 claims are made without fresh evidence. Missing installed UI,
   live voice draft, provider/network, and logcat behavior are recorded as Gap
   or Unknown. Verify:
   `rg -n "installed UI|live voice draft|provider/network|logcat|runtime evidence|Gap|Unknown" docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`

8. Static diff hygiene passes for sprint-scoped files. Verify:
   `git diff --check -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/`

## 6. Stop Exit Criteria

- **Scope blocker**: The delivered behavior cannot be mapped without writing
  outside the sprint scope.
- **Source ambiguity blocker**: Current docs, code, and tests conflict so
  strongly that a factual DBM would require a product decision.
- **Runtime-evidence blocker**: Installed UI, live voice draft, provider/network,
  logcat, device, or L3 behavior would be required to claim success. Record
  missing proof as a gap; do not claim it.
- **Authority creep**: Any pull toward target core-flow edits, Cerb demotion,
  BAKE contract writing, CHANGELOG edits, platform overlay changes, or code
  changes.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. `test -f docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`
2. Required-term `rg` check for the DBM.
3. Evidence-class `rg` check for the DBM.
4. Distinct cited-path count.
5. Ten-section contract check.
6. Tracker row and domain-status check.
7. `git diff --check` for sprint-scoped files.
8. `git diff --stat` for sprint-scoped files.

Do not run runtime/L3 validation for this sprint. Installed UI, live voice
draft, provider/network, and logcat behavior remain unproven unless a later
sprint captures fresh filtered runtime evidence.

## 9. Iteration Ledger

- **Iteration 1 — 2026-04-29**
  - Read the Sprint 14 plan, AGENTS repo contract, BAKE project tracker,
    sprint-contract schema, project-structure spec, BAKE protocol, interface
    map, SIM audio/chat core flow, historical SIM audio-chat docs, SIM shell
    docs, and current SIM chat/session/voice/follow-up Kotlin source and tests.
  - Wrote
    `docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`
    with delivered-vs-target/gap/historical/unknown treatment for blank/general
    SIM chat, composer send, persona/user metadata context, local session
    history, session persistence, voice draft/FunASR, scheduler routing,
    follow-up hosting, telemetry, Mono exclusion, tests, and runtime evidence
    gaps.
  - Updated the Sprint 14 tracker row to `done` and the `agent-chat-pipeline`
    domain status to `dbm-written`.
  - Evaluator result: static acceptance checks passed. No installed UI, live
    voice draft, provider/network, logcat, device, or L3 claims were made.

## 10. Closeout

**Status**: success

**Tracker summary**: Delivered behavior map created for agent-chat-pipeline,
covering blank/general SIM chat, composer send, persona/user context, local
session history and persistence, FunASR voice draft, scheduler and follow-up
boundaries, telemetry, tests, and runtime-evidence gaps.

**Changed files**:

- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md`
- `docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`

**Evidence command outputs**:

```text
$ test -f docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md && echo exists
exists
```

```text
$ rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md
3:## 1. Header
14:## 2. Demand
26:## 3. Scope
48:## 4. References
84:## 5. Success Exit Criteria
121:## 6. Stop Exit Criteria
135:## 7. Iteration Bound
139:## 8. Required Evidence Format
156:## 9. Iteration Ledger
175:## 10. Closeout
```

```text
$ rg -n "blank chat|general SIM chat|persona|user metadata|session history|composer|voice draft|FunASR|session persistence|durable|scheduler|follow-up|audio boundary|Mono|telemetry|runtime evidence" docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md
67:SIM chat shell outside the audio-specific pipeline: blank chat, general SIM
68:chat, composer/send, persona and user metadata prompt context, local session
69:history, session list/create/delete/restore persistence, durable message
70:types, FunASR voice draft, scheduler-shaped top-level routing, badge scheduler
71:follow-up hosting, telemetry, tests, and runtime evidence gaps.
```

```text
$ rg -n "Delivered behavior|Target behavior|Gap|Historical reference|Unknown" docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md
40:Historical reference docs:
66:Delivered behavior: this map covers agent-chat-pipeline base behavior for the
79:Target behavior: `docs/core-flow/sim-audio-artifact-chat-flow.md` says general
110:Gap: no installed UI or provider/network evidence was captured to prove the
113:Unknown: live LLM latency, retry behavior, and exact user-visible fallback copy
```

```text
$ rg -o '`(docs|app-core|core)/[^`]+`' docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md | sort -u | wc -l
21
```

```text
$ rg -n "\| 14 \| agent-chat-pipeline-dbm \| done|\| agent-chat-pipeline \| partially-shipped .* dbm-written" docs/projects/bake-transformation/tracker.md
38:| 14 | agent-chat-pipeline-dbm | done | Delivered behavior map created for agent-chat-pipeline, covering blank/general SIM chat, composer send, persona/user context, local session history and persistence, FunASR voice draft, scheduler and follow-up boundaries, telemetry, tests, and runtime-evidence gaps. | [sprints/14-agent-chat-pipeline-dbm.md](sprints/14-agent-chat-pipeline-dbm.md) |
49:| agent-chat-pipeline | partially-shipped | `docs/cerb/sim-audio-chat/spec.md` | high | tier-2 | dbm-written |
```

```text
$ git diff --check -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/
[no output]
```

```text
$ git diff --stat -- docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/
 .../delivered-behavior-map.md                      | 410 +++++++++++++++++++++
 .../sprints/14-agent-chat-pipeline-dbm.md          | 255 +++++++++++++
 docs/projects/bake-transformation/tracker.md       |   3 +-
 3 files changed, 667 insertions(+), 1 deletion(-)
```

**Lesson proposals**: none.

**CHANGELOG line**: none.
