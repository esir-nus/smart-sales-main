package com.smartsales.prism.ui.sim

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.data.audio.isBadgeOriginAudio
import com.smartsales.prism.data.audio.SimAudioDeleteResult
import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.data.audio.SimBadgeSyncSkippedReason
import com.smartsales.prism.data.audio.SimBadgeSyncTrigger
import com.smartsales.prism.data.audio.SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE
import com.smartsales.prism.data.audio.simBadgeSyncSuccessMessage
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource as DomainAudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
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

private const val SIM_AUDIO_DRAWER_SYNC_LOG_TAG = "AudioPipeline"

@HiltViewModel
class SimAudioDrawerViewModel @Inject constructor(
    private val repository: SimAudioRepository,
    connectivityBridge: ConnectivityBridge
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()
    private val _deletedAudioIds = MutableSharedFlow<String>()
    val deletedAudioIds: SharedFlow<String> = _deletedAudioIds.asSharedFlow()
    private val _expandedAudioIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedAudioIds: StateFlow<Set<String>> = _expandedAudioIds
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    private val _pendingBadgeDeleteConfirmation =
        MutableStateFlow<SimBadgeDeleteConfirmationRequest?>(null)
    internal val pendingBadgeDeleteConfirmation: StateFlow<SimBadgeDeleteConfirmationRequest?> =
        _pendingBadgeDeleteConfirmation
    private var hasConfirmedBadgeDeleteThisSession = false
    private val badgeSyncAvailability: StateFlow<SimBadgeSyncAvailability> =
        connectivityBridge.managerStatus
            .map(::resolveSimBadgeSyncAvailability)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = resolveSimBadgeSyncAvailability(connectivityBridge.managerStatus.value)
            )

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

    fun deleteAudio(audioId: String) {
        val audio = repository.getAudio(audioId) ?: return
        val confirmationRequest = resolveSimBadgeDeleteConfirmationRequest(
            audio = audio,
            hasConfirmedBadgeDeleteThisSession = hasConfirmedBadgeDeleteThisSession
        )
        Log.d(
            SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
            "SIM badge delete gate audioId=${audio.id} filename=${audio.filename} source=${audio.source.name} badgeOrigin=${isBadgeOriginAudio(audio)} sessionConfirmed=$hasConfirmedBadgeDeleteThisSession showDialog=${confirmationRequest != null}"
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

    fun syncFromBadgeManually() {
        if (_isSyncing.value) {
            return
        }

        viewModelScope.launch {
            val availability = badgeSyncAvailability.value
            Log.d(
                SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                "SIM manual badge sync tapped availability=${availability.name.lowercase()} isSyncing=${_isSyncing.value}"
            )
            val gateDecision = resolveSimBadgeManualSyncGateDecision(
                availability = availability,
                canSyncFromBadge = { repository.canSyncFromBadge() }
            )
            Log.d(
                SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                "SIM manual badge sync gate availability=${availability.name.lowercase()} branch=${gateDecision.branch.name.lowercase()} blocked=${gateDecision.blockedMessage != null}"
            )
            if (gateDecision.blockedMessage != null) {
                _uiEvents.emit(gateDecision.blockedMessage)
                return@launch
            }

            _isSyncing.value = true
            try {
                val outcome = repository.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
                if (outcome.skippedReason != SimBadgeSyncSkippedReason.ALREADY_RUNNING) {
                    _uiEvents.emit(simBadgeSyncSuccessMessage(outcome))
                }
            } catch (e: Exception) {
                Log.w(
                    SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                    "SIM manual badge sync failed after gate branch=${gateDecision.branch.name.lowercase()} error=${e.message}"
                )
                _uiEvents.emit(e.message ?: "同步失败")
            } finally {
                _isSyncing.value = false
            }
        }
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

    private fun deleteAudioConfirmed(audioId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteAudio(audioId)) {
                is SimAudioDeleteResult.Badge -> {
                    _deletedAudioIds.emit(audioId)
                    _uiEvents.emit(
                        if (result.remoteDeleteSucceeded) {
                            "已删除录音，徽章源文件已清理。"
                        } else {
                            "已删除本地录音；徽章端删除待重试，同步前不会重新导入。"
                        }
                    )
                }

                is SimAudioDeleteResult.LocalOnly -> {
                    _deletedAudioIds.emit(audioId)
                    _uiEvents.emit("已删除录音")
                }
                SimAudioDeleteResult.NotFound -> Unit
            }
        }
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

@Immutable
internal data class SimBadgeDeleteConfirmationRequest(
    val audioId: String,
    val filename: String
)

internal fun resolveSimBadgeDeleteConfirmationRequest(
    audio: AudioFile?,
    hasConfirmedBadgeDeleteThisSession: Boolean
): SimBadgeDeleteConfirmationRequest? {
    if (audio == null) return null
    if (!isBadgeOriginAudio(audio)) return null
    if (hasConfirmedBadgeDeleteThisSession) return null
    return SimBadgeDeleteConfirmationRequest(
        audioId = audio.id,
        filename = audio.filename
    )
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

internal enum class SimBadgeSyncAvailability {
    READY,
    BLE_CONNECTED_NETWORK_PENDING,
    BLE_CONNECTED_NETWORK_OFFLINE,
    UNAVAILABLE
}

internal enum class SimBadgeManualSyncGateBranch {
    MANAGER_PENDING_BLOCK,
    MANAGER_OFFLINE_BLOCK,
    STRICT_PRECHECK_ALLOWED,
    STRICT_PRECHECK_BLOCKED
}

internal data class SimBadgeManualSyncGateDecision(
    val branch: SimBadgeManualSyncGateBranch,
    val blockedMessage: String?
)

internal const val SIM_BADGE_SYNC_NETWORK_PENDING_MESSAGE =
    "徽章蓝牙已连接，正在确认设备网络状态，暂时不能同步录音。请稍候后重试。"

internal const val SIM_BADGE_SYNC_NETWORK_OFFLINE_MESSAGE =
    "徽章蓝牙已连接，但设备当前未接入可用网络，暂时不能同步录音。请检查徽章 Wi‑Fi 后重试。"

internal fun resolveSimBadgeSyncAvailability(
    managerStatus: BadgeManagerStatus
): SimBadgeSyncAvailability {
    return when (managerStatus) {
        is BadgeManagerStatus.Ready -> SimBadgeSyncAvailability.READY
        is BadgeManagerStatus.BlePairedNetworkUnknown ->
            SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_PENDING
        is BadgeManagerStatus.BlePairedNetworkOffline ->
            SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_OFFLINE
        else -> SimBadgeSyncAvailability.UNAVAILABLE
    }
}

internal suspend fun resolveSimBadgeManualSyncBlockedMessage(
    availability: SimBadgeSyncAvailability,
    canSyncFromBadge: suspend () -> Boolean
): String? {
    return resolveSimBadgeManualSyncGateDecision(
        availability = availability,
        canSyncFromBadge = canSyncFromBadge
    ).blockedMessage
}

internal suspend fun resolveSimBadgeManualSyncGateDecision(
    availability: SimBadgeSyncAvailability,
    canSyncFromBadge: suspend () -> Boolean
): SimBadgeManualSyncGateDecision {
    return when (availability) {
        SimBadgeSyncAvailability.READY -> SimBadgeManualSyncGateDecision(
            branch = SimBadgeManualSyncGateBranch.STRICT_PRECHECK_ALLOWED,
            blockedMessage = null
        )
        SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_PENDING ->
            SimBadgeManualSyncGateDecision(
                branch = SimBadgeManualSyncGateBranch.MANAGER_PENDING_BLOCK,
                blockedMessage = SIM_BADGE_SYNC_NETWORK_PENDING_MESSAGE
            )
        SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_OFFLINE ->
            SimBadgeManualSyncGateDecision(
                branch = SimBadgeManualSyncGateBranch.MANAGER_OFFLINE_BLOCK,
                blockedMessage = SIM_BADGE_SYNC_NETWORK_OFFLINE_MESSAGE
            )
        SimBadgeSyncAvailability.UNAVAILABLE -> {
            if (canSyncFromBadge()) {
                SimBadgeManualSyncGateDecision(
                    branch = SimBadgeManualSyncGateBranch.STRICT_PRECHECK_ALLOWED,
                    blockedMessage = null
                )
            } else {
                SimBadgeManualSyncGateDecision(
                    branch = SimBadgeManualSyncGateBranch.STRICT_PRECHECK_BLOCKED,
                    blockedMessage = SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE
                )
            }
        }
    }
}

data class SimAudioDiscussion(
    val audioId: String,
    val title: String,
    val summary: String
)
