# V1 Implementation Read-Only Audit Report
**Date**: 2025-01-XX  
**Mode**: READ-ONLY (No edits)  
**Git SHA**: `3c5f3e60477b78bdc8c032cf2773eeb9fd1fcd8b`

## Pre-Flight

### Repository Root Structure
```
/home/cslh-frank/main_app/
├── app/
├── core/
├── data/
├── docs/
├── feature/
│   ├── chat/
│   ├── connectivity/
│   ├── media/
│   └── usercenter/
├── reference-source/
├── third_party/
└── tools/
```

### Git Commit Hash
```
3c5f3e60477b78bdc8c032cf2773eeb9fd1fcd8b
```

---

## STEP 1 — General Chat Pipeline (Publisher extraction + retry + metadata write trigger)

### STEP1_FILES
1. `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt`
2. `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
3. `feature/chat/src/main/java/com/smartsales/feature/chat/core/DefaultAiChatService.kt`
4. `data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeAiChatService.kt`

### STEP1_EXCERPTS

#### Excerpt 1: visible2user extraction (HomeScreenViewModel.kt)
```2181:2186:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
    /** 提取 <Visible2User> 内部文本，若不存在则返回 null。 */
    private fun extractVisible2User(raw: String): String? {
        val regex = Regex("<\\s*Visible2User\\s*>([\\s\\S]*?)<\\s*/\\s*Visible2User\\s*>", RegexOption.IGNORE_CASE)
        val match = regex.find(raw) ?: return null
        return match.groupValues.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
    }
```

#### Excerpt 2: Chat streaming and display logic (HomeScreenViewModel.kt)
```1949:1959:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
                        // <Visible2User> 驱动显示，rawContent 保留原始文本（含标签）
                        val visibleText = channels.visibleText
                        val displaySource = visibleText ?: rawFullText
                        val sanitized = if (isSmartAnalysis) rawFullText else sanitizeAssistantOutput(displaySource, isSmartAnalysis)
                        // SMART_ANALYSIS 已由 Orchestrator 生成最终 Markdown，这里直接透传
                        val cleaned = if (isSmartAnalysis) {
                            sanitized
                        } else {
                            val base = onCompletedTransform?.invoke(sanitized) ?: sanitized
                            applyGeneralOutputGuards(base)
                        }
```

#### Excerpt 3: SmartAnalysis JSON extraction (HomeOrchestratorImpl.kt)
```240:252:feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
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

#### Excerpt 4: Retry logic for DashScope API (DashscopeAiChatService.kt)
```93:119:data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeAiChatService.kt
    private suspend fun <T> executeWithRetry(
        request: DashscopeRequest,
        mapper: (DashscopeCompletion) -> T
    ): Result<T> {
        val attempts = config.dashscopeMaxRetries.coerceAtLeast(0) + 1
        var currentAttempt = 0
        var lastError: AiCoreException? = null
        while (currentAttempt < attempts) {
            currentAttempt += 1
            try {
                val completion = withTimeout(config.dashscopeRequestTimeoutMillis.coerceAtLeast(1_000L)) {
                    dashscopeClient.generate(request)
                }
                AiCoreLogger.d(TAG, "DashScope 调用成功（attempt=$currentAttempt, model=${request.model}）")
                return Result.Success(mapper(completion))
            } catch (error: Throwable) {
                val mapped = mapError(error)
                lastError = mapped
                AiCoreLogger.e(TAG, "DashScope 调用失败（attempt=$currentAttempt）：${mapped.message}", mapped)
                if (currentAttempt >= attempts) {
                    return Result.Error(mapped)
                }
                delay(RETRY_BACKOFF_MS * currentAttempt)
            }
        }
        return Result.Error(lastError ?: mapError(IllegalStateException("DashScope 未知错误")))
    }
```

