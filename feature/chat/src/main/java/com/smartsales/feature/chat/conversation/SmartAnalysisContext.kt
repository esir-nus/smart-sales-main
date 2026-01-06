package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.home.ChatMessageUi
import com.smartsales.feature.usercenter.SalesPersona

/**
 * Context needed for SmartAnalysis requests.
 * 
 * P3.8: Encapsulates dependencies from HomeScreenViewModel
 * that ConversationViewModel needs for SmartAnalysis flow.
 */
data class SmartAnalysisContext(
    val sessionId: String,
    val messages: List<ChatMessageUi>,  // For InputClassifier
    val salesPersona: SalesPersona?
)
