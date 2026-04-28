# Project: bake-transformation

## Objective

Establish verified BAKE pipeline contracts for all active base-runtime domains
in the Smart Sales codebase. For each domain: produce a delivered-behavior map
(what the battle-tested code actually does), an optimized target core-flow (the
post-transformation behavioral north star), code transformation sprints that
close the gap, and a verified BAKE contract as the durable implementation record.

Code quality objectives — dead code removal, simplification, happy-path
correction, rewrite vs refactor — emerge naturally through the target core-flow
and code sprint work. They are not a separate project.

Northstar doctrine: `docs/specs/bake-protocol.md`

## Status

open

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | triage | done | Classified BAKE runtime domains, confirmed `docs/bake-contracts/`, and authored cluster-1 DBM/TCF contracts. | [sprints/01-triage.md](sprints/01-triage.md) |
| 02 | connectivity-dbm | done | Delivered behavior map created for connectivity-bridge plus badge-session, with HTTP readiness, active-device fencing, Wi-Fi repair, telemetry, and hardware-evidence gaps recorded. | [sprints/02-connectivity-dbm.md](sprints/02-connectivity-dbm.md) |
| 03 | connectivity-tcf | done | Added base-runtime scope metadata and delivered-vs-target gap notes to connectivity/session core flows, then authored the first BAKE contract sprint. | [sprints/03-connectivity-tcf.md](sprints/03-connectivity-tcf.md) |
| 04 | connectivity-bake-contract | done | Wrote the connectivity-badge-session BAKE contract, synced interface-map authority, and demoted connectivity Cerb docs to supporting reference. | [sprints/04-connectivity-bake-contract.md](sprints/04-connectivity-bake-contract.md) |

## Domain Backlog

| Domain | Runtime | Existing doc | Complexity | Priority tier | Status |
|--------|---------|--------------|------------|---------------|--------|
| connectivity-bridge | base-runtime-active | `docs/core-flow/badge-connectivity-lifecycle.md` | high | tier-1 | bake-contract-written |
| badge-session | base-runtime-active | `docs/core-flow/badge-session-lifecycle.md` | high | tier-1 | bake-contract-written |
| scheduler-path-a | base-runtime-active | `docs/core-flow/scheduler-fast-track-flow.md` | high | tier-1 | triaged |
| shell-routing | base-runtime-active | `docs/core-flow/sim-shell-routing-flow.md` | high | tier-1 | triaged |
| audio-pipeline | base-runtime-active | `docs/core-flow/sim-audio-artifact-chat-flow.md` | high | tier-2 | triaged |
| agent-chat-pipeline | partially-shipped | `docs/cerb/sim-audio-chat/spec.md` | high | tier-2 | triaged |
| entity-resolution | base-runtime-active | `docs/cerb/entity-writer/spec.md` | high | tier-2 | triaged |
| onboarding | base-runtime-active | `docs/cerb/onboarding-interaction/spec.md` | medium | tier-2 | triaged |
| typed-mutation | partially-shipped | `docs/core-flow/system-typed-mutation-flow.md` | high | tier-2 | triaged |
| session-continuity | partially-shipped | `docs/core-flow/system-session-memory-flow.md` | high | tier-2 | triaged |
| reinforcement-habit | partially-shipped | `docs/core-flow/system-reinforcement-write-through-flow.md` | medium | tier-2 | triaged |
| asr-service | base-runtime-active | `docs/cerb/asr-service/spec.md` | low | tier-3 | triaged |
| oss-service | base-runtime-active | `docs/cerb/oss-service/spec.md` | low | tier-3 | triaged |
| tingwu-pipeline | base-runtime-active | `docs/cerb/tingwu-pipeline/spec.md` | medium | tier-3 | triaged |
| notification-service | base-runtime-active | `docs/cerb/notifications/spec.md` | low | tier-3 | triaged |
| pipeline-telemetry | base-runtime-active | `docs/cerb/pipeline-telemetry/spec.md` | low | tier-3 | triaged |
| device-pairing | base-runtime-active | `docs/cerb/device-pairing/spec.md` | medium | tier-3 | triaged |
| conflict-resolver | base-runtime-active | `docs/cerb/conflict-resolver/spec.md` | medium | tier-3 | triaged |
| ux-surface-governance | base-runtime-active | `docs/core-flow/base-runtime-ux-surface-governance-flow.md` | medium | tier-3 | triaged |
| scheduler-path-b | mono-deferred | `docs/core-flow/scheduler-memory-highway-flow.md` | high | deferred | deferred |
| plugin-gateway | mono-deferred | `docs/core-flow/system-plugin-gateway-flow.md` | high | deferred | deferred |
| memory-crm | mono-deferred | `docs/core-flow/system-query-assembly-flow.md` | high | deferred | deferred |

## Decisions

- Canonical BAKE contract location is `docs/bake-contracts/`; create the
  directory in the first sprint that writes an actual BAKE contract.
- Sprint 01 registered the path here instead of `docs/specs/bake-protocol.md`
  because the sprint write scope did not include the northstar doc.

## Cross-Project Notes

- badge-session-lifecycle sprint 08 status remains `blocked` (factual closeout
  stands: local gates passed, adb/logcat evidence unavailable). BAKE
  transformation will absorb the unresolved connectivity-contract and evidence
  follow-up when the connectivity-bridge and badge-session domains are reached
  in tier-1 cluster work.

## Expected Scope

Project expected to exceed six sprints. Ongoing-justification entry required
when sprint 07 is authored, per `docs/specs/project-structure.md`.
