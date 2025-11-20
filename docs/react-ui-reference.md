# React UI Reference (Orchestrator Context)

## Introduction
This reference summarizes the **functional structure**, **data flow**, and **interaction semantics** of your React UI.  
It exists solely to help the Orchestrator plan Android ViewModels, UiState, UiEvents, navigation structure, and backend wiring.

Styling, animations, and layout visuals are out of scope.  
Only *user flows*, *props*, *events*, and *state structure* matter.

---

# 1. Layout.js — Global App Shell

## Purpose
Universal wrapper for all pages.  
Defines the mobile interaction model.

## Core Behaviors (Semantic)
- **Header Bar**
  - Left: menu button → toggles sidebar.
  - Center: dynamic page title based on route.
  - Right: navigation to UserCenter.
- **Sidebar Drawer**
  - Contains:
    - DeviceStatusCard (connection / battery / error)
    - SidebarHistoryList (chat + audio sessions)
    - UserCenter entry.
- **Content Area**
  - Hosts the active Page component (Home, DeviceManager, etc.)

## Android Mapping
- Activity / Scaffold
- Drawer state
- Title binding to current screen
- Global device status monitoring
- Navigation host

---

# 2. Pages (Route-Level Screens → Android Screens)

Each Page corresponds to a **top-level Android screen** and therefore gets its own ViewModel.

---

## 2.1 Home.jsx

### Purpose
Primary interaction hub.  
Contains ChatWelcome, ChatInput, ChatBubble list, device hints, quick actions.

### Key UI States
- Input text  
- Chat messages list  
- Loading state  
- Optional “skills” / quick actions

### Events
- Send message  
- Click quick-skill  
- Scroll to load older messages  
- Refresh device/audio info  

### Android Mapping
`ChatScreenViewModel` manages:
- Message history  
- Sending/streaming  
- Input state  
- Device context for sidebar/status  

---

## 2.2 DeviceSetup.jsx

### Purpose
Initial onboarding of hardware gadget (BLE scan → WiFi provisioning → verification).

### Key UI States
- setupStep  
- deviceList (BLE scan results)  
- wifiCredentials  
- pairingState  
- error states  

### Events
- Scan  
- Select device  
- Enter WiFi credentials  
- Submit pairing request  
- Retry  

### Android Mapping
`DeviceSetupViewModel` handles:
- BLE scanning  
- WiFi config transmission  
- Onboarding state machine  

---

## 2.3 DeviceManager.jsx

### Purpose
Manages connected device files & status.

### Key UI States
- connectionState  
- files[]  
- activeTab  
- selected file  

### Events
- Refresh device  
- Upload file  
- Delete file  
- Apply file to device  

### Android Mapping
`DeviceManagerViewModel` interacts with:
- MediaServerClient  
- DeviceConnectionManager  
- File upload/delete/apply logic  

---

## 2.4 AudioFiles.jsx

### Purpose
Listing, syncing, playing, and applying audio recordings.

### Key UI States
- recordings  
- syncing flag  
- selected audio  

### Events
- Sync  
- Play/Pause  
- Apply  
- Delete  

### Android Mapping
`AudioFilesViewModel` controls:
- Tingwu transcription pipeline  
- OSS/media sync  
- Playback state  

---

## 2.5 ChatHistory.jsx

### Purpose
Displays historical chat sessions.  
Supports selection, rename, delete, pin.

### Key UI States
- sessions[]  
- selectedSession  
- rename state  

### Events
- Open session  
- Rename  
- Pin/unpin  
- Delete  

### Android Mapping
`ChatHistoryViewModel` handles:
- Local DB of sessions  
- Session mutations  

---

## 2.6 UserCenter.jsx

### Purpose
User profile + account settings.

### Key UI States
- user profile  
- tokens  
- feature flags  

### Events
- Logout  
- Save profile  
- Toggle feature  

### Android Mapping
`UserCenterViewModel` (optional at later phase).

---

# 3. Components (Data Shape + Interaction Patterns)

These components inform UiState and backend contract.

---

## 3.1 ChatBubble.jsx

### Data Shape
- role (“user” | “assistant”)  
- content  
- timestamp  

### Events
- Copy  
- Retry  

### Android Impact
Defines `ChatMessage` structure.

---

## 3.2 ChatInput.jsx

### State & Props
- input  
- disabled/loading state  
- quick-skill actions  

### Events
- Send  
- Quick action click  

### Android Impact
Defines `UiState.input` and `UiEvent.SendMessage`.

---

## 3.3 ChatWelcome.jsx

### Behavior
- Shown when message list empty  
- Offers quick actions  

### Android Impact
Controls empty-state composable logic.

---

## 3.4 HistoryItem.jsx

### Data Shape
- sessionId  
- title  
- lastMessage  
- unread  

### Events
- select  
- delete  
- rename  

### Android Impact
Defines session list row schema.

---

## 3.5 AudioDrawer.jsx

### Behavior
- Expandable drawer with audio list  
- Actions: play/pause/apply  

### Android Impact
Maps to modal/sheet UI patterns.

---

## 3.6 DeviceFileItem.jsx

### Data Shape
- fileName  
- size  
- type  
- status (synced/pending/error)  

### Events
- apply  
- delete  
- download  

### Android Impact
Defines file entity model in Android.

---

## 3.7 DeviceStatusCard.jsx

### Data Shape
- deviceId  
- connection state  
- battery  
- wifi strength  
- errors  

### Android Impact
Feeds device banner in global layout.

---

## 3.8 SidebarHistoryList.jsx

### Behavior
- Aggregates HistoryItem rows  
- Offers filter/search  

### Android Impact
Maps to drawer list content.

---

# 4. How the Orchestrator Uses This Document

- Translate React UX flows → Android ViewModel events  
- Define UiState fields for each screen  
- Identify backend touchpoints  
- Maintain architectural consistency  
- Prevent hallucinated UI structures  
- Support scalable evolution of screens & components  

This file is **internal to the Orchestrator only**.  
Codex does **not** consume it unless explicitly instructed.


# 5. UI structure

├── Components
│   ├── audio
│   │   └── AudioDrawer.jsx
│   ├── chat
│   │   ├── ChatBubble.jsx
│   │   ├── ChatInput.jsx
│   │   ├── ChatWelcome.jsx
│   │   └── HistoryItem.jsx
│   ├── device
│   │   └── DeviceFileItem.jsx
│   ├── layout
│   │   ├── DeviceStatusCard.jsx
│   │   └── SidebarHistoryList.jsx
│   └── ui
│       ├── NeumorphicButton.jsx
│       └── NeumorphicCard.jsx
├── Layout.js
└── Pages
    ├── AudioFiles.jsx
    ├── ChatHistory.jsx
    ├── DeviceManager.jsx
    ├── DeviceSetup.jsx
    ├── Home.jsx
    └── UserCenter.jsx

## Document Role in the Integration Workflow
- 保存 React 页/组件的语义（props、事件、状态），供 ORCHESTRATOR 和 EXECUTOR 规划 Android ViewModel / UiState / UiEvent。
- 不记录具体的 HTTP 端点或字段；这些内容分别在 `frontend_inte_plan.md`（流程 + 服务）与 `api-contracts.md`（详细 schema）里维护。
- React 结构如有变动，应同步调整此文件；但整体结构力求稳定，便于跨端协作。
