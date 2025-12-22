## Orchestrator–MetaHub V1–V6 演进对比（中文说明）

> 目标：用“故事线”方式说明从 V1 到 V6，如何一步步把一个普通 LLM 应用，进化成一个 **可观测、可控、可复用的 agent-like 系统**。  
> 面向对象：技术合伙人 / 产品 / 工程团队，帮助快速对齐“为什么要这么设计？”而不仅是“现在长什么样？”。

---

### 0. 总览表：V1–V7 体验向的演进对比

> 只看“对用户/测试同学来说感觉如何”，弱化内部技术细节。

| 版本 | 核心特性（一句话） | 用户体验（尤其长音频） |
|------|--------------------|-------------------------|
| **V1** | 能调用大模型给出答案 | 只能玩短问题/短音频 Demo，用起来像在和“会走神的 ChatGPT”聊天，结果不稳定、难复现 |
| **V2** | 有了简单的中间层 | 可以把一次录音完整跑完，但如果有问题，只能靠看一堆 log 去猜，调试成本高 |
| **V3** | 开始把“显示给人看”和“存进系统”的东西分开 | 长一点的音频可以跑，但一旦出错，很难快速还原“当时系统到底做了什么”，体验偏工程向 |
| **V4** | 正式区分：气泡给人看、JSON 给系统看 | 聊天气泡相对干净了，但长音频仍然是“等整段转完再看结果”，中途几乎没有可视反馈 |
| **V5** | 明确 XFyun 链路和调试信息该怎么看 | 可以比较稳定地跑几十分钟录音，HUD 也能看到关键信息，但用户/测试往往要“等到全部结束”才能判断好坏 |
| **V6** | 为长音频优化的“批次式智能助手” | 针对一小时级录音：不再是一口气等到底，而是按批次逐步产出结果；同时 HUD 有清晰的“进展和证据”，既不用干等、又能在出问题时快速定位是哪一段出错 |
| **V7** | 在 V6 之上“补齐大脑和仪表盘”的版本 | 在 V6 批次体验基础上：默认用更稳的 Tingwu+OSS 通道、把“客户画像+对话结论+会话管理”拆成 M1–M4 四层、引入两类 Agent 分工做分析和润色，并且 HUD 固定为三块可复制的调试面板，让长音频转写和智能分析更稳定、更容易排查问题，也更符合风控与合规要求 |

---

### 一、整体时间线概览

- **V1–V3：可行性探索期（Feasibility）**
  - 核心问题：先证明“能跑起来”，再谈“跑得好不好”。
  - 典型状态：单 Agent、大一统 Prompt、输出既给用户看、又给系统吃，边界模糊。
  - 结果：功能上“勉强可用”，但：
    - 难以复现同一个问题（缺乏稳定数据形态和 trace）；
    - 用户经常看到 AI 的“思考过程”，体验不专业；
    - 很难在不破坏用户体验的前提下做大改动。

- **V4–V5：协议 / 护栏期（Protocol & Guardrails）**
  - V4：首次引入 **标签化输出信道**：
    - `<Visible2User>`：给用户看的；
    - `<Metadata>`：给 Orchestrator/MetaHub 的 JSON；
    - `<Reasoning>/<DocReference>`：只给内部调试用。
  - V5：将 **XFyun-first / transfer-only / HUD 可观测性 / 不泄密** 写入正式规范：
    - `docs/Orchestrator-MetadataHub-V5.md`（已归档，见 `docs/archived/Orchestrator-MetadataHub-V5.md`）。
  - 结果：系统边界清晰很多，但整体还是“**单批次、单 Agent、弱结构化**”的时代。

- **V6：批次化 + 多 Agent + MetaHub 时代**
  - 核心关键词：
  - **BatchPlan**：以“批次”为单位规划可编辑窗口和只读上下文；
  - **SuspiciousHint**：只做“可疑边界提示”（hint-only），不直接改文案；
  - **DecisionRecord / MemoryBank / SpeakerLabelRegistry**：所有 LLM 输出都变成“结构化可验证的决策记录”；
  - **Pseudo Streaming**：按批次渐进输出，而不是 token 级流式；
  - **HUD/MetaHub**：所有 JSON 只允许“复制”，不允许“内联展示”。
  - 规范文件（已被 V7 继承和细化）：
  - `docs/archived/Orchestrator-MetadataHub-V6.md`
