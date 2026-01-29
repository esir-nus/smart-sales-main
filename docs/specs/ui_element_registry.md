# Prism UI Element Registry (Atomic Source of Truth)

> **Status:** Active / Governance
> **Version:** 2.6 (Draft)
> **Authority:** This document governs ALL app behaviors. Code must match these specs exactly.

---

## 0. The Z-Map (Lego Layers)

> **Authority**: This Z-Map defines the immutable 3D constitution of the app.
> **Rule**: Components MUST explicitly declare their `PrismElevation` layer if they are not standard flow content.

| Layer | ID (Z) | Description | Components |
|-------|--------|-------------|------------|
| **System** | `6.0` | Global Overlays | Connectivity Modal, Toasts, Dialogs |
| **Handles** | `5.0` | **Always Touchable** | Audio Handle, Scheduler Handle |
| **Drawer** | `4.0` | Sliding Panels | History, Scheduler, Audio, Tingwu |
| **Scrim** | `3.0` | Focus Dimmer | Black alpha 0.4 overlay |
| **Chrome** | `2.0` | Floating UI | FABs, Floating Headers, Badges |
| **Content** | `1.0` | Standard UI | Chat Bubbles, Lists, Keep Content |
| **Floor** | `0.0` | Background | App Background, Wallpapers |

---

## 🏗️ Interaction Schema (The TAR Model)

Every element definition follows this strict logic:
- **Visual State**: What the user sees *before* interaction.
- **Trigger**: The exact user input (Input Event).
- **Animation**: The physics or transition curve (Feedback).
- **Result**: The logic state change or navigation (Outcome).
- **Invariant**: Hard rules that must never be broken (Guardrails).
- **Status**: Implementation State (`✅ Verified`, `🚧 In-Progress`, `❌ Pending`).

---

## 1. Global Interaction Rules

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Drawer Logic** | `Any Open` | Open New Drawer | **Atomic Snap** | 1. `old.expanded=false`<br>2. `new.expanded=true` | Only 1 drawer open. | ✅ Verified |
| **Back Gesture** | `Any` | Swipe Left | System Back | Close Drawer OR Pop Stack | Context preserved. | ✅ Verified |
| **Scrim** | `Visible` | Tap Scrim | Fade Out | Close Drawer. | Alpha `0.4`. | ✅ Verified |

### 1.1 System Notifications (Contract §4)

| Notification | Type | Trigger | Content | Invariant | Status |
|--------------|------|---------|---------|-----------|--------|
| **Clarification** | `Warn` (Amber) | Ambiguous Entity | `⚠️ 需要澄清: [Entity]` | Opens Picker. | ❌ Pending |
| **Conflict Spot** | `Warn` (Amber) | Schedule Overlap | `⚠️ 发现冲突: [A] vs [B]` | Opens Conflict Card. | ✅ Verified |
| **Relevancy Found** | `Info` (Blue) | Library Hit | `✨ 发现关联: [Entity]` | Opens Entity Card. | ❌ Pending |
| **Task Created** | `Success` (Green) | Voice Cmd | `✅ 日程已创建` | Opens Calendar. | ❌ Pending |

---

## 2. Home Screen (Base Layer)

### 1.5 Onboarding (V15 Full Spectrum)
| Element | Visual | Interaction | Microcopy | Result | Status |
|---------|--------|-------------|-----------|--------|--------|
| **Welcome** | Aurora | Tap `Start` | "您的 AI 销售教练" | Starts Permissions. | ✅ Verified |
| **Permissions** | Glass Card | Tap `Allow` | "🎙️ 麦克风权限" | Sys Dialog -> Next. | ✅ Verified |
| **Handshake** | Waveform | Voice | "帮我搞定这个客户" | AI Reply -> Next. | ✅ Verified |
| **Hardware** | Anim | Long Press | "长按中间按钮" | User verifies LED. | ✅ Verified |
| **Scan/Found** | Radar/Card | Tap `Connect` | "Found... -42dBm" | Pairs Device. | ✅ Verified |
| **WiFi Setup** | Form | Input | "SSID / Password" | Device Clones WiFi. | ✅ Verified |
| **FW Update** | Progress | Auto | "v1.0 -> v1.2" | Block until 100%. | ✅ Verified |
| **Account** | Login Form | Input | "登录以绑定" | Context Saved. | ✅ Verified |

### 1.6 User Center (Settings Blueprint)
| Element | Visual | Interaction | Microcopy | Result | Status |
|---------|--------|-------------|-----------|--------|--------|
| **Profile Card** | Avatar/Text | Tap `Edit` | "Name / Position" | Edit Screen. | ✅ Verified |
| **Preferences** | List | Tap | "Theme / AI Lab" | Toggle Setting. | ✅ Verified |
| **Storage** | Row | Tap `Clear` | "Used: 120MB" | Clears Cache. | ✅ Verified |
| **Security** | Row | Tap | "Change Password" | Nav Flow. | ✅ Verified |
| **Footer** | Button | Tap | "Log Out" | Ends Session. | ✅ Verified |

