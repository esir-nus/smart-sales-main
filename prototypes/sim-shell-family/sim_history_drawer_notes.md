# SIM History Drawer Prototype - Handoff & Rationale

## 1. Updated Design Direction (Wave 0)
This prototype has been updated to align strictly with the SIM standalone spec, shedding shared-app complexity in favor of a clean, archive-first approach. 

**Key Updates:**
- **Fixed User Dock Added**: The bottom dock is restored, containing avatar, name, membership text, and settings affordance. It is fixed outside the scrolling region to act as a stable anchor for profile access.
- **Narrow Archive Layout**: The drawer remains a left-edge dark frosted slab (`320px` wide) under a stronger scrim (`rgba(0, 0, 0, 0.6)`) so it clear owns the overlay.
- **Title-Only Rows**: Row previews, timestamps, and row-level icons have been stripped. Rows in browse state are now single-line and scan-friendly.
- **Clean Grouping**: Groups strictly follow the `置顶` / `今天` / `最近30天` / `YYYY-MM` spec.
- **Action Paradigm Reverted**: The swipe-to-reveal pattern was removed in favor of long-press action invocation, keeping the visual rhythm completely uninterrupted in browse state.
- **Active Session State**: The current active session row receives a subtle filled slab background to ground the user.

## 2. Designer Rationale
- **Fixed vs Auto-Hiding Footer**: The footer is completely fixed. By not tying it to the scroll view, it guarantees immediate access to UserCenter independently of history length. The spacing above the footer ensures the last row can be scrolled fully into view without being obscured.
- **Single-Title Header**: The header is intentionally stark ("历史记录"). Top padding (48px) clears the system status area without requiring extra utility chrome.
- **Title-Only Density**: Removing timestamps and summaries shifts the drawer from a "conversation inbox" to an "archive index." The interaction speed increases dramatically when cognitive load is reduced to just the title string.

## 3. Layout ASCII Map
```text
┌────────────────────────────┐
│ 历史记录                   │ <- Top padding clears status bar
│                            │
│ 置顶                       │
│ ├─ [Active] 业务复盘       │ <- Solid slab background
│                            │
│ 今天                       │
│ ├─ 需求评审会 V2.4         │ <- Title only rows, 48px height
│ └─ 录音分析                │
│                            │
│ 最近30天                   │
│ ├─ OKR 讨论                │
│ ...                        │
│                            │
├────────────────────────────┤
│ ( USER AVATAR ) Frank  [⚙] │ <- Fixed bottom dock
│                 PRO        │
└────────────────────────────┘
```

## 4. Compose Transplant Brief
**To implementation agent mapping this to `SimHistoryDrawer.kt`:**
- **Structure**: Parent is a `ModalNavigationDrawer` (or custom scrim + `AnimatedVisibility` slider) with a darker dimmed background (`0.6f` opacity).
- **Column Setup**: Drawer content wrapper uses `Column`. `LazyColumn` takes `weight(1f)` for the scrollable list. The User Dock sits *below* the `LazyColumn` so it pins to the bottom.
- **Row Styling**: Rows are `48.dp` tall, displaying only the title string. 
- **Active State**: Highlight the bound `sessionId` row with a subtle white-alpha background (`0.05f`) and thin border.
- **Actions**: Trigger on `Modifier.combinedClickable(onLongClick = { ... })`. Remove any `SwipeToDismiss` implementations.
- **Routing**: Avatar, name column, and the trailing `Icon` all route to the same `UserCenterScreen`. Do not create multiple routes.
- **Groups**: Use standard sticky headers or grouped lists for `置顶`, `今天`, `最近30天`, and YYYY-MM buckets. 

## 5. Defer List (Out of Scope)
- ❌ **No Live Search**: Do not add search bars or filter pills.
- ❌ **No Device Capsule/Header Utility**: The header remains pure text. Do not port over the top-right status chips.
- ❌ **No Smart App Rewrite**: This drawer is for the `SIM Shell` standalone mode only. Do not attempt to fix or modernize the broader `HistoryDrawer.kt` used by the main app unless explicitly splitting it.
- ❌ **No Swipe Actions**: Do not reconstruct swipe-to-reveal; rely on long-press.
- ❌ **No Row Previews**: Do not sneak summaries or dates back into the rows.
