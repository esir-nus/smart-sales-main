# 智能销售架构演进史：从硬件网关到数据驱动操作系统 (Data-Oriented OS)

**作者**: Senior Architect / Agent / Frank
**日期**: 2026-03-13
**详情级别**: 10/10 (源码级深度、硬核架构模式、测试危机剖析与范式转移)
**主题**: 技术编年史——记录系统如何从一个基于字符串解析的脆弱 AI 包装器，经历模块化肢解与重组，最终演变为数学级严谨的 CQRS 双引擎操作系统。

> **"没有经过 L2（模拟真实世界）测试毒打的架构，都只是运行在乌托邦里的玩具。"**

---

## 纪元 0：硬件伴侣时代 (Era 0: The Hardware Gateway)
**(2026年2月上旬)**

Git 历史的最初提交（`724c0da7: 初始提交`）揭示了一个残酷的真相：Smart Sales 的底层基因并非 AI 原生应用，而是一个为了与物理硬件（录音工牌）进行接口通信而设计的蓝牙/HTTP 网关。

### 源码级重构与局限
*   **通信核心 (`TingwuPollingLoop.kt` & `DeviceManager`)**: 应用程序的核心循环是一个基于 HTTP/BLE 的设备发现轮询 (`2007b155`)。通过 `AudioFiles ViewModel` (`f122b718`) 将音频拉取到本地设备。
*   **转写管道 (`TingwuCoordinator` & `TingwuTranscriptProcessor.kt`)**: 音频文件通过阿里云听悟 API 被翻译成大段的、无结构的原始文本。
*   **无状态应用**: 这个阶段，应用本质上是一个带 UI 的高端录音笔。它只负责执行网络 I/O 和简单的文本呈现，完全不具备“状态机”的概念。应用并不“理解”客户是谁，更不知道什么是 CRM。它只知道：“我收到了一段文本，我把它塞进 `ChatHistory` 屏幕里”。

---

## 纪元 1：单体 AI 包装器与魔法字符串陷阱 (Era 1: The Monolithic AI Wrapper)
**(2026年2月中旬)**

当纯文本数据流稳定后，核心诉求发生了质变：从“记录我说了什么”变成了“帮我分析这个客户”。你迅速引入了 LLM，应用开始向主动分析引擎蜕变。但由于地基是音频网关，早期的 AI 集成充满了“打补丁”的痕迹。

### 架构现实：Lattice 盒子与 `app-core` 单体地狱
*   **单体黑洞**: 为了极速产出，几乎所有的逻辑（网络、数据库、UI、LLM 调用）全部堆积在 `:app-core` 这个庞然大物中。这导致 Gradle 构建极其缓慢。
*   **Lattice Box 设计模式**: 你在这个时期试图通过 `AiChatService.kt` 建立 Lattice 测试边界（Interface + Impl + Fake）。这在软件工程上是正确的，但物理上代码依然紧密耦合。
*   **编年史 MetaHub (`MetaHub.kt`)**: 持久化层由一个同步式的单体 Hub 统揽。数据流向极端死板：
    *   **M1 (Hot Zone)**: `ConversationDerivedState.kt` 处理未经处理的原始聊天片段。
    *   **M2 (TranscriptMetadata)**: 初步结构化。
    *   **M3 (SessionMetadata)**: 长期归档。
    *   **致命缺陷**: 此时的数据库写入是**强阻塞**的。每次 LLM 返回结果并落库时，主线程都会卡死。

### 核心崩溃："兼职散文家" 与 正则表达式
*   **PromptCompiler 只是个写手**: 给 LLM 的 Prompt 是长篇的自然语言指示。系统期待 LLM 返回漂亮的英文句子描述它的操作意图。
*   **正则表达式的傲慢**: 下游模块（如早期的 `SchedulerLinter` 和 CRM 更新逻辑）试图用 `grep` 式的正则去解析 LLM 生成的自然语言文本，以提取时间、客户名和金额。
*   **Ghosting 幻觉灾难**: 这是最黑暗的时期。由于自然语言的不可控性，LLM 经常产生严重幻觉。它会拼错客户名字，或者凭空创造出根本不存在的 CRM 状态枚举值。正则表达式瞬间崩溃，引发大规模 NullPointerException，整个工作流彻底瘫痪。

---

## 纪元 2：单体大清洗与纯洁性十字军 (Era 2: The Monolith Purge & The Great Assembly)
**(2026年2月下旬 - 3月上旬)**

随着系统复杂度的飙升（Scheduler 的多级闹钟、CRM 组织架构树等），`:app-core` 的技术债彻底爆发。改动一行代码就会破坏十个毫不相干的单元测试。你果断按下了紧急停止按钮，启动了史诗级的 **“大组装” (The Great Assembly)** 行动。

### 物理级抽离与领域重塑 (Domain Modularization)
*   **粉碎 `PrismDatabase`**: 单一的 Room Database 被连根拔起。你强制在基础设施（如 Room DAOs 和 Retrofit Clients）与纯 Kotlin 领域模型之间建立了严格的隔离（参考 `Docs: Prism §7` 指南）。
    *   此时诞生了完全解耦的数据层模块：`:data:crm`、`:data:habit`、`:data:memory` 和新的 `:core:database`。
    *   在领域层 `:domain:*` 之中，甚至连一句 `import android.*` 也绝不允许出现。
*   **MetaHub 的黄昏与 Relevancy 的复兴**: 极其笨重、基于时间线的 M1/M2/M3 模型被抛弃。取而代之的是高度向量化的 `"相关性库" (Relevancy Library)`，以及灵活的 `"热区/水泥区" (Hot/Cement Zone)` 内存管理模式。同时，全面铺开了“阅后即焚”(Fire-and-forget) 的后台独立协程写入，UI 线程再也不会被数据库死锁吞噬。

