package com.smartsales.prism.ui.sim

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.data.connectivity.legacy.PhoneWifiProvider
import com.smartsales.prism.data.connectivity.legacy.currentNormalizedSsid
import com.smartsales.prism.data.audio.isBadgeOriginAudio
import com.smartsales.prism.data.audio.SimAudioDeleteResult
import com.smartsales.prism.data.audio.SimAudioRepository
import com.smartsales.prism.data.audio.IsolationBlockedSyncException
import com.smartsales.prism.data.audio.SimBadgeSyncIslandEvent
import com.smartsales.prism.data.audio.SimBadgeSyncSkippedReason
import com.smartsales.prism.data.audio.SimBadgeSyncTrigger
import com.smartsales.prism.data.audio.SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE
import com.smartsales.prism.data.audio.simBadgeSyncSuccessMessage
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.domain.audio.AudioSource as DomainAudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.ConnectivityPrompt
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.ui.drawers.AudioItemState
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.smartsales.prism.data.audio.SimBadgeSyncResultBranch

data class SimAudioEntry(
    val item: AudioItemState,
    val preview: String,
    val localAvailability: AudioLocalAvailability = AudioLocalAvailability.READY,
    val failureMessage: String? = null,
    val isTestImport: Boolean = false,
    val isBuiltInSeed: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val downloadTotalBytes: Long = 0L
)

private const val SIM_AUDIO_DRAWER_SYNC_LOG_TAG = "AudioPipeline"

