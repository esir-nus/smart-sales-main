// File: feature/chat/src/main/java/com/smartsales/feature/chat/audio/AudioUiState.kt
// Module: :feature:chat
// Summary: UI state for audio and device management
// Author: created on 2026-01-07 (extracted from HomeViewModel)

package com.smartsales.feature.chat.audio

import com.smartsales.feature.chat.home.AudioSummaryUi
import com.smartsales.feature.chat.home.DeviceSnapshotUi
import com.smartsales.feature.chat.home.HomeNavigationRequest

/**
 * Audio and device management UI state.
 * Extracted from HomeViewModel to focus on audio/device concerns independently.
 */
data class AudioUiState(
    val deviceSnapshot: DeviceSnapshotUi? = null,
    val audioSummary: AudioSummaryUi? = null,
    val showAudioRecoveryHint: Boolean = false,
    val audioRecoveryHintStartedAt: Long? = null,
    val isBusy: Boolean = false,
    val isInputBusy: Boolean = false,
    val navigationRequest: HomeNavigationRequest? = null,
    val snackbarMessage: String? = null
)
