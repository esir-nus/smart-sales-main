# UX 合同说明（v1.2.0）

> **文档版本 / Doc version**  
> - Version: 1.2.0  
> - Last updated: 2025-12-11

> **规则：** 每次 doc-sync 修改本文件，必须同步更新顶部版本号 + 附录 A 变更记录。

---

## Assistant UX Contract

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

* For reasoning, metadata, and analysis flows see `Orchestrator-MetadataHub-V4.md`.

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
   * Tag: `HISTORY_TOGGLE`.

2. **Device status indicator（占位）**

   * Tag: `HOME_DEVICE_INDICATOR`.
   * 当前版本中，设备指示器仅作为占位显示「设备状态」，不展示实时连接/电量/存储信息，也不作为导航入口（点击无效，不跳转 Device Manager）。
   * Future: 待硬件侧 SDK 接入后，在不改变位置与标签的前提下，将展示真实设备连接/电量状态。

3. **Session title**

   * Binds to `CurrentSessionUi.title`.
   * Single line, ellipsis overflow.
   * Uses `HomeScreenTestTags.SESSION_TITLE`.

4. **HUD dot (optional, debug only)**

   * Small dot icon; no label like “开启调试 HUD”.
   * Toggles debug HUD panel.
   * Uses `debug_toggle_metadata` tag.

5. **“+” new chat icon**

   * Starts a **fresh chat session** (see Session Titles).
   * Uses `home_new_chat_button` tag (`NEW_CHAT_BUTTON` in code).

**Note:** 顶部栏不再包含 Profile/用户头像；User Center 入口位于历史抽屉底部的独立行。

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

* History drawer slides **from the left** as a `ModalNavigationDrawer` (see Drawers).

* Trigger:

  * Tap hamburger icon.
  * Swipe from left edge beyond open threshold.

* Content order（top → bottom）:

  * **Device status card（占位）**
    * Tag: `HISTORY_DEVICE_STATUS`.
    * 位置：抽屉最上方。
    * 当前仅显示占位文案 “设备状态”，不展示实时设备数值，也不提供跳转。
    * Future: 待硬件侧接入后，将在此展示真实设备连接、电量、存储等信息，标签与位置保持不变。

  * **Session list / empty state**
    * 排序：先按 pinned（置顶会话在前），再在各自分组内按 `updatedAtMillis` 降序。
    * 展示：会话标题（与 Home 同源的标题管线）、最后一条消息预览（单行）、时间戳（按 style guide）。
    * Tags：`HISTORY_ITEM_PREFIX`（列表项），`HISTORY_EMPTY`（空态文案）。

  * **User Center（底部入口）**
    * 独立行位于抽屉底部，包含人物图标 + 文案（如“个人中心”）。
    * Tag: `HISTORY_USER_CENTER`.
    * 点击进入现有 User Center / Profile 页面。
    * 顶部栏不再提供 Profile/头像按钮，此处为 Home 进入 User Center 的规范入口。

### 5.5 User Center

**Entry point:**

* The only canonical entry is via history drawer footer (`HISTORY_USER_CENTER` tag).
* No top-bar avatar/Profile icon exists in Home top bar.

**User Center content:**

* **Profile editing:**
  * Shows and allows editing: name (displayName, required), role/position (optional), industry (optional).
  * Changes are saved to the same `UserProfileRepository` used by onboarding and Home.
  * Saved profile data is immediately reflected in Home greeting and export filenames.

* **Navigation rows:**
  * Device Manager link – navigates to Device Manager overlay.
  * Privacy/About links (optional placeholders) – reserved for future features.

**Profile fields:**

* **displayName (required):**
  * Used for Home greeting ("你好，{userName}").
  * Used as `<Username>` component in export filenames.
  * Falls back to "用户" or "SmartSales 用户" if empty.

* **role / industry (optional):**
  * Must be persisted and editable in User Center.
  * Considered MetaHub context fields that Orchestrator/LLM can use to improve CRM/summaries later, but are not mandatory for all current flows.

#### 用户中心中的销售画像编辑

- Onboarding 只是**首次**采集 SalesPersona；
- 用户中心的“个人资料 / 销售画像”页面可以修改相同字段（角色 / 行业 / 主要渠道 / 经验 / 风格）；
- 修改后：
  - 后续 Home 智能聊天、SMART 分析、Tingwu 总结都会使用**最新的 Persona** 构造 system prompt；
  - Profile 服务会将最新 Persona 同步到 MetaHub 的 UserMetadata，用于后续人群分析与导出；
  - 既不回写历史 SessionMetadata，也不会由 LLM 修改 Persona。

* Layout & style:

  * 采用全高列布局，背景为卡片风格。
  * 可选的分组间距（7 天 / 30 天等）可保留。

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

