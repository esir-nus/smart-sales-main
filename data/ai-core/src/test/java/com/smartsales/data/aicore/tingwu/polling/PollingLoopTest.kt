// File: data/ai-core/src/test/java/com/smartsales/data/aicore/tingwu/polling/PollingLoopTest.kt
// Module: :data:ai-core
// Summary: Unit tests for PollingLoop Lattice box
// Author: created on 2026-01-15

package com.smartsales.data.aicore.tingwu.polling

import com.smartsales.core.test.FakeDispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.OssSignedUrlProvider
import com.smartsales.data.aicore.TingwuCredentials
import com.smartsales.data.aicore.TingwuCredentialsProvider
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.tingwu.api.TingwuApi
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskResponse
import com.smartsales.data.aicore.tingwu.api.TingwuResultResponse
import com.smartsales.data.aicore.tingwu.api.TingwuStatusData
import com.smartsales.data.aicore.tingwu.api.TingwuStatusResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Optional

/**
 * PollingLoopTest: Tests the PollingLoop Lattice box.
 * 
 * Tests: Status transitions, progress emission, timeout, error handling.
 * Uses FakeTingwuApi → real TingwuRunnerRepository → TingwuPollingLoop.
 */
class PollingLoopTest {
    
    private val dispatcher = StandardTestDispatcher()
    private val dispatchers = FakeDispatcherProvider(dispatcher)
    
    // ==================== Fakes ====================
    
    private class FakeTingwuApiForPolling : TingwuApi {
        private val statusQueue = mutableListOf<TingwuStatusResponse>()
        var stubError: Throwable? = null
        val pollCalls = mutableListOf<String>()
        
        fun enqueueStatus(status: String, progress: Int = 50, errorCode: String? = null, errorMessage: String? = null) {
            statusQueue.add(
                TingwuStatusResponse(
                    requestId = "req-${statusQueue.size}",
                    code = "0",
                    message = "Success",
                    data = TingwuStatusData(
                        taskId = "job-1",
                        taskKey = "key",
                        taskStatus = status,
                        taskProgress = progress,
                        errorCode = errorCode,
                        errorMessage = errorMessage,
                        outputMp3Path = null,
                        outputMp4Path = null,
                        outputThumbnailPath = null,
                        outputSpectrumPath = null,
                        resultLinks = null,
                        meetingJoinUrl = null
                    )
                )
            )
        }
        
        override suspend fun getTaskStatus(taskId: String, type: String): TingwuStatusResponse {
            pollCalls.add(taskId)
            stubError?.let { throw it }
            return if (statusQueue.isNotEmpty()) {
                statusQueue.removeAt(0)
            } else {
                TingwuStatusResponse(
                    requestId = "req-default",
                    code = "0",
                    message = "Success",
                    data = TingwuStatusData(
                        taskId = taskId,
                        taskKey = "key",
                        taskStatus = "PROCESSING",
                        taskProgress = 50,
                        errorCode = null,
                        errorMessage = null,
                        outputMp3Path = null,
                        outputMp4Path = null,
                        outputThumbnailPath = null,
                        outputSpectrumPath = null,
                        resultLinks = null,
                        meetingJoinUrl = null
                    )
                )
            }
        }
        
        override suspend fun createTranscriptionTask(
            type: String, operation: String?, body: TingwuCreateTaskRequest
        ): TingwuCreateTaskResponse = throw NotImplementedError("Not used in polling tests")
        
        override suspend fun getTaskResult(
            taskId: String, format: String
        ): TingwuResultResponse = throw NotImplementedError("Not used in polling tests")
        
        fun reset() {
            statusQueue.clear()
            stubError = null
            pollCalls.clear()
        }
    }
    
    private val fakeCredentialsProvider = object : TingwuCredentialsProvider {
        override fun obtain() = TingwuCredentials(
            apiKey = "key", baseUrl = "http://test/", appKey = "app",
            accessKeyId = "id", accessKeySecret = "secret", securityToken = null, model = "model"
        )
    }
    
    private val fakeSignedUrlProvider = object : OssSignedUrlProvider {
        override suspend fun generate(objectKey: String, expiresInSeconds: Long) = Result.Success("http://test/$objectKey")
    }
    
    // ==================== Factory ====================
    
