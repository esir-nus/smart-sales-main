package com.smartsales.core.pipeline.habit

import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.context.ModeMetadata
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.core.pipeline.RealHabitListener
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.rl.ObservationSource
import com.smartsales.prism.domain.rl.ReinforcementLearner
import com.smartsales.prism.domain.rl.RlObservation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import com.smartsales.core.llm.TokenUsage

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

        val context = EnhancedContext(
            userText = "我喜欢早上开会",
            modeMetadata = ModeMetadata(Mode.ANALYST, "test", 0),
            sessionHistory = emptyList()
        )

        listener.analyzeAsync("我喜欢早上开会", context, testScope)

        val captor = argumentCaptor<List<RlObservation>>()
        verify(reinforcementLearner).processObservations(captor.capture())

        val obs = captor.firstValue.first()
        assertEquals(null, obs.entityId)
        assertEquals("preferred_time", obs.key)
        assertEquals("morning", obs.value)
        assertEquals(ObservationSource.USER_POSITIVE, obs.source)
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
}
