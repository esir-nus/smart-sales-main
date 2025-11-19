package com.smartsales.data.aicore

import javax.inject.Inject
import javax.inject.Singleton

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/TingwuCredentialsProvider.kt
// 模块：:data:ai-core
// 说明：提供Tingwu所需的密钥、访问ID以及基础域名
// 作者：创建于 2025-11-16
data class TingwuCredentials(
    val apiKey: String,
    val baseUrl: String,
    val appKey: String,
    val accessKeyId: String,
    val accessKeySecret: String,
    val securityToken: String?,
    val model: String
)

interface TingwuCredentialsProvider {
    fun obtain(): TingwuCredentials
}

@Singleton
class BuildConfigTingwuCredentialsProvider @Inject constructor() : TingwuCredentialsProvider {
    override fun obtain(): TingwuCredentials = TingwuCredentials(
        apiKey = BuildConfig.TINGWU_API_KEY.ifBlank { BuildConfig.TINGWU_APP_KEY },
        baseUrl = normalizeBaseUrl(BuildConfig.TINGWU_BASE_URL),
        appKey = BuildConfig.TINGWU_APP_KEY,
        accessKeyId = BuildConfig.TINGWU_ACCESS_KEY_ID,
        accessKeySecret = BuildConfig.TINGWU_ACCESS_KEY_SECRET,
        securityToken = BuildConfig.TINGWU_SECURITY_TOKEN.ifBlank { null },
        model = BuildConfig.TINGWU_MODEL
    )

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "https://tingwu.cn/"
        val lower = trimmed.lowercase()
        val needsOfficialPath = lower.contains("aliyuncs.com") &&
            !lower.contains("/openapi/tingwu/v2")
        val withoutTrailingSlash = trimmed.trimEnd('/')
        val withPath = if (needsOfficialPath) {
            "$withoutTrailingSlash/openapi/tingwu/v2"
        } else {
            withoutTrailingSlash
        }
        return if (withPath.endsWith("/")) withPath else "$withPath/"
    }
}
