package com.smartsales.prism.ui.drawers.scheduler

import com.smartsales.core.pipeline.IntentOrchestrator
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class SchedulerViewModelAudioBridge(
    val getActiveDayOffset: () -> Int,
    val getPipelineStatus: () -> String?,
    val setPipelineStatus: (String?) -> Unit
)

internal class SchedulerViewModelAudioIngressCoordinator(
    private val scope: CoroutineScope,
    private val asrService: AsrService,
    private val intentOrchestrator: IntentOrchestrator,
    private val bridge: SchedulerViewModelAudioBridge
) {

    fun processAudio(file: File) {
        bridge.setPipelineStatus("语音转写中...")
        scope.launch {
            try {
                when (val result = asrService.transcribe(file)) {
                    is AsrResult.Success -> handleTranscript(result.text)
                    is AsrResult.Error -> {
                        android.util.Log.e("SchedulerVM", "Transcribe Result Error: ${result.message}")
                        bridge.setPipelineStatus("转写失败: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SchedulerVM", "ProcessAudio crash: ", e)
                bridge.setPipelineStatus("系统错误")
            } finally {
                clearPipelineStatusWithDelay()
            }
        }
    }

    fun injectTranscript(
        text: String,
        displayedDateIso: String? = null,
        source: DevInjectSource = DevInjectSource.DEV_PANEL
    ) {
        scope.launch {
            try {
                android.util.Log.d(
                    "SchedulerVM",
                    "injectTranscript: source=$source displayedDateIso=${displayedDateIso ?: "null"} text=$text"
                )
                handleTranscript(text = text, displayedDateIso = displayedDateIso)
            } catch (e: Exception) {
                android.util.Log.e("SchedulerVM", "InjectTranscript crash: ", e)
                bridge.setPipelineStatus("系统错误")
            } finally {
                clearPipelineStatusWithDelay()
            }
        }
    }

    private suspend fun handleTranscript(
        text: String,
        displayedDateIso: String? = null
    ) {
        bridge.setPipelineStatus("处理意图...")
        var schedulerWriteProven = false
        var inspirationWriteProven = false
        val effectiveDisplayedDateIso = displayedDateIso ?: LocalDate.now()
            .plusDays(bridge.getActiveDayOffset().toLong())
            .toString()

        intentOrchestrator.processInput(
            text,
            isVoice = true,
            displayedDateIso = effectiveDisplayedDateIso
        ).collect { result ->
            when (result) {
                is PipelineResult.PathACommitted -> {
                    schedulerWriteProven = true
                    if (result.task.hasConflict) {
                        bridge.setPipelineStatus("⚠️ 已创建，发现冲突")
                        PipelineValve.tag(
                            checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
                            payloadSize = result.task.id.hashCode(),
                            summary = "Uni-D caution status emitted",
                            rawDataDump = result.task.conflictSummary
                        )
                        PipelineValve.tag(
                            checkpoint = PipelineValve.Checkpoint.UI_RENDERED,
                            payloadSize = result.task.id.hashCode(),
                            summary = "Uni-D conflict-visible scheduler render requested",
                            rawDataDump = result.task.conflictSummary
                        )
                        android.util.Log.d(
                            "SchedulerVM",
                            "processAudio: caution status emitted after conflict PathACommitted"
                        )
                    } else {
                        bridge.setPipelineStatus("✅ 搞定")
                        android.util.Log.d(
                            "SchedulerVM",
                            "processAudio: success status emitted after PathACommitted"
                        )
                    }
                }

                is PipelineResult.InspirationCommitted -> {
                    inspirationWriteProven = true
                    bridge.setPipelineStatus("💡 已保存灵感")
                    android.util.Log.d(
                        "SchedulerVM",
                        "processAudio: inspiration status emitted after InspirationCommitted"
                    )
                }

                is PipelineResult.ConversationalReply -> {
                    if (!schedulerWriteProven && !inspirationWriteProven) {
                        bridge.setPipelineStatus("未创建日程")
                        android.util.Log.d(
                            "SchedulerVM",
                            "processAudio: conversational reply without scheduler write proof"
                        )
                    }
                }

                is PipelineResult.MutationProposal -> {
                    bridge.setPipelineStatus("请确认执行")
                }

                is PipelineResult.TaskCommandProposal -> {
                    bridge.setPipelineStatus("请确认执行")
                }

                is PipelineResult.ClarificationNeeded,
                is PipelineResult.DisambiguationIntercepted -> {
                    bridge.setPipelineStatus("需要进一步确认")
                }

                else -> Unit
            }
        }

        if (!schedulerWriteProven && !inspirationWriteProven && bridge.getPipelineStatus() == "处理意图...") {
            bridge.setPipelineStatus("未创建日程")
            android.util.Log.d("SchedulerVM", "processAudio: pipeline completed without scheduler write proof")
        }
    }

    private suspend fun clearPipelineStatusWithDelay() {
        delay(2000)
        bridge.setPipelineStatus(null)
    }
}
