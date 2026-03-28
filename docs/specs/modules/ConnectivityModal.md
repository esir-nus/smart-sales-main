# Connectivity Modal Module

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L610-671

---

## Overview

Modal triggered by tapping the **[📶] Device State** icon in the header. Displays SmartBadge connection status and pairing controls.

---

## State Machine

### State A: Connected (Live Status)

```
┌─────────────────────────────────────────────────┐
│              [ ✕ Close ]                        │
│                                                 │
│       [🟢 3D Badge Visual (Pulse) ]             │
│            ( ID: 8842 • 🔋 85% )                │
│                                                 │
│         SmartBadge Pro                          │
│         v1.2.0                                  │
│                                                 │
│  [ ⚡ 断开连接 ]    [ 🔄 检查更新 ]              │
│  (Disconnect)       (Primary/Blue)              │
└─────────────────────────────────────────────────┘
```

### State B: Disconnected (Offline)

```
┌─────────────────────────────────────────────────┐
│              [ ✕ ]                              │
│       [⚪ Badge Visual (Grayscale) ]            │
│            🔴 离线 (Offline)                    │
│                                                 │
│       [ 重试连接 (Reconnect) ]                   │
│       (Primary/Blue)                            │
└─────────────────────────────────────────────────┘
```

### State B2: BLE Paired, Network Pending

```
┌─────────────────────────────────────────────────┐
│              [ ✕ ]                              │
│       [🔵 Bluetooth Icon ]                      │
│              已连接设备                          │
│      蓝牙已连接，正在确认设备网络状态              │
│                                                 │
│       [ 重试连接 (Reconnect) ]                   │
└─────────────────────────────────────────────────┘
```

### State B3: BLE Paired, Network Offline

```
┌─────────────────────────────────────────────────┐
│              [ ✕ ]                              │
│       [🔵 Bluetooth Icon ]                      │
│              已连接设备                          │
│     蓝牙已连接，但设备当前未接入可用网络           │
│     请确认设备附近已开机，并检查徽章 Wi‑Fi 状态    │
│                                                 │
│       [ 重试连接 (Reconnect) ]                   │
└─────────────────────────────────────────────────┘
```

### State C: Firmware Update

```
┌─────────────────────────────────────────────────┐
│              [ ⬇️ Icon ]                        │
│         发现新版本 v1.3                          │
│         包含重要安全修复                         │
│                                                 │
│       [ 立即同步 (Sync Now) ]                    │
│       (Triggers: 下载中... -> 安装中...)          │
└─────────────────────────────────────────────────┘
```

### State D: WiFi Mismatch

> Triggered if Badge WiFi creds != Phone WiFi after BLE Reconnect.
> Also used when firmware reconnect succeeds over BLE but the app has no exact remembered credential for the phone's current Wi‑Fi.
> Submitting `更新配置` immediately enters reconnect/progress state and runs the Wi‑Fi repair flow automatically.
> Manual repair returns to this screen only for a proven submitted-SSID mismatch, not for generic “badge still offline” confirmation failures.
> Closing the panel clears the transient repair override so later reopen reflects live connection state rather than stale mismatch UI.

```
┌─────────────────────────────────────────────────┐
│              [ ⚠️ WiFi Alert ]                  │
│       网络环境已变更                             │
│       检测到徽章 WiFi 与当前网络不匹配             │
│                                                 │
│       [ Input: WiFi Name (SSID) ]               │
│       [ Input: Password         ]               │
│                                                 │
│       [ 更新配置 (Update) ]  [ 忽略 ]            │
└─────────────────────────────────────────────────┘
```

---

## Design Principle

> A "truncated onboarding" view. Allows users to manage badge pairing without entering the full setup flow. Essential for hardware-first object permanence.

---

## Component States

| State | Badge Visual | Actions |
|-------|--------------|---------|
| `connected` | 🟢 Pulse animation | Disconnect, Check Updates |
| `disconnected` | ⚪ Grayscale | Retry Reconnect |
| `ble_paired_network_unknown` | 🔵 Bluetooth icon | Retry Reconnect; Debug builds may also show Disconnect |
| `ble_paired_network_offline` | 🔵 Bluetooth icon | Retry Reconnect; Debug builds may also show Disconnect |
| `updating` | ⬇️ Download icon | Sync Now |
| `wifi_mismatch` | ⚠️ Alert | Update Config, Ignore |

## Manager-Only Richer State Rule

- The connectivity manager/modal may show BLE-held but network-not-ready diagnostics.
- Shared shell routing must continue to use the stricter transport-ready contract from `ConnectionState`.
- `NeedsSetup` and explicit bridge errors must not be replaced by paired/offline manager hints.
- Any temporary disconnect affordance inside BLE-held states is debug-only test tooling and must not be used as a release contract.
- Deterministic reconnect rule: if reconnect finds `IP#0.0.0.0`, the app first tries silent credential replay using an exact remembered match for the phone's current SSID; only when no exact match exists, or replay proves the badge is still on another network, should the modal stay on `wifi_mismatch`.
- Manual Wi‑Fi repair rule: once the user submits SSID/password from `wifi_mismatch`, the manager must switch to reconnect/progress immediately instead of waiting for a second explicit retry tap.
- Submitted-credential rule: manual repair confirmation must validate against the submitted SSID rather than the phone's current SSID and must not return to `wifi_mismatch` unless the badge proves it came online on another SSID.
- Close-reset rule: closing the connectivity modal/manager cancels any in-flight reconnect/repair attempt and clears only transient UI override state; the next reopen must use current manager truth from `ConnectivityBridge.managerStatus`.
