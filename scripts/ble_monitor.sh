#!/bin/bash
# BLE Traffic Monitor via Logcat
# Usage: ./ble_monitor.sh [device_id]
#
# Filters logcat for SmartSalesConn BLE traffic (TX/RX)
# Run this in a terminal while testing WiFi provisioning

DEVICE_ARG=""
if [ -n "$1" ]; then
    DEVICE_ARG="-s $1"
fi

echo "🔍 BLE Traffic Monitor"
echo "======================"
echo "Tag: SmartSalesConn"
echo "Filtering: TX/RX packets"
echo "Press Ctrl+C to stop"
echo ""

# Clear logcat and filter for our tag
adb $DEVICE_ARG logcat -c
adb $DEVICE_ARG logcat -v time SmartSalesConn:D *:S | grep -E "(TX|RX) \[" --color=auto
