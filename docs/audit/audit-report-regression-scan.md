# Orchestrator/MetaHub 回归扫描报告

**审计日期**: 2025-12-28  
**审计范围**: 检查疑似接口回滚是否导致 orchestrator/MetaHub 行为回归  
**审计类型**: 代码审查（无代码修改）

---

## 1. MetaHub 数据模型与合并语义

### 1.1 SessionMetadata v2 字段完整性

**Status**: ✅ **OK**

**Evidence**:
- `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt:11-24`
  - 包含所有 v2 字段：`mainPerson`, `shortSummary`, `summaryTitle6Chars`, `location`, `stage`, `riskLevel`, `crmRows`, `latestMajorAnalysis*`, `tags`, `lastUpdatedAt`, `analysisSource`
  - 无重复 v2 类型

### 1.2 TranscriptMetadata v2 字段完整性

**Status**: ✅ **OK**

**Evidence**:
- `core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt:11-24`
  - 包含所有 v2 字段：`speakerMap`, `sessionId`, `diarizedSegmentsCount`, `mainPerson`, `shortSummary`, `summaryTitle6Chars`, `location`, `stage`, `riskLevel`, `extra`
  - `source` 字段为 `TranscriptSource` 枚举（包含 `TINGWU_LLM`）

### 1.3 InMemoryMetaHub 非空合并语义

**Status**: ✅ **OK**

**Evidence**:
- `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt:29-43`
  - `mergeWith()` 使用非空合并：`other.mainPerson ?: mainPerson`
  - Tags 合并：`(tags + other.tags).filter { it.isNotBlank() }.toSet()`
  - CRM rows 合并去重逻辑正确
- `core/util/src/main/java/com/smartsales/core/metahub/InMemoryMetaHub.kt:29-34`
  - `upsertSession()` 调用 `existing?.mergeWith(metadata) ?: metadata`
  - 非空合并语义保持完整

### 1.4 MetaHub 接口签名

**Status**: ✅ **OK**

**Evidence**:
- `core/util/src/main/java/com/smartsales/core/metahub/MetaHub.kt:11-47`
  - 接口签名未改变，仍支持所有 v2 元数据类型
  - 无字段被移除或废弃

**分类**: **OK** - 字段和合并语义完全匹配 v2 规范

---

## 2. HomeOrchestratorImpl & HomeScreenViewModel

### 2.1 HomeOrchestratorImpl JSON 提取与解析

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:70-94`
  - `extractJsonBlock()` 支持 fenced blocks 和 inline JSON
  - 不修改 assistant text（仅提取）
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:96-137`
  - `parseSessionMetadata()` 解析 JSON 到 `SessionMetadata` 字段 + `crmRows` + `latestMajorAnalysis*` + `analysisSource`
  - 正确设置 `latestMajorAnalysisAt` 和 `latestMajorAnalysisSource`

### 2.2 HomeOrchestratorImpl 流式路径

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:32-40`
  - 仅在 `Completed` 事件时解析元数据
  - 原始 `Completed.fullText` 直接 emit 给 VM（`emit(event)`）
  - 不修改流式事件内容

### 2.3 HomeScreenViewModel 使用 HomeOrchestrator

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:1046`
  - 使用 `homeOrchestrator.streamChat(request)` 进行 LLM 调用
  - 无直接 LLM 调用

### 2.4 HomeScreenViewModel 使用 MetaHub 和 SessionTitleResolver

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:2426-2456`
  - `maybeGenerateSessionTitle()` 优先使用 MetaHub 的 `mainPerson` 和 `summaryTitle6Chars`
  - 回退到 `sessionTitleResolver.resolveTitle()` 当 metadata 缺失时
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:2459-2487`
  - `updateTitleFromMetadata()` 从 MetaHub 读取并更新标题

### 2.5 Smart Analysis Gating & latestAnalysisMarkdown

**Status**: ✅ **OK**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:219`
  - `latestAnalysisMarkdown: String?` 本地缓存存在
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:1405-1418`
  - `onAnalysisCompleted()` 存储 `latestAnalysisMarkdown` 和 `latestAnalysisMessageId`
  - 与导出流程正确集成

**分类**: **OK** - 行为完全匹配 Overhaul 规范，仅 API 命名可能有细微变化

---

## 3. ExportOrchestrator & 导出流程

### 3.1 ExportOrchestrator LLM-free

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:34-39`
  - 构造函数仅注入 `MetaHub`, `ExportManager`, `ExportFileStore`, `DispatcherProvider`
  - 无 `AiChatService` 或其他 LLM 客户端依赖
- `grep` 搜索 `AiChatService|DashScope` 在 `ExportOrchestrator.kt` 中无匹配

### 3.2 ExportOrchestrator 文件名生成

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:113-118`
  - `buildBaseName()` 从 MetaHub `SessionMetadata` 获取 `mainPerson`、`summaryTitle6Chars`、`lastUpdatedAt`
  - 格式：`yyyyMMdd_HHmm_<mainPerson>_<summaryTitle>`
  - 缺失时回退到 "未知客户" 和 "销售咨询"

### 3.3 CSV 生成

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:61-94`
  - 从 `SessionMetadata.crmRows` 构建 CSV
  - 空列表时返回仅 header
  - 始终写入 `ExportMetadata`

### 3.4 HomeScreenViewModel 导出路径

**Status**: 🔴 **DeepRegression**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:389-419`
  - `performExport(format: ExportFormat)` 方法调用 `exportOrchestrator.exportMarkdown(sessionId, markdown, format)` (line 397)
