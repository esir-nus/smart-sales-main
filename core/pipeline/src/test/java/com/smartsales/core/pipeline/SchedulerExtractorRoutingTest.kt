package com.smartsales.core.pipeline

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.LlmProfile
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.scheduler.FollowUpRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.SchedulerLinter
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import com.smartsales.prism.domain.scheduler.UniBExtractionRequest
import com.smartsales.prism.domain.scheduler.UniCExtractionRequest
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class SchedulerExtractorRoutingTest {

    private lateinit var executor: RecordingExecutor
    private lateinit var promptCompiler: RecordingPromptCompiler
    private lateinit var schedulerLinter: SchedulerLinter

    @Before
    fun setup() {
        executor = RecordingExecutor()
        promptCompiler = RecordingPromptCompiler()
        schedulerLinter = mock()
    }

    @Test
    fun `uni a extraction uses scheduler extractor profile`() = kotlinx.coroutines.test.runTest {
        promptCompiler.uniAPrompt = "uni-a-prompt"

        val service = RealUniAExtractionService(executor, promptCompiler, schedulerLinter)
        service.extract(
            UniAExtractionRequest(
                transcript = "下周三早上8点提醒我买机票",
                nowIso = "2026-04-17T06:00:00Z",
                timezone = "Asia/Shanghai",
                unifiedId = "uni-a-1"
            )
        )

        assertEquals(ModelRegistry.SCHEDULER_EXTRACTOR, executor.lastProfile)
        assertEquals("uni-a-prompt", executor.lastPrompt)
    }

    @Test
    fun `uni b extraction uses scheduler extractor profile`() = kotlinx.coroutines.test.runTest {
        promptCompiler.uniBPrompt = "uni-b-prompt"

        val service = RealUniBExtractionService(executor, promptCompiler, schedulerLinter)
        service.extract(
            UniBExtractionRequest(
                transcript = "明天提醒我买机票",
                nowIso = "2026-04-17T06:00:00Z",
                timezone = "Asia/Shanghai",
                unifiedId = "uni-b-1"
            )
        )

        assertEquals(ModelRegistry.SCHEDULER_EXTRACTOR, executor.lastProfile)
        assertEquals("uni-b-prompt", executor.lastPrompt)
    }

    @Test
    fun `uni c extraction uses scheduler extractor profile`() = kotlinx.coroutines.test.runTest {
        promptCompiler.uniCPrompt = "uni-c-prompt"

        val service = RealUniCExtractionService(executor, promptCompiler, schedulerLinter)
        service.extract(
            UniCExtractionRequest(
                transcript = "以后想试试晨跑",
                nowIso = "2026-04-17T06:00:00Z",
                timezone = "Asia/Shanghai",
                unifiedId = "uni-c-1"
            )
        )

        assertEquals(ModelRegistry.SCHEDULER_EXTRACTOR, executor.lastProfile)
        assertEquals("uni-c-prompt", executor.lastPrompt)
    }

    @Test
    fun `global reschedule extraction uses scheduler extractor profile`() = kotlinx.coroutines.test.runTest {
        promptCompiler.globalReschedulePrompt = "global-reschedule-prompt"

        val service = RealGlobalRescheduleExtractionService(executor, promptCompiler, schedulerLinter)
        service.extract(
            GlobalRescheduleExtractionRequest(
                transcript = "把拿快递改到今天下午四点",
                nowIso = "2026-04-17T06:00:00Z",
                timezone = "Asia/Shanghai"
            )
        )

        assertEquals(ModelRegistry.SCHEDULER_EXTRACTOR, executor.lastProfile)
        assertEquals("global-reschedule-prompt", executor.lastPrompt)
    }

    @Test
    fun `follow up reschedule extraction uses scheduler extractor profile`() = kotlinx.coroutines.test.runTest {
        promptCompiler.followUpReschedulePrompt = "follow-up-prompt"

        val service = RealFollowUpRescheduleExtractionService(executor, promptCompiler, schedulerLinter)
        service.extract(
            FollowUpRescheduleExtractionRequest(
                transcript = "明天早上八点",
                nowIso = "2026-04-17T06:00:00Z",
                timezone = "Asia/Shanghai",
                selectedTaskStartIso = "2026-04-17T08:00:00+08:00",
                selectedTaskDurationMinutes = 0,
                selectedTaskTitle = "拿快递"
            )
        )

        assertEquals(ModelRegistry.SCHEDULER_EXTRACTOR, executor.lastProfile)
        assertEquals("follow-up-prompt", executor.lastPrompt)
    }

    private class RecordingExecutor : Executor {
        var lastProfile: LlmProfile? = null
        var lastPrompt: String? = null

        override suspend fun execute(profile: LlmProfile, prompt: String): ExecutorResult {
            lastProfile = profile
            lastPrompt = prompt
            return ExecutorResult.Failure("test failure")
        }
    }

    private class RecordingPromptCompiler : PromptCompiler() {
        var uniAPrompt: String = "uni-a"
        var uniBPrompt: String = "uni-b"
        var uniCPrompt: String = "uni-c"
        var globalReschedulePrompt: String = "global-reschedule"
        var followUpReschedulePrompt: String = "follow-up-reschedule"

        override fun compileUniAExtractionPrompt(request: UniAExtractionRequest): String = uniAPrompt

        override fun compileUniBExtractionPrompt(request: UniBExtractionRequest): String = uniBPrompt

        override fun compileUniCExtractionPrompt(request: UniCExtractionRequest): String = uniCPrompt

        override fun compileGlobalRescheduleExtractionPrompt(
            request: GlobalRescheduleExtractionRequest
        ): String = globalReschedulePrompt

        override fun compileFollowUpRescheduleExtractionPrompt(
            request: FollowUpRescheduleExtractionRequest
        ): String = followUpReschedulePrompt
    }
}
