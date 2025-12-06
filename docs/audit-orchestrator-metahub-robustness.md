# T-orch Orchestrator+MetaHub 守护审计报告

**日期**: 2025-12-XX  
**范围**: Home 导出链路修正后的全面自查  
**审计类型**: 只读分析，无代码修改  
**目标**: 确认导出链路、LLM 边界、Orchestrator/MetaHub 合约与 merge 语义、Transcript/Tingwu 路径符合 v2 规范

---

## 概览

**测试结果**:
- ✅ `ExportOrchestratorContractTest`: 通过（接口仅暴露 `exportPdf`/`exportCsv`，无 LLM 依赖）
- ✅ `HomeExportActionsTest`: 通过（导出链路正确，MetaHub 检查逻辑存在）

**总体状态**: 🟡 **Yellow**（存在设计缺陷，但不影响编译与基本功能）

**关键发现**:
1. ✅ 导出链路已统一为 `exportPdf`/`exportCsv`，无 `exportMarkdown` 幽灵接口
2. ✅ ExportOrchestrator 保持 LLM-free，边界清晰
3. ⚠️ `latestMajorAnalysisMessageId` 写入缺失：HomeOrchestratorImpl 写入时设为 `null`，HomeScreenViewModel 有 messageId 但未持久化
4. ✅ MetaHub merge 语义正确，无回归
5. ✅ Transcript/Tingwu 路径符合 v2 规范

---

## 1. ExportOrchestrator 接口 & LLM 边界

### 1.1 接口合约

**Status**: ✅ **OK**

**Evidence**:
- `ExportOrchestrator.kt:27-31`: 接口仅暴露 `exportPdf(sessionId, markdown)` 和 `exportCsv(sessionId)`
- `ExportOrchestratorContractTest.kt:14-24`: 显式断言接口仅包含 `exportPdf`/`exportCsv`，且不存在 `exportMarkdown`

```27:31:data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
interface ExportOrchestrator {
    suspend fun exportPdf(sessionId: String, markdown: String): Result<ExportResult>

    suspend fun exportCsv(sessionId: String): Result<ExportResult>
}
```

```14:24:data/ai-core/src/test/java/com/smartsales/data/aicore/ExportOrchestratorContractTest.kt
    @Test
    fun `export orchestrator exposes only pdf and csv`() {
        val declared = ExportOrchestrator::class.java.declaredMethods
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()
        val expected = setOf("exportPdf", "exportCsv")
        assertTrue(declared.containsAll(expected))
        assertTrue(declared.all { it in expected })
        assertFalse(declared.any { it.contains("exportMarkdown", ignoreCase = true) })
    }
```

**结论**: ✅ 接口合约正确，无 `exportMarkdown` 残留

### 1.2 依赖边界

**Status**: ✅ **OK**

**Evidence**:
- `RealExportOrchestrator` 构造函数（`ExportOrchestrator.kt:34-39`）: 仅依赖 `MetaHub`、`ExportManager`、`ExportFileStore`、`DispatcherProvider`，无 LLM 类型
- `RealTingwuCoordinator` 构造函数（`RealTingwuCoordinator.kt:62-70`）: 无 `AiChatService` 依赖，仅依赖 `TranscriptOrchestrator`
- `ExportOrchestratorContractTest.kt:27-38`: 显式断言无 LLM 依赖

```34:39:data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
@Singleton
class RealExportOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val exportManager: ExportManager,
    private val exportFileStore: ExportFileStore,
    private val dispatchers: DispatcherProvider
) : ExportOrchestrator {
```

**结论**: ✅ LLM 边界清晰，ExportOrchestrator 与 TingwuCoordinator 均无直接 LLM 依赖

---

## 2. Home 导出链路（HomeScreenViewModel）

### 2.1 导出入口与分支

**Status**: ✅ **OK**

**Evidence**:
- `onExportPdfClicked()` (`HomeScreenViewModel.kt:312-314`): 调用 `exportMarkdown(ExportFormat.PDF)`
- `onExportCsvClicked()` (`HomeScreenViewModel.kt:316-318`): 调用 `exportMarkdown(ExportFormat.CSV)`
- `exportMarkdown()` (`HomeScreenViewModel.kt:356-412`): 私有方法，内部使用 `when (format)` 分支
- `performExport()` (`HomeScreenViewModel.kt:414-446`): 最终调用 `exportOrchestrator.exportPdf(sessionId, markdown)` 或 `exportOrchestrator.exportCsv(sessionId)`

