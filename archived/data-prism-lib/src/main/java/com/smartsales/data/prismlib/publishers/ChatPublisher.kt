package com.smartsales.data.prismlib.publishers

import com.smartsales.domain.prism.core.ExecutorResult
import com.smartsales.domain.prism.core.UiState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coach 模式发布器 — 流式输出聊天消息
 * @see Prism-V1.md §2.2 #4, §4.1 Chat Mode
 */
@Singleton
class ChatPublisher @Inject constructor() : BaseModePublisher() {

    override suspend fun publish(result: ExecutorResult) {
        // 1. 设置 Thinking 状态
        setThinking("正在思考...")

        // 2. 模拟流式输出
        if (result.displayContent.isNotEmpty()) {
            simulateStreaming(result.displayContent)
        }

        // 3. 发布最终响应
        _uiState.value = UiState.Response(
            content = result.displayContent,
            structuredJson = result.structuredJson
        )
    }
}
