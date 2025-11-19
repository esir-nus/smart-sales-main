// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeAiChatService.kt
// 模块：:data:ai-core
// 说明：调用 DashScope SDK 生成真实 AI 回复并返回结构化 Markdown，并带有重试/日志
// 作者：创建于 2025-11-16
package com.smartsales.data.aicore

import com.alibaba.dashscope.exception.ApiException
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.LogTags
import com.smartsales.core.util.Result
import java.io.IOException
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@Singleton
class DashscopeAiChatService @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val dashscopeClient: DashscopeClient,
    private val credentialsProvider: DashscopeCredentialsProvider,
    optionalConfig: Optional<AiCoreConfig>
) : AiChatService {

    private val config: AiCoreConfig = optionalConfig.orElse(AiCoreConfig())

    override suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse> =
        withContext(dispatchers.io) {
            val credentials = credentialsProvider.obtain()
            if (credentials.apiKey.isBlank()) {
                return@withContext Result.Error(missingCredentialsError())
            }
            val dashscopeRequest = buildDashscopeRequest(request, credentials)
            executeWithRetry(dashscopeRequest) { completion ->
                val markdown = buildMarkdown(completion.displayText, request)
                AiChatResponse(
                    displayText = completion.displayText,
                    structuredMarkdown = markdown,
                    references = emptyList()
                )
            }
        }

    override fun streamMessage(request: AiChatRequest): Flow<AiChatStreamEvent> {
        if (!config.dashscopeEnableStreaming) {
            return super.streamMessage(request)
        }
        val credentials = credentialsProvider.obtain()
        if (credentials.apiKey.isBlank()) {
            return flowOf(AiChatStreamEvent.Error(missingCredentialsError()))
        }
        val dashscopeRequest = buildDashscopeRequest(request, credentials)
        val accumulator = StringBuilder()
        return dashscopeClient.stream(dashscopeRequest).transform { event ->
            when (event) {
                is DashscopeStreamEvent.Chunk -> {
                    accumulator.append(event.content)
                    emit(AiChatStreamEvent.Chunk(event.content))
                }
                DashscopeStreamEvent.Completed -> {
                    val display = accumulator.toString()
                    val response = AiChatResponse(
                        displayText = display,
                        structuredMarkdown = buildMarkdown(display, request),
                        references = emptyList()
                    )
                    emit(AiChatStreamEvent.Completed(response))
                }
                is DashscopeStreamEvent.Failed -> {
                    val throwable = event.throwable?.let { mapError(it) }
                        ?: AiCoreException(
                            source = AiCoreErrorSource.DASH_SCOPE,
                            reason = AiCoreErrorReason.UNKNOWN,
                            message = "DashScope 流式返回失败：${event.reason}"
                        )
                    emit(AiChatStreamEvent.Error(throwable))
                }
            }
        }
    }

    private suspend fun <T> executeWithRetry(
        request: DashscopeRequest,
        mapper: (DashscopeCompletion) -> T
    ): Result<T> {
        val attempts = config.dashscopeMaxRetries.coerceAtLeast(0) + 1
        var currentAttempt = 0
        var lastError: AiCoreException? = null
        while (currentAttempt < attempts) {
            currentAttempt += 1
            try {
                val completion = withTimeout(config.dashscopeRequestTimeoutMillis.coerceAtLeast(1_000L)) {
                    dashscopeClient.generate(request)
                }
                AiCoreLogger.d(TAG, "DashScope 调用成功（attempt=$currentAttempt, model=${request.model}）")
                return Result.Success(mapper(completion))
            } catch (error: Throwable) {
                val mapped = mapError(error)
                lastError = mapped
                AiCoreLogger.e(TAG, "DashScope 调用失败（attempt=$currentAttempt）：${mapped.message}", mapped)
                if (currentAttempt >= attempts) {
                    return Result.Error(mapped)
                }
                delay(RETRY_BACKOFF_MS * currentAttempt)
            }
        }
        return Result.Error(lastError ?: mapError(IllegalStateException("DashScope 未知错误")))
    }

    private fun buildDashscopeRequest(
        request: AiChatRequest,
        credentials: DashscopeCredentials
    ): DashscopeRequest = DashscopeRequest(
        apiKey = credentials.apiKey,
        model = credentials.model,
        messages = buildMessages(request)
    )

    private fun buildMessages(request: AiChatRequest): List<DashscopeMessage> {
        val messages = mutableListOf<DashscopeMessage>()
        val systemPrompt = buildSystemPrompt(request)
        if (systemPrompt.isNotBlank()) {
            messages += DashscopeMessage(role = "system", content = systemPrompt)
        }
        val userPrompt = buildUserPrompt(request)
        messages += DashscopeMessage(role = "user", content = userPrompt)
        return messages
    }

    private fun buildSystemPrompt(request: AiChatRequest): String {
        if (request.skillTags.isEmpty()) {
            return SYSTEM_PROMPT_BASE
        }
        val tagText = request.skillTags.joinToString("、")
        return "$SYSTEM_PROMPT_BASE\n重点关注技能：$tagText。"
    }

    private fun buildUserPrompt(request: AiChatRequest): String {
        val builder = StringBuilder(request.prompt.trim())
        request.transcriptMarkdown?.takeIf { it.isNotBlank() }?.let {
            builder.append("\n\n[会议转写]\n").append(it.trim())
        }
        if (request.attachments.isNotEmpty()) {
            builder.append("\n\n[附件列表]\n")
            request.attachments.forEachIndexed { index, attachment ->
                builder.append(index + 1)
                    .append(". ")
                    .append(attachment.name)
                    .append(" (")
                    .append(attachment.mimeType)
                    .append(")\n")
            }
        }
        return builder.toString()
    }

    private fun buildMarkdown(displayText: String, request: AiChatRequest): String {
        val builder = StringBuilder()
        if (displayText.isNotBlank()) {
            builder.append(displayText.trim()).append("\n\n")
        }
        builder.append("## 输入摘要\n")
            .append("- 原始提问：")
            .append(request.prompt.trim())
            .append("\n")
            .append("- 技能：")
            .append(request.skillTags.takeIf { it.isNotEmpty() }?.joinToString("、") ?: "默认")
            .append("\n")
            .append("- 附件数：")
            .append(request.attachments.size)
            .append("\n")

        request.transcriptMarkdown?.takeIf { it.isNotBlank() }?.let {
            builder.append("\n## 会议纪要节选\n")
                .append(it.trim())
                .append("\n")
        }
        return builder.toString().trim()
    }

    private fun mapError(error: Throwable): AiCoreException = when (error) {
        is AiCoreException -> error
        is ApiException -> AiCoreException(
            source = AiCoreErrorSource.DASH_SCOPE,
            reason = AiCoreErrorReason.REMOTE,
            message = "DashScope 返回错误：${error.message ?: "未知"}",
            suggestion = "检查模型、Prompt 或网络是否符合 DashScope 限制",
            cause = error
        )
        is TimeoutCancellationException -> AiCoreException(
            source = AiCoreErrorSource.DASH_SCOPE,
            reason = AiCoreErrorReason.TIMEOUT,
            message = "DashScope 请求超时",
            suggestion = "可增大 AiCoreConfig.dashscopeRequestTimeoutMillis",
            cause = error
        )
        is IOException -> AiCoreException(
            source = AiCoreErrorSource.DASH_SCOPE,
            reason = AiCoreErrorReason.NETWORK,
            message = "DashScope 网络异常：${error.message ?: "未知"}",
            suggestion = "检查网络连接或设置企业代理",
            cause = error
        )
        else -> AiCoreException(
            source = AiCoreErrorSource.DASH_SCOPE,
            reason = AiCoreErrorReason.UNKNOWN,
            message = error.message ?: "DashScope 未知错误",
            cause = error
        )
    }

    private fun missingCredentialsError(): AiCoreException = AiCoreException(
        source = AiCoreErrorSource.DASH_SCOPE,
        reason = AiCoreErrorReason.MISSING_CREDENTIALS,
        message = "DashScope API Key 未配置",
        suggestion = "在 local.properties 写入 DASHSCOPE_API_KEY"
    )

    companion object {
        private val TAG = "${LogTags.AI_CORE}/DashscopeAiChat"
        private const val RETRY_BACKOFF_MS = 300L
        private const val SYSTEM_PROMPT_BASE =
            "你是智能销售助手，需根据聊天内容输出结构化洞察和下一步行动。生成内容需简洁并可直接粘贴。"
    }
}
