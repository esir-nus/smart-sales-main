// File: feature/chat/src/main/java/com/smartsales/feature/chat/audio/AudioViewModel.kt
// Module: :feature:chat
// Summary: ViewModel for audio and device management
// Author: created on 2026-01-07 (extracted from HomeViewModel M9)

package com.smartsales.feature.chat.audio

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.metahub.MetaHub
import com.smartsales.core.metahub.SessionMetadata
import com.smartsales.core.util.Result
import com.smartsales.feature.connectivity.DeviceConnectionManager
import com.smartsales.feature.connectivity.ConnectionState
import com.smartsales.feature.media.MediaSyncCoordinator
import com.smartsales.feature.media.MediaSyncState
import com.smartsales.feature.chat.platform.MediaInputCoordinator
import com.smartsales.feature.chat.home.HomeNavigationRequest
import com.smartsales.feature.chat.home.DeviceConnectionStateUi
import com.smartsales.feature.chat.home.TranscriptionChatRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AudioViewModel: Manages audio file handling and device connection state.
 * 
 * Responsibilities:
 * - Device connection monitoring → deviceSnapshot
 * - Audio sync monitoring → audioSummary  
 * - Audio file picking → triggers transcription
 * - Audio recovery hint → helps user recover from interrupted transcription
 * - Navigation requests → device setup, device manager, audio files
 * 
 * This ViewModel was extracted from HomeViewModel to achieve single responsibility.
 */
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val deviceConnectionManager: DeviceConnectionManager,
    private val mediaSyncCoordinator: MediaSyncCoordinator,
    private val mediaInputCoordinator: MediaInputCoordinator,
    private val metaHub: MetaHub
) : ViewModel() {

    private val _state = MutableStateFlow(AudioUiState())
    val state: StateFlow<AudioUiState> = _state

    // Tracks current session for recovery hint persistence
    private var currentSessionId: String = DEFAULT_SESSION_ID
    private var activelyTranscribing: Boolean = false

    init {
        observeDeviceConnection()
        observeMediaSync()
    }

    /**
     * Update the current session context.
     * Called by HomeViewModel when session changes.
     */
    fun setSessionContext(sessionId: String) {
        currentSessionId = sessionId
        viewModelScope.launch {
            refreshAudioRecoveryHintFromMetaHub()
        }
    }

    /**
     * Mark transcription as actively running.
     * This suppresses the recovery hint while transcription is in progress.
     */
    fun setTranscribingState(isTranscribing: Boolean) {
        activelyTranscribing = isTranscribing
        viewModelScope.launch {
            refreshAudioRecoveryHintFromMetaHub()
        }
    }

    /**
     * Refresh device and audio status.
     * Triggers lightweight queries to update state.
     */
    fun onRefreshDeviceAndAudio() {
        viewModelScope.launch {
            when (val result = deviceConnectionManager.queryNetworkStatus()) {
                is Result.Error -> {
                    _state.update { current ->
                        val message = result.throwable.message ?: "刷新设备状态失败"
                        current.copy(snackbarMessage = current.snackbarMessage ?: message)
                    }
                }
                else -> Unit
            }
            when (val syncResult = mediaSyncCoordinator.triggerSync()) {
                is Result.Error -> {
                    _state.update { current ->
                        val message = syncResult.throwable.message ?: "刷新音频状态失败"
                        current.copy(snackbarMessage = current.snackbarMessage ?: message)
                    }
                }
                else -> Unit
            }
        }
    }

    /**
     * Navigate to appropriate screen based on device connection state.
     */
    fun onTapDeviceBanner() {
        val snapshot = _state.value.deviceSnapshot
        val destination = when (snapshot?.connectionState) {
            DeviceConnectionStateUi.CONNECTED -> HomeNavigationRequest.DeviceManager
            DeviceConnectionStateUi.CONNECTING,
            DeviceConnectionStateUi.DISCONNECTED,
            DeviceConnectionStateUi.WAITING_FOR_NETWORK,
            DeviceConnectionStateUi.ERROR,
            null -> HomeNavigationRequest.DeviceSetup
        }
        _state.update { it.copy(navigationRequest = destination) }
    }

    /**
     * Navigate to audio files screen.
     */
    fun onTapAudioSummary() {
        _state.update { it.copy(navigationRequest = HomeNavigationRequest.AudioFiles) }
    }

    /**
     * Handle audio file picked by user.
     * Saves locally and returns TranscriptionChatRequest to caller.
     */
    suspend fun onAudioFilePicked(uri: Uri, sessionId: String): Result<TranscriptionChatRequest> {
        val startedAt = System.currentTimeMillis()
        activelyTranscribing = true
        _state.update { 
            it.copy(
                showAudioRecoveryHint = false, 
                audioRecoveryHintStartedAt = null,
                isInputBusy = true,
                isBusy = true,
                snackbarMessage = null
            ) 
        }
        persistAudioTaskStartedMarker(startedAt)

        val result = mediaInputCoordinator.handleAudioPick(uri, sessionId)
        when (result) {
            is Result.Success -> {
                _state.update { 
                    it.copy(
                        isInputBusy = false, 
                        isBusy = false, 
                        snackbarMessage = "音频已上传，正在转写…"
                    ) 
                }
            }
            is Result.Error -> {
                _state.update {
                    it.copy(
                        isInputBusy = false,
                        isBusy = false,
                        snackbarMessage = result.throwable.message ?: "音频处理失败"
                    )
                }
            }
        }
        return result
    }

    /**
     * Called when transcription completes successfully.
     */
    fun onTranscriptionCompleted() {
        activelyTranscribing = false
        viewModelScope.launch {
            persistAudioTaskFinishedMarker(System.currentTimeMillis())
        }
    }

    /**
     * Called when transcription fails.
     */
    fun onTranscriptionFailed(reason: String) {
        activelyTranscribing = false
        viewModelScope.launch {
            refreshAudioRecoveryHintFromMetaHub()
            _state.update { it.copy(snackbarMessage = it.snackbarMessage ?: reason) }
        }
    }

    /**
     * User dismissed the audio recovery hint.
     */
    fun dismissAudioRecoveryHint() {
        val startedAt = _state.value.audioRecoveryHintStartedAt ?: return
        viewModelScope.launch {
            val existing = runCatching { metaHub.getSession(currentSessionId) }.getOrNull()
            val base = existing ?: SessionMetadata(sessionId = currentSessionId)
            val updated = base.copy(audioRecoveryHintDismissedForStartedAt = startedAt)
            runCatching { metaHub.upsertSession(updated) }
                .onSuccess { refreshAudioRecoveryHint(updated) }
        }
    }

    /**
     * Clear snackbar message.
     */
    fun onDismissSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Navigation request consumed by UI.
     */
    fun onNavigationConsumed() {
        _state.update { it.copy(navigationRequest = null) }
    }

    // ========== Private Observers ==========

    private fun observeDeviceConnection() {
        applyDeviceSnapshot(deviceConnectionManager.state.value)
        viewModelScope.launch {
            deviceConnectionManager.state.collectLatest { connection ->
                applyDeviceSnapshot(connection)
            }
        }
    }

    private fun applyDeviceSnapshot(connection: ConnectionState) {
        val snapshot = connection.toDeviceSnapshot()
        _state.update { current ->
            val shouldSurfaceError =
                snapshot.connectionState == DeviceConnectionStateUi.ERROR &&
                    snapshot.errorSummary != null &&
                    current.snackbarMessage == null
            current.copy(
                deviceSnapshot = snapshot,
                snackbarMessage = if (shouldSurfaceError) snapshot.errorSummary else current.snackbarMessage
            )
        }
    }

    private fun observeMediaSync() {
        applyAudioSummary(mediaSyncCoordinator.state.value)
        viewModelScope.launch {
            mediaSyncCoordinator.state.collectLatest { syncState ->
                applyAudioSummary(syncState)
            }
        }
    }

    private fun applyAudioSummary(syncState: MediaSyncState) {
        val summary = syncState.toAudioSummaryUi()
        _state.update { current ->
            val shouldSurfaceError =
                syncState.errorMessage != null && current.snackbarMessage == null
            current.copy(
                audioSummary = summary,
                snackbarMessage = if (shouldSurfaceError) syncState.errorMessage else current.snackbarMessage
            )
        }
    }

    // ========== Audio Recovery Hint Logic ==========

    private fun refreshAudioRecoveryHint(meta: SessionMetadata?) {
        val startedAt = meta?.lastAudioTaskStartedAt
        val finishedAt = meta?.lastAudioTaskFinishedAt
        val dismissedAt = meta?.audioRecoveryHintDismissedForStartedAt
        // Only show hint if task started but not finished, not dismissed, and not actively transcribing
        val show = startedAt != null &&
            finishedAt == null &&
            dismissedAt != startedAt &&
            !activelyTranscribing
        _state.update {
            it.copy(
                showAudioRecoveryHint = show,
                audioRecoveryHintStartedAt = if (show) startedAt else null
            )
        }
    }

    private suspend fun refreshAudioRecoveryHintFromMetaHub() {
        val meta = runCatching { metaHub.getSession(currentSessionId) }.getOrNull()
        refreshAudioRecoveryHint(meta)
    }

    private suspend fun persistAudioTaskStartedMarker(startedAt: Long) {
        val existing = runCatching { metaHub.getSession(currentSessionId) }.getOrNull()
        val base = existing ?: SessionMetadata(sessionId = currentSessionId)
        val updated = base.copy(
            lastAudioTaskStartedAt = startedAt,
            lastAudioTaskFinishedAt = null,
            audioRecoveryHintDismissedForStartedAt = null
        )
        runCatching { metaHub.upsertSession(updated) }
            .onSuccess { refreshAudioRecoveryHint(updated) }
    }

    private suspend fun persistAudioTaskFinishedMarker(finishedAt: Long) {
        val existing = runCatching { metaHub.getSession(currentSessionId) }.getOrNull()
        val base = existing ?: SessionMetadata(sessionId = currentSessionId)
        val updated = base.copy(lastAudioTaskFinishedAt = finishedAt)
        runCatching { metaHub.upsertSession(updated) }
            .onSuccess { refreshAudioRecoveryHint(updated) }
    }

    companion object {
        private const val DEFAULT_SESSION_ID = "home-session"
    }
}
