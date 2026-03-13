package com.smartsales.core.pipeline

import com.smartsales.core.test.fakes.FakeUnifiedPipeline
import com.smartsales.core.test.fakes.FakeMascotService
import com.smartsales.core.test.fakes.FakeContextBuilder
import com.smartsales.core.test.fakes.FakeLightningRouter
import com.smartsales.core.test.fakes.FakeScheduleBoard
import com.smartsales.core.test.fakes.FakeEntityWriter
import com.smartsales.core.test.fakes.FakeAliasCache
import com.smartsales.prism.domain.scheduler.FakeScheduledTaskRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IntentOrchestratorBreakItTest {

    private lateinit var orchestrator: IntentOrchestrator
    private lateinit var fakeContextBuilder: FakeContextBuilder
    private lateinit var fakeLightningRouter: FakeLightningRouter
    private lateinit var fakeMascotService: FakeMascotService
    private lateinit var fakeUnifiedPipeline: FakeUnifiedPipeline
    private lateinit var fakeScheduledTaskRepository: FakeScheduledTaskRepository
    private lateinit var fakeScheduleBoard: FakeScheduleBoard
    private lateinit var fakeEntityWriter: FakeEntityWriter
    private lateinit var fakeAliasCache: FakeAliasCache
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setup() {
        fakeContextBuilder = FakeContextBuilder()
        fakeLightningRouter = FakeLightningRouter()
        fakeMascotService = FakeMascotService()
        fakeUnifiedPipeline = FakeUnifiedPipeline()
        fakeScheduledTaskRepository = FakeScheduledTaskRepository()
        fakeScheduleBoard = FakeScheduleBoard()
        fakeEntityWriter = FakeEntityWriter()
        fakeAliasCache = FakeAliasCache()

        orchestrator = IntentOrchestrator(
            contextBuilder = fakeContextBuilder,
            lightningRouter = fakeLightningRouter,
            mascotService = fakeMascotService,
            unifiedPipeline = fakeUnifiedPipeline,
            scheduledTaskRepository = fakeScheduledTaskRepository,
            scheduleBoard = fakeScheduleBoard,
            entityWriter = fakeEntityWriter,
            aliasCache = fakeAliasCache,
            appScope = testScope
        )
    }

    @Test
    fun testEmptyInput() = runTest {
        // Break-it: Send an empty string
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.VAGUE, false, "输入为空"))
        val result = orchestrator.processInput("").firstOrNull()
        assertTrue(result is PipelineResult.ConversationalReply)
    }

    @Test
    fun testBlankInput() = runTest {
        // Break-it: Send a blank string
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.VAGUE, false, "未检测到有效输入"))
        val result = orchestrator.processInput("   ").firstOrNull()
        assertTrue(result is PipelineResult.ConversationalReply)
    }

    @Test
    fun testMaxLenInput() = runTest {
        // Break-it: Extremely long string
        val longString = "开会".repeat(5000)
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.DEEP_ANALYSIS, true, ""))
        
        // This should not crash the orchestrator
        val result = orchestrator.processInput(longString).firstOrNull()
        assertNull(result) // fake emits nothing
    }

    @Test
    fun testEmojiSpecialCharacters() = runTest {
        // Break-it: Emojis and special chars
        val weirdString = "👍 帮我是！！！🚨@#%@#%开会"
        fakeLightningRouter.enqueueResult(RouterResult(QueryQuality.NOISE, true, ""))
        val result = orchestrator.processInput(weirdString).firstOrNull()
        assertEquals(PipelineResult.MascotIntercepted, result)
    }
}
