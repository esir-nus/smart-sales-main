package com.smartsales.prism.ui.drawers.audio

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.data.audio.isBadgeOriginAudio
import com.smartsales.prism.data.connectivity.registry.DeviceRegistry
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioDeleteResult
import com.smartsales.prism.domain.audio.AudioRepository
import com.smartsales.prism.domain.audio.AudioSource as DomainSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.repository.HistoryRepository
import com.smartsales.prism.ui.drawers.AudioItemState
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUDIO_BADGE_DELETE_LOG_TAG = "AudioDeleteGate"

/**
 * 音频抽屉 ViewModel — 将领域模型映射为 UI 状态
 */
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val historyRepository: HistoryRepository,
    private val deviceRegistry: DeviceRegistry
) : ViewModel() {
    
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()
    private val _pendingBadgeDeleteConfirmation =
        MutableStateFlow<AudioBadgeDeleteConfirmationRequest?>(null)
    internal val pendingBadgeDeleteConfirmation = _pendingBadgeDeleteConfirmation.asStateFlow()
    private var hasConfirmedBadgeDeleteThisSession = false
    
    val audioItems: StateFlow<List<AudioItemState>> = audioRepository.getAudioFiles()
        .map { files -> files.map { it.toUiState() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
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
        val audio = audioRepository.getAudio(audioId) ?: return
        val confirmationRequest = resolveAudioBadgeDeleteConfirmationRequest(
            audio = audio,
            hasConfirmedBadgeDeleteThisSession = hasConfirmedBadgeDeleteThisSession
        )
        Log.d(
            AUDIO_BADGE_DELETE_LOG_TAG,
            "shared delete gate audioId=${audio.id} filename=${audio.filename} source=${audio.source.name} badgeOrigin=${isBadgeOriginAudio(audio)} sessionConfirmed=$hasConfirmedBadgeDeleteThisSession showDialog=${confirmationRequest != null}"
        )
        if (confirmationRequest != null) {
            _pendingBadgeDeleteConfirmation.value = confirmationRequest
            return
        }
        deleteAudioConfirmed(audioId)
    }

    fun confirmBadgeDelete() {
        val pending = _pendingBadgeDeleteConfirmation.value ?: return
        hasConfirmedBadgeDeleteThisSession = true
        _pendingBadgeDeleteConfirmation.value = null
        deleteAudioConfirmed(pending.audioId)
    }

    fun dismissBadgeDeleteConfirmation() {
        _pendingBadgeDeleteConfirmation.value = null
    }

    fun resetDeleteConfirmationSession() {
        hasConfirmedBadgeDeleteThisSession = false
        _pendingBadgeDeleteConfirmation.value = null
    }
    
    fun toggleStar(audioId: String) {
        audioRepository.toggleStar(audioId)
    }
    
    /**
     * 问AI — 创建或返回绑定的分析会话
     * @return Pair<会话 ID, 是否为新创建>, 用于导航判断是否需要注入初始上下文
     */
    suspend fun onAskAi(audioId: String): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        // 检查是否已有绑定会话
        val existingSession = audioRepository.getBoundSessionId(audioId)
        if (existingSession != null) {
            return@withContext Pair(existingSession, false)
        }
        
        // 获取音频文件信息
        val audio = audioRepository.getAudio(audioId)
        
        // 创建新会话
        val sessionId = historyRepository.createSession(
            clientName = "录音分析",
            summary = audio?.filename?.take(6) ?: "未命名",
            linkedAudioId = audioId
        )
        
        // Wave 4: 构建 UI 预览卡片并直接写入 DB，实现 0-latency 展示
        try {
            val artifacts = audioRepository.getArtifacts(audioId)
            if (artifacts != null) {
                val overviewCard = buildOverviewCard(audio?.filename, audio?.timeDisplay, artifacts)
                // 写入虚拟的 Assistant 消息，这样一进入会话就会触发 UI 显示，以及后续的自动重命名
                historyRepository.saveMessage(
                    sessionId = sessionId,
                    isUser = false,
                    content = overviewCard,
                    orderIndex = 0
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioViewModel", "Failed to build or inject overview card", e)
        }
        
        // 绑定会话到音频
        audioRepository.bindSession(audioId, sessionId)
        Pair(sessionId, true)
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
    
    private fun buildOverviewCard(
        title: String?, 
        timeDisplay: String?, 
        artifacts: com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
    ): String = buildString {
        appendLine("### 已加载音频：${title ?: "未命名"}")
        if (timeDisplay != null) {
            appendLine("**时长**: $timeDisplay")
        }
        appendLine()
        
        val keyPoints = artifacts.smartSummary?.keyPoints
        if (!keyPoints.isNullOrEmpty()) {
            val keywordsStr = keyPoints.take(5).joinToString("，")
            appendLine("**核心要点**: $keywordsStr")
        }
        
        val summaryText = artifacts.smartSummary?.summary
        if (!summaryText.isNullOrBlank()) {
            val firstParagraph = summaryText.split("\n\n").firstOrNull()?.trim()
            if (!firstParagraph.isNullOrBlank()) {
                appendLine()
                appendLine("> ${firstParagraph.replace("\n", "\n> ")}")
            }
        }
        
        appendLine()
        appendLine("我已经加载了完整的音频原文和细节分析。请问你想了解什么？")
        
        appendLine()
        appendLine("---EXPAND---")
        appendLine("**完整系统摘要**")
        if (!summaryText.isNullOrBlank()) {
            appendLine(summaryText)
        } else {
            appendLine("无")
        }
        appendLine()
        appendLine("**详细讲演转写**")
        val transcript = artifacts.transcriptMarkdown
        if (!transcript.isNullOrBlank()) {
            appendLine(transcript)
        } else {
            appendLine("无")
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
        progress = if (status == TranscriptionStatus.TRANSCRIBING) progress else null,
        badgeLabel = resolveBadgeLabel(source, badgeMac)
    )

    private fun resolveBadgeLabel(source: DomainSource, badgeMac: String?): String? {
        if (source != DomainSource.SMARTBADGE || badgeMac == null) return null
        val device = deviceRegistry.findByMac(badgeMac)
        if (device != null) return device.displayName
        val parts = badgeMac.split(":")
        return if (parts.size >= 2) "...${parts.takeLast(2).joinToString(":")}" else null
    }

    private fun deleteAudioConfirmed(audioId: String) {
        viewModelScope.launch {
            when (val result = audioRepository.deleteAudio(audioId)) {
                is AudioDeleteResult.Badge -> {
                    _uiEvents.emit(
                        if (result.remoteDeleteSucceeded) {
                            "已删除录音，徽章源文件已清理。"
                        } else {
                            "已删除本地录音；徽章端删除待重试，同步前不会重新导入。"
                        }
                    )
                }

                is AudioDeleteResult.LocalOnly -> {
                    _uiEvents.emit("已删除录音")
                }

                AudioDeleteResult.NotFound -> Unit
            }
        }
    }
}

@Immutable
internal data class AudioBadgeDeleteConfirmationRequest(
    val audioId: String,
    val filename: String
)

internal fun resolveAudioBadgeDeleteConfirmationRequest(
    audio: AudioFile?,
    hasConfirmedBadgeDeleteThisSession: Boolean
): AudioBadgeDeleteConfirmationRequest? {
    if (audio == null) return null
    if (!isBadgeOriginAudio(audio)) return null
    if (hasConfirmedBadgeDeleteThisSession) return null
    return AudioBadgeDeleteConfirmationRequest(
        audioId = audio.id,
        filename = audio.filename
    )
}
