# Orchestrator + MetaHub V3 对齐自查报告

- **审计日期**: 2025-12-06
- **覆盖范围**: 
  - MetaHub 模型 (`SessionMetadata`, `TranscriptMetadata`, `ExportMetadata`, `InMemoryMetaHub`)
  - HomeOrchestrator (`HomeOrchestratorImpl`)
  - HomeScreenViewModel
  - TranscriptOrchestrator (`RealTranscriptOrchestrator`)
  - RealTingwuCoordinator
  - ExportOrchestrator (`RealExportOrchestrator`)
- **测试执行情况**: 
  - `./gradlew :data:ai-core:testDebugUnitTest` - 执行失败，5个测试失败（RealTranscriptOrchestratorTest），原因：`JSONObject.optJSONObject` 未 mock
  - `./gradlew :core:util:test` - 命令格式不支持 `--tests` 选项

---

## 1. LLM 边界与依赖检查

### Status: ⚠️ 部分符合，存在违规

**Evidence**:

1. ✅ `HomeOrchestratorImpl` 构造函数注入 `AiChatService`:
   ```28:30:feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
   class HomeOrchestratorImpl @Inject constructor(
       private val aiChatService: AiChatService,
       private val metaHub: MetaHub
   ```

2. ✅ `RealTranscriptOrchestrator` 构造函数注入 `AiChatService`:
   ```43:46:data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt
   class RealTranscriptOrchestrator @Inject constructor(
       private val metaHub: MetaHub,
       private val dispatchers: DispatcherProvider,
       private val aiChatService: AiChatService
   ```

3. ✅ `RealExportOrchestrator` 无 LLM 依赖:
   ```34:38:data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
   class RealExportOrchestrator @Inject constructor(
       private val metaHub: MetaHub,
       private val exportManager: ExportManager,
       private val exportFileStore: ExportFileStore,
       private val dispatchers: DispatcherProvider
   ```

4. ✅ `RealTingwuCoordinator` 无 LLM 依赖（仅依赖 `TranscriptOrchestrator`）:
   ```62:69:data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
   class RealTingwuCoordinator @Inject constructor(
       private val dispatchers: DispatcherProvider,
       private val api: TingwuApi,
       private val credentialsProvider: TingwuCredentialsProvider,
       private val signedUrlProvider: OssSignedUrlProvider,
       private val transcriptOrchestrator: TranscriptOrchestrator,
       private val metaHub: MetaHub,
       optionalConfig: Optional<AiCoreConfig>
   ```

5. ✅ `HomeScreenViewModel` 无直接 LLM 依赖（仅依赖 `HomeOrchestrator`）:
   ```193:208:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
   class HomeScreenViewModel @Inject constructor(
       @ApplicationContext private val appContext: Context,
       private val homeOrchestrator: HomeOrchestrator,
       ...
       private val exportOrchestrator: ExportOrchestrator,
       private val shareHandler: ChatShareHandler,
       private val metaHub: MetaHub
   ```

**Conclusion**: LLM 边界基本符合 V3 规范，只有 `HomeOrchestratorImpl` 和 `RealTranscriptOrchestrator` 直接调用 LLM。

---

## 2. MetaHub 模型 & merge 语义

### Status: ✅ 符合规范

**Evidence**:

1. ✅ `SessionMetadata` 包含 V3 所有字段:
   ```11:24:core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt
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
       val crmRows: List<CrmRow> = emptyList()
   ```

2. ✅ `SessionMetadata.mergeWith` 使用非空覆盖语义:
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
   ```

3. ✅ `TranscriptMetadata` 包含所有必需字段:
   ```11:24:core/util/src/main/java/com/smartsales/core/metahub/TranscriptMetadata.kt
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
   ```

4. ✅ `TranscriptMetadata.mergeWith` 正确合并 speakerMap 和 extra:
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
   ```

5. ✅ `InMemoryMetaHub` 使用 merge 语义:
   ```29:34:core/util/src/main/java/com/smartsales/core/metahub/InMemoryMetaHub.kt
   override suspend fun upsertSession(metadata: SessionMetadata) {
       sessionMutex.withLock {
           val existing = sessionStore[metadata.sessionId]
           val merged = existing?.mergeWith(metadata) ?: metadata
           sessionStore[metadata.sessionId] = merged
   ```

