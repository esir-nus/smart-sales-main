# Sprint Contract: 15-agent-chat-pipeline-tcf

## 1. Header

- **Slug**: agent-chat-pipeline-tcf
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Codex-authored from previous-agent Sprint 15 plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Author and close `15-agent-chat-pipeline-tcf` as a docs-only BAKE
target-flow alignment sprint on `develop`.

**Codex interpretation**: Consume Sprint 14's delivered-behavior map, align
existing SIM audio/chat and shell target/supporting docs without creating a new
core-flow file, author Sprint 16 as the follow-on BAKE contract sprint, update
tracker state, and close with static documentation evidence only.

## 3. Scope

Docs-only. No Kotlin, API, schema, runtime, CHANGELOG, Cerb archive, or BAKE
contract implementation changes. Explicit write scope:

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md`
- `docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md`

Out of scope:

- creating `docs/core-flow/sim-agent-chat-flow.md`
- code edits
- runtime/L3 validation or installed-device claims
- provider/network, live voice draft, FunASR, microphone, or logcat claims
  without fresh evidence
- `docs/bake-contracts/agent-chat-pipeline.md`
- `docs/cerb/interface-map.md` authority sync
- moving or archiving historical Cerb docs
- `CHANGELOG.md` / `CHANGELOG.html`
- Harmony-native overlay changes

## 4. References

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md`
- `docs/projects/bake-transformation/evidence/14-agent-chat-pipeline-dbm/delivered-behavior-map.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/cerb/sim-audio-chat/spec.md` historical reference only
- `docs/cerb/sim-audio-chat/interface.md` historical reference only

## 5. Success Exit Criteria

1. Sprint 15 contract exists and has sections 1-10 with closeout evidence.
   Verify:
   `rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md`

2. Target/supporting docs include Sprint 14 delivered-vs-target notes for
   `blank/general SIM chat`, `persona`, `user metadata`, `session history`,
   `session persistence`, `durable`, `composer`, `voice draft`, `FunASR`,
   `scheduler`, `follow-up`, `Mono`, `telemetry`, and `runtime evidence`.
   Verify:
   `rg -n "blank/general SIM chat|persona|user metadata|session history|session persistence|durable|composer|voice draft|FunASR|scheduler|follow-up|Mono|telemetry|runtime evidence" docs/core-flow/sim-audio-artifact-chat-flow.md docs/core-flow/sim-shell-routing-flow.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md`

3. Sprint 16 contract exists, is authored for the agent-chat-pipeline BAKE
   contract, and limits execution to writing
   `docs/bake-contracts/agent-chat-pipeline.md`, syncing
   `docs/cerb/interface-map.md`, and demoting scoped historical/supporting docs
   to supporting reference. Verify:
   `rg -n "agent-chat-pipeline-bake-contract|docs/bake-contracts/agent-chat-pipeline.md|docs/cerb/interface-map.md|supporting-reference|supporting reference|historical/supporting|docs/cerb/sim-audio-chat/spec.md|docs/cerb/sim-shell/spec.md" docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md`

4. Tracker row marks Sprint 15 `done`, Sprint 16 `authored`, and
   `agent-chat-pipeline` status `tcf-written`. Verify:
   `rg -n "\\| 15 \\| agent-chat-pipeline-tcf \\| done|\\| 16 \\| agent-chat-pipeline-bake-contract \\| authored|agent-chat-pipeline \\| partially-shipped .* tcf-written" docs/projects/bake-transformation/tracker.md`

5. Static docs diff is clean for whitespace. Verify:
   `git diff --check -- docs/core-flow/sim-audio-artifact-chat-flow.md docs/core-flow/sim-shell-routing-flow.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md`

## 6. Stop Exit Criteria

- **Scope blocker**: Target-flow alignment requires code, runtime,
  interface-map, Cerb archive, BAKE contract, changelog, or new core-flow file
  edits during Sprint 15.
- **Source ambiguity blocker**: Sprint 14 DBM and current target/supporting docs
  conflict so strongly that the operator cannot record factual gap notes
  without a product decision.
- **Runtime-evidence blocker**: Success would require installed UI,
  provider/network, live voice draft, FunASR, microphone, scheduler side-effect,
  follow-up prompt/action strip, or logcat claims. Record those as BAKE gaps and
  stop if they cannot remain unproven.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. Sprint 15 section schema `rg` output.
