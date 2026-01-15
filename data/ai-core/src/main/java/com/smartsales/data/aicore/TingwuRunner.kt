package com.smartsales.data.aicore

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SpeakerMeta
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.LogTags
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.tingwu.api.TingwuApi
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.api.TingwuResultData
import com.smartsales.data.aicore.tingwu.api.TingwuResultResponse
import com.smartsales.data.aicore.tingwu.api.TingwuStatusData
import com.smartsales.data.aicore.tingwu.api.TingwuStatusResponse
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.tingwu.api.TingwuTaskInput
import com.smartsales.data.aicore.tingwu.api.TingwuTaskParameters
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import com.smartsales.data.aicore.tingwu.api.TingwuTranscriptSegment
import com.smartsales.data.aicore.tingwu.api.TingwuTranscriptionParameters
import com.smartsales.data.aicore.tingwu.api.TingwuDiarizationParameters
import com.smartsales.data.aicore.tingwu.api.TingwuSummarizationParameters
import com.smartsales.data.aicore.tingwu.api.TingwuTranscodingParameters
import com.smartsales.data.aicore.TranscriptMetadataRequest
import com.smartsales.data.aicore.tingwu.store.TingwuJobStore
import com.smartsales.data.aicore.tingwu.store.PersistedJob
import com.smartsales.data.aicore.tingwu.store.BatchStatus
import com.smartsales.data.aicore.disector.DisectorPlan
import com.smartsales.data.aicore.disector.DisectorBatch
import com.smartsales.data.aicore.tingwu.api.TingwuCustomPrompt
import com.smartsales.data.aicore.tingwu.api.TingwuCustomPromptContent
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.metahub.TingwuPreprocessPatchBuilder
import com.smartsales.data.aicore.tingwu.TingwuSuspiciousBoundaryDetector
import com.smartsales.data.aicore.tingwu.util.TingwuPayloadParser.getPrimitiveString
import com.smartsales.data.aicore.tingwu.util.TingwuPayloadParser.asJsonObjectOrNull
import com.smartsales.data.aicore.tingwu.util.TingwuPayloadParser.asLongOrNull

import com.smartsales.data.aicore.tingwu.artifact.TingwuArtifactFetcher
import com.smartsales.data.aicore.posttingwu.EnhancerInput
import com.smartsales.data.aicore.posttingwu.EnhancerUtterance
import com.smartsales.data.aicore.posttingwu.PostTingwuTranscriptEnhancer
import com.smartsales.data.aicore.posttingwu.applyEnhancerOutput
import com.smartsales.data.aicore.posttingwu.renderEnhancedMarkdown

