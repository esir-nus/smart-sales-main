package com.smartsales.data.aicore

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.tingwu.TingwuApi
import com.smartsales.data.aicore.tingwu.TingwuCreateTaskData
import com.smartsales.data.aicore.tingwu.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.TingwuCreateTaskResponse
import com.smartsales.data.aicore.tingwu.TingwuResultData
import com.smartsales.data.aicore.tingwu.TingwuResultResponse
import com.smartsales.data.aicore.tingwu.TingwuStatusData
import com.smartsales.data.aicore.tingwu.TingwuStatusResponse
import com.smartsales.data.aicore.tingwu.TingwuTranscription
import com.smartsales.core.metahub.AnalysisSource
import com.smartsales.core.metahub.InMemoryMetaHub
import com.smartsales.core.metahub.SpeakerMeta
import com.smartsales.core.metahub.TranscriptMetadata
import com.smartsales.data.aicore.tingwu.TingwuTaskParameters
import com.smartsales.data.aicore.tingwu.TingwuSummarizationParameters
import com.smartsales.data.aicore.tingwu.TingwuTranscriptSegment
import com.smartsales.data.aicore.tingwu.TingwuSpeaker
import com.smartsales.data.aicore.TranscriptMetadataRequest
import com.smartsales.data.aicore.TranscriptOrchestrator
import com.google.gson.Gson
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.params.PostTingwuEnhancerSettings
import com.smartsales.data.aicore.posttingwu.EnhancerInput
import com.smartsales.data.aicore.posttingwu.EnhancerOutput
import com.smartsales.data.aicore.posttingwu.SpeakerLabel
import com.smartsales.data.aicore.posttingwu.SplitLine
import com.smartsales.data.aicore.posttingwu.UtteranceEdit
import com.smartsales.data.aicore.tingwu.TingwuArtifactFetcher
import com.smartsales.data.aicore.posttingwu.PostTingwuTranscriptEnhancer
import java.io.File
import java.util.Optional
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import kotlin.io.path.createTempFile

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/RealTingwuCoordinatorTest.kt
// 模块：:data:ai-core
// 说明：验证真实 Tingwu 协调器的轮询与状态映射
// 作者：创建于 2025-11-16
@OptIn(ExperimentalCoroutinesApi::class)
class RealTingwuCoordinatorTest {

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = FakeDispatcherProvider(dispatcher)
    private val traceStore = TingwuTraceStore()
    private val noopFetcher = object : TingwuArtifactFetcher {
        override fun fetchText(url: String, timeoutMs: Int, maxChars: Int): String? = null
    }
    private val credentialsProvider = object : TingwuCredentialsProvider {
        override fun obtain(): TingwuCredentials = TingwuCredentials(
            apiKey = "demo-api-key",
            baseUrl = "https://tingwu.cn/",
            appKey = "demo-app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            securityToken = "sts",
            model = "tingwu-demo"
        )
    }
    private val signedUrlProvider = object : OssSignedUrlProvider {
        override suspend fun generate(objectKey: String, expiresInSeconds: Long): Result<String> =
            Result.Success("https://oss.example.com/$objectKey?exp=$expiresInSeconds")
    }
    private val transcriptOrchestrator = object : TranscriptOrchestrator {
        override suspend fun inferTranscriptMetadata(request: TranscriptMetadataRequest) = null
    }
    private val noopEnhancer = object : PostTingwuTranscriptEnhancer {
        override suspend fun enhance(input: com.smartsales.data.aicore.posttingwu.EnhancerInput) = null
    }
    private val defaultSettingsProvider = FakeAiParaSettingsProvider()

