# UX Audit Report: Home Chat Shell, Title Pipeline, HUD, and History Drawer

**Branch:** `UX-polishment`  
**Commit:** `39d087f7`  
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

**OK – Hamburger icon** – `HomeScreen.kt:826-831` – Hamburger icon correctly opens history drawer with `HISTORY_TOGGLE` tag.

**OK – Session title** – `HomeScreen.kt:832-840` – Session title correctly binds to `title` parameter (which comes from `state.currentSession.title`), uses `SESSION_TITLE` tag, single line with ellipsis overflow, uses `weight(1f)` for proper spacing.

**OK – Device connection indicator** – `HomeScreen.kt:841-851` – Device connection indicator correctly shown when connected.

**MISMATCH – Extra audio toggle button** – `HomeScreen.kt:852-857` – Top bar includes an audio toggle button (`AUDIO_TOGGLE` tag) that is not specified in the UX contract. The contract specifies exact order: hamburger → session title → device indicator → HUD dot → "+" new chat → profile. The audio toggle appears between device indicator and HUD dot, which is not in the contract.

**OK – HUD dot toggle** – `HomeScreen.kt:858-876` – HUD dot icon correctly implemented with `debug_toggle_metadata` tag, gated by `hudEnabled` flag, toggles `showDebugMetadata`.

**OK – "+" new chat button** – `HomeScreen.kt:877-882` – "+" new chat button correctly implemented with `NEW_CHAT_BUTTON` tag, wired to `onNewChatClick` callback.

**OK – Profile icon** – `HomeScreen.kt:883-888` – Profile icon present with correct tag.

**MISMATCH – Top bar order** – `HomeScreen.kt:819-889` – Current order: hamburger → session title → device indicator → **audio toggle** → HUD dot → "+" new chat → profile. Contract requires: hamburger → session title → device indicator → HUD dot → "+" new chat → profile. The audio toggle button is extra and disrupts the specified order.

### 2. Top bar title source

**OK – Title bound to session** – `HomeScreen.kt:412` – Title correctly passed as `state.currentSession.title` to `HomeTopBar`. The `HomeTopBar` composable receives `title: String` parameter and displays it.

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

**OK – Drawer from left** – `HomeScreen.kt:378-398` – Uses `ModalNavigationDrawer` with drawer sliding from left.

**OK – Drawer header** – `HomeScreen.kt:1538-1556` – Header shows "历史会话" with close icon at top-right.

**OK – Drawer items** – `HomeScreen.kt:1570-1605` – Items show session title, last message preview, and timestamp. Uses `HISTORY_ITEM_PREFIX` tags.

**OK – Empty state** – `HomeScreen.kt:1557-1564` – Empty state text present with `HISTORY_EMPTY` tag.

**OK – Drawer tag** – `HomeScreen.kt:387` – Drawer uses `HISTORY_PANEL` tag.

**OK – Drawer state sync** – `HomeScreen.kt:270-291` – Drawer state correctly syncs with `showHistoryPanel` flag, handles back press, scrim tap, close icon.

**OK – Gesture gating** – `HomeScreen.kt:380` – Drawer gestures disabled when `state.isInputFocused`, but hamburger tap preserved.

**OK – ChatHistoryTestTags.PAGE** – `HomeScreen.kt:1535` – Drawer content includes `ChatHistoryTestTags.PAGE` tag for legacy test compatibility.

### 8. HUD behavior

**OK – HUD flag gating** – `HomeScreen.kt:308-309` – HUD visibility gated by `CHAT_DEBUG_HUD_ENABLED` and `state.showDebugMetadata`.

**OK – HUD dot toggle** – `HomeScreen.kt:858-876` – Dot icon correctly toggles HUD, uses `debug_toggle_metadata` tag.

**OK – HUD panel** – `HomeScreen.kt:573-608` – Panel appears as bottom overlay with scrim, max height 50% (`screenHalfHeight`), scrollable content, shows sessionId/title/mainPerson/shortSummary/title6/notes.

**OK – HUD controls** – `HomeScreen.kt:911-924` – Panel contains copy and close controls with correct tags (`DEBUG_HUD_COPY`, `DEBUG_HUD_CLOSE`).

**OK – HUD panel tag** – `HomeScreen.kt:888` – Panel uses stable `DEBUG_HUD_PANEL` tag.

### 9. Input/keyboard behavior

**OK – Send dismisses keyboard** – `HomeScreen.kt:429-432` – Send button calls `onDismissKeyboard()` after `onSendClicked()`.

**OK – Tap-to-dismiss** – `HomeScreen.kt:444-447` – Tap gesture on chat content area dismisses keyboard when input focused.

**OK – Pull-down to dismiss** – `HomeScreen.kt:356-371` – `dragDismissModifier` detects vertical drag when input focused and dismisses keyboard.

**OK – Overlay gesture gating** – `HomeScreen.kt:380` – Drawer gestures disabled when `state.isInputFocused`. Overlay gestures gated via `disableOverlayGestures` callback in `AiFeatureTestActivity.kt:435`.

### 10. History drawer gestures

**OK – Gesture model** – `HomeScreen.kt:378-398` – Uses Material3 `ModalNavigationDrawer` which handles peek/commit gestures correctly.

**OK – Backdrop tap** – `HomeScreen.kt:381` – Scrim color set, backdrop tap handled by Material3 drawer.

**OK – IME gating** – `HomeScreen.kt:380` – `gesturesEnabled = !state.isInputFocused` correctly disables drawer gestures when typing.

---

## Likely Causes of Visible Issues

1. **Top bar includes audio toggle button not in contract**
   - **Cause:** `HomeTopBar` composable includes an audio toggle button (`onAudioClick`, `AUDIO_TOGGLE` tag) that is not specified in the UX contract's top bar composition.
   - **Location:** `HomeScreen.kt:852-857` – Audio toggle button between device indicator and HUD dot.
   - **Contract requirement:** The contract specifies exact order: hamburger → session title → device indicator → HUD dot → "+" new chat → profile. No audio toggle is mentioned.
   - **Note:** While the contract forbids "音频库" cards in Home body, it does not explicitly allow an audio toggle in the top bar. The contract's explicit ordering suggests this button should be removed or moved elsewhere (e.g., to a different shell or overlay).

---

## Summary

**Total rules checked:** 10  
**OK:** 8  
**MISMATCH:** 2  
**UNKNOWN:** 0

**Critical issues:**
1. Top bar includes audio toggle button not specified in UX contract.
2. Top bar order does not match contract: audio toggle appears between device indicator and HUD dot, disrupting the specified order.

**Minor issues:**
- None identified.

**Recommendations:**
1. Remove audio toggle button from top bar, or move it to a different location (e.g., audio library shell or overlay) if audio access is needed.
2. Verify at runtime that history drawer titles match main header titles (both use same session list source, should be consistent).

**Positive findings:**
- Top bar title now correctly binds to session title ✓
- "+" new chat button correctly implemented ✓
- History drawer correctly implemented as Material3 modal drawer ✓
- All other UX rules properly implemented ✓

