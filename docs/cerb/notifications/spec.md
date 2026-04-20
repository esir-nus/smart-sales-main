# Notification Service — Internal Spec

> **OS Layer**: Infrastructure (Layer 1) — Leaf service, no upstream dependencies
> **Interface**: [interface.md](file:///home/cslh-frank/main_app/docs/cerb/notifications/interface.md)
> **State**: SHIPPED

---

## Overview

Centralizes all Android notification logic behind a single interface. Features call `NotificationService.show()` instead of building notifications directly. This ensures consistent channel management, permission handling, and notification styling.

> See [interface.md](file:///home/cslh-frank/main_app/docs/cerb/notifications/interface.md) for the full interface contract and output types.
> Live OEM operator checklist: `docs/sops/oem-alarm-notification-checklist.md`
> OEM management control plane: `docs/sops/oem-alarm-notification-control-plane.md`

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

**Ship Criteria**: `TaskReminderReceiver.showNotification()` delegates to `NotificationService.show()`. Reminder notification behavior remains unchanged, while the receiver may also emit a best-effort badge chime signal that never blocks reminder delivery.

### Changes
- `TaskReminderReceiver`: Replace inline `NotificationCompat.Builder` with `NotificationService.show()` call
- `TaskReminderReceiver`: after the reminder path starts, optionally emit one best-effort badge chime (`commandend#1`) through `DeviceConnectionManager.notifyTaskFired()`
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
| TASK_REMINDER_EARLY (v3) | HIGH | `[0, 250]` | VISIBILITY_PUBLIC | System default | ❌ | ✅ |
| TASK_REMINDER_DEADLINE (v3) | HIGH | `[0, 500, 300, 500]` | VISIBILITY_PUBLIC | Alarm ringtone | ✅ | ✅ |
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
    → User Center notification toggle → system settings redirect
```

---

## Existing Code Inventory

| File | Current Role | Wave |
|------|-------------|------|
| `TaskReminderReceiver.kt` | WakeLock + Notification + fullScreenIntent + Vibration + best-effort badge chime | Shipped (Wave 1.5), Hardening (Wave 1.7) |
| `AlarmActivity.kt` | Full-screen alarm overlay (semi-transparent, 5 dismiss paths) | Shipped |
| `AlarmDismissReceiver.kt` | Unified dismiss handler (stop vibration + cancel notification) | Shipped |
| `RealNotificationService.kt` | Channel creation, vibration, notification display | Shipped |
| `RealAlarmScheduler.kt` | Alarm scheduling (WHEN) | Shipped |
| `User Center` surfaces (`UserCenterScreen.kt`, SIM user-center drawer) | Permission toggle opens Android app notification settings and refreshes state on resume | Keep behavior aligned across hosts |

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
- Split `TASK_REMINDER` into two channels: `TASK_REMINDER_EARLY` (no DND bypass) and `TASK_REMINDER_DEADLINE` (DND bypass + alarm ringtone)
- `TASK_REMINDER_DEADLINE` channel uses `RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)` with `USAGE_ALARM` AudioAttributes
- Channel IDs bumped to `_v3_early` / `_v3_deadline` (Android channels immutable after creation)
- Old channels (`_v2`, `_v3`) deleted in `ensureChannels()`

**Ship Criteria**: ✅ DEADLINE alarm plays system alarm sound + vibration + bypasses DND. EARLY respects DND.

### 1.7.5: Concurrent Alarm Notifications

**Problem**: `AlarmActivity` is `singleTop` — when 2 cascade alarms fire at the same minute, `onNewIntent()` replaces content. First task's alarm UI is lost.

**Solution**:
- Each cascade alarm gets a **unique notification** (already done — `notificationId = "$taskId-$offsetMinutes"`) 
- `AlarmActivity` should display a **stack** of active alarms instead of replacing
- When `onNewIntent()` arrives, **add** to display list instead of replacing
- the in-activity stack must de-duplicate by `taskId`, refresh the newest payload in place, and keep the newest active task on top
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

**Problem**: Xiaomi (MIUI), Huawei/Honor (EMUI/HarmonyOS), Oppo (ColorOS), Vivo (OriginOS) aggressively kill background apps, cancel `AlarmManager` intents, and block `BOOT_COMPLETED` delivery. These mechanisms are **layered** and each must be addressed independently.

**Architecture**: **Single APK, runtime detection** via `Build.MANUFACTURER`. No multi-APK flavors.

For day-to-day debugging and manual validation, keep `docs/sops/oem-alarm-notification-checklist.md`, `docs/sops/oem-alarm-notification-control-plane.md`, and `OemCompat.kt` synchronized with this section.

The shipped runtime guidance should also stay adaptive to the current OEM. If the app already knows it is on Xiaomi/HyperOS, Huawei/Honor, Oppo/Realme, or Vivo/iQOO, the user-facing hardening prompt must prefer OEM-specific checklist copy and the nearest reachable settings page over a generic one-size-fits-all alarm warning.

For Huawei/Honor, the current Android-app strategy remains a single Huawei-family branch keyed by `Build.MANUFACTURER in {"huawei", "honor"}`. This slice does **not** add native Harmony reminder APIs; it keeps the Android app path and treats HarmonyOS/EMUI reliability as a launch-management problem first.

The first branch in that adaptive prompt must be app-level notification blocking itself. If the system has already set the app to `importance=NONE`, `POST_NOTIFICATION: ignore`, or an equivalent “all notifications off” state, the runtime should guide the user back to app notification settings before suggesting deeper OEM hardening.

For Android 14+ screen-off deadline alarms, app-level full-screen-intent permission is also a first-class gate. The runtime should not treat a denied full-screen-intent setting as an OEM-only issue; it must route the user to the Android full-screen-intent settings page before or alongside OEM-specific hardening.

For Xiaomi / HyperOS, screen-off deadline alarm presentation is now understood as two separate launch/display branches:

- Android full-screen-intent policy
- HyperOS background popup policy

Do not collapse these into the same diagnosis. A device may allow `fullScreenIntent` in the Android sense while HyperOS still blocks background page launch.

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

Huawei/Honor note: on HarmonyOS/EMUI this remains secondary guidance only. The real first-line defense is App Launch / Launch Management manual allow.

#### Defense Layer 2: Autostart Guidance

**Goal**: Ensure `BOOT_COMPLETED` is delivered after reboot.

- On first alarm schedule (Chinese OEM only) → show one-time guidance dialog
- Huawei/Honor must treat this layer as the primary runtime guidance after app-level notification blocking and exact-alarm checks, even when battery optimization already looks healthy
- OEM-specific Intent map:

| OEM | Package | Activity | Fallback |
|-----|---------|----------|----------|
| Xiaomi | `com.miui.securitycenter` | `AutoStartManagementActivity` | App Info |
| Huawei/Honor | `com.huawei.systemmanager` | `StartupAppControlActivity` -> `StartupNormalAppListActivity` -> `ProtectActivity` | App Info |
| Oppo | `com.coloros.safecenter` | `StartupAppListActivity` | App Info |
| Vivo | `com.iqoo.secure` | `MainCenterActivity` | App Info |

- All Intents wrapped in `try/catch` → graceful fallback to system App Info screen
- User-facing text: "请在系统设置中开启 Smart Sales 的自启动权限，否则重启后提醒将丢失"
- Huawei/Honor copy must explicitly mention `应用启动管理 / Launch Management` and the three manual toggles: auto-launch, secondary launch, background activity
- Huawei/Honor copy must also mention lock-screen notification and floating notification settings (设置 → 通知和状态栏)

#### Screen-off deadline presentation follow-up

The current repo state is:

- Xiaomi / HyperOS has real locked-device evidence that `AlarmActivity` can launch through the shared deadline path on at least one device
- Huawei / Honor guidance is documentation-backed but still needs stronger locked-device validation evidence before this branch can be treated as fully closed

Track this work through `docs/plans/oem-alarm-hardening-plan.md`.

#### Defense Layer 3: Foreground Service (Optional, P2)

**Goal**: Nuclear option for swipe-kill protection.

- Start `AlarmKeepAliveService` when future alarms exist
- Low-priority notification: "Smart Sales · N 个任务提醒已设置"
- Service auto-stops when no future alarms remain
- **Trade-off**: Persistent notification in status bar
- **Decision**: Only enable if Layer 1+2 prove insufficient on target devices

---

### 1.7.9: Cascade Visual Tiers

**Problem**: All cascade alarms currently look and feel identical. The `0m` deadline alarm loses its urgency — user can’t distinguish “heads up, you have 15 minutes” from “THIS IS NOW.”

**Solution**: Two visual tiers based on cascade offset:

| Tier | Offsets | Priority | Notification Style | Sound | Vibration | Full-screen | DND |
|------|---------|----------|--------------------|-------|-----------|-------------|-----|
| **Early Warning** | All non-`0m` | `HIGH` | Standard banner | Channel default | One-shot `[0, 250]` | ❌ | Respects DND — suppressed in DND mode |
| **Deadline Alarm** | `0m` | `URGENT` | Full-screen overlay | System alarm ringtone | **Persistent loop** | ✅ `AlarmActivity` | **Bypasses DND** — always fires |

**Tier determination logic**:
```kotlin
fun cascadeTier(offsetMinutes: Int): CascadeTier = when (offsetMinutes) {
    0    -> CascadeTier.DEADLINE     // 0m = URGENT, full-screen, bypasses DND
    else -> CascadeTier.EARLY        // everything else = standard, respects DND
}
```

**Visual differences**:
- **Early**: Title = "⏰ {title} — {offset}分钟后开始". No action buttons. Respects DND.
- **Deadline**: Full-screen `AlarmActivity` with persistent vibration. Action: "知道了" (dismiss). Title = "🚨 {title} — 现在!". **Bypasses DND** to close the task.

**Ship Criteria**: Side-by-side, user can instantly tell “early warning” from “deadline alarm”.

---

### 1.7.10: DND (Do Not Disturb) Policy

**Policy**: Cascade alarms respect the user’s DND setting, with one exception: the final `0m` alarm.

| DND State | Early Offsets (`-2h` to `-5m`) | Deadline (`0m`) |
|-----------|-------------------------------|----------------|
| DND OFF | ✅ Fire normally | ✅ Fire normally |
| DND ON | ❌ Suppressed silently | ✅ **Bypasses DND** |

**Rationale**: Early warnings are informational — if the user is in DND, they don’t want interruption. But the deadline alarm is the *closure* signal: the task is happening NOW. Missing it defeats the purpose of scheduling.

**Implementation**: `URGENT` priority already bypasses DND on AOSP via `IMPORTANCE_HIGH` + `fullScreenIntent`. No additional code needed — Android handles this natively when `fullScreenIntent` is set.

---

### 1.7.11: UX Invariants

| Rule | How to Verify |
|------|---------------|
| **No Ghost Alarms**: If `AlarmActivity` can’t display (e.g., overdrawn), vibration must NOT start. | Code review: vibration starts inside `AlarmActivity.onCreate`, not in receiver |
| **One-Tap Cessation**: Dismissal requires exactly 1 gross motor gesture (swipe or large button) | UI inspection: button is full-width, swipe enabled |
| **Notification Grouping**: >3 concurrent alarms → summary notification ("🔔 N 个任务提醒") | Automated: count active notifications before posting |
| **Feedback within 200ms**: Tap "知道了" → vibration stops + activity finishes immediately | Stopwatch test |

---

### Wave 1.7 Test Cases

**AOSP**:
- [ ] Reboot device → future alarms still fire
- [ ] Leave alarm untouched → auto-dismisses after 1 min, summary notification remains
- [ ] Alarm plays system default sound + vibration
- [ ] 2 alarms at same time → both visible as stacked cards
- [ ] `FINISH_ALARM` broadcast cannot be spoofed by external apps

**Cascade Visual Tiers**:
- [ ] `-15m` alarm → standard banner, no full-screen, short vibration
- [ ] `0m` alarm → full-screen AlarmActivity, persistent vibration, alarm sound
- [ ] Early alarm during DND → suppressed silently
- [ ] `0m` alarm during DND → **still fires** (bypasses DND)
- [ ] >3 concurrent alarms → summary notification grouping
- [ ] Tap "知道了" → vibration stops within 200ms

**Chinese OEM (MIUI/EMUI/HarmonyOS)**:
- [ ] Lock screen + Xiaomi MIUI → screen wakes and alarm shows
- [ ] Xiaomi: Swipe kill → alarm still fires (with Layer 1+2 enabled)
- [ ] Xiaomi: Reboot → alarm survives (with autostart enabled)
- [ ] Huawei/Honor: Deep Doze → alarm fires within 1 min tolerance
- [ ] Battery optimization dialog shown on first alarm (Chinese OEM only)
- [ ] OEM autostart Intent resolves (Xiaomi, Huawei/Honor, Oppo, Vivo)
- [ ] Graceful fallback on unsupported/unknown OEM