### 7.2 GENERAL vs SMART 行为对比

* GENERAL：
  * 按普通助手对话流式展示 Delta → Completed，自然语言为主。
  * 首条回复末尾可有隐藏的 JSON 元数据块，仅供内部解析写入 MetaHub，UI 不展示 JSON。
* SMART_ANALYSIS：
  * 不流式展示 token，发送后先显示“智能分析中…”占位气泡。
  * 完成时一次性替换为 Orchestrator 基于元数据拼好的分析 Markdown 卡片。

### GENERAL 聊天首条回复的状态机

- **状态 A：噪音/问候**
  - 示例：hello、表情、纯数字/乱码。
  - 行为：输出极简自我介绍 + 能力说明 + 引导粘贴真实销售对话/纪要；不做深入分析；仍可输出占位 JSON（未知客户/未命名会话/信息不足）。
- **状态 B：模糊但有销售味道的短语**
  - 示例：`罗总 机械臂采购 合同`、`王总 奥迪 产线扩张`。
  - 行为：明确提示信息不足，列出 2–4 个必问关键信息，给 1–2 句澄清话术；JSON 可保守填写（short_summary 描述“信息不足，有采购线索”，main_person/summary_title_6chars 允许占位）。
- **状态 C：富文本/完整上下文**
  - 示例：粘贴多轮聊天、电话纪要、长邮件。
  - 行为：1 段简洁总括 + 2–4 条关注点/机会/风险/行动；可提示“使用智能分析获取完整结构化摘要”；尽量输出完整 JSON 提炼核心元数据。
- 规则：无论哪种状态，只在本会话的**第一条助手回复**尝试 GENERAL JSON 尾巴，用于命名/摘要；后续 GENERAL 回复仅用于对话，不再负责重命名。深度结构化销售分析由 SMART_ANALYSIS 负责。

### 7.3 Smart Analysis Output

* SMART_ANALYSIS 流程（快捷技能 & “分析一下”）：
  * 发送后先显示本地占位气泡（例如“正在智能分析…”），不流式展示 LLM 增量。
  * 完成时只展示 Orchestrator 本地拼好的 Markdown：概要 / 客户画像与意图 / 需求与痛点 / 机会与风险 / 建议与行动（编号 1..n）/ 核心洞察 / 关键话术。
  * 不展示 JSON / prompt 骨架；若解析失败，显示简短中文提示或清理后的可读文本。

* 该 Markdown：
  * 是屏幕展示的唯一文案。
  * 是 PDF 导出的来源（不重复分析）。

#### SMART 分析卡片语气与长度

- 语气：
  - AI 以「销售助手/同行」身份对**销售同学**说话，不扮演客户，也不扮演销售本人。
  - 默认用“你”称呼当前使用者，或用“销售顾问”做第三人称。
- 长度与结构：
  - 整体感觉：一屏内可读的分析卡，而不是长篇大论。
  - 每个分节：
    - 「会话概要」：1 段 1–2 句总结。
    - 「客户画像与意图」/「需求与痛点」/「机会与风险」/「建议与行动」：每节默认 2–3 条要点，最多 5 条。
    - 「核心洞察」：1–2 条最关键 insight 即可。
  - 若某节完全没有实质内容（仅有“暂无”之类），UI 不展示该标题，防止出现一屏多个空壳小节。
  - 列表编号：
    - 可以使用无序列表（`-`）或有序列表，但最终展示时编号必须是连续的 1..n。
    - 不允许出现明显错误编号（例如 `1 3 4 4`）或流式累积痕迹（如 `11)1)`）。

### 全局 System Prompt 与 Persona 映射

- System prompt 由三块组成：
  1) Persona 块：来自 Onboarding 的角色/行业/渠道/经验/口吻，只影响表达风格和示例形式。
  2) 行为块：定义 GENERAL 三种输入状态的处理策略，要求首条 GENERAL 回复尝试 JSON 尾巴并说明其用途。
  3) 安全/约束块：禁止编造未给出的具体数字/预算/地点，强调长度与重复约束等。
- UI/交互以本 UX 合同 + V4 为准；system prompt 是 LLM 行为约束，需与本文件约定的 persona 与状态机保持一致。

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

On first install / first open, the app shows a minimal onboarding sequence before entering Home:

### 9.1 Current Shipping Onboarding

**Step 1: Welcome**

* Short explanation of what the assistant does.
* CTA button "开始使用" to proceed.

**Step 2: Personal Info**

* Collect:
  * **姓名（必填）** – Display name (required).
  * **职位/角色（可选）** – Role/position (optional).
  * **行业（可选）** – Industry (optional).
* Stored for:
  * Personalized greetings ("你好，{userName}").
  * Export filename `<Username>` component.
  * MetaHub context (better summaries and CRM inferences when available).