2. Sprint 14 target/supporting doc `rg` output.
3. Sprint 16 authored-contract `rg` output.
4. Tracker row `rg` output.
5. `git diff --check` over Sprint 15 scope.
6. `git diff --stat` over Sprint 15 scope.

Do not run runtime/L3 validation for this sprint. Installed UI, live voice
draft, provider/network, FunASR, scheduler side effects, follow-up prompt/action
strip, and logcat behavior remain unproven unless a later sprint captures fresh
filtered runtime evidence.

## 9. Iteration Ledger

- **Iteration 1 — 2026-04-29**
  - Read the repo tracker, BAKE tracker, sprint-contract schema, Sprint 14 DBM
    and closeout, BAKE protocol, SIM audio/chat core flow, SIM shell core flow,
    SIM shell spec, SIM shell interface, interface map, and existing audio/shell
    BAKE contracts.
  - Added Sprint 14 delivered-vs-target alignment notes to the existing
    SIM audio/chat and shell target/supporting docs for blank/general SIM chat,
    persona/user metadata, local session history and persistence, durable
    message types, composer send, FunASR voice draft as draft-only,
    scheduler-shaped pre-route, badge scheduler follow-up hosting, Mono memory
    exclusion, telemetry gaps, and missing runtime/L3 evidence.
  - Authored Sprint 16 as the follow-on agent-chat-pipeline BAKE contract
    sprint, scoped to `docs/bake-contracts/agent-chat-pipeline.md`,
    `docs/cerb/interface-map.md`, and supporting-reference demotion for scoped
    historical/supporting docs.
  - Updated the BAKE tracker to Sprint 15 `done`, Sprint 16 `authored`, and
    `agent-chat-pipeline` `tcf-written`.
  - Evaluator result: static docs acceptance checks passed. No runtime/L3,
    installed UI, provider/network, live voice draft, FunASR, microphone,
    scheduler side-effect, follow-up prompt/action strip, logcat, Cerb archive,
    or CHANGELOG work was performed.

## 10. Closeout

**Status**: success

**Tracker summary**: Aligned existing SIM chat/shell target docs with Sprint 14
gaps and authored the agent-chat-pipeline BAKE contract sprint.

