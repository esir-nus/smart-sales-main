package com.smartsales.data.aicore

import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.junit.Assert.*

/**
 * DashScope Client 集成测试
 * 
 * 需要有效的 DASHSCOPE_API_KEY 才能运行实际测试。
 * 标记为 @Ignore 避免 CI 失败。
 */
class DashscopeClientIntegrationTest {
    
    private val client = DefaultDashscopeClient()
    
    /**
     * 测试 enableThinking 思考痕迹提取
     * 
     * 需要设置环境变量 DASHSCOPE_API_KEY 或取消 @Ignore
     */
    @Test
    @Ignore("需要有效 API Key — 本地手动运行")
    fun `test enableThinking extracts thinkingTrace`() = runBlocking {
        // Arrange
        val apiKey = System.getenv("DASHSCOPE_API_KEY") 
            ?: BuildConfig.DASHSCOPE_API_KEY
        
        val request = DashscopeRequest(
            apiKey = apiKey,
            model = "qwen-plus",
            messages = listOf(
                DashscopeMessage(role = "user", content = "1+1等于多少？请详细解释推理过程。")
            ),
            temperature = 0.3f,
            enableThinking = true
        )
        
        // Act
        val completion = client.generate(request)
        
        // Assert
        assertNotNull("displayText should not be null", completion.displayText)
        assertTrue("displayText should not be empty", completion.displayText.isNotBlank())
        
        // Note: thinkingTrace may be null if model doesn't support it
        // or if response is short. Log for manual verification.
        println("=== THINKING TRACE ===")
        println(completion.thinkingTrace ?: "(No thinking trace returned)")
        println("=== DISPLAY TEXT ===")
        println(completion.displayText)
    }
    
    /**
     * 单元测试：验证 Request 数据类包含 enableThinking
     */
    @Test
    fun `Request has enableThinking defaulted to true`() {
        val request = DashscopeRequest(
            apiKey = "test",
            model = "test",
            messages = emptyList()
        )
        
        assertTrue("enableThinking should default to true", request.enableThinking)
    }
    
    /**
     * 单元测试：验证 Completion 数据类包含 thinkingTrace
     */
    @Test
    fun `Completion has thinkingTrace field`() {
        val completion = DashscopeCompletion(
            displayText = "test",
            thinkingTrace = "思考过程"
        )
        
        assertEquals("思考过程", completion.thinkingTrace)
    }
}
