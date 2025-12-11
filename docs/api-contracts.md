# API Contracts（对齐 Orchestrator-MetadataHub v4.4.0）

## HomeOrchestrator

- `streamChat(request)`：返回 `ChatStreamEvent.Delta/Completed/Error`；Completed.fullText 可能包含标签。
- LLM 输出文本约定：
  - `<Visible2User>`：用户可见内容。
  - `<Metadata>`：单个 JSON 对象（会话级元数据）。
  - `<Reasoning>` / `<DocReference>`：内部使用。
- 处理规则：
  - MetaHub 写入：优先从 `<Metadata>` 解析 JSON；缺失时回退到“全文最后一个 JSON 对象”兼容逻辑。
  - UI 展示：仅使用 `<Visible2User>`；若缺失，回退到现有 sanitizeAssistantOutput 剥离 scaffold/JSON 后展示。
- 合约与 Orchestrator-MetadataHub-V4.md v4.4.0 标签化输出信道规范保持一致；不变更 SessionMetadata/SMART JSON schema。
