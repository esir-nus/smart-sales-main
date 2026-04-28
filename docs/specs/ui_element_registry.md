# Prism UI Element Registry

> **Status:** Active / Governance
> **Version:** 2.6 (Draft)
> **Last Updated:** 2026-04-01
> **Authority:** This document is a supporting registry for shared interaction, layering, and reusable UI behavior invariants. Current base-runtime surface behavior is owned first by `docs/core-flow/base-runtime-ux-surface-governance-flow.md`.

Standalone-mode note:

- Product-level app identity, major surfaces, and anti-drift UX laws start in [`SmartSales_PRD.md`](../../SmartSales_PRD.md).
- This registry governs shared interaction and layering invariants.
- It does not own the exact shell chrome for standalone modes such as SIM.
- It does not own feature-level product behavior or shell-specific source of truth.
- When a standalone implementation narrows or simplifies shell presentation, keep this registry focused on reusable interaction rules and route active shell truth through `docs/core-flow/base-runtime-ux-surface-governance-flow.md` plus the owning lower feature docs.

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

Rule for SIM:

- SIM may simplify shell chrome and support-surface presentation while still obeying this Z-map.
- SIM-specific prompt/chip or support-panel visuals must not change ownership, scrim, or drawer exclusivity rules defined here.

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

### 1.2 Global Top Safe-Area Rule

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Default Top-Reaching Surface** | `Visible` | Auto layout | None | Content begins below top safe band | Must use `status inset + 16dp blank band + content`; the blank band stays empty. | ✅ Verified |
| **Explicit Header / Monolith Surface** | `Visible` | Auto layout | None | Header occupies top slot | Header content must clear native status icons; no extra default blank band is implied. | ✅ Verified |

---

## 2. Home Screen (Base Layer)

Home empty-state composition note:

- The exact current `UX.HOME.*` surface behavior is owned first by `docs/core-flow/base-runtime-ux-surface-governance-flow.md`.
- Local `HomeShell` / `ChatWelcome` refinement is owned by `docs/cerb-ui/home-shell/spec.md`.
- The current center-header Dynamic Island renderer behavior is owned by `docs/cerb-ui/dynamic-island/spec.md`.
- This registry should only be used here for shared trigger, drawer, and layer invariants.
- Historical shell affordances that are not present in the owning home-shell spec must not be reintroduced by inference.
- Rows below may describe reusable or legacy home-surface triggers; the owning home-shell spec decides which controls are currently mounted on the empty-state shell.

### 1.5 Onboarding (5-Wave Host Split)
| Element | Visual | Interaction | Microcopy | Result | Status |
|---------|--------|-------------|-----------|--------|--------|
| **Welcome** | Dark monolith intro | Tap primary CTA | `您的 AI 销售教练` | Advances to primer in `FULL_APP` only. | Verified |
| **Permissions Primer** | Frosted cards | Tap `继续` | microphone / Bluetooth / exact alarm guidance | Explains needs without firing native prompts early. | Verified |
| **Voice Handshake** | Abstract waveform | Wait ~1.5s then tap continue | operational listening confirmation | Advances to hardware wake in `FULL_APP` only. | Verified |
| **Hardware Wake** | Frosted badge slate | Tap `蓝灯已经在闪了` | `长按中间按钮 3 秒` | Enters scan. | Verified |
| **Scan** | Technical radar | Cancel or wait for result | `正在搜索设备` | Requests Bluetooth permission at point-of-use, then emits found/error. | Verified |
| **Device Found** | High-fidelity device card | Tap `手动连接` | MAC + dBm | Manual connect only, never auto-connect. | Verified |
| **Provisioning** | Glass form + linear progress | Submit Wi-Fi, retry, or back | `配置网络` | Runs pairing/write-through/network check inside one presentation seam. | Verified |
| **Complete** | Shared success wrapper | Tap host CTA | `一切就绪！` | `FULL_APP` enters home; `SIM_CONNECTIVITY` enters manager first; `SIM_ADD_DEVICE` closes back to connectivity-owned add-device flow. | Verified |

