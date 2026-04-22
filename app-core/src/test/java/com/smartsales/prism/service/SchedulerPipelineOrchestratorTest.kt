package com.smartsales.prism.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times

@OptIn(ExperimentalCoroutinesApi::class)
class SchedulerPipelineOrchestratorTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var orchestrator: SchedulerPipelineOrchestrator

    @Before
    fun setup() {
        Dispatchers.setMain(mainDispatcher)
        context = mock()
        orchestrator = SchedulerPipelineOrchestrator(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `enqueue starts foreground service once and tracks pending work`() = runTest {
        mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            orchestrator.enqueue("log_20260421_090000.wav")

            assertTrue(orchestrator.hasPendingWork())
            assertEquals("log_20260421_090000.wav", orchestrator.receiveNext())
            mockedContextCompat.verify(
                { ContextCompat.startForegroundService(eq(context), any<Intent>()) },
                times(1)
            )
        }
    }

    @Test
    fun `duplicate enqueue is dropped when queued or in flight`() = runTest {
        mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            orchestrator.enqueue("log_20260421_090001.wav")
            orchestrator.enqueue("log_20260421_090001.wav")

            val first = orchestrator.receiveNext()
            assertEquals("log_20260421_090001.wav", first)
            assertEquals(
                null,
                withTimeoutOrNull(50) { orchestrator.receiveNext() }
            )

            orchestrator.onProcessingStarted(first)
            orchestrator.enqueue("log_20260421_090001.wav")
            assertEquals(
                null,
                withTimeoutOrNull(50) { orchestrator.receiveNext() }
            )

            mockedContextCompat.verify(
                { ContextCompat.startForegroundService(eq(context), any<Intent>()) },
                times(1)
            )
        }
    }

    @Test
    fun `finished work clears pending state and next enqueue restarts service after stop`() = runTest {
        mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            orchestrator.enqueue("log_20260421_090002.wav")
            val first = orchestrator.receiveNext()
            orchestrator.onProcessingStarted(first)
            orchestrator.onProcessingFinished(first)
            assertFalse(orchestrator.hasPendingWork())

            orchestrator.notifyServiceStopped()
            orchestrator.enqueue("log_20260421_090003.wav")

            assertEquals("log_20260421_090003.wav", orchestrator.receiveNext())
            mockedContextCompat.verify(
                { ContextCompat.startForegroundService(eq(context), any<Intent>()) },
                times(2)
            )
        }
    }
}
