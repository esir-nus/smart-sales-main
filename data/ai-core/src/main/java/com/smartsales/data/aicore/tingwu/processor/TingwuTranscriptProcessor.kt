package com.smartsales.data.aicore.tingwu.processor

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.TingwuChapter
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.debug.PipelineStage
import com.smartsales.data.aicore.debug.PipelineTracer
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.tingwu.polling.TingwuApiRepository
import com.smartsales.data.aicore.tingwu.api.TingwuApi
import com.smartsales.data.aicore.tingwu.api.TingwuResultResponse
import com.smartsales.data.aicore.tingwu.api.TingwuResultData
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription
import com.smartsales.data.aicore.tingwu.api.TingwuTranscriptSegment
import com.smartsales.data.aicore.tingwu.artifact.TingwuArtifactFetcher
import com.smartsales.data.aicore.tingwu.artifact.TingwuRawResponseDumper
import com.smartsales.data.aicore.tingwu.publisher.Publisher
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.DecimalFormat

/**
 * TranscriptResult: Output of transcript fetching/processing.
 */
data class TranscriptResult(
    val markdown: String,
    val artifacts: TingwuJobArtifacts?,
    val chapters: List<TingwuChapter>?,
    val diarizedSegments: List<DiarizedSegment>?
)

/**
 * TranscriptProcessor: Lattice interface for transcript fetching and transformation.
 * 
 * Responsibility: Fetch Tingwu result → parse → transform to markdown
 */
interface TranscriptProcessor {
    /**
     * Fetch and process transcript for a completed Tingwu job.
     */
    suspend fun fetchTranscript(
        jobId: String,
        resultLinks: Map<String, String>?,
        fallbackArtifacts: TingwuJobArtifacts?,
        runEnhancer: suspend (TingwuTranscription?, List<DiarizedSegment>, Map<String, String>, String) -> String,
        composeFinalMarkdown: (String, TingwuJobArtifacts?, Map<String, String>?) -> String
    ): TranscriptResult
}

