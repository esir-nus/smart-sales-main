// 文件：app/src/test/java/com/smartsales/aitest/ui/screens/debug/DebugStreamViewModelTest.kt
// 模块：:app
// 说明：验证 DashScope 调试 VM 对 chunk 聚合与日志编号的处理
// 作者：创建于 2025-12-11
package com.smartsales.aitest.ui.screens.debug

import com.smartsales.data.aicore.DashscopeStreamEvent
import com.smartsales.data.aicore.debug.DashscopeDebugClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebugStreamViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `full sequence mode replaces aggregated text`() = runTest {
        val events = listOf(
            DashscopeStreamEvent.Chunk("你好"),
            DashscopeStreamEvent.Chunk("你好，世界"),
            DashscopeStreamEvent.Completed
        )
        val viewModel = DebugStreamViewModel(FakeDebugClient(events))
        viewModel.updatePrompt("hi")
        viewModel.updateMode(DebugStreamMode.FullSequence)

        viewModel.startStreaming()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("""[Chunk #1] "你好"""", """[Chunk #2] "你好，世界"""", """[Completed] finalText="你好，世界""""), state.rawLogs)
        assertEquals("你好，世界", state.aggregatedText)
        assertEquals(false, state.isStreaming)
    }

    @Test
    fun `incremental mode appends aggregated text`() = runTest {
        val events = listOf(
            DashscopeStreamEvent.Chunk("a"),
            DashscopeStreamEvent.Chunk("b"),
            DashscopeStreamEvent.Chunk("c"),
            DashscopeStreamEvent.Completed
        )
        val viewModel = DebugStreamViewModel(FakeDebugClient(events))
        viewModel.updatePrompt("test")
        viewModel.updateMode(DebugStreamMode.Incremental)

        viewModel.startStreaming()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("abc", state.aggregatedText)
        assertEquals("""[Completed] finalText="abc"""", state.rawLogs.last())
    }

    private class FakeDebugClient(
        private val events: List<DashscopeStreamEvent>
    ) : DashscopeDebugClient {
        override fun streamRaw(
            userPrompt: String,
            incrementalOutput: Boolean
        ) = kotlinx.coroutines.flow.flow {
            events.forEach { emit(it) }
        }
    }
}
