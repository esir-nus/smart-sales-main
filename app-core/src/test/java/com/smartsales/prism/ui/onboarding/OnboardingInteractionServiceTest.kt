package com.smartsales.prism.ui.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingInteractionServiceTest {

    private val service = RealOnboardingInteractionService()

    @Test
    fun `consultation reply uses budget branch deterministically`() = runTest {
        val result = service.generateConsultationReply(
            transcript = "客户觉得我们的价格有点贵，预算还没批下来",
            round = 1
        )

        assertTrue(result is OnboardingConsultationServiceResult.Success)
        result as OnboardingConsultationServiceResult.Success
        assertEquals("先别急着压价格，先确认客户卡的是预算、审批，还是还没看清长期价值。", result.reply)
    }

    @Test
    fun `profile extraction parses explicit fields deterministically`() = runTest {
        val result = service.extractProfile(
            transcript = "我是王经理，做 SaaS 销售总监 8 年了，平时主要用微信联系客户。"
        )

        assertTrue(result is OnboardingProfileExtractionServiceResult.Success)
        result as OnboardingProfileExtractionServiceResult.Success
        assertEquals("王经理", result.draft.displayName)
        assertEquals("销售总监", result.draft.role)
        assertEquals("SaaS", result.draft.industry)
        assertEquals("8年", result.draft.experienceYears)
        assertEquals("微信", result.draft.communicationPlatform)
    }
}
