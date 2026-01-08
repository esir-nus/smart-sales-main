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

/**
 * Maps ChatError to user-facing message (Chinese microcopy).
 *
 * Single source of truth for error display text. UI layers call this
 * instead of doing their own mapping.
 *
 * @see ux-experience.md for state inventory
 */
fun ChatError.toUserMessage(): String = when (this) {
    is ChatError.NetworkError -> message ?: cause.message ?: "网络请求失败"
    is ChatError.NetworkTimeout -> "请求超时"
    is ChatError.NoConnection -> "无网络连接"
    is ChatError.SessionExpired -> "会话已过期"
    is ChatError.SessionNotFound -> "会话不存在"
    is ChatError.ValidationError -> message
    is ChatError.EmptyInput -> "请输入内容"
    is ChatError.InputTooLong -> "输入内容过长"
    is ChatError.ApiError -> message.ifBlank { "服务异常 ($code)" }
    is ChatError.RateLimited -> "请求过于频繁，请稍后重试"
    is ChatError.Unauthorized -> "登录已失效，请重新登录"
    is ChatError.TranscriptionFailed -> "转写失败: $reason"
    is ChatError.AudioUploadFailed -> "音频上传失败"
    is ChatError.ExportNotReady -> "导出尚未就绪"
    is ChatError.ExportFailed -> "导出失败: $reason"
    is ChatError.MetadataParseError -> "数据解析失败"
    is ChatError.MetadataNotFound -> "未找到数据"
    is ChatError.Unknown -> cause.message ?: "AI 回复失败"
}
