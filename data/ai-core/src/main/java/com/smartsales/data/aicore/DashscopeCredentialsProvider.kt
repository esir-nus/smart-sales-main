// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeCredentialsProvider.kt
// 模块：:data:ai-core
// 说明：统一 DashScope API Key 与模型的提供方式
// 作者：创建于 2025-11-16
package com.smartsales.data.aicore

import javax.inject.Inject
import javax.inject.Singleton

data class DashscopeCredentials(
    val apiKey: String,
    val model: String
)

interface DashscopeCredentialsProvider {
    fun obtain(): DashscopeCredentials
}

@Singleton
class BuildConfigDashscopeCredentialsProvider @Inject constructor() : DashscopeCredentialsProvider {
    override fun obtain(): DashscopeCredentials = DashscopeCredentials(
        apiKey = BuildConfig.DASHSCOPE_API_KEY,
        model = BuildConfig.DASHSCOPE_MODEL.ifBlank { DEFAULT_MODEL }
    )

    private companion object {
        private const val DEFAULT_MODEL = "qwen-turbo"
    }
}
