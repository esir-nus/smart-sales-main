# Prism UI Registry

> **Role**: Authoritative Component Manifest & Implementation Status.
> **Audience**: UI/UX Team (Tracking) & Developers (Handoff).
> **Status**: Live Tracking

---


---

## 1. ⚛️ Atoms: Design Tokens

Current implementation values (Open for UI Team refinement).

### Color Registry

| Token Key | Hex Value | Compose Name | Semantic Usage |
|-----------|-----------|--------------|----------------|
| `surface.thinking` | `[To Define]` | `PrismColors.ThinkingSurface` | AI thinking container |
| `surface.plan` | `[To Define]` | `PrismColors.PlanSurface` | Execution plan card |
| `surface.conflict` | `[To Define]` | `PrismColors.ConflictSurface` | Conflict resolution card |
| `surface.response` | `[To Define]` | `PrismColors.ResponseSurface` | Chat bubbles |
| `surface.input` | `[To Define]` | (Inline) | Input bar background |
| `text.primary` | `[To Define]` | `PrismColors.TextPrimary` | Main content |
| `text.thinking` | `[To Define]` | `PrismColors.ThinkingText` | Trace logs (Monospace) |
| `text.warning` | `[To Define]` | `PrismColors.TextWarning` | Alerts, conflict headers |
| `accent.action` | `[To Define]` | `PrismColors.ActionCyan` | Interactive elements |

### Shape Registry

| Token Key | Value | Implementation |
|-----------|-------|----------------|
| `shape.card` | `[To Define]` | `RoundedCornerShape(...)` |
| `shape.pill` | `[To Define]` | `RoundedCornerShape(...)` |
| `shape.toggle` | `[To Define]` | `RoundedCornerShape(...)` |

---

## 2. 🧬 Component Registry

The official list of available Prism UI components.

| ID | Component Name | Code Implementation | File Location | Status | Props Contract |
|----|----------------|---------------------|---------------|--------|----------------|
| `MOL-01` | **Thinking Box** | `ThinkingBox.kt` | `feature/chat/...` | ✅ **Verified** | `content: String` (Stream), `isComplete: Bool` |
| `MOL-02` | **Plan List** | `PlanCard.kt` | `feature/chat/...` | ✅ **Verified** | `plan: ExecutionPlan`, `completedSteps: Set<Int>` |
| `MOL-03` | **Conflict Card** | `ConflictResolver.kt` | `feature/chat/...` | ✅ **Verified** | `options: List`, `onConfirm: Fn` |
| `MOL-04` | **Chat Bubble** | `ResponseBubble.kt` | `feature/chat/...` | ✅ **Verified** | `content: String`, `UiState` |
| `MOL-05` | **Memory Toast** | `MemorySnackbar.kt` | `feature/chat/...` | ✅ **Verified** | `MemoryNotification` (Category, Message) |
| `MOL-06` | **Input Bar** | `InputBar` (Inline) | `MinimalChatScreen.kt` | ✅ **Verified** | `text: String`, `isSending: Bool`, `onInputChanged: Fn`, `onSend: Fn`, `onAudio: Fn` (Pending), `onImage: Fn` (Pending) |
| `MOL-07` | **Mode Toggle** | `ModeToggleBar` (Inline) | `MinimalChatScreen.kt` | ✅ **Verified** | `currentMode: Mode`, `onModeSwitch: Fn` |
| `MOL-08` | **Tingwu Recorder** | `TingwuRecorder.kt` | `feature/chat/...` | ✅ **Verified** | `isRecording: Bool`, `amplitude: Float`, `onStart: Fn`, `onStop: Fn` |
| `MOL-09` | **Vision Picker** | `VisionPicker.kt` | `feature/chat/...` | ✅ **Verified** | `onImageSelected: Fn` |
| `MOL-10` | **BLE Status** | `BleStatusIndicator.kt` | `feature/chat/...` | ✅ **Verified** | `isConnected: Bool`, `deviceName: String`, `onConnect: Fn` |
| `MOL-11` | **URL Fetcher** | `UrlInputBox.kt` | `feature/chat/...` | ✅ **Verified** | `url: String`, `isFetching: Bool`, `onUrlChanged: Fn`, `onFetch: Fn` |


**Status Legend**:
*   ⚪ **To Implement**: Implementation pending.
*   ✅ **Implemented**: Code complete.
*   🏁 **Verified**: Components tested in `MinimalChatScreen`.

---

## 2. 🕸️ Layout Patterns (Organisms)

Structural assembly rules (layout only, no style).

| Pattern ID | Pattern Name | Structure / Order | Status |
|------------|--------------|-------------------|--------|
| `ORG-01` | **Analyst Layout** | 1. ThinkingBox (Top)<br>2. PlanCard (Persistent)<br>3. ResponseBubble (Stream)<br>4. InputBar (Bottom) | ✅ **Verified** |
| `ORG-02` | **Coach Layout** | 1. ResponseBubble (Stream)<br>2. ConflictResolver (Inline)<br>3. MemorySnackbar (Overlay) | ✅ **Verified** |

---

## 3. ⚓ Architecture Invariants

Rules for integration, unrelated to cosmetics.

1.  **State Completeness**: All components MUST handle `Loading`, `Error`, and `Empty` states explicitly.
2.  **Strict Props**: UI components must strictly follow the `Props Contract`. No side-channel data access.
3.  **Registry First**: New component? Add to this Registry tables with status ⚪ **To Implement** before coding.
