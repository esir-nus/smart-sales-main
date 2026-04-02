# Base Runtime Truth-Lock Execution Brief

**Status:** L1 Accepted  
**Date:** 2026-03-31  
**Slice:** 5  
**Mission:** Lock one canonical non-Mono product truth in docs and guardrails  
**Primary Tracker:** `docs/plans/tracker.md`  
**North Star:** `docs/specs/base-runtime-unification.md`  
**Current Reading Priority:** Historical execution brief for an accepted docs-and-guardrail slice; use the active truth docs below before historical SIM-family context.  
**Validation Report:** `docs/reports/tests/L1-20260331-base-runtime-truth-lock.md`

---

## 1. Purpose

This slice stops future SIM/full planning drift at the source of truth layer.

It does that by locking one canonical rule:

- non-Mono work belongs to one shared base runtime
- Mono is the only lawful deeper divergence layer
- real SIM-owned standalone/runtime boundaries may still remain in implementation, but they do not create a second non-Mono product truth

This is a docs-and-guardrail slice only. It does not change runtime behavior.

---

## 2. Slice Law

This slice may do:

- sync the authoritative unification docs
- sync the leading SIM baseline docs so they preserve real standalone boundaries without implying a second product truth
- sync shared UI contract wording
- add one focused doc guardrail test
- update tracker/campaign docs for the accepted truth-lock landing

This slice must **not** do:

- runtime behavior changes
- repo-wide renaming
- onboarding cleanup
- broad whole-tree wording scans
- new structural teardown just because large files still exist

---

## 3. Delivered Truth Lock

The landed truth lock does all of the following:

- strengthens `docs/specs/base-runtime-unification.md` so standalone SIM boundaries are explicitly not a second non-Mono product truth
- keeps `docs/cerb/interface-map.md` aligned on the base-runtime vs Mono rule
- updates `docs/specs/prism-ui-ux-contract.md` so SIM-specific shards may remain real implementation boundaries without becoming a second UI/product truth
- syncs the SIM concept, mental model, mission tracker, and leading shell/scheduler/audio shards so they remain valid as the current base-runtime baseline
- adds `BaseRuntimeTruthLockGuardrailTest` as a focused JVM anti-drift guardrail for the authoritative docs and SIM baseline framing

---

## 4. Verification Status

Focused acceptance verification:

- `./gradlew --no-build-cache :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.BaseRuntimeTruthLockGuardrailTest`
- `./gradlew --no-build-cache :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`
- `./gradlew --no-build-cache :app-core:compileDebugUnitTestKotlin`

All listed commands must pass.

---

## 5. Acceptance Bar

This slice is complete only when:

- the authoritative docs state one canonical non-Mono product truth
- the SIM-led docs still describe real standalone/runtime boundaries without implying a second product line
- the new guardrail test passes
- tracker/campaign docs record the truth-lock landing

---

## 6. Related Documents

Current active truth:

- `docs/specs/base-runtime-unification.md`
- `docs/plans/base-runtime-unification-campaign.md`
- `docs/plans/tracker.md`
- `docs/specs/prism-ui-ux-contract.md`

Historical and deprecated context only; not current governing truth:

- `docs/to-cerb/sim-standalone-prototype/concept.md`
- `docs/to-cerb/sim-standalone-prototype/mental-model.md`
- `docs/plans/sim-tracker.md`
- `docs/cerb/sim-shell/spec.md`
- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/sim-audio-chat/spec.md`
