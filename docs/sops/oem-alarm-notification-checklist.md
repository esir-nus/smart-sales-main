# OEM Alarm / Notification Settings Checklist

> **Purpose**: Live operator checklist for diagnosing reminder and alarm delivery failures on OEM-customized Android ROMs.
> **Primary SOT**:
> - `docs/cerb/notifications/spec.md`
> - `app-core/src/main/java/com/smartsales/prism/data/notification/OemCompat.kt`
> **Scope**: Reminder banner, deadline alarm, lock-screen visibility, and reboot/background survival for Smart Sales / SIM scheduler reminders.

---

## 1. When to Use This Checklist

Use this when repo evidence shows:

- `RealAlarmScheduler` scheduled the reminder
- `TaskReminderReceiver` received the reminder
- `TaskReminderReceiver` logged `通知已显示`

but the user still reports one of these symptoms:

- no visible notification
- no lock-screen alarm
- no floating banner
- no sound/vibration
- alarm lost after reboot
- app only works while foregrounded

That pattern usually means **OEM permission or ROM policy drift**, not scheduler persistence failure.

---

## 2. Fast Triage Order

Check these in order:

1. **App notifications enabled at system level**
2. **Exact alarm**
3. **Battery optimization exemption**
4. **Auto-start / app launch management**
5. **Lock-screen display**
6. **Floating notification**
7. **Background local notification**

If logs show the receiver fired but the user saw nothing, first rule out app-level notification blocking, then prioritize steps 5-7 on Xiaomi/HyperOS.

---

## 3. Universal Checklist

Apply on all OEMs first:

- [ ] App notification master switch is enabled
- [ ] App notification importance is not blocked / none
- [ ] Task reminder channels are enabled
- [ ] Exact alarm permission is enabled on Android 12+
- [ ] Battery optimization is disabled / ignored for the app
- [ ] App is not in a sleeping / restricted / optimized bucket
- [ ] DND is off while validating EARLY reminders
- [ ] Volume / alarm stream is audible while validating DEADLINE reminders

---

## 4. Notification-Manager Blocked Case

If device evidence shows any of these:

- `POST_NOTIFICATION: ignore`
- app `importance=NONE`
- notification stats `numBlocked > 0`

then the reminder may fully execute in code while the system still blocks all visible delivery.

Treat that as the highest-priority fix:

- [ ] Open app notification settings
- [ ] Re-enable the app notification master switch
- [ ] Re-enable task reminder channels
- [ ] Re-run the near-future exact reminder test before checking deeper OEM settings

---

## 5. Xiaomi / Redmi / Poco / HyperOS Checklist

This is the highest-risk OEM path in the current repo.

### Required checks

- [ ] **Exact alarm** enabled
  - Path: Settings -> Apps -> Special permissions -> Alarms & reminders -> Smart Sales
- [ ] **Battery optimization** ignored
  - Path: Settings -> Apps -> Smart Sales -> Battery -> No restrictions
- [ ] **Auto-start** enabled
  - Path: Security app -> Auto start -> Smart Sales
- [ ] **Lock-screen notification/display** enabled
  - Path: Settings -> Notifications & Control center -> Lock screen notifications
  - App-specific path may also be under Security -> App permissions / Other permissions
- [ ] **Floating notifications** enabled
  - Needed for visible banner-style presentation
- [ ] **Background notifications** enabled
  - HyperOS can block local notifications from background even after scheduling succeeds

### Repo-backed interpretation

Mapped in `OemCompat.kt`:

- `needsExactAlarmPermission(...)`
- `openExactAlarmSettings(...)`
- `openLockScreenPermission(...)`
- `canShowOnLockScreen(...)`
- `canShowFloatingNotification(...)`
- `canSendBackgroundNotification(...)`

### Practical reading

If logs show:

- `收到任务提醒`
- `fullScreenIntent 已设置 (DEADLINE)`
- `通知已显示`

but the user saw nothing, Xiaomi/HyperOS permission state is still the primary suspect.

---

## 6. Huawei / Honor Checklist

Huawei/Honor usually fails through background launch management rather than Xiaomi-style AppOps.

### Required checks

- [ ] **Exact alarm** enabled on Android 12+
- [ ] **Battery optimization** disabled
- [ ] **App launch management / auto-start** set to manual allow
- [ ] **Allow auto-launch**
- [ ] **Allow secondary launch**
- [ ] **Allow background activity**
- [ ] Lock-screen notifications enabled

### Practical paths

- Settings -> Apps -> App launch -> Smart Sales -> Manage manually
- Settings -> Battery -> App launch / Launch manager
- Settings -> Notifications -> Lock screen notifications

### Repo-backed interpretation

The repo treats Huawei/Honor as:

- aggressive on boot/background survival
- less inspectable programmatically than Xiaomi
- more dependent on user-managed launch/background settings

---

## 7. Oppo / Realme / OnePlus Checklist

- [ ] Exact alarm enabled
- [ ] Battery optimization disabled
- [ ] Auto-start enabled
- [ ] Background activity allowed
- [ ] Lock-screen notification enabled

Primary failure mode: process kill / startup suppression.

---

## 8. Vivo / iQOO Checklist

- [ ] Exact alarm enabled
- [ ] Battery optimization disabled
- [ ] Auto-start enabled
- [ ] Background activity allowed
- [ ] Lock-screen notification enabled

Primary failure mode: aggressive battery/background management.

---

## 9. Symptom -> Likely Setting

| Symptom | Most Likely Cause |
|---------|-------------------|
| Logs show receiver fired, `importance=NONE` / `numBlocked > 0` | App notifications globally disabled by system |
| Logs show reminder fired, user saw nothing | Xiaomi lock-screen / floating / background notification permission |
| Reminder arrives late by minutes or longer | Exact alarm missing or battery optimization still active |
| Works only while app is open | Background restriction / OEM kill policy |
| Lost after reboot | Auto-start / app launch management |
| Full-screen deadline never appears | Full-screen/lock-screen presentation blocked by OEM settings |

---

## 10. Validation Playbook

Use a near-future exact task, then confirm both sides:

### Log evidence

- [ ] `RealAlarmScheduler` scheduled the reminder
- [ ] `TaskReminderReceiver` received the reminder
- [ ] `TaskReminderReceiver` logged `通知已显示`

### User-visible evidence

- [ ] EARLY reminder shows a visible banner or notification entry
- [ ] DEADLINE reminder shows notification and, when supported, full-screen alarm UI
- [ ] lock screen can surface the reminder when the device is locked

If log evidence is green and user-visible evidence is red:

1. rule out app-level notification blocking first
2. then treat it as an OEM-settings problem

---

## 11. Maintenance Rule

Keep this checklist aligned with:

- new OEM branches added to `OemCompat.kt`
- notification/alarm behavior changes in `docs/cerb/notifications/spec.md`
- real device findings from on-device test reports

When a new OEM-specific failure pattern is confirmed, update this checklist in the same session.
