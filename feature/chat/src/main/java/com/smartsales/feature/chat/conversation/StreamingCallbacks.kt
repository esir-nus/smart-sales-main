package com.smartsales.feature.chat.conversation

/**
 * Callbacks for streaming events.
 * 
 * P3.9.2: Decouples streaming from HSVM-specific side effects.
 * Implemented by HomeViewModel to handle delta/completed/error events.
 */
interface StreamingCallbacks {
    fun onDelta(assistantId: String, token: String)
    suspend fun onCompleted(assistantId: String, rawFullText: String)
    fun onError(assistantId: String, throwable: Throwable)
    
    // V1 retry callbacks
    suspend fun onRetryStart(attempt: Int) {}
    suspend fun onTerminal(fullText: String, attempt: Int, failureReason: String?) {}
}

