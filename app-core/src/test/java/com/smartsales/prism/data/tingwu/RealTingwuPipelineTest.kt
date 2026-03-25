package com.smartsales.prism.data.tingwu

import com.google.gson.Gson
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.TingwuCredentials
import com.smartsales.data.aicore.TingwuCredentialsProvider
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
import com.smartsales.prism.domain.tingwu.TingwuRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private class LocalFakeTingwuApi : TingwuApi {
        var lastCreateRequest: TingwuCreateTaskRequest? = null

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
        assertTrue(json.contains("\"MeetingAssistance\":{\"Types\":[\"Actions\",\"KeyInformation\"]}"))
    }
}
