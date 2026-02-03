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
│       [ 连接设备 (Reconnect) ]                   │
│       (Primary/Blue)                            │
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
| `disconnected` | ⚪ Grayscale | Reconnect |
| `updating` | ⬇️ Download icon | Sync Now |
| `wifi_mismatch` | ⚠️ Alert | Update Config, Ignore |
