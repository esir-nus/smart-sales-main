package com.smartsales.prism.data.real

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.ContextDepth
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.ModeMetadata
import com.smartsales.prism.domain.unifiedpipeline.PipelineInput
import com.smartsales.prism.domain.unifiedpipeline.PipelineResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.mock as mockk

class RealUnifiedPipelineTest {

    private lateinit var pipeline: RealUnifiedPipeline
    private lateinit var contextBuilder: ContextBuilder

    private lateinit var entityDisambiguationService: com.smartsales.prism.domain.disambiguation.EntityDisambiguationService
    private lateinit var inputParserService: com.smartsales.prism.domain.parser.InputParserService
    private lateinit var entityWriter: com.smartsales.prism.domain.memory.EntityWriter
    private lateinit var schedulerLinter: com.smartsales.prism.domain.scheduler.SchedulerLinter
    private lateinit var scheduledTaskRepository: com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
    private lateinit var scheduleBoard: com.smartsales.prism.domain.memory.ScheduleBoard
    private lateinit var inspirationRepository: com.smartsales.prism.domain.scheduler.InspirationRepository
    private lateinit var alarmScheduler: com.smartsales.prism.domain.scheduler.AlarmScheduler

    @Before
    fun setup() {
        entityDisambiguationService = mock()
        inputParserService = mock()
        entityWriter = mock()
        schedulerLinter = mock()
        scheduledTaskRepository = mock()
        scheduleBoard = mock()
        inspirationRepository = mock()
        alarmScheduler = mock()
        // Create an explicit mock for ContextBuilder that simulates delay
        contextBuilder = mock {
            onBlocking { build(any(), any(), any(), any()) } doAnswer {
                // Simulate I/O latency to test parallel execution
                kotlinx.coroutines.runBlocking { delay(100) }
                EnhancedContext(
                    userText = "test",
                    modeMetadata = ModeMetadata(currentMode = Mode.ANALYST, sessionId = "session-1", turnIndex = 1),
                    sessionHistory = emptyList(),
                    currentDate = "2026-03-05",
                    currentInstant = 0L,
                    executedTools = emptySet()
                )
            }
        }
        
        pipeline = RealUnifiedPipeline(
            contextBuilder = contextBuilder,
            entityDisambiguationService = entityDisambiguationService,
            inputParserService = inputParserService,
            entityWriter = entityWriter,
            schedulerLinter = schedulerLinter,
            scheduledTaskRepository = scheduledTaskRepository,
            scheduleBoard = scheduleBoard,
            inspirationRepository = inspirationRepository,
            alarmScheduler = alarmScheduler
        )
    }

    @Test
    fun `processInput assembles parallel context and returns ConversationalReply`() = runTest {
        // Arrange
        val input = PipelineInput(rawText = "Please analyze my pipeline", isVoice = false)

        // Act
        val results = pipeline.processInput(input).toList()

        // Assert
        assertEquals(1, results.size)
        val result = results.first()
        assertTrue(result is PipelineResult.ConversationalReply)
        val reply = result as PipelineResult.ConversationalReply
        val expectedText = "Unified Pipeline ETL assembled successfully. Payload: Context [Mode: ANALYST, Metadata: User Metadata: Active]"
        assertTrue("Expected '$expectedText' but got '${reply.text}'", reply.text.contains(expectedText))
    }

    @Test
    fun `processInput with empty string handles gracefully (Break-It)`() = runTest {
        // Arrange
        val input = PipelineInput(rawText = "   ", isVoice = false)

        // Act
        val results = pipeline.processInput(input).toList()

        // Assert
        assertTrue(results.isNotEmpty())
    }
}