- **V7：M1–M4 元数据 + Provider lane 策略 + HUD 3 块（CURRENT）**
  - 核心关键词：
  - **M1–M4 分层模型**：UserMetadata / ConversationDerivedState / SessionState / External Knowledge & Style；
  - **Provider Policy（Tingwu+OSS 默认）**：Tingwu+OSS 恢复为默认转写通道，XFyun 保持“显式开启 + fail-soft”，禁止“试试看”；
  - **两 Agent 模型**：Agent1 负责记忆与 Smart Analysis，Agent2 负责 polish，边界更清晰；
  - **HUD 3 块**：运行快照 / 原始转写输出 / 预处理快照，全部 copy-only，禁止内联敏感 JSON；
  - **Pseudo-streaming + 导出门控**：继续使用批次式 pseudo-streaming，并通过 Smart Analysis + RenamingMetadata 控制导出命名与 gating；
  - 规范文件：
  - `docs/Orchestrator-MetadataHub-V7.md`（CURRENT）
  - 与之配套的 UX / API 合同：
  - `docs/ux-contract.md`
  - `docs/api-contracts.md`

---

### 二、角色与职责：从“谁都管一点”到“四层分工”

> 参考：`docs/archived/Orchestrator-MetadataHub-V4.md`、`docs/archived/Orchestrator-MetadataHub-V5.md`、`docs/Orchestrator-MetadataHub-V6.md`

- **V1–V3**
  - LLM：既负责“想内容”，也负责“组织 UI 结构”；输出格式随 Prompt 变化。
  - Orchestrator：更多是“薄封装”（转发请求、简单后处理），没有强 schema。
  - MetaHub：尚不存在或只是“轻量 JSON 记录”。
  - UI：既要解析 markdown，又要从尾部 JSON 猜测结构。

- **V4–V5**
  - LLM：
    - 按标签区分不同信道，例如 `<Visible2User>` 与 `<Metadata>`；
    - 但仍可能输出“自由 JSON”，由 Orchestrator 去兜底解析。
  - Orchestrator：
    - 负责解析标签、抽取 `<Metadata>` JSON、写入 MetaHub；
    - 开始变成“schema 护门员”，但尚未批次化。
  - MetaHub：
    - 统一存放会话级元数据（如 main_person/summary_title 等）；
    - upsert 规则相对稳定（非空覆盖、空值不清空）。
  - UI：
    - 明确只展示 `<Visible2User>`，其余全部视为调试/内部数据。

- **V6**
  - LLM Agents：
    - **强制 batch-scoped**：每个 Agent 只看当前批次 + 必要上下文；
    - **强制结构化输出**：例如 `DecisionRecord`、`MemoryPatch`、`LabelPatch`；
    - **所有失败都必须 fail-soft**（不能静默吞掉整个流程）。
  - Orchestrator：
    - 真正负责 **BatchPlan 计算、Hint 生成、调用 Agents、应用决策、生成 trace**；
    - 每一步都要留下可审计证据（batchPlans / hintsByBatch / decisionsByBatch 等）。
  - MetaHub：
    - 成为 **二级记忆与决策记录仓库**（memoryPatches / labelPatches / decision logs）；
    - 要求“有上限、有 schema、有可重复性”。
  - UI：
    - 只关心一件事：**稳定渲染 `MM:SS speaker: utterance`**；
    - HUD 只展示人类可读表格/列表，JSON 一律通过“复制”提供。

---

### 三、数据形态：从“自由文本 + 尾部 JSON”到“稳定模型族”

> 参考：`docs/Orchestrator-MetadataHub-V7.md` 第 3/4/6 节（M1–M4 / UtteranceLine / BatchPlan / SuspiciousHint / DecisionRecord / MemoryBank / LabelPatch）

- **V1–V3**
  - Prompt 输出大多是：
    - 一段自然语言解释 + 可能带一个 JSON/表格；
    - 没有明确的“主输出 vs 元数据”概念；
    - 解析经常要依赖“最后一个 JSON 对象”之类的启发式。

- **V4–V5**
  - 输出结构开始“二分法”：
    - `<Visible2User>`：UI 直接展示；
    - `<Metadata>{...}</Metadata>`：单个 JSON，写入 SessionMetadata；
    - 尾部 JSON 仍作为兼容回退。
  - 但整体还是“**以会话为单位的粗粒度 JSON**”，对逐行逐句的结构化支持有限。