    private fun createPollingLoop(
        api: FakeTingwuApiForPolling,
        pollIntervalMs: Long = 100,
        pollTimeoutMs: Long = 5000,
        initialDelayMs: Long = 0
    ): TingwuPollingLoop {
        val config = AiCoreConfig(
            tingwuPollIntervalMillis = pollIntervalMs,
            tingwuPollTimeoutMillis = pollTimeoutMs,
            tingwuInitialPollDelayMillis = initialDelayMs
        )
        val repository = RealTingwuApiRepository(
            api = api,
            credentialsProvider = fakeCredentialsProvider,
            signedUrlProvider = fakeSignedUrlProvider,
            dispatchers = dispatchers,
            config = config
        )
        return TingwuPollingLoop(
            repository = repository,
            optionalConfig = Optional.of(config),
            dispatchers = dispatchers
        )
    }
    
    // ==================== Tests ====================
    
    @Test
    fun poll_emitsInProgressThenCompleted() = runTest(dispatcher) {
        val api = FakeTingwuApiForPolling()
        api.enqueueStatus("PROCESSING", progress = 30)
        api.enqueueStatus("PROCESSING", progress = 60)
        api.enqueueStatus("SUCCEEDED", progress = 100)
        
        val loop = createPollingLoop(api)
        val stateFlow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)
        var terminalState: TingwuJobState? = null
        
        loop.poll("job-1", stateFlow) { state -> terminalState = state }
        advanceUntilIdle()
        
        assertTrue("Expected Completed, got $terminalState", terminalState is TingwuJobState.Completed)
        assertEquals(3, api.pollCalls.size)
    }
    
    @Test
    fun poll_emitsFailedOnApiError() = runTest(dispatcher) {
        val api = FakeTingwuApiForPolling()
        api.enqueueStatus("FAILED", progress = 50, errorCode = "10001", errorMessage = "Audio too short")
        
        val loop = createPollingLoop(api)
        val stateFlow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)
        var terminalState: TingwuJobState? = null
        
        loop.poll("job-1", stateFlow) { state -> terminalState = state }
        advanceUntilIdle()
        
        assertTrue("Expected Failed, got $terminalState", terminalState is TingwuJobState.Failed)
        val failed = terminalState as TingwuJobState.Failed
        assertTrue(failed.error.message?.contains("Audio too short") == true)
    }
    
    @Test
    fun poll_emitsFailedOnTimeout() = runTest(dispatcher) {
        val api = FakeTingwuApiForPolling()
        repeat(100) { api.enqueueStatus("PROCESSING", progress = 50) }
        
        val loop = createPollingLoop(api, pollIntervalMs = 50, pollTimeoutMs = 200)
        val stateFlow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)
        var terminalState: TingwuJobState? = null
        
        loop.poll("job-1", stateFlow) { state -> terminalState = state }
        advanceTimeBy(500)
        advanceUntilIdle()
        
        assertTrue("Expected Failed due to timeout, got $terminalState", terminalState is TingwuJobState.Failed)
        val failed = terminalState as TingwuJobState.Failed
        assertEquals(AiCoreErrorReason.TIMEOUT, failed.error.reason)
    }
    
    @Test
    fun poll_emitsFailedOnNetworkException() = runTest(dispatcher) {
        val api = FakeTingwuApiForPolling()
        api.stubError = RuntimeException("Network error")
        
        val loop = createPollingLoop(api)
        val stateFlow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)
        var terminalState: TingwuJobState? = null
        
        loop.poll("job-1", stateFlow) { state -> terminalState = state }
        advanceUntilIdle()
        
        assertTrue("Expected Failed on exception, got $terminalState", terminalState is TingwuJobState.Failed)
    }
    
    @Test
    fun poll_updatesStateFlowWithProgress() = runTest(dispatcher) {
        val api = FakeTingwuApiForPolling()
        api.enqueueStatus("PROCESSING", progress = 25)
        api.enqueueStatus("PROCESSING", progress = 75)
        api.enqueueStatus("SUCCEEDED", progress = 100)
        
        val loop = createPollingLoop(api)
        val stateFlow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)
        
        loop.poll("job-1", stateFlow) { /* terminal */ }
        advanceUntilIdle()
        
        assertTrue(stateFlow.value is TingwuJobState.Completed)
    }
}
