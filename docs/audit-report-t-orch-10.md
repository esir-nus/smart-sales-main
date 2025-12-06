# T-orch-10 Guardrails / Orchestrator-MetaHub 自查审计报告

**日期**: 2025-12-06  
**范围**: T-orch-10 完成后的只读审计  
**审计类型**: 只读分析，无代码修改

---

## 1. ExportOrchestrator 合约 & 无 LLM 依赖

### 1.1 接口合约检查

**状态**: ✅ **OK**

- **接口定义** (`ExportOrchestrator.kt:27-31`): 仅暴露 `exportPdf` 和 `exportCsv` 两个方法
- **无 exportMarkdown**: 接口中不存在任何 `exportMarkdown` 方法（public/internal 任意级别）
- **契约测试** (`ExportOrchestratorContractTest.kt:14-24`): 显式检查接口仅包含 `exportPdf` 和 `exportCsv`，并断言不存在 `exportMarkdown`

**证据**:
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

### 1.2 依赖约束

**状态**: ✅ **OK**

- **RealExportOrchestrator 构造函数** (`ExportOrchestrator.kt:34-39`): 仅依赖 `MetaHub` + `ExportManager` + `ExportFileStore` + `DispatcherProvider`，无 LLM 类型
- **边界测试** (`ExportOrchestratorContractTest.kt:27-31`): 显式断言 `RealExportOrchestrator` 构造函数参数不包含 `AiChatService` 或 `DashScope`

**证据**:
```34:39:data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
@Singleton
class RealExportOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val exportManager: ExportManager,
    private val exportFileStore: ExportFileStore,
    private val dispatchers: DispatcherProvider
) : ExportOrchestrator {
```

```27:31:data/ai-core/src/test/java/com/smartsales/data/aicore/ExportOrchestratorContractTest.kt
    @Test
    fun `real export orchestrator has no ai chat dependency`() {
        val paramTypes = RealExportOrchestrator::class.java.constructors
            .flatMap { it.parameterTypes.toList() }
        assertFalse(paramTypes.any { it.name.contains("AiChatService") || it.name.contains("Dashscope", ignoreCase = true) })
    }
```

---

## 2. LLM 调用边界（谁可以、谁不可以）

### 2.1 允许 LLM 的类

**状态**: ✅ **OK**

- **RealTranscriptOrchestrator** (`TranscriptOrchestrator.kt:43-46`): 构造函数注入 `AiChatService`
- **HomeOrchestratorImpl** (`HomeOrchestratorImpl.kt:28-31`): 构造函数注入 `AiChatService`
- **RealExportOrchestrator**: 无 `AiChatService` 依赖 ✅
- **RealTingwuCoordinator** (`RealTingwuCoordinator.kt:62-70`): 无 `AiChatService` 依赖，仅依赖 `TranscriptOrchestrator` ✅

**证据**:
```43:46:data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt
@Singleton
class RealTranscriptOrchestrator @Inject constructor(
    private val metaHub: MetaHub,
    private val dispatchers: DispatcherProvider,
    private val aiChatService: AiChatService
) : TranscriptOrchestrator {
```

```28:31:feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
@Singleton
class HomeOrchestratorImpl @Inject constructor(
    private val aiChatService: AiChatService,
    private val metaHub: MetaHub
) : HomeOrchestrator {
```

### 2.2 边界测试

**状态**: ✅ **OK**

- **ExportOrchestratorContractTest** (`ExportOrchestratorContractTest.kt:33-38`): 显式断言 `RealTingwuCoordinator` 构造函数不包含 `AiChatService` 或 `DashScope`

**证据**:
```33:38:data/ai-core/src/test/java/com/smartsales/data/aicore/ExportOrchestratorContractTest.kt
    @Test
    fun `real tingwu coordinator has no ai chat dependency`() {
        val paramTypes = RealTingwuCoordinator::class.java.constructors
            .flatMap { it.parameterTypes.toList() }
        assertFalse(paramTypes.any { it.name.contains("AiChatService") || it.name.contains("Dashscope", ignoreCase = true) })
    }
```

---

## 3. MetaHub merge 不变量

### 3.1 SessionMetadata.mergeWith

**状态**: ✅ **OK**

