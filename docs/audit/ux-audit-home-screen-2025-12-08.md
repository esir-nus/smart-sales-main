# UX Audit Report: Home Chat Shell, Title Pipeline, HUD, and History Drawer

**Branch:** `UX-polishment`  
**Commit:** `180a6449`  
**Date:** 2025-12-08  
**Scope:** Home chat shell, title pipeline, HUD, and history drawer

---

## UX Rules (from docs)

From `docs/ux-contract.md` and `docs/Orchestrator-MetadataHub-Mvp-V3.md`:

1. **Top bar composition:** Left → right: hamburger icon (opens history drawer, tag `HISTORY_TOGGLE`), session title (binds to `CurrentSessionUi.title`, tag `SESSION_TITLE`), device connection indicator, HUD dot (debug only, tag `debug_toggle_metadata`), "+" new chat button (tag `home_new_chat_button`), profile/avatar. Title is single line with ellipsis overflow.

2. **Top bar title source:** Title binds to `CurrentSessionUi.title` (not hard-coded "AI 助手"). Title changes when: new session created, Orchestrator/MetaHub suggests non-placeholder name (one-shot), user manually renames.

3. **Session title lifecycle:** New session starts with placeholder (e.g., "新的聊天"). When metadata available, Orchestrator/MetaHub proposes title using `<major person>_<summary limited to 6 Chinese characters>_<MM/dd>`. UI only applies if current title is still placeholder. After non-placeholder title set, no further automatic renames.

4. **Home body layout:** Chat-first. Empty chat: hero block + quick skills. Active chat: message list. **Forbidden:** "设备管理"/"音频库" cards, global spinners, extra panels pushing messages out of view.

5. **Hero display rules:** Hero appears only when current session has no messages or transcripts. Once session becomes active, hero never renders. Hero is not a chat bubble and never appears in scrollable message list.

6. **Quick skills placement:** Empty chat: under hero, one quick skill row. Active chat: in input area, directly above text field. Never at top of message list, never duplicated, never in other shells.

7. **History drawer:** Slides from left. Trigger: tap hamburger icon or swipe from left edge. Header: "历史会话" with close icon at top-right. Items sorted by `updatedAtMillis` desc. Each card shows: session title (same logic as Home), last message preview (single line), timestamp. Uses `HISTORY_PANEL` tag, empty state uses `HISTORY_EMPTY` tag.

8. **HUD behavior:** Debug-only, controlled by `CHAT_DEBUG_HUD_ENABLED`. Trigger: small top-bar dot icon (no text), tag `debug_toggle_metadata`, toggles `showDebugMetadata`. Panel: bottom overlay, max height ~40–50% screen, internally scrollable, shows sessionId/title/mainPerson/shortSummary/summaryTitle6Chars/notes, contains copy and close controls, uses stable tag `DEBUG_HUD_PANEL`.

9. **Input/keyboard behavior:** Send button runs send pipeline then immediately dismisses keyboard. Tap-to-dismiss: if input focused and user taps chat area above skill row, keyboard hidden. Pull-down to dismiss: downward drag closes keyboard first, then scrolls normally. When typing (input focused): device/audio overlay gestures disabled.

10. **History drawer gestures:** Small drag → peek, threshold → commit. Backdrop tap closes drawer. Drawer gestures disabled when IME is focused, but hamburger tap behavior preserved.

---

## Findings by Rule

### 1. Top bar composition

**MISMATCH – Top bar title hard-coded** – `HomeScreen.kt:812` – The top bar shows hard-coded text `"AI 助手"` instead of binding to `state.currentSession.title`. The `HomeTopBar` composable receives no title parameter and always displays "AI 助手".

**OK – Hamburger icon** – `HomeScreen.kt:828-832` – Hamburger icon correctly opens history drawer with `HISTORY_TOGGLE` tag.

**OK – HUD dot toggle** – `HomeScreen.kt:833-851` – HUD dot icon correctly implemented with `debug_toggle_metadata` tag, gated by `hudEnabled` flag, toggles `showDebugMetadata`.

**MISMATCH – Missing "+" new chat button** – `HomeScreen.kt:796-860` – The top bar does not include a "+" new chat button with tag `home_new_chat_button`. The `HomeTopBar` composable only shows hamburger, title, device indicator (if connected), HUD dot (if enabled), and profile icon.

**OK – Profile icon** – `HomeScreen.kt:852-857` – Profile icon present with correct tag.

### 2. Top bar title source

**MISMATCH – Title not bound to session** – `HomeScreen.kt:811-814` – Title is hard-coded as "AI 助手" instead of using `state.currentSession.title`. The `HomeTopBar` composable does not receive the current session title as a parameter.

### 3. Session title lifecycle

**OK – Title generation logic** – `HomeScreenViewModel.kt:2546-2577` – `maybeGenerateSessionTitle` correctly checks if title is placeholder before applying suggested title from metadata. Only applies once per session.

**OK – Title update from metadata** – `HomeScreenViewModel.kt:2579-2607` – `updateTitleFromMetadata` correctly checks if title is still placeholder before updating, preventing overwrite of user-renamed titles.

**OK – Title format** – `HomeScreenViewModel.kt:2539-2544` – `buildTitleFromMetadata` correctly formats as `<date>_<person>_<title>` matching contract.

### 4. Home body layout

**OK – No device/audio cards** – `HomeScreen.kt:394-612` – No "设备管理" or "音频库" cards rendered in Home chat body. The body only contains message list or empty state hero.

**OK – No global spinners** – No pull-to-refresh spinners found in Home body.

### 5. Hero display rules

**OK – Hero gating logic** – `HomeScreen.kt:459` – Hero only renders when `state.chatMessages.isEmpty() && state.showWelcomeHero`. The `showWelcomeHero` flag is correctly managed in ViewModel.

