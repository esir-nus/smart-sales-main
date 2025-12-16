# Orchestrator–MetaHub 规范（V5）

## 标题与版本信息（Versioning header）

- 版本：V5.0.0
- 状态：CURRENT
- 生效日期：2025-12-16
- 取代：V4（`docs/Orchestrator-MetadataHub-V4.md`，已归档）

### 版本号规则

> 说明：版本号仅用于“规范/合约”演进，不等同于代码版本。

- Major：破坏性变更（契约/职责边界变更、旧行为不再成立）。
- Minor：向后兼容的新增（新增字段/规则/示例；旧实现可继续运行）。
- Patch：仅文字澄清/错别字/示例补充（不改变任何行为含义）。

## 变更日志（Changelog）

| 版本 | 日期 | 说明 |
| --- | --- | --- |
| V5.0.0 | 2025-12-16 | XFyun 优先；Tingwu 进入遗留维护；强制 `transfer-only`（禁止 translate/predict/analysis “试试看”）；明确 `docs/xfyun-asr-rest-api.md` 为 XFyun REST SoT 且禁止重复参数表；明确 HUD 调试可观测性期望；保留 OSS 作为未来扩展工具箱定位。 |

## 概述与优先级

### 规范范围

- 本文档定义 **Orchestrator / MetaHub / UI(ViewModel) / LLM** 在“聊天输出信道、元数据写入、转写产物注入聊天上下文”的职责边界与规则。
- UX/交互/布局不在本文档定义，必须以 `docs/ux-contract.md` 为准。

### 文档优先级（冲突时按此执行）

1. `docs/ux-contract.md`：唯一 UX 真实来源（交互/布局/流程）。
2. `docs/api-contracts.md`：对外 API 与数据模型的统一口径。
3. `docs/Orchestrator-MetadataHub-V5.md`：本文档（推理与元数据规范，唯一现行版本）。
4. `docs/style-guide.md`：视觉规范（不得覆盖 UX 合约）。

## V4 → V5 迁移摘要

### 变更点（需要同步认知）

- **转写提供方策略**：文档层面改为 **XFyun-first**；Tingwu 仅遗留维护（不作为默认路径）。
- **能力护栏**：当前仅允许 `resultType=transfer`；`translate/predict/analysis` 明确为“当前不可用/默认禁用”，必须在发请求前阻断，避免触发 `failType=11` 等错误。
- **XFyun REST SoT**：`docs/xfyun-asr-rest-api.md` 为唯一权威来源；其它文档只能“链接 + 结论摘要”，禁止复制大段参数表，避免后续同步失真。
- **可观测性（HUD）**：明确哪些信息必须可见以便排错；哪些敏感信息必须禁止落盘/输出。
- **OSS 定位**：保留为未来扩展能力（异步/分发/大文件/多提供方），但不要求参与当前 XFyun 直传链路。

### 不变项（保持既有实现语义）

- GENERAL/SMART 的输出形态与 UI 展示规则不变：UI 只展示 `<Visible2User>`；`<Metadata>/<Reasoning>/<DocReference>` 不展示。
- MetaHub 的会话级 upsert/merge 原则不变：非空覆盖、空值不清空；兼容“尾部 JSON”回退策略仍保留。

## 0. Roles & Responsibilities

### LLM

- GENERAL：生成用户可见的自然语言；首条可附元数据。
- SMART：仅输出 JSON，不直接展示给用户。
- LLM 输出信道（tag-based channels）：
  - `<Visible2User> ... </Visible2User>`：唯一可以展示给终端用户的文本。
  - `<Metadata>{...}</Metadata>`：单个 JSON 对象，供 Orchestrator/MetaHub 写入元数据。
  - `<Reasoning> ... </Reasoning>`：内部推理，调试/日志用，不展示给用户。
  - `<DocReference> ... </DocReference>`：引用/证据列表，供深度分析或导出，默认不展示。
- 重要：标签语义属于“输出协议”，不是 UI 逻辑；UI/ViewModel 只消费 `<Visible2User>`。

### Orchestrator

