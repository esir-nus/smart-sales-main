# HyperOS / MIUI Platform Guide

> **Purpose**: Actionable reference for developing on Xiaomi HyperOS (MIUI successor).
> **Sources**: [HyperOS Dev Portal](https://dev.mi.com/xiaomihyperos/documentation), on-device testing (Feb 2026).
> **Last Updated**: 2026-02-11

---

## Quick Decision Table

| What You Want To Do | HyperOS Default | Our Solution | Op Code |
|---|---|---|---|
| Show notification from background | **DENIED** | Onboarding guide + op 10021 detection | 10021 |
| Show lock screen notification | **DENIED** | Onboarding guide + op 10020 detection | 10020 |
| Show floating banner (heads-up) | **DENIED** | Onboarding guide + op 10016 detection | 10016 |
| Pop up Activity from background | **DENIED** | WakeLock fallback when fullScreenIntent blocked | — |
| Fire exact alarm | Delayed 5-15 min | `setAlarmClock()` for DEADLINE tier | — |
| Survive reboot | `BOOT_COMPLETED` blocked | Autostart permission guidance | — |
| Survive swipe kill | Process killed + alarms cancelled | Battery optimization exemption | — |
| Auto-start on boot | **DENIED** | Autostart settings guidance | — |

---

## 1. Process Management (Powerkeeper)

**Source**: [pId=1607](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1607)

HyperOS kills apps for these reasons (visible in `adb logcat -b events | grep am_kill`):

| Kill Reason | Trigger | Impact |
|---|---|---|
| `LockScreenClean` | User locks screen | App killed, AlarmManager intents cancelled |
| `AutoPowerKill` | Battery optimization | Background services terminated |
| `AutoThermalKill` | Thermal throttling | CPU-intensive apps killed |

**Defense**: Guide user to enable autostart + battery optimization exemption.

```bash
# Diagnose kill reason
adb logcat -b events | grep am_kill
```

---

## 2. Background Local Notification (后台发送本地通知)

**Source**: [pId=1626](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1626)

> 该权限默认拒绝。针对特殊应用提供白名单（音乐播放、日程提醒等）。

- **Default**: DENIED
- **AppOps code**: `10021`
- **Settings path**: 设置 → 应用管理 → [App] → 通知管理 → 后台发送本地通知
- **Same page as** `ACTION_APP_NOTIFICATION_SETTINGS`
- **Whitelist email**: `miui-notification-open@xiaomi.com` (日程提醒 qualifies)

**Detection**:
```kotlin
OemCompat.canSendBackgroundNotification(context) // op 10021 via reflection
```

---

## 3. Background Popup (后台弹出页面)

**Source**: [pId=1625](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1625)

> 该权限默认为拒绝。针对VOIP（来电）等提供白名单。

- **Default**: DENIED
- **Impact**: `fullScreenIntent` → launch Activity from background blocked
- **Whitelist email**: `miui-security-open@xiaomi.com`
- **Our fallback**: `SCREEN_BRIGHT_WAKE_LOCK` when `canUseFullScreenIntent()` is false

---

## 4. Autostart (自启动)

**Source**: [pId=1624](https://dev.mi.com/xiaomihyperos/documentation/detail?pId=1624)

> 手机将默认应用不可自启动。

- **Default**: DENIED
- **Impact**: `BOOT_COMPLETED` never delivered → alarms lost after reboot
- **Settings path**: `com.miui.securitycenter / AutoStartManagementActivity`
- **Our solution**: `OemCompat.openAutoStartSettings()` in onboarding

---

## 5. AppOps Code Registry

Known MIUI/HyperOS proprietary op codes used via `AppOpsManager.checkOpNoThrow()`:

| Op Code | Permission | Default | Detection Function |
|---|---|---|---|
| 10016 | 悬浮通知 (floating banner) | DENIED | `canShowFloatingNotification()` |
| 10020 | 锁屏显示 (lock screen display) | DENIED | `canShowOnLockScreen()` |
| 10021 | 后台发送本地通知 (background notification) | DENIED | `canSendBackgroundNotification()` |

**Reflection pattern** (all three use the same approach):
```kotlin
fun checkMiuiOp(context: Context, opCode: Int): Boolean? {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val method = appOps.javaClass.getMethod(
        "checkOpNoThrow", Int::class.java, Int::class.java, String::class.java
    )
    val result = method.invoke(appOps, opCode, Process.myUid(), context.packageName) as Int
    return result == AppOpsManager.MODE_ALLOWED
}
```

---

## 6. Alarm Reliability Ladder

What works on HyperOS, ordered by reliability:

| API | Reliability | When to Use |
|---|---|---|
| `setAlarmClock()` | ✅ Highest — treated as user-set alarm | DEADLINE (0m) |
| `setExactAndAllowWhileIdle()` | ⚠️ Delayed 5-15 min | Cascade tiers (15m, 5m, 1m) |
| `setAndAllowWhileIdle()` | ⚠️ Same delays | Fallback when exact perm denied |
| `setExact()` | ❌ Unreliable — Doze blocks | Don't use |

**Key**: `setAlarmClock()` requires `SCHEDULE_EXACT_ALARM` on Android S+. Guard with `canScheduleExactAlarms()`.

---

## 7. Production Checklist

For any app shipping on HyperOS that needs reliable background behavior:

- [ ] Battery optimization exemption (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
- [ ] Autostart enabled (OEM settings Intent)
- [ ] 后台发送本地通知 enabled (same page as notification settings)
- [ ] 锁屏显示 enabled
- [ ] 悬浮通知 enabled
- [ ] `SCHEDULE_EXACT_ALARM` granted (Android 12+)
- [ ] `USE_FULL_SCREEN_INTENT` granted (Android 14+)
- [ ] Whitelist application: `miui-notification-open@xiaomi.com` (for 日程提醒 category)

---

## 8. Debug Commands

```bash
# Check app op permissions
adb shell appops get com.smartsales.prism

# Watch for process kills
adb logcat -b events | grep am_kill

# Watch alarm scheduling
adb logcat -s RealAlarmScheduler:D

# Watch notification delivery
adb logcat -s TaskReminderReceiver:D

# Check background notification permission warning
adb logcat -s RealAlarmScheduler:W | grep 后台
```