```312:318:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
    fun onExportPdfClicked() {
        exportMarkdown(ExportFormat.PDF)
    }

    fun onExportCsvClicked() {
        exportMarkdown(ExportFormat.CSV)
    }
```

```421:424:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
        val result = when (format) {
            ExportFormat.PDF -> exportOrchestrator.exportPdf(sessionId, markdown)
            ExportFormat.CSV -> exportOrchestrator.exportCsv(sessionId)
        }
```

**结论**: ✅ 导出链路正确，无 `exportOrchestrator.exportMarkdown(...)` 调用

### 2.2 MetaHub + SMART_ANALYSIS 宏逻辑

**Status**: ✅ **OK**（逻辑正确，但存在设计缺陷见 2.3）

**Evidence**:
- `exportMarkdown()` (`HomeScreenViewModel.kt:356-412`): 
  1. 优先使用 VM 缓存 `latestAnalysisMarkdown`（line 365-368）
  2. 如缓存为空，检查 MetaHub `latestMajorAnalysisMessageId`（line 360-361）
  3. 若 MetaHub 有但 VM 无，显示提示并 return（line 369-379）
  4. 若 MetaHub 也无，自动触发 SMART_ANALYSIS（line 381-409）

```356:412:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
    private fun exportMarkdown(format: ExportFormat) {
        if (_uiState.value.exportInProgress) return
        viewModelScope.launch {
            // 检查 MetaHub 分析状态
            val meta = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            val hasMetaAnalysis = meta?.latestMajorAnalysisMessageId != null
            val cachedAnalysis = latestAnalysisMarkdown

            when {
                !cachedAnalysis.isNullOrBlank() -> {
                    // 直接导出：使用缓存分析 markdown
                    performExport(format, markdownOverride = cachedAnalysis)
                }
                hasMetaAnalysis -> {
                    // MetaHub 认为有分析，但 VM 没有缓存文本
                    // 不自动重跑，给轻量提示 + 退回
                    _uiState.update {
                        it.copy(
                            exportInProgress = false,
                            snackbarMessage = "检测到历史分析记录，如需导出，请重新运行一次智能分析。"
                        )
                    }
                    // 可选：退回到对话导出（逐字稿），或直接 return
                    return@launch
                }
                else -> {
                    // 没有任何分析记录，走现有 "自动 SMART_ANALYSIS 然后导出" 路径
                    val (mainContent, context) = findLatestLongContent()
                    if (mainContent == null) {
                        performExport(format, markdownOverride = null)
                        return@launch
                    }
                    pendingExportAfterAnalysis = format
                    _uiState.update { it.copy(exportInProgress = true, chatErrorMessage = null) }
                    val autoGoal = "导出前自动分析"
                    val userMessage = buildSmartAnalysisUserMessage(
                        mainContent = mainContent,
                        context = context,
                        goal = autoGoal
                    )
                    sendMessageInternal(
                        messageText = userMessage,
                        skillOverride = QuickSkillId.SMART_ANALYSIS,
                        userDisplayText = "智能分析（导出前自动生成）",
                        onCompleted = {},
                        onCompletedTransform = { body ->
                            buildString {
                                append("智能分析结果\n\n")
                                append(body.trim())
                            }.trim()
                        },
                        isAutoAnalysis = true
                    )
                }
            }
        }
    }
```

- `exportInProgress` / `pendingExportAfterAnalysis` 标志（line 179, 223）: 防止重复触发

**结论**: ✅ 宏逻辑符合规范，优先缓存 → MetaHub 检查 → 自动分析

### 2.3 latestMajorAnalysisMessageId 写入缺失

**Status**: ⚠️ **Design Gap**（不影响编译，但影响持久化）

**Evidence**:
- `HomeOrchestratorImpl.kt:132`: 写入 MetaHub 时 `latestMajorAnalysisMessageId = null`
- `HomeScreenViewModel.kt:1430-1444`: `onAnalysisCompleted()` 接收 `messageId` 但仅存储在本地变量，未写入 MetaHub

```122:136:feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
        return SessionMetadata(
            sessionId = sessionId,
            mainPerson = mainPerson,
            shortSummary = shortSummary,
            summaryTitle6Chars = summaryTitle,
            location = location,
            stage = stage,
            riskLevel = risk,
            tags = tags,
            lastUpdatedAt = System.currentTimeMillis(),
            latestMajorAnalysisMessageId = null,
            latestMajorAnalysisAt = System.currentTimeMillis(),
            latestMajorAnalysisSource = source,
            crmRows = crmRows
        )
```