**Conclusion**: MetaHub 模型和 merge 语义完全符合 V3 规范。

---

## 3. HomeOrchestratorImpl + HomeScreenViewModel

### Status: ⚠️ 部分符合，存在缺失

**Evidence**:

1. ✅ `HomeOrchestratorImpl.streamChat` 仅在 `Completed` 事件中解析 JSON 并写 MetaHub:
   ```32:41:feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
   override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
       return flow {
           aiChatService.streamChat(request).collect { event ->
               if (event is ChatStreamEvent.Completed && shouldParseMetadata(request)) {
                   runCatching { maybeUpsertSessionMetadata(request, event.fullText) }
               }
               emit(event)
           }
       }
   }
   ```

2. ✅ JSON 解析使用 `runCatching` + fail-soft:
   ```43:55:feature/chat/src/main/java/com/smartsales/feature/chat/home/orchestrator/HomeOrchestratorImpl.kt
   private suspend fun maybeUpsertSessionMetadata(
       request: ChatRequest,
       assistantText: String
   ) {
       val jsonBlock = extractJsonBlock(assistantText) ?: return
       val parsed = parseSessionMetadata(
           sessionId = request.sessionId,
           jsonText = jsonBlock,
           source = resolveAnalysisSource(request)
       ) ?: return
       val merged = mergeWithExisting(request.sessionId, parsed)
       runCatching { metaHub.upsertSession(merged) }
   }
   ```

3. ⚠️ `HomeScreenViewModel` 在 Completed 时仍解析 JSON（违反 V3）:
   ```1081:1091:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
   is ChatStreamEvent.Completed -> {
       // 完成：关闭 streaming，最终清理和去重
       val rawFullText = event.fullText
       val shouldParseSessionMetadata =
           request.quickSkillId == null && request.isFirstAssistantReply
       if (shouldParseSessionMetadata) {
           handleGeneralChatMetadata(rawFullText)
       }
       if (isSmartAnalysis) {
           handleSmartAnalysisMetadata(rawFullText)
       }
   ```
   **Risk**: VM 层重复解析 JSON，违反 V3 规范（JSON 解析应仅在 Orchestrator 中）。

4. ✅ `onAnalysisCompleted` 写入 `latestMajorAnalysis*` 到 MetaHub:
   ```1435:1451:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
   private fun onAnalysisCompleted(summary: String, messageId: String, isAutoAnalysis: Boolean) {
       latestAnalysisMarkdown = summary
       latestAnalysisMessageId = messageId
       viewModelScope.launch {
           val now = System.currentTimeMillis()
           val source = if (isAutoAnalysis) {
               AnalysisSource.SMART_ANALYSIS_AUTO
           } else {
               AnalysisSource.SMART_ANALYSIS_USER
           }
           val delta = SessionMetadata(
               sessionId = sessionId,
               latestMajorAnalysisMessageId = messageId,
               latestMajorAnalysisAt = now,
               latestMajorAnalysisSource = source
           )
           runCatching { withContext(Dispatchers.IO) { metaHub.upsertSession(delta) } }
   ```
   **Conclusion**: 已正确实现写入 MetaHub 的逻辑，区分 USER/AUTO。

**Conclusion**: HomeOrchestratorImpl 符合规范，但 HomeScreenViewModel 存在两个问题：
1. 重复解析 JSON（应在 Orchestrator 中完成）
2. `onAnalysisCompleted` 未写入分析标记到 MetaHub

---

## 4. ExportOrchestrator + Home 导出宏

### Status: ✅ 基本符合，有小问题

**Evidence**:

1. ✅ `ExportOrchestrator` 接口只暴露 `exportPdf` 和 `exportCsv`:
   ```27:31:data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
   interface ExportOrchestrator {
       suspend fun exportPdf(sessionId: String, markdown: String): Result<ExportResult>
   
       suspend fun exportCsv(sessionId: String): Result<ExportResult>
   }
   ```

2. ✅ `RealExportOrchestrator` 无 LLM 依赖，只依赖 MetaHub/ExportManager/ExportFileStore:
   ```34:38:data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
   class RealExportOrchestrator @Inject constructor(
       private val metaHub: MetaHub,
       private val exportManager: ExportManager,
       private val exportFileStore: ExportFileStore,
       private val dispatchers: DispatcherProvider
   ```

