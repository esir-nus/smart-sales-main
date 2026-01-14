#!/bin/bash
# Full BLE Debug Logcat (verbose with all connectivity logs)
# Usage: ./ble_debug_full.sh [device_id]

DEVICE_ARG=""
if [ -n "$1" ]; then
    DEVICE_ARG="-s $1"
fi

echo "🔬 Full BLE Debug Monitor"
echo "========================="
echo "Tags: SmartSalesConn (BLE) + BluetoothGatt (System)"
echo "Press Ctrl+C to stop"
echo ""

adb $DEVICE_ARG logcat -c
adb $DEVICE_ARG logcat -v time \
    SmartSalesConn:D \
    BluetoothGatt:D \
    BluetoothAdapter:D \
    *:S
