// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeClient.kt
// 模块：:data:ai-core
// 说明：抽象 DashScope 调用客户端并为后续 streaming 做准备
// 作者：创建于 2025-11-16
package com.smartsales.data.aicore

import com.alibaba.dashscope.aigc.generation.Generation
import com.alibaba.dashscope.aigc.generation.GenerationParam
import com.alibaba.dashscope.aigc.generation.GenerationResult
import com.alibaba.dashscope.common.Message
import com.alibaba.dashscope.common.ResultCallback
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DashscopeMessage(
    val role: String,
    val content: String
)

data class DashscopeRequest(
    val apiKey: String,
    val model: String,
    val messages: List<DashscopeMessage>,
    val temperature: Float = 0.3f
)

data class DashscopeCompletion(
    val displayText: String
)

sealed class DashscopeStreamEvent {
    data class Chunk(val content: String) : DashscopeStreamEvent()
    data object Completed : DashscopeStreamEvent()
    data class Failed(val reason: String, val throwable: Throwable?) : DashscopeStreamEvent()
}

interface DashscopeClient {
    fun generate(request: DashscopeRequest): DashscopeCompletion
    fun stream(request: DashscopeRequest): Flow<DashscopeStreamEvent>
}

@Singleton
class DefaultDashscopeClient @Inject constructor() : DashscopeClient {
    override fun generate(request: DashscopeRequest): DashscopeCompletion {
        val param = buildGenerationParam(request)
        val output = Generation().call(param)
        return DashscopeCompletion(displayText = extractDisplayText(output))
    }

    override fun stream(request: DashscopeRequest): Flow<DashscopeStreamEvent> = callbackFlow {
        val param = buildGenerationParam(request)
        val generation = Generation()
        generation.streamCall(param, object : ResultCallback<GenerationResult>() {
            override fun onOpen(status: com.alibaba.dashscope.common.Status?) {
                // ignore
            }

            override fun onEvent(data: GenerationResult) {
                trySend(DashscopeStreamEvent.Chunk(extractDisplayText(data)))
            }

            override fun onComplete() {
                trySend(DashscopeStreamEvent.Completed)
                close()
            }

            override fun onError(e: Exception) {
                trySend(
                    DashscopeStreamEvent.Failed(
                        reason = e.message ?: "DashScope streaming 错误",
                        throwable = e
                    )
                )
                close(e)
            }
        })
        awaitClose { }
    }

    private fun buildGenerationParam(request: DashscopeRequest): GenerationParam {
        val messages = request.messages.map {
            Message.builder()
                .role(it.role)
                .content(it.content)
                .build()
        }
        return GenerationParam.builder()
            .apiKey(request.apiKey)
            .model(request.model)
            .messages(messages)
            .temperature(request.temperature)
            .resultFormat(GenerationParam.ResultFormat.MESSAGE)
            .build()
    }

    private fun extractDisplayText(result: GenerationResult): String {
        val text = result.output?.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            return text.trim()
        }
        val firstMessage = result.output?.choices
            ?.firstOrNull()
            ?.message
            ?.content
        return firstMessage?.trim().orEmpty()
    }
}