- **V6**
  - 明确了一整套 **稳定数据模型族**：
    - `UtteranceLine`：每一行转写的**单一真相**（speakerKey、text、时间信息等）；
    - `BatchPlan`：可编辑窗口 / 只读窗口 / tail lookahead 的标准定义；
    - `SuspiciousHint`：可疑边界的 hint-only 数据（带 susId/batchId/globalBoundaryIndex/localBoundaryIndex）；
    - `DecisionRecord`：针对每个 hint 的决策记录（applyStatus / reason / safeExcerpts）；
    - `MemoryBank + MemoryPatch`：有 slot 上限的二级记忆；
    - `SpeakerLabelRegistry + LabelPatch`：说话人展示名的有限可变更系统。
  - 这些模型的特点：
    - **跨版本保持 shape 稳定**；
    - 可直接映射到 HUD/MetaHub/测试断言；
    - 为“多 Agent + 重放/回归测试”奠定基础。

---

### 四、可观测性与安全：从“多 log”到“有证据但不泄密”

> 参考：  
> - `docs/archived/Orchestrator-MetadataHub-V5.md` 中的 XFyun-first + transfer-only + SoT 规则  
> - `docs/Orchestrator-MetadataHub-V6.md` 第 8 节（Observability / Trace / HUD Policy）  
> - `docs/ToShareholders.md` 中对“专业度、安全性、可持续演进”的描述

- **V1–V3**
  - 主要依赖 logcat、本地文件、少量 HUD 标签来排错；
  - 信息多但无结构，跨版本难对比、难做自动化校验。

- **V4–V5**
  - 引入明确的 **HUD 期望与禁区**：
    - HUD 必须展示：baseUrl、orderId、轮询次数、HTTP code、failType 等非敏感字段；
    - HUD 必须禁止：密钥、签名、完整 query、原始音频内容等；
    - XFyun REST 细节统一收敛到 `docs/xfyun-asr-rest-api.md`，避免“多处复制不同步”。
  - XFyun 能力护栏（transfer-only）也在文档里固定下来，减少“试试看触发 failType=11”这种不专业行为。

- **V6**
  - 在 V5 基础上进一步升级：
    - **HUD：人类可读 + copy-only JSON**：
      - 屏幕上只出现表格、列表、简短摘要；
      - JSON 一律通过“复制”提供（例如 Copy batch plan JSON / Copy hints JSON）；
      - 永远不在 HUD 上内联展示 LLM raw JSON 或 XFyun HTTP raw body。
    - **Trace：必须有但必须安全**：
      - 必须记录 batchPlans / hintsByBatch / decisionsByBatch / memoryPatches / labelPatches；
      - 必须脱敏，禁止 secrets / signatures / raw provider body；
      - 既能为工程师提供“审计证据”，又能满足 ToShareholders 中承诺的“数据安全与合规”。

---

### 五、用户体验：从“看到 AI 思考过程”到“只看到专业结果”

> 参考：`docs/ToShareholders.md`（产品价值叙事）+ `docs/ux-contract.md` + `docs/Orchestrator-MetadataHub-V6.md` 第 6/7 节

- **V1–V3**
  - 用户经常看到：
    - Prompt scaffold、调试字段、未清理的 JSON；
    - AI “边想边说”的过程（包括不成熟的中间版本）。
  - 从 ToShareholders 视角看：
    - “像是在跟一个会走神的聊天机器人讲话”，而不是“在用一款专业工具”。

- **V4–V5**
  - UI 开始只展示 `<Visible2User>` 气泡；
  - 但在转写 / 后处理 / 多轮分析链路上，仍然存在：
    - 多次重写、用户难以理解“到底哪个版本是最终结果”的问题；
    - 调试信息与正式输出混在一起的边缘情况。

- **V6**
  - 渲染契约被明确写死为：
    - `MM:SS speaker: utterance`（时间戳 + 说话人 + 文本）；
    - 时间戳永不空白，缺失时有 fallback 且带 timeQuality 字段；
    - 说话人的真实含义由 SpeakerLabelRegistry 控制，弱推断不“乱起名”。
  - ToShareholders 里承诺的三点（更专业 / 数据驱动 / 完全自动化）在 V6 里都找到对应落点：
    - 专业：用户只看到“像资深销售顾问写的报告”，看不到 AI 垃圾中间态；
    - 数据驱动：BatchPlan / DecisionRecord / MemoryBank 为长期迭代提供“证据闭环”；
    - 自动化：整条链路从转写、hint、决策到渲染都可被自动回放与回归测试。

