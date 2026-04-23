# Scheduler Text Injection

Purpose: drive the scheduler Path A flow without badge hardware or live ASR.

## Scope

- Dev builds only
- Android scheduler lane only
- Uses the same scheduler coordinator path as transcript-backed voice ingress

## Launch Extras

`MainActivity` accepts these dev-only extras when `-PschedulerDevTools=true` is enabled:

- `scheduler_dev_openScheduler=true`
- `scheduler_dev_openSchedulerDateIso=YYYY-MM-DD`
- `scheduler_dev_text=<transcript>`
- `scheduler_dev_displayedDateIso=YYYY-MM-DD`
- `scheduler_dev_scenarioId=<scenario id>`

## One-shot Capture

Run from the repo or worktree root:

```bash
./scripts/scheduler_dev_capture.sh <name> <displayed-date-iso> [transcript]
```

Examples:

```bash
./scripts/scheduler_dev_capture.sh a1_open 2026-04-22
./scripts/scheduler_dev_capture.sh a1_after 2026-04-22 "明天下午三点客户拜访"
```

Artifacts land under `build/scheduler-captures/`:

- `<name>.png`
- `<name>.logcat.txt`
- `<name>.uia.xml`

## Direct adb Form

```bash
adb shell am start -n com.smartsales.prism/.MainActivity \
  --ez scheduler_dev_openScheduler true \
  --es scheduler_dev_openSchedulerDateIso 2026-04-22 \
  --es scheduler_dev_text "周二下午两点、三点、四点各加一个客户拜访" \
  --es scheduler_dev_displayedDateIso 2026-04-22
```

## Current Device Caveat

- The attached Xiaomi / HyperOS device may surface OEM reminder-hardening dialogs after task creation. They are real device behavior, not scheduler proof by themselves.
- Badge button, BLE wake, and live ASR remain human-only smoke territory. This seam starts below transcription.
