# 失败测试与生产代码提取总结

本文档提取了失败测试的完整方法体及其依赖的生产代码实现，按「文件名 + 方法名」分段组织。

---

## 一、测试代码提取

### 1.1 HomeExportActionsTest.kt

#### 测试方法：`export pdf uses analysis markdown and shares`

```kotlin
@Test
fun `export pdf uses analysis markdown and shares`() = runTest(dispatcher) {
    val longInput = "请帮我总结汽车行业的市场趋势和风险要点。".repeat(15)
    viewModel.onInputChanged(longInput)
    viewModel.onSmartAnalysisClicked()
    viewModel.onSendMessage()
    advanceUntilIdle()

    viewModel.onExportPdfClicked()
    advanceUntilIdle()

    assertEquals("智能分析结果\n\n分析结果", exportOrchestrator.lastPdfMarkdown)
    assertTrue(shareHandler.shared)
    assertTrue(!viewModel.uiState.value.exportInProgress)
}
```

#### 测试方法：`export runs auto smart analysis once when no analysis and long content`

```kotlin
@Test
fun `export runs auto smart analysis once when no analysis and long content`() = runTest(dispatcher) {
    val longInput = "这是一个很长的对话片段，用于导出前自动分析。".repeat(20)
    viewModel.onInputChanged(longInput)
    viewModel.onSendMessage()
    advanceUntilIdle()

    val callsBeforeExport = aiChatService.callCount

    viewModel.onExportPdfClicked()
    advanceUntilIdle()

    assertEquals(callsBeforeExport + 1, aiChatService.callCount)
    assertEquals(ExportFormat.PDF, exportOrchestrator.lastFormat)
    assertEquals(1, exportOrchestrator.pdfCallCount)
    assertTrue(exportOrchestrator.lastPdfMarkdown?.contains("智能分析结果") == true)
    assertTrue(shareHandler.shared)
    assertTrue(!viewModel.uiState.value.exportInProgress)
}
```

#### 测试专用 Helper：`RecordingExportOrchestrator`

```kotlin
private class RecordingExportOrchestrator : ExportOrchestrator {
    var lastPdfMarkdown: String? = null
    var lastFormat: ExportFormat? = null
    var pdfCallCount = 0
    var csvCallCount = 0
    override suspend fun exportPdf(
        sessionId: String,
        markdown: String,
        sessionTitle: String?,
        userName: String?
    ): Result<ExportResult> {
        lastPdfMarkdown = markdown
        lastFormat = ExportFormat.PDF
        pdfCallCount += 1
        return Result.Success(ExportResult("demo.pdf", "application/pdf", ByteArray(0)))
    }

    override suspend fun exportCsv(
        sessionId: String,
        sessionTitle: String?,
        userName: String?
    ): Result<ExportResult> {
        lastFormat = ExportFormat.CSV
        csvCallCount += 1
        return Result.Success(ExportResult("demo.csv", "text/csv", ByteArray(0)))
    }
}
```

---

### 1.2 HomeOrchestratorImplTest.kt

#### 测试方法：`metadata parser prefers last value when duplicates present`

```kotlin
@Test
fun `metadata parser prefers last value when duplicates present`() = runTest(dispatcher) {
    val metaHub = RecordingMetaHub()
    val aiChatService = CompletedAiChatService(
        """
        ```json
        { "short_summary": "短版本", "highlights": ["a"] }
        ```
        ```json
        { "short_summary": "长版本", "highlights": ["a","b"] }
        ```
        """.trimIndent()
    )
    val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

    orchestrator.streamChat(
        ChatRequest(
            sessionId = "dup",
            userMessage = "hi",
            quickSkillId = QuickSkillId.SMART_ANALYSIS.name
        )
    ).collect { /* no-op */ }

    assertEquals("长版本", metaHub.lastSession?.shortSummary)
    assertEquals(listOf("a", "b").toSet(), metaHub.lastSession?.tags)
}
```

#### 测试方法：`parses minimal metadata even when optional blocks missing`

