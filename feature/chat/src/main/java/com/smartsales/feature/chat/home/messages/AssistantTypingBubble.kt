package com.smartsales.feature.chat.home.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Typing indicator shown while assistant is streaming a response.
 * 
 * Extracted from HomeScreen to support ConversationScreen (P3.5).
 */
@Composable
fun AssistantTypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        TypingIndicator()
    }
}
