# API Contracts（对齐 Orchestrator-V1）

> 说明：
> - 本文仅描述“UI <-> Orchestrator-V1”边界与关键事件类型。
> - V7 相关 `<Metadata>` / M1/M2/M3/M4 语义已归档，不再作为当前契约。
> - 三方参数大表仍以 `docs/xfyun-asr-rest-api.md` 为唯一来源（如仍存在兼容需求）。
> - DTO/Schema 定义以 `docs/orchestrator-v1.schema.json` 为准。

---

## 1) Orchestrator Facade（UI 入口面）

V1 推荐 UI 面向统一入口，但内部拆分为可测试模块：

- DisectorOrchestrator：生成 DisectorPlan（见 schema）
- TingwuOrchestrator：批次提交/重试/状态管理
- MemoryCenterOrchestrator：SessionMemory 生成（接口先行，见 schema）
- PublisherOrchestrator：确定性发布与去重（PublishedTranscript/PublishedAnalysis）
- DebugOrchestrator：HUD 三段可复制快照

---

## 2) Session 生命周期事件（音频管线）

建议提供独立事件流（不复用聊天流）：

- `SessionEvent.PlanReady(plan)` — DisectorPlan
- `SessionEvent.BatchSubmitted(batchId, attempt)`
- `SessionEvent.BatchSucceeded(batchId, artifact)` — TingwuBatchArtifact
- `SessionEvent.BatchFailed(batchId, reason, retryable)`
- `SessionEvent.MemoryUpdated(memory)` — SessionMemory
- `SessionEvent.Published(prefixBatchIndex, published)` — PublishedTranscript / PublishedAnalysis
- `SessionEvent.Completed(finalRef)`

约束：
- UI 只能展示 Publisher 输出（连续前缀）。
- 原始 Tingwu JSON 仅可在 HUD 中读取，不进入正常气泡。

---

## 3) Publisher 输出读取

### 3.1 `getPublishedTranscript(sessionId)`
返回：`PublishedTranscript`（见 `docs/orchestrator-v1.schema.json`）。

### 3.2 `getPublishedAnalysis(sessionId)`
返回：`PublishedAnalysis`（见 `docs/orchestrator-v1.schema.json`）。

---

## 4) Debug / HUD Snapshot（强制 3 段）

### 4.1 `getDebugSnapshot(sessionId, optionalBatchId)`
需要提供 3 段可复制文本：

1) Effective Run Snapshot
   - DisectorPlan 摘要
   - Tingwu 队列状态、重试与禁用原因

2) Raw Transcription Output
   - Tingwu 原始输出或引用

3) Publisher-ready Snapshot
   - 预处理/发布前的确定性产物（批次计划、章节时间线、发布前缀）

> V1 不使用 suspicious gap 作为润色依据。

---

## 5) 错误语义（最小约束）

- 429：可重试且需更长退避。
- 其他 4xx（除 429）：默认不可重试。
- 5xx/网络超时：可重试（按确定性退避）。

---

## 6) 兼容说明

- 若存在旧版 UI 依赖 V7 事件流，应通过兼容适配层转换为 V1 事件。
- 新功能必须以 `docs/Orchestrator-V1.md` 作为唯一依据。
