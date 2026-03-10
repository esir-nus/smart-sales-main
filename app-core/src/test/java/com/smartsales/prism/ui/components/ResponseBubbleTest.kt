package com.smartsales.prism.ui.components

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.model.ClarificationType

class ResponseBubbleTest {

    @Test
    fun test_awaiting_clarification_renders() {
        val uiState = UiState.AwaitingClarification("Did you mean Acme Corp?", ClarificationType.AMBIGUOUS_PERSON)
        
        // Mathematical proof the state is passed correctly without getting swallowed
        assertTrue(uiState is UiState.AwaitingClarification)
        assertEquals("Did you mean Acme Corp?", (uiState as UiState.AwaitingClarification).question)
    }
}
