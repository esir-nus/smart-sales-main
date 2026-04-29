# Sprint Contract: 12-audio-pipeline-tcf

## 1. Header

- **Slug**: audio-pipeline-tcf
- **Project**: bake-transformation
- **Date authored**: 2026-04-29
- **Author**: Codex-authored from previous-agent Sprint 12 plan
- **Operator**: Codex
- **Lane**: develop
- **Branch**: develop
- **Worktree**: not applicable

## 2. Demand

**User ask**: Create and execute `12-audio-pipeline-tcf` as a docs-only BAKE
target-flow alignment sprint on `develop`.

**Codex interpretation**: Consume Sprint 11's audio delivered-behavior map,
sync the audio target/supporting docs with delivered-vs-target gap notes, author
the follow-on Sprint 13 BAKE contract, update tracker state, and close with
static documentation evidence only.

## 3. Scope

Docs-only. No Kotlin, API, schema, runtime, CHANGELOG, Cerb archive, or
interface-map authority changes. Explicit write scope:

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/12-audio-pipeline-tcf.md`
- `docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md`

Out of scope:

- code edits
- runtime/L3 validation or installed-device claims
- BLE, provider-network, or live Tingwu claims without fresh evidence
- `docs/cerb/interface-map.md` authority sync
- moving or archiving Cerb docs
- `docs/bake-contracts/audio-pipeline.md`
- `CHANGELOG.md` / `CHANGELOG.html`

## 4. References

- `AGENTS.md`
- `docs/specs/bake-protocol.md`
- `docs/specs/sprint-contract.md`
- `docs/specs/project-structure.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/11-audio-pipeline-dbm.md`
- `docs/projects/bake-transformation/evidence/11-audio-pipeline-dbm/delivered-behavior-map.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/tingwu-pipeline/spec.md`

## 5. Success Exit Criteria

1. Sprint 12 contract exists and has sections 1-10 with closeout evidence.
   Verify:
   `rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/12-audio-pipeline-tcf.md`

2. Audio target/supporting docs include Sprint 11 delivered-vs-target gap notes
   for `log#`, minimal ASR, empty-file counts, summary fallback, pending
   durable history, canonical valves, runtime evidence, provider proof, and
   BAKE gap language. Verify:
   `rg -n "Sprint 11|log#|minimal ASR|empty-file|summary fallback|pending|durable|canonical valves|runtime evidence|provider|BAKE gap" docs/core-flow/sim-audio-artifact-chat-flow.md docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md`

3. Sprint 13 contract exists, is authored for the audio BAKE contract, and
   limits execution to writing `docs/bake-contracts/audio-pipeline.md`, syncing
   interface-map authority, and demoting scoped audio Cerb docs to supporting
   reference. Verify:
   `rg -n "audio-pipeline-bake-contract|docs/bake-contracts/audio-pipeline.md|interface-map authority|supporting reference|docs/cerb/interface-map.md|docs/cerb/audio-management/spec.md|docs/cerb/tingwu-pipeline/spec.md" docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md`

4. Tracker row marks Sprint 12 `done`, Sprint 13 `authored`, and
   `audio-pipeline` status `tcf-written`. Verify:
   `rg -n "\\| 12 \\| audio-pipeline-tcf \\| done|\\| 13 \\| audio-pipeline-bake-contract \\| authored|audio-pipeline \\| base-runtime-active .* tcf-written" docs/projects/bake-transformation/tracker.md`

5. Static docs diff is clean for whitespace. Verify:
   `git diff --check -- docs/core-flow/sim-audio-artifact-chat-flow.md docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/12-audio-pipeline-tcf.md docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md`

## 6. Stop Exit Criteria

- **Scope blocker**: Target-flow alignment requires code, runtime, interface-map,
  Cerb archive, BAKE contract, or changelog edits during Sprint 12.
- **Source ambiguity blocker**: Sprint 11 DBM and current target/supporting docs
  conflict so strongly that the operator cannot record factual gap notes without
  a product decision.
- **Runtime-evidence blocker**: Success would require installed-device, BLE,
  provider-network, live Tingwu, or UI behavior claims. Record those as BAKE
  gaps and stop if they cannot remain unproven.
- **Iteration bound hit**: Stop rather than continue past the bound.

## 7. Iteration Bound

2 iterations.

## 8. Required Evidence Format

Closeout must include:

