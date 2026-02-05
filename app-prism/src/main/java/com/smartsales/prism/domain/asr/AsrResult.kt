package com.smartsales.prism.domain.asr

/**
 * ASR 结果 — 云端语音转文字结果
 * @see asr-service/spec.md
 */
sealed class AsrResult {
    /**
     * 转写成功
     * @param text 纯文本转写结果
     */
    data class Success(val text: String) : AsrResult()
    
    /**
     * 转写失败
     * @param code 错误类型
     * @param message 错误描述
     */
    data class Error(val code: ErrorCode, val message: String) : AsrResult()
    
    enum class ErrorCode {
        /** 音频格式不支持 */
        INVALID_FORMAT,
        /** 文件过大 */
        FILE_TOO_LARGE,
        /** API 调用错误 */
        API_ERROR,
        /** 网络错误 */
        NETWORK_ERROR,
        /** 认证失败 */
        AUTH_FAILED
    }
}
