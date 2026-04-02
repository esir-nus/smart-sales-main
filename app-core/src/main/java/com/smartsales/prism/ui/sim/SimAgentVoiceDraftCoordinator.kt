package com.smartsales.prism.ui.sim

import com.smartsales.prism.data.audio.SimRealtimeSpeechEvent
import com.smartsales.prism.data.audio.SimRealtimeSpeechFailureReason
import com.smartsales.prism.data.audio.SimRealtimeSpeechRecognitionResult
import com.smartsales.prism.data.audio.SimRealtimeSpeechRecognizer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class SimAgentVoiceDraftCoordinator(
    private val realtimeSpeechRecognizer: SimRealtimeSpeechRecognizer,
    private val scope: CoroutineScope,
    private val bridge: SimAgentUiBridge
) {

    private var voiceDraftRequestId = 0L

    fun startVoiceDraft(): Boolean {
        return startVoiceDraft(SimVoiceDraftInteractionMode.HOLD_TO_SEND)
    }

    fun onVoiceDraftPermissionRequested() {
        val state = bridge.getVoiceDraftState()
        if (
            state.isRecording ||
            state.isProcessing ||
            state.awaitingMicPermission ||
            bridge.getIsSending() ||
            bridge.getCurrentSchedulerFollowUpContext() != null ||
            realtimeSpeechRecognizer.isListening()
        ) {
            return
        }
        bridge.setVoiceDraftState(
            state.copy(
                awaitingMicPermission = true,
                interactionMode = SimVoiceDraftInteractionMode.HOLD_TO_SEND,
                errorMessage = null
            )
        )
    }

    fun onVoiceDraftPermissionResult(granted: Boolean) {
        val state = bridge.getVoiceDraftState()
        if (!granted) {
            bridge.setVoiceDraftState(
                state.copy(
                    awaitingMicPermission = false,
                    interactionMode = SimVoiceDraftInteractionMode.HOLD_TO_SEND,
                    errorMessage = "无法录音：未授予麦克风权限"
                )
            )
            bridge.setToastMessage("无法录音：未授予麦克风权限")
            return
        }
        if (state.awaitingMicPermission) {
            startVoiceDraft(SimVoiceDraftInteractionMode.TAP_TO_SEND)
        }
    }

    fun finishVoiceDraft() {
        val state = bridge.getVoiceDraftState()
        if (!state.isRecording) return
        val requestId = beginVoiceDraftProcessing(state)
        scope.launch {
            when (val result = resolveVoiceDraftResult(requestId)) {
                is SimRealtimeSpeechRecognitionResult.Success -> {
                    if (!isActiveVoiceDraftRequest(requestId)) return@launch
                    bridge.setInputText(result.text)
                    bridge.setVoiceDraftState(SimVoiceDraftUiState())
                }

                is SimRealtimeSpeechRecognitionResult.Failure -> {
                    if (!isActiveVoiceDraftRequest(requestId)) return@launch
                    bridge.setInputText("")
                    bridge.setVoiceDraftState(
                        SimVoiceDraftUiState(errorMessage = result.message)
                    )
                    if (result.reason != SimRealtimeSpeechFailureReason.CANCELLED) {
                        bridge.setToastMessage(result.message)
                    }
                }
            }
        }
    }

    fun cancelVoiceDraft() {
        invalidateVoiceDraftRequests()
        runCatching { realtimeSpeechRecognizer.cancelListening() }
        bridge.setVoiceDraftState(SimVoiceDraftUiState())
    }

    fun handleVoiceDraftEvent(event: SimRealtimeSpeechEvent) {
        when (event) {
            SimRealtimeSpeechEvent.ListeningStarted -> Unit

            SimRealtimeSpeechEvent.CaptureLimitReached -> {
                if (bridge.getVoiceDraftState().isRecording) {
                    finishVoiceDraft()
                }
            }

            is SimRealtimeSpeechEvent.PartialTranscript -> {
                updateVoiceDraftLiveTranscript(event.text)
            }

            is SimRealtimeSpeechEvent.FinalTranscript -> {
                updateVoiceDraftLiveTranscript(event.text)
            }

            is SimRealtimeSpeechEvent.Failure -> {
                val state = bridge.getVoiceDraftState()
                if (state.isRecording && !state.isProcessing) {
                    bridge.setVoiceDraftState(SimVoiceDraftUiState(errorMessage = event.message))
                    if (event.reason != SimRealtimeSpeechFailureReason.CANCELLED) {
                        bridge.setToastMessage(event.message)
                    }
                }
            }

            SimRealtimeSpeechEvent.Cancelled -> {
                val state = bridge.getVoiceDraftState()
                if (state.isRecording && !state.isProcessing) {
                    bridge.setVoiceDraftState(SimVoiceDraftUiState())
                }
            }
        }
    }

    fun clearVoiceDraftErrorOnManualInput() {
        val state = bridge.getVoiceDraftState()
        if (state.errorMessage != null) {
            bridge.setVoiceDraftState(state.copy(errorMessage = null))
        }
    }

    private fun startVoiceDraft(
        interactionMode: SimVoiceDraftInteractionMode
    ): Boolean {
        val state = bridge.getVoiceDraftState()
        if (
            state.isRecording ||
            state.isProcessing ||
            bridge.getIsSending() ||
            bridge.getCurrentSchedulerFollowUpContext() != null ||
            realtimeSpeechRecognizer.isListening()
        ) {
            return false
        }
        return runCatching {
            realtimeSpeechRecognizer.startListening()
            bridge.setVoiceDraftState(
                state.copy(
                    isRecording = true,
                    isProcessing = false,
                    awaitingMicPermission = false,
                    interactionMode = interactionMode,
                    liveTranscript = "",
                    errorMessage = null
                )
            )
            true
        }.getOrElse {
            bridge.setVoiceDraftState(
                SimVoiceDraftUiState(
                    errorMessage = "当前无法开始录音，请重试。"
                )
            )
            bridge.setToastMessage("当前无法开始录音，请重试。")
            false
        }
    }

    private fun beginVoiceDraftProcessing(
        state: SimVoiceDraftUiState
    ): Long {
        val requestId = invalidateVoiceDraftRequests()
        bridge.setInputText(state.liveTranscript.trim())
        bridge.setVoiceDraftState(
            state.copy(
                isRecording = false,
                isProcessing = true,
                awaitingMicPermission = false,
                liveTranscript = "",
                errorMessage = null
            )
        )
        return requestId
    }

    private suspend fun resolveVoiceDraftResult(
        requestId: Long
    ): SimRealtimeSpeechRecognitionResult {
        return try {
            val result = realtimeSpeechRecognizer.finishListening()
            if (isActiveVoiceDraftRequest(requestId)) {
                result
            } else {
                SimRealtimeSpeechRecognitionResult.Failure(
                    reason = SimRealtimeSpeechFailureReason.CANCELLED,
                    message = "语音识别已取消"
                )
            }
        } catch (_: CancellationException) {
            SimRealtimeSpeechRecognitionResult.Failure(
                reason = SimRealtimeSpeechFailureReason.CANCELLED,
                message = "语音识别已取消"
            )
        } catch (_: IllegalStateException) {
            SimRealtimeSpeechRecognitionResult.Failure(
                reason = SimRealtimeSpeechFailureReason.ERROR,
                message = "当前无法识别语音，请重试"
            )
        }
    }

    private fun invalidateVoiceDraftRequests(): Long {
        voiceDraftRequestId += 1L
        return voiceDraftRequestId
    }

    private fun isActiveVoiceDraftRequest(requestId: Long): Boolean {
        return requestId == voiceDraftRequestId
    }

    private fun updateVoiceDraftLiveTranscript(transcript: String) {
        val state = bridge.getVoiceDraftState()
        if (!state.isRecording && !state.isProcessing) return
        bridge.setVoiceDraftState(
            state.copy(
                liveTranscript = transcript.trim(),
                errorMessage = null
            )
        )
    }
}
