# Orchestrator–MetaHub 规范（V4）

当前规范版本：v4.4.0（标签化输出信道：<Visible2User> / <Metadata> / <Reasoning> / <DocReference>）

## 0. Roles & Responsibilities

### LLM
- GENERAL：生成用户可见的自然语言；首条可附元数据。
- SMART：仅输出 JSON，不直接展示给用户。
- LLM 输出信道（tag-based channels）：所有 Home 内 LLM 输出统一用简单标签区分内容：
  - `<Visible2User> ... </Visible2User>`：唯一可以展示给终端用户的文本。
  - `<Metadata>{...}</Metadata>`：单个 JSON 对象，供 Orchestrator/MetaHub 写入元数据。
  - `<Reasoning> ... </Reasoning>`：内部推理，调试/日志用，不展示给用户。
  - `<DocReference> ... </DocReference>`：引用/证据列表，供深度分析或导出，默认不展示。
- UI / ViewModel 只消费 `<Visible2User>` 渲染气泡；其他标签仅由 Orchestrator / MetaHub / 调试 HUD 使用。
- 兼容：若模型暂未严格产出标签，仍可回退到“尾部 JSON + 轻量 sanitize”逻辑。
- Tingwu 逐字稿气泡：仅展示 PostTingwu TranscriptEnhancer 产出的转写（或原始回退）；CustomPrompt 输出不参与气泡增强。

### Orchestrator
- 解析 LLM 输出，优先解析标签，合并元数据后写入 MetaHub。
- 负责 SMART JSON→Markdown 渲染，不改写 JSON schema。
- 不写入 persona 字段，仅消费会话级 JSON。

### MetaHub
- 存储会话级 SessionMetadata，按来源合并（GENERAL_FIRST_REPLY / SMART 等）。
- 仅接受结构化 JSON，用户可见内容不直接入库。

### UI / ViewModel
- GENERAL：优先从 `<Visible2User>` 提取展示文本；若缺失，按现有 sanitizeAssistantOutput 回退，剥离 scaffold/JSON。
- 不展示 `<Metadata>/<Reasoning>/<DocReference>`。
- SMART：仅展示 Orchestrator 生成的 Markdown 卡片。

## 1. Write rules

### GENERAL 首条回复
- 建议输出：
  - 一个 `<Visible2User>...</Visible2User>` 段落，供用户阅读。
  - 单个 JSON 对象放在 `<Metadata>{...}</Metadata>` 标签内（会话级字段，如 main_person/short_summary/summary_title_6chars/location/stage/risk_level/highlights/actionable_tips/core_insight/sharp_line）。
- MetaHub 写入优先级：
  1) 解析 `<Metadata>` 内的 JSON（要求非空字段至少一个）。
  2) 若无 `<Metadata>`，回退到全文的“最后一个 JSON 对象”兼容逻辑（与旧版一致）。
- upsertSession：非空覆盖，空值不清空；首条自动改名“一次性”规则不变。

### GENERAL 后续回复
- 仅 `<Visible2User>` 供展示，不应输出元数据；若出现标签，忽略 `<Metadata>`。

### SMART
- 仍为 JSON-only 输出，由 Orchestrator 解析后生成 Markdown，未采用标签化信道。

## 附录 A：变更记录

- v4.4.0（2025-xx-xx）
  - 引入标签化输出信道：<Visible2User>（展示）、<Metadata>（单个 JSON）、<Reasoning>/<DocReference>（内部）。
  - GENERAL 首条：优先从 `<Metadata>` 解析 JSON，缺失时回退“最后一个 JSON 对象”；UI 仅展示 `<Visible2User>`。
  - UI 回退策略：若标签缺失，沿用 V4 轻量 sanitize 去除 scaffold/JSON 尾巴。
  - SMART/Tingwu 行为不变，仍按既有 JSON/管线处理。
