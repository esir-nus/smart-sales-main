#!/bin/bash
# BLE Traffic Monitor - TX/RX Only (minimal noise)
# Usage: ./ble_traffic.sh [device_id]

DEVICE_ARG=""
if [ -n "$1" ]; then
    DEVICE_ARG="-s $1"
fi

echo "📡 BLE Traffic Monitor"
echo "======================"
echo "Shows: TX/RX + Connect/Query events"
echo "Press Ctrl+C to stop"
echo ""

adb $DEVICE_ARG logcat -c
adb $DEVICE_ARG logcat -v time SmartSalesConn:D *:S | grep -E "(TX \[|RX \[|Select peripheral|Discovered BLE|网络查询|BLE Provision|连接|timeout)" --color=auto
