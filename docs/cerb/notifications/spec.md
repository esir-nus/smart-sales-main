# Notification Service — Internal Spec

> **OS Layer**: Infrastructure (Layer 1) — Leaf service, no upstream dependencies
> **Interface**: [interface.md](file:///home/cslh-frank/main_app/docs/cerb/notifications/interface.md)
> **State**: SHIPPED

---

## Overview

Centralizes all Android notification logic behind a single interface. Features call `NotificationService.show()` instead of building notifications directly. This ensures consistent channel management, permission handling, and notification styling.

> See [interface.md](file:///home/cslh-frank/main_app/docs/cerb/notifications/interface.md) for the full interface contract and output types.

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Interface + Real Implementation | 🚢 SHIPPED | `NotificationService.kt`, `RealNotificationService.kt`, `FakeNotificationService.kt` |
| **1.5** | Migration — Scheduler | 🚢 SHIPPED | Migrate `TaskReminderReceiver` to use `NotificationService` |
| **1.7** | Alarm Platform Hardening | 🔧 IN PROGRESS | Reboot survival, snooze, auto-dismiss, sound, concurrent alarms, broadcast security, WakeLock |
| **2** | Coach Nudge Channel | 🔲 PLANNED | Add coach-mode notifications (habit nudges, session end) |
| **3** | Badge Status Channel | 🔲 PLANNED | Recording complete, sync done, connection alerts |
| **4** | Memory Update Channel | 🔲 PLANNED | Entity merge notifications |

---

## Wave 1: Core Interface + Implementation

**Goal**: Extract notification logic into a centralized service.

**Ship Criteria**: `RealNotificationService.show()` can display a notification with correct channel, priority, and vibration pattern.

### Implementation

`RealNotificationService.kt`:
- Injects `@ApplicationContext context: Context`
- `show()`: Creates notification channels on first use, builds `NotificationCompat`, posts via `NotificationManagerCompat`
- `cancel()`: Cancels by notification ID hash
- `hasPermission()`: Checks `POST_NOTIFICATIONS` on API 33+

`FakeNotificationService.kt`:
- Records all `show()` calls for test assertions
- Stubable `hasPermission()` return value

### Test Cases
- [ ] `show() records correct id, title, body, channel`
- [ ] `cancel() removes notification from list`
- [ ] `hasPermission() returns stubbed value`

### Deliverables
- `domain/notification/NotificationService.kt` — interface
- `domain/notification/PrismNotificationChannel.kt` — channel enum
- `domain/notification/NotificationPriority.kt` — priority enum
- `data/notification/RealNotificationService.kt` — Android impl
- `data/fakes/FakeNotificationService.kt` — test fake
- `di/NotificationModule.kt` — Hilt binding

---

## Wave 1.5: Migration — Scheduler

**Goal**: Migrate `TaskReminderReceiver` to use `NotificationService` instead of building notifications directly.

**Ship Criteria**: `TaskReminderReceiver.showNotification()` delegates to `NotificationService.show()`. Notification behavior unchanged.

### Changes
- `TaskReminderReceiver`: Replace inline `NotificationCompat.Builder` with `NotificationService.show()` call
- Challenge: `BroadcastReceiver` doesn't support constructor injection — use `@AndroidEntryPoint` or manual `EntryPointAccessors`

### Test Cases
- [ ] Alarm triggers notification with correct task title
- [ ] Notification displays correct time offset text

---

## Wave 2: Coach Nudge Channel

**Goal**: Add coach-mode notifications for habit nudges and session summaries.

**Ship Criteria**: TBD — define when starting wave.

---

## Wave 3: Badge Status Channel

**Goal**: Add badge status notifications for recording events and sync status.

**Ship Criteria**: TBD — define when starting wave.

---

## Wave 4: Memory Update Channel

**Goal**: Add memory-related notifications for entity merge events.

**Ship Criteria**: TBD — define when starting wave.

---

## Channel Configuration

| Channel | Importance | Vibration | Lock Screen | Sound | DND Bypass | Banner |
|---------|-----------|-----------|-------------|-------|-----------|--------|
| TASK_REMINDER (v2) | HIGH | `[0, 500, 300, 500]` | VISIBILITY_PUBLIC | System default | ✅ | ✅ |
| COACH_NUDGE | DEFAULT | `[0, 150]` | VISIBILITY_PUBLIC | None | ❌ | ❌ |
| BADGE_STATUS | LOW | None | VISIBILITY_PUBLIC | None | ❌ | ❌ |
| MEMORY_UPDATE | DEFAULT | None | VISIBILITY_PUBLIC | None | ❌ | ❌ |

---

## Permission Flow

```
App Launch
    → ensureChannels() — create all channels on Android 8+
    → hasPermission() checked before show()
    → If denied: Log warning, skip silently
    → UserCenterScreen toggle → system settings redirect
```

---

## Existing Code Inventory

| File | Current Role | Wave |
|------|-------------|------|
| `TaskReminderReceiver.kt` | WakeLock + Notification + fullScreenIntent + Vibration | Shipped (Wave 1.5), Hardening (Wave 1.7) |
| `AlarmActivity.kt` | Full-screen alarm overlay (semi-transparent, 5 dismiss paths) | Shipped |
| `AlarmDismissReceiver.kt` | Unified dismiss handler (stop vibration + cancel notification) | Shipped |
| `RealNotificationService.kt` | Channel creation, vibration, notification display | Shipped |
| `RealAlarmScheduler.kt` | Alarm scheduling (WHEN) | Shipped |
| `UserCenterScreen.kt` L202-220 | Permission toggle | Keep as-is |

---

## Wave 1.7: Alarm Platform Hardening

**Goal**: Make the alarm system production-grade from a productivity/alarm company perspective.

### 1.7.1: Reboot Survival (`BOOT_COMPLETED`)

**Problem**: `AlarmManager` alarms are lost on device reboot. If user sets alarm at 6am, reboots at 5am → alarm never fires.

**Solution**:
- Add `RECEIVE_BOOT_COMPLETED` permission to manifest
- Create `BootCompletedReceiver` that re-schedules all pending alarms from `ScheduledTaskRepository`
- Query all future tasks with `hasAlarm = true`, call `AlarmScheduler.scheduleCascade()` for each

**Ship Criteria**: Reboot device → all future alarms still fire at correct times.

### 1.7.2: ~~Snooze~~ — DROPPED

> **Decision**: Snooze is redundant with cascade. The cascade system already provides escalating reminders at scheduled intervals. "知道了" dismisses the current alarm, but the next cascade alarm fires regardless. Snooze would undermine the urgency cadence by giving users an escape valve. The cascade IS the reminder mechanism — each tier escalates naturally (⏰ → ⚠️ → 🚨).

### 1.7.3: Auto-Dismiss Timeout

**Problem**: `AlarmActivity` + vibration run indefinitely if user is away from phone → battery drain.

**Solution**:
- `AlarmActivity`: `LaunchedEffect` with `delay(60_000L)` → auto-dismiss after **1 minute**
- Stop vibration, cancel notification, finish activity
- Leave a silent notification summary: "🔔 未接提醒: {task title}"

**Ship Criteria**: Alarm auto-dismisses after 1 minute. Summary notification remains.

### 1.7.4: Alarm Sound

**Problem**: Vibration only — not audible in noisy environments.

**Solution**:
- Set `TASK_REMINDER` channel sound to system default alarm ringtone: `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)`
- Channel already has `IMPORTANCE_HIGH` — sound plays automatically when set on channel
- **Note**: Current shipped channel is `_v2`. Adding sound requires channel ID bump to `_v3` (Android channels are immutable after creation)

**Ship Criteria**: Alarm plays system default sound + vibration.

### 1.7.5: Concurrent Alarm Notifications

**Problem**: `AlarmActivity` is `singleTop` — when 2 cascade alarms fire at the same minute, `onNewIntent()` replaces content. First task's alarm UI is lost.

**Solution**:
- Each cascade alarm gets a **unique notification** (already done — `notificationId = "$taskId-$offsetMinutes"`) 
- `AlarmActivity` should display a **stack** of active alarms instead of replacing
- When `onNewIntent()` arrives, **add** to display list instead of replacing
- Each card in the stack is independently dismissible

**Ship Criteria**: 2 alarms at the same time → both visible as stacked cards in `AlarmActivity`.

### 1.7.6: Broadcast Security

**Problem**: `AlarmDismissReceiver` sends `FINISH_ALARM` as implicit broadcast — any app could dismiss our alarms on Android 14+.

**Solution**:
- Use `Intent.setPackage(context.packageName)` on the `FINISH_ALARM` broadcast
- OR switch to `LocalBroadcastManager` (deprecated but simple)
- OR use explicit component intent

**Ship Criteria**: `FINISH_ALARM` broadcast only receivable by our app.

### 1.7.7: Screen Wake (WakeLock)

**Shipped**: `PARTIAL_WAKE_LOCK` with 10s timeout in `TaskReminderReceiver`.

**Enhancement**: On aggressive OEMs (Xiaomi, Huawei), `PARTIAL_WAKE_LOCK` may not turn on the screen. Add `SCREEN_BRIGHT_WAKE_LOCK` (deprecated but effective on Chinese ROMs) as fallback when `fullScreenIntent` permission is denied.

**Ship Criteria**: Screen turns on reliably on Xiaomi/Huawei devices.

---

### 1.7.8: Chinese OEM Compatibility (Anti-Ghosting)

**Problem**: Xiaomi (MIUI), Huawei (EMUI/HarmonyOS), Oppo (ColorOS), Vivo (OriginOS) aggressively kill background apps, cancel `AlarmManager` intents, and block `BOOT_COMPLETED` delivery. These mechanisms are **layered** and each must be addressed independently.

**Architecture**: **Single APK, runtime detection** via `Build.MANUFACTURER`. No multi-APK flavors.

```kotlin
// OemCompat.kt — utility for OEM detection + settings navigation
object OemCompat {
    val manufacturer: String get() = Build.MANUFACTURER.lowercase()
    
    val isXiaomi get() = manufacturer in listOf("xiaomi", "redmi", "poco")
    val isHuawei get() = manufacturer in listOf("huawei", "honor")
    val isOppo get() = manufacturer in listOf("oppo", "realme", "oneplus")
    val isVivo get() = manufacturer in listOf("vivo", "iqoo")
    val isChineseOem get() = isXiaomi || isHuawei || isOppo || isVivo
}
```

#### The Kill Chain (What We're Defending Against)

| Layer | Mechanism | Affects | AOSP Behavior | Chinese OEM Behavior |
|-------|-----------|---------|---------------|---------------------|
| 1 | Swipe Kill | Process | App paused | **App killed + AlarmManager intents cancelled** |
| 2 | Battery Optimization | Doze | `setExactAndAllowWhileIdle` respected | Delayed 5-15 min |
| 3 | Autostart Denied | Boot | `BOOT_COMPLETED` delivered | **Never delivered** |
| 4 | Background Limit | Service | 10 min + Doze rules | **Killed after ~3 min** |

#### Defense Layer 1: Battery Optimization Exemption

**Goal**: Prevent alarm delay from aggressive Doze.

- Add `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission to manifest
- On first alarm schedule → check `PowerManager.isIgnoringBatteryOptimizations()`
- If `false` → show system dialog: `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
- User-facing text: "为确保任务提醒准时送达，请允许后台运行"
- Store preference flag → don't re-prompt after user decision

#### Defense Layer 2: Autostart Guidance

**Goal**: Ensure `BOOT_COMPLETED` is delivered after reboot.

- On first alarm schedule (Chinese OEM only) → show one-time guidance dialog
- OEM-specific Intent map:

| OEM | Package | Activity | Fallback |
|-----|---------|----------|----------|
| Xiaomi | `com.miui.securitycenter` | `AutoStartManagementActivity` | App Info |
| Huawei | `com.huawei.systemmanager` | `StartupAppControlActivity` | App Info |
| Oppo | `com.coloros.safecenter` | `StartupAppListActivity` | App Info |
| Vivo | `com.iqoo.secure` | `MainCenterActivity` | App Info |

- All Intents wrapped in `try/catch` → graceful fallback to system App Info screen
- User-facing text: "请在系统设置中开启 Smart Sales 的自启动权限，否则重启后提醒将丢失"

#### Defense Layer 3: Foreground Service (Optional, P2)

**Goal**: Nuclear option for swipe-kill protection.

- Start `AlarmKeepAliveService` when future alarms exist
- Low-priority notification: "Smart Sales · N 个任务提醒已设置"
- Service auto-stops when no future alarms remain
- **Trade-off**: Persistent notification in status bar
- **Decision**: Only enable if Layer 1+2 prove insufficient on target devices

---

### 1.7.9: Cascade Visual Tiers

**Problem**: All cascade alarms (from `-2h` early warning to `0m` deadline) currently look and feel identical. The final `0m` alarm loses its urgency — user can't distinguish "heads up, you have 15 minutes" from "THIS IS NOW, YOU'RE LATE."

**Solution**: Three visual tiers based on cascade offset:

| Tier | Offsets | Priority | Notification Style | Sound | Vibration | Full-screen |
|------|---------|----------|--------------------|-------|-----------|-------------|
| **Early Warning** | `-2h` to `-5m` | `HIGH` | Standard banner | Channel default | One-shot `[0, 250]` | ❌ |
| **Final Warning** | `-1m` | `HIGH` | Banner + ⚠️ icon | Channel default | Pattern `[0, 500, 300, 500]` | ❌ |
| **Deadline Alarm** | `0m` | `URGENT` | Full-screen overlay | System alarm ringtone | **Persistent loop** | ✅ `AlarmActivity` |

**Tier determination logic**:
```kotlin
fun cascadeTier(offsetMinutes: Int): CascadeTier = when (offsetMinutes) {
    0    -> CascadeTier.DEADLINE     // 0m = URGENT, full-screen
    1    -> CascadeTier.FINAL        // -1m = elevated warning  
    else -> CascadeTier.EARLY        // everything else = standard
}
```

**Visual differences**:
- **Early**: Title = "⏰ {title} — {offset}分钟后开始". No action buttons.
- **Final**: Title = "⚠️ {title} — 1分钟后!". Action: "知道了" (dismiss).
- **Deadline**: Full-screen `AlarmActivity` with persistent vibration. Action: "知道了" (dismiss). Title = "🚨 {title} — 现在!"

**Ship Criteria**: Side-by-side, user can instantly tell "early warning" from "deadline alarm".

---

### Wave 1.7 Test Cases

**AOSP**:
- [ ] Reboot device → future alarms still fire
- [ ] Tap snooze → alarm re-fires in 5 min as `0m` URGENT alarm
- [ ] Leave alarm untouched → auto-dismisses after 1 min, summary notification remains
- [ ] Alarm plays system default sound + vibration
- [ ] 2 alarms at same time → both visible as stacked cards
- [ ] `FINISH_ALARM` broadcast cannot be spoofed by external apps

**Cascade Visual Tiers**:
- [ ] `-15m` alarm → standard banner, no full-screen, short vibration
- [ ] `-1m` alarm → elevated banner with ⚠️, pattern vibration
- [ ] `0m` alarm → full-screen AlarmActivity, persistent vibration, alarm sound
- [ ] Snooze from `-1m` → re-fires as `0m` URGENT tier (not as another `-1m`)

**Chinese OEM (MIUI/EMUI)**:
- [ ] Lock screen + Xiaomi MIUI → screen wakes and alarm shows
- [ ] Xiaomi: Swipe kill → alarm still fires (with Layer 1+2 enabled)
- [ ] Xiaomi: Reboot → alarm survives (with autostart enabled)
- [ ] Huawei: Deep Doze → alarm fires within 1 min tolerance
- [ ] Battery optimization dialog shown on first alarm (Chinese OEM only)
- [ ] OEM autostart Intent resolves (Xiaomi, Huawei, Oppo, Vivo)
- [ ] Graceful fallback on unsupported/unknown OEM

