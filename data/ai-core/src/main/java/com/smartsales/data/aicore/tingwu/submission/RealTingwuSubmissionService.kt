// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/submission/RealTingwuSubmissionService.kt
// Module: :data:ai-core
// Summary: Real implementation of TingwuSubmissionService - calls Tingwu API
// Author: created on 2026-01-14

package com.smartsales.data.aicore.tingwu.submission

import com.google.gson.Gson
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.TingwuCredentialsProvider
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.tingwu.api.TingwuApi
import com.smartsales.data.aicore.tingwu.api.TingwuCreateTaskRequest
import com.smartsales.data.aicore.tingwu.api.TingwuCustomPrompt
import com.smartsales.data.aicore.tingwu.api.TingwuCustomPromptContent
import com.smartsales.data.aicore.tingwu.api.TingwuDiarizationParameters
import com.smartsales.data.aicore.tingwu.api.TingwuSummarizationParameters
import com.smartsales.data.aicore.tingwu.api.TingwuTaskInput
import com.smartsales.data.aicore.tingwu.api.TingwuTaskParameters
import com.smartsales.data.aicore.tingwu.api.TingwuTranscodingParameters
import com.smartsales.data.aicore.tingwu.api.TingwuTranscriptionParameters
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TingwuSubmission"

/**
 * RealTingwuSubmissionService: Calls Tingwu createTranscriptionTask API.
 * 
 * Owns settings resolution internally - callers pass minimal input.
 */
@Singleton
class RealTingwuSubmissionService @Inject constructor(
    private val api: TingwuApi,
    private val credentialsProvider: TingwuCredentialsProvider,
    private val aiParaSettingsProvider: AiParaSettingsProvider,
    private val tingwuTraceStore: TingwuTraceStore,
    private val dispatchers: DispatcherProvider,
    private val gson: Gson
) : TingwuSubmissionService {

    override suspend fun submit(input: SubmissionInput): Result<SubmissionOutput> =
        withContext(dispatchers.io) {
            runCatching {
                val credentials = credentialsProvider.obtain()
                val tingwuSettings = aiParaSettingsProvider.snapshot().tingwu
                
                // Build diarization parameters
                val diarizationEnabled = input.diarizationEnabled && tingwuSettings.transcription.diarizationEnabled
                val diarizationParameters = if (diarizationEnabled) {
                    TingwuDiarizationParameters(
                        speakerCount = tingwuSettings.transcription.diarizationSpeakerCount,
                        outputLevel = tingwuSettings.transcription.diarizationOutputLevel
                    )
                } else {
                    null
                }
                
                // Resolve custom prompt
                val defaultCustomPrompt = tingwuSettings.customPrompt.contents.firstOrNull()
                val resolvedPromptName = input.customPromptName?.takeIf { it.isNotBlank() }
                    ?: defaultCustomPrompt?.name
                val resolvedPromptText = input.customPromptText?.takeIf { it.isNotBlank() }
                    ?: defaultCustomPrompt?.prompt
                val customPromptContent = if (resolvedPromptName != null && resolvedPromptText != null) {
                    TingwuCustomPromptContent(
                        name = resolvedPromptName,
                        prompt = resolvedPromptText,
                        model = defaultCustomPrompt?.model,
                        transType = defaultCustomPrompt?.transType
                    )
                } else {
                    null
                }
                val customPromptEnabled: Boolean? = if (tingwuSettings.customPrompt.enabled && customPromptContent != null) {
                    true
                } else {
                    null
                }
                
                val model = credentials.model
                
                // Build and send API request
                val request = TingwuCreateTaskRequest(
                    appKey = credentials.appKey,
                    input = TingwuTaskInput(
                        sourceLanguage = input.sourceLanguage,
                        taskKey = input.taskKey,
                        fileUrl = input.fileUrl
                    ),
                    parameters = TingwuTaskParameters(
                        transcription = TingwuTranscriptionParameters(
                            diarizationEnabled = diarizationEnabled,
                            diarization = diarizationParameters,
                            audioEventDetectionEnabled = tingwuSettings.transcription.audioEventDetectionEnabled,
                            model = model
                        ),
                        summarizationEnabled = tingwuSettings.summarization.enabled,
                        summarization = TingwuSummarizationParameters(
                            types = tingwuSettings.summarization.types
                        ),
                        autoChaptersEnabled = tingwuSettings.autoChaptersEnabled,
                        textPolishEnabled = tingwuSettings.textPolishEnabled,
                        pptExtractionEnabled = tingwuSettings.pptExtractionEnabled,
                        customPromptEnabled = customPromptEnabled,
                        customPrompt = customPromptEnabled?.let { TingwuCustomPrompt(contents = listOf(customPromptContent!!)) },
                        transcoding = TingwuTranscodingParameters(
                            targetAudioFormat = tingwuSettings.transcoding.targetAudioFormat
                        )
                    )
                )
                
                // Record trace (before call)
                runCatching {
                    tingwuTraceStore.record(
                        taskId = input.taskKey,
                        createRequestJson = gson.toJson(request)
                    )
                }
                
                val response = api.createTranscriptionTask(body = request)
                
                // Record trace (after call)
                runCatching {
                    tingwuTraceStore.record(
                        taskId = input.taskKey,
                        createResponseJson = gson.toJson(response)
                    )
                }
                
                // Validate response
                val data = requireData(
                    code = response.code,
                    message = response.message,
                    requestId = response.requestId,
                    data = response.data,
                    action = "CreateTask"
                )
                
                val taskId = data.taskId?.takeIf { it.isNotBlank() } ?: throw AiCoreException(
                    source = AiCoreErrorSource.TINGWU,
                    reason = AiCoreErrorReason.REMOTE,
                    message = "Tingwu CreateTask 未返回 TaskId",
                    suggestion = "请根据 tingwu-doc.md 核对请求体字段"
                )
                
                AiCoreLogger.d(TAG, "Tingwu 任务创建成功：taskId=$taskId requestId=${response.requestId}")
                
                SubmissionOutput(
                    taskId = taskId,
                    requestId = response.requestId ?: ""
                )
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(mapError(it)) }
            )
        }
    
    private fun <T> requireData(
        code: String?,
        message: String?,
        requestId: String?,
        data: T?,
        action: String
    ): T {
        if (!code.isNullOrBlank() && code != "0") {
            throw AiCoreException(
                source = AiCoreErrorSource.TINGWU,
                reason = AiCoreErrorReason.REMOTE,
                message = "Tingwu $action 失败：code=$code message=${message.orEmpty()}",
                suggestion = "参考 tingwu-doc.md 检查权限或参数，requestId=$requestId"
            )
        }
        return data ?: throw AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.REMOTE,
            message = "Tingwu $action 响应缺少 Data",
            suggestion = "确认 API 是否返回 Data.* 字段，requestId=$requestId"
        )
    }
    
    private fun mapError(error: Throwable): AiCoreException {
        return if (error is AiCoreException) error else AiCoreException(
            source = AiCoreErrorSource.TINGWU,
            reason = AiCoreErrorReason.UNKNOWN,
            message = error.message ?: "Unknown error during task submission"
        )
    }
}
