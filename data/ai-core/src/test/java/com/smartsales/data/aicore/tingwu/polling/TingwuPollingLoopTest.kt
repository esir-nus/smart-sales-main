package com.smartsales.data.aicore.tingwu.polling

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.FakeTingwuApi
import com.smartsales.data.aicore.OssSignedUrlProvider
import com.smartsales.data.aicore.TingwuCredentials
import com.smartsales.data.aicore.TingwuCredentialsProvider
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.debug.PipelineEvent
import com.smartsales.data.aicore.debug.PipelineStage
import com.smartsales.data.aicore.debug.PipelineTracer
import com.smartsales.data.aicore.statusResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Optional

@ExperimentalCoroutinesApi
class TingwuPollingLoopTest {

    private lateinit var api: FakeTingwuApi
    private lateinit var repository: TingwuRunnerRepository
    private lateinit var config: AiCoreConfig
    private lateinit var loop: TingwuPollingLoop
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        api = FakeTingwuApi()
        config = AiCoreConfig()
        setupLoop(api)
    }

    private fun setupLoop(tingwuApi: FakeTingwuApi) {
        val credentialsProvider = object : TingwuCredentialsProvider {
             override fun obtain() = TingwuCredentials("","","","","","","") 
        }
        val signedUrlProvider = object : OssSignedUrlProvider {
            override suspend fun generate(key: String, exp: Long) = Result.Success("url")
        }
        val dispatchers = object : DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }
        
        repository = TingwuRunnerRepository(
            api = tingwuApi,
            credentialsProvider = credentialsProvider,
            signedUrlProvider = signedUrlProvider,
            dispatchers = dispatchers,
            config = config
        )

        loop = TingwuPollingLoop(
            repository = repository,
            optionalConfig = Optional.of(config),
            dispatchers = dispatchers
        )
    }

    @Test
    fun `poll_successfulJob_emitsCompleted`() = runTest(testDispatcher) {
        val jobId = "job_123"
        val flow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)
        
        // Mock successful response
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 50))
        api.enqueueStatus(statusResponse(status = "SUCCEEDED", progress = 100))

        loop.poll(jobId, flow) { state ->
            assertTrue(state is TingwuJobState.Completed)
        }
        
        // Advance time to allow polling to run
        advanceTimeBy(1000)

        assertTrue(flow.value is TingwuJobState.Completed)
    }

    @Test
    fun `poll_failedJob_emitsFailed`() = runTest(testDispatcher) {
        val jobId = "job_fail"
        val flow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)

        // Mock failed response from API
        api.enqueueStatus(statusResponse(status = "PROCESSING", progress = 50))
        api.enqueueStatus(statusResponse(
            status = "FAILED", 
            progress = 60,
            errorCode = "ERR_001",
            errorMessage = "Processing failed"
        ))

        loop.poll(jobId, flow) { state ->
            assertTrue(state is TingwuJobState.Failed)
        }
        
        advanceTimeBy(1000)
        
        assertTrue(flow.value is TingwuJobState.Failed)
        val failed = flow.value as TingwuJobState.Failed
        assertEquals("Processing failed", failed.reason)
    }

    @Test
    fun `poll_retryExhausted_emitsFailed`() = runTest(testDispatcher) {
        val jobId = "job_retry_fail"
        val flow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)
        
        val failureApi = object : FakeTingwuApi() {
            override suspend fun getTaskStatus(taskId: String, type: String): com.smartsales.data.aicore.tingwu.api.TingwuStatusResponse {
                // Simulate exception
                throw RuntimeException("Network Error")
            }
        }
        setupLoop(failureApi)

        loop.poll(jobId, flow) { state ->
             assertTrue(state is TingwuJobState.Failed)
        }

        advanceTimeBy(5000)

        assertTrue(flow.value is TingwuJobState.Failed)
    }

    @Test
    fun `poll_timeout_emitsFailed`() = runTest(testDispatcher) {
        val jobId = "job_timeout"
        val flow = MutableStateFlow<TingwuJobState>(TingwuJobState.Idle)
        
        config = AiCoreConfig(
           tingwuPollIntervalMillis = 100,
           tingwuPollTimeoutMillis = 200 
        )
        // Need to pass config to loop
        setupLoop(api)

        val slowApi = object : FakeTingwuApi() {
             override suspend fun getTaskStatus(taskId: String, type: String) = statusResponse(status = "PROCESSING", progress = 10)
        }
        setupLoop(slowApi)

        loop.poll(jobId, flow) { state ->
             assertTrue(state is TingwuJobState.Failed)
        }

        advanceTimeBy(500) 

        assertTrue(flow.value is TingwuJobState.Failed)
        val failed = flow.value as TingwuJobState.Failed
        assertEquals(AiCoreErrorReason.TIMEOUT, failed.error.reason)
    }
}
