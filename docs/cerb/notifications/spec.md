# Notification Service — Internal Spec

> **OS Layer**: Infrastructure (Layer 1) — Leaf service, no upstream dependencies
> **Interface**: [interface.md](file:///home/cslh-frank/main_app/docs/cerb/notifications/interface.md)

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
- `domain/notification/NotificationChannel.kt` — channel enum
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

| Channel | Importance | Vibration | Banner |
|---------|-----------|-----------|--------|
| TASK_REMINDER | HIGH | `[0, 250, 250, 250]` | ✅ |
| COACH_NUDGE | DEFAULT | `[0, 150]` | ❌ |
| BADGE_STATUS | LOW | None | ❌ |
| MEMORY_UPDATE | DEFAULT | None | ❌ |

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
| `TaskReminderReceiver.kt` | Inline notification building | Migrate in Wave 1.5 |
| `UserCenterScreen.kt` L202-220 | Permission toggle | Keep as-is |
| `RealAlarmScheduler.kt` | Alarm scheduling (WHEN) | No change — separate concern |
