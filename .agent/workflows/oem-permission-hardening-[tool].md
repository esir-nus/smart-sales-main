---
description: Diagnose and fix OEM-specific permission issues — doc alignment check + troubleshooting from user description
---

# OEM Permission Hardening

Diagnose and resolve OEM-specific permission/notification issues. Covers doc alignment, code audit, and fix planning.

**Invocation**: `/oem-permission-hardening xiaomi — notifications not showing on lock screen`

---

## Phase 1: Context Intake

Parse the user's invocation to extract:

| Field | Source |
|-------|--------|
| **OEM** | First word after slash command (xiaomi, huawei, oppo, vivo, samsung, etc.) |
| **Symptom** | Rest of the message — what's broken or needed |

If OEM is unclear, ask. If symptom is vague, ask for specifics:
- What permission/feature is affected?
- What does the user observe? (notification silent, alarm late, app killed, etc.)
- Is this a new feature or a regression?

---

## Phase 2: Evidence Gathering

// turbo-all

### 2a. Read the SOT docs

```bash
# Notification spec (primary SOT for permission hardening)
cat docs/cerb/notifications/spec.md

# Notification interface
cat docs/cerb/notifications/interface.md
```

### 2b. Read the OEM code

```bash
# OemCompat — all OEM detection + permission utilities
cat app-prism/src/main/java/com/smartsales/prism/data/notification/OemCompat.kt

# Onboarding — where OEM-specific permission steps live
cat app-prism/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingScreen.kt
cat app-prism/src/main/java/com/smartsales/prism/ui/onboarding/OnboardingStep.kt
cat app-prism/src/main/java/com/smartsales/prism/data/onboarding/OnboardingGate.kt

# AlarmScheduler — where exact alarm permission is checked
cat app-prism/src/main/java/com/smartsales/prism/data/scheduler/RealAlarmScheduler.kt
```

### 2c. Check AndroidManifest

```bash
grep -n "uses-permission\|receiver\|service\|BOOT_COMPLETED\|SCHEDULE_EXACT_ALARM\|POST_NOTIFICATIONS\|FOREGROUND_SERVICE\|WAKE_LOCK" app-prism/src/main/AndroidManifest.xml
```

---

## Phase 3: OEM Matrix Check

Cross-reference the user's OEM against known defense layers:

### Defense Layers

| Layer | What | Code Location | OEMs Affected |
|-------|------|---------------|---------------|
| **L1: Battery** | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | `OemCompat.isIgnoringBatteryOptimizations()` | All Chinese OEMs |
| **L2: Auto-Start** | OEM auto-start whitelist | `OemCompat.openAutoStartSettings()` | Xiaomi, Huawei, Oppo, Vivo |
| **L3: Exact Alarm** | `SCHEDULE_EXACT_ALARM` | `OemCompat.needsExactAlarmPermission()` | Android 12+ (all OEMs) |
| **L4: Lock Screen** | MIUI lock screen display | `OemCompat.openLockScreenPermission()` | Xiaomi/HyperOS only |
| **L5: Floating** | MIUI floating notification | `OemCompat.canShowFloatingNotification()` | Xiaomi/HyperOS only |
| **L6: Background** | HyperOS background notification | `OemCompat.canSendBackgroundNotification()` | Xiaomi/HyperOS only |
| **L7: Huawei BG** | Background activity + app launch | Onboarding guidance | Huawei/Honor only |

### OEM Quick Reference

| OEM | Unique Quirks |
|-----|--------------|
| **Xiaomi/HyperOS** | L4+L5+L6 — three hidden AppOps (10016, 10020, 10021). No API to grant, must guide user. |
| **Huawei/Honor** | No programmatic check for BG activity. Must guide to 设置 > 应用启动管理 > 手动管理. |
| **Oppo/Realme** | Auto-start page via ComponentName. Similar to Xiaomi but fewer hidden ops. |
| **Vivo/iQOO** | Auto-start page via ComponentName. Battery optimization aggressive. |
| **Samsung** | Generally AOSP-compliant. Sleeping apps list is the main concern. |

---

## Phase 4: Doc Alignment Audit

Check if the user's symptom is covered by existing docs:

```markdown
### Doc Alignment Check

| Question | Check | Result |
|----------|-------|--------|
| Spec covers this OEM? | grep OEM name in `docs/cerb/notifications/spec.md` | ✅/❌ |
| Spec covers this permission? | grep permission name in spec | ✅/❌ |
| OemCompat has detection? | grep in `OemCompat.kt` | ✅/❌ |
| Onboarding has guidance step? | grep in `OnboardingStep.kt` | ✅/❌ |
| Manifest has declaration? | grep in `AndroidManifest.xml` | ✅/❌ |
```

If any ❌: **that's the gap** — spec/code needs to be updated.

---

## Phase 5: Diagnosis Report

Deliver using `/01-senior-reviewr` format:

```markdown
## 📋 OEM Permission Diagnosis: [OEM] — [Symptom Summary]

### Root Cause
[What's actually happening and why]

### Doc Alignment
[Table from Phase 4]

### 🔴 Gaps (Missing)
[What's not covered — spec gaps, code gaps, onboarding gaps]

### 🟢 Already Handled
[What's already working]

### 💡 Recommended Fix
[Specific changes: spec update, OemCompat addition, onboarding step, etc.]
```

---

## Phase 6: Fix (If Approved)

After user approval, implement in this order:

1. **Spec update** (`docs/cerb/notifications/spec.md`) — add OEM section or wave
2. **OemCompat.kt** — add detection/intent code for the OEM
3. **OnboardingStep.kt** — add step enum if new guidance needed
4. **OnboardingScreen.kt** — add UI for the new step
5. **Build + verify**: `./gradlew :app-prism:assembleDebug`

---

## Common Symptom → Fix Mapping

| Symptom | Likely Layer | Typical Fix |
|---------|-------------|-------------|
| Notifications don't show | L1 (battery) or L6 (HyperOS BG) | Battery exemption + onboarding guidance |
| Notifications silent on lock screen | L4 (MIUI lock screen) | `openLockScreenPermission()` guidance |
| Alarm late by ~1 hour | L3 (exact alarm) | `SCHEDULE_EXACT_ALARM` + settings page |
| App killed after screen off | L2 (auto-start) | Auto-start whitelist guidance |
| Notification banner doesn't pop | L5 (MIUI floating) | Floating notification guidance |
| App won't restart after reboot | L2 (auto-start) + BOOT_COMPLETED | Auto-start + receiver registration |
