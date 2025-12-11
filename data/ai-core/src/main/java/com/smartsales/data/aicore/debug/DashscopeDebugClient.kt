// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DashscopeDebugClient.kt
// 模块：:data:ai-core
// 说明：调试专用 DashScope streaming 客户端，直接暴露原始 chunk，勿接入生产链路
// 作者：创建于 2025-12-11
package com.smartsales.data.aicore.debug

import com.smartsales.data.aicore.DashscopeClient
import com.smartsales.data.aicore.DashscopeCredentialsProvider
import com.smartsales.data.aicore.DashscopeMessage
import com.smartsales.data.aicore.DashscopeRequest
import com.smartsales.data.aicore.DashscopeStreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

interface DashscopeDebugClient {
    fun streamRaw(
        userPrompt: String,
        incrementalOutput: Boolean
    ): Flow<DashscopeStreamEvent>
}

@Singleton
class DashscopeDebugClientImpl @Inject constructor(
    private val dashscopeClient: DashscopeClient,
    private val credentialsProvider: DashscopeCredentialsProvider
) : DashscopeDebugClient {

    override fun streamRaw(
        userPrompt: String,
        incrementalOutput: Boolean
    ): Flow<DashscopeStreamEvent> {
        val credentials = credentialsProvider.obtain()
        if (credentials.apiKey.isBlank()) {
            return flowOf(
                DashscopeStreamEvent.Failed(
                    reason = "DashScope API Key 未配置",
                    throwable = null
                )
            )
        }
        val request = DashscopeRequest(
            apiKey = credentials.apiKey,
            model = credentials.model,
            messages = listOf(
                DashscopeMessage(
                    role = "user",
                    content = userPrompt.trim()
                )
            ),
            incrementalOutput = incrementalOutput
        )
        return dashscopeClient.stream(request)
    }
}
