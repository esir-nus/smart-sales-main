# God Wave 0 Execution Brief

**Status:** Implemented
**Date:** 2026-03-24
**Wave:** 0
**Mission:** God-file refactor-first campaign preflight
**Primary Tracker:** `docs/projects/god-file-cleanup/tracker.md`
**Structure Law:** `docs/specs/code-structure-contract.md`

---

## 1. Purpose

Wave 0 is the docs-and-guardrail preflight wave for the god-file cleanup campaign.

It exists to make the later cleanup waves executable without improvisation.

Wave 0 does **not** refactor production trunk files yet.

---

## 2. Wave 0 Law

Wave 0 may do:

- docs formalization
- exception seeding
- freeze-rule definition
- structural-test design for Wave 1A

Wave 0 must **not** do:

- production-file decomposition
- preview extraction
- UI subtree rewrite
- ViewModel lane extraction

---

## 3. Immediate Pilot Files

Current audit snapshot on 2026-03-24:

- `app-core/src/main/java/com/smartsales/prism/ui/AgentIntelligenceScreen.kt` — about `1861 LOC`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimShell.kt` — about `1092 LOC`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAgentViewModel.kt` — about `1503 LOC`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerViewModel.kt` — about `1243 LOC`

These files are far above the current transitional budgets and therefore require explicit temporary exceptions before Wave 1A can enforce anything honestly.

---

## 4. Freeze Rule

While the active cleanup campaign targets the four pilot files:

- no major new UI trunk transplant may land into them
- no feature expansion may use them as the default landing zone
- only bug fixes, safety fixes, or doc-aligned hot fixes may touch them
- any hotfix touching one of these files must not widen its role mix, increase its exception scope, or silently normalize the file as the new target for future work

Leaf-level UI polish may continue only when it avoids these four files.

If a freeze violation is unavoidable, log it in `docs/projects/god-file-cleanup/tracker.md` with:

- reason
- touched file
- follow-up wave
- verification surface

---

## 5. Seeded Exception Policy

Wave 0 seeds the four pilot files as temporary exceptions owned by `Codex`.

Default sunsets:

- `AgentIntelligenceScreen.kt` -> `Wave 1B`
- `SimShell.kt` -> `Wave 1C`
- `SimAgentViewModel.kt` -> `Wave 1D`
- `SimSchedulerViewModel.kt` -> `Wave 1E`

Each exception row must include:

- owner
- sunset wave/date
- required verification pack
- target decomposition

---

## 6. Wave 1A Guardrail Design Target

Wave 1A should add **two** focused JVM structural tests only.

### A. Tracker / Contract Validity Test

Checks:

- `docs/projects/god-file-cleanup/tracker.md` exists
- `docs/specs/code-structure-contract.md` exists
- each pilot exception row includes owner, sunset, required tests, target decomposition, and status

### B. Pilot Structural Budget Test

Checks:

- reads the four pilot source files directly
- measures LOC
- verifies each file is either under budget or has a valid exception row

Wave 1A should **not** yet fail on:

- preview separation
- post-cleanup role purity
- generic repo-wide scanning
- universal role inference

Those stronger checks should be staged only after the corresponding cleanup wave lands.

---

## 7. Wave 0 Exit Bar

Wave 0 is complete only when:

- this brief exists
- the four pilot files have valid exception rows in `docs/projects/god-file-cleanup/tracker.md`
- owner policy is explicit and set to `Codex`
- sunset waves are fixed to `1B / 1C / 1D / 1E`
- required verification packs are listed
- the freeze rule is documented
- Wave 1A has a decision-complete two-test design

---

## 8. Related Documents

- `docs/projects/god-file-cleanup/tracker.md`
- `docs/specs/code-structure-contract.md`
- `docs/plans/tracker.md`
- `docs/projects/ui-campaign/tracker.md`
