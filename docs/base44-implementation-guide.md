Below is the **final, consolidated, production-ready Implementation Guide**, including the **Orchestrator Addendum**, your full Base44-generated content, and cleanup for correctness and future maintainability.

You can copy/paste this into your repo as:

> `docs/web-ui-implementation-guide-base44.md`

Everything inside is role-contract-safe and ready for long-term use.

---

# **SmartSales Web UI – Implementation Guide (Base44 Version)**

### **with Orchestrator Addendum**

---

## 🔒 **Orchestrator Addendum — How This Document Must Be Used**

**This addendum overrides every other section in this file.**

### ✔ Purpose

This document is **not** a product spec, **not** a UX spec, and **not** a source of backend truth.
It is **only** a structural map of the **Base44-generated React UI** in `~/ui`.

### ✔ Who may use this document?

* **Orchestrator** → YES.
  Use it to:

  * plan integration tasks
  * reference file paths & component trees
  * identify stub handlers that need wiring
  * avoid hallucinations and scope drift

### ❌ Who should not treat this as truth?

* **Codex** → NO.
  Codex must follow:

  1. Android ViewModels
  2. api-contracts.md
  3. PRD
     **The Base44 guide is only a map of the UI code.**

* **Operator** → NO.
  Operator relies on Android implementation & PRD for correct behavior.

### ✔ If this guide disagrees with Android or API contracts:

**Android + api-contracts always win.**
Update this guide afterward.

---

# **SmartSales Web UI – Implementation Guide (Base44 Version)**

This document provides an integration guide for wiring the Base44-generated React UI (`~/ui`) into the real SmartSales backends and device APIs.
All content below reflects *only the existing UI code* and is not a UX spec.

---

# **1. File / Route Structure Overview**

The UI is organized around individual route pages and shared components.

## **Top-Level Routes**

| Route            | File                            | Description                                                                                 |
| ---------------- | ------------------------------- | ------------------------------------------------------------------------------------------- |
| `/`              | `ui/src/pages/Home.js`          | Main app entry, includes vertical overlays for AudioFiles and DeviceManager + chat surface. |
| `/devicemanager` | `ui/src/pages/DeviceManager.js` | Device media browsing (images, videos, GIFs) with inline simulator.                         |
| `/devicesetup`   | `ui/src/pages/DeviceSetup.js`   | BLE discovery, setup, and configuration flow.                                               |
| `/audiofiles`    | `ui/src/pages/AudioFiles.js`    | Audio recordings list + sync/transcription pipeline + transcript viewer.                    |
| `/chathistory`   | `ui/src/pages/ChatHistory.js`   | List of past chat sessions with pin/rename/delete.                                          |
| `/usercenter`    | `ui/src/pages/UserCenter.js`    | User profile & settings entry.                                                              |

## **Shared Layout**

```
ui/src/Layout.js
```

* Wraps app pages
* Provides mobile header and sidebar
* Sidebar includes DeviceStatusCard + History list

---

# **2. Per-page Component Responsibilities**

## **Home (`pages/Home.js`)**

Home
├─ AudioFilesPage (overlay)
├─ DeviceManagerPage (overlay)
└─ HomeView (chat UI)
  ├─ ChatWelcome
  ├─ ChatBubble
  └─ ChatInput

### Home Responsibilities:

* Middle layer in the vertical overlay stack
* Manages drag gesture (`pageIndex` = -1 / 0 / +1)
* Shows chat interface

---

## **DeviceManager (`pages/DeviceManager.js`)**

DeviceManagerPage
├─ NeumorphicCard
├─ NeumorphicButton
└─ DeviceFileItem

### Responsibilities:

* Show unified media list (image/video/GIF)
* No timestamps; videos show duration
* Inline simulator preview of selected file
* Refresh, apply-to-device, delete
* Connection-aware UI (connecting / disconnected / connected)

---

## **AudioFiles (`pages/AudioFiles.js`)**

AudioFilesPage
├─ AudioRecordingCard
└─ AudioDrawer

### Responsibilities:

* Show audio recordings (local + cloud)
* Show sync + transcription status
* Per-recording actions: Sync, Transcribe, View Transcript
* Transcript viewer bottom sheet
* Ask AI button → navigation to Chat context

---

## **ChatHistory (`pages/ChatHistory.js`)**

ChatHistoryPage
├─ HistoryItem
└─ NeumorphicCard

### Responsibilities:

* List grouped chat histories
* Search
* pin / rename / delete (as implemented in UI)

---

## **UserCenter (`pages/UserCenter.js`)**

* Displays user profile information
* Navigates to other settings areas

