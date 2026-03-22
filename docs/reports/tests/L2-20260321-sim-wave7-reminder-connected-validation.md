# SIM Wave 7 Reminder Connected Validation

Date: 2026-03-21
Scope: SIM scheduler T4.8 reminder acceptance on the connected Android device
Verdict: Conditionally accepted for connected proof; manual/operator visual L3 still open

## 1. Contract Read

- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/sim-scheduler/spec.md`
- `docs/cerb/notifications/spec.md`
- `docs/sops/oem-alarm-notification-checklist.md`

## 2. What Was Verified

### Exact-alarm redirect UX

Connected Compose coverage now proves the SIM scheduler reminder prompt path on device:

- `SchedulerDrawerSimModeTest.exactAlarmPromptShowsGuideAndRoutesPrimaryAction`
- injected guide text renders
- primary CTA routes through the reminder-action opener seam
- dialog dismisses after CTA

### Native reminder posting

Connected device instrumentation now proves the receiver-backed native reminder posting path:

- `TaskReminderReceiverDeviceTest.earlyReminderPostsNativeNotificationWithoutFullScreenIntent`
  - EARLY reminder posts a native notification on `prism_task_reminders_v3_early`
  - no `fullScreenIntent` is attached
- `TaskReminderReceiverDeviceTest.deadlineReminderPostsDeadlineNotificationWithFullScreenIntent`
  - DEADLINE reminder posts a native notification on `prism_task_reminders_v3_deadline`
  - `fullScreenIntent` is attached

### Log evidence

Focused `adb logcat -d -s TestRunner TaskReminderReceiver AlarmActivity` captured:

- `TaskReminderReceiver` received the DEADLINE reminder
- `fullScreenIntent 已设置 (DEADLINE)`
- `通知已显示: id=sim-deadline-0, tier=DEADLINE, channel=prism_task_reminders_v3_deadline`
- `TaskReminderReceiver` received the EARLY reminder
- `通知已显示: id=sim-early-15, tier=EARLY, channel=prism_task_reminders_v3_early`

## 3. Commands Run

```text
./gradlew :app-core:compileDebugKotlin :app-core:testDebugUnitTest --tests "com.smartsales.prism.ui.sim.SimSchedulerViewModelTest" --tests "com.smartsales.prism.data.notification.ReminderReliabilityAdvisorTest"
./gradlew :app-core:compileDebugKotlin :app-core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerSimModeTest,com.smartsales.prism.data.scheduler.TaskReminderReceiverDeviceTest
./gradlew :app-core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartsales.prism.data.scheduler.TaskReminderReceiverDeviceTest
adb logcat -d -s TestRunner TaskReminderReceiver AlarmActivity
```

All commands above completed successfully in the final run.

## 4. Break-It Findings

- The first connected-device run found app notifications disabled on the device, so native reminder delivery could not be exercised honestly until the test setup enabled notification permission for the target package.
- An initial stronger assertion that DEADLINE should auto-resume `AlarmActivity` under instrumentation failed on the connected device even though the receiver logged full-screen intent configuration and notification display.

## 5. Remaining Gap

This run does **not** close the final human-visible L3 branch yet.

Still unproven on device:

- user-visible EARLY banner appearance
- user-visible DEADLINE lock-screen/full-screen presentation outside instrumentation
- OEM-policy behavior for auto-launching `AlarmActivity` in a real operator flow

Use `docs/sops/oem-alarm-notification-checklist.md` for the next manual/operator pass.

## 6. Acceptance Reading

Aligned with the current SIM scheduler spec:

- exact-task reminders use the shared native stack
- EARLY vs DEADLINE tier split is preserved
- exact-alarm guidance remains UI-bound and OEM-aware

Not fully accepted as final T4.8 closeout yet because the manual visual L3 branch remains open.
