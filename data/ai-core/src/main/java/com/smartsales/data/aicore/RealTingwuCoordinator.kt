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
import com.smartsales.data.aicore.tingwu.TingwuApi
import com.smartsales.data.aicore.tingwu.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.TingwuResultData
import com.smartsales.data.aicore.tingwu.TingwuResultResponse
import com.smartsales.data.aicore.tingwu.TingwuStatusData
import com.smartsales.data.aicore.tingwu.TingwuStatusResponse
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.tingwu.TingwuTaskInput
import com.smartsales.data.aicore.tingwu.TingwuTaskParameters
import com.smartsales.data.aicore.tingwu.TingwuTranscription
import com.smartsales.data.aicore.tingwu.TingwuTranscriptSegment
import com.smartsales.data.aicore.tingwu.TingwuTranscriptionParameters
import com.smartsales.data.aicore.tingwu.TingwuDiarizationParameters
import com.smartsales.data.aicore.tingwu.TingwuSummarizationParameters
import com.smartsales.data.aicore.tingwu.TingwuTranscodingParameters
import com.smartsales.data.aicore.TranscriptMetadataRequest
import com.smartsales.data.aicore.tingwu.TingwuCustomPrompt
import com.smartsales.data.aicore.tingwu.TingwuCustomPromptContent
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.tingwu.TingwuArtifactFetcher
import com.smartsales.data.aicore.posttingwu.EnhancerInput
import com.smartsales.data.aicore.posttingwu.EnhancerUtterance
import com.smartsales.data.aicore.posttingwu.PostTingwuTranscriptEnhancer
import com.smartsales.data.aicore.posttingwu.applyEnhancerOutput
import com.smartsales.data.aicore.posttingwu.renderEnhancedMarkdown
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URL
import java.text.DecimalFormat
import java.util.Locale
import java.util.LinkedHashMap
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.net.ssl.SSLException
// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt
// 模块：:data:ai-core
// 说明：通过真实 Tingwu HTTP API 提交、轮询并拉取转写结果
// 作者：创建于 2025-11-16
@Singleton
class RealTingwuCoordinator @Inject constructor(
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

    override fun observeJob(jobId: String): Flow<TingwuJobState> =
        getOrCreateJobFlow(jobId).asStateFlow()

    private fun getOrCreateJobFlow(jobId: String): MutableStateFlow<TingwuJobState> =
        jobStates.getOrPut(jobId) { MutableStateFlow<TingwuJobState>(TingwuJobState.Idle) }

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
                        "SUCCEEDED", "COMPLETED", "FINISHED" -> {
                            AiCoreLogger.d(TAG, "任务状态为 COMPLETED，开始拉取转写结果：jobId=$jobId")
                            logVerbose { "状态完成，准备拉取结果：jobId=$jobId" }
                            if (data.resultLinks != null) {
                                AiCoreLogger.d(TAG, "Result 字段可用，包含 ${data.resultLinks.size} 个键")
                            } else {
                                AiCoreLogger.w(TAG, "Result 字段为空，将尝试 /transcription 接口")
                            }
                    val transcriptResult = try {
                        fetchTranscript(
                            jobId = jobId,
                            resultLinks = data.resultLinks,
                            fallbackArtifacts = artifacts
                        )
                    } catch (error: Throwable) {
                        if (error is CancellationException) throw error
                        val mapped = mapError(error)
                        AiCoreLogger.e(TAG, "拉取转写结果失败：${mapped.message}", mapped)
                        flow.value = TingwuJobState.Failed(jobId, mapped)
                        return@launch
                    }
                    val mergedArtifacts = (transcriptResult.artifacts ?: artifacts)?.copy(
                        chapters = transcriptResult.chapters ?: transcriptResult.artifacts?.chapters
                            ?: artifacts?.chapters,
                        autoChaptersUrl = transcriptResult.artifacts?.autoChaptersUrl
                            ?: artifacts?.autoChaptersUrl
                            ) ?: artifacts ?: transcriptResult.chapters?.let {
                                TingwuJobArtifacts(
                                    autoChaptersUrl = extractAutoChaptersUrl(data.resultLinks),
                                    chapters = it,
                                    customPromptUrl = data.resultLinks?.get(CUSTOM_PROMPT_KEY)
                                )
                            }
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
                            AiCoreLogger.d(TAG, "转写结果拉取成功：jobId=$jobId markdown长度=${transcriptResult.markdown.length}")
                            flow.value = TingwuJobState.Completed(
                                jobId = jobId,
                                transcriptMarkdown = transcriptResult.markdown,
                                artifacts = artifactsWithMetadata,
                                statusLabel = normalizedStatus
                            )
                            false
                        }
                        else -> {
                            val progress = inferProgress(data)
                            flow.value = TingwuJobState.InProgress(
                                jobId,
                                progress,
                                statusLabel = normalizedStatus,
                                artifacts = artifacts
                            )
                            true
                        }
                    }
                    if (!shouldContinue) {
                        break
                    }
                    delay(pollInterval)
                }
            } finally {
                pollingJobs.remove(jobId)
                jobContext.remove(jobId)
            }
        }
        pollingJobs[jobId] = job
    }

    // 将 Tingwu 转写产出的摘要/标签写入 MetaHub，标记最新分析来源。
    private suspend fun upsertSessionMetadataFromTingwu(
        jobId: String,
        artifacts: TingwuJobArtifacts?,
        transcriptMeta: TranscriptMetadata?,
        chapters: List<TingwuChapter>?
    ) {
        val sessionId = jobContext[jobId]?.sessionId ?: return
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
    }

    private suspend fun fetchTranscript(
        jobId: String,
        resultLinks: Map<String, String>?,
        fallbackArtifacts: TingwuJobArtifacts?
    ): TranscriptResult = withContext(dispatchers.io) {
        AiCoreLogger.d(TAG, "开始拉取转写结果：jobId=$jobId")
        logVerbose { "拉取转写：jobId=$jobId resultLinks=${resultLinks?.size ?: 0} 个键" }
        val chaptersUrl = extractAutoChaptersUrl(resultLinks) ?: fallbackArtifacts?.autoChaptersUrl
        // First try the /transcription endpoint
        AiCoreLogger.d(TAG, "尝试使用 /transcription 接口：jobId=$jobId")
        val inline = runCatching { api.getTaskResult(taskId = jobId) }.fold(
            onSuccess = { response ->
                runCatching {
                    // 重要：仅记录转写结果响应体（copy-only），不在 UI 内联展示。
                    tingwuTraceStore.record(
                        taskId = jobId,
                        transcriptionJson = gson.toJson(response)
                    )
                }
                val data = requireData(
                    code = response.code,
                    message = response.message,
                    requestId = response.requestId,
                    data = response.data,
                    action = "GetTranscription"
                )
                if (data.transcription == null) {
                    AiCoreLogger.w(TAG, "/transcription 接口响应缺少 transcription 字段，尝试 Result.Transcription 链接：jobId=$jobId")
                    logVerbose { "GetTranscription 响应缺少正文，尝试 Result.Transcription 链接" }
                    null
                } else {
                    val textLength = data.transcription.text?.length ?: 0
                    val segmentsCount = data.transcription.segments?.size ?: 0
                    AiCoreLogger.d(TAG, "/transcription 接口成功：jobId=$jobId text长度=$textLength segments=$segmentsCount requestId=${response.requestId}")
                    logVerbose {
                        "拉取成功：jobId=$jobId text=$textLength requestId=${response.requestId}"
                    }
                    val transcription = data.transcription
                    val diarizedSegments = buildDiarizedSegments(transcription)
                    val speakerLabels = buildSpeakerLabels(transcription, diarizedSegments)
                    val markdown = buildMarkdown(transcription, diarizedSegments, speakerLabels)
                    val enhancedMarkdown = runEnhancerIfEnabled(
                        jobId = jobId,
                        transcription = transcription,
                        diarizedSegments = diarizedSegments,
                        speakerLabels = speakerLabels,
                        fallback = markdown
                    )
                    AiCoreLogger.d(TAG, "转写 markdown 生成成功：jobId=$jobId markdown长度=${markdown.length}")
                    val chapters = chaptersUrl?.let { fetchChaptersSafe(it, jobId) }
                    val artifacts = data.toArtifacts(
                        transcriptionUrl = transcription.url,
                        autoChaptersUrl = extractAutoChaptersUrl(data.resultLinks),
                        customPromptUrl = data.resultLinks?.get(CUSTOM_PROMPT_KEY),
                        extraResultUrls = data.resultLinks.orEmpty(),
                        chapters = chapters,
                        smartSummary = fetchSmartSummarySafe(data.resultLinks, jobId),
                        diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() },
                        speakerLabels = speakerLabels
                    ) ?: fallbackArtifacts?.copy(
                        chapters = chapters ?: fallbackArtifacts.chapters,
                        smartSummary = fallbackArtifacts.smartSummary,
                        diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() }
                            ?: fallbackArtifacts.diarizedSegments,
                        speakerLabels = if (speakerLabels.isNotEmpty()) speakerLabels else fallbackArtifacts.speakerLabels
                    )
                    TranscriptResult(
                        markdown = enhancedMarkdown,
                        artifacts = artifacts,
                        chapters = chapters,
                        diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() }
                    )
                }
            },
            onFailure = { error ->
                if (error is HttpException && error.code() == 404) {
                    AiCoreLogger.w(TAG, "官方 /transcription 接口不可用，改用 Result.Transcription 链接")
                    null
                } else {
                    val mapped = mapError(error)
                    AiCoreLogger.e(TAG, "拉取 Tingwu 结果失败：${mapped.message}", mapped)
                    throw mapped
                }
            }
        )
        if (inline != null) {
            AiCoreLogger.d(TAG, "使用内联转写结果（/transcription 接口）")
            return@withContext inline
        }
        AiCoreLogger.d(TAG, "内联结果不可用，尝试从 Result.Transcription URL 下载")
        val signedUrl = extractTranscriptionUrl(resultLinks)
            ?: throw AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.REMOTE,
                message = "Result.Transcription 链接缺失",
                suggestion = "请在 Tingwu 控制台确认任务开启了转写输出。Result 字段可能为空或缺少 Transcription 键。"
            )
        AiCoreLogger.d(TAG, "开始从 URL 下载转写 JSON：jobId=$jobId")
        val transcription = downloadTranscription(signedUrl, jobId)
        val diarizedSegments = buildDiarizedSegments(transcription)
        val speakerLabels = buildSpeakerLabels(transcription, diarizedSegments)
        val markdown = buildMarkdown(transcription, diarizedSegments, speakerLabels)
        val enhancedMarkdown = runEnhancerIfEnabled(
            jobId = jobId,
            transcription = transcription,
            diarizedSegments = diarizedSegments,
            speakerLabels = speakerLabels,
            fallback = markdown
        )
        AiCoreLogger.d(TAG, "转写 JSON 解析成功：jobId=$jobId markdown长度=${markdown.length}")
        val chapters = chaptersUrl?.let { fetchChaptersSafe(it, jobId) }
        val smartSummary = fetchSmartSummarySafe(resultLinks, jobId)
        TranscriptResult(
            markdown = enhancedMarkdown,
            artifacts = fallbackArtifacts?.copy(
                transcriptionUrl = signedUrl,
                autoChaptersUrl = fallbackArtifacts.autoChaptersUrl,
                customPromptUrl = resultLinks?.get(CUSTOM_PROMPT_KEY) ?: fallbackArtifacts.customPromptUrl,
                extraResultUrls = fallbackArtifacts.extraResultUrls,
                chapters = chapters ?: fallbackArtifacts.chapters,
                smartSummary = smartSummary ?: fallbackArtifacts.smartSummary,
                diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() }
                    ?: fallbackArtifacts.diarizedSegments,
                speakerLabels = if (speakerLabels.isNotEmpty()) speakerLabels else fallbackArtifacts.speakerLabels
            ) ?: fallbackArtifacts,
            chapters = chapters,
            diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() }
        )
    }

    private fun extractTranscriptionUrl(resultLinks: Map<String, String>?): String? {
        if (resultLinks == null) {
            AiCoreLogger.w(TAG, "extractTranscriptionUrl: resultLinks 为 null")
            return null
        }
        if (resultLinks.isEmpty()) {
            AiCoreLogger.w(TAG, "extractTranscriptionUrl: resultLinks 为空")
            return null
        }
        val availableKeys = resultLinks.keys.joinToString(", ")
        AiCoreLogger.d(TAG, "extractTranscriptionUrl: 可用键 [$availableKeys]")
        val transcriptionUrl = resultLinks
            .entries
            .firstOrNull { it.key.equals("Transcription", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotBlank() }
        if (transcriptionUrl != null) {
            AiCoreLogger.d(TAG, "extractTranscriptionUrl: 找到 Transcription URL (长度=${transcriptionUrl.length})")
            logVerbose { "Transcription URL: $transcriptionUrl" }
        } else {
            AiCoreLogger.w(TAG, "extractTranscriptionUrl: 未找到 Transcription 键，可用键: [$availableKeys]")
        }
        return transcriptionUrl
    }

    private fun extractAutoChaptersUrl(resultLinks: Map<String, String>?): String? {
        if (resultLinks.isNullOrEmpty()) return null
        return resultLinks.entries.firstOrNull { it.key.equals("AutoChapters", ignoreCase = true) }?.value
    }

    private fun extractSmartSummaryUrl(resultLinks: Map<String, String>?): String? {
        if (resultLinks.isNullOrEmpty()) return null
        val keys = listOf("MeetingAssistance", "Summarization", "SmartSummary", "Summary")
        return resultLinks.entries.firstOrNull { entry ->
            keys.any { key -> entry.key.equals(key, ignoreCase = true) }
        }?.value
    }

    private fun fetchChaptersSafe(url: String, jobId: String): List<TingwuChapter>? =
        runCatching { downloadChapters(url, jobId) }.onFailure {
            AiCoreLogger.w(TAG, "下载章节失败，将忽略：jobId=$jobId url=${url.take(80)} error=${it.message}")
        }.getOrNull()

    private fun downloadTranscription(url: String, jobId: String): TingwuTranscription {
        AiCoreLogger.d(TAG, "开始下载转写 JSON：jobId=$jobId url=${url.take(100)}...")
        return try {
            val connection = URL(url).openConnection().apply {
                connectTimeout = config.tingwuReadTimeoutMillis.toInt()
                readTimeout = config.tingwuReadTimeoutMillis.toInt()
            }
            AiCoreLogger.d(TAG, "连接已建立，开始读取响应：jobId=$jobId")
            connection.getInputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                val payload = reader.readText()
                AiCoreLogger.d(TAG, "下载完成：jobId=$jobId payload大小=${payload.length} 字符")
                logVerbose { "下载转写 payload 前200字符：${payload.take(200)}" }
                runCatching {
                    // 重要：仅保存原始转写 JSON（copy-only），HUD 不内联展示。
                    tingwuTraceStore.record(
                        taskId = jobId,
                        transcriptionJson = payload
                    )
                }
                val parsed = parseDownloadedTranscription(payload, jobId)
                parsed ?: throw AiCoreException(
                    source = AiCoreErrorSource.TINGWU,
                    reason = AiCoreErrorReason.IO,
                    message = "无法解析 Tingwu 转写结果",
                    suggestion = "请检查 Result.Transcription 内容是否符合官方格式。Payload 前200字符：${payload.take(200)}"
                )
            }
        } catch (io: IOException) {
            AiCoreLogger.e(TAG, "下载转写 JSON 失败：jobId=$jobId error=${io.message}", io)
            throw AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.NETWORK,
                message = "下载 Tingwu 转写结果失败：${io.message}",
                suggestion = "确认 Result.Transcription 链接仍在有效期内，或检查网络连接",
                cause = io
            )
        }
    }

    private fun downloadChapters(url: String, jobId: String): List<TingwuChapter> {
        AiCoreLogger.d(TAG, "开始下载章节 JSON：jobId=$jobId url=${url.take(100)}...")
        return try {
            val connection = URL(url).openConnection().apply {
                connectTimeout = config.tingwuReadTimeoutMillis.toInt()
                readTimeout = config.tingwuReadTimeoutMillis.toInt()
            }
            connection.getInputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                val payload = reader.readText()
                logVerbose { "章节 JSON 前 200 字符：${payload.take(200)}" }
                val chapters = parseAutoChaptersPayload(payload)
                if (chapters.isEmpty()) {
                    AiCoreLogger.w(TAG, "章节 JSON 未解析出有效章节：jobId=$jobId")
                } else {
                    AiCoreLogger.d(TAG, "章节解析成功：jobId=$jobId 数量=${chapters.size}")
                }
                chapters
            }
        } catch (io: IOException) {
            AiCoreLogger.e(TAG, "下载章节 JSON 失败：jobId=$jobId error=${io.message}", io)
            emptyList()
        }
    }

    private fun fetchSmartSummarySafe(resultLinks: Map<String, String>?, jobId: String): TingwuSmartSummary? {
        val url = extractSmartSummaryUrl(resultLinks) ?: return null
        return runCatching { downloadSmartSummary(url, jobId) }.onFailure {
            AiCoreLogger.w(TAG, "下载智能总结失败，将忽略：jobId=$jobId url=${url.take(80)} error=${it.message}")
        }.getOrNull()
    }

    private fun downloadSmartSummary(url: String, jobId: String): TingwuSmartSummary? {
        AiCoreLogger.d(TAG, "开始下载智能总结 JSON：jobId=$jobId url=${url.take(100)}...")
        return try {
            val connection = URL(url).openConnection().apply {
                connectTimeout = config.tingwuReadTimeoutMillis.toInt()
                readTimeout = config.tingwuReadTimeoutMillis.toInt()
            }
            connection.getInputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                val payload = reader.readText()
                logVerbose { "智能总结 JSON 前 200 字符：${payload.take(200)}" }
                parseSmartSummaryPayload(payload)
            }
        } catch (io: IOException) {
            AiCoreLogger.e(TAG, "下载智能总结失败：jobId=$jobId error=${io.message}", io)
            null
        }
    }

    private fun parseDownloadedTranscription(json: String, jobId: String): TingwuTranscription? {
        AiCoreLogger.d(TAG, "开始解析转写 JSON：jobId=$jobId")
        // Try parsing as TingwuResultResponse first
        val response = runCatching { gson.fromJson(json, TingwuResultResponse::class.java) }.getOrNull()
        response?.data?.transcription?.let { transcription ->
            val hasContent = !transcription.text.isNullOrBlank() || !transcription.segments.isNullOrEmpty()
            AiCoreLogger.d(
                TAG,
                "解析成功（TingwuResultResponse 格式）：jobId=$jobId text长度=${transcription.text?.length ?: 0} segments=${transcription.segments?.size ?: 0}"
            )
            if (hasContent) {
                return transcription
            } else {
                AiCoreLogger.d(TAG, "TingwuResultResponse 内容为空，继续尝试其他格式：jobId=$jobId")
            }
        }
        // Try parsing as direct TingwuTranscription
        runCatching { gson.fromJson(json, TingwuTranscription::class.java) }
            .getOrNull()
            ?.let { transcription ->
                val hasContent = !transcription.text.isNullOrBlank() || !transcription.segments.isNullOrEmpty()
                AiCoreLogger.d(
                    TAG,
                    "解析成功（TingwuTranscription 格式）：jobId=$jobId text长度=${transcription.text?.length ?: 0} segments=${transcription.segments?.size ?: 0}"
                )
                if (hasContent) {
                    return transcription
                } else {
                    AiCoreLogger.d(TAG, "TingwuTranscription 内容为空，继续尝试 Paragraph 格式：jobId=$jobId")
                }
            }
        // Try parsing as legacy format
        val legacy = runCatching { gson.fromJson(json, LegacyTranscriptionResponse::class.java) }.getOrNull()
        legacy?.transcription?.toOfficial()?.let { transcription ->
            val hasContent = !transcription.text.isNullOrBlank() || !transcription.segments.isNullOrEmpty()
            AiCoreLogger.d(
                TAG,
                "解析成功（Legacy 格式）：jobId=$jobId text长度=${transcription.text?.length ?: 0} segments=${transcription.segments?.size ?: 0}"
            )
            if (hasContent) {
                return transcription
            } else {
                AiCoreLogger.d(TAG, "Legacy 内容为空，继续尝试 Paragraph 格式：jobId=$jobId")
            }
        }
        // Try parsing as paragraph style
        AiCoreLogger.d(TAG, "尝试解析为 Paragraph 格式：jobId=$jobId")
        val paragraphResult = parseParagraphStyle(json, jobId)
        if (paragraphResult != null) {
            AiCoreLogger.d(TAG, "解析成功（Paragraph 格式）：jobId=$jobId text长度=${paragraphResult.text?.length ?: 0} segments=${paragraphResult.segments?.size ?: 0}")
        } else {
            AiCoreLogger.w(TAG, "所有解析格式均失败：jobId=$jobId JSON前200字符：${json.take(200)}")
        }
        return paragraphResult
    }

    private fun parseParagraphStyle(rawJson: String, jobId: String): TingwuTranscription? {
        val root = runCatching { JsonParser.parseString(rawJson).asJsonObject }.getOrNull() ?: run {
            AiCoreLogger.w(TAG, "无法解析 JSON 根节点：jobId=$jobId")
            return null
        }
        val dataNode = root.getAsJsonObject("Data") ?: root
        val transcriptionNode = dataNode.getAsJsonObject("Transcription")
            ?: dataNode.getAsJsonObject("transcription")
            ?: run {
                AiCoreLogger.w(TAG, "Paragraph JSON 缺少 Transcription 对象：jobId=$jobId JSON前160字符：${rawJson.take(160)}")
                return null
            }
        val paragraphsElement = transcriptionNode.get("Paragraphs") ?: run {
            AiCoreLogger.w(TAG, "Paragraph JSON 缺少 Paragraphs 字段：jobId=$jobId JSON前160字符：${rawJson.take(160)}")
            return null
        }
        val paragraphsArray = when {
            paragraphsElement.isJsonArray -> paragraphsElement.asJsonArray
            paragraphsElement.isJsonPrimitive && paragraphsElement.asJsonPrimitive.isString -> runCatching {
                JsonParser.parseString(paragraphsElement.asString).asJsonArray
            }.getOrNull()
            else -> null
        } ?: run {
            AiCoreLogger.w(TAG, "Paragraphs 字段解析失败：jobId=$jobId 元素=${paragraphsElement}")
            return null
        }

        val sentenceSegments = mutableListOf<TingwuTranscriptSegment>()
        var fallbackSentenceId = 0
        paragraphsArray.forEachIndexed { paragraphIndex, paragraphElement ->
            val paragraphObj = paragraphElement.asJsonObject
            val paragraphSpeaker = paragraphObj.get("SpeakerId")?.asString
            val wordsElement = paragraphObj.get("Words") ?: run {
                AiCoreLogger.w(TAG, "Paragraph[$paragraphIndex] 缺少 Words：$paragraphObj")
                return@forEachIndexed
            }
            val wordsArray = when {
                wordsElement.isJsonArray -> wordsElement.asJsonArray
                wordsElement.isJsonPrimitive && wordsElement.asJsonPrimitive.isString -> runCatching {
                    JsonParser.parseString(wordsElement.asString).asJsonArray
                }.getOrNull()
                else -> null
            } ?: run {
                AiCoreLogger.w(TAG, "Paragraph[$paragraphIndex] Words 解析失败：$wordsElement")
                return@forEachIndexed
            }
            val grouped = mutableMapOf<Int, MutableList<JsonObject>>()
            wordsArray.forEach { wordElement ->
                val wordObj = wordElement.asJsonObject
                val sentenceId = wordObj.get("SentenceId")?.asInt
                    ?: wordObj.get("Id")?.asInt
                    ?: fallbackSentenceId++
                grouped.getOrPut(sentenceId) { mutableListOf() }.add(wordObj)
            }
            grouped.toSortedMap().values.forEach { chunks ->
                if (chunks.isEmpty()) return@forEach
                val sorted = chunks.sortedBy { it.get("Start")?.asDouble ?: 0.0 }
                val text = sorted.joinToString(separator = "") { it.get("Text")?.asString.orEmpty() }
                val start = sorted.first().get("Start")?.asDouble ?: 0.0
                val end = sorted.last().get("End")?.asDouble ?: start
                val speaker = sorted.firstNotNullOfOrNull { it.get("SpeakerId")?.asString } ?: paragraphSpeaker
                sentenceSegments += TingwuTranscriptSegment(
                    id = sorted.first().get("SentenceId")?.asInt ?: sorted.first().get("Id")?.asInt,
                    start = start,
                    end = end,
                    text = text,
                    speaker = speaker
                )
            }
        }
        AiCoreLogger.d(TAG, "Paragraph 转写解析：paragraphs=${paragraphsArray.size()} sentences=${sentenceSegments.size}")
        val aggregatedText = sentenceSegments.joinToString(separator = "\n") { it.text.orEmpty() }
        AiCoreLogger.d(TAG, "Paragraph 转写文本长度=${aggregatedText.length}")
        val audioInfo = transcriptionNode.getAsJsonObject("AudioInfo")
        val language = audioInfo?.get("Language")?.asString
        val duration = audioInfo?.get("Duration")?.asDouble
        return TingwuTranscription(
            text = transcriptionNode.get("Text")?.asString?.takeIf { it.isNotBlank() } ?: aggregatedText,
            segments = sentenceSegments.takeIf { it.isNotEmpty() },
            speakers = null,
            language = language,
            duration = duration
        )
    }

    private data class LegacyTranscriptionResponse(
        @SerializedName("task_id") val taskId: String?,
        @SerializedName("transcription") val transcription: LegacyTranscription?
    )

    private data class LegacyTranscription(
        @SerializedName("text") val text: String?,
        @SerializedName("segments") val segments: List<LegacySegment>?,
        @SerializedName("language") val language: String? = null,
        @SerializedName("duration") val duration: Double? = null
    ) {
        fun toOfficial(): TingwuTranscription = TingwuTranscription(
            text = text,
            segments = segments?.map { legacy ->
                TingwuTranscriptSegment(
                    id = legacy.id,
                    start = legacy.start,
                    end = legacy.end,
                    text = legacy.text,
                    speaker = legacy.speaker
                )
            },
            speakers = null,
            language = language,
            duration = duration
        )
    }

    private data class LegacySegment(
        @SerializedName("id") val id: Int?,
        @SerializedName("start") val start: Double?,
        @SerializedName("end") val end: Double?,
        @SerializedName("text") val text: String?,
        @SerializedName("speaker") val speaker: String?
    )

    private data class ParagraphTranscriptionResponse(
        @SerializedName("TaskId") val taskId: String?,
        @SerializedName("Transcription") val transcription: ParagraphTranscription?
    )

    private data class ParagraphTranscription(
        @SerializedName("Text") val text: String?,
        @SerializedName("Paragraphs") val paragraphs: List<ParagraphDetail>?,
        @SerializedName("AudioInfo") val audioInfo: ParagraphAudioInfo?
    )

    private data class ParagraphAudioInfo(
        @SerializedName("Language") val language: String?,
        @SerializedName("Duration") val duration: Double?
    )

    private data class ParagraphDetail(
        @SerializedName("ParagraphId") val paragraphId: String?,
        @SerializedName("SpeakerId") val speakerId: String?,
        @SerializedName("Words") val words: List<ParagraphWord>?
    )

    private data class ParagraphWord(
        @SerializedName("Id") val id: Int?,
        @SerializedName("SentenceId") val sentenceId: Int?,
        @SerializedName("Start") val start: Double?,
        @SerializedName("End") val end: Double?,
        @SerializedName("Text") val text: String?,
        @SerializedName("SpeakerId") val speakerId: String?
    )

    private fun buildMarkdown(
        transcription: TingwuTranscription?,
        diarizedSegments: List<DiarizedSegment> = emptyList(),
        speakerLabels: Map<String, String> = emptyMap(),
    ): String {
        if (diarizedSegments.isNotEmpty()) {
            val sorted = diarizedSegments.sortedBy { it.startMs }
            val builder = StringBuilder()
            builder.append("## 逐字稿\n")
            sorted.forEach { segment ->
                val label = segment.speakerId?.let { id ->
                    speakerLabels[id]?.takeIf { it.isNotBlank() } ?: id
                }
                val begin = formatTimeMs(segment.startMs)
                val end = formatTimeMs(segment.endMs)
                val hasValidRange = segment.endMs > segment.startMs &&
                    segment.endMs - segment.startMs <= MAX_SUBTITLE_DURATION_MS
                builder.append("- ")
                if (segment.startMs > 0 || segment.endMs > 0) {
                    builder.append("[")
                        .append(begin)
                    if (hasValidRange) {
                        builder.append(" - ").append(end)
                    }
                    builder.append("] ")
                }
                label?.let { builder.append(it).append("：") }
                builder.append(segment.text.ifBlank { "（空白）" }).append("\n")
            }
            return builder.toString().trimEnd()
        }
        transcription?.text?.takeIf { it.isNotBlank() }?.let { raw ->
            return buildString {
                append("## 逐字稿（无说话人分离数据）\n")
                append(raw.trim())
            }
        }
        val segments = transcription?.segments.orEmpty()
        if (segments.isEmpty()) {
            return "暂无可用的转写结果。"
        }
        return buildString {
            append("## 逐字稿\n")
            segments.forEach { segment ->
                val begin = formatTime(segment.start)
                val end = formatTime(segment.end)
                val content = segment.text?.trim().orEmpty()
                append("- ")
                if (!begin.isNullOrBlank() || !end.isNullOrBlank()) {
                    append("[")
                    append(begin)
                    append(" - ")
                    append(end)
                    append("] ")
                }
                append(content.ifBlank { "（空白）" }).append("\n")
            }
        }.trimEnd()
    }

    private fun buildDiarizedSegments(transcription: TingwuTranscription?): List<DiarizedSegment> {
        if (transcription == null) return emptyList()
        
        val segments = transcription.segments.orEmpty()
        val hasUsableSegments = segments.any { !it.text.isNullOrBlank() && !it.speaker.isNullOrBlank() }

        if (!hasUsableSegments) {
            // No diarized material; let caller fall back to transcription.text or "暂无可用..."
            return emptyList()
        }

        // If we have usable segments, do NOT disable diarization just because text looks formatted.

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

        val baseStartSeconds = sortedSegments.minOfOrNull { it.start ?: 0.0 }?.coerceAtLeast(0.0) ?: 0.0
        val diarized = sortedSegments.map { segment ->
            val (speakerId, speakerIndex) = resolveSpeaker(segment.speaker!!)
            val rawStart = (segment.start ?: 0.0) - baseStartSeconds
            val rawEnd = (segment.end ?: segment.start ?: 0.0) - baseStartSeconds
            val startMs = (rawStart * 1000).toLong()
            val endMs = (rawEnd * 1000).toLong()
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
            if (last != null && shouldMergeAsSubtitle(last, segment)) {
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

    /**
     * 根据 Tingwu 的说话人信息和分段结果，为每个 speakerId 生成稳定且可读的显示名称。
     */
    private fun buildSpeakerLabels(
        transcription: TingwuTranscription?,
        segments: List<DiarizedSegment>,
    ): Map<String, String> {
        if (transcription == null || segments.isEmpty()) return emptyMap()
        val speakerIdsInOrder = segments
            .mapNotNull { it.speakerId }
            .distinct()
        if (speakerIdsInOrder.isEmpty()) return emptyMap()
        val speakersById = transcription.speakers.orEmpty().associateBy { it.id }
        val labels = LinkedHashMap<String, String>()
        speakerIdsInOrder.forEach { id ->
            val fromName = speakersById[id]?.name?.takeIf { it.isNotBlank() }
            val fallback = id
            labels[id] = fromName ?: fallback
        }
        return labels
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

    private fun formatTime(value: Double?): String {
        if (value == null) return "00:00"
        val totalSeconds = max(value.toInt(), 0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${timeFormatter.format(minutes)}:${timeFormatter.format(seconds)}"
    }

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

    private fun inferProgress(status: TingwuStatusData): Int {
        val normalized = status.taskStatus?.uppercase(Locale.US) ?: "PENDING"
        val progress = status.taskProgress ?: when (normalized) {
            "PENDING" -> 5
            "QUEUED" -> 10
            "PROCESSING", "RUNNING" -> 50
            "TRANSCRIBING" -> 75
            else -> 20
        }
        return min(max(progress, 0), 95)
    }

    private fun resolveMissingUrlError(): AiCoreException = AiCoreException(
        source = AiCoreErrorSource.TINGWU,
        reason = AiCoreErrorReason.IO,
        message = "未检测到可访问的音频 URL",
        suggestion = "请先上传音频或配置可访问的 fileUrl"
    )

    private suspend fun resolveFileUrl(request: TingwuRequest): Result<String> {
        val direct = request.fileUrl?.trim()?.takeIf { it.isNotBlank() }
        if (direct != null) {
            return Result.Success(direct)
        }
        val objectKey = request.ossObjectKey?.takeIf { it.isNotBlank() }
            ?: return Result.Error(resolveMissingUrlError())
        AiCoreLogger.d(TAG, "未提供 fileUrl，开始为 $objectKey 生成预签名 URL")
        return signedUrlProvider.generate(objectKey, config.tingwuPresignUrlValiditySeconds)
    }

    private fun buildTaskKey(request: TingwuRequest): String {
        val baseName = request.ossObjectKey
            ?.substringAfterLast("/")
            ?.substringBefore(".")
            ?.takeIf { it.isNotBlank() }
            ?: request.audioAssetName
        val sanitized = baseName.replace(Regex("[^a-zA-Z0-9]"), "_")
        return sanitized + "_" + System.currentTimeMillis()
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

    private fun mapSourceLanguage(language: String): String {
        val normalized = language.lowercase(Locale.US)
        return when {
            normalized.startsWith("zh") || normalized.startsWith("cn") -> "cn"
            normalized.startsWith("en") -> "en"
            normalized.startsWith("ja") -> "ja"
            normalized.startsWith("yue") -> "yue"
            normalized.startsWith("fspk") -> "fspk"
            else -> DEFAULT_SOURCE_LANGUAGE
        }
    }

    private fun validateCredentials(credentials: TingwuCredentials): AiCoreException? {
        val missing = when {
            credentials.appKey.isBlank() -> "TINGWU_APP_KEY"
            credentials.baseUrl.isBlank() -> "TINGWU_BASE_URL"
            credentials.accessKeyId.isBlank() -> "ALIBABA_CLOUD_ACCESS_KEY_ID"
            credentials.accessKeySecret.isBlank() -> "ALIBABA_CLOUD_ACCESS_KEY_SECRET"
            config.requireTingwuSecurityToken && credentials.securityToken.isNullOrBlank() -> "TINGWU_SECURITY_TOKEN"
            else -> null
        }
        return missing?.let {
            AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.MISSING_CREDENTIALS,
                message = "Tingwu 配置缺失（$it）",
                suggestion = "在 local.properties 配置 $it"
            )
        }
    }

    private fun mapError(error: Throwable): AiCoreException = when (error) {
        is AiCoreException -> error
        is HttpException -> {
            val code = error.code()
            val body = error.response()?.errorBody()?.string()
            AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.REMOTE,
                message = "Tingwu HTTP $code：${body ?: error.message()}",
                suggestion = "请检查 OSS URL 是否可访问或 Tingwu 配置是否正确",
                cause = error
            )
        }
        is SocketTimeoutException -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.TIMEOUT,
            message = "Tingwu 请求超时",
            suggestion = "检查网络或增加 AiCoreConfig.tingwuPollTimeoutMillis",
            cause = error
        )
        is UnknownHostException -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.NETWORK,
            message = "Tingwu 域名解析失败：${error.message ?: "UnknownHost"}",
            suggestion = "检查设备 DNS 或启用 AiCoreConfig.enableTingwuHttpDns",
            cause = error
        )
        is SSLException -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.NETWORK,
            message = "Tingwu SSL 握手失败：${error.message ?: "SSLException"}",
            suggestion = "确认系统 CA 证书与网络代理设置",
            cause = error
        )
        is IOException -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.NETWORK,
            message = "Tingwu 网络异常：${error.message ?: "未知"}",
            suggestion = "确认设备可访问 Tingwu 域名或网关",
            cause = error
        )
        else -> AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.UNKNOWN,
            message = error.message ?: "Tingwu 未知错误",
            cause = error
        )
    }

    /** 判断两段是否可以安全合并为一行字幕。 */
    private fun shouldMergeAsSubtitle(previous: DiarizedSegment, next: DiarizedSegment): Boolean {
        if (previous.speakerIndex != next.speakerIndex) return false
        val gapMs = next.startMs - previous.endMs
        if (gapMs < 0 || gapMs > MAX_SUBTITLE_GAP_MS) return false
        val combinedDuration = max(next.endMs, previous.endMs) - min(previous.startMs, next.startMs)
        if (combinedDuration > MAX_SUBTITLE_DURATION_MS) return false
        val combinedLength = previous.text.length + 1 + next.text.length
        if (combinedLength > MAX_SUBTITLE_TEXT_LENGTH) return false
        return true
    }

    private fun splitIntoSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        val current = StringBuilder()
        text.forEach { ch ->
            current.append(ch)
            if (ch in SENTENCE_DELIMITERS) {
                val sentence = current.toString().trim()
                if (sentence.isNotEmpty()) {
                    sentences.add(sentence)
                }
                current.clear()
            }
        }
        val remainder = current.toString().trim()
        if (remainder.isNotEmpty()) {
            sentences.add(remainder)
        }
        return sentences
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
                    startMs = it.startMs ?: 0,
                    headline = it.title,
                    summary = null
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

    private data class TranscriptResult(
        val markdown: String,
        val artifacts: TingwuJobArtifacts?,
        val chapters: List<TingwuChapter>?,
        val diarizedSegments: List<DiarizedSegment>?
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

        return builder.toString().trimEnd()
    }

    companion object {
        private val TAG = "${LogTags.AI_CORE}/Tingwu"
        private const val DEFAULT_LANGUAGE = "zh-CN"
        private const val DEFAULT_SOURCE_LANGUAGE = "cn"
        private const val SUBMITTED_PROGRESS = 1
        private const val CUSTOM_PROMPT_KEY = "CustomPrompt"
        private const val MAX_SUBTITLE_GAP_MS = 2_000L
        private const val MAX_SUBTITLE_DURATION_MS = 10_000L
        private const val MAX_SUBTITLE_TEXT_LENGTH = 100
        private val SENTENCE_DELIMITERS = setOf('。', '？', '！', '.', '!', '?')
    }
}

internal fun parseAutoChaptersPayload(json: String): List<TingwuChapter> {
    val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return emptyList()
    val array: JsonArray? = when {
        root.isJsonArray -> root.asJsonArray
        root.isJsonObject -> {
            val obj = root.asJsonObject
            obj.getAsJsonArray("Chapters")
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
        val title = obj.getPrimitiveString("Title")
            ?: obj.getPrimitiveString("title")
            ?: obj.getPrimitiveString("Name")
            ?: obj.getPrimitiveString("name")
        val startRaw = obj.getPrimitiveNumber("Start")
            ?: obj.getPrimitiveNumber("StartTime")
            ?: obj.getPrimitiveNumber("StartMs")
        val endRaw = obj.getPrimitiveNumber("End")
            ?: obj.getPrimitiveNumber("EndTime")
            ?: obj.getPrimitiveNumber("EndMs")
        val startMs = startRaw?.let { toMillis(it) }
        val endMs = endRaw?.let { toMillis(it) }
        if (title.isNullOrBlank() || startMs == null) return@mapNotNull null
        TingwuChapter(title = title, startMs = startMs, endMs = endMs)
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