```1430:1444:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
    private fun onAnalysisCompleted(summary: String, messageId: String) {
        latestAnalysisMarkdown = summary
        latestAnalysisMessageId = messageId
        if (!hasShownAnalysisExportHint) {
            hasShownAnalysisExportHint = true
            appendAssistantMessage(
                content = "智能分析完成，如需分享可直接导出 PDF 或 CSV。"
            )
        }
        pendingExportAfterAnalysis?.let { format ->
            pendingExportAfterAnalysis = null
            viewModelScope.launch {
                performExport(format, markdownOverride = summary)
            }
        }
    }
```

**问题**: 
- HomeOrchestratorImpl 无法获取 messageId（仅接收 `ChatRequest` 和 `fullText`）
- HomeScreenViewModel 有 messageId 但未写入 MetaHub
- 导致 VM 重启后无法从 MetaHub 恢复分析状态

**推荐修复**:
1. 方案 A: HomeScreenViewModel 在 `onAnalysisCompleted()` 中显式写入 MetaHub，更新 `latestMajorAnalysisMessageId`
2. 方案 B: 扩展 `ChatRequest` 或 `ChatStreamEvent.Completed` 携带 messageId，让 HomeOrchestratorImpl 写入

**结论**: ⚠️ 设计缺陷，需修复以支持持久化

### 2.4 测试对齐

**Status**: ✅ **OK**

**Evidence**:
- `HomeExportActionsTest.kt:182-193`: 测试“有缓存分析 → 导出使用分析”
- `HomeExportActionsTest.kt:209-223`: 测试“第二次导出复用缓存，不重跑分析”
- `HomeExportActionsTest.kt:226-239`: 测试“MetaHub 有最新分析但 VM 缓存为空 → 提示不重跑”

```226:239:feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt
    @Test
    fun `when metahub has analysis but vm cache empty it hints and skips auto run`() = runTest(dispatcher) {
        metaHub.session = SessionMetadata(
            sessionId = "home-session",
            latestMajorAnalysisMessageId = "m1",
            lastUpdatedAt = 1L
        )

        viewModel.onExportPdfClicked()
        advanceUntilIdle()

        assertEquals(0, aiChatService.callCount)
        assertEquals(null, exportOrchestrator.lastFormat)
        assertTrue(viewModel.uiState.value.snackbarMessage?.contains("历史分析记录") == true)
    }
```

**结论**: ✅ 测试覆盖关键场景，断言与实现一致

---

## 3. Orchestrator ↔ MetaHub 合约与 LLM 边界

### 3.1 允许调用 LLM 的类

**Status**: ✅ **OK**

**Evidence**:
- `HomeOrchestratorImpl.kt:28-31`: 构造函数注入 `AiChatService`
- `RealTranscriptOrchestrator.kt:43-46`: 构造函数注入 `AiChatService`

```28:31:feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
@Singleton
class HomeOrchestratorImpl @Inject constructor(
    private val aiChatService: AiChatService,
    private val metaHub: MetaHub
) : HomeOrchestrator {
```

```43:46:data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt
@Singleton
class RealTranscriptOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val dispatchers: DispatcherProvider,
    private val aiChatService: AiChatService
) : TranscriptOrchestrator {
```

**结论**: ✅ 仅允许的类依赖 LLM，边界清晰

### 3.2 禁止直接调用 LLM 的类

**Status**: ✅ **OK**

**Evidence**:
- `RealExportOrchestrator`: 无 `AiChatService` 依赖（见 1.2）
- `RealTingwuCoordinator`: 无 `AiChatService` 依赖（见 1.2）
- `HomeScreenViewModel`: 无 `AiChatService` 依赖，仅依赖 `HomeOrchestrator`

```193:209:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val homeOrchestrator: HomeOrchestrator,
    private val aiSessionRepository: AiSessionRepository,
    private val deviceConnectionManager: DeviceConnectionManager,
    private val mediaSyncCoordinator: MediaSyncCoordinator,
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val audioStorageRepository: AudioStorageRepository,
    private val quickSkillCatalog: QuickSkillCatalog,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val sessionRepository: SessionRepository,
    private val sessionTitleResolver: SessionTitleResolver,
    private val userProfileRepository: UserProfileRepository,
    private val exportOrchestrator: ExportOrchestrator,
    private val shareHandler: ChatShareHandler,
    private val metaHub: MetaHub
) : ViewModel() {
```