### 2. Drawers (Auxiliary)

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **History Trigger [☰]** | `Idle` | Tap | Ripple | Opens **History Drawer**. | No Edge Swipe. | ✅ Verified |
| **Device Status [📶]** | `Connected` | Tap | Ripple | Opens **Connectivity Modal**. | Fullscreen Overlay. | ✅ Verified |
| **Device Status [📶]** | `Connected` | Tap | Ripple | Opens **Connectivity Modal**. | Fullscreen Overlay. | ✅ Verified |
| **Connectivity Modal** | `Connected` | Auto | Pulse | Shows Battery/ID. | Mutex (Atomic). | ✅ Verified |
| **Connectivity (WiFi)** | `Mismatch` | Auto | Alert | Manual SSID/Pwd. | Reconnect Edge Case. | ✅ Verified |
| **Session Title** | `Read` | Tap | Ripple | Edit Mode. | Autosave. | ✅ Verified |
| **Debug Toggle [🐞]** | `Off` | Tap | Tint | **Legacy Debug**. | Beta only. | ✅ Verified |
| **New Session [➕]** | `Idle` | Tap | Ripple | 1. Clear Context.<br>2. New UUID. | Reset Coach. | ✅ Verified |
| **Tingwu Menu [≣]** | `Idle` | Tap | Slide In | Opens **Tingwu Drawer**. | Exclusive. | ❌ Pending |
| **Tingwu Menu [≣]** | `Idle` | Tap | Slide In | Opens **Tingwu Drawer**. | Exclusive. | ❌ Pending |
| **Artifacts [📦]** | `Has Items` | Tap | Slide In | Opens **Artifacts Drawer**. | Exclusive. | ❌ Pending |
| **Scheduler Trigger** | `Any` | Drag Handle (Top) | Slide In | Opens **Scheduler**. | Visible Pill. | ✅ Verified |
| **Audio Trigger** | `Any` | Drag Bottom Zone (120dp) | Slide In | Opens **Audio Sheet**. | Invisible target. | ✅ Verified |

### 2.2 Input & Modes

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Mode Toggle** | `Coach` | Tap 'Analyst' | Slide + **Haptic** | Theme->Blue. `intent=ANALYST`. | **NO Nav**. | ✅ Verified |
| **Input Bar** | `Idle` | Tap/Focus | Slide Up | Nav to **Chat Screen**. | Preserve Mode. | ✅ Verified |
| **Attachment [📎]** | `Idle` | Tap | Ripple | System Picker. | **Max 11**. | ✅ Verified |
| **Audio Upload** | `Picked` | Confirm | Progress | Sync to **Audio Drawer**. | `storage` folder. | ❌ Pending |
| **Mic FAB** | `Idle` | Tap | Morph | **Phone Mic** Capture. | Not Badge. | ✅ Verified |
| **Audio Card** | `Star` | Tap | `spring` | **Star Toggle** | Heart/Star flip, color change. | ✅ Verified |
| **Audio Card** | `PENDING` | Swipe L→R | `Shimmer` | **Transcribe** | "右滑开始转写 >>>" prompt. | ✅ Verified |
| **Audio Card** | `TRANSCRIBING` | System | `LinearProgress` | **Processing** | "正在转写..." + progress bar. | ✅ Verified |

---

## 3. History Drawer (Left Side)

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Drawer Panel** | `Closed` | Trigger [☰] | Slide `spring(Low)` | Expands 85%. | Scrim covers Home. | 🚧 In-Progress |
| **Session Item** | `Idle` | Tap | Ripple | Load Session. | Highlight Current. | ❌ Pending |
| **Sessions** | `List` | Scroll | Kinetic | Visual List. | Grouped (Today). | ❌ Pending |

---

## 4. Scheduler Drawer (Top)

### 4.1 Conflict Card (Golden Sample)

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Conflict Card** | `Collapsed` | Tap Body | `spring(LowBouncy)` | `isExpanded=true`. Reveal Chat. | Red Tint. | ✅ Verified |
| **Conflict Card** | `Expanded` | Chatting | **Breathing Red Tint** | Updates via NL. | Negotation. | ✅ Verified |
| **Actions** | `Resolved` | System | Fade Out | Atomic API Calls. | DB Update. | ✅ Verified |
| **All Cards** | `Swipe L->R` | Swipe | `spring` | **Delete/Dismiss** | "Cleaner" Gesture (**Collapsed Only**). | ✅ Verified |
| **Task Card** | `Input` | Chat | `CircularProgress` | **Reschedule** | Card **Slides Out** (LEFT=past, RIGHT=future, `FastOutSlowInEasing` 350ms) + Fade → Toast. | ✅ Verified |

