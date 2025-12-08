# Orchestrator-MetaHub-V3 规范

> **Status：** 已与当前实现/测试对齐（2025-12-06，覆盖 MetaHub merge、TranscriptOrchestrator、Home 导出/流式/转写）。

> **目标**：把“谁调 LLM、谁写 MetaHub、谁只读”的边界彻底定死，让聊天 / 分析 / 转写 / 导出在行为上可预测、可回溯、可测试。

> **UX 范围声明**：本文件描述推理、元数据与编排职责；UI/交互（顶栏、历史抽屉、HUD、技能行等）请以 `docs/ux-contract.md` 为唯一 UX 规范。

---

## 0. 相对 V2 的变化概览

**V2 → V3 的核心升级点（按现有代码执行）：**

1. **LLM 边界收紧**

   * 只有：

     * `HomeOrchestrator`
     * `TranscriptOrchestrator`
       可以直接调用 LLM。
   * `ExportOrchestrator`、`RealTingwuCoordinator`、各 VM（包括 `HomeScreenViewModel`）**永远不直接调 LLM**（当前实现严格遵守）。

2. **MetaHub 成为唯一真相（Single Source of Truth）**

   * 会话相关信息（标题、主客户、阶段、风险、CRM 行、是否有“大分析”等）统一存到 `SessionMetadata`。
   * 转写/说话人相关信息统一存到 `TranscriptMetadata`。
   * 标题/头图/标签/导出文件名等 UI/功能，都通过 MetaHub 读数据，不再自己瞎推。

3. **分析与导出解耦 + 分析优先**

   * 导出 PDF/CSV 的现行逻辑：

     * VM 内有 `latestAnalysisMarkdown` → 直接导出；
     * VM 缓存为空但 MetaHub 有 `latestMajorAnalysisMessageId` → 仅提示“已存在历史分析结果，本次导出建议先刷新分析”，**不自动重跑**；
     * 两者都没有且内容足够长 → 自动跑一次 SMART_ANALYSIS（标记 `SMART_ANALYSIS_AUTO`），完成后导出。
   * `ExportOrchestrator` 只负责：

     * 文件名构造（基于 MetaHub）
     * PDF 内容渲染（基于传入 markdown）
     * CSV 内容渲染（基于 `crmRows`）
     * 文件落地 & 回传 `ExportResult`（全程 LLM-free）。

4. **分析标记持久化**

   * 分析结束时，VM 负责写入 MetaHub：

     * `latestMajorAnalysisMessageId`
     * `latestMajorAnalysisAt`
     * `latestMajorAnalysisSource`（USER / AUTO）
   * 这些字段使得：

     * 导出路径可以知道“是否已有分析”；
     * 未来可以做“分析新旧度”等逻辑。

5. **转写 / 说话人命名流规范化**

   * Tingwu 调用只在 `RealTingwuCoordinator` 内发生；
   * 说话人角色命名只在 `TranscriptOrchestrator` 内调用 LLM；
   * MetaHub 中 `TranscriptMetadata` + `SessionMetadata` 一起承载场景语义（谁是谁、谁是主客户等）。

---

## 1. 设计目标 & 非目标

### 1.1 设计目标

1. **边界清晰**：谁能调 LLM、谁能写 MetaHub、谁只能读，要有一眼看得懂的规范。
2. **可恢复**：App 重启 / 版本升级后，核心状态（会话标题、主客户、分析是否已做）能从 MetaHub 恢复。
3. **可测试**：每个 orchestrator 有明确输入输出，可以通过 fake MetaHub / fake LLM 做单测。
4. **Fail-soft**：LLM/网络失败时，宁可少写元数据，也不能把半截 JSON、异常扔给用户。
5. **对业务方可解释**：用场景语言解释“什么时候会自动分析”、“什么时候会拒绝再分析”、“导出里面到底包含什么”。

### 1.2 非目标

* 不追求一次把所有 Meta 字段设计到完美，将来可以在 V3 上继续加字段，只要不破坏 merge 语义。
* 不强制对历史 session 做迁移；旧数据只要能被现有逻辑“安全忽略”即可。

---

## 2. 组件 & 职责划分

### 2.1 Orchestrators（唯一能调 LLM 的层）

