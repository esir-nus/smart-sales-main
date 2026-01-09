# UX Experience Spec

> **📝 UX-OWNED DOCUMENT**
>
> This file defines **HOW** things are presented—states, flows, microcopy, timing, and layout.
> The UX Specialist persona (`/8-ux-specialist`) may modify this file during polish sessions.
>
> For **WHAT** the system presents (data contracts, pipelines, boundaries), see [`ux-contract.md`](ux-contract.md).

---

## Quick Reference

| Section | Purpose |
|---------|---------|
| [State Inventories](#state-inventories) | Every state users can experience, per flow |
| [Layout Invariants](#layout-invariants) | Component placement rules |
| [Timing & Feedback](#timing--feedback) | Response latency and progress rules |
| [Microcopy](#microcopy) | Text strings for system messages |
| [Changelog](#changelog) | History of experience changes |

---

## State Inventories

### Chat Flow

#### State Inventory

> **Note**: State names below are UX concepts describing user experience, not code symbols. For implementation details, see [api-contracts.md](api-contracts.md).

| State | Sub-State | Trigger | User Sees | Microcopy |
|-------|-----------|---------|-----------|-----------|
| `idle` | — | default | Input field ready, session title in header | — |
| `composing` | — | user types | Text in input field | — |
| `sending` | — | tap send | User bubble appears, assistant placeholder created | — |
| `streaming` | `waiting` | LLM connected, no tokens yet | Empty bubble + typing indicator | — |
| `streaming` | `active` | tokens arriving | Text accumulating in bubble | — |
| `streaming` | `stalled` | no token for >2s | Text + "..." indicator | — |
| `streaming` | `rename` | `<Rename>` block detected | (invisible) title candidate extracted | — |
| `complete` | — | stream ends | Full response, session title may update | — |
| `complete` | `title_updated` | auto-rename applied | Header title changes (subtle transition) | — |
| `error` | `network` | connection fail | Error bubble + retry button | "无网络连接" |
| `error` | `timeout` | request timeout | Error bubble + retry button | "请求超时，点击重试" |
| `error` | `recoverable` | LLM error (retryable) | Error bubble + retry button | "AI 回复失败，点击重试" |
| `error` | `terminal` | LLM error (not retryable) | Error bubble, no retry | "无法完成请求" |

#### Error Recovery Sub-Flow

| State | Trigger | User Sees | Behavior |
|-------|---------|-----------|----------|
| `error:recoverable` | tap retry | Original bubble replaced with new `sending` | Re-sends same message |
| `error:terminal` | — | Error bubble persists | No retry available |
| Any error | dismiss (swipe/tap elsewhere) | Error clears, returns to `idle` | User can compose new message |

#### Session Renaming Sub-Flow

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `title:placeholder` | new session | Header shows "新会话" or timestamp | — |
| `title:auto_candidate` | first LLM reply with `<Rename>` | (invisible) candidate stored | — |
| `title:auto_applied` | TitleResolver accepts candidate | Header title updates smoothly | — |
| `title:user_editing` | long-press session in drawer → Rename | Rename dialog with text field | "重命名会话" |
| `title:user_confirmed` | tap Confirm in dialog | Dialog closes, title updates | — |
| `title:user_locked` | user manually renamed | Future auto-candidates ignored | — |

#### Flow Diagram

```
idle
├── type → composing
│   └── tap send → sending
│       └── LLM connected → streaming:waiting
│           ├── first token → streaming:active
│           │   ├── token received → streaming:active (accumulate)
│           │   ├── no token >2s → streaming:stalled
│           │   │   └── token resumes → streaming:active
│           │   ├── <Rename> detected → streaming:rename (extract candidate)
│           │   └── stream ends → complete
│           │       ├── title candidate + placeholder → complete:title_updated
│           │       └── no candidate OR user-edited → complete (no change)
│           └── connection fail → error:network
├── error:recoverable → tap retry → sending (re-send)
├── error:terminal → (no recovery, user must compose new)
├── error (any) → dismiss → idle
└── drawer: long-press session → title:user_editing
    ├── cancel → idle
    └── confirm → title:user_confirmed → title:user_locked
```

#### Invariants

| Rule | How to Verify |
|------|---------------|
| Streaming feedback within 200ms of send | Stopwatch: tap send → bubble appears |
| If no token for >2s during streaming, show stall indicator | Timer test with slow mock |
| Retry button only appears for recoverable errors | Error type inspection |
| Terminal errors have no retry affordance | UI inspection |
| Title never auto-updates after user manual rename | Set `isTitleUserEdited=true` → auto-rename blocked |
| Placeholder titles are format "新会话" or timestamp-based | grep `SessionTitlePolicy.isPlaceholder` |
| `<Rename>` tag never visible to user | Code review: Publisher strips before display |
| GENERAL first reply always outputs `<Rename>` | LLM prompt requires it; fallback: "新客户 - 打招呼" |
| SmartAnalysis derives title from `main_person` + `summary_title_6chars` | No `<Rename>` tag in JSON-only output |
| `<Rename>` / title fields must not be "..." or empty | Parser rejects placeholder values |
| Rename dialog requires non-empty text | Confirm button disabled when blank |
| Title update is a smooth transition (no flash) | UI review: no jarring reflow |
| Error microcopy fits within bubble without truncation | UI review with long strings |

> [!NOTE]
> **Tingwu renaming is DEFERRED.** The Tingwu pipeline uses a different flow (audio upload → transcription) and may derive session title from transcript metadata in a future iteration.

### Audio Upload Flow

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `idle` | 默认 | "+" 按钮可见 | — |
| `picking` | 点击 + | 系统文件选择器 | — |
| `uploading` | 文件已选 | 进度条 | "上传中…" |
| `transcribing` | 上传完成 | 聊天区加载动画 | "音频已上传，正在转写…" |
| `complete` | 转写完成 | 转写结果气泡 | — |
| `error:upload` | 网络失败 | 错误提示 | "音频处理失败" |
| `error:transcription` | Tingwu 错误 | 错误提示 | "转写失败，请重试" |

#### Invariants

| Rule | How to Verify |
|------|---------------|
| 文件选择器仅限音频 (`audio/*`) | Code: `audioPicker.launch(arrayOf("audio/*"))` |
| 上传/转写期间输入框禁用 | `AudioUiState.isInputBusy = true` during flow |
| 中断恢复提示仅在有未完成任务时显示 | `showAudioRecoveryHint` logic in `AudioViewModel` |
| 成功后清除恢复提示 | `onTranscriptionCompleted()` persists marker |
| 错误消息展示为 Snackbar | `AudioViewModel.snackbarMessage` → UI collection |

### Transcription Flow

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------| 
| `uploading` | 开始上传音频 | 上传进度条 | "上传中... {progress}%" |
| `submitted` | 上传完成，提交转写 | 等待提示 | "正在转写音频文件 {fileName} ..." |
| `in_progress` | Tingwu 开始处理 | 进度百分比 | "正在转写音频文件 {fileName} ... {progress}%" |
| `batches_arriving` | 批次到达 | 逐步显示转写内容 | — |
| `complete` | 所有批次接收完成 | 完整转写结果 + 完成提示 | "转写完成：{fileName}" |
| `error:upload` | 上传失败 | 错误 Snackbar | "音频处理失败" |
| `error:transcription` | Tingwu 错误 | 错误提示 | "转写失败：{reason}" |

#### Invariants

| Rule | How to Verify |
|------|---------------|
| 批次必须在完成回调前处理完 | Sequential flow in `TranscriptionCoordinatorImpl.runTranscription()` |
| StateFlow 观察者顺序不可依赖 | Avoid parallel launch for same event; use sequential collection |
| HUD 数据来源于 `DebugSnapshot` | `DebugOrchestrator` populates section1/2/3 text |
| 转写内容由 `handleProcessedBatch` 创建新消息 | Check `currentMessageId` logic |
| 完成消息覆盖进度消息 | `onCompleted` updates same `progressMessageId` |

### L3 SmartAnalysis Card

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `collapsed` | default | Summary line + expand button | — |
| `expanded` | tap card | Highlights, Action items, Entities, Pointers | — |

### GIF Upload Flow (Badge)

> **Context**: User sends a GIF/image to the ESP32 badge for display.

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `idle` | default | "发送到徽章" button | — |
| `preparing` | tap send | Spinner | "处理中..." |
| `connecting` | BLE jpg#send | Spinner | "正在连接徽章..." |
| `uploading:N/M` | frame ready | Progress bar | "正在上传 {n}/{total}..." |
| `complete` | jpg#display received | Success toast | "发送成功！" |
| `error:ble` | BLE disconnect | Error card | "徽章连接断开，请重试" |
| `error:network` | HTTP fail | Error card | "网络错误，请检查WiFi" |
| `error:frame` | Single frame fail | Error card | "第{n}张图片上传失败" |
| `error:timeout` | Total timeout (5min) | Error card | "上传超时，请重试" |

#### Invariants

| Rule | How to Verify |
|------|---------------|
| Progress visible within 500ms of transfer start | Stopwatch test |
| Per-frame progress updates every frame | Count-based progress |
| Error shows which step failed (BLE vs HTTP) | Test both failure modes |
| Cancel available during long transfers | UI inspection |
| Retry uses exponential backoff (1s, 2s, 4s) | Code review |

### WAV Download Flow (Badge)

> **Context**: User downloads audio recordings from the ESP32 badge.

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `idle` | default | "获取录音" button | — |
| `scanning` | tap button | Spinner | "正在扫描录音文件..." |
| `files_found` | /list returns | File list with checkboxes | "{n} 个录音文件" |
| `downloading` | user confirms | Progress bar | "正在下载 {filename}..." |
| `complete` | all selected done | Success toast | "下载完成" |
| `empty` | /list returns [] | Empty state | "暂无录音" |
| `error:ble` | BLE disconnect | Error card | "徽章连接断开" |
| `error:network` | HTTP fail | Error card | "下载失败，请重试" |
| `error:sd` | SD unmounted (empty []) | Error card | "徽章存储卡未就绪" |

#### Invariants

| Rule | How to Verify |
|------|---------------|
| File list shows filename + size (if available) | UI inspection |
| Multi-select: select all / deselect all | UI inspection |
| Download progress per file | Per-file progress bar |
| Cancel available during download | UI inspection |
| Completed files saved to app storage | File system check |

---

## Layout Invariants

| Component | Rule | Visual Spec Ref |
|-----------|------|-----------------|
| **Home Hero** | Visible ONLY when session is empty (no messages, no imported transcripts). Never rendered as chat bubble. | §6.2 |
| **Quick Skill Row** | Under hero when empty; above text field when active. **Never more than one row visible.** | §6.3 |
| **History Drawer** | Device-status card at top, profile entry at bottom. Both use full drawer width. | §6.6 |

---

## Timing & Feedback

| Rule | Target | Verification |
|------|--------|--------------|
| Action acknowledgment | < 200ms | Stopwatch test: tap → visual change |
| Progress update interval | Every 2 seconds | Manual timing during upload |
| Loading states | Always show indicator | UI review: no "frozen" states |
| Error display duration | Until user acknowledges | Cannot auto-dismiss |

---

## Microcopy

### System Messages

| Context | Message |
|---------|---------|
| Upload progress | "上传中... {progress}%" |
| Transcription pending | "转写中..." |
| Transcription complete | — (transcript renders inline) |
| Network error (chat) | "无网络连接" |
| Timeout error (chat) | "请求超时" |
| Network error (upload) | "音频上传失败" |
| Transcription error | "转写失败" |
| Generic error | "AI 回复失败" |

### Badge Transfer

| Context | Message |
|---------|---------|
| GIF preparing | "处理中..." |
| GIF connecting | "正在连接徽章..." |
| GIF uploading | "正在上传 {n}/{total}..." |
| GIF complete | "发送成功！" |
| GIF error (BLE) | "徽章连接断开，请重试" |
| GIF error (network) | "网络错误，请检查WiFi" |
| GIF error (frame) | "第{n}张图片上传失败" |
| GIF error (timeout) | "上传超时，请重试" |
| WAV scanning | "正在扫描录音文件..." |
| WAV found | "{n} 个录音文件" |
| WAV downloading | "正在下载 {filename}..." |
| WAV complete | "下载完成" |
| WAV empty | "暂无录音" |
| WAV error (SD) | "徽章存储卡未就绪" |

### Session Rename

| Context | Message |
|---------|---------|
| Rename dialog title | "重命名会话" |
| Rename confirm button | "保存" |
| Rename cancel button | "取消" |

### Empty States

| Screen | Message |
|--------|---------|
| Chat (no history) | [Hero + Quick Skills visible] |
| Transcript view (loading) | "Loading transcript..." |
| History drawer (no sessions) | "No previous conversations" |

---

## Open Questions

Track unresolved UX decisions here for Product/Eng review:

- [ ] Should error states auto-dismiss after N seconds?
- [ ] Maximum transcription duration before timeout warning?
- [ ] Should "Cancel" be available during transcription (after upload complete)?

---

## Changelog

| Date | Flow | Change | Reason |
|------|------|--------|--------|
| 2026-01-09 | Badge Transfer | Added GIF Upload Flow and WAV Download Flow state inventories | ESP32 connectivity audit; pre-implementation UX spec |
| 2026-01-09 | Chat | Refined streaming states (waiting/active/stalled), error sub-states (recoverable/terminal), error recovery sub-flow | UX Specialist review: intermediate states needed for production-grade feedback |
| 2026-01-08 | Chat | Chinese microcopy for all error states | Sync with implementation, Chinese Priority policy |
| 2026-01-08 | Chat | Enhanced with session renaming sub-flow | Document auto-rename via `<Rename>` and manual rename via drawer |
| 2026-01-08 | — | Initial creation | Split from ux-contract.md |

