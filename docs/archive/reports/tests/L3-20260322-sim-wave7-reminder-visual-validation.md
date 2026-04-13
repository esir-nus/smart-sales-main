# SIM Wave 7 Reminder Visual Validation

Date: 2026-03-22
Device: 2410DPN6CC
Scope: SIM scheduler T4.8 native reminder visibility on real device
Verdict: Accepted

## 1. Contract Read

- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/notifications/spec.md`
- `docs/sops/oem-alarm-notification-checklist.md`

## 2. Device Preconditions

- SIM debug build and androidTest APK installed on the connected device
- App notification block for `com.smartsales.prism` was corrected before the live rerun
- Validation crossed local midnight on device, so evidence timestamps below are on **2026-03-22**

## 3. What Was Checked

### EARLY reminder

Used the visual-hold harness `TaskReminderReceiverVisualHoldTest#earlyReminderVisualHold` to trigger the real receiver path on-device.

Observed evidence:

- `TaskReminderReceiver` log on **2026-03-22 00:10:13**:
  - `收到任务提醒: taskId=sim-early-visual, title=SIM EARLY Visual, offset=15min, tier=EARLY`
  - `通知已显示: id=sim-early-visual-15, tier=EARLY, channel=prism_task_reminders_v3_early`
- `dumpsys notification --noredact` immediately after the run showed an active `NotificationRecord` for:
  - package `com.smartsales.prism`
  - title `⏰ SIM EARLY Visual`
  - channel `prism_task_reminders_v3_early`
  - effective importance `4`
- a later wake/capture follow-up on **2026-03-22 00:38** surfaced the same EARLY reminder as a human-visible secure lock-screen card showing:
  - title `⏰ SIM EARLY Visual`
  - body `15分钟后开始`
- matching `dumpsys notification --noredact` lines after that capture still showed:
  - `NotificationRecord(... pkg=com.smartsales.prism ... importance=4 ...)`
  - `android.title=String (⏰ SIM EARLY Visual)`
  - `android.text=String (15分钟后开始)`

Result:

- native EARLY notification delivery is proven on real device
- EARLY reminder now also has human-visible lock-screen presentation proof on this Xiaomi/HyperOS device

### DEADLINE reminder, unlocked device

Used the visual-hold harness `TaskReminderReceiverVisualHoldTest#deadlineReminderVisualHold` on an unlocked device.

Observed evidence:

- `TaskReminderReceiver` log on **2026-03-22 00:14:07**:
  - `收到任务提醒: taskId=sim-deadline-visual, title=SIM DEADLINE Visual, offset=0min, tier=DEADLINE`
  - `fullScreenIntent 已设置 (DEADLINE)`
  - `通知已显示: id=sim-deadline-visual-0, tier=DEADLINE, channel=prism_task_reminders_v3_deadline`
- live device screenshot showed a visible DEADLINE reminder card with:
  - title `🚨 SIM DEADLINE Visual`
  - body `现在开始!`
  - action button `知道了`

Result:

- DEADLINE reminder is visually visible on real device in the unlocked branch
- the device presented it as a prominent heads-up style surface during this run

### DEADLINE reminder, locked device

Repeated `TaskReminderReceiverVisualHoldTest#deadlineReminderVisualHold` after turning the display off first.

Observed evidence:

- `TaskReminderReceiver` log on **2026-03-22 00:25:16**:
  - `收到任务提醒: taskId=sim-deadline-visual, title=SIM DEADLINE Visual, offset=0min, tier=DEADLINE`
  - `fullScreenIntent 已设置 (DEADLINE)`
  - `通知已显示: id=sim-deadline-visual-0, tier=DEADLINE, channel=prism_task_reminders_v3_deadline`
- `AlarmActivity` log on **2026-03-22 00:25:16**:
  - `持续振动已启动 (onCreate)`

Result:

- the locked-device run proves the DEADLINE branch created `AlarmActivity`, not just the notification object
- screenshot capture on this pass was not visually reliable; the captured frame remained a dark lock-screen/fingerprint surface even though `AlarmActivity.onCreate()` fired

## 4. Commands Used

```text
./gradlew :app-core:installDebug :app-core:installDebugAndroidTest
adb shell am instrument -w -r -e class com.smartsales.prism.data.scheduler.TaskReminderReceiverVisualHoldTest#earlyReminderVisualHold com.smartsales.prism.test/androidx.test.runner.AndroidJUnitRunner
adb shell dumpsys notification --noredact | rg "sim-early-visual|SIM EARLY Visual|prism_task_reminders_v3_early|NotificationRecord"
adb shell am instrument -w -r -e class com.smartsales.prism.data.scheduler.TaskReminderReceiverVisualHoldTest#deadlineReminderVisualHold com.smartsales.prism.test/androidx.test.runner.AndroidJUnitRunner
adb logcat -d -s TaskReminderReceiver AlarmActivity
adb shell input keyevent 26
adb shell input swipe 700 2600 700 800
adb exec-out screencap -p > /tmp/lock_seq1.png
adb shell dumpsys notification --noredact | rg -n "SIM EARLY Visual|sim-early-visual|prism_task_reminders_v3_early|15分钟后开始|NotificationRecord"
```

## 5. Acceptance Reading

### Spec examiner

- aligned:
  - exact-task reminders use the shared native stack
  - EARLY and DEADLINE remain split by tier
  - DEADLINE reuses `fullScreenIntent` plus `AlarmActivity`
- follow-up EARLY lock-screen capture now closes the remaining presentation proof gap for the narrowed T4.8 contract

### Contract examiner

- no ownership drift found in this validation
- reminder behavior still stays inside the narrowed SIM T4.8 contract

### Build examiner

- install and live instrumentation harness runs succeeded
- real-device logs and notification-manager state were captured

### Break-it examiner

- initial attempts exposed a real OEM/state issue: app-level notification blocking (`importance=NONE`) suppressed live visibility until corrected
- after the block was cleared, both reminder tiers became operator-visible on this Xiaomi/HyperOS device, though the strongest EARLY proof arrived on the secure lock screen rather than an unlocked home/shade surface

## 6. Verdict

Accepted.

What is now accepted:

- exact-alarm guidance CTA path
- EARLY native reminder posting on real device
- EARLY human-visible reminder card on the secure lock screen
- DEADLINE visible reminder surface on unlocked real device
- DEADLINE `AlarmActivity` creation on locked real device

What remains open:

- no reminder-specific blocker remains for the narrowed T4.8 SIM contract
- broader OEM variance still belongs to future hardware coverage rather than this acceptance gate
