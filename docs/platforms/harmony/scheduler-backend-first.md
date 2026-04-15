# Harmony Scheduler Backend-First Phase 1

> **Purpose**: Define the first Harmony scheduler slice as a backend/dataflow verification lane rather than a UI-parity build.
> **Status**: Active backend-first phase
> **Date**: 2026-04-10
> **Primary Laws**:
> - `docs/platforms/harmony/native-development-framework.md`
> - `docs/sops/harmony-operator-runbook.md`
> - `docs/specs/platform-governance.md`
> **Primary Tracker**:
> - `docs/plans/harmony-tracker.md`
> **Shared Truth Sources**:
> - `docs/core-flow/scheduler-fast-track-flow.md`
> - `docs/cerb/scheduler-path-a-spine/spec.md`
> - `docs/cerb/scheduler-path-a-spine/interface.md`
> - `docs/cerb-ui/scheduler/contract.md`
> - `docs/cerb/notifications/spec.md`

---

## 1. Phase Role

This phase does **not** try to ship the full Harmony scheduler experience.

It exists to prove that Harmony scheduler dataflow is no longer a black box.

Current engineering stance:

- preserve shared scheduler semantics
- rewrite platform-owned seams natively
- verify dataflow before broad UI translation
- keep the visible experiment limited to a small Path A mini lab
- do not imply reminder or background parity before that adapter work is proven

---

## 2. Supported in Phase 1

Phase 1 supports one operator-only backend verification sandbox inside the Harmony-owned root.

Current visible experiment scope:

- operator-seeded `Uni-A` exact-create verification
- operator-seeded `Uni-B` vague-create verification
- local persistence of backend verification tasks and trace events
- Harmony-local telemetry for ingress, classification, Path A commit, and local snapshot persistence
- owner-chain and ingress-owner visibility in the operator surface

Current hidden internal scaffold verification scope:

- collapsed internal-only trigger path for `CONFLICT_CREATE`
- collapsed internal-only trigger path for `RESCHEDULE`
- collapsed internal-only trigger path for `NULL_FAIL`
- the `RESCHEDULE` scaffold must carry an explicit target plus a new exact time and must resolve across all active exact Harmony scheduler tasks without newest-task bias
- these triggers exist only to complete backend Path A verification without widening the default visible Harmony scheduler surface

This is backend verification support, not user-facing scheduler support.

---

## 3. Deferred or Explicitly Unsupported

Phase 1 does not claim:

- user-facing scheduler parity
- scheduler drawer parity
- dynamic-island scheduler parity
- reminder or alarm parity
- background execution parity
- onboarding scheduler promises or quick-start parity
- badge-driven scheduler ingress
- natural-language semantic completeness beyond the bounded mini-lab

Additional note:

- conflict-visible create, reschedule replacement-path, and null-input safe-fail cases are allowed only in the collapsed internal scaffold verification section
- they are backend-proof controls, not widened product UI scope
- they must not be presented as scheduler parity or default visible Harmony capability

If any broader scheduler promise looks present in the UI, that is a control-plane failure.

---

## 4. Critical Dataflow Joints

Harmony scheduler telemetry for the current mini-lab must exist at these joints:

1. ingress accepted
2. command classification chosen
3. Path A commit checkpoint
4. local snapshot persistence

Operator visibility must also show:

- ingress owner
- owner chain
- last committed Path A record
- persisted task snapshot

Telemetry rule:

- log boundary events and payload shape summaries
- do not dump huge payloads or pretend telemetry itself is the product

---

## 5. Phase 1 Verification Matrix

The current backend completion matrix is:

- default visible mini-lab:
  - `EXACT_CREATE`
  - `VAGUE_CREATE`
- hidden internal scaffold verification:
  - `CONFLICT_CREATE`
  - `RESCHEDULE`
  - `NULL_FAIL`

Acceptance for this mini-lab means:

- each supported backend scenario reaches the expected telemetry conclusion
- mutation scenarios persist an honest local task snapshot
- the operator surface shows ingress owner, owner chain, and the latest Path A commit record
- vague create does not fabricate an exact due time
- reschedule only commits when the operator scenario carries an explicit target plus a new exact time and the Harmony resolver finds exactly one active exact task globally
- null safe-fail produces no task mutation and leaves the latest Path A commit unchanged

---

## 6. Cerb Interface Reuse Rule

Use Cerb interfaces semantically, not structurally.

For this phase:

- scheduler core-flow and Path A contracts define what the scheduler means
- Harmony backend scaffold may borrow contract meanings such as `PathACommitted`, `unifiedId` / root identity, and safe-fail semantics
- Harmony must not blindly clone Android repository or notification seams

Special caution:

- `docs/cerb/notifications/spec.md` is a platform-adapter area, so reminder/alarm work remains deferred until a Harmony-native adapter is defined honestly

---

## 7. Current Scaffold Files

The current backend-first scaffold inside the Harmony root should stay bounded to:

- Harmony scheduler models
- Harmony scheduler telemetry
- Harmony scheduler local store
- Harmony scheduler repository
- Harmony scheduler ingress coordinator
- a minimal operator surface to run the mini-lab and inspect traces
- a collapsed internal scaffold verification section for backend-only completion work

That surface is an operator tool, not the beginning of fake scheduler parity UI.