### Onboarding 收集的销售画像字段

- 销售岗位/角色（单选）：如销售新人、客户经理、大客户经理、解决方案顾问、售前工程师等。
- 所属行业（单选+“其他”）：如汽车、制造、软件、医疗、教育、金融等。
- 主要沟通渠道（单选）：微信+电话、邮件为主、线下会议为主或混合。
- 经验水平（单选）：新手（<1 年）、有经验（1–5 年）、资深（>5 年）。
- 表达风格偏好（单选）：偏正式商务、偏口语/和同事聊天感。
- 这些字段写入用户 profile，用于构造 LLM system prompt 的 persona 块，影响 GENERAL/SMART 的语气与示例风格，不改变 UI 结构。

**Step 3: Enter Home Chat**

* Once personal info is saved and `hasCompletedOnboarding` is marked `true`, all future launches go straight to Home.
* Onboarding completion state is persisted and checked on app startup.

### 9.2 Device & Wi-Fi Setup (Future Extension)

Device pairing + Wi-Fi 配网 are handled by the existing Device Manager / 配网 flow and are **not blocked behind the onboarding gate** in the current version.

* Users can access device setup via:
  * Device Manager overlay (from Home top bar device indicator).
  * User Center → Device Manager link.
* Future UX may re-introduce tighter coupling between onboarding and device setup, but for now it is explicitly out of the mandatory onboarding sequence.

### 9.3 Testing

Automated UI tests may bypass onboarding by overriding the onboarding completion flag (e.g. `OnboardingStateRepository.testOverrideCompleted`). This is considered allowed per contract and does not affect production behavior.

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

  * A drag of 0–15% of the drawer width is treated as a peek.
  * When the finger is released within this range, the drawer **snaps back** to its previous state.

* **Threshold → commit**

  * Once progress passes ≈15% of the drawer width, releasing the finger causes the drawer to auto-complete to fully open or fully closed (depending on direction), without requiring a full travel swipe.
  * This applies both when opening from closed and when closing from open.
  * No half-open resting positions.

* **Backdrop**

  * Tapping backdrop closes the drawer if it is open.

### 10.2 History Drawer Specifics

* Horizontal drag from the left edge controls the drawer:
  * Drag progress 0–15% of drawer width → peek; on release, snaps back to previous state (closed or open).
  * Drag progress ≥ 15% → on release, commits and snaps fully open (dragging in) or fully closed (dragging back).
* Backdrop tap closes the drawer if it is open.
* gesturesEnabled is false while IME is focused: no drawer drag while typing, but tapping the hamburger still opens/closes the drawer.

History layout is described in **5.3**.

### 10.3 Audio & Device Overlays

* Triggered by explicit UI actions (buttons / icons).
* Vertical overlay stack gestures（Home center, Audio down, Device up）:
  * Vertical drag 0–15% of screen height → peek, return to previous overlay on release.
  * Vertical drag ≥ 15% → on release, commit to switching overlay（Home ↔ Audio ↔ Device）.
  * As with the drawer, overlay drag gestures are disabled while IME is focused; explicit navigation/handles still work once the keyboard is dismissed.
* While IME is open (typing):
  * Drawer/overlay **gestures** (drag handles, swipe) are disabled.
  * Exact behavior for explicit taps can be decided later; if changed, update this doc.
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
Any UX work on the Assistant must respect this document, plus `style-guide.md` and `Orchestrator-MetadataHub-V4.md`.

---

## 附录 A：变更记录（仅供追溯，非规范）

> ⚠️ 本附录仅用于追溯 UX 文案/规则的演进，**不作为实现依据**。实际行为以正文为准。

### v1.2.0（2025-12-11）

- 扩展用户中心销售画像编辑说明：Onboarding 仅为首次采集，用户中心可修改 Persona 字段；修改后影响后续 AI 行为并同步到 MetaHub UserMetadata。

### v1.1.0（2025-12-10）

- 补充 SMART 分析卡片的语气与长度规范（助手→销售顾问视角、一屏内可读、每节 2–3 条要点）。
- 明确编号/列表展示要求（编号连续、避免流式累积痕迹）。

### v1.0.0

- 初版 UX 合同，定义 Home 布局、会话标题、快捷技能、历史抽屉等核心交互。

### v1.1.1（2025-12-10）

- Onboarding 增补销售画像字段（角色/行业/渠道/经验/口吻），用于 persona prompt。
- GENERAL 首条回复状态机：噪音/模糊/富文本三类输入的响应与占位 JSON 尾巴要求。
- 新增全局 System Prompt 组成：persona 块、行为块（GENERAL 状态机+JSON 尾巴）、安全/约束块。