- **非空覆盖** (`SessionMetadata.kt:29-43`): 非空字段覆盖旧值，null 保持旧值不变
- **tags 合并** (`SessionMetadata.kt:37`): 使用 set 方式合并，去重并过滤空白
- **crmRows 去重** (`SessionMetadata.kt:45-50`): 通过 `distinctBy { it.client.trim() + "|" + it.owner.trim() }` 去重
- **lastUpdatedAt 单调** (`SessionMetadata.kt:38`): 使用 `maxOf(lastUpdatedAt, other.lastUpdatedAt)` 保证单调

**测试覆盖** (`SessionMetadataMergeTest.kt`):
- `mergeWith_preservesExistingWhenNewIsNull`: 验证 null 不覆盖旧值
- `mergeWith_overridesNonNullAndMergesCollections`: 验证非空覆盖、tags 合并、crmRows 去重

**证据**:
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

```45:50:core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt
    private fun mergeCrmRows(oldRows: List<CrmRow>, newRows: List<CrmRow>): List<CrmRow> {
        if (newRows.isEmpty()) return oldRows
        if (oldRows.isEmpty()) return newRows
        val combined = (oldRows + newRows).filter { it.client.isNotBlank() || it.owner.isNotBlank() }
        return combined.distinctBy { it.client.trim() + "|" + it.owner.trim() }
    }
```

### 3.2 TranscriptMetadata.mergeWith

**状态**: ✅ **OK**

- **speakerMap merge** (`TranscriptMetadata.kt:45-65`): 相同 speakerId 用新值覆盖，其他保留；置信度被夹紧到 [0,1]
- **extra 合并** (`TranscriptMetadata.kt:42`): 使用 `extra + other.extra` 合并
- **createdAt 单调** (`TranscriptMetadata.kt:34`): 使用 `maxOf(createdAt, other.createdAt)` 保证单调

**测试覆盖** (`TranscriptMetadataMergeTest.kt`):
- `mergeWith_preservesExistingSpeakerAndMergesNewOnes`: 验证说话人合并、置信度夹紧、extra 合并

**证据**:
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

---

## 4. JSON 解析鲁棒性（Transcript + Home）

### 4.1 TranscriptOrchestrator JSON 解析

**状态**: ✅ **OK**

- **fenced 优先** (`TranscriptOrchestrator.kt:162-186`): 优先使用 ```json fenced block，fallback 到 brace-depth 方案
- **多 fenced block / 冗余字段处理** (`RealTranscriptOrchestratorTest.kt:204-236`): 测试覆盖 "fenced json with leading text and ignores unknown keys"
- **fail-soft** (`RealTranscriptOrchestratorTest.kt:152-172`): 测试覆盖 "invalid json fails soft without corrupting metadata"，返回 null，不写 MetaHub，不抛异常

**证据**:
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

```204:236:data/ai-core/src/test/java/com/smartsales/data/aicore/RealTranscriptOrchestratorTest.kt
    @Test
    fun `parses fenced json with leading text and ignores unknown keys`() = runTest(dispatcher) {
        val metaHub = InMemoryMetaHub()
        val ai = RecordingAiChatService(
            """
            文本开头
            ```json
            {
              "speaker_map": {
                "spk_a": {"display_name": "顾客", "role": "客户", "confidence": 0.95}
              },
              "main_person": "顾客",
              "extra_field": "ignored"
            }
            ```
            结尾文本
            """.trimIndent()
        )
        val orchestrator = RealTranscriptOrchestrator(metaHub, dispatchers, ai)

        val result = orchestrator.inferTranscriptMetadata(
            TranscriptMetadataRequest(
                transcriptId = "t-6",
                sessionId = "s-6",
                diarizedSegments = listOf(DiarizedSegment("spk_a", 0, 0, 1_000, "你好")),
                speakerLabels = emptyMap()
            )
        )
        advanceUntilIdle()

        assertEquals("顾客", result?.speakerMap?.get("spk_a")?.displayName)
        assertEquals("顾客", metaHub.getSession("s-6")?.mainPerson)
    }
