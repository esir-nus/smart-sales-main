// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/testutil/NoopXfyunVoiceprintApi.kt
// 模块：:feature:chat
// 说明：提供单元测试用的讯飞声纹 API 空实现（不触发网络）
// 作者：创建于 2025-12-22
package com.smartsales.feature.chat.testutil

import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.xfyun.XfyunConfigProvider
import com.smartsales.data.aicore.xfyun.XfyunHttpClient
import com.smartsales.data.aicore.xfyun.XfyunVoiceprintApi

/**
 * 说明：构造不会在测试中触发网络的 VoiceprintApi，仅满足 ViewModel 依赖注入。
 * 重要：测试不调用 register/delete，避免任何 IO。
 */
fun buildNoopXfyunVoiceprintApi(): XfyunVoiceprintApi {
    val provider = object : AiParaSettingsProvider {
        override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
    }
    return XfyunVoiceprintApi(
        httpClient = XfyunHttpClient(),
        configProvider = XfyunConfigProvider(provider),
    )
}
