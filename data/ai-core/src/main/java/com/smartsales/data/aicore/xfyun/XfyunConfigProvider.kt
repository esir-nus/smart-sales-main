// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunConfigProvider.kt
// 模块：:data:ai-core
// 说明：从 BuildConfig 读取讯飞配置，避免在代码中硬编码密钥
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import com.smartsales.data.aicore.BuildConfig
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XfyunConfigProvider @Inject constructor(
    private val aiParaSettingsProvider: AiParaSettingsProvider,
) {

    fun credentials(): XfyunCredentials {
        // 重要：baseUrlOverride 只用于调试与灰度，避免线上误配。
        val overrideBaseUrl = aiParaSettingsProvider.snapshot().transcription.xfyun.baseUrlOverride.trim()
        val baseUrl = overrideBaseUrl.takeIf { it.isNotBlank() }
            ?: BuildConfig.XFYUN_BASE_URL.takeIf { it.isNotBlank() }
            ?: DEFAULT_BASE_URL
        return XfyunCredentials(
            appId = BuildConfig.XFYUN_APP_ID,
            accessKeyId = BuildConfig.XFYUN_ACCESS_KEY_ID,
            accessKeySecret = BuildConfig.XFYUN_ACCESS_KEY_SECRET,
            baseUrl = baseUrl
        )
    }

    fun voiceprintBaseUrl(): String {
        // 重要：声纹 baseUrl 必须独立于转写 baseUrlOverride，避免误把转写 host 劫持到声纹域名。
        val voiceprintSettings = aiParaSettingsProvider.snapshot().transcription.xfyun.voiceprint
        val overrideBaseUrl = voiceprintSettings.baseUrlOverride.trim()
        return overrideBaseUrl.takeIf { it.isNotBlank() }
            ?: DEFAULT_VOICEPRINT_BASE_URL
    }

    companion object {
        const val DEFAULT_BASE_URL: String = "https://office-api-ist-dx.iflyaisol.com"
        // 重要：声纹接口走 personal 域名；与转写 upload/getResult 的域名不同。
        const val DEFAULT_VOICEPRINT_BASE_URL: String = "https://office-api-personal-dx.iflyaisol.com"
    }
}