1. Sprint 12 section schema `rg` output.
2. Sprint 13 authored-contract `rg` output.
3. Audio target/supporting doc `rg` output for Sprint 11 gap terms.
4. Tracker row `rg` output.
5. `git diff --check` over Sprint 12 scope.
6. `git diff --stat` over Sprint 12 scope.

Do not run runtime/L3 validation for this sprint. Any installed-device, BLE,
provider-network, or live Tingwu behavior remains unproven unless fresh filtered
`adb logcat` and relevant command evidence are captured.

## 9. Iteration Ledger

- **Iteration 1 — 2026-04-29**
  - Read the repo tracker, BAKE tracker, sprint-contract schema, Sprint 11 DBM
    and closeout, BAKE protocol, audio core flow, audio-management spec, and
    Tingwu pipeline spec.
  - Added Sprint 11 delivered-vs-target alignment notes to the audio core flow
    and supporting Cerb specs for `log#` minimal ASR ingest, empty-file sync
    counts, summary fallback copy, pending durable history, canonical valves,
    and runtime/provider evidence gaps.
  - Authored Sprint 13 as the follow-on audio BAKE contract sprint, scoped to
    `docs/bake-contracts/audio-pipeline.md`, interface-map authority sync, and
    supporting-reference demotion for scoped audio Cerb docs.
  - Updated the BAKE tracker to Sprint 12 `done`, Sprint 13 `authored`, and
    `audio-pipeline` `tcf-written`.
  - Evaluator result: static docs acceptance checks passed. No runtime/L3,
    provider-network, BLE, live Tingwu, or installed-device claims were made.

## 10. Closeout

**Status**: success

**Tracker summary**: Aligned audio target/supporting docs with Sprint 11 gaps
and authored the audio pipeline BAKE contract sprint.

