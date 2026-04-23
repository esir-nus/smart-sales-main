#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "usage: $0 <name> <displayed-date-iso> [transcript]" >&2
  exit 1
fi

NAME="$1"
DISPLAYED_DATE_ISO="$2"
TRANSCRIPT="${3:-}"
PKG="com.smartsales.prism"
ACTIVITY="${PKG}/.MainActivity"
OUT_DIR="${SCHEDULER_CAPTURE_DIR:-build/scheduler-captures}"

mkdir -p "$OUT_DIR"

adb shell am force-stop "$PKG"
adb logcat -c

START_ARGS=(
  am start -n "$ACTIVITY"
  --ez scheduler_dev_openScheduler true
  --es scheduler_dev_openSchedulerDateIso "$DISPLAYED_DATE_ISO"
)

if [[ -n "$TRANSCRIPT" ]]; then
  START_ARGS+=(--es scheduler_dev_text "$TRANSCRIPT")
  START_ARGS+=(--es scheduler_dev_displayedDateIso "$DISPLAYED_DATE_ISO")
fi

adb shell "${START_ARGS[@]}"
sleep 4

adb shell screencap -p "/sdcard/${NAME}.png"
adb pull "/sdcard/${NAME}.png" "${OUT_DIR}/${NAME}.png" >/dev/null
adb logcat -d > "${OUT_DIR}/${NAME}.logcat.txt"
adb shell uiautomator dump "/sdcard/${NAME}.xml" >/dev/null
adb pull "/sdcard/${NAME}.xml" "${OUT_DIR}/${NAME}.uia.xml" >/dev/null

echo "saved ${OUT_DIR}/${NAME}.png"
echo "saved ${OUT_DIR}/${NAME}.logcat.txt"
echo "saved ${OUT_DIR}/${NAME}.uia.xml"
