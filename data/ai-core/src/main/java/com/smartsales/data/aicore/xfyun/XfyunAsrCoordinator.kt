// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunAsrCoordinator.kt
// 模块：:data:ai-core
// 说明：提供讯飞 ASR 的提交与轮询协调器（最小闭环：upload→poll→markdown）
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.debug.XfyunTraceSnapshot
import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

sealed interface XfyunAsrJobState {
    data object Idle : XfyunAsrJobState

    data class InProgress(
        val jobId: String,
        val progressPercent: Int,
        val statusLabel: String? = null,
    ) : XfyunAsrJobState

    data class Completed(
        val jobId: String,
        val transcriptMarkdown: String,
    ) : XfyunAsrJobState

    data class Failed(
        val jobId: String,
        val reason: String,
        val errorCode: String? = null,
    ) : XfyunAsrJobState
}

@Singleton
class XfyunAsrCoordinator @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val api: XfyunAsrApi,
    private val configProvider: XfyunConfigProvider,
    private val parser: XfyunOrderResultParser,
    private val traceStore: XfyunTraceStore,
    private val aiParaSettingsProvider: AiParaSettingsProvider,
    private val postXFyun: PostXFyun,
) {

    private val jobContextByOrderId = ConcurrentHashMap<String, JobContext>()

    suspend fun submitTranscription(
        file: File,
        language: String,
        durationMs: Long?,
    ): Result<String> = withContext(dispatchers.io) {
        val credentials = configProvider.credentials()
        validateCredentials(credentials)?.let { return@withContext Result.Error(it) }
        if (!file.exists() || !file.canRead()) {
            return@withContext Result.Error(
                AiCoreException(
                    source = AiCoreErrorSource.XFYUN,
                    reason = AiCoreErrorReason.IO,
                    message = "音频文件不存在或不可读：${file.absolutePath}"
                )
            )
        }

        // 重要：
        // - Option 1：默认仅允许 transfer。
        // - translate/predict 未开通能力时会触发 failType=11，因此必须在发请求前阻断。
        val xfyunSettings = aiParaSettingsProvider.snapshot().transcription.xfyun
        val resolvedResultType = runCatching {
            xfyunSettings.result.resolveApiValueOrThrow(xfyunSettings.capabilities)
        }.getOrElse { throwable ->
            val mapped = mapError(throwable)
            traceStore.recordFailure(desc = mapped.message)
            return@withContext Result.Error(mapped)
        }

        // 重要：signatureRandom 需在 upload 与 getResult 之间保持一致。
        val signatureRandom = XfyunIdFactory.random16()
        val normalizedLanguage = normalizeLanguage(language)
        val context = JobContext(signatureRandom = signatureRandom, resultType = resolvedResultType)
        runCatching {
            val upload = api.upload(
                file = file,
                resultType = context.resultType,
                requestedLanguage = normalizedLanguage,
                durationMs = durationMs,
                signatureRandom = signatureRandom,
                preferredAttempt = context.preferredAttempt
            )
            context.preferredAttempt = upload.attemptUsed
            val jobId = buildJobId(upload.orderId)
            jobContextByOrderId[upload.orderId] = context
            AiCoreLogger.d(TAG, "XFyun submit 成功：jobId=$jobId")
            Result.Success(jobId)
        }.getOrElse {
            val mapped = mapError(it)
            AiCoreLogger.e(TAG, "XFyun submit 失败：${mapped.message}", mapped)
            Result.Error(mapped)
        }
    }

    fun observeJob(jobId: String): Flow<XfyunAsrJobState> = flow {
        val orderId = parseOrderId(jobId)
        if (orderId.isBlank()) {
            traceStore.recordFailure(desc = "无效的任务ID")
            emit(XfyunAsrJobState.Failed(jobId = jobId, reason = "无效的任务ID"))
            return@flow
        }
        val context = jobContextByOrderId[orderId]
        if (context == null) {
            traceStore.recordFailure(desc = "任务上下文缺失")
            emit(XfyunAsrJobState.Failed(jobId = jobId, reason = "任务上下文缺失，请重新提交"))
            return@flow
        }
        val start = System.currentTimeMillis()
        val deadline = start + POLL_TIMEOUT_MS
        var pollCount = 0

        emit(XfyunAsrJobState.InProgress(jobId = jobId, progressPercent = 5, statusLabel = "已提交"))
        // 记录首次查询使用的 resultType，避免在轮询中重复写入
        traceStore.recordResultTypeAttempt(
            phase = "getResult",
            resultType = context.resultType,
            downgradedBecauseFailType11 = false
        )

        while (System.currentTimeMillis() < deadline) {
            val progress = computeProgressPercent(start, deadline)
            var result = runCatching {
                withContext(dispatchers.io) {
                    api.getResult(
                        orderId = orderId,
                        signatureRandom = context.signatureRandom,
                        resultType = context.resultType,
                        preferredAttempt = context.preferredAttempt
                    )
                }
            }.getOrElse { throwable ->
                val mapped = mapError(throwable)
                traceStore.recordFailure(desc = mapped.message)
                emit(
                    XfyunAsrJobState.Failed(
                        jobId = jobId,
                        reason = mapped.message ?: "查询失败",
                    )
                )
                jobContextByOrderId.remove(orderId)
                return@flow
            }

            // 兼容：如果账号未开通 predict/translate 等能力（failType=11），自动降级到 transfer 重新查询一次
            if (!context.downgradedBecauseFailType11 &&
                XfyunResultTypePolicy.shouldDowngradeOnFailType11(
                    status = result.status,
                    failType = result.failType,
                    resultType = context.resultType
                )
            ) {
                context.downgradedBecauseFailType11 = true
                context.resultType = RESULT_TYPE_TRANSFER
                traceStore.recordResultTypeAttempt(
                    phase = "getResult",
                    resultType = context.resultType,
                    downgradedBecauseFailType11 = true
                )
                result = runCatching {
                    withContext(dispatchers.io) {
                        api.getResult(
                            orderId = orderId,
                            signatureRandom = context.signatureRandom,
                            resultType = context.resultType,
                            preferredAttempt = context.preferredAttempt
                        )
                    }
                }.getOrElse { throwable ->
                    val mapped = mapError(throwable)
                    traceStore.recordFailure(desc = mapped.message)
                    emit(
                        XfyunAsrJobState.Failed(
                            jobId = jobId,
                            reason = mapped.message ?: "查询失败",
                        )
                    )
                    jobContextByOrderId.remove(orderId)
                    return@flow
                }
            }

            context.preferredAttempt = result.attemptUsed
            pollCount += 1
            traceStore.recordPollProgress(
                pollCount = pollCount,
                elapsedMs = (System.currentTimeMillis() - start).coerceAtLeast(0L)
            )
            val status = result.status
            when (status) {
                4 -> {
                    val parsed = parser.parseTranscript(result.orderResult)
                    // 重要：用于验证 PostXFyun 是否启用/是否产生可疑边界/LLM 是否总是返回 NONE，避免误判为“未生效”。
                    val postResult = runCatching {
                        postXFyun.polish(
                            originalMarkdown = parsed.markdown,
                            segments = parsed.segments,
                        )
                    }.getOrElse {
                        // 重要：后处理属于“可选增强”，任何失败都必须回退原始渲染，不影响主流程。
                        PostXFyunResult(polishedMarkdown = parsed.markdown, repairs = emptyList())
                    }
                    runCatching {
                        // 重要：验证切片——存储最近一次“原始/后处理”逐字稿，供 HUD 一键复制对比。
                        traceStore.recordPostXfyunMarkdown(
                            originalMarkdown = parsed.markdown,
                            polishedMarkdown = postResult.polishedMarkdown,
                        )
                    }
                    postResult.debugInfo?.let { debugInfo ->
                        traceStore.recordPostXfyunSettings(
                            XfyunTraceSnapshot.PostXfyunSettingsDebug(
                                enabled = debugInfo.settings.enabled,
                                maxRepairsPerTranscript = debugInfo.settings.maxRepairsPerTranscript,
                                suspiciousGapThresholdMs = debugInfo.settings.suspiciousGapThresholdMs,
                                confidenceThreshold = debugInfo.settings.confidenceThreshold,
                                modelEffective = debugInfo.settings.modelEffective,
                                promptLength = debugInfo.settings.promptLength,
                                promptPreview = debugInfo.settings.promptPreview,
                                promptSha256 = debugInfo.settings.promptSha256,
                            )
                        )
                        traceStore.recordPostXfyunSuspicious(
                            debugInfo.suspiciousBoundaries.map { boundary ->
                                XfyunTraceSnapshot.PostXfyunSuspiciousBoundary(
                                    boundaryIndex = boundary.boundaryIndex,
                                    gapMs = boundary.gapMs,
                                    prevSpeakerId = boundary.prevSpeakerId,
                                    nextSpeakerId = boundary.nextSpeakerId,
                                    prevExcerpt = boundary.prevExcerpt,
                                    nextExcerpt = boundary.nextExcerpt,
                                )
                            }
                        )
                        traceStore.recordPostXfyunDecisions(
                            debugInfo.decisions.map { decision ->
                                XfyunTraceSnapshot.PostXfyunDecisionDebug(
                                    attemptIndex = decision.attemptIndex,
                                    boundaryIndex = decision.boundaryIndex,
                                    gapMs = decision.gapMs,
                                    prevSpeakerId = decision.prevSpeakerId,
                                    nextSpeakerId = decision.nextSpeakerId,
                                    prevExcerpt = decision.prevExcerpt,
                                    nextExcerpt = decision.nextExcerpt,
                                    modelUsed = decision.modelUsed,
                                    action = decision.action.name,
                                    span = decision.span,
                                    confidence = decision.confidence,
                                    reason = decision.reason,
                                    rawResponsePreview = decision.rawResponsePreview,
                                    parseStatus = decision.parseStatus,
                                    errorHint = decision.errorHint,
                                )
                            }
                        )
                        traceStore.recordPostXfyunRunStats(
                            candidatesCount = debugInfo.candidatesCount,
                            arbitrationsAttempted = debugInfo.arbitrationsAttempted,
                            arbitrationBudget = debugInfo.arbitrationBudget,
                            repairsApplied = debugInfo.repairsApplied,
                        )
                    }
                    if (postResult.repairs.isNotEmpty()) {
                        val mapped = postResult.repairs.map { repair ->
                            XfyunTraceSnapshot.PostXfyunRepair(
                                boundaryIndex = repair.boundaryIndex,
                                action = repair.action.name,
                                span = repair.span,
                                confidence = repair.confidence,
                                gapMs = repair.gapMs,
                                prevSpeakerId = repair.prevSpeakerId,
                                nextSpeakerId = repair.nextSpeakerId,
                                beforePrevLine = repair.beforePrevLine,
                                beforeNextLine = repair.beforeNextLine,
                                afterPrevLine = repair.afterPrevLine,
                                afterNextLine = repair.afterNextLine,
                            )
                        }
                        traceStore.recordPostXfyunRepairs(mapped)
                    }
                    val markdown = postResult.polishedMarkdown
                    // 重要：用于 HUD 判断是否返回了 rl（说话人标签）
                    traceStore.recordSuccess(
                        resultHasRoleLabels = markdown.contains("- 发言人 ")
                    )
                    emit(XfyunAsrJobState.Completed(jobId = jobId, transcriptMarkdown = markdown))
                    jobContextByOrderId.remove(orderId)
                    return@flow
                }

                3, 0, null -> {
                    emit(
                        XfyunAsrJobState.InProgress(
                            jobId = jobId,
                            progressPercent = progress,
                            statusLabel = "处理中…"
                        )
                    )
                }

                -1 -> {
                    val failType = result.failType?.toString() ?: "未知"
                    emit(
                        XfyunAsrJobState.Failed(
                            jobId = jobId,
                            reason = "讯飞转写失败（failType=$failType）",
                            errorCode = failType
                        )
                    )
                    jobContextByOrderId.remove(orderId)
                    return@flow
                }

                else -> {
                    emit(
                        XfyunAsrJobState.Failed(
                            jobId = jobId,
                            reason = "讯飞转写异常（status=$status）",
                            errorCode = status.toString()
                        )
                    )
                    jobContextByOrderId.remove(orderId)
                    return@flow
                }
            }

            delay(POLL_INTERVAL_MS)
        }

        emit(XfyunAsrJobState.Failed(jobId = jobId, reason = "讯飞转写超时，请稍后重试"))
        traceStore.recordFailure(desc = "讯飞转写超时")
        jobContextByOrderId.remove(orderId)
    }

    private fun validateCredentials(credentials: XfyunCredentials): AiCoreException? {
        val missing = when {
            credentials.appId.isBlank() -> "XFYUN_APP_ID"
            credentials.accessKeyId.isBlank() -> "XFYUN_ACCESS_KEY_ID"
            credentials.accessKeySecret.isBlank() -> "XFYUN_ACCESS_KEY_SECRET"
            else -> null
        }
        return missing?.let {
            AiCoreException(
                source = AiCoreErrorSource.XFYUN,
                reason = AiCoreErrorReason.MISSING_CREDENTIALS,
                message = "讯飞配置缺失（$it）",
                suggestion = "请在 local.properties 填写 $it"
            )
        }
    }

    private fun normalizeLanguage(raw: String): String {
        // 讯飞当前文档仅接受 autodialect / autominor；上层可能传 zh-CN，这里做最小映射。
        val trimmed = raw.trim()
        return when (trimmed) {
            "autodialect", "autominor" -> trimmed
            else -> "autodialect"
        }
    }

    private fun computeProgressPercent(start: Long, deadline: Long): Int {
        val total = (deadline - start).coerceAtLeast(1)
        val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(0)
        val ratio = (elapsed.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
        return (5 + ratio * 90).toInt().coerceIn(5, 95)
    }

    private fun mapError(error: Throwable): AiCoreException = when (error) {
        is AiCoreException -> error
        else -> AiCoreException(
            source = AiCoreErrorSource.XFYUN,
            reason = AiCoreErrorReason.UNKNOWN,
            message = error.message ?: "讯飞未知错误",
            cause = error
        )
    }

    private fun buildJobId(orderId: String): String = "${JOB_PREFIX}$orderId"

    private fun parseOrderId(jobId: String): String =
        jobId.removePrefix(JOB_PREFIX).trim()

    private data class JobContext(
        val signatureRandom: String,
        var resultType: String,
        var preferredAttempt: XfyunRequestAttempt? = null,
        var downgradedBecauseFailType11: Boolean = false,
    )

    private companion object {
        private const val JOB_PREFIX = "xfyun-"
        private const val RESULT_TYPE_TRANSFER = "transfer"
        private const val POLL_INTERVAL_MS = 5_000L
        private const val POLL_TIMEOUT_MS = 600_000L
        private const val TAG = "SmartSalesAi/XFyunCoordinator"
    }
}