### 1.6 User Center (Settings Blueprint)
| Element | Visual | Interaction | Microcopy | Result | Status |
|---------|--------|-------------|-----------|--------|--------|
| **Profile Card** | Centered hero | Tap `Edit` | "Name / Position / metadata chips" | Edit Screen. | ✅ Verified |
| **Preferences** | List | Tap | "Theme / AI Lab / Message Notifications" | `Theme` opens a persisted `Dark / Light / System` selector; AI Lab remains a toggle setting. | ✅ Verified |
| **Storage** | Two rows | Tap `Clear` | "Used Space / Clear Cache" | Clears Cache. | ✅ Verified |
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
| **Dynamic Island** | `One-Line Summary` | Tap | Ripple | Opens **Scheduler Drawer**. | Sticky top slot, single-line only, horizontal overflow only. | ✅ Verified |
| **Debug Toggle [🐞]** | `Off` | Tap | Tint | **Legacy Debug**. | Beta only. | ✅ Verified |
| **New Session [➕]** | `Idle` | Tap | Ripple | 1. Clear Context.<br>2. New UUID. | Reset Coach. | ✅ Verified |
| **Tingwu Menu [≣]** | `Idle` | Tap | Slide In | Opens **Tingwu Drawer**. | Exclusive. | ❌ Pending |
| **Tingwu Menu [≣]** | `Idle` | Tap | Slide In | Opens **Tingwu Drawer**. | Exclusive. | ❌ Pending |
| **Artifacts [📦]** | `Has Items` | Tap | Slide In | Opens **Artifacts Drawer**. | Exclusive. | ❌ Pending |
| **Scheduler Trigger** | `Any` | Drag from top activation band / top handle | Slide In | Opens **Scheduler**. | Narrow header-edge band, protected center scroll, vertical-intent lock, distance or fling confirmation; shipped SIM top band is 88dp tall. A first-launch-only teaser may auto-open the drawer once for discoverability, but it must not repeat on later launches. | ✅ Verified |
| **Audio Trigger** | `Any` | Drag from bottom activation band / bottom affordance | Slide In | Opens **Audio Sheet**. | Narrow bottom-edge strip, protected center scroll, vertical-intent lock, distance or fling confirmation; shipped SIM strip is 28dp tall and must not overlap composer hit targets. | ✅ Verified |

### 2.2 Input & Modes

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Mode Toggle** | `Coach` | Tap 'Analyst' | Slide + **Haptic** | Theme->Blue. `intent=ANALYST`. | **NO Nav**. | ✅ Verified |
| **Input Bar** | `Idle` | Tap/Focus | Slide Up | Nav to **Chat Screen**. | Preserve Mode. In SIM, the empty idle state uses one shimmering inline hint line at a time, rotating across `输入消息...`, audio-library swipe-up guidance, and attachment guidance; do not stack multiple hint rows. | ✅ Verified |
| **Attachment [📎]** | `Idle` | Tap | Ripple | System Picker or SIM audio selector. | Generic chat may open picker; grounded SIM audio chat reopens Audio Drawer selector. | 🚧 In-Progress |
| **Audio Upload** | `Picked` | Confirm | Progress | Sync to **Audio Drawer**. | `storage` folder. | ❌ Pending |
| **Mic FAB** | `Idle` | Tap | Morph | **Phone Mic** Capture. | Not Badge. | ✅ Verified |
| **Audio Card** | `Star` | Tap | `spring(0.9, 500)` | **Star Toggle** | Heart/Star flip, color change. Refined motion contract: `docs/specs/modules/AudioDrawer.md` §R.5. | ✅ Verified |
| **Audio Card** | `PENDING` | Swipe L→R | `aurora-chip` | **Transcribe** | Aurora chip "→ transcribe" + static hint. Replaces the shimmer prompt per `AudioDrawer.md` §R.4. | 🚧 Refining |
| **Audio Card** | `TRANSCRIBING` | System | `LinearProgress` (aurora gradient) | **Processing** | Progress bar + numeric % label. No "正在转写..." text. See `AudioDrawer.md` §R.4. | 🚧 Refining |
| **Audio Drawer (Select Mode)** | `Opened from Chat` | Tap attach/upload in grounded chat | Bottom sheet `spring(0.9, 500)` | Open audio selector | No swipe hints, no bottom CTA. | ❌ Pending |
| **Audio Card (Select Mode)** | `TRANSCRIBED` | Tap card | Ripple + aurora edge-bar | Bind current chat to selected audio | Show truncated transcript preview for recognition; current selection signaled by 2px aurora edge-bar (no pill). | ❌ Pending |
| **Audio Card (Select Mode)** | `PENDING/TRANSCRIBING` | Tap card | Ripple | Bind current chat and continue processing in chat | Compact row-body copy plus progress communicates continued chat-side processing. | ❌ Pending |

