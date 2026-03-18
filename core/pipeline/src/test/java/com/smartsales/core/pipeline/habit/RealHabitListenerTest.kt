package com.smartsales.core.pipeline.habit

import com.smartsales.core.context.ChatTurn
import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.context.ModeMetadata
import com.smartsales.core.context.ToolArtifact
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.TokenUsage
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.core.pipeline.RealHabitListener
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.memory.EntityRef
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import com.smartsales.prism.domain.scheduler.SchedulerPatternContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class RealHabitListenerTest {

    private lateinit var executor: Executor
    private lateinit var reinforcementLearner: ReinforcementLearner
    private lateinit var contextBuilder: com.smartsales.core.context.ContextBuilder
    private lateinit var listener: RealHabitListener
    
    private val dummyTokenUsage = TokenUsage(0, 0)
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        executor = mock()
        reinforcementLearner = mock()
        contextBuilder = mock()
        listener = RealHabitListener(executor, reinforcementLearner, contextBuilder)
    }

    @Test
    fun `analyzeAsync with valid observations calls reinforcementLearner`() = runTest {
        val mockJson = """
            ```json
            {
              "rl_observations": [
                {
                  "entityId": "null",
                  "key": "preferred_time",
                  "value": "morning",
                  "source": "USER_POSITIVE",
                  "evidence": "我喜欢早上开会"
                }
              ]
            }
            ```
        """.trimIndent()

        whenever(executor.execute(eq(ModelRegistry.EXTRACTOR), any())).thenReturn(ExecutorResult.Success(mockJson, dummyTokenUsage))
        val checkpoints = mutableListOf<PipelineValve.Checkpoint>()
        PipelineValve.testInterceptor = { checkpoint, _, _ ->
            checkpoints.add(checkpoint)
        }

        val context = EnhancedContext(
            userText = "我喜欢早上开会",
            modeMetadata = ModeMetadata(Mode.ANALYST, "test", 0),
            sessionHistory = emptyList()
        )

        try {
            listener.analyzeAsync("我喜欢早上开会", context, testScope)

            val captor = argumentCaptor<List<RlObservation>>()
            verify(reinforcementLearner).processObservations(captor.capture())
            verify(contextBuilder).applyHabitUpdates(any())

            val obs = captor.firstValue.first()
            assertEquals(null, obs.entityId)
            assertEquals("preferred_time", obs.key)
            assertEquals("morning", obs.value)
            assertEquals(ObservationSource.USER_POSITIVE, obs.source)
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.RL_LISTENER_TRIGGERED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.RL_EXTRACTION_EMITTED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.RL_PAYLOAD_DECODED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.RL_HABIT_WRITE_EXECUTED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.DB_WRITE_EXECUTED))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.RL_RAM_REFRESH_APPLIED))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `analyzeAsync with empty observations does nothing`() = runTest {
        val mockJson = """
            {
              "rl_observations": []
            }
        """.trimIndent()

        whenever(executor.execute(eq(ModelRegistry.EXTRACTOR), any())).thenReturn(ExecutorResult.Success(mockJson, dummyTokenUsage))

        val context = EnhancedContext(
            userText = "今天天气不错",
            modeMetadata = ModeMetadata(Mode.ANALYST, "test", 0),
            sessionHistory = emptyList()
        )

        listener.analyzeAsync("今天天气不错", context, testScope)

        verify(reinforcementLearner, never()).processObservations(any())
    }

    @Test
    fun `analyzeAsync with invalid JSON ignores gracefully`() = runTest {
        whenever(executor.execute(eq(ModelRegistry.EXTRACTOR), any())).thenReturn(ExecutorResult.Success("Not a json at all", dummyTokenUsage))

        val context = EnhancedContext(
            userText = "asdf",
            modeMetadata = ModeMetadata(Mode.ANALYST, "test", 0),
            sessionHistory = emptyList()
        )

        listener.analyzeAsync("asdf", context, testScope)

        verify(reinforcementLearner, never()).processObservations(any())
    }

    @Test
    fun `analyzeAsync uses bounded packet and summarized scheduler pattern signal only`() = runTest {
        val mockJson = """
            {
              "rl_observations": [
                {
                  "entityId": "null",
                  "key": "preferred_meeting_time",
                  "value": "morning",
                  "source": "INFERRED",
                  "evidence": "User keeps scheduling meetings in the morning"
                }
              ]
            }
        """.trimIndent()
        whenever(executor.execute(eq(ModelRegistry.EXTRACTOR), any())).thenReturn(ExecutorResult.Success(mockJson, dummyTokenUsage))
        val checkpoints = mutableListOf<PipelineValve.Checkpoint>()
        PipelineValve.testInterceptor = { checkpoint, _, _ ->
            checkpoints.add(checkpoint)
        }

        val context = EnhancedContext(
            userText = "Set another review tomorrow morning",
            modeMetadata = ModeMetadata(Mode.ANALYST, "test", 5),
            entityContext = mapOf(
                "Tom" to EntityRef(
                    entityId = "c-tom",
                    displayName = "Tom",
                    entityType = "CONTACT"
                )
            ),
            sessionHistory = listOf(
                ChatTurn("user", "oldest turn should be dropped"),
                ChatTurn("assistant", "Let us talk about tomorrow"),
                ChatTurn("user", "Set the meeting for Tom tomorrow morning"),
                ChatTurn("assistant", "Okay, I can do that"),
                ChatTurn("user", "No, keep it short next time")
            ),
            scheduleContext = "SCHEDULE_CONTEXT_SHOULD_NOT_APPEAR",
            schedulerPatternContext = SchedulerPatternContext(
                upcomingTaskCount = 3,
                preferredTimeWindow = "morning",
                preferredDurationMinutes = 30,
                leadTimeStyle = "next_day",
                urgencyStyle = "important_heavy"
            ),
            documentContext = "DOCUMENT_CONTEXT_SHOULD_NOT_APPEAR",
            lastToolResult = ToolArtifact(
                toolId = "DRAFT_EMAIL",
                title = "Draft Email",
                preview = "TOOL_ARTIFACT_SHOULD_NOT_APPEAR"
            )
        )

        try {
            listener.analyzeAsync("Set another review tomorrow morning", context, testScope)

            val promptCaptor = argumentCaptor<String>()
            verify(executor).execute(eq(ModelRegistry.EXTRACTOR), promptCaptor.capture())
            val prompt = promptCaptor.firstValue

            assertTrue(prompt.contains("Set another review tomorrow morning"))
            assertTrue(prompt.contains("ID: c-tom Name: Tom"))
            assertTrue(prompt.contains("[assistant] Let us talk about tomorrow"))
            assertTrue(prompt.contains("[user] Set the meeting for Tom tomorrow morning"))
            assertTrue(prompt.contains("[assistant] Okay, I can do that"))
            assertTrue(prompt.contains("Scheduler Pattern Signals:"))
            assertTrue(prompt.contains("preferred_time_window: morning"))
            assertTrue(prompt.contains("preferred_duration_minutes: 30"))
            assertTrue(prompt.contains("lead_time_style: next_day"))
            assertTrue(prompt.contains("urgency_style: important_heavy"))
            assertFalse(prompt.contains("oldest turn should be dropped"))
            assertFalse(prompt.contains("SCHEDULE_CONTEXT_SHOULD_NOT_APPEAR"))
            assertFalse(prompt.contains("DOCUMENT_CONTEXT_SHOULD_NOT_APPEAR"))
            assertFalse(prompt.contains("TOOL_ARTIFACT_SHOULD_NOT_APPEAR"))
            assertTrue(checkpoints.contains(PipelineValve.Checkpoint.RL_SCHEDULER_PATTERN_ATTACHED))
        } finally {
            PipelineValve.testInterceptor = null
        }
    }

    @Test
    fun `analyzeAsync drops client habit inference from scheduler pattern without active entity`() = runTest {
        val mockJson = """
            {
              "rl_observations": [
                {
                  "entityId": "c-tom",
                  "key": "preferred_meeting_style",
                  "value": "short",
                  "source": "INFERRED",
                  "evidence": "Scheduler pattern implies Tom prefers short meetings"
                }
              ]
            }
        """.trimIndent()
        whenever(executor.execute(eq(ModelRegistry.EXTRACTOR), any())).thenReturn(ExecutorResult.Success(mockJson, dummyTokenUsage))

        val context = EnhancedContext(
            userText = "Schedule another review",
            modeMetadata = ModeMetadata(Mode.ANALYST, "test", 2),
            sessionHistory = listOf(
                ChatTurn("user", "Schedule another review"),
                ChatTurn("assistant", "Okay")
            ),
            schedulerPatternContext = SchedulerPatternContext(
                upcomingTaskCount = 2,
                preferredTimeWindow = "morning",
                preferredDurationMinutes = 30,
                leadTimeStyle = "next_day",
                urgencyStyle = "important_heavy"
            )
        )

        listener.analyzeAsync("Schedule another review", context, testScope)

        verify(reinforcementLearner, never()).processObservations(any())
    }

    @Test
    fun `analyzeAsync drops client habit inference from scheduler pattern even with active entity present`() = runTest {
        val mockJson = """
            {
              "rl_observations": [
                {
                  "entityId": "c-tom",
                  "key": "preferred_meeting_style",
                  "value": "short",
                  "source": "INFERRED",
                  "evidence": "Scheduler pattern implies Tom prefers short meetings"
                }
              ]
            }
        """.trimIndent()
        whenever(executor.execute(eq(ModelRegistry.EXTRACTOR), any())).thenReturn(ExecutorResult.Success(mockJson, dummyTokenUsage))

        val context = EnhancedContext(
            userText = "Schedule another review",
            modeMetadata = ModeMetadata(Mode.ANALYST, "test", 2),
            entityContext = mapOf(
                "Tom" to EntityRef(
                    entityId = "c-tom",
                    displayName = "Tom",
                    entityType = "CONTACT"
                )
            ),
            sessionHistory = listOf(
                ChatTurn("user", "Schedule another review"),
                ChatTurn("assistant", "Okay")
            ),
            schedulerPatternContext = SchedulerPatternContext(
                upcomingTaskCount = 2,
                preferredTimeWindow = "morning",
                preferredDurationMinutes = 30,
                leadTimeStyle = "next_day",
                urgencyStyle = "important_heavy"
            )
        )

        listener.analyzeAsync("Schedule another review", context, testScope)

        verify(reinforcementLearner, never()).processObservations(any())
    }
}
