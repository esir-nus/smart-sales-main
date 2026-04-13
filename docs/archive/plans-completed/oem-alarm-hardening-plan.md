# OEM Alarm Hardening Plan

> **Status**: Runtime gate slice implemented; validation follow-up pending
> **Date**: 2026-04-03
> **Owning Spec**: `docs/cerb/notifications/spec.md`
> **OEM Control Plane**: `docs/sops/oem-alarm-notification-control-plane.md`
> **Operator Checklist**: `docs/sops/oem-alarm-notification-checklist.md`
> **Supporting References**:
> - `docs/reference/hyperos-platform-guide.md`
> - `docs/reference/harmonyos-platform-guide.md`
> - `docs/reports/tests/L3-20260322-sim-wave7-reminder-visual-validation.md`

---

## 1. Goal

Close the remaining OEM reminder hardening gaps for screen-off and lock-screen deadline alarms without changing the current single-APK Android strategy.

This plan is specifically about:

- Android 14+ full-screen alarm permission recovery
- Xiaomi / HyperOS screen-off deadline presentation
- Huawei / Honor / HarmonyOS launch-management-first reliability
- evidence-driven validation for real locked-device behavior

---

## 2. Review Outcome That Triggered This Plan

The 2026-04-03 review found that the current reminder stack is close, but not fully closed:

- Android 14+ `USE_FULL_SCREEN_INTENT` denial is detected in code, but there is no dedicated user recovery route yet
- Xiaomi / HyperOS treats background popup as a separate gate from lock-screen display, floating notifications, and background local notifications
- the current Xiaomi operator checklist does not explicitly manage that background-popup gate
- Huawei / Honor guidance is doc-backed, but locked-screen full-screen reminder proof is still weaker than the current Xiaomi proof

This plan keeps `docs/cerb/notifications/spec.md` as the behavioral SOT and turns the remaining follow-up into explicit tracked work.

---

## 3. Deliverables

This plan now delivers:

- this implementation plan
- `docs/sops/oem-alarm-notification-control-plane.md` as the management document
- synchronized references from the notification spec and OEM checklist
- Android 14+ full-screen-intent settings routing in the runtime advisor stack
- Xiaomi / HyperOS runtime guidance that calls out background popup as a distinct screen-off gate

Remaining follow-up slices must deliver:

- refreshed Huawei / Honor validation evidence for the locked-screen deadline path

---

## 4. Phase Map

### Phase 1: Doc lock

- state the missing Android and OEM gates explicitly
- separate the fast operator checklist from the broader OEM management doc
- keep Xiaomi and Huawei branches distinct rather than collapsing them into one generic Chinese-OEM warning

### Phase 2: Runtime gate completion

- add an Android 14+ full-screen-intent recovery branch using the app-level full-screen settings page
- extend Xiaomi / HyperOS guidance so screen-off deadline handling accounts for:
  - lock-screen display
  - floating notification
  - background local notification
  - background popup page
- keep Huawei / Honor guidance launch-management-first and do not invent native Harmony reminder integration

Status on 2026-04-03:

- implemented in `OemCompat.kt`
- implemented in `ReminderReliabilityAdvisor.kt`
- still awaiting locked-device validation evidence

### Phase 3: Validation closure

- rerun Xiaomi locked-screen deadline validation after the runtime guidance changes land
- add at least one Huawei / Honor locked-screen deadline validation pass with `adb logcat`
- record whether `AlarmActivity` only launches, or also becomes human-visible, under locked-screen conditions

---

## 5. Managed Gaps

### Gap A: Android 14+ full-screen-intent recovery moved from missing to shipped

Current state:

- manifest permission exists
- runtime checks `canUseFullScreenIntent()`
- runtime degrades to non-full-screen delivery plus wake-lock fallback
- dedicated app-level full-screen-intent settings routing now exists
- reminder-advisor copy now explains why this setting matters for screen-off deadline alarms

Remaining gap:

- locked-screen device validation is still required to prove the shipped recovery path on real Android 14+ hardware

### Gap B: Xiaomi / HyperOS screen-off branch now manages background-popup wording

Current state:

- the repo manages lock-screen display, floating notification, and background local notification risk
- runtime guidance now calls out HyperOS background popup as a separate screen-off branch
- real Xiaomi deadline validation already proves `AlarmActivity` launch on one device

Remaining gap:

- fresh validation should confirm that the new guidance matches what users see on current HyperOS builds

### Gap C: Huawei / Honor validation is doc-backed but not evidence-closed

Current state:

- the repo correctly treats Huawei / Honor as launch-management-first
- auto-start targets and guidance already exist

Missing:

- a current locked-screen deadline validation report for Huawei / Honor
- stronger evidence around what is visible to the user when the screen is off

---

## 6. Acceptance

This plan is ready to close only when all of these are true:

- Android 14+ devices have a dedicated full-screen-intent recovery route
- Xiaomi / HyperOS guidance explicitly covers background popup page in addition to the existing visibility gates
- Huawei / Honor guidance remains Android-app-based and launch-management-first
- at least one Xiaomi and one Huawei / Honor locked-screen deadline validation report exist with `adb logcat` evidence
- the OEM management doc, checklist, and notification spec do not contradict each other

---

## 7. Non-Goals

- no native HarmonyOS reminder integration in this slice
- no multi-APK or OEM flavor split
- no generic foreground-service expansion unless later device evidence proves the current hardening stack is insufficient
