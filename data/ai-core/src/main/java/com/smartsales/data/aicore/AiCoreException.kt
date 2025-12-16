// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreException.kt
// 模块：:data:ai-core
// 说明：统一 AI 模块的错误结构，便于 UI 感知来源与建议
// 作者：创建于 2025-11-17
package com.smartsales.data.aicore

enum class AiCoreErrorSource {
    DASH_SCOPE,
    TINGWU,
    EXPORT,
    OSS,
    XFYUN,
}

enum class AiCoreErrorReason {
    MISSING_CREDENTIALS,
    NETWORK,
    TIMEOUT,
    REMOTE,
    // 本地能力/开关不支持（例如：未开通 translate/predict 能力时禁止发起请求）
    UNSUPPORTED_CAPABILITY,
    IO,
    UNKNOWN
}

class AiCoreException(
    val source: AiCoreErrorSource,
    val reason: AiCoreErrorReason,
    message: String,
    val suggestion: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    val userFacingMessage: String = suggestion?.let { "$message（建议：$it）" } ?: message
}
