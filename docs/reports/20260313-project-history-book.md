# 智能销售架构演进：从设备接入端到数据驱动操作系统 (Data-Oriented OS)

**作者**: Senior Architect / Agent / Frank
**日期**: 2026-03-13
**详情级别**: 10/10 (源码级深度、架构演进、核心重构与范式转移)
**主题**: 记录系统如何从早期的文本处理管道，经历微服务化拆解与测试重塑，最终演进为基于 CQRS 双引擎和强类型契约的数据驱动操作系统。

> **"未经现实复杂场景 (L2) 验证的架构，往往隐藏着最深层的耦合。"**

---

## 阶段 0：早期探索与硬件接入 (Era 0: The Hardware Gateway)
**(2026年2月上旬)**

项目的最初始版本（基线提交 `724c0da7`）侧重于物理硬件的集成。Smart Sales 的初代形态主要是一个负责与外部录音硬件通信的网关应用。

### 源码级结构与功能设定
*   **通信与拉取 (`TingwuPollingLoop.kt` & `DeviceManager`)**: 应用的核心机制是基于 HTTP/BLE 的设备发现轮询 (`2007b155`)。通过 `AudioFiles ViewModel` (`f122b718`) 将外部录音媒体文件系统性地拉取到本地。
*   **基础转写管道 (`TingwuCoordinator` & `TingwuTranscriptProcessor.kt`)**: 本地音频文件通过阿里云听悟 API 被转写为大规模的、无明确标识结构的自然文本格式。
*   **无状态处理模式**: 此时，应用缺乏上下文感知能力。它主要负责网络 I/O 与基础的文本展示，并不“理解”客户身份或 CRM 的关联关系。其核心逻辑是将接收到的文本追加到基础的 `ChatHistory` 视图中。

---

## 阶段 1：自然语言驱动与单体架构局限 (Era 1: The Monolithic AI Wrapper)
**(2026年2月中旬)**

随着文本数据流转顺畅，业务诉求从“纯记录”升级为“语义分析”。大语言模型 (LLM) 被引入，应用开始具备了意图解析的能力。但受限于既有的媒体播放器底层，早期的 AI 机制存在一定程度的设计妥协。

### 架构现状：Lattice 模式与 `app-core` 单体
*   **高度集中的单体工程**: 为了实现快速迭代，网络层、持久化层、UI 渲染以及 LLM 请求逻辑被高度耦合在 `:app-core` 模块中，直接导致了 Gradle 构建成本的大幅增加。
*   **Lattice 测试边界**: 你开始尝试引入 Lattice 模式（Interface + Impl + Fake，见 `AiChatService.kt`）来建立基础的接口隔离，但在物理包层级，组件依然紧密交织。
*   **编年史式的 MetaHub (`MetaHub.kt`)**: 持久化机制由同步的单例 Hub 控制。数据严格按照处理顺序存储：
    *   **M1 (Hot Zone)**: `ConversationDerivedState.kt` 管理原始碎片对话。
    *   **M2 (TranscriptMetadata)**: 初步结构化分析数据。
    *   **M3 (SessionMetadata)**: 会话生命周期结束后的归档。
    *   **架构瓶颈**: 此时的数据库 I/O 是强同步的。任何与 LLM 结果关联的落库操作都会短暂阻塞主线程 (UI Thread)。

### 机制隐患：非结构化解析与正则表达式
*   **非结构化 Prompt 的不可控性**: `PromptCompiler` 主要通过发送长篇幅的自然语言模板来指导 LLM。系统寄希望于 LLM 能够返回格式稳定的文本描述。
*   **基于正则表达式的解析瓶颈**: 下游的处理模块（如早期的 `SchedulerLinter`）试图通过正则表达式技术来检索 LLM 输出文本中的时间、联系人和意图分类。
*   **数据幻觉 (Hallucinations) 与状态不一致**: LLM 偶尔会偏离指令，输出错误格式的字段或伪造出数据库中不存在的 CRM 枚举值（如不存在的商机阶段）。这种突发性的非结构化输出会导致正则匹配失效，引发关键流程（如定时任务创建、属性更新）的空指针异常 (NullPointerException)。

---

## 阶段 2：核心层解耦与领域模块化 (Era 2: The Monolith Purge & Domain Modularization)
**(2026年2月下旬 - 3月上旬)**

不可控的耦合问题随着 Scheduler 等大型功能的集成迅速暴露。为此，你启动了代号为 **“大组装” (The Great Assembly)** 的工程拆分计划。

### 物理模块解耦与纯粹领域模型 (Domain Purity)
*   **解体 `PrismDatabase`**: 单一的 Room Database 被完全拆解。你在基础设施（Room DAOs/Retrofit）与纯 Kotlin 领域模型之间建立了物理意义上的防腐层（参考 `Docs: Prism §7` 指南）。
    *   重构产出了职责单一的数据层模块：`:data:crm`、`:data:habit`、`:data:memory` 和核心的 `:core:database`。
    *   针对领域层 `:domain:*` 制定了严苛的约束规则：禁止引入任何 `android.*` 依赖。
*   **Relevancy 模型的演进**: 抛弃了僵化的 M1/M2/M3 时间线逻辑。引入了基于向量相似度的 `"相关性库" (Relevancy Library)`，以及支持异步读写的 `"热区/水泥区" (Hot/Cement Zone)` 上下文管理机制。同时全面启用了“异步非阻塞”(Fire-and-forget) 写入模式，彻底释放了主线程。

