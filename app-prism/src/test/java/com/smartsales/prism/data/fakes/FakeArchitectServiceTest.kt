package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.pipeline.EnhancedContext

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeArchitectServiceTest {

    private lateinit var service: FakeArchitectService

    @Before
    fun setup() {
        service = FakeArchitectService()
    }

    @Test
    fun `generatePlan returns structured PlanResult`() = runTest {
        val input = "分析大客户"
        val context = createEmptyContext()
        val history = emptyList<ChatTurn>()

        val result = service.generatePlan(input, context, history)

        assertEquals("📊 客户流失风险分析计划", result.title)
        assertTrue(result.summary.contains("三个维度"))
    }

    @Test
    fun `investigate returns Analysis and Workflows`() = runTest {
        val inputPlan = service.generatePlan("test", createEmptyContext(), emptyList())
        val context = createEmptyContext()
        val history = emptyList<ChatTurn>()

        val result = service.investigate(inputPlan, context, history)

        assertTrue(result.analysisContent.contains("深度分析结果"))
        assertEquals(2, result.suggestedWorkflows.size)
        assertEquals("EXPORT_CSV", result.suggestedWorkflows[0].workflowId)
    }

    private fun createEmptyContext() = EnhancedContext(userText = "test")
}
