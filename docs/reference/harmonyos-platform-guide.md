# Huawei HarmonyOS / EMUI Platform Guide

> **Purpose**: Actionable reference for developing on Huawei HarmonyOS (EMUI).
> **Sources**: [HarmonyOS Guides](https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/application-dev-guide), Industry Knowledge (DontKillMyApp).
> **Last Updated**: 2026-02-11

---

## Quick Decision Table

| What You Want To Do | HarmonyOS/EMUI Default | Our Solution |
|---|---|---|
| Run in background | **Aggressively Killed** | Manual "App Launch" management |
| Auto-start on boot | **DENIED** | Manual "App Launch" management |
| Show alarms from background | **Blocked** if app killed | "Agent-powered Reminders" (Native) OR Manual Whitelist (Android) |
| Foreground Service | Often ignored/killed | Helps, but needs "App Launch" whitelist |
| Android Intents (Boot, Alarm) | Intercepted/Blocked | **User Manual Configuration Required** |

---

## 1. App Launch Management (启动管理)

**The #1 Blocker**: Huawei's "App Launch" (or "Launch Management") system supersedes standard Android Doze/App Standby. By default, it manages apps "Automatically", which usually means killing them shortly after screen lock.

**The Fix**: Users MUST switch to **"Manage Manually"** (手动管理).

**Settings Path**:
`设置` → `应用和服务` → `应用启动管理` → `[App Name]`

**Required Toggles**:
You must disable "Manage Automatically" and enable ALL three:
1.  **Auto-launch** (允许自启动) — Essential for `BOOT_COMPLETED`.
2.  **Secondary launch** (允许关联启动) — Less critical for us, but usually enabled with others.
3.  **Run in background** (允许后台活动) — **CRITICAL**. Without this, alarms die.

> **Note**: Unlike Xiaomi, there is no reliable "hidden AppOps code" to check this state programmatically. You must guide the user to this screen via `OemCompat.openAutoStartSettings()`.

---

## 2. PowerGenie (The Killer)

**Mechanism**: A system service (`com.huawei.powergenie`) that applies aggressive whitelist-based killing policies from the cloud.
**Impact**: Even with Foreground Services, apps not in the PowerGenie whitelist may be killed.
**Defense**: The "Manage Manually" setting *usually* overrides PowerGenie for that specific app.

---

## 3. Notifications & Permissions

HarmonyOS has a dual permission model (Android + Harmony). For Android apps:

-   **Notification Permission**: Standard Android `POST_NOTIFICATIONS` triggers system prompt.
-   **Lock Screen**: controlled via `设置` → `通知和状态栏`.
-   **Advanced**: `ohos.permission.PUBLISH_AGENT_REMINDER` is for native Harmony apps using the Agent-powered Reminder system.

**Agent-powered Reminders (Native)**:
HarmonyOS has a dedicated system for alarms called "Agent-powered Reminders" (代理提醒). This allows the system to post notifications even if the app process is dead.
*   **Pros**: 100% reliability.
*   **Cons**: Requires native HarmonyOS integration (not standard Android `AlarmManager`) and specific permission application to Huawei.
*   **Android App Strategy**: Stick to `AlarmManager.setAlarmClock()` + "Manage Manually" whitelist.

---

## 4. Alarm Reliability Ladder

| API | Reliability on EMUI | Notes |
|---|---|---|
| `setAlarmClock()` | ✅ High (with Whitelist) | Shows icon, usually respects timing. |
| `setExactAndAllowWhileIdle()` | ⚠️ Medium | Subject to 5-min deviations or kills if not whitelisted. |
| `setAndAllowWhileIdle()` | ❌ Low | Aggressive batching. |

**Key**: Huawei ignores `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intents often. The "App Launch" settings are the *real* battery optimization toggle.

---

## 5. Production Checklist

For reliable operation on Huawei/Honor devices:

- [ ] **App Launch (启动管理)** set to **Manage Manually** (Auto-launch + Background).
- [ ] **Battery Optimization** set to "Don't Allow" (secondary to App Launch).
- [ ] **Lock in Recents**: Instruct user to swipe down on app in task switcher to lock it (padlock icon).
- [ ] **Permissions**: Notification, Lock Screen, Sound enabled.
- [ ] **Check Log**: Monitor for `process died` or missing alarms in `adb logcat`.

---

## 6. Debug Commands

**Launch Settings Intent** (Try these to open settings programmatically):
```kotlin
// ComponentName for various EMUI versions
ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
```

**Check Package State**:
```bash
adb shell dumpsys package com.smartsales.prism | grep -i "userState"
```