---

# **3. State & Event Integration Points**

This section lists **only stub handlers in the Base44 UI** that need real backend integration.

## **HomeView**

| Handler      | Purpose              | Integration                        |
| ------------ | -------------------- | ---------------------------------- |
| `handleSend` | Sends a chat message | Replace with DashScope chat API    |
| `loadUser`   | Fetch user profile   | Replace with real user profile API |

---

## **DeviceManagerPage**

| Handler         | Current Behavior (Stub)       | Real Integration                |
| --------------- | ----------------------------- | ------------------------------- |
| `loadFiles`     | returns mock media list       | Device HTTP media list endpoint |
| `handleConnect` | makes fake connection         | BLE/Wi-Fi provisioning APIs     |
| `handleRefresh` | re-runs `loadFiles`           | real refresh endpoint           |
| `handleApply`   | marks file is_applied locally | device “apply media” endpoint   |
| `handleDelete`  | removes file from UI only     | delete media file endpoint      |

---

## **AudioFilesPage**

| Handler                           | Stub Behavior           | Real Integration                               |
| --------------------------------- | ----------------------- | ---------------------------------------------- |
| `loadRecordings`                  | returns mock recordings | GET /audio-recordings                          |
| `handleSingleRecordingSync`       | updates local status    | DeviceSyncRepository.sync(...)                 |
| `handleSingleRecordingTranscribe` | updates local status    | OSS upload → Tingwu create → poll              |
| `handleViewTranscript`            | opens drawer            | ok as is                                       |
| `handleAskAI`                     | navigate with params    | Build TranscriptionChatRequest + route to Chat |
| `handleGlobalSync`                | fake bulk sync          | Optional; check backend support                |
| `handleGlobalTranscribe`          | fake bulk transcription | Optional; check backend support                |

---

# **4. Data Shapes Expected by UI**

These reflect actual usage in `~/ui`.

## **DeviceMediaItem**

```ts
interface DeviceMediaItem {
  id: number;
  filename: string;
  type: 'image' | 'video' | 'gif';
  url: string;
  is_applied: boolean;
  duration?: string; // videos only
}
```

## **AudioRecording**

```ts
interface AudioRecording {
  id: string;
  filename: string;
  created_date: string;
  duration: string;
  source: 'phone' | 'cloud';
  sync_status: 'NotSynced' | 'Syncing' | 'Synced' | 'Error';
  transcription_status: 'None' | 'Uploading' | 'Transcribing' | 'Ready' | 'Error';
  transcript_preview?: string;
  full_transcript?: string;
}
```

## **ChatMessage**

```ts
interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
}
```

## **ChatHistoryItem**

```ts
interface ChatHistoryItem {
  id: string;
  date: string;
  person: string;
  theme: string;
  pinned: boolean;
}
```

---

# **5. Special Interaction Patterns (Evidence-Based)**

## **Vertical Overlays (Home)**

* Controlled by `pages/Home.js`
* `pageIndex` controls which layer is visible
* Dragging Home up → DeviceManager overlay
* Dragging Home down → AudioFiles overlay

## **Inline Simulator (DeviceManager)**

* Controlled in `pages/DeviceManager.js`
* `selectedFile` determines preview
* Preview embedded on page (no fullscreen viewer)

## **Transcript Viewer + Ask AI Flow**

* Controlled by `pages/AudioFiles.js` + `components/audio/AudioDrawer.js`
* View Transcript → bottom drawer
* Ask AI → navigation to Chat with audio context

---

# **6. Integration Checklist**

### **Layout**

* No backend integration needed

### **Home**

* Replace LLM stub with DashScope streaming endpoint
* Parse `audioId` + `context=analyze` parameters to pre-fill chat
* Replace base44.auth.me with actual profile API

### **DeviceManager**

* Wire `loadFiles` to device HTTP
* Wire `handleApply` to device “display media” API
* Wire `handleDelete` to device “delete media” API
* Align connection states with Android connection manager

### **AudioFiles**

* Wire `loadRecordings` to backend audio list
* Wire sync handlers to device sync API
* Wire transcription handlers to OSS + Tingwu
* Inject real `transcript_preview` and `full_transcript`

### **ChatHistory**

* Replace mock history with backend history API
* Wire rename/pin/delete if backend supports them

### **UserCenter**

* Replace base44.me() with real profile endpoint

---

# **End of Document**

Let me know when you want me to produce:

* The **Orchestrator addendum only**
* A **crosswalk table** mapping UI ↔ Android ViewModel ↔ API
* Or the **T-task batch** to integrate each page.