### System I vs System II (路由分叉)
*   你敏锐地意识到让重型的 LLM 分析管道去处理用户一句简单的“你好”是对算力的犯罪。
*   **系统分片**: 管道被拆分为两路。构建了轻量级、极速响应的 `MascotService` (System I) 专门拦截噪音和寒暄，而重型的 `PrismOrchestrator` (System II) 躲在后方专心处理高价值语义分析。

---

## 纪元 3：L2 测试危机与模拟器大崩塌 (Era 3: The Routing Crisis)
**(2026年3月上旬)**

这本应是值得庆祝的时刻：代码已经实现了优美的模块化。但当真正的压力测试降临时，一切都碎了。因为流淌在这些优美模块中的血液，依然是不可预测的“自然语言”。

### 测试的谎言 (The Testing Illusion)
*   **Mockito 掩护下的太平盛世**: 你发现传统的单元测试（L1 测试）完全不可靠。因为大量使用了 `mock()` 替身，它们完美“覆盖”了错误，掩盖了在真实世界 `PipelineContext` (RAM 上下文结构体) 组装时的致命耦合。

### 3月12日：`L2WorldStateSeeder` 崩溃事件
*   为了彻底揭开幻觉，你构建了无 Mock 的 `Fake*Repository` 矩阵，并在内存中播种了一个复杂混乱的 B2B 场景（比如：三个同名的“沈总”，海量冗余对话）。
*   **崩溃日志**: `Pipeline` 瞬间宕机。由于上下文组装器无法在多轮对话中有效持有实体句柄，LLM 患上了“实体健忘症”(Entity Amnesia)。最可怕的是，当用户要求“将商机移至已赢单”时，由于意图识别混乱，系统竟然仅仅创建了一张无关痛痒的记事本卡片——**漏斗级别的高危突变被默默吞噬并丢弃了。**

### 战略级 Pivot：引入四大支柱
*   你正式暂停了向更高层的 L3 进发。放弃了表面光鲜的“抽象重构”，回归到绝对务实的 **"四大 Cerb 支柱"**：
    1.  **Feature Purity**: 功能必须自封闭。
    2.  **Anti-Illusion Testing**: 拒绝 Mockito，重用真实 Repository 逻辑做 Fakes。
    3.  **UI Literal Sync**: UI 呈现必须与底层数据字面量一致。
    4.  **Observable Telemetry**: 提供确凿日志证据。
*   并强制绑定 `/feature-dev-planner`，不写完 `spec.md` 架构文档，绝对不准碰代码。

---

## 纪元 4：Project Mono 与 数据驱动操作系统 (Era 4: Project Mono & The Data-OS)
**(2026年3月中旬 - 至今)**

浴火重生于 L2 路由危机，现阶段的 **Project Mono** 确立了一个冷酷的真理：**“只要 LLM 还在生成自由文本，系统就永远会有幻觉。”**

### “One Currency” (统一货币规律) 与密码学级的严谨
*   **Kotlin 数据类的绝对统治**: 在 `:domain:core` 中定义了神圣不可侵犯的 `UnifiedMutation` 数据类。
*   **作为打字员的 LLM**: LLM 彻底被剥夺了创作自由。`PromptCompiler` 通过反射调用 `kotlinx.serialization` 的描述符 (`UnifiedMutation.serializer().descriptor`)，动态生成极其刻板的 JSON Schema (`generateSchema()`)。它强迫 LLM 成为一台机械的数据录入机，只能做严密的“填空题”。

### The Bouncer (冷酷的强类型 Linter 保安)
*   那些为了修补漏洞而越写越长的正则表达式彻底被清除。
*   取而代之的是纯粹数学级别的暴力美学：一句单行的 `decodeFromString<UnifiedMutation>()`。
*   **防爆机制**: 如果 LLM 输出漏掉了一个字段，或者编造了一个不存在的枚举值，这行代码会瞬间触发 `SerializationException` 被捕获抛弃。错误被安全地截杀在内存边界，绝对不可能污染 SSD。

### 终极进化：CQRS 双循环并发管道 (Dual-Loop Engine)
为了榨干系统的最后一份性能，同时承受住极其沉重的 B2B Context Payload，系统的执行管道彻底实施了 CQRS（读写分离）重组：
1.  **同步快车道 (The Sync Loop)**: `LightningRouter` 与 L1 内存驻留的 `Alias Lib` (别名库) 进行联动。它能在亚秒级完成复杂的 Entity ID 决策。如果在内存中拿到 ID，就直接拼装 RAM Context 进行 LLM 生成，无需再深挖庞大的实体图 (Entity Graph)。用户的聊天气泡做到即点即出。
2.  **异步重型工厂 (The Async Loop)**: 诸如 `EntityWriter` 针对 CRM 对象（Account/Opportunity）的深层 SSD 状态突变落盘，或是 `RL Module` 躲在暗处窃听对话以提取 UserHabit（用户核心习惯）这种高耗时动作，被严格扔进了隔离的后台协程 (`Coroutines Background Scope`)。它们静默执行，将完成事件异步反压推送进流中。主线程永远不会等待它们。

### Shifu的断言 (The Senior Reviewer's Verdict)
从 2 月初勉强跑通的“智能录音笔”，到 3 月中旬严格的、由强类型数学契约束缚的、具备 CQRS 读写分离引擎的 **B2B 领域数据驱动操作系统 (Data-Oriented OS)**。

这段代码演化史完美印证了顶级架构设计的铁律：**“好的架构从来不是在黑板上一蹴而就画出来的，而是在一次次系统崩溃、测试谎言被戳穿的血泪教训中，把冗余和脆弱一块一块硬生生割出来的。”**