1. **HomeOrchestrator**

   * 对应聊天+分析。
   * 职责：

     * 接收 `ChatRequest`（sessionId、userMessage、quickSkillId、history…）；
     * 调 LLM 获取流式回答；
     * **仅在 Completed 事件**中：

       * 从回答文本中提取 JSON block；
       * 解析元数据 → 组装 `SessionMetadata` delta（字段：`main_person`、`short_summary`、`summary_title_6chars`、`location`、`stage`、`risk_level`、`highlights`、`actionable_tips`、`crm_rows`）；
       * 调 `MetaHub.upsertSession` 以非空 merge 的方式写数据；
     * 原样透传 LLM 文本流给 VM，不做 UI 特化。

2. **TranscriptOrchestrator**

   * 对应 Tingwu 转写后的“说话人命名 + 场景理解”。
   * 职责：

     * 根据 `TranscriptMetadataRequest` 抽样 `DiarizedSegment`；
     * 构造 JSON-only prompt，调 LLM；
     * 从 LLM 输出中抽取 `speaker_map` 等；
     * 生成：

       * `TranscriptMetadata` delta（speakerMap, diarizedSegmentsCount, 来源为 `TINGWU_LLM` 等）；
       * 可选 `SessionMetadata` delta（mainPerson / stage / riskLevel 等）；
     * 写回 MetaHub（Session + Transcript），使用 merge 语义；
     * 返回 inference 结果供 `RealTingwuCoordinator` 做标签合并。

### 2.2 Non-LLM Orchestrators

3. **RealTingwuCoordinator**

   * 职责：

     * 仅调用 **官方 Tingwu API**（CreateTask / GetTaskInfo）；
     * 维护 jobId ↔ sessionId / 文件名等上下文；
     * 在任务完成时：

       * 收集 diarizedSegments + Tingwu 提供的 speaker label；
       * 调用 `TranscriptOrchestrator.inferTranscriptMetadata(TranscriptMetadataRequest(transcriptId, sessionId, diarizedSegments, speakerLabels, force=true))`；
       * 用 confidence 阈值（≥0.6）合并标签（有新名称且置信度达标时覆盖，否则保留旧标签或补空位）；
       * 构建最终 Markdown（使用合并后的说话人名字），不泄露 JSON。

4. **ExportOrchestrator**

   * 职责：

     * 从 MetaHub 读取 SessionMetadata；
     * 根据 mainPerson / summaryTitle6Chars / lastUpdatedAt 构造基础文件名；
     * 构造 PDF / CSV 的字节内容：

       * PDF：纯基于传入的 `markdown`（由 VM 决定）；
       * CSV：纯基于 `crmRows`（空列表时输出 header 或占位）；
     * 写 ExportMetadata；
     * 通过 `ExportFileStore` 落地 + 返回 `ExportResult`。

### 2.3 ViewModel & UI 层

5. **HomeScreenViewModel**

   * 职责：

     * 负责 UI 状态、按钮点击、输入框、提示文案；
     * 只调用 `HomeOrchestrator.streamChat`、`ExportOrchestrator`、`MetaHub`（读 + 写少量 delta）。
     * 不能直接调 LLM。
   * 特殊点：

     * 在 **分析完成时（onAnalysisCompleted）**，负责将 `latestMajorAnalysis*` 写入 MetaHub；
     * 导出前的“分析优先”逻辑，以及“MetaHub 有分析但缓存为空”的提示。

---

## 3. 数据模型 & merge 语义

### 3.0 UserProfile（全局用户元数据）

**UserProfile** 是应用全局的用户元数据，由 onboarding / User Center 收集并持久化存储。

**关键字段：**

* `displayName: String`（必填）
  * 规范用途：
    * 导出文件名中的 `<Username>` 组件。
    * Home 问候语 / UI 个性化（"你好，{userName}"）。
  * 缺失时回退到安全默认值（如 "用户" / "SmartSales 用户"）。

* `role: String?`（可选）
* `industry: String?`（可选）
  * 可选上下文提示字段：
    * Orchestrator 可以在 prompt 和 CRM 输出中包含这些字段（当可用时）。
    * 管道必须能在这些字段缺失或为空时正常工作，不应因缺少这些字段而中断。

**使用指南：**