```kotlin
@Test
fun `parses minimal metadata even when optional blocks missing`() = runTest(dispatcher) {
    val metaHub = RecordingMetaHub()
    val aiChatService = CompletedAiChatService(
        """
        结果如下：
        ```json
        { "main_person": "王总" }
        ```
        """.trimIndent()
    )
    val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

    val events = mutableListOf<ChatStreamEvent>()
    orchestrator.streamChat(
        ChatRequest(
            sessionId = "s-min",
            userMessage = "hi",
            quickSkillId = QuickSkillId.SMART_ANALYSIS.name
        )
    ).collect { events.add(it) }

    assertEquals("王总", metaHub.lastSession?.mainPerson)
    val completed = events.first() as ChatStreamEvent.Completed
    assertTrue(completed.fullText.contains("王总"))
}
```

#### 测试方法：`partial smart analysis keeps existing metadata and adds tags`

```kotlin
@Test
fun `partial smart analysis keeps existing metadata and adds tags`() = runTest(dispatcher) {
    val metaHub = RecordingMetaHub()
    metaHub.lastSession = SessionMetadata(
        sessionId = "merge",
        mainPerson = "初始客户",
        shortSummary = "旧摘要",
        summaryTitle6Chars = "旧标题",
        stage = com.smartsales.core.metahub.SessionStage.NEGOTIATION,
        riskLevel = com.smartsales.core.metahub.RiskLevel.HIGH,
        tags = setOf("旧标签")
    )
    val aiChatService = CompletedAiChatService(
        """
        ```json
        {
          "summary_title_6chars": "新标题",
          "actionable_tips": ["新增行动"]
        }
        ```
        """.trimIndent()
    )
    val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

    orchestrator.streamChat(
        ChatRequest(
            sessionId = "merge",
            userMessage = "go",
            quickSkillId = QuickSkillId.SMART_ANALYSIS.name
        )
    ).collect { /* no-op */ }

    val saved = metaHub.lastSession!!
    assertEquals("初始客户", saved.mainPerson)
    assertEquals("旧摘要", saved.shortSummary)
    assertEquals("新标题", saved.summaryTitle6Chars)
    assertEquals(com.smartsales.core.metahub.SessionStage.NEGOTIATION, saved.stage)
    assertEquals(com.smartsales.core.metahub.RiskLevel.HIGH, saved.riskLevel)
    assertTrue(saved.tags.contains("新增行动"))
    assertEquals(AnalysisSource.SMART_ANALYSIS_USER, saved.latestMajorAnalysisSource)
    assertTrue(saved.latestMajorAnalysisAt != null)
}
```

#### 测试方法：`smart analysis formatter removes progressive scaffolding and renumbers list`

```kotlin
@Test
fun `smart analysis formatter removes progressive scaffolding and renumbers list`() = runTest(dispatcher) {
    val messy = """
        ###### 客## 客户## 客户画像
        客户对产品有兴趣
        客户对产品有兴趣，询问价格和交付周期。
        1) 初步介绍
        3) 提供报价
        4) 跟进确认
        ```json
        {
          "short_summary": "短版本",
          "short_summary": "长版本",
          "highlights": ["a"],
          "highlights": ["a","b"],
          "summary": {
            "core_insight": "短",
            "core_insight": "长"
          }
        }
        ```
    """.trimIndent()
    val aiChatService = CompletedAiChatService(messy)
    val orchestrator = HomeOrchestratorImpl(aiChatService, RecordingMetaHub())

    val events = mutableListOf<ChatStreamEvent>()
    orchestrator.streamChat(
        ChatRequest(
            sessionId = "clean-1",
            userMessage = "go",
            quickSkillId = QuickSkillId.SMART_ANALYSIS.name
        )
    ).collect { events.add(it) }

    val completed = events.first() as ChatStreamEvent.Completed
    val cleaned = completed.fullText
    assertTrue(cleaned.contains("长版本"))
    assertFalse(cleaned.contains("######"))
    assertTrue(cleaned.contains("## 会话概要"))
    assertTrue(cleaned.contains("## 核心洞察"))
    assertFalse(cleaned.contains("短版本"))
    assertFalse(cleaned.contains("{"))
}
```

