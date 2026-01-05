package com.smartsales.feature.chat.home.transcription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.feature.chat.core.transcription.V1BatchIndexPrefixGate
import com.smartsales.feature.chat.core.transcription.V1TingwuWindowedChunkBuilder
import com.smartsales.feature.media.audiofiles.AudioTranscriptionBatchEvent
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.data.aicore.debug.TingwuTraceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject
import android.util.Log

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/transcription/TranscriptionViewModel.kt
// 模块：:feature:chat
// 说明：转写协调器 ViewModel，从 HomeScreenViewModel 中提取
// 作者：创建于 2026-01-05

private const val TAG = "TranscriptionViewModel"

/**
 * 转写 ViewModel：负责 Tingwu 转写任务的协调、批次门禁和窗口过滤。
 *
 * 职责：
 * - 管理批次门禁（确保有序发布）
 * - 执行窗口过滤（宏窗口范围过滤）
 * - 暴露 Job 状态流和处理后的批次流
 * - 记录 Tingwu 追踪数据
 *
 * 设计原则：
 * - 纯协调器，不处理 UI 消息创建
 * - UI 消息创建由 HomeScreenViewModel 负责
 * - 暴露处理后的数据流供调用方消费
 */
@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val tingwuTraceStore: TingwuTraceStore,
    optionalConfig: Optional<AiCoreConfig>
) : ViewModel() {

    private val _state = MutableStateFlow(TranscriptionUiState())
    val state: StateFlow<TranscriptionUiState> = _state.asStateFlow()

    // 批次门禁：确保批次按顺序发布
    private val batchGate = V1BatchIndexPrefixGate<AudioTranscriptionBatchEvent.BatchReleased>()

    // 窗口过滤开关
    private val enableV1TingwuMacroWindowFilter =
        optionalConfig.orElse(AiCoreConfig()).enableV1TingwuMacroWindowFilter

    /**
     * 观察转写任务状态。
     */
    fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
        return transcriptionCoordinator.observeJob(jobId)
    }

    /**
     * 观察处理后的批次（经过门禁和窗口过滤）。
     *
     * 该流会自动处理：
     * - 批次门禁（确保有序发布）
     * - 窗口过滤（如果启用）
     * - Tingwu 追踪记录
     */
    fun observeProcessedBatches(jobId: String): Flow<ProcessedBatch> = flow {
        transcriptionCoordinator.observeBatches(jobId).collect { event ->
            when (event) {
                is AudioTranscriptionBatchEvent.BatchReleased -> {
                    // 检查是否已标记为 final
                    if (_state.value.isFinal) {
                        Log.w(
                            TAG,
                            "event=transcription_batch_after_final " +
                                "jobId=${event.jobId} batchIndex=${event.batchIndex}"
                        )
                        return@collect
                    }

                    // 记录 Tingwu 追踪数据
                    tingwuTraceStore.record(
                        batchPlanRule = event.ruleLabel,
                        batchPlanBatchSize = event.batchSize,
                        batchPlanTotalBatches = event.totalBatches,
                        batchPlanCurrentBatchIndex = event.batchIndex,
                        v1BatchPlanRule = event.v1BatchPlanRule,
                        v1BatchDurationMs = event.v1BatchDurationMs,
                        v1OverlapMs = event.v1OverlapMs,
                        v1BatchPlanTotalBatches = event.v1TotalBatches,
                        v1BatchPlanCurrentBatchIndex = event.v1CurrentBatchIndex
                    )

                    // 通过门禁发布批次
                    val releasables = batchGate.offer(event.batchIndex, event)

                    // 处理所有可发布的批次
                    for (released in releasables) {
                        val effectiveChunk = processChunk(released)
                        val isFinal = released.isFinal

                        // 发布处理后的批次
                        emit(
                            ProcessedBatch(
                                batchIndex = released.batchIndex,
                                effectiveChunk = effectiveChunk,
                                isFinal = isFinal,
                                event = released
                            )
                        )

                        // 如果是最终批次，标记状态
                        if (isFinal) {
                            _state.update { it.copy(isFinal = true) }
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理批次数据：应用窗口过滤，返回有效的 markdown 块。
     */
    fun processChunk(event: AudioTranscriptionBatchEvent.BatchReleased): String {
        val window = event.v1Window
        val timedSegments = event.timedSegments

        // 应用宏窗口过滤（如果启用）
        return if (enableV1TingwuMacroWindowFilter && window != null && timedSegments != null) {
            val result = V1TingwuWindowedChunkBuilder.buildWindowedMarkdownChunkResult(
                absStartMs = window.absStartMs,
                absEndMs = window.absEndMs,
                timedSegments = timedSegments
            )

            Log.d(
                TAG,
                "event=v1_tingwu_window_filter_applied " +
                    "batchIndex=${event.batchIndex} " +
                    "absStartMs=${window.absStartMs} " +
                    "absEndMs=${window.absEndMs} " +
                    "segmentsIn=${result.segmentsInCount} " +
                    "segmentsOut=${result.segmentsOutCount} " +
                    "chunkChars=${result.chunk.length}"
            )

            result.chunk
        } else {
            // 回退到原始 markdown
            event.markdownChunk
        }
    }

    /**
     * 重置转写状态（开始新的转写时调用）。
     */
    fun reset() {
        batchGate.reset()
        _state.update {
            TranscriptionUiState()
        }
    }

    /**
     * 标记转写为最终状态。
     */
    fun markFinal() {
        _state.update { it.copy(isFinal = true) }
    }

    /**
     * 设置当前处理的消息ID。
     */
    fun setCurrentMessageId(messageId: String?) {
        _state.update { it.copy(currentMessageId = messageId) }
    }

    /**
     * 启动转写任务。
     */
    fun startTranscription(jobId: String) {
        _state.update {
            it.copy(
                jobId = jobId,
                isActive = true,
                isFinal = false
            )
        }
    }
}
