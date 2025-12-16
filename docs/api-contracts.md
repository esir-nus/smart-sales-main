# API Contracts（对齐 Orchestrator-MetadataHub V5.0.0）

## HomeOrchestrator

- `streamChat(request)`：返回 `ChatStreamEvent.Delta/Completed/Error`；Completed.fullText 可能包含标签。
- LLM 输出文本约定：
  - `<Visible2User>`：用户可见内容。
  - `<Metadata>`：单个 JSON 对象（会话级元数据）。
  - `<Reasoning>` / `<DocReference>`：内部使用。
- 处理规则：
  - MetaHub 写入：优先从 `<Metadata>` 解析 JSON；缺失时回退到“全文最后一个 JSON 对象”兼容逻辑。
  - UI 展示：仅使用 `<Visible2User>`；若缺失，回退到现有 sanitizeAssistantOutput 剥离 scaffold/JSON 后展示。
- 合约与 `docs/Orchestrator-MetadataHub-V5.md` V5.0.0 标签化输出信道规范保持一致；不变更 SessionMetadata/SMART JSON schema。

## 转写气泡合约（XFyun 优先；Tingwu 遗留）

### XFyun（当前默认路径）

- 气泡/音频页仅展示“转写 Markdown”（由转写链路产出后交给 UI 渲染）。
- **能力护栏：当前仅允许 `transfer`**：
  - `translate/predict/analysis` **当前不可用/默认禁用**，必须在发请求前阻断，禁止“试试看”，避免触发 `failType=11` 等失败。
  - XFyun REST 细节以 `docs/xfyun-asr-rest-api.md` 为唯一权威来源（其它文档禁止复制其参数表）。

### Tingwu（legacy/deprecated，仅遗留维护）

- Tingwu 不再作为默认路径；仅用于历史链路维护与兼容。
- 仍沿用既有原则：气泡仅展示转写 Markdown，不再拼接 CustomPrompt/Summarization/AutoChapters 到气泡；CustomPrompt 可作为独立产物但不用于转写增强。