**Changed files**:

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/projects/bake-transformation/tracker.md`
- `docs/projects/bake-transformation/sprints/12-audio-pipeline-tcf.md`
- `docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md`

**Evidence command outputs**:

```text
$ rg -n "^## [0-9]+\\. Header|^## [0-9]+\\. Demand|^## [0-9]+\\. Scope|^## [0-9]+\\. References|^## [0-9]+\\. Success Exit Criteria|^## [0-9]+\\. Stop Exit Criteria|^## [0-9]+\\. Iteration Bound|^## [0-9]+\\. Required Evidence Format|^## [0-9]+\\. Iteration Ledger|^## [0-9]+\\. Closeout" docs/projects/bake-transformation/sprints/12-audio-pipeline-tcf.md
3:## 1. Header
14:## 2. Demand
24:## 3. Scope
46:## 4. References
59:## 5. Success Exit Criteria
84:## 6. Stop Exit Criteria
96:## 7. Iteration Bound
100:## 8. Required Evidence Format
115:## 9. Iteration Ledger
133:## 10. Closeout
```

```text
$ rg -n "audio-pipeline-bake-contract|docs/bake-contracts/audio-pipeline.md|interface-map authority|supporting reference|docs/cerb/interface-map.md|docs/cerb/audio-management/spec.md|docs/cerb/tingwu-pipeline/spec.md" docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md
1:# Sprint Contract: 13-audio-pipeline-bake-contract
5:- **Slug**: audio-pipeline-bake-contract
20:contract for the audio pipeline, syncs interface-map authority, and demotes the
21:scoped audio Cerb docs to supporting reference.
28:- `docs/bake-contracts/audio-pipeline.md`
29:- `docs/cerb/interface-map.md`
30:- `docs/cerb/audio-management/spec.md`
31:- `docs/cerb/tingwu-pipeline/spec.md`
93:3. `docs/cerb/interface-map.md` cites `docs/bake-contracts/audio-pipeline.md`
95:   supporting reference.
```

```text
$ rg -n "Sprint 11|log#|minimal ASR|empty-file|summary fallback|pending|durable|canonical valves|runtime evidence|provider|BAKE gap" docs/core-flow/sim-audio-artifact-chat-flow.md docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md
docs/core-flow/sim-audio-artifact-chat-flow.md:159:## Sprint 11 Delivered-Vs-Target Alignment
docs/core-flow/sim-audio-artifact-chat-flow.md:165:- BAKE gap: successful `log#` scheduler-pipeline drawer ingest currently writes
docs/core-flow/sim-audio-artifact-chat-flow.md:166:  minimal ASR transcript artifacts into the SIM audio namespace.
docs/core-flow/sim-audio-artifact-chat-flow.md:170:- BAKE gap: the manual sync empty-file skip count shape exists, including
docs/core-flow/sim-audio-artifact-chat-flow.md:173:- BAKE gap: pending chat-side completion has durable artifact-history helpers,
docs/core-flow/sim-audio-artifact-chat-flow.md:177:- BAKE gap: canonical valves in this doc remain ahead of delivered telemetry.
docs/core-flow/sim-audio-artifact-chat-flow.md:181:- BAKE gap: runtime evidence remains missing for physical badge manual sync,
docs/cerb/audio-management/spec.md:143:## Sprint 11 Alignment Notes
docs/cerb/audio-management/spec.md:149:- BAKE gap: `log#` / `BadgeAudioPipeline` remains scheduler-owned, but its
docs/cerb/audio-management/spec.md:150:  successful drawer ingest currently persists minimal ASR transcript artifacts.
docs/cerb/audio-management/spec.md:155:- BAKE gap: the empty-file skip count shape exists for manual sync outcome
docs/cerb/audio-management/spec.md:159:- BAKE gap: pending chat-side completion still requires durable artifact
docs/cerb/audio-management/spec.md:163:- BAKE gap: canonical audio valves remain ahead of delivered telemetry.
docs/cerb/audio-management/spec.md:166:- BAKE gap: runtime evidence for badge hardware, BLE reconnect/resume, provider
docs/cerb/tingwu-pipeline/spec.md:39:- when provider summary is absent, fallback copy must be honest degradation
docs/cerb/tingwu-pipeline/spec.md:80:## Sprint 11 Alignment Notes
docs/cerb/tingwu-pipeline/spec.md:85:- BAKE gap: delivered summary fallback copy can currently imply a summary exists
docs/cerb/tingwu-pipeline/spec.md:89:- BAKE gap: live provider submit, polling, result-link download, and optional
docs/cerb/tingwu-pipeline/spec.md:92:- BAKE gap: the `log#` scheduler-pipeline drawer ingest path writes minimal ASR
```

```text
$ rg -n "\| 12 \| audio-pipeline-tcf \| done|\| 13 \| audio-pipeline-bake-contract \| authored|audio-pipeline \| base-runtime-active .* tcf-written" docs/projects/bake-transformation/tracker.md
36:| 12 | audio-pipeline-tcf | done | Aligned audio target/supporting docs with Sprint 11 gaps and authored the audio pipeline BAKE contract sprint. | [sprints/12-audio-pipeline-tcf.md](sprints/12-audio-pipeline-tcf.md) |
37:| 13 | audio-pipeline-bake-contract | authored | Write the audio-pipeline BAKE contract, sync interface-map authority, and demote scoped audio Cerb docs to supporting reference. | [sprints/13-audio-pipeline-bake-contract.md](sprints/13-audio-pipeline-bake-contract.md) |
47:| audio-pipeline | base-runtime-active | `docs/core-flow/sim-audio-artifact-chat-flow.md` | high | tier-2 | tcf-written |
```

```text
$ git diff --check -- docs/core-flow/sim-audio-artifact-chat-flow.md docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/12-audio-pipeline-tcf.md docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md
# no output
```

```text
$ git diff --stat -- docs/core-flow/sim-audio-artifact-chat-flow.md docs/cerb/audio-management/spec.md docs/cerb/tingwu-pipeline/spec.md docs/projects/bake-transformation/tracker.md docs/projects/bake-transformation/sprints/12-audio-pipeline-tcf.md docs/projects/bake-transformation/sprints/13-audio-pipeline-bake-contract.md
 docs/cerb/audio-management/spec.md                 |  28 ++++
 docs/cerb/tingwu-pipeline/spec.md                  |  19 +++
 docs/core-flow/sim-audio-artifact-chat-flow.md     |  27 ++++
 .../sprints/12-audio-pipeline-tcf.md               | 230 +++++++++++++++++++++
 .../sprints/13-audio-pipeline-bake-contract.md     | 156 +++++++++++++++++++++
 docs/projects/bake-transformation/tracker.md       |   4 +-
 6 files changed, 463 insertions(+), 1 deletion(-)
```

**Runtime evidence**: not run by sprint design. Installed-device, BLE,
provider-network, and live Tingwu behavior remain unproven.

**Lesson proposals**: none.

**CHANGELOG line**: none.
