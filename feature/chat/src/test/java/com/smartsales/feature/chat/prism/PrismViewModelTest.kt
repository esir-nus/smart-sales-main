package com.smartsales.feature.chat.prism

import com.smartsales.domain.prism.core.*
import com.smartsales.domain.prism.core.fakes.FakeOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * PrismViewModel 集成测试
 * 验证 ViewModel → FakeOrchestrator → UiState 流程
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrismViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeOrchestrator: FakeOrchestrator
    private lateinit var fakeCoachPublisher: TestModePublisher
    private lateinit var fakeAnalystPublisher: TestModePublisher
    private lateinit var fakeSchedulerPublisher: TestModePublisher
    private lateinit var viewModel: PrismViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeOrchestrator = FakeOrchestrator()
        fakeCoachPublisher = TestModePublisher()
        fakeAnalystPublisher = TestModePublisher()
        fakeSchedulerPublisher = TestModePublisher()
        
        viewModel = PrismViewModel(
            coachPublisher = fakeCoachPublisher,
            analystPublisher = fakeAnalystPublisher,
            schedulePublisher = fakeSchedulerPublisher,
            orchestrator = fakeOrchestrator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is COACH mode with Idle UiState`() {
        assertEquals(Mode.COACH, viewModel.currentMode.value)
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `switchMode updates currentMode`() = runTest {
        viewModel.switchMode(Mode.ANALYST)
        advanceUntilIdle()
        
        assertEquals(Mode.ANALYST, viewModel.currentMode.value)
    }

    @Test
    fun `send with empty input does nothing`() = runTest {
        viewModel.onInputChanged("")
        viewModel.send()
        
        assertFalse(viewModel.isSending.value)
        assertEquals(UiState.Idle, fakeCoachPublisher.uiState.value)
    }

    @Test
    fun `send with valid input publishes to coach publisher`() = runTest {
        viewModel.onInputChanged("分析客户数据")
        viewModel.send()
        advanceUntilIdle()
        
        // 验证 Publisher 收到了结果
        val state = fakeCoachPublisher.uiState.value
        assertTrue("Expected Response state, got $state", state is UiState.Response)
        assertTrue((state as UiState.Response).content.contains("分析客户数据"))
    }

    @Test
    fun `send clears input after success`() = runTest {
        viewModel.onInputChanged("测试输入")
        viewModel.send()
        advanceUntilIdle()
        
        assertEquals("", viewModel.inputText.value)
    }

    /**
     * 测试用 ModePublisher — 最小化实现
     */
    private class TestModePublisher : ModePublisher {
        private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
        override val uiState: StateFlow<UiState> = _uiState

        override suspend fun publish(result: ExecutorResult) {
            _uiState.value = UiState.Response(result.displayContent, result.structuredJson)
        }
    }
}
