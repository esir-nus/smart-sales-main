package com.smartsales.domain.prism.core

import com.smartsales.domain.prism.core.fakes.FakeContextBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * ContextBuilder 接口契约测试
 */
class ContextBuilderTest {
    
    private val builder: ContextBuilder = FakeContextBuilder()
    
    @Test
    fun `buildContext returns EnhancedContext with userText`() = runTest {
        val input = "用户输入文本"
        val context = builder.buildContext(input, Mode.COACH)
        
        assertEquals(input, context.userText)
    }
    
    @Test
    fun `buildContext includes mode`() = runTest {
        val context = builder.buildContext("test", Mode.ANALYST)
        
        assertEquals(Mode.ANALYST, context.mode)
    }
    
    @Test
    fun `buildContext includes dummy memoryHits`() = runTest {
        val context = builder.buildContext("test", Mode.COACH)
        
        assertFalse(context.memoryHits.isEmpty())
    }
    
    @Test
    fun `buildContext includes userProfile`() = runTest {
        val context = builder.buildContext("test", Mode.COACH)
        
        assertNotNull(context.userProfile)
        assertTrue(context.userProfile?.displayName?.isNotEmpty() == true)
    }
    
    @Test
    fun `buildContext includes userHabits`() = runTest {
        val context = builder.buildContext("test", Mode.COACH)
        
        assertFalse(context.userHabits.isEmpty())
    }
}
