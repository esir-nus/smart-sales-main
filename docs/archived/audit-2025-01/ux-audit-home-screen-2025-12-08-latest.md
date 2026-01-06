# UX Audit Report: Home Chat Shell, Title Pipeline, HUD, and History Drawer

**Branch:** `UX-polishment`  
**Commit:** `202205c4`  
**Date:** 2025-12-08  
**Scope:** Home chat shell, title pipeline, HUD, and history drawer

---

## UX Rules (from docs)

From `docs/ux-contract.md` and `docs/Orchestrator-MetadataHub-Mvp-V3.md`:

1. **Top bar composition:** Left → right: hamburger icon (opens history drawer, tag `HISTORY_TOGGLE`), session title (binds to `CurrentSessionUi.title`, tag `SESSION_TITLE`), device connection indicator, HUD dot (debug only, tag `debug_toggle_metadata`), "+" new chat button (tag `home_new_chat_button`), profile/avatar. Title is single line with ellipsis overflow.

2. **Top bar title source:** Title binds to `CurrentSessionUi.title` (not hard-coded "AI 助手"). Title changes when: new session created, Orchestrator/MetaHub suggests non-placeholder name (one-shot), user manually renames.

3. **Device connection indicator:** Small icon/chip, non-invasive. States: Connected / Connecting / Disconnected. Tap may navigate to Device Manager (when implemented).

4. **Session title lifecycle:** New session starts with placeholder (e.g., "新的聊天"). When metadata available, Orchestrator/MetaHub proposes title using `<major person>_<summary limited to 6 Chinese characters>_<MM/dd>`. UI only applies if current title is still placeholder. After non-placeholder title set, no further automatic renames.

5. **Home body layout:** Chat-first. Empty chat: hero block + quick skills. Active chat: message list. **Forbidden:** "设备管理"/"音频库" cards, global spinners, extra panels pushing messages out of view.

6. **Hero display rules:** Hero appears only when current session has no messages or transcripts. Once session becomes active, hero never renders. Hero is not a chat bubble and never appears in scrollable message list.

7. **Quick skills placement:** Empty chat: under hero, one quick skill row. Active chat: in input area, directly above text field. Never at top of message list, never duplicated, never in other shells.

8. **History drawer:** Slides from left. Trigger: tap hamburger icon or swipe from left edge. Header: "历史会话" with close icon at top-right. Items sorted by `updatedAtMillis` desc (newest first). Each card shows: session title (same logic as Home), last message preview (single line), timestamp. Uses `HISTORY_PANEL` tag, empty state uses `HISTORY_EMPTY` tag.

9. **HUD behavior:** Debug-only, controlled by `CHAT_DEBUG_HUD_ENABLED`. Trigger: small top-bar dot icon (no text), tag `debug_toggle_metadata`, toggles `showDebugMetadata`. Panel: bottom overlay, max height ~40–50% screen, internally scrollable, shows sessionId/title/mainPerson/shortSummary/summaryTitle6Chars/notes, contains copy and close controls, uses stable tag `DEBUG_HUD_PANEL`.

10. **Input/keyboard behavior:** Send button runs send pipeline then immediately dismisses keyboard. Tap-to-dismiss: if input focused and user taps chat area above skill row, keyboard hidden. Pull-down to dismiss: downward drag closes keyboard first, then scrolls normally. When typing (input focused): device/audio overlay gestures disabled.

11. **History drawer gestures:** Small drag → peek, threshold → commit. Backdrop tap closes drawer. Drawer gestures disabled when IME is focused, but hamburger tap behavior preserved.

---

## Findings by Rule

### 1. Top bar composition

**OK – Hamburger icon** – `HomeScreen.kt:829-834` – Hamburger icon correctly opens history drawer with `HISTORY_TOGGLE` tag.

**OK – Session title** – `HomeScreen.kt:835-843` – Session title correctly binds to `title` parameter (which comes from `state.currentSession.title`), uses `SESSION_TITLE` tag, single line with ellipsis overflow, uses `weight(1f)` for proper spacing.

**MISMATCH – Missing device connection indicator** – `HomeScreen.kt:809-876` – The contract specifies a "device connection indicator" (small icon/chip showing connection state) should appear between session title and HUD dot. The `HomeTopBar` composable receives `deviceSnapshot` and `onDeviceClick` parameters (lines 814, 816), but no device indicator is rendered in the top bar. The current order is: hamburger → session title → HUD dot → "+" new chat → profile, missing the device indicator.

**OK – HUD dot toggle** – `HomeScreen.kt:844-862` – HUD dot icon correctly implemented with `DEBUG_HUD_TOGGLE` tag, gated by `hudEnabled` flag, toggles `showDebugMetadata`.