---

## 3. History Drawer (Left Side)

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Drawer Panel** | `Closed` | Trigger [☰] | Slide `spring(Low)` | Opens full-app history drawer. | Scrim covers Home; no edge-swipe open. | ✅ Verified |
| **Device Capsule** | `Connected / Reconnecting / Offline` | Tap | Ripple | Opens Connectivity Modal. | Lives inside the drawer header seam. | ✅ Verified |
| **Session Group Card** | `Expanded / Collapsed` | Tap header | Rotate chevron + expand/collapse | Shows grouped sessions. | Collapsible groups remain local to drawer. | ✅ Verified |
| **Session Item** | `Idle` | Tap | Ripple | Load Session and close drawer. | Visible overflow remains the primary action entry. | ✅ Verified |
| **Session Actions** | `Overflow Open` | Tap `⋮` | Menu open | Exposes `Pin / Rename / Delete`. | Long-press may remain as supplemental entry only. | ✅ Verified |
| **Footer Profile / Settings** | `Idle` | Tap | Ripple | Opens User Center. | Both footer entries hand off to the same overlay seam. | ✅ Verified |

---

## 4. Scheduler Drawer (Top)

> **Tracking System**: `Spec` = Intent, `Code` = Reality  
> **Statuses**: `✅ Impl` | `🚧 Partial` | `❌ Missing` | `⚔️ Conflict` (Human review needed)

### 4.1 Conflict Card (Golden Sample)

