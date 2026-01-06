package com.smartsales.feature.chat.conversation

import com.smartsales.feature.chat.core.ChatRequest

/**
 * Context for streaming responses.
 * 
 * P3.9.1: Encapsulates streaming parameters for incremental extraction.
 * Pattern: Similar to SendContext and SmartAnalysisContext.
 */
data class StreamingContext(
    val request: ChatRequest,
    val assistantId: String,
    val onCompleted: (String) -> Unit = {},
    val onCompletedTransform: ((String) -> String)? = null,
    val isAutoAnalysis: Boolean = false
)