**OK – "+" new chat button** – `HomeScreen.kt:863-868` – "+" new chat button correctly implemented with `NEW_CHAT_BUTTON` tag, wired to `onNewChatClick` callback.

**OK – Profile icon** – `HomeScreen.kt:869-874` – Profile icon present with correct tag.

**MISMATCH – Top bar order missing device indicator** – `HomeScreen.kt:822-875` – Current order: hamburger → session title → HUD dot → "+" new chat → profile. Contract requires: hamburger → session title → **device connection indicator** → HUD dot → "+" new chat → profile. The device indicator is missing from the rendered UI.

### 2. Top bar title source

**OK – Title bound to session** – `HomeScreen.kt:415` – Title correctly passed as `state.currentSession.title` to `HomeTopBar`. The `HomeTopBar` composable receives `title: String` parameter and displays it.

### 3. Device connection indicator

**MISMATCH – Device indicator not rendered** – `HomeScreen.kt:809-876` – The contract requires a "device connection indicator" (small icon/chip, non-invasive, showing Connected/Connecting/Disconnected states) between session title and HUD dot. While `deviceSnapshot` and `onDeviceClick` are passed to `HomeTopBar`, no device indicator component is rendered in the top bar layout.

### 4. Session title lifecycle

**OK – Title generation logic** – `HomeScreenViewModel.kt:2557-2588` – `maybeGenerateSessionTitle` correctly checks if title is placeholder using `SessionTitlePolicy.isPlaceholder()` before applying suggested title from metadata. Only applies once per session.

**OK – Title update from metadata** – `HomeScreenViewModel.kt:2579-2607` – `updateTitleFromMetadata` correctly checks if title is still placeholder before updating, preventing overwrite of user-renamed titles.

**OK – Title format** – `HomeScreenViewModel.kt:2552-2555` – Uses `SessionTitlePolicy.buildSuggestedTitle()` which formats as `<date>_<person>_<title>` matching contract.

### 5. Home body layout

**OK – No device/audio cards** – `HomeScreen.kt:394-612` – No "设备管理" or "音频库" cards rendered in Home chat body. The body only contains message list or empty state hero.

**OK – No global spinners** – No pull-to-refresh spinners found in Home body.

### 6. Hero display rules

**OK – Hero gating logic** – `HomeScreen.kt:459` – Hero only renders when `state.chatMessages.isEmpty() && state.showWelcomeHero`. The `showWelcomeHero` flag is correctly managed in ViewModel.

**OK – Hero never in message list** – `HomeScreen.kt:458-541` – Hero is rendered separately via `AnimatedContent`, never as a chat bubble in the message list.

**OK – Hero content structure** – `HomeScreen.kt:617-690` – `EmptyStateContent` correctly shows LOGO, greeting, role line, bullet list, "让我们开始吧", and quick skill row.

### 7. Quick skills placement

**OK – Quick skills in empty state** – `HomeScreen.kt:681-686` – Quick skills appear under hero in empty state.

**OK – Quick skills in active chat** – `HomeScreen.kt:1228-1235` – Quick skills appear in input area above text field when `showQuickSkills` is true (which is `!state.showWelcomeHero`).

**OK – Single quick skill row** – Quick skills only rendered once, either in empty state or input area, never duplicated.

### 8. History drawer

**OK – Drawer from left** – `HomeScreen.kt:378-398` – Uses `ModalNavigationDrawer` with drawer sliding from left.

**OK – Drawer header** – `HomeScreen.kt:1534-1552` – Header shows "历史会话" with close icon at top-right.

**OK – Drawer items** – `HomeScreen.kt:1566-1605` – Items show session title, last message preview, and timestamp. Uses `HISTORY_ITEM_PREFIX` tags.

**OK – Empty state** – `HomeScreen.kt:1553-1560` – Empty state text present with `HISTORY_EMPTY` tag.

**OK – Drawer tag** – `HomeScreen.kt:387` – Drawer uses `HISTORY_PANEL` tag.

**OK – Drawer state sync** – `HomeScreen.kt:270-291` – Drawer state correctly syncs with `showHistoryPanel` flag, handles back press, scrim tap, close icon.

**OK – Gesture gating** – `HomeScreen.kt:380` – Drawer gestures disabled when `state.isInputFocused`, but hamburger tap preserved.

**OK – ChatHistoryTestTags.PAGE** – `HomeScreen.kt:1531` – Drawer content includes `ChatHistoryTestTags.PAGE` tag for legacy test compatibility.

**MISMATCH – History sorting includes pinned priority** – `RoomAiSessionRepository.kt:23-26` – The contract specifies items should be "Sorted by `updatedAtMillis` (desc, newest first)". The implementation sorts by `pinned` first (descending), then by `updatedAtMillis` (descending). This means pinned sessions appear first even if they are older than newer unpinned sessions. While this may be acceptable UX behavior, it diverges from the contract's explicit "newest first" rule.

