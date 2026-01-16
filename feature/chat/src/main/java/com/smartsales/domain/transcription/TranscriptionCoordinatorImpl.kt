// File: feature/chat/src/main/java/com/smartsales/domain/transcription/TranscriptionCoordinatorImpl.kt
// Module: :feature:chat
// Summary: Transcription task coordination implementation - batch gate and window filtering
// Author: created on 2026-01-07

package com.smartsales.domain.transcription


import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import com.smartsales.feature.media.audiofiles.AudioTranscriptionJobState
import com.smartsales.data.aicore.debug.TingwuTraceStore
import com.smartsales.feature.chat.home.transcription.TranscriptionUiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Transcription Coordinator implementation.
 */
@Singleton
class TranscriptionCoordinatorImpl @Inject constructor(
    private val transcriptionCoordinator: AudioTranscriptionCoordinator,
    private val tingwuTraceStore: TingwuTraceStore,
    optionalConfig: Optional<AiCoreConfig>
) : TranscriptionCoordinator {

    private val _state = MutableStateFlow(TranscriptionUiState())
    override val state: StateFlow<TranscriptionUiState> = _state.asStateFlow()


    override fun observeJob(jobId: String): Flow<AudioTranscriptionJobState> {
        return transcriptionCoordinator.observeJob(jobId)
    }



    override fun reset() {
        _state.update {
            TranscriptionUiState()
        }
    }

    override fun markFinal() {
        _state.update { it.copy(isFinal = true) }
    }

    override fun setCurrentMessageId(messageId: String?) {
        _state.update { it.copy(currentMessageId = messageId) }
    }

    override fun startTranscription(jobId: String) {
        _state.update {
            it.copy(
                jobId = jobId,
                isActive = true,
                isFinal = false
            )
        }
    }



    override suspend fun runTranscription(
        jobId: String,
        fileName: String,
        progressMessageId: String,
        onProgressUpdate: (percent: Int, messageId: String) -> Unit,
        onCompleted: (transcriptMarkdown: String, messageId: String) -> Unit,
        onFailed: (reason: String, messageId: String) -> Unit
    ) {
        // Observe job state
        val terminalState = observeJob(jobId)
            .onEach { state ->
                when (state) {
                    is AudioTranscriptionJobState.InProgress -> {
                        onProgressUpdate(state.progressPercent, progressMessageId)
                    }
                    else -> Unit
                }
            }
            .first { state ->
                // Wait for Completed with non-blank content (TingwuRunner emits twice:
                // first empty from PollingLoop, then populated after transcript fetch)
                (state is AudioTranscriptionJobState.Completed && state.transcriptMarkdown.isNotBlank())
                    || state is AudioTranscriptionJobState.Failed
            }

        when (terminalState) {
            is AudioTranscriptionJobState.Completed -> {
                onCompleted(terminalState.transcriptMarkdown, progressMessageId)
            }
            is AudioTranscriptionJobState.Failed -> {
                onFailed(terminalState.reason.ifBlank { "转写失败" }, progressMessageId)
            }
            else -> Unit
        }
    }
}
