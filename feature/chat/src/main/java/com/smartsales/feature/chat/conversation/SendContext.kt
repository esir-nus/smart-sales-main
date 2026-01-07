package com.smartsales.feature.chat.conversation

import com.smartsales.feature.usercenter.SalesPersona

/**
 * Context needed for building chat requests.
 * 
 * P3.1.B2: Encapsulates dependencies from HomeViewModel
 * that ConversationViewModel needs to build requests.
 */
data class SendContext(
    val sessionId: String,
    val salesPersona: SalesPersona?,
    val isFirstAssistant: Boolean
)