**Changed files**:

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-shell/interface.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md`
- `docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md`

**Evidence command outputs**:

```text
$ rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md
3:## 1. Header
14:## 2. Demand
24:## 3. Scope
50:## 4. References
66:## 5. Success Exit Criteria
93:## 6. Stop Exit Criteria
107:## 7. Iteration Bound
111:## 8. Required Evidence Format
127:## 9. Iteration Ledger
151:## 10. Closeout
```

```text
$ rg -n "blank/general SIM chat|persona|user metadata|session history|session persistence|durable|composer|voice draft|FunASR|scheduler|follow-up|Mono|telemetry|runtime evidence" docs/core-flow/sim-audio-artifact-chat-flow.md docs/core-flow/sim-shell-routing-flow.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md
docs/core-flow/sim-audio-artifact-chat-flow.md:193:- BAKE target: blank/general SIM chat is a first-class entry before audio. It
docs/core-flow/sim-audio-artifact-chat-flow.md:197:- BAKE target: general SIM chat is grounded in system persona, user metadata,
docs/core-flow/sim-audio-artifact-chat-flow.md:200:- BAKE target: local session history and session persistence stay SIM-only.
docs/core-flow/sim-audio-artifact-chat-flow.md:204:- BAKE target: composer send is explicit. FunASR voice draft is draft-only:
docs/core-flow/sim-audio-artifact-chat-flow.md:207:- BAKE boundary: scheduler-shaped text in a general SIM chat may pre-route to
docs/core-flow/sim-audio-artifact-chat-flow.md:211:- BAKE boundary: badge scheduler follow-up may be hosted in the normal SIM chat
docs/core-flow/sim-audio-artifact-chat-flow.md:214:- BAKE exclusion: Mono/smart-agent memory stays outside this target. Kernel
docs/core-flow/sim-audio-artifact-chat-flow.md:217:- BAKE gap: agent-chat-pipeline telemetry is incomplete. General chat input,
docs/core-flow/sim-audio-artifact-chat-flow.md:221:- BAKE gap: runtime evidence remains missing for installed blank chat,
docs/core-flow/sim-shell-routing-flow.md:38:- Target behavior remains general-chat-first: blank/general SIM chat is a real
docs/cerb/sim-shell/spec.md:43:- blank/general SIM chat is a supported shell state before audio attachment,
docs/cerb/sim-shell/interface.md:25:- blank/general SIM chat is a supported shell surface before audio attachment.
```

```text
$ rg -n "agent-chat-pipeline-bake-contract|docs/bake-contracts/agent-chat-pipeline.md|docs/cerb/interface-map.md|supporting-reference|supporting reference|historical/supporting|docs/cerb/sim-audio-chat/spec.md|docs/cerb/sim-shell/spec.md" docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md
1:# Sprint Contract: 16-agent-chat-pipeline-bake-contract
5:- **Slug**: agent-chat-pipeline-bake-contract
22:scoped historical/supporting docs to supporting reference.
30:- `docs/bake-contracts/agent-chat-pipeline.md`
31:- `docs/cerb/interface-map.md`
32:- `docs/cerb/sim-audio-chat/spec.md`
34:- `docs/cerb/sim-shell/spec.md`
49:  from supporting-reference demotion to physical archive movement
90:1. `docs/bake-contracts/agent-chat-pipeline.md` exists and follows the BAKE
104:3. `docs/cerb/interface-map.md` cites
106:   implementation authority and keeps scoped historical/supporting docs as
107:   supporting reference.
```

```text
$ rg -n "\| 15 \| agent-chat-pipeline-tcf \| done|\| 16 \| agent-chat-pipeline-bake-contract \| authored|agent-chat-pipeline \| partially-shipped .* tcf-written" docs/projects/bake-transformation/tracker.md
39:| 15 | agent-chat-pipeline-tcf | done | Aligned existing SIM chat/shell target docs with Sprint 14 gaps and authored the agent-chat-pipeline BAKE contract sprint. | [sprints/15-agent-chat-pipeline-tcf.md](sprints/15-agent-chat-pipeline-tcf.md) |
40:| 16 | agent-chat-pipeline-bake-contract | authored | Write the agent-chat-pipeline BAKE contract, sync interface-map authority, and demote scoped historical/supporting docs to supporting reference. | [sprints/16-agent-chat-pipeline-bake-contract.md](sprints/16-agent-chat-pipeline-bake-contract.md) |
51:| agent-chat-pipeline | partially-shipped | `docs/cerb/sim-audio-chat/spec.md` | high | tier-2 | tcf-written |
```

```text
$ git diff --check -- docs/core-flow/sim-audio-artifact-chat-flow.md docs/core-flow/sim-shell-routing-flow.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md
[no output]
```

```text
$ git diff --stat -- docs/core-flow/sim-audio-artifact-chat-flow.md docs/core-flow/sim-shell-routing-flow.md docs/cerb/sim-shell/spec.md docs/cerb/sim-shell/interface.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md
 docs/cerb/sim-shell/interface.md               | 19 ++++++++++++
 docs/cerb/sim-shell/spec.md                    | 26 ++++++++++++++++
 docs/core-flow/sim-audio-artifact-chat-flow.md | 41 ++++++++++++++++++++++++++
 docs/core-flow/sim-shell-routing-flow.md       | 26 ++++++++++++++++
 docs/projects/bake-transformation/tracker.md   |  4 ++-
 5 files changed, 115 insertions(+), 1 deletion(-)
```

```text
$ git diff --stat --no-index /dev/null docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md || true
 .../sprints/15-agent-chat-pipeline-tcf.md          | 260 +++++++++++++++++++++
 1 file changed, 260 insertions(+)

$ git diff --stat --no-index /dev/null docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md || true
 .../16-agent-chat-pipeline-bake-contract.md        | 173 +++++++++++++++++++++
 1 file changed, 173 insertions(+)
```

```text
$ git status --short --ignored -- docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md
!! docs/projects/bake-transformation/sprints/15-agent-chat-pipeline-tcf.md
!! docs/projects/bake-transformation/sprints/16-agent-chat-pipeline-bake-contract.md
```

**Runtime evidence**: not run by sprint design. Installed UI, live voice draft,
provider/network, FunASR, microphone, scheduler side effects, follow-up
prompt/action strip, and logcat behavior remain unproven.

**Lesson proposals**: none.

**CHANGELOG line**: none.
