# God Wave 3A Execution Brief

**Status:** L1 Accepted
**Date:** 2026-03-24
**Wave:** 3A
**Mission:** `SimAudioDrawer.kt` structural cleanup
**Primary Tracker:** `docs/projects/god-file-cleanup/tracker.md`
**Structure Law:** `docs/specs/code-structure-contract.md`
**Current Reading Priority:** Historical execution reference only; not current source of truth.
**Current Active Truth:** `docs/projects/god-file-cleanup/tracker.md`, `docs/plans/tracker.md`, `docs/specs/code-structure-contract.md`, `docs/core-flow/sim-audio-artifact-chat-flow.md`, `docs/cerb/audio-management/spec.md`, `docs/cerb/audio-management/interface.md`, `docs/cerb/tingwu-pipeline/spec.md`, `docs/cerb/tingwu-pipeline/interface.md`
**Historical Deprecated Context:** `docs/cerb/sim-audio-chat/spec.md`, `docs/cerb/sim-audio-chat/interface.md`
**Validation Report:** `docs/reports/tests/L1-20260324-god-wave3a-sim-audio-drawer.md`

---

## 1. Purpose

Wave 3A is the first reopened UI-safe cleanup wave after the Wave 2 business-logic tranche.

It targets `SimAudioDrawer.kt`, which had remained a large feature-local drawer file even after the SIM audio/chat contract stabilized enough to separate host wiring from content, card rendering, and helper/support ownership.

Wave 3A reduces that drawer into a thin public host file without changing the accepted SIM audio browse-vs-select behavior.

---

## 2. Current Active Truth

Wave 3A should now be read against the shared audio and Tingwu docs:

- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/audio-management/interface.md`
- `docs/cerb/tingwu-pipeline/spec.md`
- `docs/cerb/tingwu-pipeline/interface.md`
- `docs/plans/tracker.md`
- `docs/projects/god-file-cleanup/tracker.md`
- `docs/specs/code-structure-contract.md`

Historical deprecated context at the time:

- `docs/cerb/sim-audio-chat/spec.md`
- `docs/cerb/sim-audio-chat/interface.md`

Wave 3A must preserve the accepted browse-vs-select mode split, artifact display flow, and current drawer-to-chat handoff behavior.

---

## 3. Wave 3A Law

Wave 3A may do:

- public-host reduction for `SimAudioDrawer.kt`
- extraction of drawer content/header/debug-action ownership
- extraction of card/artifact/compact-preview ownership
- extraction of SIM audio drawer support helpers and shared presentation constants
- focused structure-test and guardrail updates for the accepted split
- tracker/doc sync for the delivered Wave 3A shape

Wave 3A must **not** do:

- behavior changes that widen or narrow the accepted SIM audio/chat product contract
- drawer-to-chat routing rewrites under the banner of structural cleanup
- new abstractions or DI seams added only to satisfy file-count goals
- unrelated cleanup of `OnboardingScreen.kt` or other deferred UI trunks

---

## 4. Delivered Structure

Wave 3A leaves `app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawer.kt` as the public drawer host file.

Delivered extraction map:

- `SimAudioDrawer.kt`
  - host overlay, animation, lifecycle collection, and top-level callback wiring only (`136 LOC`)
- `SimAudioDrawerContent.kt`
  - title/header actions, list composition, test-import/debug-action sections
- `SimAudioDrawerCard.kt`
  - browse/select card rendering, artifact loading, compact preview rows, swipe prompt
- `SimAudioDrawerSupport.kt`
  - shared colors, selection DTO, transcript/select-copy helpers, transparent-state labels

Wave 3A also:

- keeps the public `SimAudioDrawer(...)` seam source-compatible for current shell callers
- keeps browse-mode sync/header actions and chat-reselect select-mode behavior unchanged
- adds a focused structure regression test for the accepted drawer split
- upgrades the tracker row from deferred debt to an accepted Wave 3A host split

---

## 5. Verification Status

Wave 3A acceptance used focused app-core JVM verification:

- `GodStructureGuardrailTest`
- `SimAudioDrawerStructureTest`
- `SimAudioDrawerViewModelTest`
- `SimShellHandoffTest`
- `./gradlew :app-core:compileDebugUnitTestKotlin`

Executed command:

- `./gradlew :app-core:compileDebugUnitTestKotlin :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.sim.SimAudioDrawerStructureTest --tests com.smartsales.prism.ui.sim.SimAudioDrawerViewModelTest --tests com.smartsales.prism.ui.sim.SimShellHandoffTest --tests com.smartsales.prism.ui.GodStructureGuardrailTest`

---

## 6. Wave 3A Acceptance Bar

Wave 3A is complete only when:

- `SimAudioDrawer.kt` is under the UI host budget
- the tracker row moves from deferred debt to `Accepted`
- focused drawer structure and SIM audio behavior tests stay green
- shell callers remain source-compatible
- docs and validation records reference the delivered Wave 3A split

---

## 7. Related Documents

- `docs/projects/god-file-cleanup/tracker.md`
- `docs/plans/tracker.md`
- `docs/specs/code-structure-contract.md`
- `docs/core-flow/sim-audio-artifact-chat-flow.md`
- `docs/cerb/audio-management/spec.md`
- `docs/cerb/audio-management/interface.md`
- `docs/reports/tests/L1-20260324-god-wave3a-sim-audio-drawer.md`
