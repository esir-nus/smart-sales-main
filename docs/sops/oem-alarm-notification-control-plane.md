# OEM Alarm / Notification Control Plane

> **Purpose**: Management document for OEM-specific reminder hardening across Xiaomi / HyperOS and Huawei / Honor / HarmonyOS, with the current Android alarm stack as the delivery path.
> **Status**: Active SOP
> **Last Updated**: 2026-04-03
> **Owning Spec**: `docs/cerb/notifications/spec.md`
> **Fast Operator Checklist**: `docs/sops/oem-alarm-notification-checklist.md`
> **Implementation Plan**: `docs/plans/oem-alarm-hardening-plan.md`

---

## 1. Why This Doc Exists

The repo already has a fast operator checklist for reminder failures, but the current OEM work now needs one higher-level document that manages:

- which gate belongs to Android versus the OEM
- which gate is already handled in code versus still only documented
- which gaps remain open for Xiaomi / HyperOS and Huawei / Honor
- which verification evidence is still missing before the reminder stack can be treated as closed

Use this document as the management surface.
Use the checklist as the fast triage surface.

---

## 2. Control Plane Map

Use these docs in this order:

1. `docs/cerb/notifications/spec.md`
2. this control-plane doc
3. `docs/sops/oem-alarm-notification-checklist.md`
4. `docs/reference/hyperos-platform-guide.md`
5. `docs/reference/harmonyos-platform-guide.md`
6. relevant device test or L3 reports

Code surfaces currently involved:

- `app-core/src/main/java/com/smartsales/prism/data/scheduler/RealAlarmScheduler.kt`
- `app-core/src/main/java/com/smartsales/prism/data/scheduler/TaskReminderReceiver.kt`
- `app-core/src/main/java/com/smartsales/prism/data/notification/OemCompat.kt`
- `app-core/src/main/java/com/smartsales/prism/data/notification/ReminderReliabilityAdvisor.kt`
- `app-core/src/main/java/com/smartsales/prism/ui/alarm/AlarmActivity.kt`

---

## 3. Gate Model

Treat reminder reliability as a layered gate chain, not one permission.

### Universal Android gates

- app notifications enabled
- reminder channel not blocked
- exact alarm permission on Android 12+
- full-screen-intent permission on Android 14+ for screen-off deadline alarms

### Xiaomi / HyperOS-specific gates

- lock-screen display
- floating notification
- background local notification
- background popup page
- auto-start
- battery optimization exemption

### Huawei / Honor / HarmonyOS-specific gates

- launch management set to manual allow
- auto-launch
- secondary launch
- background activity
- lock-screen notifications
- floating / banner notifications
- battery optimization as a secondary branch

---

## 4. Managed Gap Ledger

| ID | Gap | Why It Matters | Current State | Next Owner |
|----|-----|----------------|---------------|------------|
| OEM-001 | Android 14+ full-screen-intent settings recovery | Deadline alarm may degrade silently when screen is off | Dedicated settings route and advisor CTA shipped; locked-device proof still pending | L3 device validation report |
| OEM-002 | Xiaomi / HyperOS background popup page management | `AlarmActivity` can still be blocked from background launch even when other Xiaomi gates look healthy | checklist and runtime guidance shipped; popup state is still guidance-only rather than directly detectable | L3 device validation report |
| OEM-003 | Xiaomi CTA granularity | current OEM guidance clusters several Xiaomi display gates together | runtime copy now separates Android full-screen and HyperOS popup guidance more clearly; no direct popup detection yet | `ReminderReliabilityAdvisor.kt` |
| OEM-004 | Huawei / Honor locked-screen deadline evidence | current guidance is doc-backed but not fully device-proven | open | L3 device validation report |

Keep this table current when an OEM gate changes from documented-only to implemented, or from implemented to verified.

---

## 5. Xiaomi / HyperOS Management Branch

### Required management reading

- `docs/reference/hyperos-platform-guide.md`
- `docs/sops/oem-alarm-notification-checklist.md`

### Screen-off deadline alarm law

For Xiaomi / HyperOS, a successful screen-off deadline alarm depends on all of these being understood separately:

- Android app notifications are enabled
- exact alarm is enabled
- Android 14+ full-screen-intent permission is enabled
- lock-screen display is enabled
- background popup page is not blocked
- background local notification is not blocked
- auto-start and battery behavior do not kill the app before delivery

Do not treat any one of these as a substitute for the others.

### Current repo position

- lock-screen display detection exists
- floating notification detection exists
- background local notification detection exists
- `setAlarmClock()` is already used for the `DEADLINE` tier
- real Xiaomi device evidence already proves `AlarmActivity` launch on a locked device

### Current repo gap

- popup management is still guidance-driven rather than directly detectable through public Android APIs
- locked-screen validation still needs to confirm whether current HyperOS builds show the alarm surface or only launch the activity

---

## 6. Huawei / Honor / HarmonyOS Management Branch

### Required management reading

- `docs/reference/harmonyos-platform-guide.md`
- `docs/sops/oem-alarm-notification-checklist.md`

### Screen-off deadline alarm law

For Huawei / Honor, the first assumption should be launch-management failure, not Xiaomi-style hidden AppOps failure.

The current Android-app strategy remains:

- `AlarmManager`
- `setAlarmClock()` for the `DEADLINE` tier
- manual app-launch management allow
- lock-screen and floating notification enablement

This repo does not currently ship native HarmonyOS reminder APIs.

### Current repo position

- Huawei / Honor has dedicated auto-start target ordering
- the reminder advisor already prioritizes app notifications, then exact alarm, then Huawei launch management
- the docs already state that battery optimization is secondary to launch management

### Current repo gap

- there is still no fresh Huawei / Honor locked-screen deadline validation report comparable to the current Xiaomi evidence

---

## 7. Validation Standard

For OEM reminder work, the following evidence order is mandatory:

1. `adb logcat`
2. notification-manager state such as `dumpsys notification --noredact`
3. user-visible locked-screen or unlocked-screen capture

Minimum locked-screen deadline evidence should show:

- the reminder receiver fired
- whether `fullScreenIntent` was attached
- whether `AlarmActivity` `onCreate` executed
- whether the user could actually see the alarm surface when the screen was off

If a report only proves notification-object creation but not user-visible behavior, mark the branch as partially verified rather than closed.

---

## 8. Immediate Follow-Up Order

1. rerun Xiaomi locked-screen deadline validation after the guidance changes
2. capture a Huawei / Honor locked-screen deadline validation report
3. record whether `AlarmActivity` only launches or is actually human-visible with the screen off
4. resync the advisor copy if device evidence shows a newer OEM gate split

---

## 9. Maintenance Rule

When any of these change, update this document in the same session:

- Android full-screen-intent policy handling
- Xiaomi / HyperOS gate coverage
- Huawei / Honor launch-management or lock-screen guidance
- OEM reminder L2 or L3 validation evidence

This doc should stay short and managerial.
Detailed click-path diagnosis belongs in the checklist.