| Element | State | Animation | Spec | Code | Link |
|---------|-------|-----------|------|------|------|
| **Conflict Card** | `Collapsed` → Tap | `spring(LowBouncy)` expand | Required | ✅ Impl | [SchedulerCards.kt:L180](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerCards.kt#L180) |
| **Conflict Card** | `Expanded` | **Breathing Red Tint** | Required | ❌ Missing | — |
| **Actions** | `Resolved` | **Fade Out** animation | Required | ❌ Missing | — |
| **All Cards** | `Swipe L→R` | `spring` delete | Required | ✅ Impl | [SchedulerTimeline.kt:L141](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerTimeline.kt#L141) |
| **Task Card** | `Reschedule` | Slide Out (L/R) + Fade | Required | ✅ Impl | [SchedulerTimeline.kt:L88](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerTimeline.kt#L88) |

### 4.2 Timeline & Calendar

| Element | State | Animation | Spec | Code | Link |
|---------|-------|-----------|------|------|------|
| **Handle** | `Week` → `Month` | Expansion | Required | ✅ Impl | [SchedulerCalendar.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerCalendar.kt) |
| **Task Checkbox** | `Pending` → `Done` | Cross-fade | Required | ✅ Impl | [SchedulerCards.kt:L62](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerCards.kt#L62) |
| **Inspiration** | `Idle` → Tap | Multi-Select Mode | Required | ✅ Impl | [SchedulerViewModel.kt:L46](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerViewModel.kt#L46) |
| **Date Cell (New Task)** | `Glow` | `infiniteTransition` 2s breathing | Required | ✅ Impl | [SchedulerCalendar.kt:L240](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/drawers/scheduler/SchedulerCalendar.kt#L240) |

### 4.3 Small Attention Flow (Path A)

| Element | State | Animation | Spec | Code | Lnk |
|---------|-------|-----------|------|------|------|
| **Red Flag Card** | `Vague Task` | **Breathing Red Border** | Required | ❌ Missing | — |
| **Caution Banner** | `Conflict` | Slide Down (Inline Header) | Required | ❌ Missing | — |
| **Inspiration Note** | `Timeless` | **Distinct Note Stylization** | Required | ❌ Missing | — |

---

## 5. Audio Drawer (Bottom)

> **Visual & motion authority**: `docs/specs/modules/AudioDrawer.md` §R (Refined Visual & Motion Contract). The rows below capture interaction invariants only; chrome and spring values live in §R.

| Element | Visual State | Trigger | Animation | Result | Invariant | Status |
|---------|--------------|---------|-----------|--------|-----------|--------|
| **Drawer Handle** | `Peeking` | Drag Up | `spring(0.9, 500)` | Expands 50%/100%. | Fade Content. Critically damped, no overshoot per `AudioDrawer.md` §R.5. | ✅ Verified |
| **Waveform** | `Recording` | Audio In | Hertz | Visualizes **Phone Mic**. | Phone Source. | 🚧 In-Progress |
| **Audio List** | `Syncing` | Event | Aurora-dot pulse on sync-pill | Mirror `storage`. | Badge + Local. Sync state encoded by aurora dot color (mint/blue/red), not pill chrome. | ❌ Pending |
| **Audio Card** | `Idle` | Play | Toggle | Playback. | Stop others. | ❌ Pending |
| **Audio Card** | `Holding for Resume` | Active badge not READY while badge file is waiting in DOWNLOADING/QUEUED, including post-switch and disconnect recovery windows | Slow alpha pulse 1500ms reverse on dim bar | Automatic recovery uses filename suffix `(恢复中)` and label `恢复中，等待徽章自动重连…`; manual disconnect uses `(等待手动重连)` and `等待手动重连`; generic hold keeps `(等待恢复)` / `等待恢复传输…`. Dim pulsing bar replaces fast indeterminate scroll. | Mutually exclusive with active-download styling; never inherits success-green surface or border tint; phone-source audio does not enter badge recovery copy. | 🚧 In-Progress |
| **Audio Card** | `Non-Transcribed` | Tap | Opacity dip 120ms | **Rejects Expansion**. | Replaces shake keyframes per `AudioDrawer.md` §R.5. | ❌ Pending |
| **Audio Card** | `Transcribed` | Tap | Expand `spring(0.9, 500)` | Opens Hub. | Hub fades in with 4px Y-translation. | ❌ Pending |
| **Audio Card** | `问AI` | Tap | Navigate | **Creates/Opens Analyst Session** | Session binding. Aurora glass `ask-ai` button with "✧ Ask AI" glyph. | ✅ Verified |
| **Audio Drawer** | `Select Mode` | Chat attach/upload | Slide Up `spring(0.9, 500)` | Opens `选择要讨论的录音`. | Static picker; no swipe or expand affordances. | ❌ Pending |
| **Audio Card** | `Select / Current` | Visible in selector | Aurora edge-bar (left, 2px) | Remains current discussion audio. | Inline `当前讨论中 · …` in summary; no pill chrome per `AudioDrawer.md` §R.4. | ❌ Pending |
| **Audio Card** | `Select / Transcribed` | Tap | Ripple + edge-bar slide-in 180ms | Switch chat to this audio immediately. | Compact header plus truncated transcript preview; no dedicated button or status pill. | ❌ Pending |
| **Audio Card** | `Select / Pending` | Tap | Ripple | Switch chat and continue processing. | Compact row-body copy explains continued processing in chat; no source badge chrome. | ❌ Pending |

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
| **Plan Card** | `Visible` | Analyst | Slide Down | Rendered in chat. | **Part of Chat History** (not sticky). | ❌ Pending |
| **Plan Card** | `Parsing` | System | Shimmer | Showing current parsing task (e.g., "Reading PDF..."). | Ticker updates. | ❌ Pending |
| **Plan Card** | `Building` | System | Expand | Full plan revealed (Goals, Highlights, Deliverables). | User types to select. | ❌ Pending |
| **Step Item** | `Pending` | Tool Start | Spinner | Active Execution. | N/A | ❌ Pending |
| **Step Item** | `Prompting` | **Logic Wait** | Flash Yellow | **Prompt User** via Chat. | "Should I gen PDF?" | ❌ Pending |
| **Step Item** | `Completed` | Tool Success | Checkmark | Step Done. | N/A | ❌ Pending |

### 6.2 Agent Activity Banner (Two-Tier Structure)

> **Design Principle**: Agent activity uses a **two-tier hierarchy** for rich showcasing of agent thinking. This is a core differentiator.

> **Position**: The AgentActivityBanner renders **inline in the chat history** and **persists** as a permanent record of agent cognition. When the agent finishes thinking, the response bubble appears as a **separate, following message** (not a replacement). This creates a visible trail: User → ThinkingBox → AI Response.

#### Visual Structure

```
┌─────────────────────────────────────────────────────────┐
│  Phase: 📝 规划分析步骤                                  │  ← Primary (what high-level task)
│  Action: 🧠 思考中...                                    │  ← Secondary (specific operation)
├─────────────────────────────────────────────────────────┤
│  [Thinking trace / transcript / memory hits...]         │  ← Body (streaming content)
└─────────────────────────────────────────────────────────┘
```

#### Phase Types (ActivityPhase)

| Phase | Icon | Microcopy | When |
|-------|------|-----------|------|
| `PLANNING` | 📝 | "规划分析步骤" / "生成执行计划" | Analyst mode plan generation |
| `EXECUTING` | ⚙️ | "执行工具: {toolName}" | Running a tool (chart, report, etc.) |
| `RESPONDING` | 💬 | "生成回复" | Standard response generation |
| `ERROR` | ⚠️ | "发生错误" | Error state |

#### Action Types (ActivityAction)

| Action | Icon | Microcopy | Trace Source |
|--------|------|-----------|--------------|
| `THINKING` | 🧠 | "思考中..." | Qwen3-Max native CoT stream |
| `PARSING` | 📄 | "解析中... ({filename})" | Qwen-VL vision output / OCR |
| `TRANSCRIBING` | 🎙️ | "转写中... ({filename})" | Tingwu transcript stream |
| `RETRIEVING` | 📚 | "检索记忆..." | Relevancy Library matches |
| `ASSEMBLING` | 📋 | "整理上下文..." | Assembled context preview |
| `STREAMING` | ✨ | "生成中..." | LLM response stream |

#### Trace Sources (Native vs Synthetic)

| Source | Type | Details |
|--------|------|---------|
| **Qwen3-Max CoT** | Native | `enable_thinking=true` → `reasoning_content` (Analyst: full) |
| **Qwen-Plus CoT** | Native | `enable_thinking=true` → `reasoning_content` (Coach: truncated 3 lines) |
| **Qwen-VL CoT** | Native | `enable_thinking=true` → `reasoning_content` (Vision: OCR trace) |
| **Tingwu** | Native | Real-time transcript (pseudo-thinking) |
| **Relevancy Library** | Synthetic | Show matched entities/memories |
| **Context Assembly** | Synthetic | Show assembled sources |

#### Component Signature

```kotlin
data class AgentActivity(
    val phase: ActivityPhase,        // Required: PLANNING, EXECUTING, etc.
    val action: ActivityAction?,     // Optional: THINKING, PARSING, etc.
    val trace: String? = null        // Optional: Streaming content
)

@Composable
fun AgentActivityBanner(
    phase: ActivityPhase,            // Required: always visible
    action: ActivityAction?,         // Optional: visible if present
    trace: List<String>?,            // Optional: body content
    modifier: Modifier = Modifier
)
```

#### Rendering Rules

| Scenario | Visual Output |
|----------|---------------|
| `action = null, trace = null` | Phase only (e.g., "⚠️ 网络连接失败") |
| `action = THINKING, trace = null` | Phase + Action, no body |
| `action = THINKING, trace = [...]` | Phase + Action + scrollable trace |

#### State Matrix

| Element | Phase | Action | Trace | Invariant | Status |
|---------|-------|--------|-------|-----------|--------|
| **Planning** | `PLANNING` | `THINKING` | CoT stream | Scroll-to-bottom | ❌ Pending |
| **Tool (PDF)** | `EXECUTING` | `THINKING` | Tool's CoT | Updates in place | ❌ Pending |
| **Vision** | `EXECUTING` | `PARSING` | OCR results | Per-file progress | ❌ Pending |
| **Audio** | `EXECUTING` | `TRANSCRIBING` | Transcript | Per-file progress | ❌ Pending |
| **Memory** | `RESPONDING` | `RETRIEVING` | Entity matches | Optional detail | ❌ Pending |
| **Context** | `RESPONDING` | `ASSEMBLING` | Source list | Brief summary | ❌ Pending |
| **Error** | `ERROR` | `null` | Error message | Title only | ❌ Pending |

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
| **v2.9** | 2026-01-30 | **Draft** | Refactored §6.2 to two-tier structure (Phase + Action + Trace) for rich agent activity showcase. |
| **v2.8** | 2026-01-29 | Locked | Unified §6.2 Thinking Box/Ticker into `AgentActivityBanner` (title + optional trace). |
| **v2.7** | 2026-01-29 | Locked | Added Analyst Flow States (Parsing, Building), Thinking Ticker, Artifact Card. |
| **v2.6** | 2026-01-28 | **Locked** | Added Z-Map (Lego Layers). |
| **v2.5** | 2026-01-28 | Archived | Tracker Added. Plan Logic Fixed (Prompt vs Artifact). |
| **v2.4** | 2026-01-28 | Archived | Spec-Mirror Edition. |