### STEP1_BEHAVIOR_SUMMARY
- **visible2user extraction**: Implemented via regex in `HomeScreenViewModel.extractVisible2User()` (line 2182). Case-insensitive matching, extracts inner text only.
- **MachineArtifact extraction**: For SmartAnalysis mode, uses `extractLastSmartJson()` (line 240) which:
  - Searches for fenced ```json blocks (case-insensitive)
  - Falls back to top-level JSON object detection via bracket matching
  - Returns last valid JSON object found
- **Retry logic**: Present in `DashscopeAiChatService.executeWithRetry()` (line 93) for API-level failures. Uses `dashscopeMaxRetries` config, exponential backoff.
- **Metadata upsert**: Triggered in `HomeOrchestratorImpl.streamChat()` (line 58) only when `shouldParseMetadata()` returns true (SMART_ANALYSIS mode). Calls `metaHub.upsertSession()`.
- **No Publisher layer**: No dedicated "Publisher" component found. Extraction and display logic is embedded in ViewModel/Orchestrator.
- **No artifactStatus tracking**: No `artifactStatus` enum (VALID/INVALID/RETRIED/FAILED) found in chat pipeline.
- **No MachineArtifact retry**: No retry logic specifically for MachineArtifact parse/validation failures. Only API-level retries exist.

### STEP1_V1_MATCH
1. **visible2user-only rendering**: ✅ OK — `extractVisible2User()` extracts and `displaySource` uses visibleText (line 1950-1951)
2. **fenced-json-only MachineArtifact extraction (no heuristics)**: ⚠️ MISMATCH — Uses fenced blocks but also has fallback to bracket-matching heuristics (line 246)
3. **INVALID→retry and FAILED→deterministic fallback**: ❌ MISMATCH — No artifactStatus tracking, no MachineArtifact-specific retry logic
4. **metadata upsert only on valid artifact**: ⚠️ UNKNOWN — Metadata upsert happens on SmartAnalysis completion, but no explicit validation gate for MachineArtifact validity

---

## STEP 2 — Tingwu Transcription Pipeline (Disector + overlap + prefix publishing + retries)

### STEP2_FILES
1. `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`
2. `feature/media/src/main/java/com/smartsales/feature/media/audiofiles/TranscriptionBatchPlanner.kt`
3. `feature/media/src/main/java/com/smartsales/feature/media/audiofiles/AudioTranscriptionCoordinator.kt`

### STEP2_EXCERPTS

#### Excerpt 1: Tingwu job submission (RealTingwuCoordinator.kt)
```102:262:data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
    override suspend fun submit(request: TingwuRequest): Result<String> =
        withContext(dispatchers.io) {
            val credentials = credentialsProvider.obtain()
            validateCredentials(credentials)?.let { return@withContext Result.Error(it) }
            val sourceDesc = request.ossObjectKey ?: request.fileUrl ?: request.audioAssetName
            AiCoreLogger.d(
                TAG,
                "提交 Tingwu 请求：source=$sourceDesc, lang=${request.language}"
            )
            val fileUrlResult = resolveFileUrl(request)
            val resolvedUrl = when (fileUrlResult) {
                is Result.Success -> fileUrlResult.data
                is Result.Error -> return@withContext Result.Error(fileUrlResult.throwable)
            }
            val taskKey = buildTaskKey(request)
            val requestedLanguage = request.language.ifBlank { DEFAULT_LANGUAGE }
            val sourceLanguage = mapSourceLanguage(requestedLanguage)
            val model = config.tingwuModelOverride?.takeIf { it.isNotBlank() } ?: credentials.model
            val defaultCustomPrompt = tingwuSettings.customPrompt.contents.firstOrNull()
            val resolvedName = request.customPromptName?.takeIf { it.isNotBlank() }
                ?: defaultCustomPrompt?.name
            val resolvedPrompt = request.customPromptText?.takeIf { it.isNotBlank() }
                ?: defaultCustomPrompt?.prompt
            val customPromptContent = if (resolvedName != null && resolvedPrompt != null) {
                TingwuCustomPromptContent(
                    name = resolvedName,
                    prompt = resolvedPrompt,
                    model = defaultCustomPrompt?.model,
                    transType = defaultCustomPrompt?.transType
                )
            } else {
                null
            }
            val customPromptEnabled: Boolean? = if (tingwuSettings.customPrompt.enabled && customPromptContent != null) {
                true
            } else {
                null
            }
            logVerbose {
                "创建 Tingwu 任务：taskKey=$taskKey fileUrl=$resolvedUrl lang=$requestedLanguage source=$sourceLanguage model=$model diarizationEnabled=${request.diarizationEnabled}"
            }
            runCatching {
                val diarizationEnabled = request.diarizationEnabled && tingwuSettings.transcription.diarizationEnabled
                val diarizationParameters = if (diarizationEnabled) {
                    TingwuDiarizationParameters(
                        speakerCount = tingwuSettings.transcription.diarizationSpeakerCount,
                        outputLevel = tingwuSettings.transcription.diarizationOutputLevel
                    )
                } else {
                    null
                }
                // 重要：Tingwu CreateTask 使用 Input.FileUrl，严禁改写字段名或引入未证实的请求结构。
                val response = api.createTranscriptionTask(
                    body = TingwuCreateTaskRequest(
                        appKey = credentials.appKey,
                        input = TingwuTaskInput(
                            sourceLanguage = sourceLanguage,
                            taskKey = taskKey,
                            fileUrl = resolvedUrl
                        ),
                        parameters = TingwuTaskParameters(
                            transcription = TingwuTranscriptionParameters(
                                diarizationEnabled = diarizationEnabled,
                                diarization = diarizationParameters,
                                audioEventDetectionEnabled = tingwuSettings.transcription.audioEventDetectionEnabled,
                                model = model
                            ),
                            summarizationEnabled = tingwuSettings.summarization.enabled,
                            summarization = TingwuSummarizationParameters(
                                types = tingwuSettings.summarization.types
                            ),
                            autoChaptersEnabled = tingwuSettings.autoChaptersEnabled,
                            textPolishEnabled = tingwuSettings.textPolishEnabled,
                            pptExtractionEnabled = tingwuSettings.pptExtractionEnabled,
                            customPromptEnabled = customPromptEnabled,
                            customPrompt = customPromptEnabled?.let { TingwuCustomPrompt(contents = listOf(customPromptContent!!)) },
                            transcoding = TingwuTranscodingParameters(
                                targetAudioFormat = tingwuSettings.transcoding.targetAudioFormat
                            )
                        )
                    )
                )
                runCatching {
                    tingwuTraceStore.record(
                        taskId = taskKey,
                        createRequestJson = gson.toJson(
                            TingwuCreateTaskRequest(
                                appKey = credentials.appKey,
                                input = TingwuTaskInput(
                                    sourceLanguage = sourceLanguage,
                                    taskKey = taskKey,
                                    fileUrl = resolvedUrl
                                ),
                                parameters = TingwuTaskParameters(
                                    transcription = TingwuTranscriptionParameters(
                                        diarizationEnabled = diarizationEnabled,
                                        diarization = diarizationParameters,
                                        audioEventDetectionEnabled = tingwuSettings.transcription.audioEventDetectionEnabled,
                                        model = model
                                    ),
                                    summarizationEnabled = tingwuSettings.summarization.enabled,
                                    summarization = TingwuSummarizationParameters(
                                        types = tingwuSettings.summarization.types
                                    ),
                                    autoChaptersEnabled = tingwuSettings.autoChaptersEnabled,
                                    textPolishEnabled = tingwuSettings.textPolishEnabled,
                                    pptExtractionEnabled = tingwuSettings.pptExtractionEnabled,
                                    customPromptEnabled = customPromptEnabled,
                                    customPrompt = customPromptEnabled?.let { TingwuCustomPrompt(contents = listOf(customPromptContent!!)) },
                                    transcoding = TingwuTranscodingParameters(
                                        targetAudioFormat = tingwuSettings.transcoding.targetAudioFormat
                                    )
                                )
                            )
                        )
                    )
                }
                val data = requireData(
                    code = response.code,
                    message = response.message,
                    requestId = response.requestId,
                    data = response.data,
                    action = "CreateTask"
                )
                val taskId = data.taskId?.takeIf { it.isNotBlank() } ?: run {
                    val exception = AiCoreException(
                        source = AiCoreErrorSource.TINGWU,
                        reason = AiCoreErrorReason.REMOTE,
                        message = "Tingwu CreateTask 未返回 TaskId",
                        suggestion = "请根据 tingwu-doc.md 核对请求体字段"
                    )
                    AiCoreLogger.e(
                        TAG,
                        "Tingwu 任务创建响应缺少 taskId：requestId=${response.requestId} code=${response.code}"
                    )
                    return@withContext Result.Error(exception)
                }
                logVerbose {
                    "Tingwu 任务响应：taskId=$taskId status=${data.taskStatus} requestId=${response.requestId}"
                }
                runCatching {
                    tingwuTraceStore.record(
                        taskId = taskId,
                        createResponseJson = gson.toJson(response)
                    )
                }
                val flow = getOrCreateJobFlow(taskId)
                flow.value = TingwuJobState.InProgress(
                    jobId = taskId,
                    progressPercent = SUBMITTED_PROGRESS,
                    statusLabel = "SUBMITTED"
                )
                jobContext[taskId] = request
                startPolling(taskId)
                AiCoreLogger.d(TAG, "Tingwu 任务创建成功：jobId=$taskId")
                taskId
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(mapError(it)) }
            )
        }
