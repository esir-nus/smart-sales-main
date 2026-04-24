# God Wave 1A Execution Brief

**Status:** L1 Accepted
**Date:** 2026-03-24
**Wave:** 1A
**Mission:** God-file shallow structural guardrails
**Primary Tracker:** `docs/projects/god-file-cleanup/tracker.md`
**Structure Law:** `docs/specs/code-structure-contract.md`
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave1a-guardrails.md`

---

## 1. Purpose

Wave 1A is the first executable guardrail wave for the god-file cleanup campaign.

It adds shallow structural enforcement so later cleanup waves can fail honestly when tracker state or exception coverage drifts.

Wave 1A does **not** refactor production trunk files yet.

---

## 2. Wave 1A Law

Wave 1A may do:

- docs sync for the guardrail wave
- tracker/contract validity enforcement
- pilot structural budget enforcement

Wave 1A must **not** do:

- production-file decomposition
- preview separation enforcement
- repo-wide structural scanning
- post-cleanup role-purity enforcement
- generic role inference beyond the four pilot files

---

## 3. Delivered Guardrails

Wave 1A delivers one JVM guardrail test class:

- `app-core/src/test/java/com/smartsales/prism/ui/GodStructureGuardrailTest.kt`

That class contains exactly two checks:

1. **Tracker / Contract Validity**
   - `docs/projects/god-file-cleanup/tracker.md` exists
   - `docs/specs/code-structure-contract.md` exists
   - each pilot exception row includes target decomposition, owner, sunset, required tests, and status
   - each pilot row remains explicitly tracked as `Exception`

2. **Pilot Structural Budget**
   - reads the four pilot source files directly
   - measures current LOC from source
   - verifies each file is either under budget or covered by a valid exception row

---

## 4. Active Pilot Files

Wave 1A guardrails apply only to:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt`

No other tracked god-file candidates are enforced in this wave.

---

## 5. Verification Status

Focused verification is now green for the delivered Wave 1A guardrail slice.

Executed commands:

- `./gradlew :domain:scheduler:compileKotlin`
- `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`

---

## 6. Wave 1A Acceptance Bar

Wave 1A is complete only when:

- this brief exists
- `docs/projects/god-file-cleanup/tracker.md` records the delivered Wave 1A guardrail anchor
- the focused guardrail JVM test passes
- the L1 validation report records the exact command and verdict
- the four pilot files remain temporary exceptions until their cleanup waves land

---

## 7. Related Documents

- `docs/projects/god-file-cleanup/tracker.md`
- `docs/specs/code-structure-contract.md`
- `docs/plans/tracker.md`
- `docs/reports/tests/L1-20260324-god-wave1a-guardrails.md`
