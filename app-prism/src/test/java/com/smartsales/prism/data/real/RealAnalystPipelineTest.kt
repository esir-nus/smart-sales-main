package com.smartsales.prism.data.real

import com.smartsales.prism.domain.analyst.AnalystResponse
import com.smartsales.prism.domain.analyst.AnalystState
import com.smartsales.prism.domain.pipeline.ContextBuilder
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import com.smartsales.prism.domain.pipeline.TokenUsage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class RealAnalystPipelineTest {

    private lateinit var pipeline: RealAnalystPipeline
    private lateinit var contextBuilder: ContextBuilder
    private lateinit var executor: Executor

    @Before
    fun setup() {
        contextBuilder = mock()
        executor = mock()
        pipeline = RealAnalystPipeline(contextBuilder, executor)
    }

    @Test
    fun `L2 Scenario 1 - Executor Failure - returns Chat and stays IDLE`() = runTest {
        whenever(contextBuilder.build(any(), any())).thenReturn(mock())
        whenever(executor.execute(any())).thenReturn(ExecutorResult.Failure("Network error"))

        val response = pipeline.handleInput("分析", emptyList())

        assertTrue(response is AnalystResponse.Chat)
        assertEquals("网络通信异常，请重试。", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }

    @Test
    fun `L2 Scenario 2 - info_sufficient is false - returns Chat and stays IDLE`() = runTest {
        whenever(contextBuilder.build(any(), any())).thenReturn(mock())
        val jsonStr = """{"info_sufficient": false, "response": "请问您想分析哪个客户？"}"""
        whenever(executor.execute(any())).thenReturn(ExecutorResult.Success(jsonStr, TokenUsage(0, 0)))

        val response = pipeline.handleInput("分析", emptyList())

        assertTrue(response is AnalystResponse.Chat)
        assertEquals("请问您想分析哪个客户？", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }

    @Test
    fun `L2 Scenario 3 - info_sufficient is true - returns Plan and transitions to PROPOSAL`() = runTest {
        whenever(contextBuilder.build(any(), any())).thenReturn(mock())
        val jsonStr = """{"info_sufficient": true, "response": "准备分析"}"""
        whenever(executor.execute(any())).thenReturn(ExecutorResult.Success(jsonStr, TokenUsage(0, 0)))

        val response = pipeline.handleInput("分析大客户数据", emptyList())

        assertTrue(response is AnalystResponse.Plan)
        assertEquals("准备分析", (response as AnalystResponse.Plan).summary)
        assertEquals(AnalystState.PROPOSAL, pipeline.state.value)
    }

    @Test
    fun `L2 Scenario 4 - JSON parsing fails - returns Chat and stays IDLE`() = runTest {
        whenever(contextBuilder.build(any(), any())).thenReturn(mock())
        val invalidJsonStr = "Not JSON"
        whenever(executor.execute(any())).thenReturn(ExecutorResult.Success(invalidJsonStr))

        val response = pipeline.handleInput("分析大客户数据", emptyList())

        assertTrue(response is AnalystResponse.Chat)
        assertEquals("我没完全明白，能再详细说说你想分析的内容吗？", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }

    @Test
    fun `L2 Scenario 5 - In PROPOSAL state - returns Chat and resets to IDLE`() = runTest {
        // Setup to reach PROPOSAL state
        whenever(contextBuilder.build(any(), any())).thenReturn(mock())
        val jsonStr = """{"info_sufficient": true, "response": "准备分析"}"""
        whenever(executor.execute(any())).thenReturn(ExecutorResult.Success(jsonStr, TokenUsage(0, 0)))
        pipeline.handleInput("分析大客户数据", emptyList())
        assertEquals(AnalystState.PROPOSAL, pipeline.state.value)

        // Now test handling input in PROPOSAL state
        val response = pipeline.handleInput("ok", emptyList())

        assertTrue(response is AnalystResponse.Chat)
        assertEquals("执行引擎正在升级中 (Wave 3)，目前仅支持到计划生成阶段。", (response as AnalystResponse.Chat).content)
        assertEquals(AnalystState.IDLE, pipeline.state.value)
    }
}
