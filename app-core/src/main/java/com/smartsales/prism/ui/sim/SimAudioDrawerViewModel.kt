package com.smartsales.prism.ui.sim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.data.audio.SimBadgeSyncSkippedReason
import com.smartsales.prism.data.audio.SimBadgeSyncTrigger
import com.smartsales.prism.data.audio.simBadgeSyncSuccessMessage
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource as DomainAudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.drawers.AudioItemState
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimAudioEntry(
    val item: AudioItemState,
    val preview: String,
    val failureMessage: String? = null,
    val isTestImport: Boolean = false
)

@HiltViewModel
class SimAudioDrawerViewModel @Inject constructor(
    private val repository: SimAudioRepository
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()
    private val _expandedAudioIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedAudioIds: StateFlow<Set<String>> = _expandedAudioIds
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    private var hasAttemptedAutoSyncThisSession = false

    val entries: StateFlow<List<SimAudioEntry>> = repository.getAudioFiles()
        .map { files -> files.map { it.toSimEntry() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun toggleStar(audioId: String) {
        repository.toggleStar(audioId)
    }

    fun startTranscription(audioId: String) {
        viewModelScope.launch {
            try {
                repository.startTranscription(audioId)
            } catch (e: Exception) {
                _uiEvents.emit(e.message ?: "转写失败")
            }
        }
    }

    fun importTestAudio(uriString: String) {
        viewModelScope.launch {
            try {
                repository.addLocalAudio(uriString)
                _uiEvents.emit("测试音频已导入")
            } catch (e: Exception) {
                _uiEvents.emit(e.message ?: "导入测试音频失败")
            }
        }
    }

    fun seedDebugFailureScenario() {
        viewModelScope.launch {
            try {
                repository.seedDebugFailureScenario()
                _uiEvents.emit("调试失败场景已就绪")
            } catch (e: Exception) {
                _uiEvents.emit(e.message ?: "调试失败场景创建失败")
            }
        }
    }

    fun seedDebugMissingSectionsScenario() {
        viewModelScope.launch {
            try {
                repository.seedDebugMissingSectionsScenario()
                _uiEvents.emit("缺失区块调试场景已就绪")
            } catch (e: Exception) {
                _uiEvents.emit(e.message ?: "缺失区块调试场景创建失败")
            }
        }
    }

    fun seedDebugFallbackScenario() {
        viewModelScope.launch {
            try {
                repository.seedDebugFallbackScenario()
                _uiEvents.emit("回退展示调试场景已就绪")
            } catch (e: Exception) {
                _uiEvents.emit(e.message ?: "回退展示调试场景创建失败")
            }
        }
    }

    fun syncFromBadgeManually() {
        if (!canStartSimAudioSync(hasAttemptedAutoSync = false, isSyncing = _isSyncing.value)) {
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val outcome = repository.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
                if (outcome.skippedReason != SimBadgeSyncSkippedReason.ALREADY_RUNNING) {
                    _uiEvents.emit(simBadgeSyncSuccessMessage(outcome.importedCount))
                }
            } catch (e: Exception) {
                _uiEvents.emit(e.message ?: "同步失败")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun maybeAutoSyncFromBadge() {
        if (!canStartSimAudioSync(hasAttemptedAutoSyncThisSession, _isSyncing.value)) {
            return
        }
        hasAttemptedAutoSyncThisSession = true

        viewModelScope.launch {
            if (!repository.canSyncFromBadge()) {
                return@launch
            }

            _isSyncing.value = true
            try {
                val outcome = repository.syncFromBadge(SimBadgeSyncTrigger.AUTO)
                if (shouldShowSimAudioAutoSyncMessage(outcome.importedCount)) {
                    _uiEvents.emit(simBadgeSyncSuccessMessage(outcome.importedCount))
                }
            } catch (e: Exception) {
                _uiEvents.emit(e.message ?: "同步失败")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun resetSyncSession() {
        hasAttemptedAutoSyncThisSession = false
    }

    suspend fun startTranscriptionForChat(audioId: String) {
        repository.startTranscription(audioId)
    }

    fun createDiscussion(audioId: String): SimAudioDiscussion? {
        val audio = repository.getAudio(audioId) ?: return null
        if (audio.status != TranscriptionStatus.TRANSCRIBED) {
            viewModelScope.launch { _uiEvents.emit("仅已转写条目可进入讨论") }
            return null
        }

        return SimAudioDiscussion(
            audioId = audioId,
            title = audio.filename,
            summary = audio.summary ?: "音频讨论"
        )
    }

    suspend fun getArtifacts(audioId: String): TingwuJobArtifacts? {
        return repository.getArtifacts(audioId)
    }

    fun toggleExpanded(audioId: String) {
        _expandedAudioIds.update { expandedIds ->
            toggleExpandedAudioIds(expandedIds, audioId)
        }
    }

    fun resetExpandedCards() {
        _expandedAudioIds.value = emptySet()
    }

    fun bindDiscussion(audioId: String, sessionId: String) {
        repository.bindSession(audioId, sessionId)
    }

    fun getBoundSessionId(audioId: String): String? {
        return repository.getBoundSessionId(audioId)
    }

    fun getAudio(audioId: String): AudioFile? {
        return repository.getAudio(audioId)
    }

    private fun AudioFile.toSimEntry(): SimAudioEntry {
        return SimAudioEntry(
            item = AudioItemState(
                id = id,
                filename = filename,
                timeDisplay = timeDisplay,
                source = when (source) {
                    DomainAudioSource.SMARTBADGE -> AudioSource.SMARTBADGE
                    DomainAudioSource.PHONE -> AudioSource.PHONE
                },
                status = when (status) {
                    TranscriptionStatus.PENDING -> AudioStatus.PENDING
                    TranscriptionStatus.TRANSCRIBING -> AudioStatus.TRANSCRIBING
                    TranscriptionStatus.TRANSCRIBED -> AudioStatus.TRANSCRIBED
                },
                isStarred = isStarred,
                summary = summary,
                progress = if (status == TranscriptionStatus.TRANSCRIBING) progress else null
            ),
            preview = when (status) {
                TranscriptionStatus.PENDING -> lastErrorMessage ?: "真实本地音频已就绪，点击开始转写。"
                TranscriptionStatus.TRANSCRIBING -> "Tingwu 正在处理音频，SIM 仅做进度与展示承接。"
                TranscriptionStatus.TRANSCRIBED -> summary ?: "转写结果已可阅读。"
            },
            failureMessage = lastErrorMessage,
            isTestImport = isTestImport
        )
    }
}

internal fun toggleExpandedAudioIds(
    expandedIds: Set<String>,
    audioId: String
): Set<String> {
    return if (expandedIds.contains(audioId)) {
        expandedIds - audioId
    } else {
        expandedIds + audioId
    }
}

internal fun canStartSimAudioSync(
    hasAttemptedAutoSync: Boolean,
    isSyncing: Boolean
): Boolean {
    return !hasAttemptedAutoSync && !isSyncing
}

internal fun shouldShowSimAudioAutoSyncMessage(importedCount: Int): Boolean = importedCount > 0

data class SimAudioDiscussion(
    val audioId: String,
    val title: String,
    val summary: String
)
