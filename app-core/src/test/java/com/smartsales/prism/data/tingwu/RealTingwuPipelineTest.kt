package com.smartsales.prism.data.tingwu

import com.google.gson.Gson
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.TingwuCredentials
import com.smartsales.data.aicore.TingwuCredentialsProvider
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.params.TingwuSettings
import com.smartsales.data.aicore.params.TingwuTranscriptionSettings
import com.smartsales.data.aicore.tingwu.api.TingwuApi
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskData
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskResponse
import com.smartsales.data.aicore.tingwu.api.TingwuResultResponse
import com.smartsales.data.aicore.tingwu.api.TingwuStatusData
import com.smartsales.data.aicore.tingwu.api.TingwuStatusResponse
import com.smartsales.data.aicore.tingwu.identity.TingwuIdentityContentHint
import com.smartsales.data.aicore.tingwu.identity.TingwuIdentityHint
import com.smartsales.data.aicore.tingwu.identity.TingwuIdentityHintResolver
import com.smartsales.prism.domain.tingwu.TingwuJobState
import com.smartsales.prism.domain.tingwu.TingwuRequest
import com.smartsales.prism.ui.sim.buildSpeakerAwareTranscript
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealTingwuPipelineTest {

    private class LocalDispatcherProvider(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
        override val io: CoroutineDispatcher = dispatcher
        override val main: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }

    private class LocalSettingsProvider(
        private val snapshot: AiParaSettingsSnapshot = AiParaSettingsSnapshot()
    ) : AiParaSettingsProvider {
        override fun snapshot(): AiParaSettingsSnapshot = snapshot
    }

    private class LocalFakeTingwuApi : TingwuApi {
        var lastCreateRequest: TingwuCreateTaskRequest? = null
        var statusResponse: TingwuStatusResponse? = null

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

        override suspend fun getTaskStatus(taskId: String, type: String): TingwuStatusResponse {
            statusResponse?.let { return it }
            return TingwuStatusResponse(
                requestId = "req-status",
                code = "0",
                message = "Success",
                data = TingwuStatusData(
                    taskId = taskId,
                    taskKey = taskId,
                    taskStatus = "FAILED",
                    taskProgress = 0,
                    errorCode = "TEST",
                    errorMessage = "stop polling",
                    outputMp3Path = null,
                    outputMp4Path = null,
                    outputThumbnailPath = null,
                    outputSpectrumPath = null,
                    resultLinks = emptyMap(),
                    meetingJoinUrl = null
                )
            )
        }

        override suspend fun getTaskResult(taskId: String, format: String): TingwuResultResponse {
            throw NotImplementedError("Not needed for request test")
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = LocalDispatcherProvider(dispatcher)

    @Test
    fun `submit includes identity recognition and key information in live SIM request`() = runTest(dispatcher) {
        val api = LocalFakeTingwuApi()
        val pipeline = RealTingwuPipeline(
            api = api,
            credentialsProvider = object : TingwuCredentialsProvider {
                override fun obtain(): TingwuCredentials = TingwuCredentials(
                    apiKey = "api-key",
                    baseUrl = "https://tingwu.test/",
                    appKey = "app-key",
                    accessKeyId = "id",
                    accessKeySecret = "secret",
                    securityToken = null,
                    model = "tingwu-test"
                )
            },
            aiParaSettingsProvider = LocalSettingsProvider(),
            identityHintResolver = object : TingwuIdentityHintResolver {
                override suspend fun resolveCurrentHint(): TingwuIdentityHint = TingwuIdentityHint(
                    enabled = true,
                    sceneIntroduction = "汽车销售沟通场景，围绕车型介绍、价格和成交推进展开。",
                    identityContents = listOf(
                        TingwuIdentityContentHint("销售顾问", "负责介绍方案和价格"),
                        TingwuIdentityContentHint("客户", "提出需求并做决策")
                    )
                )
            },
            dispatchers = dispatchers
        )

        val result = pipeline.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )

        assertTrue(result is Result.Success)
        val request = api.lastCreateRequest
        requireNotNull(request)
        assertEquals(true, request.parameters.identityRecognitionEnabled)
        assertEquals(
            "汽车销售沟通场景，围绕车型介绍、价格和成交推进展开。",
            request.parameters.identityRecognition?.sceneIntroduction
        )
        val identityNames = request.parameters.identityRecognition?.identityContents?.map { content -> content.name }
        assertEquals(
            listOf("销售顾问", "客户"),
            identityNames
        )
        assertEquals(
            listOf("Actions", "KeyInformation"),
            request.parameters.meetingAssistance?.types
        )
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"IdentityRecognitionEnabled\":true"))
        assertTrue(json.contains("\"DiarizationEnabled\":true"))
        assertTrue(json.contains("\"SpeakerCount\":0"))
        assertTrue(json.contains("\"MeetingAssistance\":{\"Types\":[\"Actions\",\"KeyInformation\"]}"))
    }

    @Test
    fun `submit omits diarization when central settings disable speaker separation`() = runTest(dispatcher) {
        val api = LocalFakeTingwuApi()
        val pipeline = createPipeline(
            api = api,
            settingsProvider = LocalSettingsProvider(
                AiParaSettingsSnapshot(
                    tingwu = TingwuSettings(
                        transcription = TingwuTranscriptionSettings(diarizationEnabled = false)
                    )
                )
            )
        )

        val result = pipeline.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )

        assertTrue(result is Result.Success)
        val transcription = api.lastCreateRequest?.parameters?.transcription
        assertEquals(false, transcription?.diarizationEnabled)
        assertEquals(null, transcription?.diarization)
        val json = Gson().toJson(api.lastCreateRequest)
        assertTrue(!json.contains("\"Diarization\":{\"SpeakerCount\""))
    }

    @Test
    fun `submit omits incomplete identity recognition hints`() = runTest(dispatcher) {
        val api = LocalFakeTingwuApi()
        val pipeline = createPipeline(
            api = api,
            identityHintResolver = object : TingwuIdentityHintResolver {
                override suspend fun resolveCurrentHint(): TingwuIdentityHint = TingwuIdentityHint(
                    enabled = true,
                    sceneIntroduction = "",
                    identityContents = listOf(TingwuIdentityContentHint("客户", "提出需求"))
                )
            }
        )

        val result = pipeline.submit(
            TingwuRequest(
                audioAssetName = "demo.wav",
                fileUrl = "https://oss.example.com/demo.wav"
            )
        )

        assertTrue(result is Result.Success)
        val request = api.lastCreateRequest
        assertEquals(false, request?.parameters?.identityRecognitionEnabled)
        assertEquals(null, request?.parameters?.identityRecognition)
        val json = Gson().toJson(request)
        assertTrue(json.contains("\"IdentityRecognitionEnabled\":false"))
        assertTrue(!json.contains("\"IdentityRecognition\":{\"SceneIntroduction\""))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `completed paragraph transcription renders speaker-aware transcript`() = runTest(dispatcher) {
        val server = startJsonServer(
            """
                {
                  "Data": {
                    "Transcription": {
                      "Text": "你好罗总\n欢迎光临",
                      "Paragraphs": [
                        {
                          "Words": [
                            {"SentenceId": 1, "Text": "你好", "Start": 0, "End": 250, "SpeakerId": "spk_1"},
                            {"SentenceId": 1, "Text": "罗总", "Start": 250, "End": 600, "SpeakerId": "spk_1"},
                            {"SentenceId": 2, "Text": "欢迎", "Start": 800, "End": 1100, "SpeakerId": "spk_2"},
                            {"SentenceId": 2, "Text": "光临", "Start": 1100, "End": 1500, "SpeakerId": "spk_2"}
                          ]
                        }
                      ]
                    },
                    "IdentityRecognition": [
                      {"SpeakerId": "spk_1", "Identity": {"Name": "客户"}},
                      {"SpeakerId": "spk_2", "Identity": {"Name": "销售顾问"}}
                    ]
                  }
                }
            """.trimIndent()
        )
        try {
            val api = LocalFakeTingwuApi()
            api.statusResponse = TingwuStatusResponse(
                requestId = "req-status",
                code = "0",
                message = "Success",
                data = TingwuStatusData(
                    taskId = "job-1",
                    taskKey = "job-1",
                    taskStatus = "COMPLETED",
                    taskProgress = 100,
                    errorCode = null,
                    errorMessage = null,
                    outputMp3Path = null,
                    outputMp4Path = null,
                    outputThumbnailPath = null,
                    outputSpectrumPath = null,
                    resultLinks = mapOf("Transcription" to server.url),
                    meetingJoinUrl = null
                )
            )
            val pipeline = createPipeline(api = api)

            val result = pipeline.submit(
                TingwuRequest(
                    audioAssetName = "demo.wav",
                    fileUrl = "https://oss.example.com/demo.wav"
                )
            )

            assertTrue(result is Result.Success)
            val jobId = (result as Result.Success).data
            val completed = async {
                pipeline.observeJob(jobId)
                    .first { it is TingwuJobState.Completed } as TingwuJobState.Completed
            }
            advanceTimeBy(5_100)
            advanceUntilIdle()

            val artifacts = completed.await().artifacts
            requireNotNull(artifacts)
            assertEquals(2, artifacts.diarizedSegments?.size)
            assertEquals("客户", artifacts.speakerLabels["spk_1"])
            assertEquals("销售顾问", artifacts.speakerLabels["spk_2"])
            assertEquals(
                "客户：你好罗总\n销售顾问：欢迎光临",
                buildSpeakerAwareTranscript(artifacts)
            )
        } finally {
            server.stop()
        }
    }

    private fun createPipeline(
        api: TingwuApi = LocalFakeTingwuApi(),
        settingsProvider: AiParaSettingsProvider = LocalSettingsProvider(),
        identityHintResolver: TingwuIdentityHintResolver = object : TingwuIdentityHintResolver {
            override suspend fun resolveCurrentHint(): TingwuIdentityHint = TingwuIdentityHint(enabled = false)
        }
    ): RealTingwuPipeline {
        return RealTingwuPipeline(
            api = api,
            credentialsProvider = object : TingwuCredentialsProvider {
                override fun obtain(): TingwuCredentials = TingwuCredentials(
                    apiKey = "api-key",
                    baseUrl = "https://tingwu.test/",
                    appKey = "app-key",
                    accessKeyId = "id",
                    accessKeySecret = "secret",
                    securityToken = null,
                    model = "tingwu-test"
                )
            },
            aiParaSettingsProvider = settingsProvider,
            identityHintResolver = identityHintResolver,
            dispatchers = dispatchers
        )
    }

    private data class JsonServer(
        val socket: ServerSocket,
        val url: String
    ) {
        fun stop() {
            runCatching { socket.close() }
        }
    }

    private fun startJsonServer(body: String): JsonServer {
        val socket = ServerSocket(0)
        thread(start = true, isDaemon = true) {
            runCatching {
                socket.accept().use { client ->
                    client.getInputStream().bufferedReader().readLine()
                    val bytes = body.toByteArray(Charsets.UTF_8)
                    val header = buildString {
                        append("HTTP/1.1 200 OK\r\n")
                        append("Content-Type: application/json; charset=utf-8\r\n")
                        append("Content-Length: ${bytes.size}\r\n")
                        append("Connection: close\r\n")
                        append("\r\n")
                    }.toByteArray(Charsets.UTF_8)
                    client.getOutputStream().use { output ->
                        output.write(header)
                        output.write(bytes)
                    }
                }
            }
        }
        return JsonServer(
            socket = socket,
            url = "http://127.0.0.1:${socket.localPort}/transcription.json"
        )
    }
}