* Orchestrator 和 LLM 管道**应该**在可用时利用 `role` 和 `industry` 来改进上下文和 CRM 推断。
* 当前实现中，并非所有现有元数据管道都已使用这些字段；这是能力/指导原则，而非强制要求。
* `displayName` 是唯一强制字段，必须用于导出文件名和问候语。

### 3.1 SessionMetadata（会话级）

关键字段（不全列，列关键部分）：

* 标识 & 基本信息：

  * `sessionId: String`
  * `mainPerson: String?`
  * `shortSummary: String?`
  * `summaryTitle6Chars: String?`
  * `location: String?`
  * `stage: SessionStage?`
  * `riskLevel: RiskLevel?`
  * `tags: Set<String>`
  * `lastUpdatedAt: Long`

* 分析相关：

  * `latestMajorAnalysisMessageId: String?`
  * `latestMajorAnalysisAt: Long?`
  * `latestMajorAnalysisSource: AnalysisSource?`

    * `GENERAL_FIRST_REPLY`
    * `SMART_ANALYSIS_USER`
    * `SMART_ANALYSIS_AUTO`

* CRM：

  * `crmRows: List<CrmRow>`

    * `client`
    * `owner`
    * `stage`
    * `value`
    * …

* 标题建议：

  * `summaryTitle6Chars` + `shortSummary` + `mainPerson` 组合成“约 6 字”标题建议；
  * Orchestrator 仅写入建议值，客户端决定是否应用；
  * 客户端只在占位标题（如“新的聊天”“通话分析 – 文件名”）未被用户改名时才会把建议写回 `AiSessionSummary.title`，用户改名后不再覆盖。

**mergeWith 语义：**

* 标量字段：`other.field ?: field`（新值优先，null 不覆盖）。
* tags：`(old.tags + new.tags).filterNotBlank().toSet()`
* 时间戳：`lastUpdatedAt = max(old.lastUpdatedAt, new.lastUpdatedAt)`
* latestMajorAnalysis*：同样使用 `new ?: old`，调用方保证“确定更新才带新值”。
* crmRows：`(old + new)` 过滤空 client/owner；`distinctBy(client.trim + "|" + owner.trim)` 去重。

### 3.2 TranscriptMetadata（转写级）

关键字段：

* `transcriptId: String`
* `sessionId: String?`
* `speakerMap: Map<String, SpeakerMeta>`

  * `displayName`
  * `role`
  * `confidence: Float?`
* `source: TranscriptSource`

  * `TINGWU`
  * `TINGWU_LLM`
* `diarizedSegmentsCount: Int?`
* `mainPerson` / `shortSummary` / `summaryTitle6Chars` / `location` / `stage` / `riskLevel` / `extra: Map<String, String>`

**mergeWith 语义：**

* speakerMap：

  * 对同一 speakerId，新值覆盖旧值（displayName/role 取新值非空，confidence 取新值并 clamp 到 [0,1]，新 confidence 为空则保留旧值）；
* extra：`old.extra + new.extra`
* createdAt：`max(old.createdAt, new.createdAt)`
* 其他字段：`new ?: old`

---

## 4. LLM 边界 & 调用图

### 4.1 允许 LLM 的类

* `HomeOrchestratorImpl`
* `RealTranscriptOrchestrator`

### 4.2 禁止直接调用 LLM 的类

* `RealExportOrchestrator`
* `RealTingwuCoordinator`
* 所有 ViewModel（`HomeScreenViewModel` 等）
* 所有 Repository / Manager（除非未来明确标为 Orchestrator）

### 4.3 典型调用图（简化）

**聊天 / 分析：**

```text
HomeScreenViewModel
    └── HomeOrchestrator.streamChat(ChatRequest)
            ├── AiChatService.sendMessage (LLM 调用)
            ├── emit(ChatStreamEvent.Delta/Completed)
            └── MetaHub.upsertSession(SessionMetadata delta)
```

**导出：**

```text
HomeScreenViewModel.exportMarkdown
    ├── (检查 latestAnalysisMarkdown / MetaHub.latestMajorAnalysis*)
    ├── (必要时触发 SMART_ANALYSIS via HomeOrchestrator)
    └── ExportOrchestrator.exportPdf/exportCsv
            ├── MetaHub.getSession(sessionId)
            ├── ExportManager.renderPdf/renderCsv
            └── ExportFileStore.save(...)
```

