package com.smartsales.feature.chat.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.smartsales.feature.chat.home.ChatMessageRole
import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.chat.home.HomeScreenTestTags
import com.smartsales.feature.chat.home.messages.MessageBubble
import com.smartsales.feature.chat.home.messages.AssistantTypingBubble

/**
 * Conversation screen: message list + typing indicator.
 * 
 * P3.5: Extracted from HomeScreen to isolate conversation UI.
 * Driven by ConversationViewModel (via HomeUiState during migration).
 */
@Composable
fun ConversationScreen(
    messages: List<ChatMessageUi>,
    isLoadingHistory: Boolean,
    showRawAssistantOutput: Boolean,
    smartReasoningText: String?,
    onCopy: (String) -> Unit,
    onLoadMoreHistory: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val hasActiveChat = messages.isNotEmpty()
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(HomeScreenTestTags.LIST),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
            userScrollEnabled = true
        ) {
            if (!hasActiveChat) {
                item("welcome_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
            } else {
                if (isLoadingHistory) {
                    item("history-loading") {
                        Text(
                            text = "加载历史记录...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                
                itemsIndexed(messages, key = { _, item -> item.id }) { index, message ->
                    val isTranscriptSummary = message.role == ChatMessageRole.ASSISTANT &&
                        message.content.contains("通话分析")
                    val tagModifier = if (message.role == ChatMessageRole.ASSISTANT &&
                        (index == messages.lastIndex || isTranscriptSummary)
                    ) {
                        Modifier.testTag(HomeScreenTestTags.ASSISTANT_MESSAGE)
                    } else {
                        Modifier
                    }
                    val alignEnd = message.role == ChatMessageRole.USER
                    
                    MessageBubble(
                        message = message,
                        alignEnd = alignEnd,
                        modifier = tagModifier,
                        reasoningText = if (!alignEnd && message.isSmartAnalysis) {
                            smartReasoningText
                        } else {
                            null
                        },
                        showRawAssistantOutput = showRawAssistantOutput,
                        onCopyAssistant = { content ->
                            onCopy(content)
                        }
                    )
                }
            }
            
            if (messages.lastOrNull()?.isStreaming == true) {
                item("typing-indicator") {
                    AssistantTypingBubble()
                }
            }
            
            item("chat-bottom-pad") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