**OK – Hero never in message list** – `HomeScreen.kt:458-541` – Hero is rendered separately via `AnimatedContent`, never as a chat bubble in the message list.

**OK – Hero content structure** – `HomeScreen.kt:617-690` – `EmptyStateContent` correctly shows LOGO, greeting, role line, bullet list, "让我们开始吧", and quick skill row.

### 6. Quick skills placement

**OK – Quick skills in empty state** – `HomeScreen.kt:681-686` – Quick skills appear under hero in empty state.

**OK – Quick skills in active chat** – `HomeScreen.kt:1228-1235` – Quick skills appear in input area above text field when `showQuickSkills` is true (which is `!state.showWelcomeHero`).

**OK – Single quick skill row** – Quick skills only rendered once, either in empty state or input area, never duplicated.

### 7. History drawer

**OK – Drawer from left** – `HomeScreen.kt:373-393` – Uses `ModalNavigationDrawer` with drawer sliding from left.

**OK – Drawer header** – `HomeScreen.kt:1508-1526` – Header shows "历史会话" with close icon at top-right.

**OK – Drawer items** – `HomeScreen.kt:1536-1587` – Items show session title, last message preview, and timestamp. Uses `HISTORY_ITEM_PREFIX` tags.

**OK – Empty state** – `HomeScreen.kt:1527-1534` – Empty state text present with `HISTORY_EMPTY` tag.

**OK – Drawer tag** – `HomeScreen.kt:382` – Drawer uses `HISTORY_PANEL` tag.

**OK – Drawer state sync** – `HomeScreen.kt:270-291` – Drawer state correctly syncs with `showHistoryPanel` flag, handles back press, scrim tap, close icon.

**OK – Gesture gating** – `HomeScreen.kt:375` – Drawer gestures disabled when `state.isInputFocused`, but hamburger tap preserved.

### 8. HUD behavior

**OK – HUD flag gating** – `HomeScreen.kt:308-309` – HUD visibility gated by `CHAT_DEBUG_HUD_ENABLED` and `state.showDebugMetadata`.

**OK – HUD dot toggle** – `HomeScreen.kt:833-851` – Dot icon correctly toggles HUD, uses `debug_toggle_metadata` tag.

**OK – HUD panel** – `HomeScreen.kt:573-608` – Panel appears as bottom overlay with scrim, max height 50% (`screenHalfHeight`), scrollable content, shows sessionId/title/mainPerson/shortSummary/title6/notes.

**OK – HUD controls** – `HomeScreen.kt:911-924` – Panel contains copy and close controls with correct tags (`DEBUG_HUD_COPY`, `DEBUG_HUD_CLOSE`).

**OK – HUD panel tag** – `HomeScreen.kt:888` – Panel uses stable `DEBUG_HUD_PANEL` tag.

### 9. Input/keyboard behavior

**OK – Send dismisses keyboard** – `HomeScreen.kt:429-432` – Send button calls `onDismissKeyboard()` after `onSendClicked()`.

**OK – Tap-to-dismiss** – `HomeScreen.kt:444-447` – Tap gesture on chat content area dismisses keyboard when input focused.

**OK – Pull-down to dismiss** – `HomeScreen.kt:356-371` – `dragDismissModifier` detects vertical drag when input focused and dismisses keyboard.

**OK – Overlay gesture gating** – `HomeScreen.kt:375` – Drawer gestures disabled when `state.isInputFocused`. Overlay gestures gated via `disableOverlayGestures` callback in `AiFeatureTestActivity.kt:435`.

### 10. History drawer gestures

**OK – Gesture model** – `HomeScreen.kt:373-393` – Uses Material3 `ModalNavigationDrawer` which handles peek/commit gestures correctly.

**OK – Backdrop tap** – `HomeScreen.kt:376` – Scrim color set, backdrop tap handled by Material3 drawer.

**OK – IME gating** – `HomeScreen.kt:375` – `gesturesEnabled = !state.isInputFocused` correctly disables drawer gestures when typing.

---

## Likely Causes of Visible Issues

1. **Top bar still showing "AI 助手" instead of session title**
   - **Cause:** `HomeTopBar` composable hard-codes title text instead of receiving `currentSession.title` as parameter.
   - **Location:** `HomeScreen.kt:811-814` – `Text(text = "AI 助手", ...)`
   - **Fix needed:** Pass `state.currentSession.title` to `HomeTopBar` and use it instead of hard-coded string.

2. **Missing "+" new chat button in top bar**
   - **Cause:** `HomeTopBar` composable does not include the new chat button in its layout.
   - **Location:** `HomeScreen.kt:796-860` – `HomeTopBar` function definition and call site.
   - **Fix needed:** Add "+" icon button between HUD dot and profile icon, with `home_new_chat_button` tag, wired to `onNewChatClicked` callback.

3. **History drawer title may diverge from main header**
   - **Status:** Cannot confirm from code inspection alone. Both use `session.title` from `SessionListItemUi` / `CurrentSessionUi`, which should be consistent if `applySessionList()` correctly maps from `AiSessionSummary`. Need runtime verification.

---

## Summary

**Total rules checked:** 10  
**OK:** 8  
**MISMATCH:** 2  
**UNKNOWN:** 0

**Critical issues:**
1. Top bar title hard-coded as "AI 助手" instead of using session title.
2. Missing "+" new chat button in top bar.

**Minor issues:**
- None identified.

**Recommendations:**
1. Update `HomeTopBar` to accept `currentSessionTitle: String` parameter and display it instead of "AI 助手".
2. Add "+" new chat button to `HomeTopBar` between HUD dot and profile icon.
3. Verify at runtime that history drawer titles match main header titles.