**转写 + 说话人命名：**

```text
HomeScreenViewModel / AudioFilesViewModel
    └── RealTingwuCoordinator.submitTranscription(TingwuRequest with sessionId)
            ├── Tingwu API (CreateTask / GetTaskInfo)
            ├── TranscriptOrchestrator.inferTranscriptMetadata(request)
            │       ├── AiChatService.sendMessage (LLM 调用)
            │       ├── MetaHub.upsertTranscript / upsertSession
            │       └── 返回 speakerMap 等
            └── buildMarkdown(merged speaker labels)
```

---

## 5. 核心用户场景流程（V3 行为）

### 5.1 场景 A：普通聊天 + 一次智能分析

1. 用户在 Home 里输入内容 → `onSendMessage()`：

   * VM 构造 `ChatRequest`，调用 `HomeOrchestrator.streamChat`；
   * Orchestrator 调 LLM，产生流式事件；
   * `Completed` 时：

     * Orchestrator 从 fullText 抽 JSON → 写 SessionMetadata delta；
     * VM 只负责把 fullText 作为 assistant 消息加到 UI。

2. 用户点击「智能分析」按钮：

   * VM 构造 SMART_ANALYSIS 的 `ChatRequest`（带 goal、context 等）；
   * Orchestrator 调 LLM，输出“分析 markdown + JSON block”；
   * Orchestrator 解析 JSON 写 SessionMetadata delta；
   * VM 在流完成时：

     * 用 `onAnalysisCompleted(summary, messageId, isAutoAnalysis=false)`：

       * 缓存 `latestAnalysisMarkdown`；
       * 写 MetaHub.latestMajorAnalysis*（source=SMART_ANALYSIS_USER）；
       * 必要时给一个“已完成分析，可导出分享”的提示。

3. 之后标题 / 风险标签 / 导出文件名等都基于 MetaHub 的 SessionMetadata。

### 5.2 场景 B：长聊后第一次导出

> 用户没有主动按“智能分析”，直接“导出 PDF”。

1. VM `onExportPdfClicked()` → `exportMarkdown(PDF)`：

   * 读取 MetaHub：拿 SessionMetadata（若失败当成 null）。
   * 情形判断：

     1. 有 `latestAnalysisMarkdown` 缓存：

        * 直接 `performExport(PDF, markdownOverride = cached)`；
     2. 缓存空但 MetaHub.latestMajorAnalysisMessageId != null：

        * Snackbar：“检测到历史分析记录，如需导出，请重新运行一次智能分析。”
        * 不重跑 SMART_ANALYSIS，不导出；
     3. 缓存空，MetaHub 也无记录：

        * 从聊天里找长内容；
        * 有的话：自动构造 SMART_ANALYSIS 请求，标记 pendingExportAfterAnalysis；
        * 分析完成 → 写 MetaHub.latestMajorAnalysis*（source=SMART_ANALYSIS_AUTO），然后导出；
        * 没有长内容：提示“内容太少，暂无可导出的分析”。

2. `ExportOrchestrator.exportPdf(sessionId, markdown)`：

   * 读取 MetaHub（文件名用 mainPerson+summary+date）；
   * 用传入的 markdown 渲染 PDF；
   * 写 ExportMetadata；
   * 把 `ExportResult` 回给 VM，再由 share handler 分享。

### 5.3 场景 C：第二次导出同一会话

1. 因为之前分析已经完成，`latestAnalysisMarkdown` 已存在：

   * 直接走缓存导出；
   * 不再触发任何 LLM；
   * 单测中验证：两次导出只触发一次 LLM 调用。

### 5.4 场景 D：Home 上传录音 → Tingwu 转写 → 说话人命名

1. Home 中用户「上传音频并转写」：

   * 使用当前 `sessionId` 构造 `TingwuRequest`；
   * `RealTingwuCoordinator.submitTranscription(jobId, sessionId, ...)` 记录 job ↔ session 绑定。

2. Tingwu 任务完成后：

   * Coordinator 拉取转写结果 + diarizedSegments；
   * 调用 `TranscriptOrchestrator.inferTranscriptMetadata`；
   * 得到 speakerMap / mainPerson / stage / risk 等元数据，写入 MetaHub。