### 系统拆分 (System I vs System II)
*   为了更合理地分配 LLM 算力，降低 API 成本，系统路由被重新设计。
*   构建了极低延迟的 `MascotService` (System I)，专门过滤并处理日常寒暄及无效噪声；而运算量巨大的 `PrismOrchestrator` (System II) 则专注于多轮意图识别与业务突发事件。

---

## 阶段 3：L2 复杂场景验证与架构反思 (Era 3: The Routing Assessment & Architecture Pivot)
**(2026年3月上旬)**

虽然代码在物理上已经实现了高内聚低耦合，但用于传递意图的 Payload 本质上依然是“自然语言”，这掩盖了系统最深层的脆弱性。

### Mock 驱动测试的局限性 (The Testing Illusion)
*   在回顾早期的单元测试时，你发现过度依赖 `Mockito`（`mock()`, `whenever()`）造成了“高代码覆盖率”的假象。它完美通过了独立模块测试，但掩盖了 `PipelineContext`（RAM 上下文）在多级状态流转时的拼装缺陷。

### 3月12日：`L2WorldStateSeeder` 极限验证
*   为验证真实的鲁棒性，你引入了完全无 Mock 代理的 `Fake*Repository`，并注入了一组重度干扰的 B2B 上下文（如：数据库中预埋了三位重名的“沈总”，且对话存在高频的重点切换）。
*   **暴露问题**: 实测结果导致 Pipeline 判定失效。由于上下文拼装器未能在多级跳转中稳定维持实体指针 (Entity Handle)，LLM 出现了“上下文失忆”(Entity Amnesia)。例如，在收到“修改商机状态”的指令时，错将高优先级的 CRM 状态更新识别为了 Scheduler 的普通备忘录记录，导致严重的业务数据流失。

### 引入工程标准：四大支柱 (The Four Pillars)
*   你正式叫停了针对抽象层的过度封装，转而确立了务实的工程准则：
    1.  **功能独立性 (Feature Purity)**
    2.  **反幻觉测试验证 (Anti-Illusion Testing)**：废弃 Mockito，启用高仿真 Repository Fakes。
    3.  **UI 字面量映射一致性 (UI Literal Sync)**
    4.  **完整的审计可观测性 (Observable Telemetry)**
*   并引入 `/feature-dev-planner` 工具流，要求所有的代码实现必须强关联标准的 `spec.md` 架构契约。

---

## 阶段 4：Project Mono 与数据操作系统 (Era 4: Project Mono & The Data-OS)
**(2026年3月中旬 - 至今)**

L2 验证危机直接孵化了现阶段的核心架构：**Project Mono**。该架构的最核心论点是：“如果 LLM 输出的数据格式与底层数据库模型 (Entity Graph) 的要求不提供绝对的一致性保证，模块化就失去了意义。”

### 统一数据契约 (The "One Currency" Contract)
*   在 `:domain:core` 中确立了核心地位的 `UnifiedMutation` 数据类。
*   基于对大模型的工程化应用约束，LLM 在管道中不再被视为“拟人化助理”，而是受控的 **结构化数据处理器**。`PromptCompiler` 通过反射扫描 `kotlinx.serialization` 的元数据 (`UnifiedMutation.serializer().descriptor`)，动态编译出严格的 JSON Schema 格式文件并注入给 LLM，要求 LLM 执行严格的键值对填充。

### 强类型检查器 (The Typed Linter)
*   清除了此前维护成本极高的冗余正则表达式校验逻辑。
*   在 Linter 层级，使用原生的 `decodeFromString<UnifiedMutation>()` 对输出 Payload 进行数学层面的单一校验。
*   **安全截断机制**: 遇到 LLM 幻觉产生的不合规字段、超出限定范围的枚举（如伪造权限字），系统将直接在当前执行栈抛出安全的 `SerializationException`。脏数据可以在落盘前被瞬时丢弃或重试，从而彻底阻断数据污染向持久层的扩散。

### 最终形态：CQRS 双并发管道 (Dual-Loop Engine)
为了彻底解决深层 B2B 上下文推理与产品交互响应速度之间的矛盾，执行管道最终升级为彻底的读写分离模式 (CQRS)：
1.  **同步查询环 (The Sync Loop)**: `LightningRouter` 搭配一级缓存 `Alias Lib`。以亚秒级耗时解析关联 Entity ID。若能在缓存内查得意图实体，便绕开深度图查询立刻构建 RAM Context 并投射 UI 反馈。真正做到了交互的“所见即响应”。
2.  **异步突变环 (The Async Loop)**: 那些伴随重度耗时的过程，如 `EntityWriter` 对底层 CRM 对象集的事务更新，以及 `RL Module` 试图识别并持久化的 UserHabit，被悉数委托至后台挂起协程 (`Coroutines Background Scope`)。主线程继续推动对话流转，而异步任务完结后再将变更事件无缝反压回当前 Context，真正实现了流畅对话与后台计算的彻底解耦。

### 架构演进总结 (Architectural Retrospective)
历时数周的集中攻坚，Smart Sales 完成了一次令人瞩目的底层重构。

它从最原始的蓝牙协议转写应用，演变为基于字符串搜索与正则匹配的初期 AI 产品，进而经历了剥离痛楚的物理结构大解体，在最拟真的 L2 场景的暴风骤雨后重新沉淀。
最终，它剥离了那些脆弱的假象，落地为一套 **约束在 Kotlin 强类型安全系统内、数学层面防雪崩、具备 CQRS 高性能读写分离架构的数据驱动操作系统 (Data-Oriented OS)**。
