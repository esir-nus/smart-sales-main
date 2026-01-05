// File: feature/chat/src/main/java/com/smartsales/domain/error/ChatError.kt
// Module: :feature:chat
// Summary: Typed error model for domain layer
// Author: created on 2026-01-05

package com.smartsales.domain.error

/**
 * ChatError: Typed error hierarchy for domain layer.
 *
 * Replaces generic throwables with named error types for better error handling.
 * UI can pattern match on specific errors to show appropriate messages.
 */
sealed class ChatError {
    // Network errors
    data class NetworkError(val cause: Throwable, val message: String? = null) : ChatError()
    object NetworkTimeout : ChatError()
    object NoConnection : ChatError()

    // Session errors
    object SessionExpired : ChatError()
    data class SessionNotFound(val sessionId: String) : ChatError()

    // Validation errors
    data class ValidationError(val message: String) : ChatError()
    object EmptyInput : ChatError()
    object InputTooLong : ChatError()

    // API errors
    data class ApiError(val code: Int, val message: String) : ChatError()
    object RateLimited : ChatError()
    object Unauthorized : ChatError()

    // Transcription errors
    data class TranscriptionFailed(val jobId: String, val reason: String) : ChatError()
    object AudioUploadFailed : ChatError()

    // Export errors
    object ExportNotReady : ChatError()
    data class ExportFailed(val format: String, val reason: String) : ChatError()

    // Metadata errors
    data class MetadataParseError(val raw: String) : ChatError()
    object MetadataNotFound : ChatError()

    // Unknown/Generic
    data class Unknown(val cause: Throwable) : ChatError()
}

/**
 * Helper to convert throwables to typed errors.
 */
fun Throwable.toChatError(): ChatError = when (this) {
    is java.net.SocketTimeoutException -> ChatError.NetworkTimeout
    is java.net.UnknownHostException -> ChatError.NoConnection
    is java.io.IOException -> ChatError.NetworkError(this)
    else -> ChatError.Unknown(this)
}