#### 测试方法：`uses last json block when multiple fenced blocks exist`

```kotlin
@Test
fun `uses last json block when multiple fenced blocks exist`() = runTest(dispatcher) {
    val metaHub = RecordingMetaHub()
    val aiChatService = CompletedAiChatService(
        """
        ```json
        { "main_person": "客户一", "summary_title_6chars": "标题一" }
        ```
        无关文本
        ```json
        { "main_person": "客户二", "summary_title_6chars": "标题二" }
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

    assertEquals("客户二", metaHub.lastSession?.mainPerson)
    assertEquals("标题二", metaHub.lastSession?.summaryTitle6Chars)
}
```

#### 测试方法：`markdown omits empty sections and hides json braces`

```kotlin
@Test
fun `markdown omits empty sections and hides json braces`() = runTest(dispatcher) {
    val metaHub = RecordingMetaHub()
    val aiChatService = CompletedAiChatService(
        """
        ```json
        { "sharp_line": "一句话话术" }
        ```
        """.trimIndent()
    )
    val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

    val events = mutableListOf<ChatStreamEvent>()
    orchestrator.streamChat(
        ChatRequest(
            sessionId = "omit",
            userMessage = "go",
            quickSkillId = QuickSkillId.SMART_ANALYSIS.name
        )
    ).collect { events.add(it) }

    val text = (events.first() as ChatStreamEvent.Completed).fullText
    assertFalse(text.contains("{"))
    assertFalse(text.contains("}"))
    assertFalse(text.contains("需求与痛点"))
    assertFalse(text.contains("建议与行动"))
    assertTrue(text.contains("## 一句话话术"))
}
```

#### 测试方法：`smart analysis parses metadata and renders formatted markdown`

```kotlin
@Test
fun `smart analysis parses metadata and renders formatted markdown`() = runTest(dispatcher) {
    val metaHub = RecordingMetaHub()
    val aiChatService = CompletedAiChatService(
        """
        ## 分析
        内容
        ```json
        {
          "main_person": "罗总",
          "short_summary": "会议纪要",
          "summary_title_6chars": "报价跟进",
          "location": "北京",
          "highlights": ["报价讨论"],
          "actionable_tips": ["跟进报价"],
          "summary": {
            "core_insight": "客户关注价格",
            "sharp_line": "锁定价格优势"
          }
        }
        ```
        """.trimIndent()
    )
    val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

    val events = mutableListOf<ChatStreamEvent>()
    orchestrator.streamChat(
        ChatRequest(
            sessionId = "session-1",
            userMessage = "请做智能分析",
            quickSkillId = QuickSkillId.SMART_ANALYSIS.name
        )
    ).collect { events.add(it) }

    assertEquals(1, events.size)
    val completed = events.first() as ChatStreamEvent.Completed
    assertTrue(completed.fullText.contains("罗总"))
    assertTrue(completed.fullText.contains("## 会话概要"))
    assertTrue(completed.fullText.contains("## 建议与行动"))
    assertTrue(completed.fullText.contains("1."))
    assertFalse(completed.fullText.contains("{"))

    val stored = metaHub.lastSession
    assertEquals("罗总", stored?.mainPerson)
    assertEquals("报价跟进", stored?.summaryTitle6Chars)
    assertTrue(stored?.tags?.contains("报价讨论") == true)
}
```

#### 测试方法：`actionable tips are renumbered locally`

```kotlin
@Test
fun `actionable tips are renumbered locally`() = runTest(dispatcher) {
    val metaHub = RecordingMetaHub()
    val aiChatService = CompletedAiChatService(
        """
        ```json
        {
          "main_person": "林总",
          "actionable_tips": ["跟进报价", "安排试驾", "回访决策人"]
        }
        ```
        """.trimIndent()
    )
    val orchestrator = HomeOrchestratorImpl(aiChatService, metaHub)

    val events = mutableListOf<ChatStreamEvent>()
    orchestrator.streamChat(
        ChatRequest(
            sessionId = "tips",
            userMessage = "go",
            quickSkillId = QuickSkillId.SMART_ANALYSIS.name
        )
    ).collect { events.add(it) }

    val completed = events.first() as ChatStreamEvent.Completed
    val numbered = completed.fullText.lines()
        .map { it.trim() }
        .filter { it.matches(Regex("^\\d+\\.\\s.+")) }
    assertEquals(listOf("1. 跟进报价", "2. 安排试驾", "3. 回访决策人"), numbered)
}
```

---

### 1.3 SessionTitleGeneratorTest.kt

#### 测试方法：`derive_title_with_company_and_email`

```kotlin
@Test
fun derive_title_with_company_and_email() {
    val title = SessionTitleGenerator.deriveSessionTitle(
        updatedAtMillis = fixedDate,
        firstUserMessage = "给阿里巴巴写一封报价跟进邮件",
        firstAssistantMessage = null
    )
    assertEquals("12/24_阿里巴巴_报价跟进邮件", title)
}
```

#### 测试方法：`derive_title_with_honorific_and_scene`

```kotlin
@Test
fun derive_title_with_honorific_and_scene() {
    val title = SessionTitleGenerator.deriveSessionTitle(
        updatedAtMillis = fixedDate,
        firstUserMessage = "罗总，中国区奥迪主管。展会项目",
        firstAssistantMessage = null
    )
    assertEquals("12/24_罗总_展会项目", title)
}
```

#### 测试方法：`derive_title_generic_when_vague`

```kotlin
@Test
fun derive_title_generic_when_vague() {
    val title = SessionTitleGenerator.deriveSessionTitle(
        updatedAtMillis = fixedDate,
        firstUserMessage = "帮我优化一下销售话术",
        firstAssistantMessage = null
    )
    assertEquals("12/24_未知客户_销售话术优化", title)
}
```

---

## 二、生产代码实现提取

### 2.1 HomeScreenViewModel.kt - 导出逻辑

#### 方法：`onExportPdfClicked()`

```kotlin
fun onExportPdfClicked() {
    exportMarkdown(ExportFormat.PDF)
}
```

#### 方法：`onExportCsvClicked()`

```kotlin
fun onExportCsvClicked() {
    exportMarkdown(ExportFormat.CSV)
}
```

#### 方法：`exportMarkdown(format: ExportFormat)`

```kotlin
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
                    // ... 其他参数
                )
            }
        }
    }
}
```

#### 方法：`performExport(format: ExportFormat, markdownOverride: String? = null)`

```kotlin
private suspend fun performExport(format: ExportFormat, markdownOverride: String? = null) {
    val markdown = markdownOverride ?: latestAnalysisMarkdown ?: buildTranscriptMarkdown(_uiState.value.chatMessages)
    if (format == ExportFormat.PDF && markdown.isBlank()) {
        _uiState.update { it.copy(exportInProgress = false, snackbarMessage = "暂无可导出的内容") }
        return
    }
    _uiState.update { it.copy(exportInProgress = true, chatErrorMessage = null) }
    val sessionTitle = _uiState.value.currentSession.title
    val result = when (format) {
        ExportFormat.PDF -> exportOrchestrator.exportPdf(sessionId, markdown, sessionTitle, _uiState.value.userName)
        ExportFormat.CSV -> exportOrchestrator.exportCsv(sessionId, sessionTitle, _uiState.value.userName)
    }
    when (result) {
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
```

#### 方法：`findLatestLongContent(): Pair<String?, String?>`

```kotlin
private fun findLatestLongContent(): Pair<String?, String?> {
    val messages = _uiState.value.chatMessages.asReversed()
    var primary: String? = null
    val contextChunks = mutableListOf<String>()
    var contextLength = 0
    for (msg in messages) {
        val text = msg.content.trim()
        if (text.isEmpty()) continue
        if (primary == null && text.length >= LONG_CONTENT_THRESHOLD) {
            primary = text
            continue
        }
        if (contextChunks.size < CONTEXT_MESSAGE_LIMIT && contextLength < CONTEXT_LENGTH_LIMIT) {
            val toAdd = text.take(CONTEXT_LENGTH_LIMIT - contextLength)
            contextChunks += toAdd
            contextLength += toAdd.length
        } else {
            break
        }
    }
    return primary to contextChunks.asReversed().joinToString("\n")
}
```

---

### 2.2 HomeOrchestratorImpl.kt - 元数据解析与格式化

#### 方法：`extractLastSmartJson(text: String): JSONObject?`

```kotlin
private fun extractLastSmartJson(text: String): JSONObject? {
    val candidates = mutableListOf<String>()
    val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
    fenced.findAll(text).forEach { match ->
        match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
    }
    candidates.addAll(collectTopLevelJsonSlices(text))
    for (slice in candidates.asReversed()) {
        val obj = runCatching { JSONObject(slice) }.getOrNull()
        if (obj != null) return obj
    }
    return null
}
```

#### 方法：`collectTopLevelJsonSlices(text: String): List<String>`

```kotlin
private fun collectTopLevelJsonSlices(text: String): List<String> {
    val startStack = ArrayDeque<Int>()
    val ranges = mutableListOf<IntRange>()
    text.forEachIndexed { index, ch ->
        when (ch) {
            '{' -> startStack.addLast(index)
            '}' -> if (startStack.isNotEmpty()) {
                val start = startStack.removeLast()
                if (startStack.isEmpty()) {
                    ranges.add(start..index)
                }
            }
        }
    }
    return ranges.map { range ->
        text.substring(range.first, range.last + 1).trim()
    }
}
```

#### 方法：`parseSmartAnalysisPayload(rawText: String): ParsedSmartAnalysis?`

```kotlin
private fun parseSmartAnalysisPayload(rawText: String): ParsedSmartAnalysis? {
    val jsonObject = extractLastSmartJson(rawText) ?: return null
    val summary = jsonObject.optJSONObject("summary")

    val mainPerson = jsonObject.optString("main_person").takeIf { it.isNotBlank() }
    val shortSummary = jsonObject.optString("short_summary").takeIf { it.isNotBlank() }
    val summaryTitle = jsonObject.optString("summary_title_6chars").takeIf { it.isNotBlank() }?.take(6)
    val location = jsonObject.optString("location").takeIf { it.isNotBlank() }

    val stage = jsonObject.optString("stage").takeIf { it.isNotBlank() }?.let { toStage(it) }
    val risk = jsonObject.optString("risk_level").takeIf { it.isNotBlank() }?.let { toRisk(it) }

    val highlights = jsonObject.optJSONArray("highlights")?.toStringList().orEmpty()
    val actionable = jsonObject.optJSONArray("actionable_tips")?.toStringList().orEmpty()
    val coreInsight = jsonObject.optString("core_insight").takeIf { it.isNotBlank() }
        ?: summary?.optString("core_insight")?.takeIf { it.isNotBlank() }
    val sharpLine = jsonObject.optString("sharp_line").takeIf { it.isNotBlank() }
        ?: summary?.optString("sharp_line")?.takeIf { it.isNotBlank() }

    val hasContent = listOf(mainPerson, shortSummary, summaryTitle, location, coreInsight, sharpLine)
        .any { !it.isNullOrBlank() } ||
        highlights.isNotEmpty() ||
        actionable.isNotEmpty() ||
        stage != null ||
        risk != null
    if (!hasContent) return null

    return ParsedSmartAnalysis(
        mainPerson = mainPerson,
        shortSummary = shortSummary,
        summaryTitle6Chars = summaryTitle,
        location = location,
        stage = stage,
        riskLevel = risk,
        highlights = highlights,
        actionableTips = actionable,
        coreInsight = coreInsight,
        sharpLine = sharpLine
    )
}
```

#### 方法：`mergeWithExisting(sessionId: String, parsed: SessionMetadata): SessionMetadata`

```kotlin
private suspend fun mergeWithExisting(
    sessionId: String,
    parsed: SessionMetadata
): SessionMetadata {
    // 从 MetaHub 读取已有的会话元数据
    val existing = metaHub.getSession(sessionId)

    // 使用 SessionMetadata.mergeWith 做"非空优先新值"的合并：
    // - 如果没有 existing，就直接用 parsed
    // - 如果有 existing，就让 existing.mergeWith(parsed)，保证新分析结果覆盖旧值
    return existing?.mergeWith(parsed) ?: parsed
}
```

#### 方法：`buildSmartAnalysisMarkdown(parsed: ParsedSmartAnalysis): String`

```kotlin
private fun buildSmartAnalysisMarkdown(parsed: ParsedSmartAnalysis): String {
    // SMART_ANALYSIS 最终 Markdown 生成：完全由本地控制，避免 LLM 模板污染
    val sb = StringBuilder()
    parsed.shortSummary?.takeIf { it.isNotBlank() }?.let { summary ->
        sb.appendLine("## 会话概要")
        sb.appendLine("- ${summary.trim()}")
        sb.appendLine()
    }

    val personaLines = mutableListOf<String>()
    parsed.mainPerson?.takeIf { it.isNotBlank() }?.let { personaLines.add("- 主要联系人：${it.trim()}") }
    parsed.location?.takeIf { it.isNotBlank() }?.let { personaLines.add("- 所在地：${it.trim()}") }
    if (personaLines.isNotEmpty()) {
        sb.appendLine("## 客户画像与意图")
        personaLines.forEach { line -> sb.appendLine(line) }
        sb.appendLine()
    }

    if (parsed.highlights.isNotEmpty()) {
        sb.appendLine("## 需求与痛点")
        parsed.highlights.forEach { highlight ->
            if (highlight.isNotBlank()) {
                sb.appendLine("- ${highlight.trim()}")
            }
        }
        sb.appendLine()
    }

    val riskLines = mutableListOf<String>()
    parsed.stage?.let { riskLines.add("- 销售阶段：${formatStage(it)}") }
    parsed.riskLevel?.let { riskLines.add("- 风险等级：${formatRisk(it)}") }
    if (riskLines.isNotEmpty()) {
        sb.appendLine("## 机会与风险")
        riskLines.forEach { line -> sb.appendLine(line) }
        sb.appendLine()
    }

    if (parsed.actionableTips.isNotEmpty()) {
        sb.appendLine("## 建议与行动")
        parsed.actionableTips.forEachIndexed { index, tip ->
            if (tip.isNotBlank()) {
                sb.appendLine("${index + 1}. ${tip.trim()}")
            }
        }
        sb.appendLine()
    }

    parsed.coreInsight?.takeIf { it.isNotBlank() }?.let { insight ->
        sb.appendLine("## 核心洞察")
        sb.appendLine("- ${insight.trim()}")
        sb.appendLine()
    }

    parsed.sharpLine?.takeIf { it.isNotBlank() }?.let { line ->
        sb.appendLine("## 一句话话术")
        sb.appendLine("- ${line.trim()}")
        sb.appendLine()
    }

    return sb.toString().trimEnd().ifBlank { SMART_ANALYSIS_FAILURE_MESSAGE }
}
```

---

### 2.3 SessionTitleGenerator.kt - 标题生成

#### 方法：`deriveSessionTitle(updatedAtMillis: Long, firstUserMessage: String, firstAssistantMessage: String?): String`

```kotlin
fun deriveSessionTitle(
    updatedAtMillis: Long,
    firstUserMessage: String,
    firstAssistantMessage: String?
): String {
    val datePart = dateFormatter.format(Date(updatedAtMillis))
    val majorName = extractMajorName(firstUserMessage) ?: extractMajorName(firstAssistantMessage ?: "")
    val summary = extractSummary(firstUserMessage) ?: extractSummary(firstAssistantMessage ?: "")
    val safeName = SessionTitlePolicy.resolvePerson(majorName)
    val safeSummary = SessionTitlePolicy.resolveSummary(summary)
    return "${safeName}_${safeSummary}_${datePart}"
}
```

#### 方法：`extractMajorName(text: String): String?`

```kotlin
private fun extractMajorName(text: String): String? {
    if (text.isBlank()) return null
    honorificPattern.find(text)?.let { match ->
        val surname = match.groupValues[1]
        val honorific = match.groupValues[2]
        return "$surname$honorific"
    }
    companyWritePattern.find(text)?.let { match ->
        val candidate = match.groupValues.getOrNull(1)?.trim().orEmpty()
        if (candidate.isNotBlank()) return candidate
    }
    companyPattern.find(text)?.let { match ->
        for (i in 1..3) {
            val candidate = match.groupValues.getOrNull(i)?.trim().orEmpty()
            if (candidate.isNotBlank()) return candidate
        }
    }
    return null
}
```

#### 方法：`extractSummary(text: String): String?`

```kotlin
private fun extractSummary(text: String): String? {
    if (text.isBlank()) return null
    summaryKeywords.forEach { (key, value) ->
        if (text.contains(key)) return value
    }
    return null
}
```

---

## 三、关键依赖说明

### 3.1 导出流程依赖

- `latestAnalysisMarkdown`: ViewModel 中缓存的智能分析结果 markdown
- `exportOrchestrator.exportPdf()` / `exportOrchestrator.exportCsv()`: 执行实际导出
- `shareHandler.shareExport()`: 分享导出结果

### 3.2 元数据解析依赖

- `extractLastSmartJson()`: 提取最后一个有效的 JSON 块（优先 fenced blocks，fallback 到 top-level JSON）
- `parseSmartAnalysisPayload()`: 解析 JSON 并构建 `ParsedSmartAnalysis`
- `mergeWithExisting()`: 合并新解析的元数据与已有元数据
- `buildSmartAnalysisMarkdown()`: 将解析结果格式化为最终 markdown（去除 scaffolding，重编号列表，隐藏 JSON）

### 3.3 标题生成依赖

- `extractMajorName()`: 提取主要对象（honorific 或公司名）
- `extractSummary()`: 提取场景关键词
- `SessionTitlePolicy.resolvePerson()` / `SessionTitlePolicy.resolveSummary()`: 安全处理并生成最终标题

---

## 四、测试期望对照

### 4.1 导出测试期望

1. **`export pdf uses analysis markdown and shares`**: 
   - 期望：导出时使用缓存的智能分析 markdown（格式为 "智能分析结果\n\n分析结果"）
   - 期望：调用分享 handler
   - 期望：导出完成后 `exportInProgress` 为 false

2. **`export runs auto smart analysis once when no analysis and long content`**:
   - 期望：当没有分析结果且内容足够长时，自动触发一次智能分析
   - 期望：导出 markdown 包含 "智能分析结果"
   - 期望：只调用一次 PDF 导出

### 4.2 Orchestrator 测试期望

1. **`metadata parser prefers last value when duplicates present`**: 
   - 期望：当存在多个 JSON 块时，使用最后一个块的值

2. **`parses minimal metadata even when optional blocks missing`**: 
   - 期望：即使缺少可选字段，也能解析最小元数据

3. **`partial smart analysis keeps existing metadata and adds tags`**: 
   - 期望：部分分析时保留已有元数据，只更新新字段

4. **`smart analysis formatter removes progressive scaffolding and renumbers list`**: 
   - 期望：去除 progressive scaffolding（如 "######"），重编号列表，使用最后一个值

5. **`uses last json block when multiple fenced blocks exist`**: 
   - 期望：多个 fenced JSON 块时使用最后一个

6. **`markdown omits empty sections and hides json braces`**: 
   - 期望：markdown 中省略空章节，隐藏 JSON 大括号

7. **`smart analysis parses metadata and renders formatted markdown`**: 
   - 期望：解析元数据并渲染格式化的 markdown

8. **`actionable tips are renumbered locally`**: 
   - 期望：actionable tips 在本地重编号（1. 2. 3.）

### 4.3 标题生成测试期望

1. **`derive_title_with_company_and_email`**: 
   - 期望：从 "给阿里巴巴写一封报价跟进邮件" 提取公司名和场景

2. **`derive_title_with_honorific_and_scene`**: 
   - 期望：从 "罗总，中国区奥迪主管。展会项目" 提取 honorific 和场景

3. **`derive_title_generic_when_vague`**: 
   - 期望：模糊信息时使用通用 fallback（"未知客户" + 场景关键词）
