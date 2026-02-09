package com.smartsales.data.oss

/**
 * OSS 上传结果 — 成功返回公开 URL，失败返回错误码和消息。
 */
sealed class OssUploadResult {
    data class Success(val publicUrl: String) : OssUploadResult()
    data class Error(val code: OssErrorCode, val message: String) : OssUploadResult()
}

/**
 * OSS 错误类型枚举。
 */
enum class OssErrorCode {
    AUTH_FAILED,
    BUCKET_NOT_FOUND,
    NETWORK_ERROR,
    FILE_TOO_LARGE,
    UNKNOWN
}
