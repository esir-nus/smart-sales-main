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
| 01 | triage | authored | Classify all domains, produce ordered sprint backlog, author cluster-1 DBM and TCF contracts. | [sprints/01-triage.md](sprints/01-triage.md) |

## Domain Backlog

> Triage sprint (01) fills and confirms this table. Entries below are seeded
> from the plan and are non-binding until sprint 01 closes.

| Domain | Runtime | Priority | Status |
|--------|---------|----------|--------|
| connectivity-bridge | base-runtime-active | tier-1 | pending |
| badge-session | base-runtime-active | tier-1 | pending |
| scheduler-path-a | base-runtime-active | tier-1 | pending |
| shell-routing | base-runtime-active | tier-1 | pending |
| audio-pipeline | base-runtime-active | tier-2 | pending |
| agent-chat-pipeline | partially-shipped | tier-2 | pending |
| entity-resolution | base-runtime-active | tier-2 | pending |
| onboarding | base-runtime-active | tier-2 | pending |
| typed-mutation | partially-shipped | tier-2 | pending |
| session-continuity | partially-shipped | tier-2 | pending |
| reinforcement-habit | partially-shipped | tier-2 | pending |
| asr-service | base-runtime-active | tier-3 | pending |
| oss-service | base-runtime-active | tier-3 | pending |
| tingwu-pipeline | base-runtime-active | tier-3 | pending |
| notification-service | base-runtime-active | tier-3 | pending |
| pipeline-telemetry | base-runtime-active | tier-3 | pending |
| device-pairing | base-runtime-active | tier-3 | pending |
| conflict-resolver | base-runtime-active | tier-3 | pending |
| ux-surface-governance | base-runtime-active | tier-3 | pending |
| scheduler-path-b | mono-deferred | deferred | deferred |
| plugin-gateway | mono-deferred | deferred | deferred |
| memory-crm | mono-deferred | deferred | deferred |

## Decisions

- bake-contracts/ location: to be confirmed by sprint 01 and registered in
  `docs/specs/bake-protocol.md` before the first BAKE contract is written.

## Cross-Project Notes

- badge-session-lifecycle sprint 08 status remains `blocked` (factual closeout
  stands: local gates passed, adb/logcat evidence unavailable). BAKE
  transformation will absorb the unresolved connectivity-contract and evidence
  follow-up when the connectivity-bridge and badge-session domains are reached
  in tier-1 cluster work.

## Expected Scope

Project expected to exceed six sprints. Ongoing-justification entry required
when sprint 07 is authored, per `docs/specs/project-structure.md`.
