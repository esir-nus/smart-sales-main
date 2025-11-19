package com.smartsales.data.aicore

import com.smartsales.core.util.Result
import java.io.File

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/OssUploadClient.kt
// 模块：:data:ai-core
// 说明：定义 OSS 上传接口及其入参/出参
// 作者：创建于 2025-11-17
data class OssUploadRequest(
    val file: File,
    val objectKey: String? = null
)

data class OssUploadResult(
    val objectKey: String,
    val presignedUrl: String
)

interface OssUploadClient {
    suspend fun uploadAudio(request: OssUploadRequest): Result<OssUploadResult>
}