### 4.2 Timeline & Calendar

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Handle** | `Week` | Drag/Tap | Expansion | **Month View** (5 Rows). | Object Permanence. | ✅ Verified |
| **Task Checkbox** | `Pending` | Tap | Cross-fade | `isDone=true`. | Undo Toast. | ✅ Verified |
| **Inspiration** | `Idle` | Tap | Ripple | **Multi-Select Mode**. | No Nav. | ✅ Verified |

---

## 5. Audio Drawer (Bottom)

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Drawer Handle** | `Peeking` | Drag Up | `spring(Low)` | Expands 50%/100%. | Fade Content. | ✅ Verified |
| **Waveform** | `Recording` | Audio In | Hertz | Visualizes **Phone Mic**. | Phone Source. | 🚧 In-Progress |
| **Audio List** | `Syncing` | Event | Pulse | Mirror `storage`. | Badge + Local. | ❌ Pending |
| **Audio Card** | `Idle` | Play | Toggle | Playback. | Stop others. | ❌ Pending |
| **Audio Card** | `Non-Transcribed` | Tap | Shake | **Rejects Expansion**. | "Transcribe First". | ❌ Pending |
| **Audio Card** | `Transcribed` | Tap | Expand | Opens Hub. | N/A | ❌ Pending |
| **Audio Card** | `问AI` | Tap | Navigate | **Creates/Opens Analyst Session** | Session binding. | ✅ Verified |
| **Source Badge** | `Static` | N/A | N/A | Local (Phone) vs Cloud (Badge). | **Below Star** (No Overlap). | ❌ Pending |

---

## 6. Chat Interface

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Thinking Box** | `Folded` | **Deep Think** | Accordion | Reveal Stream. | API Flag. | ❌ Pending |
| **User Bubble** | `Sent` | N/A | Slide In | Appears (Optimistic). | Right Align. | ❌ Pending |
| **System Bubble** | `Streaming` | Token | Typewriter | Append Text. | Left Align. | ❌ Pending |

### 6.1 Plan Card (Analyst - Plan Once)

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Plan Card** | `Visible` | Analyst | Slide Down | Sticky Top Header. | Persistent. | ❌ Pending |
| **Plan Card** | `Parsing` | System | Shimmer | Showing current parsing task (e.g., "Reading PDF..."). | Ticker updates. | ❌ Pending |
| **Plan Card** | `Building` | System | Expand | Full plan revealed (Goals, Highlights, Deliverables). | User types to select. | ❌ Pending |
| **Step Item** | `Pending` | Tool Start | Spinner | Active Execution. | N/A | ❌ Pending |
| **Step Item** | `Prompting` | **Logic Wait** | Flash Yellow | **Prompt User** via Chat. | "Should I gen PDF?" | ❌ Pending |
| **Step Item** | `Completed` | Tool Success | Checkmark | Step Done. | N/A | ❌ Pending |

### 6.2 Thinking Box (Analyst Cognition)

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Thinking Box** | `Folded` | **Deep Think** | Accordion | Reveal Stream. | API Flag. | ❌ Pending |
| **Thinking Ticker** | `Active` | Analyst Input | Typewriter | Streams perception (e.g., "Reading Page 3/12"). | Organic delays. | ❌ Pending |
| **Thinking Trace** | `Streaming` | Token | Append | Qwen Max reasoning steps. | Scroll to bottom. | ❌ Pending |

### 6.3 Artifact Card (Tool Output)
| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Artifact Card** | `Preview` | Tool Done | Slide In | Shows thumbnail/summary of output. | N/A | ❌ Pending |
| **Artifact Card** | `Preview` | Tap `Full View` | Nav | Opens full-screen viewer. | N/A | ❌ Pending |
| **Action Buttons** | N/A | Tap `Download` | System | Invokes native download. | N/A | ❌ Pending |
| **Action Buttons** | N/A | Tap `Share` | System | Invokes native share sheet. | N/A | ❌ Pending |

---

## 7. Version History

| Version | Date | Status | Changes |
|---------|------|--------|---------|
| **v2.7** | 2026-01-29 | **Draft** | Added Analyst Flow States (Parsing, Building), Thinking Ticker, Artifact Card. |
| **v2.6** | 2026-01-28 | **Locked** | Added Z-Map (Lego Layers). |
| **v2.5** | 2026-01-28 | Archived | Tracker Added. Plan Logic Fixed (Prompt vs Artifact). |
| **v2.4** | 2026-01-28 | Archived | Spec-Mirror Edition. |