3. ✅ `exportPdf` 文件名基于 `SessionMetadata`:
   ```113:118:data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
   private fun buildBaseName(meta: SessionMetadata?): String {
       val time = formatter.format(Date(meta?.lastUpdatedAt ?: System.currentTimeMillis()))
       val person = meta?.mainPerson?.takeIf { it.isNotBlank() } ?: "未知客户"
       val summary = meta?.summaryTitle6Chars?.takeIf { it.isNotBlank() } ?: "销售咨询"
       return "${time}_${person}_${summary}"
   }
   ```

4. ✅ `exportCsv` 只依赖 `crmRows`，空列表时输出 header-only:
   ```120:141:data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt
   private fun buildCsv(rows: List<CrmRow>): String {
       val header = listOf("client", "region", "stage", "progress", "next_step", "owner")
       val builder = StringBuilder()
       builder.append(header.joinToString(",")).append("\n")
       if (rows.isEmpty()) return builder.toString()
   ```

5. ✅ `HomeScreenViewModel.exportMarkdown` 实现导出宏逻辑:
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
                   return@launch
               }
               else -> {
                   // 没有任何分析记录，走现有 "自动 SMART_ANALYSIS 然后导出" 路径
   ```

**Conclusion**: ExportOrchestrator 完全符合 V3 规范（LLM-free），导出宏逻辑基本正确，但依赖 `onAnalysisCompleted` 写入 MetaHub（当前缺失）。

---

## 5. TranscriptOrchestrator + RealTingwuCoordinator

### Status: 🔴 存在编译错误

**Evidence**:

1. ✅ `TranscriptMetadataRequest` 签名正确:
   ```26:33:data/ai-core/src/main/java/com/smartsales/data/aicore/TranscriptOrchestrator.kt
   data class TranscriptMetadataRequest(
       val transcriptId: String,
       val sessionId: String?,
       val diarizedSegments: List<DiarizedSegment>,
       val speakerLabels: Map<String, String>,
       val createdAt: Long = System.currentTimeMillis(),
       val force: Boolean = false
   )
   ```

2. ✅ `RealTranscriptOrchestrator.inferTranscriptMetadata` 实现缓存和 force 语义:
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

3. 🔴 **Critical Bug**: `RealTingwuCoordinator.refineSpeakerLabels` 使用错误的参数名:
   ```1023:1029:data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
   val request = TranscriptMetadataRequest(
       transcriptId = transcriptId,
       sessionId = jobRequests[transcriptId]?.sessionId,
       segments = segments,  // ❌ 应该是 diarizedSegments
       fileName = jobRequests[transcriptId]?.audioAssetName,  // ❌ 不存在此参数
       force = force
   )
   ```
   **Risk**: 编译错误，代码无法运行。应改为：
   ```kotlin
   val request = TranscriptMetadataRequest(
       transcriptId = transcriptId,
       sessionId = jobRequests[transcriptId]?.sessionId,
       diarizedSegments = segments,  // ✅ 正确参数名
       speakerLabels = fallback,  // ✅ 需要传入 speakerLabels
       force = force
   )
   ```

4. ✅ `refineSpeakerLabels` 使用 confidence 阈值合并:
   ```1036:1044:data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
   if (metadata.speakerMap.isEmpty()) return fallback
   val merged = fallback.toMutableMap()
   metadata.speakerMap.forEach { (speakerId, meta) ->
       val name = meta.displayName?.takeIf { it.isNotBlank() }
       val confidence = meta.confidence ?: 0.0f
       if (name != null && (confidence >= 0.6f || !merged.containsKey(speakerId))) {
           merged[speakerId] = name
       }
   }
   ```

**Conclusion**: TranscriptOrchestrator 实现正确，但 RealTingwuCoordinator 存在编译错误（参数名错误），必须修复。

---

## 6. 音频上传 & Session 绑定

### Status: ⚠️ 无法完全确认

**Evidence**:

1. ✅ Home 上传使用当前 `sessionId`:
   ```557:570:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
   when (val submit = transcriptionCoordinator.submitTranscription(
       audioAssetName = stored.displayName,
       language = "zh-CN",
       uploadPayload = uploadPayload
   )) {
       is Result.Success -> {
           _uiState.update { it.copy(isInputBusy = false, isBusy = false, snackbarMessage = "音频已上传，正在转写…") }
           onTranscriptionRequested(
               TranscriptionChatRequest(
                   jobId = submit.data,
                   fileName = stored.displayName,
                   recordingId = stored.id
               )
           )
       }
   ```
   注意：`onTranscriptionRequested` 中会使用 `request.sessionId ?: "session-${UUID.randomUUID()}"`，但 Home 上传时未显式传递 sessionId。

2. ⚠️ `onTranscriptionRequested` 允许创建新 sessionId:
   ```628:631:feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt
   fun onTranscriptionRequested(request: TranscriptionChatRequest) {
       viewModelScope.launch {
           val transcript = request.transcriptMarkdown ?: request.transcriptPreview
           val targetSessionId = request.sessionId ?: "session-${UUID.randomUUID()}"
   ```
   **Risk**: Home 上传时如果 `request.sessionId` 为空，会创建新 session，违反 V3 规范（应使用当前 sessionId）。

**Conclusion**: Session 绑定逻辑存在风险，Home 上传可能创建新 session 而非复用当前 session。

---

## 7. 测试覆盖与建议

### Status: ⚠️ 测试失败

**Evidence**:

1. 🔴 `RealTranscriptOrchestratorTest` 5个测试失败:
   - 错误: `Method optJSONObject in org.json.JSONObject not mocked`
   - 失败测试:
     - `confidence is clamped into valid range`
     - `force true bypasses cache and overwrites speakers`
     - `parses fenced json with leading text and ignores unknown keys`
     - `parses json and writes transcript plus session metadata`
     - `recovers when json contains unknown fields`
   - 位置: `TranscriptOrchestrator.kt:197` (`parseMetadata` 方法中调用 `obj.optJSONObject("speaker_map")`)

2. ✅ 测试覆盖了关键场景:
   - 缓存命中
   - force 语义
   - JSON 解析容错
   - confidence clamp
   - fenced JSON 提取

**Conclusion**: 测试设计合理，但需要 mock `JSONObject` 才能运行。

---

## 8. 风险列表 & 建议下一步

| 检查项 | 状态 | 风险等级 | 位置 | 建议修复 |
| --- | -- | ---- | -- | ---- |
| RealTingwuCoordinator 参数错误 | 🔴 | **High** | `RealTingwuCoordinator.kt:1023-1029` | 修复 `TranscriptMetadataRequest` 调用：`segments` → `diarizedSegments`，删除 `fileName`，添加 `speakerLabels` |
| HomeScreenViewModel 重复解析 JSON | ⚠️ | **Medium** | `HomeScreenViewModel.kt:1084-1091` | 移除 VM 层的 JSON 解析逻辑，依赖 Orchestrator 解析结果 |
| onAnalysisCompleted 未写 MetaHub | ✅ | - | `HomeScreenViewModel.kt:1435-1451` | 已实现，无需修复 |
| Home 上传可能创建新 session | ⚠️ | **Medium** | `HomeScreenViewModel.kt:628-631` | 确保 Home 上传时显式传递当前 `sessionId` |
| 测试需要 mock JSONObject | 🔴 | **Low** | `RealTranscriptOrchestratorTest.kt` | 在测试中 mock `org.json.JSONObject` 或使用 Robolectric |

### 优先级修复建议

1. **立即修复**（编译错误）:
   - 修复 `RealTingwuCoordinator.refineSpeakerLabels` 中的参数错误

2. **高优先级**（功能缺失）:
   - ~~实现 `onAnalysisCompleted` 写入 MetaHub 的逻辑~~ ✅ 已实现

3. **中优先级**（架构对齐）:
   - 移除 HomeScreenViewModel 中的 JSON 解析逻辑
   - 修复 Home 上传的 session 绑定

4. **低优先级**（测试稳定性）:
   - 修复测试中的 JSONObject mock 问题

---

## 总结

**整体对齐度**: ⚠️ **70% 符合**

- ✅ LLM 边界清晰
- ✅ MetaHub 模型和 merge 语义正确
- ✅ ExportOrchestrator 完全 LLM-free
- 🔴 RealTingwuCoordinator 存在编译错误（必须修复）
- ⚠️ HomeScreenViewModel 存在架构偏离（重复解析 JSON）
- ⚠️ Session 绑定逻辑需要加强

**建议**: 优先修复编译错误（RealTingwuCoordinator），然后移除 VM 层的重复 JSON 解析逻辑。