@Singleton
class TingwuTranscriptProcessor @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val api: TingwuApi,
    optionalConfig: java.util.Optional<AiCoreConfig>,
    private val formatter: TranscriptFormatter,
    private val apiRepository: TingwuApiRepository,
    private val artifactFetcher: TingwuArtifactFetcher,
    private val transcriptPublisher: Publisher,
    private val tingwuRawResponseDumper: TingwuRawResponseDumper,
    private val tingwuTraceStore: TingwuTraceStore,
    private val pipelineTracer: PipelineTracer
) : TranscriptProcessor {
    private val config = optionalConfig.orElse(AiCoreConfig())
    private val gson = Gson()
    private val timeFormatter = DecimalFormat("00")

    override suspend fun fetchTranscript(
        jobId: String,
        resultLinks: Map<String, String>?,
        fallbackArtifacts: TingwuJobArtifacts?,
        // Callbacks to allow TingwuRunner to inject its specific logic if needed
        runEnhancer: suspend (TingwuTranscription?, List<DiarizedSegment>, Map<String, String>, String) -> String,
        composeFinalMarkdown: (String, TingwuJobArtifacts?, Map<String, String>?) -> String
    ): TranscriptResult {
        AiCoreLogger.d(TAG, "开始拉取转写结果：jobId=$jobId")
        pipelineTracer.emit(
            stage = PipelineStage.TINGWU_FETCH,
            status = "STARTED",
            message = "jobId=$jobId"
        )
        val chaptersUrl = transcriptPublisher.extractAutoChaptersUrl(resultLinks) ?: fallbackArtifacts?.autoChaptersUrl

        // First try the /transcription endpoint
        AiCoreLogger.d(TAG, "尝试使用 /transcription 接口：jobId=$jobId")
        val inline = runCatching { api.getTaskResult(taskId = jobId) }.fold(
            onSuccess = { response ->
                val rawJson = gson.toJson(response)
                val data = requireData(
                    code = response.code,
                    message = response.message,
                    requestId = response.requestId,
                    data = response.data,
                    action = "GetTranscription"
                )
                if (data.transcription == null) {
                    AiCoreLogger.w(TAG, "/transcription 接口响应缺少 transcription 字段，尝试 Result.Transcription 链接：jobId=$jobId")
                    null
                } else {
                    val textLength = data.transcription.text?.length ?: 0
                    val segmentsCount = data.transcription.segments?.size ?: 0
                    AiCoreLogger.d(TAG, "/transcription 接口成功：jobId=$jobId text长度=$textLength segments=$segmentsCount requestId=${response.requestId}")
                    
                    val transcription = data.transcription
                    val diarizedSegments = formatter.buildDiarizedSegments(transcription)
                    val recordingOriginSegments = buildRecordingOriginSegments(transcription)
                    val speakerLabels = formatter.buildSpeakerLabels(transcription, diarizedSegments)
                    val markdown = formatter.buildMarkdown(transcription, diarizedSegments, speakerLabels)
                    
                    // 重要：转写原始响应仅落盘，不在内存/HUD 直接保存原文。
                    recordTingwuRawDump(
                        jobId = jobId,
                        rawJson = rawJson,
                        transcriptText = resolveTranscriptText(transcription, markdown),
                        sessionId = null // Will be filled by caller context if possible, or null
                    )
                    
                    val enhancedMarkdown = runEnhancer(
                        transcription,
                        diarizedSegments,
                        speakerLabels,
                        markdown
                    )
                    
                    AiCoreLogger.d(TAG, "转写 markdown 生成成功：jobId=$jobId markdown长度=${markdown.length}")
                    val chapters = chaptersUrl?.let { transcriptPublisher.fetchChaptersSafe(it, jobId) }
                    val artifacts = data.toArtifacts(
                        transcriptionUrl = transcription.url,
                        autoChaptersUrl = transcriptPublisher.extractAutoChaptersUrl(data.resultLinks),
                        customPromptUrl = data.resultLinks?.get("CustomPrompt"),
                        extraResultUrls = data.resultLinks.orEmpty(),
                        chapters = chapters,
                        smartSummary = transcriptPublisher.fetchSmartSummarySafe(data.resultLinks, jobId),
                        diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() },
                        recordingOriginDiarizedSegments = recordingOriginSegments.takeIf { it.isNotEmpty() },
                        speakerLabels = speakerLabels
                    ) ?: fallbackArtifacts?.copy(
                        chapters = chapters ?: fallbackArtifacts.chapters,
                        smartSummary = fallbackArtifacts.smartSummary,
                        diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() }
                            ?: fallbackArtifacts.diarizedSegments,
                        recordingOriginDiarizedSegments = recordingOriginSegments.takeIf { it.isNotEmpty() }
                            ?: fallbackArtifacts.recordingOriginDiarizedSegments,
                        speakerLabels = if (speakerLabels.isNotEmpty()) speakerLabels else fallbackArtifacts.speakerLabels
                    )
                    
                    val finalMarkdown = composeFinalMarkdown(
                        enhancedMarkdown,
                        artifacts,
                        data.resultLinks
                    )
                    
                    pipelineTracer.emit(
                        stage = PipelineStage.TINGWU_FETCH,
                        status = "COMPLETED",
                        message = "jobId=$jobId source=inline"
                    )
                    
                    TranscriptResult(
                        markdown = finalMarkdown,
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
                    val mapped = apiRepository.mapError(error)
                    AiCoreLogger.e(TAG, "拉取 Tingwu 结果失败：${mapped.message}", mapped)
                    throw mapped
                }
            }
        )
        
        if (inline != null) {
            AiCoreLogger.d(TAG, "使用内联转写结果（/transcription 接口）")
            return inline
        }
        
        AiCoreLogger.d(TAG, "内联结果不可用，尝试从 Result.Transcription URL 下载")
        val signedUrl = transcriptPublisher.extractTranscriptionUrl(resultLinks)
            ?: throw AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.REMOTE,
                message = "Result.Transcription 链接缺失",
                suggestion = "请在 Tingwu 控制台确认任务开启了转写输出。Result 字段可能为空或缺少 Transcription 键。"
            )
            
        AiCoreLogger.d(TAG, "开始从 URL 下载转写 JSON：jobId=$jobId")
        val transcription = downloadTranscription(signedUrl, jobId)
        val diarizedSegments = formatter.buildDiarizedSegments(transcription)
        val recordingOriginSegments = buildRecordingOriginSegments(transcription)
        val speakerLabels = formatter.buildSpeakerLabels(transcription, diarizedSegments)
        val markdown = formatter.buildMarkdown(transcription, diarizedSegments, speakerLabels)
        
        val enhancedMarkdown = runEnhancer(
            transcription,
            diarizedSegments,
            speakerLabels,
            markdown
        )
        
        AiCoreLogger.d(TAG, "转写 JSON 解析成功：jobId=$jobId markdown长度=${markdown.length}")
        val chapters = chaptersUrl?.let { transcriptPublisher.fetchChaptersSafe(it, jobId) }
        val smartSummary = transcriptPublisher.fetchSmartSummarySafe(resultLinks, jobId)
        
        val artifacts = fallbackArtifacts?.copy(
            transcriptionUrl = signedUrl,
            autoChaptersUrl = fallbackArtifacts.autoChaptersUrl,
            customPromptUrl = resultLinks?.get("CustomPrompt") ?: fallbackArtifacts.customPromptUrl,
            extraResultUrls = fallbackArtifacts.extraResultUrls,
            chapters = chapters ?: fallbackArtifacts.chapters,
            smartSummary = smartSummary ?: fallbackArtifacts.smartSummary,
            diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() }
                ?: fallbackArtifacts.diarizedSegments,
            recordingOriginDiarizedSegments = recordingOriginSegments.takeIf { it.isNotEmpty() }
                ?: fallbackArtifacts.recordingOriginDiarizedSegments,
            speakerLabels = if (speakerLabels.isNotEmpty()) speakerLabels else fallbackArtifacts.speakerLabels
        ) ?: fallbackArtifacts
        
        val finalMarkdown = composeFinalMarkdown(
            enhancedMarkdown,
            artifacts,
            resultLinks
        )
        
        pipelineTracer.emit(
            stage = PipelineStage.TINGWU_FETCH,
            status = "COMPLETED",
            message = "jobId=$jobId source=download"
        )
        
        return TranscriptResult(
            markdown = finalMarkdown,
            artifacts = artifacts,
            chapters = chapters,
            diarizedSegments = diarizedSegments.takeIf { it.isNotEmpty() }
        )
    }

    private fun downloadTranscription(url: String, jobId: String): TingwuTranscription {
        AiCoreLogger.d(TAG, "开始下载转写 JSON：jobId=$jobId url=${url.take(100)}...")
        val payload = artifactFetcher.fetchText(url, timeoutMs = config.tingwuReadTimeoutMillis.toInt(), maxChars = 5_000_000)
            ?: throw AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.NETWORK,
                message = "下载 Tingwu 转写结果失败：ArtifactFetcher 返回 null",
                suggestion = "确认 Result.Transcription 链接仍在有效期内，或检查网络连接"
            )

        AiCoreLogger.d(TAG, "下载完成：jobId=$jobId payload大小=${payload.length} 字符")
        val parsed = parseDownloadedTranscription(payload, jobId)
        // 重要：转写原始响应仅落盘，不在内存/HUD 直接保存原文。
        recordTingwuRawDump(
            jobId = jobId,
            rawJson = payload,
            transcriptText = resolveTranscriptText(parsed, null),
            sessionId = null
        )
        return parsed ?: throw AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.IO,
            message = "无法解析 Tingwu 转写结果",
            suggestion = "请检查 Result.Transcription 内容是否符合官方格式。Payload 前200字符：${payload.take(200)}"
        )
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
                // NOTE: Paragraph JSON's Words.Start/End are in MILLISECONDS,
                // but TingwuTranscriptSegment.start/end expect SECONDS.
                // Convert ms → seconds to match expected unit.
                val startMs = sorted.first().get("Start")?.asDouble ?: 0.0
                val endMs = sorted.last().get("End")?.asDouble ?: startMs
                val start = startMs / 1000.0
                val end = endMs / 1000.0
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

    private fun resolveTranscriptText(
        transcription: TingwuTranscription?,
        fallback: String?
    ): String? {
        val rawText = transcription?.text?.trim().orEmpty()
        if (rawText.isNotBlank()) return rawText
        val segments = transcription?.segments.orEmpty()
        if (segments.isNotEmpty()) {
            val joined = segments.mapNotNull { it.text?.trim() }
                .filter { it.isNotBlank() }
                .joinToString(separator = "\n")
            if (joined.isNotBlank()) return joined
        }
        return fallback?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun recordTingwuRawDump(
        jobId: String,
        rawJson: String,
        transcriptText: String?,
        sessionId: String?
    ) {
        runCatching {
            val dump = tingwuRawResponseDumper.dumpTranscription(
                jobId = jobId,
                sessionId = sessionId,
                rawJson = rawJson,
                transcriptText = transcriptText,
            )
            tingwuTraceStore.record(
                taskId = jobId,
                transcriptionDumpPath = dump.rawDumpPath,
                transcriptionDumpBytes = dump.rawDumpBytes,
                transcriptionDumpSavedAtMs = dump.rawDumpSavedAtMs,
                transcriptDumpPath = dump.transcriptDumpPath,
                transcriptDumpBytes = dump.transcriptDumpBytes,
                transcriptDumpSavedAtMs = dump.transcriptDumpSavedAtMs,
            )
        }.onFailure { error ->
            AiCoreLogger.d(TAG, "Tingwu raw dump 失败：${error.message}")
        }
    }

    private fun buildRecordingOriginSegments(transcription: TingwuTranscription?): List<DiarizedSegment> {
        val segments = transcription?.segments.orEmpty()
        if (segments.isEmpty()) return emptyList()

        return segments.mapNotNull { it ->
            val startMs = (it.start ?: 0.0) * 1000
            val endMs = (it.end ?: 0.0) * 1000
            val text = it.text?.trim()
            if (text.isNullOrBlank()) null
            else DiarizedSegment(
                startMs = startMs.toLong(),
                endMs = endMs.toLong(),
                text = text,
                speakerId = it.speaker ?: "unknown",
                speakerIndex = it.speaker?.toIntOrNull() ?: 0
            )
        }
    }

    // Helper functions for TingwuRunnerRepository compatibility replacement
    private fun TingwuResultData.toArtifacts(
        transcriptionUrl: String? = null,
        autoChaptersUrl: String? = null,
        customPromptUrl: String? = null,
        extraResultUrls: Map<String, String> = emptyMap(),
        chapters: List<TingwuChapter>? = null,
        smartSummary: com.smartsales.data.aicore.TingwuSmartSummary? = null,
        diarizedSegments: List<DiarizedSegment>? = null,
        recordingOriginDiarizedSegments: List<DiarizedSegment>? = null,
        speakerLabels: Map<String, String> = emptyMap()
    ): TingwuJobArtifacts? {
        val hasAny = transcriptionUrl != null || autoChaptersUrl != null || customPromptUrl != null || !chapters.isNullOrEmpty()
        if (!hasAny) return null
        return TingwuJobArtifacts(
            transcriptionUrl = transcriptionUrl,
            autoChaptersUrl = autoChaptersUrl,
            customPromptUrl = customPromptUrl,
            extraResultUrls = extraResultUrls,
            chapters = chapters,
            smartSummary = smartSummary,
            diarizedSegments = diarizedSegments,
            recordingOriginDiarizedSegments = recordingOriginDiarizedSegments,
            speakerLabels = speakerLabels
        )
    }


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

    private fun requireData(
        code: String?,
        message: String?,
        requestId: String?,
        data: TingwuResultData?,
        action: String
    ): TingwuResultData {
        // Tingwu API uses "0" as success code (Chinese API convention)
        val isSuccess = code == "0" || code == "200" || code == "OK" || code == "Success"
        if (!isSuccess || data == null) {
             throw com.smartsales.data.aicore.AiCoreException(
                 source = com.smartsales.data.aicore.AiCoreErrorSource.TINGWU,
                 reason = com.smartsales.data.aicore.AiCoreErrorReason.REMOTE,
                 message = "$action 失败：$message (code=$code, requestId=$requestId)",
                 suggestion = "检查任务状态"
             )
        }
        return data
    }

    companion object {
        private const val TAG = "TingwuTranscriptProcessor"
    }
}
