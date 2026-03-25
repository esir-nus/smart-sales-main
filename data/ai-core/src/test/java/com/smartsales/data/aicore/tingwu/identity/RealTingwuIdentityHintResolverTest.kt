package com.smartsales.data.aicore.tingwu.identity

import com.smartsales.prism.data.fakes.FakeUserProfileRepository
import com.smartsales.prism.domain.memory.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealTingwuIdentityHintResolverTest {

    @Test
    fun `resolveFromProfile builds automotive sales hint from metadata`() = runTest {
        val repository = FakeUserProfileRepository()
        repository.updateProfile(
            repository.getProfile().copy(
                role = "销售经理",
                industry = "汽车",
                preferredLanguage = "zh-CN"
            )
        )
        val resolver = RealTingwuIdentityHintResolver(repository)

        val hint = resolver.resolveCurrentHint()

        assertTrue(hint.enabled)
        assertTrue(hint.sceneIntroduction.orEmpty().contains("汽车销售沟通场景"))
        assertEquals("销售经理", hint.identityContents.first().name)
        assertEquals(listOf("销售经理", "客户", "其他参会人"), hint.identityContents.map { it.name })
    }

    @Test
    fun `resolveFromProfile falls back to generic sales preset when metadata is weak`() {
        val resolver = RealTingwuIdentityHintResolver(FakeUserProfileRepository())
        val hint = resolver.resolveFromProfile(
            UserProfile(
                id = 0,
                displayName = "",
                role = "",
                industry = "",
                experienceLevel = "",
                updatedAt = 0L
            )
        )

        assertTrue(hint.enabled)
        assertTrue(hint.sceneIntroduction.orEmpty().contains("销售与客户沟通场景"))
        assertEquals(listOf("销售方", "客户", "其他参会人"), hint.identityContents.map { it.name })
    }
}
