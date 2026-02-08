package com.smartsales.prism.domain.coach

import com.smartsales.prism.data.fakes.FakeCoachPipeline
import com.smartsales.prism.domain.pipeline.ChatTurn
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * FakeCoachPipeline 单元测试
 * 
 * 验证 Fake 实现的核心行为：
 * - 响应内容非空
 * - 关键词触发 suggestAnalyst
 * - 会话历史传递
 */
class FakeCoachPipelineTest {
    
    private lateinit var pipeline: FakeCoachPipeline
    
    @Before
    fun setUp() {
        pipeline = FakeCoachPipeline()
    }
    
    @Test
    fun `process returns Chat response with content`() = runTest {
        val response = pipeline.process("你好")
        
        assertTrue(response is CoachResponse.Chat)
        val chat = response as CoachResponse.Chat
        assertTrue(chat.content.isNotEmpty())
        assertTrue(chat.content.contains("[Coach]"))
    }
    
    @Test
    fun `process with analysis keyword sets suggestAnalyst to true`() = runTest {
        val response = pipeline.process("帮我分析这个客户")
        
        assertTrue(response is CoachResponse.Chat)
        val chat = response as CoachResponse.Chat
        assertTrue(chat.suggestAnalyst)
    }
    
    @Test
    fun `process with data keyword sets suggestAnalyst to true`() = runTest {
        val response = pipeline.process("查看数据报表")
        
        assertTrue(response is CoachResponse.Chat)
        val chat = response as CoachResponse.Chat
        assertTrue(chat.suggestAnalyst)
    }
    
    @Test
    fun `process without analysis keywords keeps suggestAnalyst false`() = runTest {
        val response = pipeline.process("今天天气怎么样")
        
        assertTrue(response is CoachResponse.Chat)
        val chat = response as CoachResponse.Chat
        assertFalse(chat.suggestAnalyst)
    }
    
    @Test
    fun `process with session history includes turn count in response`() = runTest {
        val history = listOf(
            ChatTurn(role = "user", content = "你好"),
            ChatTurn(role = "assistant", content = "您好！")
        )
        
        val response = pipeline.process("继续聊", history)
        
        assertTrue(response is CoachResponse.Chat)
        val chat = response as CoachResponse.Chat
        assertTrue(chat.content.contains("2 条历史记录"))
    }
    
    @Test
    fun `process with memory keyword returns mock memoryHits`() = runTest {
        val response = pipeline.process("查看历史记录")
        
        assertTrue(response is CoachResponse.Chat)
        val chat = response as CoachResponse.Chat
        assertTrue(chat.memoryHits.isNotEmpty())
    }
}