### 9. HUD behavior

**OK – HUD flag gating** – `HomeScreen.kt:308-309` – HUD visibility gated by `CHAT_DEBUG_HUD_ENABLED` and `state.showDebugMetadata`.

**OK – HUD dot toggle** – `HomeScreen.kt:844-862` – Dot icon correctly toggles HUD, uses `DEBUG_HUD_TOGGLE` tag (which maps to `debug_toggle_metadata`).

**OK – HUD panel** – `HomeScreen.kt:573-608` – Panel appears as bottom overlay with scrim, max height 50% (`screenHalfHeight`), scrollable content, shows sessionId/title/mainPerson/shortSummary/title6/notes.

**OK – HUD controls** – `HomeScreen.kt:911-924` – Panel contains copy and close controls with correct tags (`DEBUG_HUD_COPY`, `DEBUG_HUD_CLOSE`).

**OK – HUD panel tag** – `HomeScreen.kt:888` – Panel uses stable `DEBUG_HUD_PANEL` tag.

### 10. Input/keyboard behavior

**OK – Send dismisses keyboard** – `HomeScreen.kt:429-432` – Send button calls `onDismissKeyboard()` after `onSendClicked()`.

**OK – Tap-to-dismiss** – `HomeScreen.kt:444-447` – Tap gesture on chat content area dismisses keyboard when input focused.

**OK – Pull-down to dismiss** – `HomeScreen.kt:356-371` – `dragDismissModifier` detects vertical drag when input focused and dismisses keyboard.

**OK – Overlay gesture gating** – `HomeScreen.kt:380` – Drawer gestures disabled when `state.isInputFocused`. Overlay gestures gated via `disableOverlayGestures` callback in `AiFeatureTestActivity.kt:435`.

### 11. History drawer gestures

**OK – Gesture model** – `HomeScreen.kt:378-398` – Uses Material3 `ModalNavigationDrawer` which handles peek/commit gestures correctly.

**OK – Backdrop tap** – `HomeScreen.kt:381` – Scrim color set, backdrop tap handled by Material3 drawer.

**OK – IME gating** – `HomeScreen.kt:380` – `gesturesEnabled = !state.isInputFocused` correctly disables drawer gestures when typing.

---

## Likely Causes of Visible Issues

1. **Missing device connection indicator in top bar**
   - **Cause:** `HomeTopBar` composable receives `deviceSnapshot` and `onDeviceClick` parameters but does not render any device indicator component in the top bar layout.
   - **Location:** `HomeScreen.kt:809-876` – `HomeTopBar` function definition. The device indicator should appear between session title (line 835-843) and HUD dot (line 844-862), but no such component exists.
   - **Contract requirement:** The contract explicitly requires a "device connection indicator" (small icon/chip showing Connected/Connecting/Disconnected states) between session title and HUD dot.
   - **Note:** The `DEVICE_TOGGLE` tag is defined (line 807) and `onDeviceClick` callback is wired (lines 422-424), suggesting the device toggle functionality was intended but the UI component was removed or not implemented.

2. **History drawer sorting prioritizes pinned sessions**
   - **Cause:** `RoomAiSessionRepository` sorts by `pinned` first (descending), then by `updatedAtMillis` (descending), rather than strictly by `updatedAtMillis` desc as the contract specifies.
   - **Location:** `RoomAiSessionRepository.kt:23-26` – Sorting logic in `summaries` Flow.
   - **Contract requirement:** Items should be "Sorted by `updatedAtMillis` (desc, newest first)". The implementation adds a pinned priority layer.
   - **Note:** This may be intentional UX enhancement (pinned sessions always appear first), but it diverges from the contract's explicit sorting rule. The contract does not mention pinned sessions at all.

---

## Summary

**Total rules checked:** 11  
**OK:** 9  
**MISMATCH:** 2  
**UNKNOWN:** 0

**Critical issues:**
1. Device connection indicator missing from top bar (contract requires it between session title and HUD dot).
2. History drawer sorting prioritizes pinned sessions before `updatedAtMillis`, diverging from contract's "newest first" rule.

**Minor issues:**
- None identified.

**Recommendations:**
1. Add device connection indicator component to `HomeTopBar` between session title and HUD dot, showing connection state (Connected/Connecting/Disconnected) as a chip or icon button.
2. Consider updating the contract to explicitly allow pinned session priority in history sorting, or adjust implementation to strictly sort by `updatedAtMillis` desc only.

**Positive findings:**
- Top bar title correctly binds to session title ✓
- "+" new chat button correctly implemented ✓
- History drawer correctly implemented as Material3 modal drawer ✓
- HUD behavior matches contract ✓
- Hero display logic correct ✓
- Quick skills placement correct ✓
- Input/keyboard behavior correct ✓
- Session title lifecycle correct ✓

