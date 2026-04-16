#!/bin/bash

set -euo pipefail

PACKAGE_NAME="com.smartsales.prism"
MODE="${1:-all}"
SERIAL="${2:-}"

if [[ "$MODE" != "all" && "$MODE" != "connectivity" && "$MODE" != "oem" && "$MODE" != "snapshot" && "$MODE" != "help" && "$MODE" != "--help" && "$MODE" != "-h" ]]; then
    SERIAL="$MODE"
    MODE="all"
fi

ADB=(adb)
if [[ -n "$SERIAL" ]]; then
    ADB+=( -s "$SERIAL" )
fi

print_help() {
    cat <<'EOF'
Usage: scripts/device-verify-connectivity.sh [all|connectivity|oem|snapshot] [serial]

Modes:
  all           Print connectivity/OEM repro checklist, dump device/package snapshot, then stream focused logcat
  connectivity  Print connectivity-specific repro checklist, dump snapshot, then stream focused logcat
  oem           Print OEM reminder-specific repro checklist, dump snapshot, then stream focused logcat
  snapshot      Dump device/package snapshot only

Examples:
  scripts/device-verify-connectivity.sh
  scripts/device-verify-connectivity.sh connectivity
  scripts/device-verify-connectivity.sh all emulator-5554
EOF
}

if [[ "$MODE" == "help" || "$MODE" == "--help" || "$MODE" == "-h" ]]; then
    print_help
    exit 0
fi

require_device() {
    local device_count
    device_count="$("${ADB[@]}" devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
    if [[ "$device_count" -eq 0 ]]; then
        echo "No adb device attached. Connect a target device and rerun." >&2
        exit 1
    fi
}

print_header() {
    echo "Device Connectivity Verification"
    echo "================================"
    echo "Mode: $MODE"
    echo "Package: $PACKAGE_NAME"
    if [[ -n "$SERIAL" ]]; then
        echo "Serial: $SERIAL"
    fi
    echo
}

device_snapshot() {
    echo "[Device]"
    "${ADB[@]}" wait-for-device >/dev/null
    echo "serial=$("${ADB[@]}" get-serialno | tr -d '\r')"
    echo "manufacturer=$("${ADB[@]}" shell getprop ro.product.manufacturer | tr -d '\r')"
    echo "brand=$("${ADB[@]}" shell getprop ro.product.brand | tr -d '\r')"
    echo "model=$("${ADB[@]}" shell getprop ro.product.model | tr -d '\r')"
    echo "android_release=$("${ADB[@]}" shell getprop ro.build.version.release | tr -d '\r')"
    echo "sdk=$("${ADB[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
    echo
}

package_snapshot() {
    echo "[Package / Permission Snapshot]"
    "${ADB[@]}" shell dumpsys package "$PACKAGE_NAME" | grep -E "POST_NOTIFICATIONS|SCHEDULE_EXACT_ALARM|USE_FULL_SCREEN_INTENT|REQUEST_IGNORE_BATTERY_OPTIMIZATIONS|granted=" || true
    echo
    echo "[Notification Snapshot]"
    "${ADB[@]}" shell dumpsys notification --noredact 2>/dev/null | grep -A6 -B2 "$PACKAGE_NAME" || true
    echo
}

print_connectivity_steps() {
    cat <<'EOF'
[Connectivity Repro]
1. Launch Smart Sales and open the connectivity flow for the badge.
2. Keep the badge disconnected or transport-not-ready, then trigger one badge recording-ready event.
3. Confirm no Prism-side recording-ready consumer flow is surfaced while transport is not ready.
4. Reconnect until the app reaches transport-ready state with a usable badge IP.
5. Trigger another badge recording-ready event and confirm the event is surfaced only now.

Expected log evidence:
- `SmartSalesConn`: persistent session + badge notification activity
- `AudioPipeline`: `Dropping recording notification while transport not ready` for the blocked event
- `AudioPipeline`: download / readiness logs only after transport-ready state is established
EOF
    echo
}

print_oem_steps() {
    cat <<'EOF'
[OEM Reminder Repro]
1. Create a near-future DEADLINE reminder task inside the scheduler drawer.
2. Lock the screen before the trigger time.
3. Let the reminder fire while the app is backgrounded.
4. Observe whether full-screen alarm, banner, sound, and wake behavior occur.
5. If delivery fails, walk the OEM settings checklist: notifications, exact alarm, full-screen intent, battery optimization, auto-start/app launch, lock-screen display, floating notification.

Expected log evidence:
- `RealAlarmScheduler`: exact alarm scheduling
- `TaskReminderReceiver`: receiver entered, full-screen-intent granted/denied branch, notification shown/failure
- `NotificationService`: notification displayed or blocked
- `OemCompat`: settings routing if the operator opens OEM guidance from the app
EOF
    echo
}

stream_logs() {
    echo "[Focused Logcat]"
    echo "Press Ctrl+C to stop."
    echo
    "${ADB[@]}" logcat -c
    "${ADB[@]}" logcat -v time \
        AudioPipeline:D \
        ConnectivityService:D \
        ConnectivityVM:D \
        SmartSalesConn:D \
        TaskReminderReceiver:D \
        RealAlarmScheduler:D \
        NotificationService:D \
        OemCompat:D \
        AlarmDismissReceiver:D \
        BluetoothGatt:D \
        ActivityTaskManager:I \
        AlarmManager:I \
        *:S
}

print_header
require_device
device_snapshot
package_snapshot

case "$MODE" in
    all)
        print_connectivity_steps
        print_oem_steps
        stream_logs
        ;;
    connectivity)
        print_connectivity_steps
        stream_logs
        ;;
    oem)
        print_oem_steps
        stream_logs
        ;;
    snapshot)
        ;;
esac
