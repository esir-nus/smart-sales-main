package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FakeOrchestrator 单元测试
 * 验证 Pipeline 核心契约
 */
class FakeOrchestratorTest {

    @Test
    fun `processInput returns Response for Coach mode`() = runTest {
        val orchestrator = FakeOrchestrator()
        
        val result = orchestrator.processInput("test input")
        
        assertTrue("Expected UiState.Response", result is UiState.Response)
    }

    @Test
    fun `switchMode updates currentMode`() = runTest {
        val orchestrator = FakeOrchestrator()
        
        orchestrator.switchMode(Mode.ANALYST)
        
        assertEquals(Mode.ANALYST, orchestrator.currentMode.value)
    }

    @Test
    fun `processInput returns PlanCard for Analyst mode`() = runTest {
        val orchestrator = FakeOrchestrator()
        orchestrator.switchMode(Mode.ANALYST)
        
        val result = orchestrator.processInput("analyze this")
        
        assertTrue("Expected UiState.PlanCard", result is UiState.PlanCard)
    }
}
