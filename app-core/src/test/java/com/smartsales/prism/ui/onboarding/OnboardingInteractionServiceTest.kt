package com.smartsales.prism.ui.onboarding

import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.LlmProfile
import com.smartsales.core.llm.ModelRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingInteractionServiceTest {

    private val executor = FakeExecutor()
    private val service = RealOnboardingInteractionService(executor)

    @Test
    fun `consultation reply uses onboarding consultation model and trims llm output`() = runTest {
        executor.nextResult = ExecutorResult.Success("\"可以先从客户的预算卡点切入。\"")

        val result = service.generateConsultationReply(
            transcript = "客户觉得我们的价格有点贵，预算还没批下来",
            round = 1
        )

        assertTrue(result is OnboardingConsultationServiceResult.Success)
        result as OnboardingConsultationServiceResult.Success
        assertEquals("可以先从客户的预算卡点切入。", result.reply)
        assertEquals(ModelRegistry.ONBOARDING_CONSULTATION, executor.lastProfile)
    }

    @Test
    fun `profile extraction uses onboarding extraction model and parses structured json`() = runTest {
        executor.nextResult = ExecutorResult.Success(
            """
            {
              "acknowledgement": "谢谢您的分享，我已经记住了您的背景。",
              "displayName": "王经理",
              "role": "销售总监",
              "industry": "SaaS",
              "experienceYears": "8年",
              "communicationPlatform": "微信"
            }
            """.trimIndent()
        )

        val result = service.extractProfile(
            transcript = "我是王经理，做 SaaS 销售总监 8 年了，平时主要用微信联系客户。"
        )

        assertTrue(result is OnboardingProfileExtractionServiceResult.Success)
        result as OnboardingProfileExtractionServiceResult.Success
        assertEquals("谢谢您的分享，我已经记住了您的背景。", result.acknowledgement)
        assertEquals("王经理", result.draft.displayName)
        assertEquals("销售总监", result.draft.role)
        assertEquals("SaaS", result.draft.industry)
        assertEquals("8年", result.draft.experienceYears)
        assertEquals("微信", result.draft.communicationPlatform)
        assertEquals(ModelRegistry.ONBOARDING_PROFILE_EXTRACTION, executor.lastProfile)
    }

    @Test
    fun `profile extraction fails when llm json is malformed`() = runTest {
        executor.nextResult = ExecutorResult.Success("not-json")

        val result = service.extractProfile(
            transcript = "我是王经理，做 SaaS 销售总监 8 年了，平时主要用微信联系客户。"
        )

        assertTrue(result is OnboardingProfileExtractionServiceResult.Failure)
    }

    @Test
    fun `consultation reply does not enforce an internal timeout`() = runTest {
        executor.delayMillis = 1_500L
        executor.nextResult = ExecutorResult.Success("延迟后仍应成功")

        val result = service.generateConsultationReply(
            transcript = "客户觉得我们的价格有点贵，预算还没批下来",
            round = 1
        )

        assertTrue(result is OnboardingConsultationServiceResult.Success)
    }

    private class FakeExecutor : Executor {
        var nextResult: ExecutorResult = ExecutorResult.Success("")
        var delayMillis: Long = 0L
        var lastProfile: LlmProfile? = null

        override suspend fun execute(profile: LlmProfile, prompt: String): ExecutorResult {
            lastProfile = profile
            if (delayMillis > 0L) {
                delay(delayMillis)
            }
            return nextResult
        }
    }
}
