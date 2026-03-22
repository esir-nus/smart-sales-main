# Onboarding Flow

> **Definitions**: Uses [GLOSSARY.md](../GLOSSARY.md)
> **Type**: Flow (multi-step user journey)
> **Status**: ✅ Shipped
> **Source**: Extracted from prism-ui-ux-contract.md L416-608

---

## Overview

High-fidelity "unboxing" journey. Priorities: Hardware reliability, account binding, and confidence building.

---

## The Golden Thread (12 Phases)

| Phase | Name | Description |
|-------|------|-------------|
| 1 | **Welcome** | Brand hero screen ("Your AI Sales Coach") |
| 2 | **Permissions** | Glass-card priming before native Mic/BLE dialogs |
| 3 | **Voice Handshake** | "Getting to know you" via phone mic |
| 4 | **Hardware Wake** | Manual: "Turn on device, hold button 3s until blue light" |
| 5 | **Scan (Radar)** | High-fidelity pulse animation |
| 6 | **Found (Manual Select)** | **NO Auto-Connect**. User MUST tap device card |
| 7 | **WiFi Setup** | Prompt for SSID and Password via BLE |
| 8 | **FW Check & Update** | Force-update if FW < Min version |
| 9 | **Device Naming** | Personalized naming (e.g., "Frank's Badge") |
| 10 | **Account Gate** | "Bind Device" to save setup to cloud |
| 11 | **Profile Collection** | Name, Role, Industry, Notes |
| 12 | **Complete** | Success checkmark, transition to Home |

---

## Phase Details

### Phase 1: Welcome

```
┌─────────────────────────────────────────────────┐
│          ( Aurora Animation: Slow Pulse )       │
│                                                 │
│       [ LOGO ]                                  │
│       SmartSales                                │
│       您的 AI 销售教练                          │
│                                                 │
│       [ 开启旅程 ] (Solid White Button)         │
└─────────────────────────────────────────────────┘
```
> No login/signup buttons. Clean entry.

---

### Phase 2: Permissions

```
┌─────────────────────────────────────────────────┐
│  ┌───────────────────────────────────────────┐  │
│  │ 麦克风权限                                  │  │
│  │ 为了分析您的销售对话...                    │  │
│  │ [ 允许访问 ]                               │  │
│  └───────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────┐  │
│  │ 蓝牙权限                                    │  │
│  │ 为了连接 SmartBadge...                     │  │
│  │ [ 允许访问 ]                               │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

---

### Phase 3: Voice Handshake

```
┌─────────────────────────────────────────────────┐
│       让我们先认识一下                           │
│       试着说："你好，帮我搞定这个客户"           │
│                                                 │
│       ||||||||||||||||||||||||                  │
│       ( Waveform reacts to PHONE mic )          │
│                                                 │
│       ( Upon Voice Detect ) -> AI Response      │
│       "好的，先告诉我客户的情况..."             │
│       [ 继续 ]                                   │
└─────────────────────────────────────────────────┘
```

---

### Phase 4: Hardware Wake

```
┌─────────────────────────────────────────────────┐
│        [ DEVICE ANIMATION: Finger Press ]       │
│        ( 3... 2... 1... Blink! )                │
│                                                 │
│        启动您的 SmartBadge                       │
│        长按中间按钮 3 秒，直到蓝灯闪烁           │
│                                                 │
│        [ 灯已经在闪了 ]                          │
└─────────────────────────────────────────────────┘
```

---

### Phase 5: Scan

```
┌─────────────────────────────────────────────────┐
│       ((      (      )      ))                  │
│          正在搜索设备...                         │
│       [ 取消 ]                                  │
└─────────────────────────────────────────────────┘
```

---

### Phase 6: Device Found (Manual Connect)

```
┌─────────────────────────────────────────────────┐
│  ┌───────────────────────────────────────────┐  │
│  │ 📱 SmartBadge (Frank's)           -42dBm  │  │
│  │ ID: FF:23:44:A1                           │  │
│  │ [ 连接 ]                                  │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```
> **Critical**: NO Auto-Connect. User MUST tap to confirm intent.

---

### Phase 7: WiFi Setup

```
┌─────────────────────────────────────────────────┐
│       配置网络 — 让徽章独立工作                  │
│                                                 │
│       [ WiFi 名称 (SSID)       ]                │
│       [ 密码                   ]                │
│                                                 │
│       [ 连接网络 ]   [ 跳过 ]                    │
│                                                 │
│    ⚠️ 跳过后，徽章录音需通过蓝牙手动同步          │
└─────────────────────────────────────────────────┘
```

---

### Phase 8: FW Check & Update

```
┌─────────────────────────────────────────────────┐
│          正在检查固件版本...                     │
│          v1.0.2 -> v1.2.0 (必需)                │
│                                                 │
│          [=====================>    ] 75%       │
│          请勿关闭设备                            │
└─────────────────────────────────────────────────┘
```

---

### Phase 9: Device Naming

```
┌─────────────────────────────────────────────────┐
│       给它起个名字                               │
│       [ Frank's Badge          ]                │
│       [ 确定 ]                                  │
└─────────────────────────────────────────────────┘
```

---

### Phase 10: Account Gate

```
┌─────────────────────────────────────────────────┐
│  保存您的设置                                   │
│  登录以绑定 "Frank's Badge"                     │
│                                                 │
│  [ 邮箱/手机号        ]                         │
│  [ 密码               ]                         │
│                                                 │
│  [     登录并绑定     ]                         │
│                                                 │
│  没有账号？ [ 立即注册 ]                        │
└─────────────────────────────────────────────────┘
```

---

### Phase 11: Profile Collection

```
┌─────────────────────────────────────────────────┐
│       让我更好地帮助你                           │
│       (可跳过，后续可完善)                        │
│                                                 │
│  🎙️ [ 按住说话介绍自己 ]                         │
│            或者                                 │
│  ┌───────────────────────────────────────────┐  │
│  │ 我是 [李明]，负责 [政企大客户销售]...        │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
│  [ 完成 ]       [ 稍后完善 ]                     │
└─────────────────────────────────────────────────┘
```

---

### Phase 12: Complete

```
┌─────────────────────────────────────────────────┐
│          ( Success Checkmark Animation )        │
│          一切就绪！                              │
│          ( Auto-navigates to Home )             │
└─────────────────────────────────────────────────┘
```

---

## Key Principles

| Rule | Description |
|------|-------------|
| **Manual Gate** | "Device Detected" requires user tap to connect |
| **Persistence** | Badge-to-Account binding on successful handshake |
| **Dev-Doorway** | "Rocket" button resets `isOnboarding` and `pairedState` |