**结论**: ✅ 禁止类均无 LLM 依赖，边界未破

### 3.3 MetaHub merge 不变量

**Status**: ✅ **OK**

**Evidence**:
- `SessionMetadata.mergeWith()` (`SessionMetadata.kt:29-43`):
  - 非空字段覆盖旧值，null 不覆盖（line 31-36）
  - tags 合并去重（line 37）
  - crmRows 合并去重（line 42, 45-50）
  - `lastUpdatedAt` 使用 `maxOf`（line 38）
  - `latestMajorAnalysisMessageId` 使用 `?:` 保留旧值（line 39）

```29:43:core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt
    fun mergeWith(other: SessionMetadata): SessionMetadata = SessionMetadata(
        sessionId = sessionId,
        mainPerson = other.mainPerson ?: mainPerson,
        shortSummary = other.shortSummary ?: shortSummary,
        summaryTitle6Chars = other.summaryTitle6Chars ?: summaryTitle6Chars,
        location = other.location ?: location,
        stage = other.stage ?: stage,
        riskLevel = other.riskLevel ?: riskLevel,
        tags = (tags + other.tags).filter { it.isNotBlank() }.toSet(),
        lastUpdatedAt = maxOf(lastUpdatedAt, other.lastUpdatedAt),
        latestMajorAnalysisMessageId = other.latestMajorAnalysisMessageId ?: latestMajorAnalysisMessageId,
        latestMajorAnalysisAt = other.latestMajorAnalysisAt ?: latestMajorAnalysisAt,
        latestMajorAnalysisSource = other.latestMajorAnalysisSource ?: latestMajorAnalysisSource,
        crmRows = mergeCrmRows(crmRows, other.crmRows)
    )
```

- `TranscriptMetadata.mergeWith()` (`TranscriptMetadata.kt:29-43`):
  - speakerMap 合并策略：新值覆盖，confidence 夹紧 [0,1]（line 32, 45-65）
  - extra 合并使用 `extra + other.extra`（line 42）
  - `createdAt` 使用 `maxOf`（line 34）

```29:43:core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt
    fun mergeWith(other: TranscriptMetadata): TranscriptMetadata = TranscriptMetadata(
        transcriptId = transcriptId,
        sessionId = other.sessionId ?: sessionId,
        speakerMap = mergeSpeakers(speakerMap, other.speakerMap),
        source = other.source.takeIf { it != TranscriptSource.UNKNOWN } ?: source,
        createdAt = maxOf(createdAt, other.createdAt),
        diarizedSegmentsCount = other.diarizedSegmentsCount ?: diarizedSegmentsCount,
        mainPerson = other.mainPerson ?: mainPerson,
        shortSummary = other.shortSummary ?: shortSummary,
        summaryTitle6Chars = other.summaryTitle6Chars ?: summaryTitle6Chars,
        location = other.location ?: location,
        stage = other.stage ?: stage,
        riskLevel = other.riskLevel ?: riskLevel,
        extra = extra + other.extra
    )
```

**结论**: ✅ merge 语义正确，无回归

---

## 4. JSON 解析鲁棒性（Home + Transcript）

### 4.1 TranscriptOrchestrator JSON

**Status**: ✅ **OK**

**Evidence**:
- `TranscriptOrchestrator.kt:162-186`: `extractJsonBlock()` 优先解析 ```json fenced block，回退 brace-depth 扫描
- `TranscriptOrchestrator.kt:188-196`: `parseMetadata()` 使用 `runCatching { JSONObject(jsonText) }`，失败返回 null
- 写入前检查 `parsed == null`，fail-soft 不抛异常

```162:186:data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt
    private fun extractJsonBlock(text: String): String? {
        val fenced = Regex("```json\\s*(\\{[\\s\\S]*?})\\s*```", RegexOption.IGNORE_CASE)
        fenced.find(text)?.let { match ->
            return match.groupValues.getOrNull(1)?.trim()
        }
        val anyFence = Regex("```\\s*(\\{[\\s\\S]*?})\\s*```")
        anyFence.find(text)?.let { match ->
            return match.groupValues.getOrNull(1)?.trim()
        }
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(start, min(i + 1, text.length)).trim()
                    }
                }
            }
        }
        return null
    }
