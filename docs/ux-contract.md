
---

# Assistant UX Contract

> **Purpose**
>
> This document is the **primary UX source of truth** for the Assistant app.
> It defines **layouts, flows, behaviors, and constraints** for the UI.
> It **replaces and archives** the previous `assistant-ux-contract.md`.

* If this contract conflicts with:

  * legacy Android implementation, or
  * archived React UI, or
  * old screenshots / mocks

  → **this contract wins**.

* For visuals (colors, typography, spacing, component shapes) see `style-guide.md`.

* For reasoning, metadata, and analysis flows see `Orchestrator-MetadataHub-Mvp-V3.md`.

* For APIs and data models see `api-contracts.md`.

Any new UX work must either:

* **Fit inside this contract**, or
* **Extend it explicitly** (update this file as part of the change).

If a UX area is **not defined** here, it is considered **open for future specification** and must be documented once a design is chosen.

---

## 1. Boss Rules for UX Work

1. **UX-first, code-second**

   * Product intent and this contract define the UI.
   * Code and tests must be refactored to match the contract, not the other way around.

2. **Concrete instructions, not pseudo-design**

   * This doc specifies *what the user sees and can do* (e.g. “Hamburger opens left history drawer”), not internal implementation details.

3. **Single source of truth**

   * Session naming, exports, and CRM shape are driven by **Orchestrator + MetaHub**.
   * This contract describes **how** the UI applies that data and how users interact with it.

4. **Every UX change updates this doc**

   * Any PR that changes layout, navigation, title behavior, drawers, onboarding, etc. must also update this contract in the same PR.

---

## 2. Global Shells & Navigation

We currently have three primary shells:

1. **Home Chat Shell**

   * Main assistant experience.
   * Contains: top bar, message content, quick skills, input area.
   * Hosts overlays: history drawer, HUD panel.

2. **Audio Library Shell**

   * Manages recordings and transcription status.
   * Entry point for “transcribe” or “analyze this recording” → can create/continue chat sessions.

3. **Device Manager Shell**

   * Shows device connection state (BLE + Wi-Fi) and basic troubleshooting.

Other spaces (user center, subscription, file manager) are **allowed to be placeholders** until explicitly defined here.

---

## 3. Home Chat – Overall Layout

Home is **chat-first**.

Vertical structure:

1. **Top Bar**

   * Navigation + session context, always visible.

2. **Content Area**

   * Empty chat: hero block + quick skills.
   * Active chat: (optional) small session header and message list.

3. **Quick Skill Row + Input Area**

   * Skills + text input + send.

**Forbidden in Home body:**

* “设备管理” / “音频库” cards.
* Global spinners from pull-to-refresh.
* Any extra panels that push messages out of view.

---

## 4. Home Top Bar

### 4.1 Composition & Order

Left → right:

1. **Hamburger icon**

   * Opens **history drawer**.
   * Keeps history test tag (e.g. `HomeScreenTestTags.HISTORY_TOGGLE`).

2. **Session title**

   * Binds to `CurrentSessionUi.title`.
   * Single line, ellipsis overflow.
   * Uses `HomeScreenTestTags.SESSION_TITLE`.

3. **Device connection indicator**

   * Small icon/chip, non-invasive:

     * States: Connected / Connecting / Disconnected.
   * Tap may navigate to Device Manager (when implemented).

4. **HUD dot (optional, debug only)**

   * Small dot icon; no label like “开启调试 HUD”.
   * Toggles debug HUD panel.
   * Uses `debug_toggle_metadata` tag.

5. **“+” new chat icon**

   * Starts a **fresh chat session** (see Session Titles).
   * Uses `home_new_chat_button` tag.

6. **Profile / avatar**

   * Opens User Center (can be placeholder).

### 4.2 Behavior

* Top bar is **pinned**. It never scrolls away and is not affected by keyboard.
* Title changes when:

  * New session is created.
  * Orchestrator/MetaHub suggests a non-placeholder name (one-shot).
  * User manually renames the session.

---

## 5. Session Titles & History

### 5.1 Authority & One-Shot Auto Rename

**Authority chain:**

1. Orchestrator + MetaHub generate **metadata** (`mainPerson`, `summaryTitle6Chars`, etc.).
2. UI applies a derived **suggested title** once per session.

**Lifecycle:**

1. New session starts with a **placeholder**:

   * e.g. `"新的聊天"` (or equivalent default).

2. When metadata is available, Orchestrator/MetaHub proposes a title using:

   ```text
   <major person>_<summary limited to 6 Chinese characters>_<MM/dd>
   ```

   * `major person` from metadata `mainPerson`.

     * If missing, UI uses a neutral fallback like `未知联系人`.
   * `summary limited to 6 Chinese characters` from `summaryTitle6Chars`
     (truncate to at most 6 Chinese characters).
   * `MM/dd` derived from the session’s date.