@HiltViewModel
class SimAudioDrawerViewModel @Inject constructor(
    private val repository: SimAudioRepository,
    connectivityBridge: ConnectivityBridge,
    private val connectivityPrompt: ConnectivityPrompt,
    private val phoneWifiProvider: PhoneWifiProvider,
    @ApplicationContext context: Context
) : ViewModel() {

    private val deleteWarningPrefs = context.getSharedPreferences(
        "sim_audio_delete_warning", Context.MODE_PRIVATE
    )
    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()
    private val _deletedAudioIds = MutableSharedFlow<String>()
    val deletedAudioIds: SharedFlow<String> = _deletedAudioIds.asSharedFlow()
    private val _expandedAudioIds = MutableStateFlow<Set<String>>(emptySet())
    val expandedAudioIds: StateFlow<Set<String>> = _expandedAudioIds
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    private val _syncFeedback = MutableStateFlow<SimAudioSyncFeedback?>(null)
    internal val syncFeedback: StateFlow<SimAudioSyncFeedback?> = _syncFeedback
    private val _lastSyncTimestamp = MutableStateFlow<Instant?>(null)
    internal val lastSyncTimestamp: StateFlow<Instant?> = _lastSyncTimestamp
    private val _syncIslandEvents = MutableSharedFlow<SimBadgeSyncIslandEvent>(
        extraBufferCapacity = 4
    )
    internal val syncIslandEvents: SharedFlow<SimBadgeSyncIslandEvent> = _syncIslandEvents.asSharedFlow()
    private val _pendingBadgeDeleteConfirmation =
        MutableStateFlow<SimBadgeDeleteConfirmationRequest?>(null)
    internal val pendingBadgeDeleteConfirmation: StateFlow<SimBadgeDeleteConfirmationRequest?> =
        _pendingBadgeDeleteConfirmation
    private var syncFeedbackResetJob: Job? = null
    private var autoSyncDebounceJob: Job? = null
    private val badgeSyncAvailability: StateFlow<SimBadgeSyncAvailability> =
        connectivityBridge.managerStatus
            .map(::resolveSimBadgeSyncAvailability)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = resolveSimBadgeSyncAvailability(connectivityBridge.managerStatus.value)
            )

    init {
        // 监听连接状态，非 Ready → Ready 时自动触发 /list 同步（3s 防抖）
        viewModelScope.launch {
            var prevAvailability = badgeSyncAvailability.value
            badgeSyncAvailability.collect { current ->
                val wasNotReady = prevAvailability != SimBadgeSyncAvailability.READY
                val isNowReady = current == SimBadgeSyncAvailability.READY
                prevAvailability = current
                if (wasNotReady && isNowReady) {
                    scheduleAutoSync()
                }
            }
        }
    }

    val entries: StateFlow<List<SimAudioEntry>> = repository.getAudioFiles()
        .map { files -> files.map { it.toSimEntry() } }
        .onEach { entries ->
            Log.d(
                SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                "SIM audio drawer entries emitted count=${entries.size} downloading=${entries.count { it.localAvailability == AudioLocalAvailability.DOWNLOADING }} queued=${entries.count { it.localAvailability == AudioLocalAvailability.QUEUED }} maxDownloadProgress=${entries.maxOfOrNull { it.downloadProgress } ?: 0f}"
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun toggleStar(audioId: String) {
        repository.toggleStar(audioId)
    }

    fun deleteAudio(audioId: String) {
        val audio = repository.getAudio(audioId) ?: return
        val hasOptedOut = deleteWarningPrefs.getBoolean(PREF_KEY_OPTED_OUT_BADGE_DELETE_WARNING, false)
        val confirmationRequest = resolveSimBadgeDeleteConfirmationRequest(
            audio = audio,
            hasOptedOutBadgeDeleteWarning = hasOptedOut
        )
        Log.d(
            SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
            "SIM badge delete gate audioId=${audio.id} filename=${audio.filename} source=${audio.source.name} badgeOrigin=${isBadgeOriginAudio(audio)} optedOut=$hasOptedOut showDialog=${confirmationRequest != null}"
        )
        if (confirmationRequest != null) {
            _pendingBadgeDeleteConfirmation.value = confirmationRequest
            return
        }
        deleteAudioConfirmed(audioId)
    }

    fun confirmBadgeDelete(optOutWarning: Boolean = false) {
        val pending = _pendingBadgeDeleteConfirmation.value ?: return
        if (optOutWarning) {
            deleteWarningPrefs.edit().putBoolean(PREF_KEY_OPTED_OUT_BADGE_DELETE_WARNING, true).apply()
        }
        _pendingBadgeDeleteConfirmation.value = null
        deleteAudioConfirmed(pending.audioId)
    }

    fun dismissBadgeDeleteConfirmation() {
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
            val strictPrecheckOwnedByGate =
                shouldRunSimBadgeStrictPrecheckInGate(availability)
            var gateDecision: SimBadgeManualSyncGateDecision? = null
            Log.d(
                SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                "SIM manual badge sync tapped availability=${availability.name.lowercase()} isSyncing=${_isSyncing.value}"
            )
            try {
                if (strictPrecheckOwnedByGate) {
                    _isSyncing.value = true
                }

                gateDecision = resolveSimBadgeManualSyncGateDecision(
                    availability = availability,
                    canSyncFromBadge = { repository.canSyncFromBadge() }
                )
                Log.d(
                    SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                    "SIM manual badge sync gate availability=${availability.name.lowercase()} branch=${gateDecision.branch.name.lowercase()} blocked=${gateDecision.blockedMessage != null}"
                )
                val blockedMessage = gateDecision.blockedMessage
                if (blockedMessage != null) {
                    if (shouldPromptWifiMismatchForManualSyncBlock(gateDecision.branch)) {
                        Log.d(
                            SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                            "SIM manual badge sync prompt wifi_mismatch requested source=drawer_gate branch=${gateDecision.branch.name.lowercase()}"
                        )
                        requestWifiMismatchPrompt()
                    } else {
                        Log.d(
                            SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                            "SIM manual badge sync prompt wifi_mismatch skipped source=drawer_gate branch=${gateDecision.branch.name.lowercase()}"
                        )
                    }
                    showSyncFeedback(SimAudioSyncFeedback.DENIED, durationMillis = 1200L)
                    _uiEvents.emit(blockedMessage)
                    return@launch
                }

                if (!strictPrecheckOwnedByGate) {
                    _isSyncing.value = true
                }

                _syncIslandEvents.tryEmit(SimBadgeSyncIslandEvent.ManualSyncStarted)
                val outcome = if (strictPrecheckOwnedByGate) {
                    repository.syncFromBadgeAfterVerifiedReadiness(SimBadgeSyncTrigger.MANUAL)
                } else {
                    repository.syncFromBadge(SimBadgeSyncTrigger.MANUAL)
                }
                if (outcome.skippedReason != SimBadgeSyncSkippedReason.ALREADY_RUNNING) {
                    when (outcome.resultBranch) {
                        SimBadgeSyncResultBranch.QUEUED -> {
                            _lastSyncTimestamp.value = Instant.now()
                            _syncIslandEvents.tryEmit(
                                SimBadgeSyncIslandEvent.ManualSyncComplete(outcome.queuedCount)
                            )
                        }
                        SimBadgeSyncResultBranch.ALREADY_PRESENT,
                        SimBadgeSyncResultBranch.DEVICE_EMPTY -> {
                            _syncIslandEvents.tryEmit(SimBadgeSyncIslandEvent.AlreadyUpToDate)
                        }
                        null -> Unit
                    }
                    showSyncFeedback(SimAudioSyncFeedback.SYNCED)
                    _uiEvents.emit(simBadgeSyncSuccessMessage(outcome))
                    _lastSyncTimestamp.value = Instant.now()
                    val islandEvent = when {
                        outcome.queuedCount > 0 ->
                            SimBadgeSyncIslandEvent.ManualSyncComplete(outcome.queuedCount)
                        else -> SimBadgeSyncIslandEvent.AlreadyUpToDate
                    }
                    _syncIslandEvents.tryEmit(islandEvent)
                }
            } catch (e: IsolationBlockedSyncException) {
                // 疑似 AP 客户端隔离：ConnectivityModal 已通过 promptSuspectedIsolation 接管提示，
                // 此处仅收起同步进度，不另行展示错误 snackbar
                Log.w(
                    SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                    "SIM manual badge sync isolation blocked ip=${e.badgeIp}"
                )
                showSyncFeedback(SimAudioSyncFeedback.DENIED, durationMillis = 1200L)
            } catch (e: Exception) {
                Log.w(
                    SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                    "SIM manual badge sync failed after gate branch=${gateDecision?.branch?.name?.lowercase() ?: "unknown"} error=${e.message}"
                )
                showSyncFeedback(SimAudioSyncFeedback.ERROR)
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

    private fun scheduleAutoSync() {
        autoSyncDebounceJob?.cancel()
        autoSyncDebounceJob = viewModelScope.launch {
            delay(3_000L)
            if (_isSyncing.value) return@launch
            if (repository.shouldSuppressAutoSync()) return@launch
            try {
                _syncIslandEvents.tryEmit(SimBadgeSyncIslandEvent.ManualSyncStarted)
                val outcome = repository.syncFromBadge(SimBadgeSyncTrigger.AUTO)
                if (outcome.skippedReason == null) {
                    _lastSyncTimestamp.value = Instant.now()
                    val islandEvent = when {
                        outcome.queuedCount > 0 ->
                            SimBadgeSyncIslandEvent.ManualSyncComplete(outcome.queuedCount)
                        else -> SimBadgeSyncIslandEvent.AlreadyUpToDate
                    }
                    _syncIslandEvents.tryEmit(islandEvent)
                }
            } catch (e: Exception) {
                Log.d(
                    SIM_AUDIO_DRAWER_SYNC_LOG_TAG,
                    "SIM auto badge sync silent failure: ${e.message}"
                )
            }
        }
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
                TranscriptionStatus.PENDING -> when (localAvailability) {
                    AudioLocalAvailability.READY ->
                        lastErrorMessage ?: "真实本地音频已就绪，点击开始转写。"
                    AudioLocalAvailability.QUEUED ->
                        "录音已发现，等待后台同步"
                    AudioLocalAvailability.DOWNLOADING ->
                        "正在后台同步录音..."
                    AudioLocalAvailability.FAILED ->
                        lastErrorMessage ?: "录音同步失败，请重试"
                }
                TranscriptionStatus.TRANSCRIBING -> "Tingwu 正在处理音频，SIM 仅做进度与展示承接。"
                TranscriptionStatus.TRANSCRIBED -> summary ?: "转写结果已可阅读。"
            },
            localAvailability = localAvailability,
            failureMessage = lastErrorMessage,
            isTestImport = isTestImport,
            isBuiltInSeed = id == SIM_AUDIO_DEMO_SEED_ID,
            downloadProgress = downloadProgress,
            downloadedBytes = downloadedBytes,
            downloadTotalBytes = downloadTotalBytes
        )
    }

    private fun showSyncFeedback(
        feedback: SimAudioSyncFeedback,
        durationMillis: Long = 2200L
    ) {
        syncFeedbackResetJob?.cancel()
        _syncFeedback.value = feedback
        syncFeedbackResetJob = viewModelScope.launch {
            delay(durationMillis)
            if (_syncFeedback.value == feedback) {
                _syncFeedback.value = null
            }
        }
    }

    private suspend fun requestWifiMismatchPrompt() {
        connectivityPrompt.promptWifiMismatch(phoneWifiProvider.currentNormalizedSsid())
    }
}

@Immutable
internal data class SimBadgeDeleteConfirmationRequest(
    val audioId: String,
    val filename: String
)

private const val PREF_KEY_OPTED_OUT_BADGE_DELETE_WARNING = "opted_out_badge_delete_warning"

internal fun resolveSimBadgeDeleteConfirmationRequest(
    audio: AudioFile?,
    hasOptedOutBadgeDeleteWarning: Boolean
): SimBadgeDeleteConfirmationRequest? {
    if (audio == null) return null
    if (!isBadgeOriginAudio(audio)) return null
    if (hasOptedOutBadgeDeleteWarning) return null
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

internal fun shouldRunSimBadgeStrictPrecheckInGate(
    availability: SimBadgeSyncAvailability
): Boolean {
    return availability == SimBadgeSyncAvailability.READY ||
        availability == SimBadgeSyncAvailability.UNAVAILABLE
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
        SimBadgeSyncAvailability.READY -> {
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

internal fun shouldPromptWifiMismatchForManualSyncBlock(
    branch: SimBadgeManualSyncGateBranch
): Boolean {
    return branch == SimBadgeManualSyncGateBranch.MANAGER_OFFLINE_BLOCK
}

data class SimAudioDiscussion(
    val audioId: String,
    val title: String,
    val summary: String
)