```

### 4.2 HomeOrchestratorImpl JSON 解析

**状态**: ✅ **OK**

- **多 fenced block** (`HomeOrchestratorImplTest.kt:76-102`): 测试覆盖 "uses first json block when multiple fenced blocks exist"，只使用第一个有效 JSON
- **缺省字段解析** (`HomeOrchestratorImplTest.kt:104-126`): 测试覆盖 "parses minimal metadata even when optional blocks missing"，JSON 缺少 `speaker_map` / `crm_rows` 时仍能安全解析 session-level 字段

**证据**:
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

```76:102:feature/chat/src/test/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImplTest.kt
    @Test
    fun `uses first json block when multiple fenced blocks exist`() = runTest(dispatcher) {
        val metaHub = RecordingMetaHub()
        val aiChatService = CompletedAiChatService(
            """
            ```json
            { "main_person": "客户一", "summary_title_6chars": "标题一" }
            ```
            无关文本
            ```json
            { "main_person": "客户二" }
            ```
            """.trimIndent()
        )
        val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

        orchestrator.streamChat(
            ChatRequest(
                sessionId = "s-first",
                userMessage = "hi",
                quickSkillId = QuickSkillId.SMART_ANALYSIS.name
            )
        ).collect { /* no-op */ }

        assertEquals("客户一", metaHub.lastSession?.mainPerson)
        assertEquals("标题一", metaHub.lastSession?.summaryTitle6Chars)
    }
```

---

## 5. Home Export 宏与 guardrail 测试

### 5.1 导出分支

**状态**: ✅ **OK**

- **onExportPdfClicked** (`HomeScreenViewModel.kt:310-312`): 调用 `exportMarkdown(ExportFormat.PDF)`
- **onExportCsvClicked** (`HomeScreenViewModel.kt:314-316`): 调用 `exportMarkdown(ExportFormat.CSV)`
- **exportMarkdown 内部** (`HomeScreenViewModel.kt:389-419`): 最终调用 `exportOrchestrator.exportPdf` 或 `exportOrchestrator.exportCsv`，无 `exportMarkdown` 调用

**证据**:
```310:316:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
    fun onExportPdfClicked() {
        exportMarkdown(ExportFormat.PDF)
    }

    fun onExportCsvClicked() {
        exportMarkdown(ExportFormat.CSV)
    }
```

```389:419:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
    private fun performExport(format: ExportFormat) {
        val markdown = latestAnalysisMarkdown ?: buildTranscriptMarkdown(_uiState.value.chatMessages)
        if (markdown.isBlank()) {
            _uiState.update { it.copy(exportInProgress = false, snackbarMessage = "暂无可导出的内容") }
            return
        }
        _uiState.update { it.copy(exportInProgress = true, chatErrorMessage = null) }
        viewModelScope.launch {
            when (val result = exportOrchestrator.exportMarkdown(sessionId, markdown, format)) {
                is Result.Success -> {
                    when (val share = shareHandler.shareExport(result.data)) {
                        is Result.Success -> _uiState.update { it.copy(exportInProgress = false) }
                        is Result.Error -> _uiState.update {
                            it.copy(
                                exportInProgress = false,
                                chatErrorMessage = share.throwable.message ?: "分享失败"
                            )
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            exportInProgress = false,
                            chatErrorMessage = result.throwable.message ?: "导出失败"
                        )
                    }
                }
            }
        }
    }