    private fun newCoordinator(
        api: TingwuApi,
        optionalConfig: Optional<AiCoreConfig> = Optional.empty(),
        enhancer: PostTingwuTranscriptEnhancer = noopEnhancer,
        settingsProvider: AiParaSettingsProvider = defaultSettingsProvider,
        metaHub: com.smartsales.core.metahub.MetaHub = InMemoryMetaHub()
    ): RealTingwuCoordinator = RealTingwuCoordinator(
        dispatchers = dispatchers,
        api = api,
        credentialsProvider = credentialsProvider,
        signedUrlProvider = signedUrlProvider,
        transcriptOrchestrator = transcriptOrchestrator,
        metaHub = metaHub,
        tingwuTraceStore = traceStore,
        artifactFetcher = noopFetcher,
        postTingwuTranscriptEnhancer = enhancer,
        aiParaSettingsProvider = settingsProvider,
        optionalConfig = optionalConfig
    )
    @Test
    fun submit_emitsCompletedTranscript() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "## 摘要\n- 测试成功",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 10.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = mapOf(
                    "Transcription" to "https://example.com/transcription.json",
                    "AutoChapters" to "https://example.com/chapters.json"
                ),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertTrue(completed.transcriptMarkdown.contains("测试成功"))
        assertEquals("https://example.com/transcription.json", completed.artifacts?.transcriptionUrl)
        assertEquals("https://example.com/chapters.json", completed.artifacts?.autoChaptersUrl)
    }

    @Test
    fun createTask_enablesSummarization() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        val coordinator = newCoordinator(api)

        coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )

        val request = api.lastCreateRequest
        assertEquals(true, request?.parameters?.summarizationEnabled)
        assertEquals(
            listOf("Paragraph", "Conversational", "QuestionsAnswering"),
            request?.parameters?.summarization?.types
        )
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"SummarizationEnabled\":true"))
        assertTrue(json.contains("\"Summarization\":{\"Types\":[\"Paragraph\",\"Conversational\",\"QuestionsAnswering\"]}"))
        assertTrue(json.contains("\"DiarizationEnabled\":true"))
        assertTrue(json.contains("\"SpeakerCount\":0"))
        assertTrue(json.contains("\"OutputLevel\":1"))
        assertTrue(json.contains("\"AudioEventDetectionEnabled\":true"))
        assertTrue(json.contains("\"AutoChaptersEnabled\":true"))
        assertTrue(json.contains("\"TextPolishEnabled\":true"))
        assertTrue(json.contains("\"PptExtractionEnabled\":true"))
        assertTrue(json.contains("\"Transcoding\":{\"TargetAudioFormat\":\"mp3\"}"))
    }

    @Test
    fun createTask_includesCustomPromptWhenEnabled() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        val coordinator = newCoordinator(api)

        coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav",
                customPromptEnabled = false,
                customPromptName = "",
                customPromptText = ""
            )
        )

        val request = api.lastCreateRequest
        assertTrue(request != null)
        val params = request!!.parameters

        assertTrue(params.customPromptEnabled == true)

        val customPrompt = params.customPrompt
        assertTrue(customPrompt != null)
        assertTrue(customPrompt!!.contents.isNotEmpty())

        val content0 = customPrompt.contents.first()
        assertTrue(content0.name.isNotBlank())
        assertTrue(content0.prompt.isNotBlank())

        // Verify model wiring (model may be optional in some configs)
        assertTrue(content0.model.isNullOrBlank().not())

        // Also verify JSON serialization includes expected keys (without pinning content values)
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"CustomPromptEnabled\":true"))
        assertTrue(json.contains("\"CustomPrompt\""))
        assertTrue(json.contains("\"Contents\""))
        assertTrue(json.contains("\"Prompt\""))
    }

    @Test
    fun createTask_whenSummarizationDisabled_omitsSummarizationTypes() {
        val request = TingwuCreateTaskRequest(
            appKey = "demo",
            input = com.smartsales.data.aicore.tingwu.TingwuTaskInput(
                sourceLanguage = "cn",
                taskKey = "k1",
                fileUrl = "https://example.com/a.wav"
            ),
            parameters = TingwuTaskParameters(
                transcription = null,
                translationEnabled = null,
                summarizationEnabled = false,
                summarization = null
            )
        )
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"SummarizationEnabled\":false"))
        assertTrue(!json.contains("\"Summarization\""))
    }

    @Test
    fun createTask_respectsDiarizationFlag() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        val coordinator = newCoordinator(api)

        coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav",
                diarizationEnabled = false
            )
        )

        val request = api.lastCreateRequest
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"DiarizationEnabled\":false"))
        // 关闭说话人分离时，不应包含 Diarization 对象
        assertTrue(!json.contains("\"Diarization\""))
    }

    @Test
    fun diarizedSegments_renderWithSpeakerLabels() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "",
                    segments = listOf(
                        TingwuTranscriptSegment(id = 1, start = 0.0, end = 1.2, text = "你好", speaker = "spk_1"),
                        TingwuTranscriptSegment(id = 2, start = 1.3, end = 2.4, text = "欢迎光临", speaker = "spk_2"),
                        TingwuTranscriptSegment(id = 3, start = 2.5, end = 3.0, text = "继续说明", speaker = "spk_2")
                    ),
                    speakers = listOf(
                        TingwuSpeaker(id = "spk_1", name = "客户"),
                        TingwuSpeaker(id = "spk_2", name = "销售")
                    ),
                    language = "zh",
                    duration = 3.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = emptyMap(),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val lines = completed.transcriptMarkdown.lines().filter { it.startsWith("- ") }
        // 两个说话人应各有一行字幕，包含时间戳和完整文本
        assertEquals(2, lines.size)
        val first = lines[0]
        val second = lines[1]
        // TingwuSpeaker.name 提供了自定义名称，应优先展示为“客户/销售”
        assertTrue(first.contains("客户"))
        assertTrue(second.contains("销售"))
        assertTrue(second.contains("欢迎光临 继续说明"))
        // 校验时间戳格式为 [mm:ss] 或 [mm:ss - mm:ss]
        assertTrue(first.contains("[00:00]") || Regex("\\[\\d{2}:\\d{2}(\\s-\\s\\d{2}:\\d{2})?]").containsMatchIn(first))
        assertTrue(Regex("\\[\\d{2}:\\d{2}(\\s-\\s\\d{2}:\\d{2})?]").containsMatchIn(second))
        assertEquals(2, completed.artifacts?.diarizedSegments?.size)
        // speakerLabels 中也应包含两个人的映射
        val labels = completed.artifacts?.speakerLabels
        assertEquals("客户", labels?.get("spk_1"))
        assertEquals("销售", labels?.get("spk_2"))
    }

    @Test
    fun diarizedSegments_withoutSpeakerNames_useNeutralLabels() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "",
                    segments = listOf(
                        TingwuTranscriptSegment(id = 1, start = 0.0, end = 1.2, text = "你好", speaker = "spk_1"),
                        TingwuTranscriptSegment(id = 2, start = 1.3, end = 2.4, text = "欢迎光临", speaker = "spk_2")
                    ),
                    speakers = listOf(
                        TingwuSpeaker(id = "spk_1", name = null),
                        TingwuSpeaker(id = "spk_2", name = "")
                    ),
                    language = "zh",
                    duration = 3.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = emptyMap(),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val lines = completed.transcriptMarkdown.lines().filter { it.startsWith("- ") }
        assertTrue(lines.any { it.contains("spk_1") })
        assertTrue(lines.any { it.contains("spk_2") })
        val labels = completed.artifacts?.speakerLabels
        assertEquals(mapOf("spk_1" to "spk_1", "spk_2" to "spk_2"), labels)
        lines.forEach { line ->
            assertFalse(line.contains("客户"))
            assertFalse(line.contains("销售"))
            assertFalse(line.contains("发言人"))
        }
    }

    @Test
    fun plainTextTranscription_rendersNonDiarizedTranscript_withoutSyntheticSpeakers() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-2",
                transcription = TingwuTranscription(
                    text = "你好罗总。这是今天的试驾安排。",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 30.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = emptyMap(),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "plain.wav",
                fileUrl = "https://oss.example.com/plain.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val markdown = completed.transcriptMarkdown
        assertTrue(markdown.contains("逐字稿（无说话人分离数据）"))
        assertTrue(markdown.contains("你好罗总。这是今天的试驾安排。"))
        assertFalse(markdown.contains("发言人"))
        val diarized = completed.artifacts?.diarizedSegments
        assertTrue(diarized.isNullOrEmpty())
    }

    @Test
    fun completedJob_withSummarizationLink_emitsSmartSummary() = runTest(dispatcher) {
        val summaryFile = createTempFile(suffix = ".json").toFile().apply {
            writeText(
                """
                {
                  "Summary":"会议概览",
                  "KeyPoints":["要点1"],
                  "ActionItems":["行动A"]
                }
                """.trimIndent()
            )
        }
        val summaryUrl = summaryFile.toURI().toURL().toString()
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf(
                    "Transcription" to "https://example.com/transcription.json",
                    "Summarization" to summaryUrl
                )
            )
        )
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 10.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = mapOf(
                    "Transcription" to "https://example.com/transcription.json",
                    "Summarization" to summaryUrl
                ),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val summary = completed.artifacts?.smartSummary
        assertEquals("会议概览", summary?.summary)
        assertEquals(listOf("要点1"), summary?.keyPoints)
        assertEquals(listOf("行动A"), summary?.actionItems)
    }

    @Test
    fun completedJob_writesSessionMetadataWithTingwuSource() = runTest(dispatcher) {
        val summaryFile = createTempFile(suffix = ".json").toFile().apply {
            writeText(
                """
                {
                  "Summary":"会议概览",
                  "KeyPoints":["要点1"],
                  "ActionItems":["行动A"]
                }
                """.trimIndent()
            )
        }
        val summaryUrl = summaryFile.toURI().toURL().toString()
        val api = FakeTingwuApi()
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf(
                    "Transcription" to "https://example.com/transcription.json",
                    "Summarization" to summaryUrl
                )
            )
        )
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-meta",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 10.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = mapOf(
                    "Transcription" to "https://example.com/transcription.json",
                    "Summarization" to summaryUrl
                ),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val metaHub = InMemoryMetaHub()
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            ),
            metaHub = metaHub
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav",
                sessionId = "session-meta"
            )
        )
        assertTrue(result is Result.Success)
        advanceTimeBy(20)
        advanceUntilIdle()

        val saved = metaHub.getSession("session-meta")
        assertEquals("会议概览", saved?.shortSummary)
        assertEquals("会议概览", saved?.summaryTitle6Chars)
        assertEquals(AnalysisSource.TINGWU, saved?.latestMajorAnalysisSource)
        assertTrue((saved?.latestMajorAnalysisAt ?: 0) > 0)
        assertTrue(saved?.tags?.contains("要点1") == true)
    }

    @Test
    fun completedJob_withoutSummarizationLink_keepsSmartSummaryNull() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 10.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = mapOf("Transcription" to "https://example.com/transcription.json"),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertEquals(null, completed.artifacts?.smartSummary)
    }

    @Test
    fun submit_updatesFailedStateWhenApiFails() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(
            statusResponse(
                status = "FAILED",
                progress = 60,
                errorCode = "Error",
                errorMessage = "语音太短"
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "error.wav",
                fileUrl = "https://oss.example.com/error.wav"
            )
        )
        assertTrue(result is Result.Success)
        val jobId = (result as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()
        val failed = coordinator.observeJob(jobId).first { it is TingwuJobState.Failed } as TingwuJobState.Failed
        assertEquals("语音太短", failed.reason)
        assertEquals(AiCoreErrorReason.REMOTE, failed.error.reason)
    }

    @Test
    fun submit_returnsErrorWhenCredentialsMissing() = runTest(dispatcher) {
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = FakeTingwuApi(),
            credentialsProvider = object : TingwuCredentialsProvider {
                override fun obtain(): TingwuCredentials = TingwuCredentials(
                    apiKey = "",
                    baseUrl = "https://tingwu.cn/",
                    appKey = "",
                    accessKeyId = "",
                    accessKeySecret = "",
                    securityToken = null,
                    model = "tingwu"
                )
            },
            signedUrlProvider = signedUrlProvider,
            transcriptOrchestrator = transcriptOrchestrator,
            metaHub = InMemoryMetaHub(),
            tingwuTraceStore = traceStore,
            artifactFetcher = noopFetcher,
            postTingwuTranscriptEnhancer = noopEnhancer,
            aiParaSettingsProvider = defaultSettingsProvider,
            optionalConfig = Optional.empty()
        )

        val result = coordinator.submit(
            TingwuRequest(
                audioAssetName = "missing.wav",
                fileUrl = "https://oss.example.com/missing.wav"
            )
        )

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable as AiCoreException
        assertEquals(AiCoreErrorReason.MISSING_CREDENTIALS, error.reason)
    }

    @Test
    fun fetchTranscriptFallsBackToResultLinkWhenApiMissing() = runTest(dispatcher) {
        val api = FakeTingwuApi().apply { failResultWith404 = true }
        val processing = statusResponse(status = "PROCESSING", progress = 20)
        api.enqueueStatus(processing)
        val tempFile = createTempFile(suffix = ".json").toFile().apply {
            writeText(
                """
                {
                  "RequestId":"req-link",
                  "Code":"0",
                  "Message":"success",
                  "Data":{
                    "TaskId":"job-1",
                    "Transcription":{
                      "Text":"## Link 模式",
                      "Segments":[],
                      "Speakers":[],
                      "Language":"zh",
                      "Duration":5.0
                    }
                  }
                }
                """.trimIndent()
            )
            deleteOnExit()
        }
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf("Transcription" to tempFile.toURI().toURL().toString())
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10))
        )
        val submitResult = coordinator.submit(
            TingwuRequest(
                audioAssetName = "link.wav",
                fileUrl = "https://oss.example.com/link.wav"
            )
        )
        assertTrue(submitResult is Result.Success)
        advanceTimeBy(20)
        advanceUntilIdle()
        val completed = coordinator.observeJob("job-1").first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertTrue(completed.transcriptMarkdown.contains("Link 模式"))
    }

    @Test
    fun refineSpeakerLabels_mergesMetadataAndSetsForce() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "",
                    segments = listOf(
                        TingwuTranscriptSegment(id = 1, start = 0.0, end = 1.0, text = "你好", speaker = "spk_1"),
                        TingwuTranscriptSegment(id = 2, start = 1.0, end = 2.0, text = "欢迎", speaker = "spk_2")
                    ),
                    speakers = listOf(
                        TingwuSpeaker(id = "spk_1", name = "客户"),
                        TingwuSpeaker(id = "spk_2", name = "销售")
                    ),
                    language = "zh",
                    duration = 2.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = emptyMap(),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val orchestrator = RecordingTranscriptOrchestrator(
            result = TranscriptMetadata(
                transcriptId = "job-1",
                sessionId = "s-merge",
                speakerMap = mapOf(
                    "spk_1" to SpeakerMeta(displayName = "老板", role = null, confidence = 0.9f),
                    "spk_2" to SpeakerMeta(displayName = "销售主管", role = null, confidence = 0.4f)
                )
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            transcriptOrchestrator = orchestrator,
            metaHub = InMemoryMetaHub(),
            tingwuTraceStore = traceStore,
            artifactFetcher = noopFetcher,
            postTingwuTranscriptEnhancer = noopEnhancer,
            aiParaSettingsProvider = defaultSettingsProvider,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10, tingwuPollTimeoutMillis = 200))
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav",
                sessionId = "s-merge"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val request = orchestrator.lastRequest
        assertEquals("s-merge", request?.sessionId)
        assertEquals(true, request?.force)
        assertEquals(2, request?.diarizedSegments?.size)
        assertEquals(mapOf("spk_1" to "客户", "spk_2" to "销售"), request?.speakerLabels)
        val labels = completed.artifacts?.speakerLabels
        assertEquals("老板", labels?.get("spk_1"))
        assertEquals("销售", labels?.get("spk_2"))
        assertEquals(1, orchestrator.callCount)
    }

    @Test
    fun refineSpeakerLabels_whenInferenceFails_keepsFallbackLabels() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "",
                    segments = listOf(
                        TingwuTranscriptSegment(id = 1, start = 0.0, end = 1.0, text = "你好", speaker = "spk_1"),
                        TingwuTranscriptSegment(id = 2, start = 1.0, end = 2.0, text = "欢迎", speaker = "spk_2")
                    ),
                    speakers = listOf(
                        TingwuSpeaker(id = "spk_1", name = "客户"),
                        TingwuSpeaker(id = "spk_2", name = "销售")
                    ),
                    language = "zh",
                    duration = 2.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = emptyMap(),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val orchestrator = RecordingTranscriptOrchestrator(throwable = IllegalStateException("llm fail"))
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            transcriptOrchestrator = orchestrator,
            metaHub = InMemoryMetaHub(),
            tingwuTraceStore = traceStore,
            artifactFetcher = noopFetcher,
            postTingwuTranscriptEnhancer = noopEnhancer,
            aiParaSettingsProvider = defaultSettingsProvider,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10, tingwuPollTimeoutMillis = 200))
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav",
                sessionId = "s-fail"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val labels = completed.artifacts?.speakerLabels
        assertEquals(mapOf("spk_1" to "客户", "spk_2" to "销售"), labels)
        assertEquals(1, orchestrator.callCount)
        assertEquals(true, orchestrator.lastRequest?.force)
    }

    @Test
    fun resultLinks_includeCustomPromptUrl() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf("CustomPrompt" to "https://example.com/custom_prompt")
            )
        )
        val fetcher = object : TingwuArtifactFetcher {
            override fun fetchText(url: String, timeoutMs: Int, maxChars: Int): String? {
                return """
                    {
                      "CustomPrompt": [
                        {"Name": "demo", "Result": "定制结果文本", "Truncated": false}
                      ]
                    }
                """.trimIndent()
            }
        }
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "你好",
                    segments = emptyList(),
                    speakers = emptyList(),
                    language = "zh",
                    duration = 1.0
                ),
                resultLinks = mapOf("CustomPrompt" to "https://example.com/custom_prompt"),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            transcriptOrchestrator = transcriptOrchestrator,
            metaHub = InMemoryMetaHub(),
            tingwuTraceStore = traceStore,
            artifactFetcher = fetcher,
            postTingwuTranscriptEnhancer = noopEnhancer,
            aiParaSettingsProvider = defaultSettingsProvider,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10, tingwuPollTimeoutMillis = 200))
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertEquals("https://example.com/custom_prompt", completed.artifacts?.customPromptUrl)
        assertTrue(completed.transcriptMarkdown.contains("逐字稿"))
        assertFalse(completed.transcriptMarkdown.contains("自定义转写"))
        assertFalse(completed.transcriptMarkdown.contains("定制结果文本"))
    }

    @Test
    fun summarization_invalidJson_doesNotLeakRawPayload() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf("Summarization" to "https://example.com/summarization")
            )
        )
        val fetcher = object : TingwuArtifactFetcher {
            override fun fetchText(url: String, timeoutMs: Int, maxChars: Int): String? {
                return """{"UnknownKey": ["raw1", "raw2"]}"""
            }
        }
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 1.0
                ),
                resultLinks = mapOf("Summarization" to "https://example.com/summarization"),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            transcriptOrchestrator = transcriptOrchestrator,
            metaHub = InMemoryMetaHub(),
            tingwuTraceStore = traceStore,
            artifactFetcher = fetcher,
            postTingwuTranscriptEnhancer = noopEnhancer,
            aiParaSettingsProvider = defaultSettingsProvider,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10, tingwuPollTimeoutMillis = 200))
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertTrue(completed.transcriptMarkdown.contains("逐字稿"))
        assertFalse(completed.transcriptMarkdown.contains("摘要"))
        assertFalse(completed.transcriptMarkdown.contains("{"))
    }

    @Test
    fun summarization_schemaRendersMarkdown() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf("Summarization" to "https://example.com/summarization")
            )
        )
        val fetcher = object : TingwuArtifactFetcher {
            override fun fetchText(url: String, timeoutMs: Int, maxChars: Int): String? {
                return """
                    {
                      "TaskId":"job-1",
                      "Summarization":{
                        "ParagraphTitle":"标题一",
                        "ParagraphSummary":"段落摘要内容",
                        "ConversationalSummary":[{"SpeakerId":"spk1","Summary":"发言人总结1"}],
                        "QuestionsAnsweringSummary":[{"Question":"问什么","Answer":"答这个"}]
                      }
                    }
                """.trimIndent()
            }
        }
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 1.0
                ),
                resultLinks = mapOf("Summarization" to "https://example.com/summarization"),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            transcriptOrchestrator = transcriptOrchestrator,
            metaHub = InMemoryMetaHub(),
            tingwuTraceStore = traceStore,
            artifactFetcher = fetcher,
            postTingwuTranscriptEnhancer = noopEnhancer,
            aiParaSettingsProvider = defaultSettingsProvider,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10, tingwuPollTimeoutMillis = 200))
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val markdown = completed.transcriptMarkdown
        assertTrue(markdown.contains("逐字稿"))
        assertFalse(markdown.contains("摘要（Summarization）"))
        assertFalse(markdown.contains("发言人总结"))
    }

    @Test
    fun autoChapters_schemaRendersMarkdown() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf("AutoChapters" to "https://example.com/autochapters")
            )
        )
        val fetcher = object : TingwuArtifactFetcher {
            override fun fetchText(url: String, timeoutMs: Int, maxChars: Int): String? {
                return """
                    {
                      "TaskId":"job-1",
                      "AutoChapters":[
                        {"Id":1,"Start":4480,"Headline":"新手摩托车选择","Summary":"概览说明"}
                      ]
                    }
                """.trimIndent()
            }
        }
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 1.0
                ),
                resultLinks = mapOf("AutoChapters" to "https://example.com/autochapters"),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            transcriptOrchestrator = transcriptOrchestrator,
            metaHub = InMemoryMetaHub(),
            tingwuTraceStore = traceStore,
            artifactFetcher = fetcher,
            postTingwuTranscriptEnhancer = noopEnhancer,
            aiParaSettingsProvider = defaultSettingsProvider,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10, tingwuPollTimeoutMillis = 200))
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val markdown = completed.transcriptMarkdown
        assertTrue(markdown.contains("逐字稿"))
        assertFalse(markdown.contains("章节（AutoChapters）"))
    }

    @Test
    fun customPrompt_stageDirectionMovesToTopOnce() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(
            statusResponse(
                status = "SUCCEEDED",
                progress = 100,
                resultLinks = mapOf("CustomPrompt" to "https://example.com/custom_prompt")
            )
        )
        val fetcher = object : TingwuArtifactFetcher {
            override fun fetchText(url: String, timeoutMs: Int, maxChars: Int): String? {
                return "[开场寒暄：双方互致问候]\n正文第一行\n[开场寒暄：双方互致问候]\n正文第二行"
            }
        }
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "正文",
                    segments = null,
                    speakers = null,
                    language = "zh",
                    duration = 1.0
                ),
                resultLinks = mapOf("CustomPrompt" to "https://example.com/custom_prompt"),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = RealTingwuCoordinator(
            dispatchers = dispatchers,
            api = api,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            transcriptOrchestrator = transcriptOrchestrator,
            metaHub = InMemoryMetaHub(),
            tingwuTraceStore = traceStore,
            artifactFetcher = fetcher,
            postTingwuTranscriptEnhancer = noopEnhancer,
            aiParaSettingsProvider = defaultSettingsProvider,
            optionalConfig = Optional.of(AiCoreConfig(tingwuPollIntervalMillis = 10, tingwuPollTimeoutMillis = 200))
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val markdown = completed.transcriptMarkdown
        assertTrue(markdown.contains("逐字稿"))
        assertFalse(markdown.contains("自定义转写"))
    }

    private class RecordingTranscriptOrchestrator(
        var result: TranscriptMetadata? = null,
        var throwable: Throwable? = null
    ) : TranscriptOrchestrator {
        var lastRequest: TranscriptMetadataRequest? = null
        var callCount: Int = 0

        override suspend fun inferTranscriptMetadata(request: TranscriptMetadataRequest): TranscriptMetadata? {
            callCount++
            lastRequest = request
            throwable?.let { throw it }
            return result
        }
    }

    private class FakeTingwuApi : TingwuApi {
        private val statusQueue = ConcurrentLinkedQueue<TingwuStatusResponse>()
        var resultData: TingwuResultResponse? = null
        var failResultWith404: Boolean = false
        var lastCreateRequest: TingwuCreateTaskRequest? = null

        fun enqueueStatus(data: TingwuStatusResponse) {
            statusQueue += data
        }

        override suspend fun createTranscriptionTask(
            type: String,
            operation: String?,
            body: TingwuCreateTaskRequest
        ): TingwuCreateTaskResponse {
            lastCreateRequest = body
            return TingwuCreateTaskResponse(
                requestId = "req-create",
                code = "0",
                message = "Success",
                data = TingwuCreateTaskData(
                    taskId = "job-1",
                    taskKey = body.input.taskKey,
                    taskStatus = "QUEUED",
                    meetingJoinUrl = null
                )
            )
        }

        override suspend fun getTaskStatus(
            taskId: String,
            type: String
        ): TingwuStatusResponse {
            return statusQueue.poll()
                ?: statusResponse(
                    status = "FAILED",
                    progress = 0,
                    errorCode = "Empty",
                    errorMessage = "状态为空"
                )
        }

        override suspend fun getTaskResult(
            taskId: String,
            format: String
        ): TingwuResultResponse {
            if (failResultWith404) {
                throw HttpException(
                    Response.error<TingwuResultResponse>(
                        404,
                        "Not Found".toResponseBody("application/json".toMediaType())
                    )
                )
            }
            return resultData
                ?: TingwuResultResponse(
                    requestId = "req-result-empty",
                    code = "0",
                    message = "Success",
                    data = TingwuResultData(
                        taskId = taskId,
                        transcription = TingwuTranscription(
                            text = "",
                            segments = emptyList(),
                            speakers = emptyList(),
                            language = "zh",
                            duration = 0.0
                        ),
                        resultLinks = emptyMap(),
                        outputMp3Path = null,
                        outputMp4Path = null,
                        outputThumbnailPath = null,
                        outputSpectrumPath = null
                    )
                )
        }
    }

    @Test
    fun parseAutoChaptersPayload_parsesList() {
        val json = """
            {
              "Chapters": [
                {"Title": "开场", "Start": 0, "End": 12.5},
                {"Title": "报价讨论", "StartTime": 42, "EndTime": 85}
              ]
            }
        """.trimIndent()
        val chapters = parseAutoChaptersPayload(json)
        assertEquals(2, chapters.size)
        assertEquals("开场", chapters.first().title)
        assertEquals(0L, chapters.first().startMs)
    }

    @Test
    fun parseSmartSummaryPayload_parsesSummaryAndLists() {
        val json = """
            {
              "Summary": "会议概览",
              "KeyPoints": ["要点1", "要点2"],
              "ActionItems": ["行动A"]
            }
        """.trimIndent()
        val summary = parseSmartSummaryPayload(json)
        assertEquals("会议概览", summary?.summary)
        assertEquals(listOf("要点1", "要点2"), summary?.keyPoints)
        assertEquals(listOf("行动A"), summary?.actionItems)
    }

    @Test
    fun transcriptBubble_doesNotIncludeCustomPromptOrSummarySections() = runTest(dispatcher) {
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "你好",
                    segments = listOf(
                        TingwuTranscriptSegment(id = 1, start = 0.0, end = 1.0, text = "你好", speaker = "spk_1")
                    ),
                    speakers = listOf(TingwuSpeaker(id = "spk_1", name = "客户")),
                    language = "zh",
                    duration = 1.0
                ),
                resultLinks = mapOf(
                    "CustomPrompt" to "https://example.com/custom_prompt",
                    "Summarization" to "https://example.com/summarization",
                    "AutoChapters" to "https://example.com/autochapters"
                ),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            )
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val markdown = completed.transcriptMarkdown
        assertTrue(markdown.contains("逐字稿"))
        assertFalse(markdown.contains("自定义转写"))
        assertFalse(markdown.contains("Summarization"))
        assertFalse(markdown.contains("AutoChapters"))
    }

    @Test
    fun enhancerEnabled_appliesSplitAndKeepsOrdering() = runTest(dispatcher) {
        val settingsProvider = FakeAiParaSettingsProvider(
            AiParaSettingsSnapshot(
                tingwu = AiParaSettingsSnapshot().tingwu.copy(
                    postTingwuEnhancer = PostTingwuEnhancerSettings(enabled = true)
                )
            )
        )
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "",
                    segments = listOf(
                        TingwuTranscriptSegment(id = 1, start = 0.0, end = 1.0, text = "开场介绍", speaker = "spk_1"),
                        TingwuTranscriptSegment(id = 2, start = 1.0, end = 2.0, text = "补充需求", speaker = "spk_2")
                    ),
                    speakers = listOf(
                        TingwuSpeaker(id = "spk_1", name = "销售"),
                        TingwuSpeaker(id = "spk_2", name = "客户")
                    ),
                    language = "zh",
                    duration = 3.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = emptyMap(),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val enhancer = object : PostTingwuTranscriptEnhancer {
            override suspend fun enhance(input: EnhancerInput): EnhancerOutput = EnhancerOutput(
                speakerRoster = listOf(SpeakerLabel("spk_1", "销售顾问", 0.9)),
                utteranceEdits = listOf(
                    UtteranceEdit(
                        index = 0,
                        split = listOf(
                            SplitLine("销售顾问", "开场介绍"),
                            SplitLine("客户", "补充需求")
                        ),
                        confidence = 0.9
                    )
                )
            )
        }
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            ),
            enhancer = enhancer,
            settingsProvider = settingsProvider
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        val lines = completed.transcriptMarkdown.lines()
        assertTrue(lines.any { it.startsWith("- [00:00] 销售顾问：开场介绍") })
        assertTrue(lines.any { it.trimStart().startsWith("客户：补充需求") && !it.contains("[") })
    }

    @Test
    fun enhancerFailure_fallsBackToRawMarkdown() = runTest(dispatcher) {
        val settingsProvider = FakeAiParaSettingsProvider(
            AiParaSettingsSnapshot(
                tingwu = AiParaSettingsSnapshot().tingwu.copy(
                    postTingwuEnhancer = PostTingwuEnhancerSettings(enabled = true)
                )
            )
        )
        val api = FakeTingwuApi()
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 30))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))
        api.resultData = TingwuResultResponse(
            requestId = "req-result",
            code = "0",
            message = "Success",
            data = TingwuResultData(
                taskId = "job-1",
                transcription = TingwuTranscription(
                    text = "",
                    segments = listOf(
                        TingwuTranscriptSegment(id = 1, start = 0.0, end = 1.0, text = "原始内容", speaker = "spk_1")
                    ),
                    speakers = listOf(TingwuSpeaker(id = "spk_1", name = "客户")),
                    language = "zh",
                    duration = 2.0,
                    url = "https://example.com/transcription.json"
                ),
                resultLinks = emptyMap(),
                outputMp3Path = null,
                outputMp4Path = null,
                outputThumbnailPath = null,
                outputSpectrumPath = null
            )
        )
        val enhancer = object : PostTingwuTranscriptEnhancer {
            override suspend fun enhance(input: EnhancerInput): EnhancerOutput? {
                throw IllegalStateException("boom")
            }
        }
        val coordinator = newCoordinator(
            api = api,
            optionalConfig = Optional.of(
                AiCoreConfig(
                    tingwuPollIntervalMillis = 10,
                    tingwuPollTimeoutMillis = 200
                )
            ),
            enhancer = enhancer,
            settingsProvider = settingsProvider
        )

        val submit = coordinator.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )
        assertTrue(submit is Result.Success)
        val jobId = (submit as Result.Success).data
        advanceTimeBy(20)
        advanceUntilIdle()

        val completed = coordinator.observeJob(jobId).first { it is TingwuJobState.Completed } as TingwuJobState.Completed
        assertTrue(completed.transcriptMarkdown.contains("原始内容"))
        assertTrue(completed.transcriptMarkdown.contains("客户"))
    }

    private class FakeAiParaSettingsProvider(
        private var snapshot: AiParaSettingsSnapshot = AiParaSettingsSnapshot()
    ) : AiParaSettingsProvider {
        fun override(snapshot: AiParaSettingsSnapshot) {
            this.snapshot = snapshot
        }

        override fun snapshot(): AiParaSettingsSnapshot = snapshot
    }
}

private fun statusResponse(
    status: String,
    progress: Int,
    errorCode: String? = null,
    errorMessage: String? = null,
    resultLinks: Map<String, String>? = null
): TingwuStatusResponse = TingwuStatusResponse(
    requestId = "req-$status-$progress",
    code = "0",
    message = "Success",
    data = TingwuStatusData(
        taskId = "job-1",
        taskKey = "job-1",
        taskStatus = status,
        taskProgress = progress,
        errorCode = errorCode,
        errorMessage = errorMessage,
        outputMp3Path = null,
        outputMp4Path = null,
        outputThumbnailPath = null,
        outputSpectrumPath = null,
        resultLinks = resultLinks,
        meetingJoinUrl = null
    )
)