import java.net.URL
import java.text.DecimalFormat
import java.util.LinkedHashMap
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import com.smartsales.data.aicore.tingwu.TingwuMultiBatchOrchestrator
import com.smartsales.data.aicore.tingwu.MultiBatchStitcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/TingwuRunner.kt
// 模块：:data:ai-core
// 说明：通过真实 Tingwu HTTP API 提交、轮询并拉取转写结果
// 作者：创建于 2025-11-16
@Singleton
class TingwuRunner @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val api: TingwuApi,
    private val credentialsProvider: TingwuCredentialsProvider,
    private val signedUrlProvider: OssSignedUrlProvider,
    private val transcriptOrchestrator: TranscriptOrchestrator,
    private val metaHub: MetaHub,
    private val tingwuTraceStore: TingwuTraceStore,

    private val artifactFetcher: TingwuArtifactFetcher,
    private val postTingwuTranscriptEnhancer: PostTingwuTranscriptEnhancer,
    private val aiParaSettingsProvider: AiParaSettingsProvider,
    private val apiRepository: com.smartsales.data.aicore.tingwu.polling.TingwuApiRepository,
    private val transcriptProcessor: com.smartsales.data.aicore.tingwu.processor.TranscriptProcessor,
    private val pipelineTracer: com.smartsales.data.aicore.debug.PipelineTracer,
    private val disector: com.smartsales.data.aicore.disector.Disector,
    private val multiBatchOrchestrator: com.smartsales.data.aicore.tingwu.TingwuMultiBatchOrchestrator,
    private val pollingLoop: com.smartsales.data.aicore.tingwu.polling.PollingLoop,
    private val jobStore: TingwuJobStore,
    private val submissionService: com.smartsales.data.aicore.tingwu.submission.TingwuSubmissionService,
    private val metaHubWriter: com.smartsales.data.aicore.tingwu.metadata.MetaHubWriter,
    private val resultProcessor: com.smartsales.data.aicore.tingwu.result.ResultProcessor,

    optionalConfig: Optional<AiCoreConfig>
) : TingwuCoordinator {

    private val config = optionalConfig.orElse(AiCoreConfig())
    private val tingwuSettings get() = aiParaSettingsProvider.snapshot().tingwu
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val jobStates = ConcurrentHashMap<String, MutableStateFlow<TingwuJobState>>()
    private val pollingJobs = ConcurrentHashMap<String, Job>()
    private val jobContext = ConcurrentHashMap<String, TingwuRequest>()
    private val timeFormatter = DecimalFormat("00")
    private val speakerDisplayConfig: SpeakerDisplayConfig = config.speakerDisplayConfig
    private val verboseLogging = config.tingwuVerboseLogging
    private val gson = Gson()

    override suspend fun submit(request: TingwuRequest): Result<String> =
        withContext(dispatchers.io) {
            val credentials = credentialsProvider.obtain()
            apiRepository.validateCredentials(credentials)?.let { return@withContext Result.Error(it) }
            val sourceDesc = request.ossObjectKey ?: request.fileUrl ?: request.audioAssetName
            AiCoreLogger.d(
                TAG,
                "提交 Tingwu 请求：source=$sourceDesc, lang=${request.language}"
            )
            val fileUrlResult = apiRepository.resolveFileUrl(request)
            val resolvedUrl = when (fileUrlResult) {
                is Result.Success -> fileUrlResult.data
                is Result.Error -> return@withContext Result.Error(fileUrlResult.throwable)
            }
            val taskKey = apiRepository.buildTaskKey(request)
            val requestedLanguage = request.language.ifBlank { DEFAULT_LANGUAGE }
            val sourceLanguage = apiRepository.mapSourceLanguage(requestedLanguage)
            logVerbose {
                "创建 Tingwu 任务：taskKey=$taskKey fileUrl=$resolvedUrl lang=$requestedLanguage source=$sourceLanguage diarizationEnabled=${request.diarizationEnabled}"
            }
            
            // V1 Appendix A: Disector batch planning (if duration provided)
            val disectorPlan = request.durationMs?.let { durationMs ->
                val audioAssetId = request.ossObjectKey ?: taskKey
                val recordingSessionId = request.sessionId ?: taskKey
                disector.createPlan(durationMs, audioAssetId, recordingSessionId).also { plan ->
                    // Delegate multi-batch submission to orchestrator
                    val audioFile = request.audioFilePath
                    if (plan.batches.size > 1 && audioFile != null) {
                        val parentJobId = multiBatchOrchestrator.submitMultiBatch(
                            plan = plan,
                            request = request,
                            audioFile = audioFile,
                            scope = scope,
                            getOrCreateJobFlow = ::getOrCreateJobFlow,
                            submitSingleBatch = { req -> submit(req) },
                            waitForJob = { id -> waitForJob(id) }
                        )
                        if (parentJobId != null) {
                            return@withContext Result.Success(parentJobId)
                        }
                    } else if (plan.batches.size > 1 && audioFile == null) {
                        return@withContext Result.Error(
                            AiCoreException(
                                source = AiCoreErrorSource.TINGWU,
                                reason = AiCoreErrorReason.IO,
                                message = "Multi-batch submission requires local 'audioFilePath'"
                            )
                        )
                    } else {
                        pipelineTracer.emit(
                            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_UPLOAD,
                            status = "SINGLE_BATCH",
                            message = "planId=${plan.disectorPlanId} durationMs=$durationMs"
                        )
                    }
                }
            }
            
            // Delegate to TingwuSubmissionService (Lattice Box)
            val submissionResult = submissionService.submit(
                com.smartsales.data.aicore.tingwu.submission.SubmissionInput(
                    fileUrl = resolvedUrl,
                    taskKey = taskKey,
                    sourceLanguage = sourceLanguage,
                    diarizationEnabled = request.diarizationEnabled,
                    customPromptName = request.customPromptName,
                    customPromptText = request.customPromptText
                )
            )
            
            when (submissionResult) {
                is Result.Success -> {
                    val taskId = submissionResult.data.taskId
                    val flow = getOrCreateJobFlow(taskId)
                    flow.value = TingwuJobState.InProgress(
                        jobId = taskId,
                        progressPercent = SUBMITTED_PROGRESS,
                        statusLabel = "SUBMITTED"
                    )
                    jobContext[taskId] = request
                    startPolling(taskId)
                    AiCoreLogger.d(TAG, "Tingwu 任务创建成功：jobId=$taskId")
                    pipelineTracer.emit(
                        stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_UPLOAD,
                        status = "COMPLETED",
                        message = "jobId=$taskId"
                    )
                    Result.Success(taskId)
                }
                is Result.Error -> submissionResult
            }
        }

    private suspend fun waitForJob(jobId: String): TingwuJobState {
        return observeJob(jobId)
            .filter { it is TingwuJobState.Completed || it is TingwuJobState.Failed }
            .first()
    }

    override fun observeJob(jobId: String): Flow<TingwuJobState> =
        getOrCreateJobFlow(jobId).asStateFlow()

    override suspend fun retryJob(jobId: String): Result<String> = withContext(dispatchers.io) {
        // Load persisted job
        val persistedJob = jobStore.loadJob(jobId)
            ?: return@withContext Result.Error(AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.UNKNOWN,
                message = "Job not found: $jobId"
            ))

        // Filter failed/pending batches
        val failedBatches = persistedJob.batches.filter { batch ->
            batch.status == BatchStatus.FAILED || batch.status == BatchStatus.PENDING
        }
        if (failedBatches.isEmpty()) {
            return@withContext Result.Error(AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.UNKNOWN,
                message = "No failed batches to retry in job: $jobId"
            ))
        }

        // Verify source file exists
        val sourceFile = persistedJob.audioFilePath?.let { java.io.File(it) }
        if (sourceFile == null || !sourceFile.exists()) {
            return@withContext Result.Error(AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.IO,
                message = "Source audio file not found for retry: ${persistedJob.audioFilePath}"
            ))
        }

        AiCoreLogger.d(TAG, "Retrying job $jobId: ${failedBatches.size} failed batches of ${persistedJob.batches.size} total")

        // Build DisectorPlan for failed batches only
        val retryPlan = DisectorPlan(
            disectorPlanId = "${persistedJob.jobId}_retry",
            audioAssetId = persistedJob.audioAssetName,
            recordingSessionId = persistedJob.jobId,
            totalMs = persistedJob.totalDurationMs ?: 0,
            batches = failedBatches.map { batch ->
                DisectorBatch(
                    batchAssetId = "${batch.batchIndex}_retry",
                    batchIndex = batch.batchIndex,
                    absStartMs = batch.captureStartMs ?: 0,
                    absEndMs = batch.captureEndMs ?: 0,
                    captureStartMs = batch.captureStartMs ?: 0,
                    captureEndMs = batch.captureEndMs ?: 0
                )
            }
        )

        // Rebuild original request for batch submission
        val request = TingwuRequest(
            audioAssetName = persistedJob.audioAssetName,
            language = persistedJob.language,
            ossObjectKey = persistedJob.ossObjectKey,
            fileUrl = persistedJob.fileUrl,
            audioFilePath = sourceFile
        )

        // Create flow for retry job
        val retryJobId = retryPlan.disectorPlanId
        val flow = getOrCreateJobFlow(retryJobId)
        flow.value = TingwuJobState.InProgress(retryJobId, 5, "正在重试失败的批次...")

        // Execute batch loop for failed batches
        val result = multiBatchOrchestrator.executeBatchLoop(
            plan = retryPlan,
            originalRequest = request,
            sourceFile = sourceFile,
            submitSingleBatch = { batchRequest -> submit(batchRequest) },
            waitForJob = { batchJobId -> waitForJob(batchJobId) },
            onProgress = { percent ->
                flow.value = TingwuJobState.InProgress(retryJobId, percent)
            },
            onComplete = { _, segments ->
                // Stitch with existing succeeded segments
                val existingSegments = persistedJob.batches
                    .filter { it.status == BatchStatus.SUCCEEDED }
                    .flatMap { 
                        // TODO: Load cached segments from artifact files
                        emptyList<DiarizedSegment>()
                    }
                val allSegments = (existingSegments + segments).sortedBy { it.startMs }
                val markdown = com.smartsales.data.aicore.tingwu.processor.TranscriptFormatter()
                    .buildMarkdown(null, allSegments, emptyMap())
                flow.value = TingwuJobState.Completed(
                    jobId = retryJobId,
                    transcriptMarkdown = markdown,
                    artifacts = TingwuJobArtifacts(
                        recordingOriginDiarizedSegments = allSegments
                    )
                )
            }
        )

        when (result) {
            is Result.Success -> Result.Success(retryJobId)
            is Result.Error -> {
                flow.value = TingwuJobState.Failed(
                    jobId = retryJobId,
                    error = result.throwable as? AiCoreException ?: AiCoreException(
                        source = AiCoreErrorSource.TINGWU,
                        reason = AiCoreErrorReason.UNKNOWN,
                        message = result.throwable.message ?: "Retry failed"
                    )
                )
                Result.Error(result.throwable)
            }
        }
    }

    override suspend fun getRetryableJobs(): List<PersistedJob> = withContext(dispatchers.io) {
        jobStore.getRetryableJobs()
    }

    /**
     * Cancels the internal coroutine scope. Call this in tests after each test
     * to avoid UncompletedCoroutinesError from leaked SupervisorJob.
     *
     * NOTE: In production, TingwuRunner is @Singleton and lives for app lifetime,
     * so this is typically not called. For test isolation, this is essential.
     */
    fun cancel() {
        scope.cancel()
    }

    private fun getOrCreateJobFlow(jobId: String): MutableStateFlow<TingwuJobState> =
        jobStates.getOrPut(jobId) { MutableStateFlow<TingwuJobState>(TingwuJobState.Idle) }






    private fun startPolling(jobId: String) {
        val existing = pollingJobs[jobId]
        if (existing != null && existing.isActive) {
            return
        }
        
        pipelineTracer.emit(
            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
            status = "STARTED",
            message = "jobId=$jobId (delegated to TingwuPollingLoop)"
        )

        val flow = getOrCreateJobFlow(jobId)

        val job = scope.launch {
            pollingLoop.poll(
                jobId = jobId,
                stateFlow = flow,
                onTerminal = { terminalState ->
                    // Handle terminal state (Completed or Failed)
                    try {
                        if (terminalState is TingwuJobState.Completed) {
                            handleJobCompletion(jobId, terminalState, flow)
                        } else if (terminalState is TingwuJobState.Failed) {
                            handleJobFailure(jobId, terminalState)
                        }
                    } finally {
                        pollingJobs.remove(jobId)
                        jobContext.remove(jobId)
                    }
                }
            )
        }
        pollingJobs[jobId] = job
    }

    private suspend fun handleJobCompletion(
        jobId: String,
        state: TingwuJobState.Completed,
        flow: MutableStateFlow<TingwuJobState>
    ) {
        AiCoreLogger.d(TAG, "任务状态为 COMPLETED，开始拉取转写结果：jobId=$jobId")
        
        // Fetch full transcript content (not included in polling status)
        val transcriptResult = try {
            fetchTranscript(
                jobId = jobId,
                resultLinks = state.artifacts?.extraResultUrls,
                fallbackArtifacts = state.artifacts
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            val mapped = apiRepository.mapError(error)
            AiCoreLogger.e(TAG, "拉取转写结果失败：${mapped.message}", mapped)
            flow.value = TingwuJobState.Failed(jobId, mapped)
            
            pipelineTracer.emit(
                stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
                status = "FAILED",
                message = "Fetch transcript failed: ${mapped.message}"
            )
            return
        }

        val mergedArtifacts = transcriptResult.artifacts
        val transcriptMeta = resultProcessor.refineSpeakerLabels(
            transcriptId = jobId,
            sessionId = jobContext[jobId]?.sessionId,
            diarizedSegments = transcriptResult.diarizedSegments.orEmpty(),
            speakerLabels = mergedArtifacts?.speakerLabels.orEmpty()
        )
        val mergedSpeakerLabels = resultProcessor.mergeSpeakerLabels(
            base = mergedArtifacts?.speakerLabels.orEmpty(),
            incoming = transcriptMeta?.speakerMap.orEmpty()
        )
        val artifactsWithMetadata = mergedArtifacts?.copy(
            speakerLabels = mergedSpeakerLabels
        ) ?: mergedArtifacts

        val request = jobContext[jobId]
        val audioAssetId = request?.ossObjectKey ?: request?.fileUrl ?: jobId
        metaHubWriter.writeSessionMetadata(
            com.smartsales.data.aicore.tingwu.metadata.SessionMetadataWriteInput(
                jobId = jobId,
                sessionId = jobContext[jobId]?.sessionId ?: "",
                audioAssetId = audioAssetId,
                artifacts = artifactsWithMetadata,
                transcriptMeta = transcriptMeta,
                chapters = transcriptResult.chapters
            )
        )

        // 说明：Tingwu 完成后记录可疑边界，供 M2 预处理补丁使用。
        runCatching {
            val boundaries = TingwuSuspiciousBoundaryDetector.detect(
                transcriptMarkdown = transcriptResult.markdown
            )
            tingwuTraceStore.record(
                taskId = jobId,
                suspiciousBoundaries = boundaries
            )
        }.onFailure { error ->
            AiCoreLogger.w(TAG, "Tingwu 可疑边界记录失败：${error.message}")
            tingwuTraceStore.record(
                taskId = jobId,
                suspiciousBoundaries = emptyList()
            )
        }

        metaHubWriter.writePreprocessPatch(
            com.smartsales.data.aicore.tingwu.metadata.PreprocessPatchInput(
                jobId = jobId,
                sessionId = jobContext[jobId]?.sessionId ?: "",
                transcriptMarkdown = transcriptResult.markdown
            )
        )

        AiCoreLogger.d(TAG, "转写结果拉取成功：jobId=$jobId markdown长度=${transcriptResult.markdown.length}")
        
        pipelineTracer.emit(
            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
            status = "COMPLETED",
            message = "jobId=$jobId chapters=${transcriptResult.chapters?.size ?: 0}"
        )

        // Emit final Completed state with full content
        flow.value = TingwuJobState.Completed(
            jobId = jobId,
            transcriptMarkdown = transcriptResult.markdown,
            artifacts = artifactsWithMetadata,
            statusLabel = state.statusLabel
        )
    }

    private fun handleJobFailure(jobId: String, state: TingwuJobState.Failed) {
        pipelineTracer.emit(
            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
            status = "FAILED",
            message = "jobId=$jobId error=${state.error.message}"
        )
    }

    private suspend fun fetchTranscript(
        jobId: String,
        resultLinks: Map<String, String>?,
        fallbackArtifacts: TingwuJobArtifacts?
    ): com.smartsales.data.aicore.tingwu.processor.TranscriptResult {
        return transcriptProcessor.fetchTranscript(
            jobId = jobId,
            resultLinks = resultLinks,
            fallbackArtifacts = fallbackArtifacts,
            runEnhancer = { trans, diarized, speakers, fallback ->
                runEnhancerIfEnabled(jobId, trans, diarized, speakers, fallback)
            },
            composeFinalMarkdown = { markdown, artifacts, links ->
                composeFinalMarkdown(markdown, artifacts, links)
            }
        )
    }
















    private fun <T> requireData(
        code: String?,
        message: String?,
        requestId: String?,
        data: T?,
        action: String
    ): T {
        if (!code.isNullOrBlank() && code != "0") {
            throw AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.REMOTE,
                message = "Tingwu $action 失败：code=$code message=${message.orEmpty()}",
                suggestion = "参考 tingwu-doc.md 检查权限或参数，requestId=$requestId"
            )
        }
        return data ?: throw AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.REMOTE,
            message = "Tingwu $action 响应缺少 Data",
            suggestion = "确认 API 是否返回 Data.* 字段，requestId=$requestId"
        )
    }




    private inline fun logVerbose(message: () -> String) {
        if (verboseLogging) {
            AiCoreLogger.v(TAG, message())
        }
    }

    private fun fetchCustomPromptResult(url: String): String? {
        val raw = artifactFetcher.fetchText(url) ?: return null
        val trimmed = raw.trim()
        val parsed = runCatching {
            val json = JsonParser.parseString(raw)
            if (!json.isJsonObject) return@runCatching null
            val obj = json.asJsonObject
            val array = obj.getAsJsonArray("CustomPrompt")
            val first = array?.firstOrNull()?.asJsonObject
            val result = first?.getPrimitiveString("Result")
            result?.takeIf { it.isNotBlank() }
        }.getOrNull()
        if (!parsed.isNullOrBlank()) return parsed
        // 避免在气泡展示原始 JSON
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return null
        return trimmed.takeIf { it.isNotBlank() }
    }

    private fun normalizeStageDirection(text: String?): String? {
        val raw = text?.trim().orEmpty()
        if (raw.isBlank()) return null
        val stage = "[开场寒暄：双方互致问候]"
        val hasStage = raw.contains(stage)
        val body = if (hasStage) raw.replace(stage, "").trim() else raw
        val builder = StringBuilder()
        if (hasStage) builder.appendLine(stage)
        if (body.isNotBlank()) builder.append(body.trim())
        val result = builder.toString().trim()
        return result.ifBlank { null }
    }

    private suspend fun runEnhancerIfEnabled(
        jobId: String,
        transcription: TingwuTranscription?,
        diarizedSegments: List<DiarizedSegment>,
        speakerLabels: Map<String, String>,
        fallback: String
    ): String {
        val settings = tingwuSettings.postTingwuEnhancer
        if (!settings.enabled) return fallback
        // 注意：增强仅基于转写 JSON（含时间戳/说话人），不使用 CustomPrompt 输出
        val utterances = buildEnhancerUtterances(diarizedSegments, transcription)
        if (utterances.isEmpty()) return fallback
        val output = runCatching {
            postTingwuTranscriptEnhancer.enhance(
                EnhancerInput(
                    jobId = jobId,
                    language = transcription?.language,
                    utterances = utterances
                )
            )
        }.onFailure {
            AiCoreLogger.w(TAG, "转写增强调用失败：${it.message}")
        }.getOrNull() ?: return fallback
        val lines = applyEnhancerOutput(
            utterances = utterances,
            output = output,
            baseSpeakerLabels = speakerLabels
        )
        if (lines.isEmpty()) return fallback
        return renderEnhancedMarkdown(lines)
    }

    private fun buildEnhancerUtterances(
        diarizedSegments: List<DiarizedSegment>,
        transcription: TingwuTranscription?
    ): List<EnhancerUtterance> {
        if (diarizedSegments.isNotEmpty()) {
            return diarizedSegments.sortedBy { it.startMs }.mapIndexed { index, segment ->
                EnhancerUtterance(
                    index = index,
                    startMs = segment.startMs,
                    endMs = segment.endMs,
                    speakerId = segment.speakerId,
                    text = segment.text
                )
            }
        }
        val segments = transcription?.segments.orEmpty()
        if (segments.isNotEmpty()) {
            return segments
                .sortedBy { it.start ?: 0.0 }
                .mapIndexed { index, segment ->
                    EnhancerUtterance(
                        index = index,
                        startMs = segment.start?.times(1000)?.toLong(),
                        endMs = segment.end?.times(1000)?.toLong(),
                        speakerId = segment.speaker,
                        text = segment.text?.trim().orEmpty()
                    )
                }
        }
        return emptyList()
    }



    /**
     * 仅调试/后续扩展使用，当前气泡不再拼接自定义转写或摘要。
     */
    private fun composeFinalMarkdown(
        transcriptMarkdown: String,
        artifacts: TingwuJobArtifacts?,
        resultLinks: Map<String, String>?
    ): String {
        val builder = StringBuilder()
        val normalizedTranscript = transcriptMarkdown.trim().ifBlank { "暂无可用的转写结果。" }
        builder.appendLine(normalizedTranscript).appendLine()
        val customPromptText = normalizeStageDirection(
            artifacts?.customPromptUrl?.let { fetchCustomPromptResult(it) }
        ) ?: "自定义转写暂无可用内容"
        val summarizationText = resultProcessor.fetchSummarizationText(resultLinks)
            ?: "摘要暂无可用内容"
        val chaptersText = resultProcessor.buildChaptersText(artifacts, resultLinks)
            ?: "章节暂无可用内容"

        builder.appendLine("## 自定义转写（CustomPrompt）")
        builder.appendLine(customPromptText.trim()).appendLine()

        builder.appendLine("## 摘要（Summarization）")
        builder.appendLine(summarizationText.trim()).appendLine()

        builder.appendLine("## 章节（AutoChapters）")
        builder.appendLine(chaptersText.trim()).appendLine()

        val result = builder.toString().trimEnd()
        AiCoreLogger.d(TAG, "composeFinalMarkdown 生成完成：总长度=${result.length} 包含摘要=${result.contains("摘要")} 包含章节=${result.contains("章节")}")
        return result
    }

    companion object {
        private val TAG = "${LogTags.AI_CORE}/Tingwu"
        private const val DEFAULT_LANGUAGE = "zh-CN"
        private const val DEFAULT_SOURCE_LANGUAGE = "cn"
        private const val SUBMITTED_PROGRESS = 1
        private const val CUSTOM_PROMPT_KEY = "CustomPrompt"
        private val SENTENCE_DELIMITERS = setOf('。', '？', '！', '.', '!', '?')
    }
}