- 解析 LLM 输出，优先解析标签，合并元数据后写入 MetaHub。
- 负责 SMART JSON → Markdown 渲染，不改写 JSON schema。
- 仅消费会话级 JSON；不写入 persona 等不在 schema 内的字段。

### MetaHub

- 存储会话级 SessionMetadata，按来源合并（GENERAL_FIRST_REPLY / SMART 等）。
- 仅接受结构化 JSON；用户可见内容不直接入库。

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

## 2. 转写接入总览（XFyun-first）

> 说明：以下是“系统内数据如何流动”的稳定口径；具体 XFyun REST 参数与签名细节请统一查 `docs/xfyun-asr-rest-api.md`。

典型链路（以 AudioFiles 为入口）：

1. AudioFiles 选择本地音频文件（WAV/MP3）。
2. XFyun 上传：使用 **file-stream** 方式直接上传音频数据到 `/v2/upload`，获取 `orderId`。
3. 结果轮询：用 `orderId` 调用 `/v2/getResult` 轮询，直到 `status=4`（完成）或 `status=-1`（失败）。
4. 产物：将 `orderResult` 解析为转写 Markdown（必要时包含说话人标签/时间信息）。
5. 展示：音频页展示转写 Markdown；用户可将该转写注入 Home 聊天上下文，触发 AI 分析。

## 3. XFyun SoT 引用与能力护栏

### 3.1 XFyun REST SoT（唯一权威）

- 讯飞 REST 的唯一权威来源：`docs/xfyun-asr-rest-api.md`
- 规则：
  - 其它文档 **不得复制** 该文档中的大段参数表/错误码表（只允许链接 + 结论摘要）。
  - 任何与签名/URL query 相关的描述必须以 SoT 为准（尤其是“空值不参与签名”）。

### 3.2 能力护栏（transfer-only）

> 重要：为避免触发 `failType=11`（能力未开通导致任务创建失败），当前策略必须是“先阻断，再申请开通/再放行”。

- 当前允许：`resultType=transfer`（转写）
- 当前禁止（默认禁用）：`translate` / `predict` / `analysis`
  - 禁止“先发请求试试看”。
  - 若未来确需启用，必须先：
    1) 明确账号已开通能力（平台侧）
    2) 再在配置层显式打开 capability 开关
    3) 最后才允许发请求

## 4. 可观测性与调试期望（HUD）

### 4.1 必须可见（便于排错）

- 非敏感信息：effective `baseUrl`、上传/查询的 URL path、实际生效的 query 参数（脱敏后）、`orderId`、轮询次数与耗时、HTTP code、服务端 `code/descInfo`、`status/failType`、必要的 payload 摘要片段（截断后）。
- 说明：这些信息用于快速判断问题属于“鉴权/签名、参数不一致、服务端失败、网络/超时”中的哪一类。

### 4.2 必须禁止（避免泄露）

- 任何密钥：AppID/APIKey/APISecret、AccessKeySecret、临时 Token。
- 任何可复用的签名材料：`signature` header、签名 baseString（若包含敏感字段）、未脱敏的完整 query（含敏感项）。
- 原始音频内容（除非用户显式导出，并且在本地受控路径存储）。

## 5. OSS 的保留定位（未来扩展工具箱）

> 说明：XFyun 当前链路为 file-stream 直传，并不依赖 OSS。

OSS 仍保留在体系内的原因：

- 未来多提供方接入（不同 ASR/分析服务可能要求 URL 读取或异步分发）。
- 大文件分片/断点续传与分发（减少重复上传与网络抖动影响）。
- 异步工作流与跨端共享（例如长音频、批处理、团队协作分发）。

## 6. 弃用/遗留策略（Tingwu）

- Tingwu 在文档层面标记为 **legacy/deprecated**：
  - 仅做遗留维护（修 bug、兼容老数据），不再作为默认转写路径。
  - 若存在 Tingwu 相关行为描述，应明确标注“历史实现/遗留路径”，并指向 V5 + XFyun SoT。

## 链接索引

- UX 合约：`docs/ux-contract.md`
- API 合约：`docs/api-contracts.md`
- XFyun REST SoT：`docs/xfyun-asr-rest-api.md`
- V4（已归档）：`docs/Orchestrator-MetadataHub-V4.md`

