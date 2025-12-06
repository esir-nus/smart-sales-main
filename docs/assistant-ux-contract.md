# AI Assistant – UI/UX Contract (Boss Rules)

> This document is the **single source of truth for UX behavior and layout** for the AI Assistant app. It describes principles and contracts, not pixel-perfect mocks. When there is a conflict, **this doc wins over all other UX sources** (React `/ui`, legacy screenshots, tests, etc.).

---

## 0. Priority & Relationships

1. **Priority order for UI/UX decisions**
   1. This document (UX contract, “boss rules”).
   2. Live Compose implementation + recent UX review notes.
   3. `style-guide.md` (visual language only: colors, typography, spacing).
   4. Backend / orchestrator docs (only for data & flows).
   5. Archived React `/ui` (inspiration only, never binding).

2. **What this doc does**
   - Defines **high-level interaction patterns, layout principles, and navigation contracts**.
   - Captures **one-time automatic behaviors** (e.g., auto session naming).
   - Records **important UX decisions over time** in a short decision log.

3. **What this doc does *not* do**
   - It does not dictate exact component trees, tag names, or test details.
   - It does not freeze individual screens; UX can evolve as long as it stays within these principles.
   - It does not redefine typography, color, or iconography—that lives in `style-guide.md`.

---

## 1. North-star Experience

- The app is a **chat-first sales assistant**:
  - Primary surface is a conversational thread.
  - Everything else (device manager, audio library, history, debug HUD) is **secondary and peripheral**.
- The user should always feel:
  - **Safe to experiment** (no surprising navigation jumps).
  - **In control** of context (which session, which recording, what’s being analyzed).
  - **Uninterrupted** while they are typing or reviewing an answer.

---

## 2. Global Layout & Navigation Principles

1. **Single Home Shell**
   - There is **one Home chat shell** containing:
     - Top app bar (navigation + session title + key actions).
     - Message area (hero + chat list).
     - Input area (quick skills + text box + send).
   - Device Manager and Audio Library live in their own **pages or drawers**, not as big cards inside the chat stream.

2. **Top App Bar**
   - Left: **history entrypoint** (hamburger or equivalent).
   - Center: **current session title** (bound to the active conversation, not a fixed “AI 助手” label).
   - Right:
     - **HUD toggle** (simple dot or subtle icon; only visible when debug is enabled).
     - **“New chat” action** (e.g., “+” icon) that starts a fresh session in the same shell.
     - **Profile / user entrypoint**.

3. **History Drawer**
   - Opens from the side over Home; does **not** replace the Home shell.
   - Each row shows: title, short preview, and time.
   - Selecting a row:
     - Switches the active session.
     - Closes the drawer and keeps the user inside the same Home shell.

---

## 3. Conversation & Session Principles

1. **Session identity**
   - A “session” is a long-lived conversation between the user and the assistant.
   - Home always operates on **exactly one active session** at a time.

2. **Automatic session naming (one-time)**
   - New sessions start with a **placeholder title** (e.g., “新的聊天” or “通话分析 …”).
   - Orchestrator/MetaHub can suggest a better title once there is enough context.
   - **One-time rename rule**:
     - If the title is still a placeholder, we may auto-rename it from metadata / content.
     - Once the user renames a session manually, we **never overwrite** it automatically.

3. **Session list vs top bar**
   - **Top bar title and history list title must always match** for the active session.
   - Any title change (auto or manual) updates:
     - The session repo.
     - The current session header.
     - The history drawer entry.

4. **Transcription / call analysis sessions**
   - Treated as normal sessions with extra context (e.g., “通话分析” tag).
   - Call analysis summary is displayed as **chat messages**, not a separate screen.
   - Auto naming for these sessions follows the same **one-time rename** rule.

---

## 4. Interaction Principles

1. **Quick skills**
   - Quick skills are **shortcuts for structured tasks** (e.g., “智能分析”, “生成 PDF”, “生成 CSV”).
   - Placement:
     - Visible in the **empty state hero** (to start something quickly).
     - Visible as a **single row above the input** in active chats.
   - They **do not** create separate pages; they always act within the current session.

2. **Keyboard / IME**
   - When the keyboard is open:
     - **Top app bar stays visible** and interactive.
     - Content and input area resize (adjustResize), no panning of the entire shell.
     - Downward drag in the chat area and tapping above the skill row are valid ways to **dismiss the keyboard**.
     - Device/Audio drawers and other heavy gestures are **temporarily disabled** to avoid accidental navigation.
   - When the keyboard closes, all drawers and gestures return to normal.

3. **Debug HUD**
   - HUD is a **developer/ops tool**, never a user feature.
   - Toggle:
     - Single icon/dot in the top bar when enabled by feature flag.
     - No explicit “开启/关闭调试 HUD” text in the main UX.
   - Panel:
     - Bottom overlay, height-capped, scrollable.
     - Shows structured metadata (session id, summary, main person, etc.).
     - Always includes **Copy** and **Close** actions.

4. **Overlays & drawers**
   - Overlays (history, device, audio, HUD) are **lightweight layers over Home**.
   - They:
     - Do not reset the current session.
     - Respect the “typing lock” (input focus can temporarily block them).
     - Close with back, tap on backdrop, or dedicated close control.

---

## 5. Design Process (“Boss Rules” Workflow)

1. **Text-first UX**
   - We define screens and interactions in **clear text descriptions** first.
   - That description is treated as the **primary spec** for Codex/implementation.

2. **Incremental refactor**
   - We refactor **one area/page at a time** (e.g., Home shell, history drawer, audio overlay).
   - Each refactor:
     - Starts with a **short, high-level analysis** against this contract.
     - Ends with a **note in the decision log** (section 6).

3. **Tests follow UX**
   - When UX changes are intentional, **tests are updated to match the new contract**, not vice versa.
   - Tests should assert:
     - Presence of key actions (history, HUD dot, new chat, quick skills).
     - Correct session title behavior.
     - Basic keyboard and overlay behaviors.

---

## 6. Decision Log (high-level)

> Use this as a running list of important UX decisions. Each entry should be 3–5 lines max.

- **2025-12-xx – Home shell chat-first**
  - Removed device/audio cards from Home body; moved them to dedicated overlays/pages.
  - Home now shows: hero + skills (empty) or messages + skills + input (non-empty).

- **2025-12-xx – Session auto-naming**
  - Introduced one-time automatic title suggestion from orchestrator/MetaHub.
  - Auto rename only applies while title is a placeholder; user titles are never overwritten.

- **2025-12-xx – HUD simplification**
  - HUD trigger changed from text button to dot icon; panel added copy/close controls.
  - HUD is only visible when debug flag is enabled.

- **2025-12-xx – Home shell polish**
  - 顶栏改为：历史入口 + 动态会话标题 + HUD 点 + 新建对话 + 头像。
  - Home 主体只保留聊天/技能/输入，不再展示设备/音频卡片。
  - 历史抽屉显示标题/预览/时间，HUD 面板保留复制与关闭动作。

*(Add future decisions here as we refine the UX.)*
