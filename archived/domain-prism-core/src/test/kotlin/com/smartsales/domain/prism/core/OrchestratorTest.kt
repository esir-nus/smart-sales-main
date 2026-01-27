package com.smartsales.domain.prism.core

import com.smartsales.domain.prism.core.fakes.FakeOrchestrator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Orchestrator 接口契约测试
 * 使用 FakeOrchestrator 验证 interface 约定
 */
class OrchestratorTest {
    
    private val orchestrator: Orchestrator = FakeOrchestrator()
    
    @Test
    fun `currentMode initial value is COACH`() = runTest {
        val mode = orchestrator.currentMode.first()
        assertEquals(Mode.COACH, mode)
    }
    
    @Test
    fun `switchMode updates currentMode flow`() = runTest {
        orchestrator.switchMode(Mode.ANALYST)
        val mode = orchestrator.currentMode.first()
        assertEquals(Mode.ANALYST, mode)
    }
    
    @Test
    fun `processUserIntent returns result with current mode`() = runTest {
        val result = orchestrator.processUserIntent("测试输入")
        
        assertEquals(Mode.COACH, result.mode)
        assertNotNull(result.executorResult)
        assertTrue(result.executorResult.displayContent.isNotEmpty())
    }
    
    @Test
    fun `processUserIntent reflects mode after switch`() = runTest {
        orchestrator.switchMode(Mode.SCHEDULER)
        val result = orchestrator.processUserIntent("安排会议")
        
        assertEquals(Mode.SCHEDULER, result.mode)
    }
    
    @Test
    fun `processUserIntent includes input in response`() = runTest {
        val input = "特定测试输入"
        val result = orchestrator.processUserIntent(input)
        
        assertTrue(result.executorResult.displayContent.contains(input))
    }
}
