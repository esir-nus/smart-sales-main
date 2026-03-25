// File: data/ai-core/src/test/java/com/smartsales/data/aicore/tingwu/submission/TingwuSubmissionServiceTest.kt
// Module: :data:ai-core
// Summary: Unit tests for TingwuSubmissionService Lattice box
// Author: created on 2026-01-15

package com.smartsales.data.aicore.tingwu.submission

import com.google.gson.Gson
import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.TingwuCredentials
import com.smartsales.data.aicore.TingwuCredentialsProvider
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.tingwu.api.TingwuApi
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskData
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskResponse
import com.smartsales.data.aicore.tingwu.api.TingwuResultResponse
import com.smartsales.data.aicore.tingwu.api.TingwuStatusResponse
import com.smartsales.data.aicore.tingwu.identity.TingwuIdentityContentHint
import com.smartsales.data.aicore.tingwu.identity.TingwuIdentityHint
import com.smartsales.data.aicore.tingwu.identity.TingwuIdentityHintResolver
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TingwuSubmissionServiceTest: Tests the TingwuSubmissionService Lattice box.
 * 
 * Tests: API call, settings resolution, error handling.
 * Dependencies: FakeTingwuApi, FakeAiParaSettingsProvider, FakeCredentialsProvider
 */
class TingwuSubmissionServiceTest {
    
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = FakeDispatcherProvider(dispatcher)
    private val traceStore = TingwuTraceStore()
    private val gson = Gson()
    
    // ==================== Fakes ====================
    
    private class FakeCredentialsProvider(
        private var credentials: TingwuCredentials = TingwuCredentials(
            apiKey = "test-api-key",
            baseUrl = "https://tingwu.test/",
            appKey = "test-app-key",
            accessKeyId = "test-id",
            accessKeySecret = "test-secret",
            securityToken = null,
            model = "tingwu-test"
        )
    ) : TingwuCredentialsProvider {
        override fun obtain(): TingwuCredentials = credentials
        
        fun setCredentials(creds: TingwuCredentials) {
            credentials = creds
        }
    }
    
    private class FakeSettingsProvider(
        private var snapshot: AiParaSettingsSnapshot = AiParaSettingsSnapshot()
    ) : AiParaSettingsProvider {
        override fun snapshot(): AiParaSettingsSnapshot = snapshot
        
        fun setSnapshot(s: AiParaSettingsSnapshot) {
            snapshot = s
        }
    }
    
    private class FakeTingwuApiForSubmission : TingwuApi {
        var stubResponse: TingwuCreateTaskResponse? = null
        var stubError: Throwable? = null
        val createCalls = mutableListOf<TingwuCreateTaskRequest>()
        
        override suspend fun createTranscriptionTask(
            type: String,
            operation: String?,
            body: TingwuCreateTaskRequest
        ): TingwuCreateTaskResponse {
            createCalls.add(body)
            stubError?.let { throw it }
            return stubResponse ?: TingwuCreateTaskResponse(
                requestId = "req-${System.currentTimeMillis()}",
                code = "0",
                message = "Success",
                data = TingwuCreateTaskData(
                    taskId = "task-${createCalls.size}",
                    taskKey = body.input.taskKey,
                    taskStatus = "CREATED",
                    meetingJoinUrl = null
                )
            )
        }
        
        override suspend fun getTaskStatus(
            taskId: String,
            type: String
        ): TingwuStatusResponse = throw NotImplementedError("Not used in submission tests")
        
        override suspend fun getTaskResult(
            taskId: String,
            format: String
        ): TingwuResultResponse = throw NotImplementedError("Not used in submission tests")
        
        fun reset() {
            stubResponse = null
            stubError = null
            createCalls.clear()
        }
    }

    private class FakeIdentityHintResolver(
        var hint: TingwuIdentityHint = TingwuIdentityHint(
            enabled = true,
            sceneIntroduction = "汽车销售沟通场景，围绕车型介绍、价格和成交推进展开。",
            identityContents = listOf(
                TingwuIdentityContentHint("销售顾问", "负责介绍方案和价格"),
                TingwuIdentityContentHint("客户", "提出需求并做决策"),
                TingwuIdentityContentHint("其他参会人", "陪同人员或同事")
            )
        )
    ) : TingwuIdentityHintResolver {
        override suspend fun resolveCurrentHint(): TingwuIdentityHint = hint
    }
    
    // ==================== Factory ====================
    
    private fun createService(
        api: TingwuApi = FakeTingwuApiForSubmission(),
        credentialsProvider: TingwuCredentialsProvider = FakeCredentialsProvider(),
        settingsProvider: AiParaSettingsProvider = FakeSettingsProvider(),
        identityHintResolver: TingwuIdentityHintResolver = FakeIdentityHintResolver()
    ): TingwuSubmissionService {
        return RealTingwuSubmissionService(
            api = api,
            credentialsProvider = credentialsProvider,
            aiParaSettingsProvider = settingsProvider,
            identityHintResolver = identityHintResolver,
            tingwuTraceStore = traceStore,
            dispatchers = dispatchers,
            gson = gson
        )
    }
    
    // ==================== Tests ====================
    
