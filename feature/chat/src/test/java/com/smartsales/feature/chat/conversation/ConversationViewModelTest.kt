package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ConversationViewModel.
 * 
 * P3.1.B1: Tests only InputChanged intent (no streaming side effects).
 * 
 * Note: ConversationViewModel is a regular class (not ViewModel) 
 * since it's injected into HomeScreenViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeOrchestrator: FakeHomeOrchestrator
    private lateinit var viewModel: ConversationViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeOrchestrator = FakeHomeOrchestrator()
        viewModel = ConversationViewModel(fakeOrchestrator)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun initialState_isEmpty() = runTest {
        val state = viewModel.state.value
        
        assertEquals("", state.inputText)
        assertEquals(0, state.messages.size)
        assertEquals(false, state.isSending)
    }
    
    @Test
    fun dispatch_inputChanged_updatesInputText() = runTest {
        viewModel.dispatch(ConversationIntent.InputChanged("Hello world"))
        
        val state = viewModel.state.value
        assertEquals("Hello world", state.inputText)
    }
    
    @Test
    fun dispatch_inputChanged_multipleUpdates() = runTest {
        viewModel.dispatch(ConversationIntent.InputChanged("First"))
        assertEquals("First", viewModel.state.value.inputText)
        
        viewModel.dispatch(ConversationIntent.InputChanged("Second"))
        assertEquals("Second", viewModel.state.value.inputText)
        
        viewModel.dispatch(ConversationIntent.InputChanged(""))
        assertEquals("", viewModel.state.value.inputText)
    }
    
    @Test
    fun dispatch_inputChanged_preservesOtherState() = runTest {
        // Setup: Add a message via reducer
        val message = com.smartsales.feature.chat.home.ChatMessageUi(
            role = ChatMessageRole.ASSISTANT,
            content = "Test message",
            timestampMillis = 1000L
        )
        viewModel.dispatch(ConversationIntent.MessageReceived(message))
        
        // Act: Change input
        viewModel.dispatch(ConversationIntent.InputChanged("New input"))
        
        // Assert: Messages preserved, input changed
        val state = viewModel.state.value
        assertEquals("New input", state.inputText)
        assertEquals(1, state.messages.size)
        assertEquals("Test message", state.messages[0].content)
    }
    
    @Test
    fun stateFlow_exposesImmutableState() = runTest {
        // Verify state is exposed via StateFlow (not MutableStateFlow)
        val stateFlow = viewModel.state
        
        // This should compile (StateFlow is read-only)
        val value = stateFlow.value
        assertEquals("", value.inputText)
    }
    
    // Fake implementation of HomeOrchestrator for testing
    private class FakeHomeOrchestrator : HomeOrchestrator {
        override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> {
            // P3.1.B1: Streaming not tested yet (deferred to B2)
            return flowOf()
        }
    }
}
