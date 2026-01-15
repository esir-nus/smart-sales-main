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
                    if (plan.batches.size > 1) {
                        pipelineTracer.emit(
                            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_UPLOAD,
                            status = "MULTI_BATCH_DELEGATION",
                            message = "Delegating to TingwuMultiBatchOrchestrator planId=${plan.disectorPlanId}"
                        )
                        
                        val audioFile = request.audioFilePath
                            ?: return@withContext Result.Error(
                                AiCoreException(
                                    source = AiCoreErrorSource.TINGWU,
                                    reason = AiCoreErrorReason.IO,
                                    message = "Multi-batch submission requires local 'audioFilePath'"
                                )
                            )

                        // Emit progress immediately so UI shows "processing" state
                        val parentJobId = "multi_${plan.disectorPlanId}"
                        val parentFlow = getOrCreateJobFlow(parentJobId)
                        parentFlow.value = TingwuJobState.InProgress(
                            jobId = parentJobId,
                            progressPercent = 5,
                            statusLabel = "MULTI_BATCH_STARTED"
                        )

                        // Launch batch processing in background (non-blocking)
                        scope.launch {
                            val result = multiBatchOrchestrator.executeBatchLoop(
                                plan = plan,
                                originalRequest = request,
                                sourceFile = audioFile,
                                submitSingleBatch = { req -> submit(req) },
                                waitForJob = { id -> waitForJob(id) },
                                onProgress = { progress ->
                                    // Update parent job progress
                                    parentFlow.value = TingwuJobState.InProgress(
                                        jobId = parentJobId,
                                        progressPercent = progress,
                                        statusLabel = "PROCESSING_BATCHES"
                                    )
                                },
                                onComplete = { _, stitchedSegments ->
                                    // Surface stitched segments to job state for UI consumption
                                    parentFlow.value = TingwuJobState.Completed(
                                        jobId = parentJobId,
                                        transcriptMarkdown = "",  // Deferred: assemble markdown from segments
                                        artifacts = TingwuJobArtifacts(
                                            recordingOriginDiarizedSegments = stitchedSegments
                                        )
                                    )
                                }
                            )
                            
                            // Handle failure explicitly
                            if (result is Result.Error) {
                                val error = result.throwable as? AiCoreException
                                    ?: AiCoreException(
                                        source = AiCoreErrorSource.TINGWU,
                                        reason = AiCoreErrorReason.UNKNOWN,
                                        message = result.throwable.message ?: "Multi-batch processing failed"
                                    )
                                parentFlow.value = TingwuJobState.Failed(
                                    jobId = parentJobId,
                                    error = error
                                )
                            }
                        }
                        
                        // Return immediately so UI can start observing
                        return@withContext Result.Success(parentJobId)
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
        val transcriptMeta = refineSpeakerLabels(
            transcriptId = jobId,
            diarizedSegments = transcriptResult.diarizedSegments.orEmpty(),
            speakerLabels = mergedArtifacts?.speakerLabels.orEmpty()
        )
        val mergedSpeakerLabels = mergeSpeakerLabels(
            base = mergedArtifacts?.speakerLabels.orEmpty(),
            incoming = transcriptMeta?.speakerMap.orEmpty()
        )
        val artifactsWithMetadata = mergedArtifacts?.copy(
            speakerLabels = mergedSpeakerLabels
        ) ?: mergedArtifacts

        upsertSessionMetadataFromTingwu(
            jobId = jobId,
            artifacts = artifactsWithMetadata,
            transcriptMeta = transcriptMeta,
            chapters = transcriptResult.chapters
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

        appendTingwuPreprocessPatch(
            jobId = jobId,
            transcriptMarkdown = transcriptResult.markdown
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

    // 将 Tingwu 转写产出的摘要/标签写入 MetaHub，标记最新分析来源。
    // V1 §6.2: Also writes chapters to M2B TranscriptMetadata with source pointers.
    private suspend fun upsertSessionMetadataFromTingwu(
        jobId: String,
        artifacts: TingwuJobArtifacts?,
        transcriptMeta: TranscriptMetadata?,
        chapters: List<TingwuChapter>?
    ) {
        val sessionId = jobContext[jobId]?.sessionId ?: return
        val request = jobContext[jobId]
        val audioAssetId = request?.ossObjectKey ?: request?.fileUrl ?: jobId
        val recordingSessionId = sessionId
        
        val smartSummary = artifacts?.smartSummary
        val chapterTitle = (artifacts?.chapters ?: chapters).orEmpty().firstOrNull()?.title
        val summary = smartSummary?.summary
            ?: transcriptMeta?.shortSummary
            ?: chapterTitle
        val titleHint = chapterTitle ?: transcriptMeta?.summaryTitle6Chars
        val mainPerson = transcriptMeta?.mainPerson
        val tags = buildSet {
            smartSummary?.keyPoints?.forEach { add(it) }
            smartSummary?.actionItems?.forEach { add(it) }
        }.filter { it.isNotBlank() }.toSet().takeIf { it.isNotEmpty() }
        val input = TingwuMetadataInput(
            sessionId = sessionId,
            callSummary = summary,
            shortTitleHint = titleHint,
            mainPersonName = mainPerson,
            tags = tags,
            completedAt = System.currentTimeMillis()
        )
        val patch = TingwuSessionMetadataMapper.toMetadataPatch(input) ?: return
        val patchWithSource = patch.copy(
            latestMajorAnalysisSource = AnalysisSource.TINGWU,
            latestMajorAnalysisAt = input.completedAt,
            lastUpdatedAt = maxOf(patch.lastUpdatedAt, input.completedAt)
        )
        val merged = runCatching { metaHub.getSession(sessionId) }.getOrNull()
            ?.mergeWith(patchWithSource) ?: patchWithSource
        runCatching {
            // Tingwu 转写完成后，将摘要等信息写入 MetaHub，标记为通话转写来源。
            metaHub.upsertSession(merged)
        }.onFailure {
            AiCoreLogger.w(TAG, "Tingwu 元数据写入 MetaHub 失败：${it.message}")
        }
        
        // V1 §6.2: Write chapters to M2B TranscriptMetadata with source pointers
        val allChapters = (artifacts?.chapters ?: chapters).orEmpty()
        if (allChapters.isNotEmpty()) {
            val chapterMetas = allChapters.mapIndexed { index, tingwuChapter ->
                com.smartsales.core.metahub.ChapterMeta(
                    title = tingwuChapter.displayTitle(),
                    startMs = tingwuChapter.startMs,
                    endMs = tingwuChapter.endMs ?: tingwuChapter.startMs,
                    summary = tingwuChapter.summary,
                    audioAssetId = audioAssetId,
                    recordingSessionId = recordingSessionId,
                    chapterId = "${jobId}_ch${index + 1}"
                )
            }
            val transcriptUpdate = TranscriptMetadata(
                transcriptId = jobId,
                sessionId = sessionId,
                source = com.smartsales.core.metahub.TranscriptSource.TINGWU,
                shortSummary = summary,
                summaryTitle6Chars = titleHint,
                mainPerson = mainPerson,
                chapters = chapterMetas
            )
            runCatching {
                metaHub.upsertTranscript(transcriptUpdate)
            }.onFailure {
                AiCoreLogger.w(TAG, "Tingwu M2B 章节写入 MetaHub 失败：${it.message}")
            }
        }
    }

    // Tingwu 转写完成后写入 M2 预处理补丁（内部派生），用于 HUD 预处理快照展示。
    private suspend fun appendTingwuPreprocessPatch(
        jobId: String,
        transcriptMarkdown: String
    ) {
        val sessionId = jobContext[jobId]?.sessionId ?: return
        val createdAt = System.currentTimeMillis()
        val traceSnapshot = tingwuTraceStore.getSnapshot()
            .takeIf { it.lastTaskId == jobId }
        val patch = TingwuPreprocessPatchBuilder.build(
            sessionId = sessionId,
            jobId = jobId,
            transcriptMarkdown = transcriptMarkdown,
            createdAt = createdAt,
            traceBatchPlan = traceSnapshot?.batchPlan,
            traceSuspiciousBoundaries = traceSnapshot?.suspiciousBoundaries
        )
        runCatching {
            metaHub.appendM2Patch(sessionId, patch)
        }.onFailure {
            AiCoreLogger.w(TAG, "Tingwu 预处理补丁写入 MetaHub 失败：${it.message}")
        }
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











    /** Helper: formats milliseconds to time string for chapter display. */
    private fun formatTimeMs(value: Long): String {
        if (value <= 0) return "00:00"
        val totalSeconds = (value / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "${timeFormatter.format(hours)}:${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
        } else {
            "${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
        }
    }


    private suspend fun refineSpeakerLabels(
        transcriptId: String,
        diarizedSegments: List<DiarizedSegment>,
        speakerLabels: Map<String, String>,
    ): TranscriptMetadata? {
        val request = TranscriptMetadataRequest(
            transcriptId = transcriptId,
            sessionId = jobContext[transcriptId]?.sessionId,
            diarizedSegments = diarizedSegments,
            speakerLabels = speakerLabels,
            force = true
        )
        return runCatching { transcriptOrchestrator.inferTranscriptMetadata(request) }
            .onFailure { AiCoreLogger.w(TAG, "写入转写元数据失败：${it.message}") }
            .getOrNull()
    }

    private fun mergeSpeakerLabels(
        base: Map<String, String>,
        incoming: Map<String, SpeakerMeta>,
        minConfidence: Float = 0.6f
    ): Map<String, String> {
        if (incoming.isEmpty()) return base
        val merged = LinkedHashMap(base)
        incoming.forEach { (id, meta) ->
            val name = meta.displayName?.takeIf { it.isNotBlank() }
            val confidence = meta.confidence ?: 0f
            if (name != null && confidence >= minConfidence) {
                merged[id] = name
            } else if (name != null && !merged.containsKey(id)) {
                merged[id] = name
            }
        }
        return merged
    }

    private fun TingwuStatusData.toArtifacts(): TingwuJobArtifacts? =
        buildArtifacts(
            mp3 = outputMp3Path,
            mp4 = outputMp4Path,
            thumb = outputThumbnailPath,
            spectrum = outputSpectrumPath,
            links = resultLinks,
            customPromptUrl = resultLinks?.get(CUSTOM_PROMPT_KEY)
        )

    private fun TingwuResultData.toArtifacts(
        fallbackArtifacts: TingwuJobArtifacts? = null,
        transcriptionUrl: String? = null,
        autoChaptersUrl: String? = null,
        customPromptUrl: String? = null,
        extraResultUrls: Map<String, String> = emptyMap(),
        chapters: List<TingwuChapter>? = null,
        smartSummary: TingwuSmartSummary? = null,
        diarizedSegments: List<DiarizedSegment>? = null,
        recordingOriginDiarizedSegments: List<DiarizedSegment>? = null,
        speakerLabels: Map<String, String> = emptyMap(),
    ): TingwuJobArtifacts? =
        buildArtifacts(
            mp3 = outputMp3Path ?: fallbackArtifacts?.outputMp3Path,
            mp4 = outputMp4Path ?: fallbackArtifacts?.outputMp4Path,
            thumb = outputThumbnailPath ?: fallbackArtifacts?.outputThumbnailPath,
            spectrum = outputSpectrumPath ?: fallbackArtifacts?.outputSpectrumPath,
            links = resultLinks ?: fallbackArtifacts?.resultLinks?.associate { it.label to it.url },
            transcriptionUrl = transcriptionUrl ?: fallbackArtifacts?.transcriptionUrl,
            autoChaptersUrl = autoChaptersUrl ?: fallbackArtifacts?.autoChaptersUrl,
            customPromptUrl = customPromptUrl ?: fallbackArtifacts?.customPromptUrl,
            extraResultUrls = if (extraResultUrls.isNotEmpty()) extraResultUrls else fallbackArtifacts?.extraResultUrls.orEmpty(),
            chapters = chapters ?: fallbackArtifacts?.chapters,
            smartSummary = smartSummary ?: fallbackArtifacts?.smartSummary,
            diarizedSegments = diarizedSegments ?: fallbackArtifacts?.diarizedSegments,
            recordingOriginDiarizedSegments = recordingOriginDiarizedSegments
                ?: fallbackArtifacts?.recordingOriginDiarizedSegments,
            speakerLabels = if (speakerLabels.isNotEmpty()) speakerLabels else fallbackArtifacts?.speakerLabels.orEmpty(),
        )

    private fun buildArtifacts(
        mp3: String?,
        mp4: String?,
        thumb: String?,
        spectrum: String?,
        links: Map<String, String>?,
        transcriptionUrl: String? = null,
        autoChaptersUrl: String? = null,
        customPromptUrl: String? = null,
        extraResultUrls: Map<String, String> = emptyMap(),
        chapters: List<TingwuChapter>? = null,
        smartSummary: TingwuSmartSummary? = null,
        diarizedSegments: List<DiarizedSegment>? = null,
        recordingOriginDiarizedSegments: List<DiarizedSegment>? = null,
        speakerLabels: Map<String, String> = emptyMap(),
    ): TingwuJobArtifacts? {
        if (
            mp3.isNullOrBlank() &&
            mp4.isNullOrBlank() &&
            thumb.isNullOrBlank() &&
            spectrum.isNullOrBlank() &&
            links.isNullOrEmpty() &&
            transcriptionUrl.isNullOrBlank() &&
            autoChaptersUrl.isNullOrBlank() &&
            customPromptUrl.isNullOrBlank() &&
            extraResultUrls.isEmpty() &&
            chapters.isNullOrEmpty() &&
            smartSummary == null &&
            diarizedSegments.isNullOrEmpty() &&
            recordingOriginDiarizedSegments.isNullOrEmpty() &&
            speakerLabels.isEmpty()
        ) {
            return null
        }
        val resultLinks = links.orEmpty()
            .mapNotNull { (label, url) ->
                url.takeIf { it.isNotBlank() }?.let { TingwuResultLink(label, it) }
            }
        return TingwuJobArtifacts(
            outputMp3Path = mp3,
            outputMp4Path = mp4,
            outputThumbnailPath = thumb,
            outputSpectrumPath = spectrum,
            resultLinks = resultLinks,
            transcriptionUrl = transcriptionUrl,
            autoChaptersUrl = autoChaptersUrl,
            customPromptUrl = customPromptUrl ?: links?.get(CUSTOM_PROMPT_KEY),
            extraResultUrls = extraResultUrls,
            chapters = chapters,
            smartSummary = smartSummary,
            diarizedSegments = diarizedSegments,
            recordingOriginDiarizedSegments = recordingOriginDiarizedSegments,
            speakerLabels = speakerLabels,
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

    private fun fetchSummarizationText(resultLinks: Map<String, String>?): String? {
        val url = resultLinks?.entries
            ?.firstOrNull { it.key.equals("Summarization", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val raw = artifactFetcher.fetchText(url) ?: return null
        val parsed = runCatching {
            val json = JsonParser.parseString(raw)
            if (!json.isJsonObject) return@runCatching null
            val obj = json.asJsonObject
            val summaryObj = obj.getAsJsonObject("Summarization") ?: obj
            val paragraphTitle = summaryObj.getPrimitiveString("ParagraphTitle")
            val paragraphSummary = summaryObj.getPrimitiveString("ParagraphSummary")
            val conversational = summaryObj.getAsJsonArray("ConversationalSummary")
                ?.mapNotNull { element ->
                    element.asJsonObjectOrNull()?.let { item ->
                        val speaker = item.getPrimitiveString("SpeakerName")
                            ?: item.getPrimitiveString("SpeakerId")
                        val summary = item.getPrimitiveString("Summary")
                        if (summary.isNullOrBlank()) null else speaker to summary
                    }
                }.orEmpty()
            val qa = summaryObj.getAsJsonArray("QuestionsAnsweringSummary")
                ?.mapNotNull { element ->
                    element.asJsonObjectOrNull()?.let { item ->
                        val q = item.getPrimitiveString("Question")?.takeIf { it.isNotBlank() }
                        val a = item.getPrimitiveString("Answer")?.takeIf { it.isNotBlank() }
                        if (q == null && a == null) null else q to a
                    }
                }.orEmpty()
            if (
                paragraphTitle.isNullOrBlank() &&
                paragraphSummary.isNullOrBlank() &&
                conversational.isEmpty() &&
                qa.isEmpty()
            ) null
            else {
                buildString {
                    if (!paragraphTitle.isNullOrBlank() || !paragraphSummary.isNullOrBlank()) {
                        appendLine("### 段落摘要")
                        paragraphTitle?.let { appendLine("**$it**") }
                        paragraphSummary?.let { appendLine(it) }
                        appendLine()
                    }
                    if (conversational.isNotEmpty()) {
                        appendLine("### 发言人总结")
                        conversational.forEach { (speaker, summary) ->
                            val label = speaker ?: "说话人"
                            appendLine("- **$label**：$summary")
                        }
                        appendLine()
                    }
                    if (qa.isNotEmpty()) {
                        appendLine("### 问答回顾")
                        qa.forEach { (q, a) ->
                            q?.let { appendLine("- **Q：** $it") }
                            a?.let { appendLine("  **A：** $it") }
                        }
                        appendLine()
                    }
                }.trim()
            }
        }.getOrNull()
        return parsed
    }

    private fun buildChaptersText(
        artifacts: TingwuJobArtifacts?,
        resultLinks: Map<String, String>?
    ): String? {
        val parsedChapters = artifacts?.chapters
        val chapters = if (!parsedChapters.isNullOrEmpty()) {
            parsedChapters.map {
                ChapterDisplay(
                    startMs = it.startMs,
                    headline = it.displayTitle(),
                    summary = it.summary
                )
            }
        } else {
            fetchAutoChapters(resultLinks)
        }
        if (chapters.isNullOrEmpty()) return null
        return buildString {
            chapters.forEach { chapter ->
                val start = formatTimeMs(chapter.startMs ?: 0)
                appendLine("- [$start] ${chapter.headline}")
                chapter.summary?.takeIf { it.isNotBlank() }?.let { appendLine("  - $it") }
            }
        }.trim()
    }

    private fun fetchAutoChapters(resultLinks: Map<String, String>?): List<ChapterDisplay>? {
        val url = resultLinks?.entries
            ?.firstOrNull { it.key.equals("AutoChapters", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val raw = artifactFetcher.fetchText(url) ?: return null
        return runCatching {
            val root = JsonParser.parseString(raw)
            val arr = when {
                root.isJsonObject -> root.asJsonObject.getAsJsonArray("AutoChapters")
                root.isJsonArray -> root.asJsonArray
                else -> null
            } ?: return@runCatching null
            arr.mapNotNull { element ->
                val obj = element.asJsonObjectOrNull() ?: return@mapNotNull null
                val headline = obj.getPrimitiveString("Headline") ?: obj.getPrimitiveString("Title")
                val summary = obj.getPrimitiveString("Summary")
                val start = obj.get("Start")?.asLongOrNull()
                    ?: obj.get("StartTime")?.asLongOrNull()
                    ?: obj.get("StartMs")?.asLongOrNull()
                if (headline.isNullOrBlank()) null
                else ChapterDisplay(startMs = start ?: 0, headline = headline, summary = summary)
            }.takeIf { it.isNotEmpty() }
        }.getOrNull()
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

    private data class ChapterDisplay(
        val startMs: Long?,
        val headline: String,
        val summary: String?
    )



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
        val summarizationText = fetchSummarizationText(resultLinks)
            ?: "摘要暂无可用内容"
        val chaptersText = buildChaptersText(artifacts, resultLinks)
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

internal fun buildRecordingOriginSegmentsWithOffset(
    transcription: TingwuTranscription?,
    baseOffsetMs: Long,
    shouldMerge: (DiarizedSegment, DiarizedSegment) -> Boolean
): List<DiarizedSegment> {
    if (transcription == null) return emptyList()
    val segments = transcription.segments.orEmpty()
    val hasUsableSegments = segments.any { !it.text.isNullOrBlank() && !it.speaker.isNullOrBlank() }
    if (!hasUsableSegments) return emptyList()

    val speakerOrder = LinkedHashMap<String, Int>()
    transcription.speakers?.forEachIndexed { index, speaker ->
        speakerOrder[speaker.id] = index + 1
    }
    var nextIndex = speakerOrder.size + 1
    fun resolveSpeaker(idRaw: String): Pair<String, Int> {
        val key = idRaw.trim()
        val idx = speakerOrder.getOrPut(key) { nextIndex++ }
        return key to idx
    }
    val sortedSegments = transcription.segments.orEmpty()
        .filter { !it.text.isNullOrBlank() && !it.speaker.isNullOrBlank() }
        .sortedBy { it.start ?: 0.0 }
    if (sortedSegments.isEmpty()) {
        return emptyList()
    }

    // 说明：baseOffsetMs 用于 per-window 切片锚定；录音起点绝对时间 = baseOffsetMs + 相对时间。
    val diarized = sortedSegments.map { segment ->
        val (speakerId, speakerIndex) = resolveSpeaker(segment.speaker!!)
        val rawStart = segment.start ?: 0.0
        val rawEnd = segment.end ?: segment.start ?: 0.0
        val startMs = (rawStart * 1000).toLong() + baseOffsetMs
        val endMs = (rawEnd * 1000).toLong() + baseOffsetMs
        val normalizedStart = max(startMs, 0)
        val normalizedEnd = max(endMs, 0)
        val safeEnd = if (normalizedEnd >= normalizedStart) normalizedEnd else normalizedStart
        DiarizedSegment(
            speakerId = speakerId,
            speakerIndex = speakerIndex,
            startMs = normalizedStart,
            endMs = safeEnd,
            text = segment.text?.trim().orEmpty()
        )
    }
    val merged = mutableListOf<DiarizedSegment>()
    diarized.forEach { segment ->
        val last = merged.lastOrNull()
        if (last != null && shouldMerge(last, segment)) {
            val combined = last.copy(
                endMs = max(last.endMs, segment.endMs),
                text = (last.text + " " + segment.text).trim()
            )
            merged[merged.lastIndex] = combined
        } else {
            merged += segment
        }
    }
    return merged
}

internal fun parseAutoChaptersPayload(json: String): List<TingwuChapter> {
    val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return emptyList()
    val array: JsonArray? = when {
        root.isJsonArray -> root.asJsonArray
        root.isJsonObject -> {
            val obj = root.asJsonObject
            obj.getAsJsonArray("AutoChapters")
                ?: obj.getAsJsonArray("Chapters")
                ?: obj.getAsJsonArray("chapters")
                ?: obj.getAsJsonArray("Items")
                ?: obj.getAsJsonArray("items")
        }
        else -> null
    }
    val target = array ?: return emptyList()
    return target.mapNotNull { element ->
        if (!element.isJsonObject) return@mapNotNull null
        val obj = element.asJsonObject
        val headline = obj.getPrimitiveString("Headline")
        val title = obj.getPrimitiveString("Title")
            ?: obj.getPrimitiveString("title")
            ?: obj.getPrimitiveString("Name")
            ?: obj.getPrimitiveString("name")
        val summary = obj.getPrimitiveString("Summary")
        val startRaw = obj.getPrimitiveNumber("Start")
            ?: obj.getPrimitiveNumber("StartTime")
            ?: obj.getPrimitiveNumber("StartMs")
        val endRaw = obj.getPrimitiveNumber("End")
            ?: obj.getPrimitiveNumber("EndTime")
            ?: obj.getPrimitiveNumber("EndMs")
        val startMs = startRaw?.let { toMillis(it) }
        val endMs = endRaw?.let { toMillis(it) }
        val displayTitle = headline ?: title
        if (displayTitle.isNullOrBlank() || startMs == null) return@mapNotNull null
        TingwuChapter(
            title = displayTitle,
            startMs = startMs,
            endMs = endMs,
            headline = headline,
            summary = summary
        )
    }
}

private fun JsonObject.getPrimitiveString(key: String): String? =
    this.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

private fun JsonObject.getPrimitiveNumber(key: String): Number? =
    this.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asNumber

private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
    takeIf { it.isJsonObject }?.asJsonObject

private fun JsonElement.asLongOrNull(): Long? {
    return runCatching {
        when {
            isJsonPrimitive && this.asJsonPrimitive.isNumber -> this.asLong
            isJsonPrimitive && this.asJsonPrimitive.isString -> this.asJsonPrimitive.asString.toLongOrNull()
            else -> null
        }
    }.getOrNull()
}

private fun toMillis(number: Number): Long {
    val value = number.toDouble()
    return if (value > 100000) value.toLong() else (value * 1000).toLong()
}

internal fun parseSmartSummaryPayload(json: String): TingwuSmartSummary? {
    val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return null
    if (!root.isJsonObject) return null
    val obj = root.asJsonObject
    val summary = obj.getPrimitiveString("Summary")
        ?: obj.getPrimitiveString("Abstract")
        ?: obj.getPrimitiveString("Summarization")
    val keyPoints = obj.getAsJsonArray("KeyPoints")
        ?: obj.getAsJsonArray("Highlights")
        ?: obj.getAsJsonArray("Keypoints")
    val actionItems = obj.getAsJsonArray("ActionItems")
        ?: obj.getAsJsonArray("Todos")
        ?: obj.getAsJsonArray("Tasks")
    val keys = keyPoints?.mapNotNull { it.asStringOrNull() } ?: emptyList()
    val actions = actionItems?.mapNotNull { it.asStringOrNull() } ?: emptyList()
    if (summary.isNullOrBlank() && keys.isEmpty() && actions.isEmpty()) return null
    return TingwuSmartSummary(
        summary = summary,
        keyPoints = keys,
        actionItems = actions
    )
}

private fun JsonElement.asStringOrNull(): String? =
    takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