3. Coordinator 合并 Tingwu 原始 speaker label 与 LLM 推断结果：

   * confidence ≥ threshold（比如 0.6）时覆盖；
   * 构造 Markdown 显示“顾客/销售顾问/试驾教官”等。

4. Home 通过 MetaHub 读 SessionMetadata / TranscriptMetadata：

   * 标题可以显示“某某客户试驾通话”等；
   * `isTranscription` 标志可来自 TranscriptMetadata 或 SessionMetadata 的显式字段，而非标题前缀。

5. 后续导出 PDF/CSV 时：

   * 直接用 SessionMetadata（带 mainPerson、crmRows），不关心 Tingwu 细节。

---

## 6. Invariants（不变式 / Guardrails）

V3 要求以下不变式**长期保持**：

1. **LLM 调用只发生在 Orchestrator 中**

   * `HomeOrchestratorImpl`、`RealTranscriptOrchestrator` 之外的类不能依赖 `AiChatService`/LLM 客户端。

2. **JSON 解析只发生在 Orchestrator 中**

   * VM / Coordinator / Export 层禁止自己写 JSON 解析逻辑，从 LLM 文本抽 JSON；
   * 一切 LLM 输出 JSON 的解析和降级逻辑，都集中在 Orchestrator 内部。

3. **MetaHub 是唯一真相**

   * 会话标题、主客户、阶段/风险、分析是否已做等状态，都以 MetaHub 为准；
   * VM 内部的字段（如 `userName`、`latestAnalysisMarkdown`）只能是 cache，不是源头。

4. **mergeWith 不破坏旧值**

   * 所有元数据写入 MetaHub 时都必须走 `mergeWith`；
   * 不允许“先读旧值再构造完整对象覆盖”的破坏性写法。

5. **Fail-soft，不泄露 JSON**

   * JSON 解析失败时，Orchestrator 应该：

     * 记录日志（仅内部）；
     * 返回 null 或不写 MetaHub；
     * 绝不把 JSON 内容原样显示给用户。

6. **会话 ID 一致性**

   * Home 上传录音必须复用当前 sessionId；
   * Audio Sync 入口必须明确新建 sessionId，并全程传递到 Home 和 Tingwu；
   * 同一个 jobId 的转写结果只写入一个 sessionId，不漂移。

7. **导出路径 LLM-free**

   * `ExportOrchestrator` 及其调用链中禁止出现 LLM 调用；
   * 导出逻辑仅依赖当前实存的 markdown / crmRows / SessionMetadata。

---

## 7. 测试与契约

V3 建议保持/新增以下测试门类：

1. **接口契约测试**

   * `ExportOrchestrator` 只暴露 `exportPdf` / `exportCsv`；
   * `RealExportOrchestrator` / `RealTingwuCoordinator` 构造函数参数中不包含 LLM 类型；
   * Orchestrator 的 public API 与文档一致。

2. **Merge 语义测试**

   * `SessionMetadataMergeTest`：

     * null 不覆盖；
     * tags 合并去重；
     * crmRows 去重；
     * latestMajorAnalysis* 新值优先。
   * `TranscriptMetadataMergeTest`：

     * speakerMap 合并；
     * confidence clamp；
     * extra 合并。

3. **HomeOrchestratorImpl 测试**

   * 单 fenced JSON、多 fenced JSON；
   * 缺少可选字段的容错；
   * 不从 Delta/Partial 事件写 MetaHub，只在 Completed 写。

4. **TranscriptOrchestrator 测试**

   * 抽样策略；
   * fenced vs brace-depth JSON；
   * invalid JSON fail-soft；
   * cache + force 语义。

5. **导出宏测试（HomeExportActionsTest）**

   * 有缓存分析 → 一次导出 → 不触发 SMART_ANALYSIS；
   * 无缓存但 MetaHub 有 latestMajorAnalysis* → 提示而不重跑；
   * 无任何分析，但内容够长 → 自动分析一次并导出；
   * 二次导出 → 不重复调用 LLM；
   * 用户分析 vs 自动分析 → latestMajorAnalysisSource 分别为 USER/AUTO。

6. **Tingwu 测试**

   * jobId ↔ sessionId 绑定；
   * speaker label 合并策略；
   * TranscriptOrchestrator 失败时 fallback 行为。