- `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt:27-31`
  - 接口仅定义 `exportPdf(sessionId: String, markdown: String)` 和 `exportCsv(sessionId: String)`
  - **不存在 `exportMarkdown` 方法**

**问题**: HomeScreenViewModel 调用不存在的接口方法，会导致编译错误或运行时错误。

**风险**: 🔴 **High** - 阻塞导出功能

**注意**: 虽然存在另一个 `performExport(format: ExportFormat, markdownOverride: String? = null)` 方法（line 407）正确使用 `exportPdf` 和 `exportCsv`，但 `exportMarkdown()` 调用的是旧方法（line 389），导致实际执行路径错误。

### 3.5 导出宏使用 MetaHub 元数据

**Status**: ⚠️ **Partial**

**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:354-387`
  - `exportMarkdown()` 检查 `latestAnalysisMarkdown`（VM 本地缓存）
  - 如果为空，自动触发 SMART_ANALYSIS
  - **未从 MetaHub 读取 `latestMajorAnalysis*` 字段来判断是否已有分析**

**问题**: 仅使用 VM 本地缓存，未从 MetaHub 读取已存在的分析结果。当 MetaHub 有分析但缓存缺失时，可能重复触发分析。

**风险**: 🟡 **Medium** - 可能重复触发分析，但不会崩溃

**分类**: **DeepRegression** - 导出路径调用不存在的接口方法，阻塞导出功能

---

## 4. TranscriptOrchestrator & RealTingwuCoordinator

### 4.1 TranscriptOrchestrator LLM 调用

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:46,60`
  - 构造函数注入 `aiChatService: AiChatService`
  - 调用 `aiChatService.sendMessage(AiChatRequest(prompt = prompt))`

### 4.2 TranscriptOrchestrator 采样、JSON 提取、解析

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:87-119`
  - `sampleSegments()` 实现：限制段数（maxSegments=30）、每段字符数（maxCharsPerSegment=200）、覆盖所有说话人
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:162-186`
  - `extractJsonBlock()` 实现：优先 fenced blocks（```json），fallback 到 brace-depth scan
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:188-250`
  - `parseMetadata()` 使用 `JSONObject.opt*` 容忍解析，失败返回 null

### 4.3 TranscriptOrchestrator 置信度限制与合并

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:261-264`
  - confidence 使用 `value.toFloat().coerceIn(0f, 1f)` 限制到 [0,1]
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:216`
  - `TranscriptMetadata` 创建时 `source = TranscriptSource.TINGWU_LLM`
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:227-248`
  - 如果 `sessionId` 存在，构建 `SessionMetadata` delta 并通过 `mergeWith` 合并
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:276-287`
  - `persistMetadata()` 使用 `mergeWith` 进行非空合并

### 4.4 TranscriptOrchestrator force + cache 语义

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt:72-85`
  - `readCachedMetadata()` 检查：`force == false` 且存在非默认 speakerMap 时返回缓存
  - 非默认判断：`displayName` 非空且不以"发言人"/"speaker"开头
  - `if (request.force || !hasReadableSpeaker) return null` - force=true 时跳过缓存

### 4.5 RealTingwuCoordinator 集成

**Status**: ✅ **OK**

**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:1016-1045`
  - `refineSpeakerLabels()` 调用 `transcriptOrchestrator.inferTranscriptMetadata()`
  - 合并逻辑：`confidence >= 0.6f || !merged.containsKey(speakerId)`
- `grep` 搜索 `AiChatService` 在 `RealTingwuCoordinator.kt` 中无匹配
  - RealTingwuCoordinator 不直接调用 LLM

**分类**: **OK** - 行为完全符合 T-orch-07/08/09 规范

---

## 总结

### 状态概览

- ✅ **完全符合**: 3/4 项
- ⚠️ **部分符合**: 1/4 项
- 🔴 **深度回归**: 1/4 项

### 整体状态: 🔴 **DeepRegression**

存在 1 个高优先级阻塞问题：HomeScreenViewModel 的 `performExport(format: ExportFormat)` 方法调用不存在的 `exportOrchestrator.exportMarkdown()` 方法。

---

## 推荐修复（按优先级）

### 🔴 High Priority (阻塞发布)

1. **修复 HomeScreenViewModel 的 performExport 方法**
   - 文件: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:389-419`
   - 问题: 调用不存在的 `exportOrchestrator.exportMarkdown()` 方法
   - 修复: 删除旧方法（line 389-419），或修改为调用 `exportPdf`/`exportCsv`。确保 `exportMarkdown()` 调用时使用正确的方法签名（带 `markdownOverride` 参数的方法）

### 🟡 Medium Priority (功能改进)

2. **Home export 使用 MetaHub 的 latestMajorAnalysis**
   - 文件: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:354-387`
   - 问题: 仅使用 VM 本地缓存 `latestAnalysisMarkdown`，未从 MetaHub 读取
   - 修复: 在 `exportMarkdown()` 中先检查 MetaHub 的 `latestMajorAnalysis*` 字段，如果存在且 `latestAnalysisMarkdown` 为空，尝试从 messageId 恢复或提示用户

---

**注意**: 本报告仅关注 orchestrator/MetaHub 行为是否因接口回滚而回归，不涉及新功能建议或重构建议。除导出路径的接口调用错误外，其他 orchestrator/MetaHub 行为均保持完整。

