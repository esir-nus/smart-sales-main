package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ConversationReducer.
 * 
 * Pure function testing - no mocks, no Android dependencies needed.
 * Tests verify deterministic behavior: same inputs always produce same outputs.
 */
class ConversationReducerTest {
    
    // ===== InputChanged tests =====
    
    @Test
    fun inputChanged_updatesInputText() {
        val state = ConversationState()
        val intent = ConversationIntent.InputChanged("Hello world")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals("Hello world", result.inputText)
    }
    
    @Test
    fun inputChanged_preservesOtherState() {
        val state = ConversationState(
            messages = listOf(sampleMessage()),
            isSending = true
        )
        val intent = ConversationIntent.InputChanged("New text")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals(1, result.messages.size)
        assertTrue(result.isSending)
    }
    
    // ===== SendMessage tests =====
    
    @Test
    fun sendMessage_whenInputEmpty_noChange() {
        val state = ConversationState(inputText = "")
        val intent = ConversationIntent.SendMessage(timestamp = 1000L)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals(state, result)
    }
    
    @Test
    fun sendMessage_whenInputBlank_noChange() {
        val state = ConversationState(inputText = "   ")
        val intent = ConversationIntent.SendMessage(timestamp = 1000L)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals(state, result)
    }
    
    @Test
    fun sendMessage_whenAlreadySending_noChange() {
        val state = ConversationState(inputText = "Hello", isSending = true)
        val intent = ConversationIntent.SendMessage(timestamp = 1000L)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals(state, result)
    }
    
    @Test
    fun sendMessage_whenValid_addsUserMessage() {
        val state = ConversationState(inputText = "Hello world")
        val intent = ConversationIntent.SendMessage(timestamp = 1234567890L)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals(1, result.messages.size)
        val message = result.messages[0]
        assertEquals(ChatMessageRole.USER, message.role)
        assertEquals("Hello world", message.content)
        assertEquals(1234567890L, message.timestampMillis)
    }
    
    @Test
    fun sendMessage_whenValid_trimsInput() {
        val state = ConversationState(inputText = "  Hello world  ")
        val intent = ConversationIntent.SendMessage(timestamp = 1000L)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals("Hello world", result.messages[0].content)
    }
    
    @Test
    fun sendMessage_whenValid_clearsInputText() {
        val state = ConversationState(inputText = "Hello world")
        val intent = ConversationIntent.SendMessage(timestamp = 1000L)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals("", result.inputText)
    }
    
    @Test
    fun sendMessage_whenValid_setsSendingTrue() {
        val state = ConversationState(inputText = "Hello", isSending = false)
        val intent = ConversationIntent.SendMessage(timestamp = 1000L)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertTrue(result.isSending)
    }
    
    @Test
    fun sendMessage_whenValid_clearsError() {
        val state = ConversationState(
            inputText = "Hello",
            errorMessage = "Previous error"
        )
        val intent = ConversationIntent.SendMessage(timestamp = 1000L)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertNull(result.errorMessage)
    }
    
    @Test
    fun sendMessage_isDeterministic() {
        val state = ConversationState(inputText = "Test")
        val intent = ConversationIntent.SendMessage(timestamp = 9999L)
        
        val result1 = ConversationReducer.reduce(state, intent)
        val result2 = ConversationReducer.reduce(state, intent)
        
        // State should be identical except for message IDs (which are random UUIDs)
        assertEquals(result1.inputText, result2.inputText)
        assertEquals(result1.isSending, result2.isSending)
        assertEquals(result1.messages.size, result2.messages.size)
        assertEquals(result1.messages[0].content, result2.messages[0].content)
        assertEquals(result1.messages[0].timestampMillis, result2.messages[0].timestampMillis)
        assertEquals(result1.messages[0].role, result2.messages[0].role)
    }
    
    // ===== MessageReceived tests =====
    
    @Test
    fun messageReceived_addsMessage() {
        val state = ConversationState()
        val message = sampleMessage()
        val intent = ConversationIntent.MessageReceived(message)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals(1, result.messages.size)
        assertEquals(message, result.messages[0])
    }
    
    @Test
    fun messageReceived_resetsSendingFlag() {
        val state = ConversationState(isSending = true)
        val intent = ConversationIntent.MessageReceived(sampleMessage())
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertFalse(result.isSending)
    }
    
    @Test
    fun messageReceived_appendsToExistingMessages() {
        val existing = sampleMessage(content = "First")
        val state = ConversationState(messages = listOf(existing))
        val newMessage = sampleMessage(content = "Second")
        val intent = ConversationIntent.MessageReceived(newMessage)
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals(2, result.messages.size)
        assertEquals("First", result.messages[0].content)
        assertEquals("Second", result.messages[1].content)
    }
    
    // ===== SendSmartAnalysis tests (P3.8) =====
    
    @Test
    fun sendSmartAnalysis_whenAlreadySending_noChange() {
        val state = ConversationState(isSending = true)
        val intent = ConversationIntent.SendSmartAnalysis(timestamp = 1000L, goal = "analyze this")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals(state, result)
    }
    
    @Test
    fun sendSmartAnalysis_setsSmartAnalysisModeTrue() {
        val state = ConversationState(isSmartAnalysisMode = false)
        val intent = ConversationIntent.SendSmartAnalysis(timestamp = 1000L, goal = "总结要点")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertTrue(result.isSmartAnalysisMode)
    }
    
    @Test
    fun sendSmartAnalysis_setSendingTrue() {
        val state = ConversationState(isSending = false)
        val intent = ConversationIntent.SendSmartAnalysis(timestamp = 1000L, goal = "analyze")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertTrue(result.isSending)
    }
    
    @Test
    fun sendSmartAnalysis_capturesGoal() {
        val state = ConversationState()
        val intent = ConversationIntent.SendSmartAnalysis(timestamp = 1000L, goal = "总结要点")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertEquals("总结要点", result.smartAnalysisGoal)
    }
    
    @Test
    fun sendSmartAnalysis_withEmptyGoal_setsGoalNull() {
        val state = ConversationState()
        val intent = ConversationIntent.SendSmartAnalysis(timestamp = 1000L, goal = "")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertNull(result.smartAnalysisGoal)
    }
    
    @Test
    fun sendSmartAnalysis_withBlankGoal_setsGoalNull() {
        val state = ConversationState()
        val intent = ConversationIntent.SendSmartAnalysis(timestamp = 1000L, goal = "   ")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertNull(result.smartAnalysisGoal)
    }
    
    @Test
    fun sendSmartAnalysis_clearsErrorMessage() {
        val state = ConversationState(errorMessage = "Previous error")
        val intent = ConversationIntent.SendSmartAnalysis(timestamp = 1000L, goal = "analyze")
        
        val result = ConversationReducer.reduce(state, intent)
        
        assertNull(result.errorMessage)
    }
    
    // ===== Helper functions =====
    
    private fun sampleMessage(
        role: ChatMessageRole = ChatMessageRole.ASSISTANT,
        content: String = "Sample message"
    ): ChatMessageUi {
        return ChatMessageUi(
            role = role,
            content = content,
            timestampMillis = 1000L
        )
    }
}
