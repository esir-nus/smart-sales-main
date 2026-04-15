# L3 SIM Wave 1 Shell Acceptance

**Date:** 2026-03-19  
**Mission:** SIM standalone prototype  
**Wave:** 1  
**Scope:** Standalone shell entry, shell routing, preserved shell practices, smart-surface suppression, and history action regression closeout

---

## 1. Acceptance Target

Behavioral authority:

- `docs/core-flow/sim-shell-routing-flow.md`
- `docs/cerb/sim-shell/spec.md`

Wave objective:

- prove SIM boots through `SimMainActivity`
- prove normal SIM use does not fall back into the smart shell
- prove preserved shell practices work in simplified SIM form
- prove smart-only shell surfaces are absent in normal SIM operation

---

## 2. Evidence

### Build / Install

- `./gradlew :app-core:compileDebugKotlin --no-daemon` -> passed
- `./gradlew :app-core:installDebug --no-daemon` -> passed

### Runtime Entry Proof

- launched `adb shell am start -S -W -n com.smartsales.prism/com.smartsales.prism.SimMainActivity`
- confirmed foreground activity via `dumpsys activity`:
  - `topResumedActivity = com.smartsales.prism/.SimMainActivity`
  - `mCurrentFocus = com.smartsales.prism/com.smartsales.prism.SimMainActivity`

### On-Device Route Pass

Passed on physical device:

- SIM entry opens correctly
- no debug HUD button
- no mascot overlay
- scheduler drawer route works
- SIM audio drawer route works
- SIM sample / `Ask AI` handoff works
- connectivity opens and closes
- settings opens and closes

### Regression Closeout

History actions initially exposed a regression-like blank drawer after reinstall.

Real cause:

- cold-start SIM history had no local sessions
- drawer had no explicit empty-state treatment

Fix applied:

- seed stable SIM Wave 1 local sample sessions
- add explicit history empty-state
- add visible history row actions: `Pin/Unpin`, `Rename`, `Delete`

Retest result:

- history cards visible
- `Pin/Unpin` visible
- `Rename` visible
- `Delete` visible
- user reported all green

---

## 3. Acceptance Verdict

**Verdict:** Accepted

Wave 1 is complete for its intended scope:

- standalone shell boundary is real
- preserved shell practices are operational in SIM form
- smart-only shell surfaces are not part of normal SIM use

---

## 4. Residual Notes

- SIM still shares the same Android package/app process as the smart app in Wave 1; isolation is activity/composition-root based, not separate-application-ID based
- some support surfaces still reuse existing shared viewmodel-backed UI seams; this is acceptable for Wave 1 shell proof, but later waves must keep guarding contamination boundaries
