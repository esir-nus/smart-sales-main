package com.smartsales.data.aicore

import javax.inject.Inject

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/OssCredentialsProvider.kt
// 模块：:data:ai-core
// 说明：提供 OSS 配置的获取与校验
// 作者：创建于 2025-11-17
data class OssCredentials(
    val accessKeyId: String,
    val accessKeySecret: String,
    val bucket: String,
    val endpoint: String
)

interface OssCredentialsProvider {
    fun obtain(): OssCredentials
}

class BuildConfigOssCredentialsProvider @Inject constructor() : OssCredentialsProvider {
    override fun obtain(): OssCredentials = OssCredentials(
        accessKeyId = BuildConfig.OSS_ACCESS_KEY_ID,
        accessKeySecret = BuildConfig.OSS_ACCESS_KEY_SECRET,
        bucket = BuildConfig.OSS_BUCKET_NAME,
        endpoint = BuildConfig.OSS_ENDPOINT
    )
}