```

**🔴 编译错误**: `HomeScreenViewModel.kt:397` 中调用了 `exportOrchestrator.exportMarkdown(sessionId, markdown, format)`，但 `ExportOrchestrator` 接口中不存在此方法。这会导致编译失败。

### 5.2 MetaHub + 缓存逻辑

**状态**: 🟡 **Partial**

- **导出宏逻辑** (`HomeScreenViewModel.kt:354-387`): `exportMarkdown` 方法中，如果 `latestAnalysisMarkdown` 为空，会查找最新长内容并触发自动分析
- **MetaHub 检查**: 代码中未显式检查 MetaHub 的 `latestMajorAnalysis*` 字段来决定是否需要自动分析
- **测试覆盖** (`HomeExportActionsTest.kt:226-239`): 测试覆盖 "when metahub has analysis but vm cache empty it hints and skips auto run"，但实现中似乎缺少对 MetaHub 的显式检查

**证据**:
```354:387:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
    private fun exportMarkdown(format: ExportFormat) {
        if (_uiState.value.exportInProgress) return
        val analysis = latestAnalysisMarkdown
        if (analysis.isNullOrBlank()) {
            val (mainContent, context) = findLatestLongContent()
            if (mainContent == null) {
                performExport(format)
                return
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
            return
        }
        performExport(format)
    }
```

### 5.3 测试覆盖

**状态**: ✅ **OK**

- **HomeExportActionsTest** (`HomeExportActionsTest.kt:293-310`): `RecordingExportOrchestrator` 明确记录 `exportPdf` 和 `exportCsv` 调用
- **导出调用计数** (`HomeExportActionsTest.kt:209-223`): 测试覆盖 "second export reuses cached analysis without rerun"，确保第二次导出不会重复跑分析
- **MetaHub 提示测试** (`HomeExportActionsTest.kt:226-239`): 测试覆盖 MetaHub 有分析但 VM 缓存为空时的提示场景

**证据**:
```293:310:feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt
    private class RecordingExportOrchestrator : ExportOrchestrator {
        var lastPdfMarkdown: String? = null
        var lastFormat: ExportFormat? = null
        var pdfCallCount = 0
        var csvCallCount = 0
        override suspend fun exportPdf(sessionId: String, markdown: String): Result<ExportResult> {
            lastPdfMarkdown = markdown
            lastFormat = ExportFormat.PDF
            pdfCallCount += 1
            return Result.Success(ExportResult("demo.pdf", "application/pdf", ByteArray(0)))
        }

        override suspend fun exportCsv(sessionId: String): Result<ExportResult> {
            lastFormat = ExportFormat.CSV
            csvCallCount += 1
            return Result.Success(ExportResult("demo.csv", "text/csv", ByteArray(0)))
        }
    }
```

---

## 6. Tingwu & Transcript 路径是否保持 v2 行为

### 6.1 RealTranscriptOrchestrator

**状态**: ✅ **OK**

- **执行流程** (`TranscriptOrchestrator.kt:49-70`): 采样 → LLM → JSON → confidence clamp → MetaHub.merge
- **force/cached 语义** (`RealTranscriptOrchestratorTest.kt:77-109, 111-150`): 测试覆盖缓存命中与 force=true 绕过缓存

**证据**:
```49:70:data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt
    override suspend fun inferTranscriptMetadata(
        request: TranscriptMetadataRequest
    ): TranscriptMetadata? = withContext(dispatchers.default) {
        val cached = readCachedMetadata(request)
        if (cached != null) return@withContext cached

        val sampledSegments = sampleSegments(request.diarizedSegments)
        if (sampledSegments.isEmpty()) {
            return@withContext null
        }
        val prompt = buildPrompt(sampledSegments, request.speakerLabels)
        val rawText = when (val result = aiChatService.sendMessage(AiChatRequest(prompt = prompt))) {
            is Result.Success -> result.data.displayText
            is Result.Error -> {
                AiCoreLogger.w(TAG, "转写元数据推理失败：${result.throwable.message}")
                return@withContext null
            }
        }
        val jsonText = extractJsonBlock(rawText) ?: return@withContext null
        val parsed = parseMetadata(jsonText, request, sampledSegments.size) ?: return@withContext null
        persistMetadata(parsed)
    }
```

### 6.2 RealTingwuCoordinator

**状态**: ✅ **OK**

- **只调用 TranscriptOrchestrator** (`RealTingwuCoordinator.kt:1016-1045`): `refineSpeakerLabels` 方法调用 `transcriptOrchestrator.inferTranscriptMetadata`
- **confidence 阈值** (`RealTingwuCoordinator.kt:1040`): 使用 `confidence >= 0.6f` 决定覆盖 speaker 标签
- **Markdown 展示** (`RealTingwuCoordinator.kt:836-900`): `buildMarkdown` 只展示结果标签，不泄露 JSON 或 debug 文本

**证据**:
```1016:1045:data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
    private suspend fun refineSpeakerLabels(
        transcriptId: String,
        segments: List<DiarizedSegment>,
        fallback: Map<String, String>,
        force: Boolean = false
    ): Map<String, String> {
        if (segments.isEmpty()) return fallback
        val request = TranscriptMetadataRequest(
            transcriptId = transcriptId,
            sessionId = jobRequests[transcriptId]?.sessionId,
            segments = segments,
            fileName = jobRequests[transcriptId]?.audioAssetName,
            force = force
        )
        val metadata = runCatching { transcriptOrchestrator.inferTranscriptMetadata(request) }
            .getOrElse {
                logVerbose { "转写说话人推理失败：${it.message}" }
                return fallback
            } ?: return fallback
        if (metadata.speakerMap.isEmpty()) return fallback
        val merged = fallback.toMutableMap()
        metadata.speakerMap.forEach { (speakerId, meta) ->
            val name = meta.displayName?.takeIf { it.isNotBlank() }
            val confidence = meta.confidence ?: 0.0f
            if (name != null && (confidence >= 0.6f || !merged.containsKey(speakerId))) {
                merged[speakerId] = name
            }
        }
        return merged
    }
```

---

## 7. 回归/风险总览

| 检查项 | 状态 | 关键证据 |
|--------|------|----------|
| ExportOrchestrator 接口合约 | ✅ OK | 仅暴露 exportPdf/exportCsv，有契约测试 |
| ExportOrchestrator 无 LLM 依赖 | ✅ OK | 构造函数无 AiChatService，有边界测试 |
| RealTingwuCoordinator 无 LLM 依赖 | ✅ OK | 构造函数无 AiChatService，有边界测试 |
| SessionMetadata merge 不变量 | ✅ OK | 非空覆盖、tags 合并、crmRows 去重、时间戳单调，有单测 |
| TranscriptMetadata merge 不变量 | ✅ OK | speakerMap 合并、extra 合并、时间戳单调，有单测 |
| TranscriptOrchestrator JSON 解析 | ✅ OK | fenced 优先、fail-soft、冗余字段处理，有单测 |
| HomeOrchestratorImpl JSON 解析 | ✅ OK | 多 fenced block、缺省字段解析，有单测 |
| Home 导出分支 | 🔴 **Mismatch** | `HomeScreenViewModel.kt:397` 调用不存在的 `exportOrchestrator.exportMarkdown` |
| Home MetaHub 缓存逻辑 | 🟡 Partial | 缺少对 MetaHub `latestMajorAnalysis*` 的显式检查 |
| Tingwu v2 行为保持 | ✅ OK | 采样→LLM→JSON→confidence clamp→MetaHub.merge |
| Transcript v2 行为保持 | ✅ OK | 只调用 TranscriptOrchestrator，confidence ≥ 0.6f |

---

## 8. 高风险问题

### 🔴 编译错误风险

**位置**: `HomeScreenViewModel.kt:397`

**问题**: 代码调用 `exportOrchestrator.exportMarkdown(sessionId, markdown, format)`，但 `ExportOrchestrator` 接口中不存在此方法。

**预期行为**: 应该根据 `format` 分别调用 `exportOrchestrator.exportPdf(sessionId, markdown)` 或 `exportOrchestrator.exportCsv(sessionId)`。

**建议修复**:
```kotlin
when (format) {
    ExportFormat.PDF -> exportOrchestrator.exportPdf(sessionId, markdown)
    ExportFormat.CSV -> exportOrchestrator.exportCsv(sessionId)
}
```

---

## 9. 总体结论 + 推荐下一步

### 总体结论

**状态**: 🟡 **Partial** - 大部分 guardrails 已到位，但存在一个编译错误风险

**亮点**:
- ✅ ExportOrchestrator 接口合约清晰，有契约测试守护
- ✅ LLM 依赖边界明确，有边界测试验证
- ✅ MetaHub merge 不变量完整，有单测覆盖
- ✅ JSON 解析鲁棒性良好，有边界用例测试
- ✅ Tingwu/Transcript v2 行为保持

**问题**:
- 🔴 `HomeScreenViewModel.kt:397` 存在编译错误风险（调用不存在的方法）
- 🟡 Home 导出宏缺少对 MetaHub `latestMajorAnalysis*` 的显式检查

### 推荐下一步

1. **立即修复**: 修复 `HomeScreenViewModel.kt:397` 的导出调用，改为分别调用 `exportPdf` 或 `exportCsv`
2. **增强测试**: 在 `HomeExportActionsTest` 中增加对 MetaHub `latestMajorAnalysis*` 检查的测试
3. **代码审查**: 确认 `HomeScreenViewModel.exportMarkdown` 方法是否需要检查 MetaHub 的 `latestMajorAnalysis*` 字段

---

**审计完成时间**: 2025-12-06  
**审计人**: AI Assistant (只读模式)