```

**结论**: ✅ JSON 解析鲁棒，fail-soft 策略正确

### 4.2 HomeOrchestratorImpl JSON

**Status**: ✅ **OK**

**Evidence**:
- `HomeOrchestratorImpl.kt:70-94`: `extractJsonBlock()` 优先 fenced block，回退 brace-depth
- `HomeOrchestratorImpl.kt:96-137`: `parseSessionMetadata()` 使用 `optString()`/`optJSONArray()`，缺少可选字段仍能解析
- 多个 fenced block 时使用第一个（`find()` 而非 `findAll()`）

```70:94:feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
    private fun extractJsonBlock(text: String): String? {
        val fencedRegex = Regex("```json\\s*(\\{[\\s\\S]*?})\\s*```", RegexOption.IGNORE_CASE)
        fencedRegex.find(text)?.let { match ->
            return match.groupValues.getOrNull(1)?.trim()
        }
        val anyFence = Regex("```\\s*(\\{[\\s\\S]*?})\\s*```")
        anyFence.find(text)?.let { match ->
            return match.groupValues.getOrNull(1)?.trim()
        }
        val start = text.indexOf('{')
        if (start == -1) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return text.substring(start, i + 1).trim()
                    }
                }
            }
        }
        return null
    }
```

**结论**: ✅ JSON 解析鲁棒，可选字段处理正确

---

## 5. Tingwu / Transcript / Session 绑定

**Status**: ✅ **OK**

**Evidence**:
- `RealTingwuCoordinator.kt:346-351`: `upsertTranscriptMetadata()` 使用 `request?.sessionId`，正确绑定
- `RealTingwuCoordinator.kt:1228-1236`: 写入 `TranscriptMetadata` 时 `source = TranscriptSource.TINGWU`
- `TranscriptOrchestrator.kt:216`: 写入时 `source = TranscriptSource.TINGWU_LLM`
- `TranscriptOrchestrator.kt:276-287`: `persistMetadata()` 合并 SessionMetadata 遵守 merge 语义

```346:351:data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
                            upsertTranscriptMetadata(
                                jobId = taskId,
                                sessionId = request?.sessionId,
                                speakerLabels = labelsForMeta,
                                diarizedSegments = diarizedForMeta
                            )
```

```1228:1236:data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
        val meta = TranscriptMetadata(
            transcriptId = jobId,
            sessionId = sessionId, // TODO: 后续与会话 ID 关联
            speakerMap = speakerMap,
            source = TranscriptSource.TINGWU,
            createdAt = System.currentTimeMillis(),
            diarizedSegmentsCount = diarizedSegments.size
        )
```

**结论**: ✅ Transcript/Tingwu 路径符合 v2 规范，sessionId 绑定正确

---

## 总体结论与下一步建议

### 总体状态: 🟡 **Yellow**

**编译与功能**: ✅ 无阻塞问题，导出链路工作正常

**设计缺陷**: ⚠️ `latestMajorAnalysisMessageId` 未持久化，影响 VM 重启后状态恢复

### 关键发现总结

1. ✅ **导出链路**: 已统一为 `exportPdf`/`exportCsv`，无幽灵接口
2. ✅ **LLM 边界**: ExportOrchestrator 保持 LLM-free，边界清晰
3. ⚠️ **持久化缺陷**: `latestMajorAnalysisMessageId` 未写入 MetaHub，需修复
4. ✅ **Merge 语义**: SessionMetadata/TranscriptMetadata merge 正确，无回归
5. ✅ **JSON 解析**: 鲁棒性良好，fail-soft 策略正确
6. ✅ **Tingwu 路径**: 符合 v2 规范，sessionId 绑定正确

### 推荐修复优先级

**High**:
- 修复 `latestMajorAnalysisMessageId` 持久化：在 `HomeScreenViewModel.onAnalysisCompleted()` 中显式写入 MetaHub，或扩展 `ChatStreamEvent.Completed` 携带 messageId

**Low**:
- 考虑在 `HomeOrchestratorImpl.parseSessionMetadata()` 中接收 messageId 参数（需扩展 `ChatRequest` 或事件流）

### 测试建议

- ✅ 现有单测覆盖关键场景，无需新增
- 建议添加集成测试：VM 重启后从 MetaHub 恢复 `latestMajorAnalysisMessageId` 状态

---

**审计完成时间**: 2025-12-XX  
**审计工具**: 只读代码分析 + 单测验证
