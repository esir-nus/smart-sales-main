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

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `idle` | default | Input field ready, session title in header | — |
| `composing` | user types | Text in input field | — |
| `sending` | tap send | User bubble appears, assistant placeholder created | — |
| `streaming` | LLM responds | Text streams into assistant bubble | — |
| `streaming:rename` | LLM emits `<Rename>` block | (invisible) title candidate extracted | — |
| `complete` | stream ends | Full response, session title may update | — |
| `complete:title_updated` | auto-rename applied | Header title changes (subtle transition) | — |
| `error:network` | connection fail | Error indicator on bubble | "无网络连接，请恢复网络后重试" |
| `error:timeout` | request timeout | Error indicator on bubble | "请求超时" |
| `error:llm` | LLM error | Error indicator on bubble | "AI 回复失败" |

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
│       └── LLM starts → streaming
│           ├── token received → streaming (accumulate)
│           ├── <Rename> detected → streaming:rename (extract candidate)
│           └── stream ends → complete
│               ├── title candidate + placeholder title → complete:title_updated
│               └── no candidate OR user-edited title → complete (no change)
├── error → error:network | error:llm
│   └── dismiss → idle
└── drawer: long-press session → title:user_editing
    ├── cancel → idle
    └── confirm → title:user_confirmed → title:user_locked
```

#### Invariants

| Rule | How to Verify |
|------|---------------|
| Streaming feedback within 200ms of send | Stopwatch: tap send → bubble appears |
| Title never auto-updates after user manual rename | Set `isTitleUserEdited=true` → auto-rename blocked |
| Placeholder titles are format "新会话" or timestamp-based | grep `SessionTitlePolicy.isPlaceholder` |
| `<Rename>` tag never visible to user | Code review: Publisher strips before display |
| GENERAL first reply always outputs `<Rename>` | LLM prompt requires it; fallback: "新客户 - 打招呼" |
| SmartAnalysis derives title from `main_person` + `summary_title_6chars` | No `<Rename>` tag in JSON-only output |
| `<Rename>` / title fields must not be "..." or empty | Parser rejects placeholder values |
| Rename dialog requires non-empty text | Confirm button disabled when blank |
| Title update is a smooth transition (no flash) | UI review: no jarring reflow |

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
| `loading` | enter transcript view | Skeleton/spinner | "Loading transcript..." |
| `partial` | batches arriving | Growing transcript | — |
| `complete` | all batches received | Full transcript | — |
| `chapters_loading` | analysis pending | Chapter skeleton | "Analyzing..." |
| `chapters_ready` | analysis complete | Chapter list | — |
| `error` | API fail | Error state | "Couldn't load transcript." |

### L3 SmartAnalysis Card

| State | Trigger | User Sees | Microcopy |
|-------|---------|-----------|-----------|
| `collapsed` | default | Summary line + expand button | — |
| `expanded` | tap card | Highlights, Action items, Entities, Pointers | — |

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
| 2026-01-08 | Chat | Chinese microcopy for all error states | Sync with implementation, Chinese Priority policy |
| 2026-01-08 | Chat | Enhanced with session renaming sub-flow | Document auto-rename via `<Rename>` and manual rename via drawer |
| 2026-01-08 | — | Initial creation | Split from ux-contract.md |
