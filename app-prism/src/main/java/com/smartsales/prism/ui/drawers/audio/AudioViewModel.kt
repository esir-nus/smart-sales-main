package com.smartsales.prism.ui.drawers.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.audio.AudioSource as DomainSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.ui.drawers.AudioItemState
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 音频抽屉 ViewModel — 将领域模型映射为 UI 状态
 */
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {
    
    val audioItems: StateFlow<List<AudioItemState>> = audioRepository.getAudioFiles()
        .map { files -> files.map { it.toUiState() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun syncFromDevice() {
        viewModelScope.launch {
            audioRepository.syncFromDevice()
        }
    }
    
    fun startTranscription(audioId: String) {
        viewModelScope.launch {
            audioRepository.startTranscription(audioId)
        }
    }
    
    fun deleteAudio(audioId: String) {
        audioRepository.deleteAudio(audioId)
    }
    
    fun toggleStar(audioId: String) {
        audioRepository.toggleStar(audioId)
    }
    
    private fun AudioFile.toUiState() = AudioItemState(
        id = id,
        filename = filename,
        timeDisplay = timeDisplay,
        source = when (source) {
            DomainSource.SMARTBADGE -> AudioSource.SMARTBADGE
            DomainSource.PHONE -> AudioSource.PHONE
        },
        status = when (status) {
            TranscriptionStatus.PENDING -> AudioStatus.PENDING
            TranscriptionStatus.TRANSCRIBING -> AudioStatus.TRANSCRIBING
            TranscriptionStatus.TRANSCRIBED -> AudioStatus.TRANSCRIBED
        },
        isStarred = isStarred,
        summary = summary,
        progress = if (status == TranscriptionStatus.TRANSCRIBING) progress else null
    )
}