#### 长音频体验：从“一口气等完”到“边出结果边看”

- **V1–V3**
  - 一小时会议录音更像是“全拼人品”：要么一次性跑完，要么中间挂掉；
  - 用户基本只能“开好任务 → 去忙别的 → 过很久回来看看有没有结果”，缺少过程感。

- **V4–V5**
  - 系统在工程层面更稳了，长音频整体成功率提升；
  - 但从用户视角看变化不大：**依然是“等整段转完再看结果”**，中途只能盯着进度提示或 HUD 数字。

- **V6**
  - 默认假设录音可以很长（例如一小时），因此从体验设计上做了两件事：
    - 把整段逐字稿切成多个“小批次”，每批处理完成就可以更新一部分可见内容；
    - HUD 里可以看到“系统现在在处理哪一段、大概做了哪些决策”，而不是一片空白。
  - 对用户/测试来说，V6 的长音频体验可以总结成一句话：
    - **“不用干等到最后一秒，系统一边消化录音、一边给你看阶段性结果和证据。”**

---

### 六、小结：V6 为何是“agent-like 系统”的拐点？

- **从 Prompt 到协议**：V4–V5 让 LLM 不再是“自由发挥”，而是遵守标签/JSON 协议；
- **从协议到体系**：V6 把一次对话拆成若干批次，每个批次引入 hint、决策记录、记忆与标签补丁，真正支撑多 Agent 协作；
- **从 Demo 到产品**：借助可观测性、安全策略与 UX 合约，V6 让 SmartSales 从“AI Demo”升级为“可上线、可审计、可持续演进”的产品级系统。

### 七、小结：V7 对股东 / 业务方意味着什么？

用“外行视角”看，V7 在 V6 之上，重点做了三件事：

- **更稳的“进水口”**：  
  - 默认统一走 Tingwu+OSS 这条已经验证稳定的转写链路；  
  - XFyun 保持“显式开启 + 能力确认”后才使用，彻底杜绝“试试看就把配额和体验赌进去”的情况；  
  - 对股东来说，这意味着：**长音频任务成功率更高、不可预期的失败更少、云端费用更可控**。

- **更清晰的“资产分类账本”（M1–M4）**：  
  - 把“客户自身信息（M1）”、“这次对话里推断出来的信号（M2）”、“会话标题/导出命名等管理信息（M3）”和“公司级知识与话术包（M4）”彻底分开；  
  - 每一层都有自己的“谁能写、谁能读、带不带证据”的规则；  
  - 对股东来说，这意味着：**以后要做复盘、风控、合规审计或迁移到新系统时，有一套清楚的“总账 + 明细”可以查，不会陷在一团难以复用的大 JSON 里**。

- **更专业的“仪表盘 + 证据链”（HUD 3 块 + 两 Agent 分工）**：  
  - HUD 固定为三块：本次运行关键设置、原始转写 JSON、预处理后的摘要与可疑边界——全部可复制，但不会把敏感原始报文直接晾在 UI 上；  
  - 两类 Agent 各司其职：一个负责“记笔记 + 做分析结论”，另一个只负责“把逐字稿润色到专业可读”，避免单个大模型“一把抓”带来的不可控改写；  
  - 对股东来说，这意味着：**当销售/客服的智能分析结果有争议时，我们既能给出清晰的证据链，又能保证系统不会“私自篡改事实”，从而在专业度和合规性上站得住脚**。

综合来看：  
V6 把 SmartSales 从“AI Demo”拉到了“可上线的产品”；  
V7 则是在这个基础上，把“稳定性、可追责性和可扩展性”补齐到能长期支撑业务增长的水平——**更像一套可以放心托管长音频资产和销售洞察的基础设施，而不是一个好玩的 AI 小玩具**。

这份文档只是 V1–V7 的高维度对比；具体字段与不变量请始终以以下文档为准：

- CURRENT 规范：`docs/Orchestrator-MetadataHub-V7.md`
- 归档版本：`docs/archived/Orchestrator-MetadataHub-V6.md`、`docs/archived/Orchestrator-MetadataHub-V5.md`、`docs/archived/Orchestrator-MetadataHub-V4.md`
- UX 合约：`docs/ux-contract.md`
- API 合约：`docs/api-contracts.md`
- 产品叙事：`docs/ToShareholders.md`