3. UI only **applies** this suggested title if the current title is still a placeholder.

4. After a non-placeholder title is set (auto or manual), **no further automatic renames** happen for that session.

**Fallback from first AI reply:**

* If some fields are missing in metadata:

  * UI may parse the **first assistant reply** to guess `major person` (e.g. name in summary).
  * If still missing, fall back to safe placeholder strings (`未知联系人`, generic summary).

### 5.2 Manual Rename

* User can rename a session via UI (exact entry point TBD, but:

  * Manual name **overrides** all automatic titles for:

    * Home top bar
    * History drawer list
    * Export filenames
  * Manual rename **does not** modify underlying MetaHub metadata.

* Users **cannot** ask the agent in chat to rename sessions. Rename is controlled only by UI.

### 5.3 History Drawer Content & Visuals

* History drawer slides **from the left** (see Drawers).

* Trigger:

  * Tap hamburger icon.
  * Swipe from left edge beyond open threshold.

* Header:

  * Title: “历史会话”.
  * Close icon at top-right of drawer.

* Items:

  * Sorted by `updatedAtMillis` (desc, newest first).
  * Each card shows:

    * Session title (same logic as Home).
    * Last message preview (single line).
    * Time stamp (formatted per style guide).

* Layout & style:

  * Visual structure follows the provided reference screenshot:

    * Light background, card-like items.
    * Space between recent groups (7 days / 30 days etc.) – these sections are allowed but optional until specified.

### 5.4 Export Filename Pattern

All exported files (PDF, CSV, etc.) use:

```text
<Username>_<major person>_<summary limited to 6 Chinese characters>_<timestamp>.<ext>
```

* `<Username>`: user display name from profile.
* `<major person>` and summary: exactly as in title rules.
* `<timestamp>`: export time, local, formatted `yyyyMMdd_HHmmss`.
  (File-system safe: no slashes.)

---

## 6. Quick Skills & Input Area

### 6.1 Quick Skill Row

Current quick skills:

* `智能分析`
* `生成 PDF`
* `生成 CSV`

**Where they appear:**

1. **Empty chat state**

   * Under hero:

     * LOGO
     * Greeting (“你好，{userName}”)
     * Role line (“我是您的销售助手”)
     * Bullet list of abilities
     * “让我们开始吧”
   * Then **one** quick skill row.

2. **Active chat**

   * In the **input area**, directly above the text field.

**Where they do NOT appear:**

* Not at the top of the message list.
* Not duplicated in any overlay drawer.
* Not in other shells by default (Audio, Device) unless explicitly specified later.

### 6.2 Input Field & Keyboard

* **Send behavior**:

  * Tapping “发送” runs the existing send pipeline, **then immediately dismisses the keyboard** (clearFocus + hide IME).

* **Tap-to-dismiss**:

  * If the input is focused and the user taps anywhere in the chat area **above the skill row**, keyboard is hidden.

* **Pull-down to dismiss**:

  * A downward drag inside the chat content:

    * If IME is open: first drag closes the keyboard.
    * After IME is closed, vertical scroll behaves normally.

* **When typing** (input focused):

  * Device manager and audio overlay **gestures** are disabled (no accidental drawers while typing).
  * Explicit buttons may still navigate if the product later allows it; this doc must be updated in that case.

---

## 7. Audio / Transcription & Smart Analysis

### 7.1 Session Routing

**From existing chat**

* If user triggers **audio analysis** from within an existing chat (e.g. attach recording & ask to analyze):

  * Results stay **in the current session**.
  * Metadata for that recording is associated with this session in MetaHub.

**From Audio Library page**

* If user is on the **audio library** and taps **“转写”** or **“用 AI 分析本次通话”**:

  * A **new session** is created (if not already linked).
  * Session title follows **the same metadata rules** as other sessions.
  * We **do not** rely on “通话分析 – {fileName}” as a permanent title.
    That string is considered a temporary placeholder only, if used at all.

### 7.2 Smart Analysis Output

* SMART_ANALYSIS responses must be rendered as **human-readable markdown**:

  * No raw JSON keys like `"highlights"`, `"actionable_tips"` in user-visible text.
  * Orchestrator already provides structured summaries; UI shows headings, lists, etc.

* This analysis markdown is:

  * The canonical text for **on-screen display**.
  * The input for **PDF export** (no re-analysis).

---

## 8. Export Pipelines

### 8.1 PDF Export

* Takes the **already rendered analysis markdown** from the session.
* Orchestrator pipeline builds the PDF (branding, layout, etc.).
* Filename uses the pattern in **5.4**.
* Export happens in-session and does not change the session content.

### 8.2 CSV Export

* CSV is generated from **metadata/CRM rows** (MetaHub output), not from free-text markdown.
* Columns should align with CRM tools like Salesforce / HubSpot:

  * e.g. `client`, `owner`, `role`, `stage`, `topic`, `next_step`, etc.
* Filename uses the pattern in **5.4**.

---

## 9. Onboarding Flow

On first install / first open:

1. **Welcome**

   * Short explanation of what the assistant does.

2. **Personal Info**

   * Collect:

     * Display name
     * Role/position
     * Industry
   * Stored for:

     * Personalized greetings.
     * Export filename `<Username>` component.
     * MetaHub context (better summaries and CRM inferences).

3. **Device + Wi-Fi Setup (via BLE)**

   * UI asks for:

     * **Wi-Fi name** (as shown in system Wi-Fi list; label it “Wi-Fi 名称”, not “SSID”).
     * **Wi-Fi password** (“Wi-Fi 密码”).
   * This pair is sent via BLE to the device.
   * Device joins the same network.

4. **Return to Home Chat**

   * Once device is connected, user is taken to Home and can start chatting.

User center and subscription areas are **allowed to be placeholders** but navigation hooks should exist.

---

## 10. Drawers & Gestures

We have three major overlays:

1. **History drawer** (Home, left side).
2. **Audio overlay** (audio library).
3. **Device manager overlay**.

### 10.1 Common Gesture Model

For all drawers:

* **Small drag → peek**

  * Short drag moves the drawer slightly (peek state).
  * If user releases within this small range, drawer **snaps back** to its original state.

* **Threshold → commit**

  * Drag beyond a defined threshold (in distance and/or velocity):

    * From closed, dragging open: on release, drawer snaps **fully open**.
    * From open, dragging closed: on release, drawer snaps **fully closed**.
  * No half-open resting positions.

* **Backdrop**

  * Tapping backdrop closes the drawer if it is open.

### 10.2 History Drawer Specifics

* **Side**: from the **left edge**.
* **Open**:

  * Tap hamburger icon in top bar.
  * Swipe from left beyond open threshold.
* **Close**:

  * Tap backdrop.
  * Drag left beyond close threshold.
  * Tap close icon in drawer header.

History layout is described in **5.3**.

### 10.3 Audio & Device Overlays

* Triggered by explicit UI actions (buttons / icons).
* While IME is open (typing):

  * Drawer **gestures** (drag handles, swipe) are disabled.
  * Exact behavior for explicit taps can be decided later; if changed, update this doc.

---

## 11. HUD & Debug Metadata

* HUD is **debug-only** and controlled by configuration (e.g. `CHAT_DEBUG_HUD_ENABLED`).

**Trigger:**

* Small top-bar **dot icon**, no text.
* Toggles boolean `showDebugMetadata`.
* Uses `debug_toggle_metadata` tag.

**Panel:**

* Appears as a **bottom overlay panel**.

* Max height ~40–50% of the screen.

* Internally scrollable.

* Shows:

  * sessionId
  * title
  * mainPerson
  * shortSummary
  * summaryTitle6Chars
  * recent debug notes (parse failures, upsert failures, etc.)

* Contains:

  * **Copy** control: copies all HUD text to clipboard.
  * **Close** control: sets `showDebugMetadata = false`.

* Uses a stable test tag (e.g. `DEBUG_HUD_PANEL`) for instrumentation.

---

## 12. Audio Library & File Manager (High-Level)

### 12.1 Audio Library

* Shows list of recordings:

  * Title (metadata or filename)
  * Duration
  * Transcription status (pending / processing / done).
* Supports:

  * Pull-to-refresh to sync.
  * Actions on each recording:

    * “转写本次通话” (transcribe).
    * “用 AI 分析本次通话” (smart analysis → new or linked session).

### 12.2 File Manager

* Shows uploaded documents/media.
* Requirements:

  * Thumbnails for images/video.
  * Icons for docs.
  * Selecting a file highlights card and shows preview in a **preview/emulator area**.
  * Clicking the card does **not** auto-open a separate modal; preview is controlled explicitly.

Future versions must extend this section when file manager UX is implemented.

---

## 13. History Drawer Extras & Subscription Placeholder

* Bottom of history drawer may contain:

  * A compact **device overview** (connection status, last sync).
  * Tap navigates to Device Manager.
* Subscription / billing:

  * Reserve simple placeholder entries (“订阅”, “账户设置”) in User Center or history drawer.
  * Actual flows to be defined later.

---

## 14. Open UX Surfaces

The following areas are **intentionally open** and must be specified here once designed:

* Detailed User Center layout and flows.
* Subscription / billing UX.
* Full CRM view / contact management screens.
* Advanced analytics dashboards.
* Any additional overlays or modals.

For all such features:

1. First design them under **Boss Rules** (describe interactions and flows).
2. Update this `ux-contract.md`.
3. Only then implement or refactor code/tests.

---

This contract now supersedes the previous UX contract.
Any UX work on the Assistant must respect this document, plus `style-guide.md` and `Orchestrator-MetadataHub-Mvp-V3.md`.