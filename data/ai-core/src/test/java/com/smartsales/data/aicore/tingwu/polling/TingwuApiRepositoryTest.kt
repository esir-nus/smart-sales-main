package com.smartsales.data.aicore.tingwu.polling

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.TingwuRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for TingwuApiRepository Fake contract verification.
 */
class TingwuApiRepositoryTest {

    private lateinit var repository: FakeTingwuApiRepository

    @Before
    fun setup() {
        repository = FakeTingwuApiRepository()
    }

    @Test
    fun `pollWithRetry tracks call history`() = runTest {
        repository.stubPollError = RuntimeException("Test error")

        val result = runCatching { repository.pollWithRetry("job-1") }

        assertTrue(result.isFailure)
        assertTrue(repository.pollCalls.contains("job-1"))
    }

    @Test
    fun `resolveFileUrl returns stubbed result`() = runTest {
        repository.stubFileUrl = Result.Success("https://custom.oss/audio.wav")

        val result = repository.resolveFileUrl(
            TingwuRequest(audioAssetName = "test.wav", fileUrl = "https://example.com/test.wav")
        )

        assertTrue(result is Result.Success)
        assertEquals("https://custom.oss/audio.wav", (result as Result.Success).data)
    }

    @Test
    fun `buildTaskKey returns stubbed key`() {
        repository.stubTaskKey = "custom_key_123"

        val key = repository.buildTaskKey(
            TingwuRequest(audioAssetName = "test.wav", fileUrl = "https://example.com/test.wav")
        )

        assertEquals("custom_key_123", key)
    }

    @Test
    fun `reset clears all state`() = runTest {
        repository.stubTaskKey = "custom"
        repository.stubLanguage = "en"
        repository.pollCalls.add("job-1")

        repository.reset()

        assertEquals("fake_task_key", repository.stubTaskKey)
        assertEquals("cn", repository.stubLanguage)
        assertTrue(repository.pollCalls.isEmpty())
    }
}
