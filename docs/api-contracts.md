# API Contracts（对齐 Orchestrator-MetadataHub V7.0.0）

> 说明：
> - 本文仅描述“UI <-> Orchestrator/MetaHub”边界与关键事件类型，不包含三方参数大表。
> - XFyun REST 细节以 `docs/xfyun-asr-rest-api.md` 为唯一权威来源（禁止在其它文档复制其参数表）。

## 1. Orchestrator Facade（UI 入口面）

V7 允许 UI 面向一个统一的 facade（入口），但内部必须拆分为可测试模块（避免 god-object）：

- ChatOrchestrator：对话流
- TranscriptionOrchestrator：转写任务与伪流式（batch-by-batch）
- AnalysisOrchestrator：Agent1/Agent2 调度、Smart Analysis 产物
- ExportOrchestrator：CSV/PDF/Mindmap（受 Smart Analysis gate 控制）
- DebugOrchestrator：HUD 三段 copy snapshot
- ExternalMemoryClient（占位）：M4（风格/知识）检索，不承诺具体后端

本文用“端点/方法名”描述期望能力，具体 Kotlin 接口名可在实现时统一命名。

---

## 2. Chat（GENERAL）

### 2.1 `streamChat(request) -> Flow<ChatStreamEvent>`

- `ChatStreamEvent.Delta(textChunk)`
- `ChatStreamEvent.Completed(fullText, usage?, meta?)`
- `ChatStreamEvent.Error(code, message, recoverable?)`

### 2.2 LLM 输出标签约定（维持 V5，但扩展 MetaHub 写入含义）

- `<Visible2User>...</Visible2User>`：唯一用户可见内容（Markdown/纯文本）。
- `<Metadata>{...}</Metadata>`：单个 JSON 对象，只用于 Orchestrator 解析并写入 MetaHub；UI 永不展示。
- `<Reasoning>` / `<DocReference>`：内部使用，UI 永不展示。

#### `<Metadata>` 在 V7 的语义
`<Metadata>` 是 Orchestrator 侧的写入输入，允许包含：
- M2（ConversationDerivedState）补丁（patch）
- M3（SessionState / RenamingMetadata）候选值补丁（candidate-only）
- Smart Analysis artifact 的引用信息（或小型摘要信息）

禁止：
- UI 直接消费 `<Metadata>`。
- 在聊天气泡里展示任何 JSON 或内部标签。

### 2.3 MetaHub 写入与回退规则（Orchestrator 负责）

- 优先从 `<Metadata>` 解析 JSON。
- 若缺失 `<Metadata>`，允许有限回退：
  - 从全文尾部提取“最后一个 JSON 对象”作为兼容逻辑（仅用于迁移期）。
- Orchestrator 负责：
  - 校验（schemaVersion/字段范围）
  - 合并（patch-based merge）
  - 写入（M2/M3/M1/M4 引用）
- UI 只展示 `<Visible2User>`；缺失时可回退到 sanitize（轻量清理）展示。

---

## 3. Transcription（Tingwu+OSS 默认；XFyun 可选）

### 3.1 Provider lane policy（重要）

- 默认 lane：Tingwu + OSS
- XFyun lane：默认禁用，仅在显式开启并验证可用时启用
- transfer-only guardrail：
  - translate/predict/analysis 视为不可用，除非显式启用且被证明可用
  - 禁止“先发请求试试看”

### 3.2 转写伪流式合约（batch-by-batch）

建议引入独立事件流（不要塞进 ChatStreamEvent）：

- `TranscriptStreamEvent.Progress(statusText, percent?)`
- `TranscriptStreamEvent.BatchReleased(batchId, renderedTextDeltaOrChunk, batchRangeInfo?)`
- `TranscriptStreamEvent.Completed(finalRenderedText, rawProviderRef?, preprocessRef?)`
- `TranscriptStreamEvent.Error(code, message, retryable?)`

约束：
- UI 可在 batch 释放时逐步展示（伪流式）。
- 说话人标签可在处理中更新，但 Completed 后必须冻结。

### 3.3 Raw transcription 输出（供 HUD/调试用）

- Provider 原生输出必须可获取（或可引用）用于 HUD Section 2。
- UI 的正式展示必须是“转写 Markdown / 规范化文本”，而不是原生 JSON。

---

## 4. Smart Analysis（Agent1）与导出 gating

### 4.1 Smart Analysis
- 由 Agent1 产出结构化结果，用于：
  - 更新 M2 派生状态（risk/stage/intent/highlights/pain points/next steps）
  - 更新 M3 的命名候选（RenamingMetadata.candidate）
  - 供导出与 UI 卡片使用（可通过 ref/hash 指向产物）

### 4.2 Export gating
- CSV/PDF 导出必须满足：
  - `SessionState.exportGate.smartAnalysisReady == true`
- Export naming：
  - 优先使用 M3 accepted（若用户已编辑/接受）
  - 否则使用 M3 candidate（由 Smart Analysis/first-20 提案）
  - 否则 fallback 到 session title / timestamp

---

## 5. Debug / HUD Snapshot（强制 3 段 copy）

定义一个稳定读取能力：

### 5.1 `getDebugSnapshot(sessionId, optionalJobId) -> DebugSnapshot`

DebugSnapshot 需要包含三段可复制文本（以及必要的 copy-only JSON）：

1) Effective Run Snapshot
   - AiParaSettings（sanitized）+ effective config
   - provider lane 选择与禁用原因
   - LLM prompt pack 状态（版本/哈希）
   - diarization 等关键能力状态（“请求 vs 生效”）
   - M4 状态（是否启用、pack 版本、检索 traceId）
   - 注意：禁止泄露密钥、signature 原文、raw HTTP bodies

2) Raw Transcription Output
   - Tingwu 或 XFyun 的原始输出（或引用/下载链接/缓存句柄）
   - copy-only，不进入正式聊天气泡展示

3) Preprocessed Snapshot
   - first 20 lines summary（rendered）
   - suspicious boundaries
   - batch plan 概览
   - 任何“最终润色前”的确定性产物

---

## 6. External Memory / M4（占位）

定义一个内部接口占位：
- Query-only by default（permission-gated + feature-flagged）
- 返回 agent-friendly items（STYLE/POLICY/FAQ/JARGON_MAP/TEMPLATE）
- HUD Section 1 必须记录：
  - enabled/disabled
  - pack versions
  - retrieval trace id / item ids / token estimate