    @Test
    fun submit_callsApiWithCorrectParameters() = runTest(dispatcher) {
        val api = FakeTingwuApiForSubmission()
        val service = createService(api = api)
        
        val input = SubmissionInput(
            fileUrl = "https://oss.example.com/audio.wav",
            taskKey = "test-task-123",
            sourceLanguage = "cn",
            diarizationEnabled = true
        )
        
        val result = service.submit(input)
        
        assertTrue("Expected success, got $result", result is Result.Success)
        assertEquals(1, api.createCalls.size)
        
        val request = api.createCalls[0]
        assertEquals("test-app-key", request.appKey)
        assertEquals("https://oss.example.com/audio.wav", request.input.fileUrl)
        assertEquals("test-task-123", request.input.taskKey)
        assertEquals("cn", request.input.sourceLanguage)
        assertEquals(true, request.parameters.identityRecognitionEnabled)
        assertEquals("汽车销售沟通场景，围绕车型介绍、价格和成交推进展开。", request.parameters.identityRecognition?.sceneIntroduction)
        assertEquals(listOf("销售顾问", "客户", "其他参会人"), request.parameters.identityRecognition?.identityContents?.map { it.name })
    }
    
    @Test
    fun submit_returnsTaskIdOnSuccess() = runTest(dispatcher) {
        val api = FakeTingwuApiForSubmission()
        api.stubResponse = TingwuCreateTaskResponse(
            requestId = "req-test",
            code = "0",
            message = "Success",
            data = TingwuCreateTaskData(
                taskId = "tingwu-job-456",
                taskKey = "key",
                taskStatus = "CREATED",
                meetingJoinUrl = null
            )
        )
        val service = createService(api = api)
        
        val result = service.submit(
            SubmissionInput(
                fileUrl = "https://oss.example.com/audio.wav",
                taskKey = "key",
                sourceLanguage = "cn"
            )
        )
        
        assertTrue(result is Result.Success)
        assertEquals("tingwu-job-456", (result as Result.Success).data.taskId)
        assertEquals("req-test", result.data.requestId)
    }
    
    @Test
    fun submit_whenApiReturnsErrorCode_returnsError() = runTest(dispatcher) {
        val api = FakeTingwuApiForSubmission()
        api.stubResponse = TingwuCreateTaskResponse(
            requestId = "req-fail",
            code = "40001",
            message = "Invalid audio format",
            data = null
        )
        val service = createService(api = api)
        
        val result = service.submit(
            SubmissionInput(
                fileUrl = "https://oss.example.com/bad.wav",
                taskKey = "key",
                sourceLanguage = "cn"
            )
        )
        
        assertTrue("Expected error, got $result", result is Result.Error)
        val error = (result as Result.Error).throwable as AiCoreException
        assertEquals(AiCoreErrorReason.REMOTE, error.reason)
        assertTrue(error.message?.contains("40001") == true)
    }
    
    @Test
    fun submit_whenApiThrows_returnsError() = runTest(dispatcher) {
        val api = FakeTingwuApiForSubmission()
        api.stubError = RuntimeException("Network error")
        val service = createService(api = api)
        
        val result = service.submit(
            SubmissionInput(
                fileUrl = "https://oss.example.com/audio.wav",
                taskKey = "key",
                sourceLanguage = "cn"
            )
        )
        
        assertTrue(result is Result.Error)
    }
    
    @Test
    fun submit_whenNullTaskId_returnsError() = runTest(dispatcher) {
        val api = FakeTingwuApiForSubmission()
        api.stubResponse = TingwuCreateTaskResponse(
            requestId = "req-test",
            code = "0",
            message = "Success",
            data = TingwuCreateTaskData(
                taskId = null,
                taskKey = "key",
                taskStatus = "CREATED",
                meetingJoinUrl = null
            )
        )
        val service = createService(api = api)
        
        val result = service.submit(
            SubmissionInput(
                fileUrl = "https://oss.example.com/audio.wav",
                taskKey = "key",
                sourceLanguage = "cn"
            )
        )
        
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable as AiCoreException
        assertTrue(error.message?.contains("TaskId") == true)
    }
    
    @Test
    fun submit_respectsDiarizationDisabled() = runTest(dispatcher) {
        val api = FakeTingwuApiForSubmission()
        val service = createService(api = api)
        
        service.submit(
            SubmissionInput(
                fileUrl = "https://oss.example.com/audio.wav",
                taskKey = "key",
                sourceLanguage = "cn",
                diarizationEnabled = false
            )
        )
        
        assertEquals(1, api.createCalls.size)
        val params = api.createCalls[0].parameters
        assertEquals(false, params.transcription?.diarizationEnabled)
    }

    @Test
    fun submit_whenIdentityHintDisabled_omitsIdentityRecognitionObject() = runTest(dispatcher) {
        val api = FakeTingwuApiForSubmission()
        val identityResolver = FakeIdentityHintResolver(
            hint = TingwuIdentityHint(enabled = false)
        )
        val service = createService(
            api = api,
            identityHintResolver = identityResolver
        )

        service.submit(
            SubmissionInput(
                fileUrl = "https://oss.example.com/audio.wav",
                taskKey = "key",
                sourceLanguage = "cn"
            )
        )

        val request = api.createCalls.single()
        assertEquals(false, request.parameters.identityRecognitionEnabled)
        assertEquals(null, request.parameters.identityRecognition)
        val json = gson.toJson(request)
        assertTrue(json.contains("\"IdentityRecognitionEnabled\":false"))
        assertTrue(!json.contains("\"IdentityRecognition\":{\"SceneIntroduction\""))
    }
}