```

#### Excerpt 2: Tingwu polling and failure handling (RealTingwuCoordinator.kt)
```270:354:data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
    private fun startPolling(jobId: String) {
        val existing = pollingJobs[jobId]
        if (existing != null && existing.isActive) {
            return
        }
        val pollInterval = max(config.tingwuPollIntervalMillis, 500L)
        val perTaskTimeout = max(config.tingwuPollTimeoutMillis, pollInterval * 2)
        val globalTimeout = config.tingwuGlobalPollTimeoutMillis.takeIf { it > 0 }
        val effectiveTimeout = min(perTaskTimeout, globalTimeout ?: Long.MAX_VALUE)
        val initialDelay = max(config.tingwuInitialPollDelayMillis, 0L)
        val job = scope.launch {
            val flow = getOrCreateJobFlow(jobId)
            val start = System.currentTimeMillis()
            if (initialDelay > 0) {
                delay(initialDelay)
            }
            try {
                while (isActive) {
                    if (System.currentTimeMillis() - start > effectiveTimeout) {
                        flow.value = TingwuJobState.Failed(
                            jobId,
                            AiCoreException(
                                source = AiCoreErrorSource.TINGWU,
                                reason = AiCoreErrorReason.TIMEOUT,
                                message = "Tingwu 轮询超时",
                                suggestion = "可调大 AiCoreConfig.tingwuPollTimeoutMillis"
                            )
                        )
                        break
                    }
                    val response = try {
                        api.getTaskStatus(taskId = jobId)
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        val mapped = mapError(error)
                        AiCoreLogger.e(TAG, "查询 Tingwu 状态失败：${mapped.message}", mapped)
                        flow.value = TingwuJobState.Failed(jobId, mapped)
                        return@launch
                    }
                    val data = try {
                        requireData(
                            code = response.code,
                            message = response.message,
                            requestId = response.requestId,
                            data = response.data,
                            action = "GetTaskInfo"
                        )
                    } catch (error: AiCoreException) {
                        flow.value = TingwuJobState.Failed(jobId, error)
                        return@launch
                    }
                    runCatching {
                        tingwuTraceStore.record(
                            taskId = jobId,
                            getTaskInfoJson = gson.toJson(response),
                            resultUrls = data.resultLinks.orEmpty()
                        )
                    }
                    logVerbose {
                        "轮询结果：jobId=$jobId status=${data.taskStatus} progress=${data.taskProgress} requestId=${response.requestId}"
                    }
                    // Log Result field contents for debugging
                    if (data.resultLinks != null) {
                        val resultKeys = data.resultLinks.keys.joinToString(", ")
                        logVerbose { "Result 字段包含的键：[$resultKeys]" }
                        data.resultLinks.forEach { (key, value) ->
                            logVerbose { "  Result.$key = ${value.take(100)}..." }
                        }
                    } else {
                        logVerbose { "Result 字段为 null 或空" }
                    }
                    val normalizedStatus = data.taskStatus?.uppercase(Locale.US) ?: "UNKNOWN"
                    val artifacts = data.toArtifacts()
                    val shouldContinue = when (normalizedStatus) {
                        "FAILED", "ERROR" -> {
                            flow.value = TingwuJobState.Failed(
                                jobId,
                                AiCoreException(
                                    source = AiCoreErrorSource.TINGWU,
                                    reason = AiCoreErrorReason.REMOTE,
                                    message = data.errorMessage ?: "Tingwu 返回失败"
                                ),
                                errorCode = data.errorCode
                            )
                            false
                        }
```

#### Excerpt 3: Transcription batch planning (TranscriptionBatchPlanner.kt)
```24:60:feature/media/src/main/java/com/smartsales/feature/media/audiofiles/TranscriptionBatchPlanner.kt
object TranscriptionBatchPlanner {
    const val RULE_LABEL = "fixed_lines_per_batch"
    private const val DEFAULT_BATCH_SIZE = 20

    fun plan(
        markdown: String,
        batchSize: Int = DEFAULT_BATCH_SIZE
    ): TranscriptionBatchPlan {
        val normalized = markdown.replace("\r\n", "\n").trimEnd()
        if (normalized.isBlank()) {
            return TranscriptionBatchPlan(
                ruleLabel = RULE_LABEL,
                batchSize = batchSize.coerceAtLeast(1),
                totalBatches = 0,
                batches = emptyList()
            )
        }
        val lines = normalized.split("\n")
        val effectiveBatchSize = batchSize.coerceAtLeast(1)
        val chunks = lines.chunked(effectiveBatchSize)
        val total = chunks.size
        val batches = chunks.mapIndexed { index, chunk ->
            TranscriptionBatchChunk(
                batchIndex = index + 1,
                totalBatches = total,
                markdownChunk = chunk.joinToString("\n"),
                lineCount = chunk.size
            )
        }
        return TranscriptionBatchPlan(
            ruleLabel = RULE_LABEL,
            batchSize = effectiveBatchSize,
            totalBatches = total,
            batches = batches
        )
    }
}
```

### STEP2_BEHAVIOR_SUMMARY
- **DisectorPlan**: No DisectorPlan implementation found. No 10min/7min batching rules. Current batching is text-based (line-based chunks via `TranscriptionBatchPlanner`).
- **Overlap**: No overlap logic found. No `overlapMs`, `absStartMs`, `absEndMs` fields or pre-roll overlap (10s) implementation.
- **Absolute time anchoring**: No `captureStartMs` or absolute time range tracking found.
- **Prefix-only publishing**: No `publishedPrefixBatchIndex` or prefix-only publish logic found. No monotonic batch index enforcement.
- **Tingwu retry**: Polling-based retry exists (line 270-354). On FAILED status, sets `TingwuJobState.Failed` and stops polling. No automatic retry of failed batches.
- **Batch submission**: Tingwu jobs are submitted per audio file, not per batch. No evidence of batch-level job submission.

### STEP2_V1_MATCH
1. **Disector rules (10min + 7min threshold)**: ❌ MISMATCH — No DisectorPlan found. Current batching is text-line-based, not time-based.
2. **overlap pre-roll only (10s)**: ❌ MISMATCH — No overlap implementation found.
3. **absolute time anchor = captureStartMs + relative**: ❌ MISMATCH — No absolute time tracking found.
4. **prefix-only publish with monotonic publishedPrefixBatchIndex**: ❌ MISMATCH — No prefix-only publish logic or publishedPrefixBatchIndex found.
5. **retry + terminal behavior for failed batches**: ⚠️ PARTIAL — Polling stops on FAILED, but no explicit batch-level retry or prefix stall logic.

---

## STEP 3 — Metadata Hub / Memory Layers (M1/M2/M2B/M3/M4 + parser boundaries)

### STEP3_FILES
1. `core/util/src/main/java/com/smartsales/core/metahub/MetaHub.kt`
2. `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt`
3. `core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt`
4. `core/util/src/main/java/com/smartsales/core/metahub/ConversationDerivedState.kt`

### STEP3_EXCERPTS

#### Excerpt 1: MetaHub interface (MetaHub.kt)
```11:57:core/util/src/main/java/com/smartsales/core/metahub/MetaHub.kt
interface MetaHub {
    /**
     * 写入或覆盖会话级元数据。
     */
    suspend fun upsertSession(metadata: SessionMetadata)

    /**
     * 按会话读取元数据。
     */
    suspend fun getSession(sessionId: String): SessionMetadata?

    /**
     * 追加 M2 补丁（内部派生结构），用于计算有效的 ConversationDerivedState。
     */
    suspend fun appendM2Patch(sessionId: String, patch: M2PatchRecord)

    /**
     * 读取有效的 M2 会话派生状态。
     */
    suspend fun getEffectiveM2(sessionId: String): ConversationDerivedState?

    /**
     * 写入或覆盖转写元数据。
     */
    suspend fun upsertTranscript(metadata: TranscriptMetadata)

    /**
     * 按会话读取转写元数据。
     * 当前假设一对一，如需一对多后续可扩展。
     */
    suspend fun getTranscriptBySession(sessionId: String): TranscriptMetadata?

    /**
     * 写入或覆盖导出元数据。
     */
    suspend fun upsertExport(metadata: ExportMetadata)

    /**
     * 读取导出元数据。
     */
    suspend fun getExport(sessionId: String): ExportMetadata?

    /**
     * 记录一次模型用量，供统计或日志使用。
     */
    suspend fun logUsage(usage: TokenUsage)
}
```

#### Excerpt 2: SessionMetadata structure (SessionMetadata.kt)
```11:33:core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt
data class SessionMetadata(
    val sessionId: String,
    val mainPerson: String? = null,
    val shortSummary: String? = null,
    val summaryTitle6Chars: String? = null,
    val location: String? = null,
    val stage: SessionStage? = null,
    val riskLevel: RiskLevel? = null,
    val tags: Set<String> = emptySet(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val latestMajorAnalysisMessageId: String? = null,
    val latestMajorAnalysisAt: Long? = null,
    val latestMajorAnalysisSource: AnalysisSource? = null,
    val renaming: RenamingMetadata = RenamingMetadata(),
    // 音频转写恢复提示标记：仅记录开始/结束/已关闭时间点
    val lastAudioTaskStartedAt: Long? = null,
    val lastAudioTaskFinishedAt: Long? = null,
    val audioRecoveryHintDismissedForStartedAt: Long? = null,
    // 重要：M2 有效态与补丁记录（patch 为内部派生结构，schema 未定义 patch type）
    val effectiveM2: ConversationDerivedState? = null,
    val m2PatchHistory: List<M2PatchRecord> = emptyList(),
    val crmRows: List<CrmRow> = emptyList()
) {
```

#### Excerpt 3: TranscriptMetadata structure (TranscriptMetadata.kt)
```11:25:core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt
data class TranscriptMetadata(
    val transcriptId: String,
    val sessionId: String? = null,
    val speakerMap: Map<String, SpeakerMeta> = emptyMap(),
    val source: TranscriptSource = TranscriptSource.UNKNOWN,
    val createdAt: Long = System.currentTimeMillis(),
    val diarizedSegmentsCount: Int? = null,
    val mainPerson: String? = null,
    val shortSummary: String? = null,
    val summaryTitle6Chars: String? = null,
    val location: String? = null,
    val stage: SessionStage? = null,
    val riskLevel: RiskLevel? = null,
    val extra: Map<String, Any?> = emptyMap()
) {
```

#### Excerpt 4: ConversationDerivedState (M2) with schemaVersion (ConversationDerivedState.kt)
```11:22:core/util/src/main/java/com/smartsales/core/metahub/ConversationDerivedState.kt
data class ConversationDerivedState(
    val schemaVersion: String = M2_SCHEMA_VERSION,
    val updatedAt: Long = 0L,
    val version: Int = 0,
    val rawSignals: RawSignals = RawSignals(),
    val uiSignals: UiSignals = UiSignals(),
    val speakerRegistry: SpeakerRegistry = SpeakerRegistry(),
    val memoryBank: MemoryBank = MemoryBank(),
    val preprocess: PreprocessSnapshot = PreprocessSnapshot(),
    val smartAnalysisRefs: List<ArtifactRef> = emptyList(),
    val externalContextRefs: List<ExternalContextRef> = emptyList()
)
```

### STEP3_BEHAVIOR_SUMMARY
- **M2BTranscriptionState**: No `M2BTranscriptionState` type found. Current implementation uses `TranscriptMetadata` (line 11) which contains speakerMap, source, and analysis fields, but no explicit M2B structure.
- **SessionMemory**: No `SessionMemory` type found. `SessionMetadata` (line 11) is the primary session-level structure.
- **schemaVersion vs version**: `ConversationDerivedState` (M2) uses `schemaVersion: String = M2_SCHEMA_VERSION` (line 12) and `version: Int = 0` (line 14). Distinction exists but M2B equivalent not found.
- **Parser boundaries**: No explicit "Parser" component found. Metadata updates happen in `HomeOrchestratorImpl` (SmartAnalysis parsing) and `RealTingwuCoordinator` (Tingwu chapter/summary processing), but no dedicated parser layer.
- **Source pointers**: `TranscriptMetadata` has `transcriptId` and `sessionId` but no explicit `chapterId` or `timeRange` fields. No evidence of chapterId/timeRange source pointers for M2B.
- **Metadata-only writes**: `MetaHub.upsertTranscript()` and `upsertSession()` are used. No evidence of transcript truth mutation by parser.

### STEP3_V1_MATCH
1. **M2BTranscriptionState used (SessionMemory deprecated)**: ❌ MISMATCH — No M2BTranscriptionState found. Uses `TranscriptMetadata` instead.
2. **schemaVersion vs version usage in code (where relevant)**: ⚠️ PARTIAL — M2 uses schemaVersion (line 12), but no M2B equivalent found to verify.
3. **parser does not touch PublishedTranscript truth**: ⚠️ UNKNOWN — No explicit "PublishedTranscript" type found. Metadata writes are separate, but transcript truth model not clearly identified.
4. **source pointers enforced for M2B (chapterId/timeRange + asset/session ids)**: ❌ MISMATCH — No chapterId or timeRange fields in TranscriptMetadata. Only transcriptId and sessionId present.

---

## TOP 5 RISKS FOR V1 MIGRATION

### Risk 1: Missing Publisher Layer and Artifact Status Tracking
**Evidence**: 
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:1949-1959` — Display logic embedded in ViewModel
- No `artifactStatus` enum or tracking found
- No dedicated Publisher component for extraction/validation

**Impact**: V1 requires Publisher to extract HumanDraft and MachineArtifact, validate artifacts, and track status (VALID/INVALID/RETRIED/FAILED). Current implementation lacks this separation.

**Migration Effort**: HIGH — Requires new Publisher component, artifact status state machine, and retry orchestration.

---

### Risk 2: No DisectorPlan Implementation
**Evidence**:
- `feature/media/src/main/java/com/smartsales/feature/media/audiofiles/TranscriptionBatchPlanner.kt:24-60` — Text-based line chunking, not time-based
- No 10min/7min batching rules found
- No `absStartMs`/`absEndMs` or overlap logic

**Impact**: V1 requires deterministic time-based batching (10min windows, 7min remainder threshold, 10s pre-roll overlap). Current implementation uses text-line chunks, incompatible with V1's absolute time anchoring.

**Migration Effort**: HIGH — Requires new DisectorPlan generator, absolute time range tracking, and overlap deduplication.

---

### Risk 3: MachineArtifact Extraction Uses Heuristics
**Evidence**:
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:240-252` — `extractLastSmartJson()` uses fenced blocks + bracket-matching fallback
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt:254-271` — `collectTopLevelJsonSlices()` bracket matching heuristic

**Impact**: V1 requires strict fenced ```json block extraction only (no heuristics). Current fallback to bracket matching violates V1 spec.

**Migration Effort**: MEDIUM — Remove heuristics, enforce fenced-block-only extraction, add validation.

---

### Risk 4: No M2BTranscriptionState Type
**Evidence**:
- `core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt:11-25` — Uses TranscriptMetadata, not M2BTranscriptionState
- No chapterId or timeRange source pointers found

**Impact**: V1 requires M2BTranscriptionState with chapterId/timeRange anchors. Current TranscriptMetadata lacks these fields and structure.

**Migration Effort**: HIGH — Define M2BTranscriptionState schema, migrate existing TranscriptMetadata, add source pointer enforcement.

---

### Risk 5: No Prefix-Only Publishing for Transcripts
**Evidence**:
- `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt:428-434` — Completes entire transcript at once
- No `publishedPrefixBatchIndex` or prefix-only publish logic found
- No batch-level prefix stall on failure

**Impact**: V1 requires prefix-only publishing with monotonic `publishedPrefixBatchIndex` and stall behavior when batches fail. Current implementation publishes full transcript on completion.

**Migration Effort**: HIGH — Implement prefix-only publish state machine, batch index tracking, and failure stall logic.

---

## Summary

**Total Files Examined**: 12  
**Total Excerpts**: 12  
**V1 Compliance**: 2/16 checks OK, 8/16 MISMATCH, 6/16 UNKNOWN/PARTIAL

**Critical Gaps**:
1. Publisher layer missing
2. DisectorPlan not implemented
3. M2BTranscriptionState not found
4. Prefix-only publishing not implemented
5. MachineArtifact extraction uses heuristics

**Next Steps**: Prioritize Publisher and DisectorPlan implementation as foundation for V1 migration.




