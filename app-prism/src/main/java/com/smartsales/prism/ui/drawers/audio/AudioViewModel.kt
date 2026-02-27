package com.smartsales.prism.ui.drawers.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.audio.AudioSource as DomainSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.ui.drawers.AudioItemState
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri

/**
 * 音频抽屉 ViewModel — 将领域模型映射为 UI 状态
 */
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()
    
    val audioItems: StateFlow<List<AudioItemState>> = audioRepository.getAudioFiles()
        .map { files -> files.map { it.toUiState() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun syncFromDevice() {
        viewModelScope.launch {
            try {
                audioRepository.syncFromDevice()
            } catch (e: Exception) {
                _uiEvents.emit("同步失败: ${e.message}")
            }
        }
    }
    
    fun startTranscription(audioId: String) {
        viewModelScope.launch {
            try {
                audioRepository.startTranscription(audioId)
            } catch (e: Exception) {
                _uiEvents.emit("转写失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    fun uploadLocalAudio(uri: Uri) {
        viewModelScope.launch {
            try {
                audioRepository.addLocalAudio(uri.toString())
            } catch (e: Exception) {
                _uiEvents.emit("上传失败: ${e.message}")
            }
        }
    }
    
    fun deleteAudio(audioId: String) {
        audioRepository.deleteAudio(audioId)
    }
    
    fun toggleStar(audioId: String) {
        audioRepository.toggleStar(audioId)
    }
    
    /**
     * 问AI — 创建或返回绑定的分析会话
     * @return 会话 ID，用于导航
     */
    fun onAskAi(audioId: String): String {
        // 检查是否已有绑定会话
        val existingSession = audioRepository.getBoundSessionId(audioId)
        if (existingSession != null) {
            return existingSession
        }
        
        // 获取音频文件信息
        val audio = audioRepository.getAudio(audioId)
        
        // 创建新会话
        val sessionId = historyRepository.createSession(
            clientName = "录音分析",
            summary = audio?.filename?.take(6) ?: "未命名",
            linkedAudioId = audioId
        )
        
        // 绑定会话到音频
        audioRepository.bindSession(audioId, sessionId)
        return sessionId
    }
    
    /**
     * Lazy load artifacts from JSON when the Audio Card is expanded
     */
    suspend fun getArtifacts(audioId: String): com.smartsales.prism.domain.tingwu.TingwuJobArtifacts? {
        return try {
            audioRepository.getArtifacts(audioId)
        } catch (e: Exception) {
            null
        }
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
