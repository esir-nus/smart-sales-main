package com.smartsales.prism.data.audio

import android.content.Context
import android.net.Uri
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.core.util.Result
import com.smartsales.data.oss.OssUploadResult
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuJobState
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.domain.tingwu.TingwuRequest
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal const val ORPHANED_SIM_TRANSCRIPTION_MESSAGE =
    "检测到未完成的旧转写状态，请重新开始转写。"
internal const val DEBUG_FAILURE_SCENARIO_MESSAGE = "调试场景：模拟转写失败，请重试。"
internal const val SIM_AUDIO_METADATA_FILENAME = "sim_audio_metadata.json"

private const val DEBUG_FAILURE_AUDIO_ID = "sim_debug_failure"
private const val DEBUG_MISSING_SECTIONS_AUDIO_ID = "sim_debug_missing_sections"
private const val DEBUG_FALLBACK_AUDIO_ID = "sim_debug_fallback"
private const val DEBUG_SCENARIO_TIME_DISPLAY = "Debug Scenario"
private const val DEBUG_SCENARIO_ASSET_NAME = "sim_wave2_seed.mp3"
internal const val SIM_AUDIO_BADGE_SYNC_REQUESTED_SUMMARY = "SIM audio badge sync requested"
internal const val SIM_AUDIO_BADGE_SYNC_COMPLETED_SUMMARY = "SIM audio badge sync completed"
internal const val SIM_AUDIO_SYNC_FAILED_WHEN_CONNECTIVITY_UNAVAILABLE_SUMMARY =
    "SIM audio sync failed while connectivity unavailable"
internal const val SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE =
    "当前无法连接徽章，暂时不能同步录音。请检查设备连接后重试。"
private const val SIM_BADGE_SYNC_LIST_FAILURE_MESSAGE =
    "暂时无法获取徽章录音列表，请稍后重试。"
private const val SIM_BADGE_SYNC_DOWNLOAD_FAILURE_MESSAGE =
    "发现新的徽章录音，但暂时无法下载。请稍后重试。"
private const val SIM_AUDIO_OFFLINE_LOG_TAG = "SimAudioOffline"
private const val SIM_AUDIO_SYNC_LOG_TAG = "SimAudioSync"

internal enum class SimBadgeSyncTrigger {
    MANUAL,
    AUTO
}

internal enum class SimBadgeSyncSkippedReason {
    NOT_READY,
    ALREADY_RUNNING
}

internal data class SimBadgeSyncOutcome(
    val trigger: SimBadgeSyncTrigger,
    val importedCount: Int = 0,
    val skippedReason: SimBadgeSyncSkippedReason? = null
)

internal fun recoverOrphanedSimTranscriptions(entries: List<AudioFile>): List<AudioFile> {
    return entries.map { audio ->
        if (audio.status == TranscriptionStatus.TRANSCRIBING && audio.activeJobId.isNullOrBlank()) {
            audio.copy(
                status = TranscriptionStatus.PENDING,
                progress = 0f,
                lastErrorMessage = audio.lastErrorMessage ?: ORPHANED_SIM_TRANSCRIPTION_MESSAGE
            )
        } else {
            audio
        }
    }
}

internal fun isSimDebugFailureScenario(audioId: String): Boolean = audioId == DEBUG_FAILURE_AUDIO_ID

internal fun buildSimDebugMissingSectionsArtifacts(): TingwuJobArtifacts {
    return TingwuJobArtifacts(
        transcriptMarkdown = """
            客户问：这次试点如果预算先按季度走，最晚什么时候能启动？
            
            销售答：如果本周确认人选和范围，下周就能安排演示与报价复核。
            
            客户补充：先不要展开章节和会后动作，我们只看原始转写能否稳定展示。
        """.trimIndent(),
        smartSummary = TingwuSmartSummary(
            summary = "该调试样本只保留转写与摘要，用于验证缺失章节、重点、说话人等可选区块时，SIM 不会自行补齐不存在的内容。"
        )
    )
}

internal fun buildSimDebugFallbackArtifacts(): TingwuJobArtifacts {
    return TingwuJobArtifacts(
        transcriptMarkdown = """
            [Provider Raw Transcript]
            Speaker A: 先记录原话，不做额外润色。
            Speaker B: 可以，这里故意保留提供方风格和轻微噪声。
            Speaker A: 目标是验证当可读性润色层不可用时，SIM 仍然展示提供方结果，而不是空白卡片。
        """.trimIndent(),
        meetingAssistanceRaw = """
            {
              "MeetingAssistance": {
                "Keywords": ["原始输出", "回退路径", "不空白"],
                "KeySentences": [
                  { "Text": "这里展示的是提供方原始或轻格式化结果。" }
                ]
              }
            }
        """.trimIndent()
    )
}

internal fun upsertSimDebugScenarioEntry(
    current: List<AudioFile>,
    entry: AudioFile
): List<AudioFile> {
    return if (current.any { it.id == entry.id }) {
        current.map { existing -> if (existing.id == entry.id) entry else existing }
    } else {
        current + entry
    }
}

internal fun existingSimBadgeFilenames(entries: List<AudioFile>): Set<String> {
    return entries
        .filter { it.source == AudioSource.SMARTBADGE }
        .map { it.filename }
        .toSet()
}

internal fun selectNewSimBadgeFilenames(
    badgeFilenames: List<String>,
    existingBadgeFilenames: Set<String>
): List<String> {
    return badgeFilenames
        .distinct()
        .filter { it !in existingBadgeFilenames }
}

internal fun simBadgeSyncSuccessMessage(importedCount: Int): String {
    return if (importedCount > 0) {
        "已同步 $importedCount 条徽章录音"
    } else {
        "未发现新的徽章录音"
    }
}

internal fun emitSimAudioBadgeSyncRequestedTelemetry(
    trigger: SimBadgeSyncTrigger,
    log: (String) -> Unit = { message -> android.util.Log.d(SIM_AUDIO_SYNC_LOG_TAG, message) }
) {
    val detail = "trigger=${trigger.name.lowercase()}"
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_BADGE_SYNC_REQUESTED_SUMMARY,
        rawDataDump = detail
    )
    log("$SIM_AUDIO_BADGE_SYNC_REQUESTED_SUMMARY: $detail")
}

internal fun emitSimAudioBadgeSyncCompletedTelemetry(
    trigger: SimBadgeSyncTrigger,
    importedCount: Int,
    log: (String) -> Unit = { message -> android.util.Log.d(SIM_AUDIO_SYNC_LOG_TAG, message) }
) {
    val detail = "trigger=${trigger.name.lowercase()} importedCount=$importedCount"
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_BADGE_SYNC_COMPLETED_SUMMARY,
        rawDataDump = detail
    )
    log("$SIM_AUDIO_BADGE_SYNC_COMPLETED_SUMMARY: $detail")
}

internal fun emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
    detail: String,
    log: (String) -> Unit = { message -> android.util.Log.d(SIM_AUDIO_OFFLINE_LOG_TAG, message) }
) {
    PipelineValve.tag(
        checkpoint = PipelineValve.Checkpoint.UI_STATE_EMITTED,
        payloadSize = detail.length,
        summary = SIM_AUDIO_SYNC_FAILED_WHEN_CONNECTIVITY_UNAVAILABLE_SUMMARY,
        rawDataDump = detail
    )
    log("$SIM_AUDIO_SYNC_FAILED_WHEN_CONNECTIVITY_UNAVAILABLE_SUMMARY: $detail")
}

internal fun isLikelySimBadgeConnectivityUnavailable(rawReason: String?): Boolean {
    val normalized = rawReason
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: return true

    return listOf(
        "oss_unknown",
        "timeout",
        "timed out",
        "offline",
        "network",
        "socket",
        "connect",
        "connection",
        "unreachable",
        "refused",
        "no route",
        "unable to resolve",
        "unknown host",
        "dns",
        "null"
    ).any { normalized.contains(it) }
}

internal fun buildSimBadgeSyncListFailureMessage(rawReason: String?): String {
    return if (isLikelySimBadgeConnectivityUnavailable(rawReason)) {
        SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE
    } else {
        SIM_BADGE_SYNC_LIST_FAILURE_MESSAGE
    }
}

internal fun buildSimBadgeSyncDownloadFailureMessage(rawReason: String?): String {
    return if (isLikelySimBadgeConnectivityUnavailable(rawReason)) {
        SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE
    } else {
        SIM_BADGE_SYNC_DOWNLOAD_FAILURE_MESSAGE
    }
}

internal fun simStoredAudioFilename(audioId: String, extension: String): String {
    return "sim_${audioId}.$extension"
}

internal fun simArtifactFilename(audioId: String): String {
    return "sim_${audioId}_artifacts.json"
}

/**
 * SIM 专属音频仓库。
 * 使用独立命名空间，避免污染智能版音频持久化。
 */
@Singleton
class SimAudioRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectivityBridge: ConnectivityBridge,
    private val ossUploader: OssUploader,
    private val tingwuPipeline: TingwuPipeline
) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val metadataFile = File(context.filesDir, SIM_AUDIO_METADATA_FILENAME)
    private val fileMutex = Mutex()
    private val syncMutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val _audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    private val observationJobs = mutableMapOf<String, Job>()
    private val seedDefinitions = listOf(
        SimSeedDefinition(
            id = "sim_wave2_seed",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Seed.mp3",
            isStarred = true
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_a",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_A.mp3"
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_b",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_B.mp3"
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_c",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_C.mp3"
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_d",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_D.mp3"
        ),
        SimSeedDefinition(
            id = "sim_wave2_seed_pending_e",
            assetName = "sim_wave2_seed.mp3",
            filename = "SIM_Wave2_Pending_E.mp3"
        )
    )

    init {
        loadFromDisk()
        backfillSeedInventory()
        resumeTrackedJobs()
    }

    fun getAudioFiles(): Flow<List<AudioFile>> = _audioFiles.asStateFlow()

    internal suspend fun canSyncFromBadge(): Boolean = withContext(ioDispatcher) {
        connectivityBridge.isReady()
    }

    internal suspend fun syncFromBadge(trigger: SimBadgeSyncTrigger): SimBadgeSyncOutcome = withContext(ioDispatcher) {
        val isReady = canSyncFromBadge()
        if (trigger == SimBadgeSyncTrigger.AUTO && !isReady) {
            return@withContext SimBadgeSyncOutcome(
                trigger = trigger,
                skippedReason = SimBadgeSyncSkippedReason.NOT_READY
            )
        }

        if (trigger == SimBadgeSyncTrigger.MANUAL && !isReady) {
            emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                detail = "manual preflight failed: connectivity not ready"
            )
            throw Exception(SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE)
        }

        if (!syncMutex.tryLock()) {
            return@withContext SimBadgeSyncOutcome(
                trigger = trigger,
                skippedReason = SimBadgeSyncSkippedReason.ALREADY_RUNNING
            )
        }

        try {
            emitSimAudioBadgeSyncRequestedTelemetry(trigger)
            val importedCount = performBadgeSyncLocked()
            emitSimAudioBadgeSyncCompletedTelemetry(trigger, importedCount)
            SimBadgeSyncOutcome(trigger = trigger, importedCount = importedCount)
        } finally {
            syncMutex.unlock()
        }
    }

    suspend fun syncFromDevice() = withContext(ioDispatcher) {
        syncMutex.withLock {
            performBadgeSyncLocked()
        }
    }

    suspend fun addLocalAudio(uriString: String) = withContext(ioDispatcher) {
        val uri = Uri.parse(uriString)
        val newId = UUID.randomUUID().toString()
        val extension = guessExtensionFromUri(uriString)
        val destFile = storedAudioFile(newId, extension)

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("无法打开本地音频流")
        } catch (e: Exception) {
            throw Exception("无法读取本地音频文件", e)
        }

        val newFile = AudioFile(
            id = newId,
            filename = "SIM_Local_${System.currentTimeMillis()}.$extension",
            timeDisplay = "Just now",
            source = AudioSource.PHONE,
            status = TranscriptionStatus.PENDING,
            isStarred = false,
            isTestImport = true
        )
        mutateAndSave { current -> current + newFile }
    }

    suspend fun seedDebugFailureScenario() = withContext(ioDispatcher) {
        ensureDebugAudioFileExists(DEBUG_FAILURE_AUDIO_ID)
        artifactFile(DEBUG_FAILURE_AUDIO_ID).delete()
        mutateAndSave { current ->
            upsertSimDebugScenarioEntry(
                current = current,
                entry = AudioFile(
                    id = DEBUG_FAILURE_AUDIO_ID,
                    filename = "SIM_Debug_Failure.mp3",
                    timeDisplay = DEBUG_SCENARIO_TIME_DISPLAY,
                    source = AudioSource.PHONE,
                    status = TranscriptionStatus.PENDING,
                    summary = "调试场景：用于验证转写失败是否显式可见且可重试。"
                )
            )
        }
    }

    suspend fun seedDebugMissingSectionsScenario() = withContext(ioDispatcher) {
        ensureDebugAudioFileExists(DEBUG_MISSING_SECTIONS_AUDIO_ID)
        val artifacts = buildSimDebugMissingSectionsArtifacts()
        artifactFile(DEBUG_MISSING_SECTIONS_AUDIO_ID).writeText(json.encodeToString(artifacts))
        mutateAndSave { current ->
            upsertSimDebugScenarioEntry(
                current = current,
                entry = AudioFile(
                    id = DEBUG_MISSING_SECTIONS_AUDIO_ID,
                    filename = "SIM_Debug_Missing_Sections.mp3",
                    timeDisplay = DEBUG_SCENARIO_TIME_DISPLAY,
                    source = AudioSource.PHONE,
                    status = TranscriptionStatus.TRANSCRIBED,
                    summary = artifacts.smartSummary?.summary,
                    progress = 1f
                )
            )
        }
    }

    suspend fun seedDebugFallbackScenario() = withContext(ioDispatcher) {
        ensureDebugAudioFileExists(DEBUG_FALLBACK_AUDIO_ID)
        val artifacts = buildSimDebugFallbackArtifacts()
        artifactFile(DEBUG_FALLBACK_AUDIO_ID).writeText(json.encodeToString(artifacts))
        mutateAndSave { current ->
            upsertSimDebugScenarioEntry(
                current = current,
                entry = AudioFile(
                    id = DEBUG_FALLBACK_AUDIO_ID,
                    filename = "SIM_Debug_Fallback.mp3",
                    timeDisplay = DEBUG_SCENARIO_TIME_DISPLAY,
                    source = AudioSource.PHONE,
                    status = TranscriptionStatus.TRANSCRIBED,
                    summary = artifacts.transcriptMarkdown?.take(80),
                    progress = 1f
                )
            )
        }
    }

    suspend fun startTranscription(audioId: String) {
        val audio = getAudio(audioId) ?: throw Exception("找不到音频条目")
        if (audio.status == TranscriptionStatus.TRANSCRIBED && getArtifacts(audioId) != null) {
            android.util.Log.d("SimAudioRepository", "skip rerun for already-transcribed audioId=$audioId")
            return
        }

        audio.activeJobId?.takeIf { audio.status == TranscriptionStatus.TRANSCRIBING }?.let { jobId ->
            observeTranscription(audioId, jobId)
            return
        }

        mutateAndSave { current ->
            current.map {
                if (it.id == audioId) {
                    it.copy(
                        status = TranscriptionStatus.TRANSCRIBING,
                        progress = 0.05f,
                        lastErrorMessage = null
                    )
                } else {
                    it
                }
            }
        }

        try {
            val fileToTranscribe = resolveStoredAudioFile(audioId)
                ?: throw Exception("找不到 SIM 本地音频实体")

            if (isSimDebugFailureScenario(audioId)) {
                throw Exception(DEBUG_FAILURE_SCENARIO_MESSAGE)
            }

            val objectKey = "smartsales/sim/audio/${System.currentTimeMillis()}/${fileToTranscribe.name}"
            val uploadResult = ossUploader.upload(fileToTranscribe, objectKey)
            val publicUrl = when (uploadResult) {
                is OssUploadResult.Success -> uploadResult.publicUrl
                is OssUploadResult.Error -> throw Exception("[OSS_${uploadResult.code}] ${uploadResult.message}")
            }

            val request = TingwuRequest(
                ossObjectKey = objectKey,
                fileUrl = publicUrl,
                audioAssetName = fileToTranscribe.name,
                language = "zh-CN",
                audioFilePath = fileToTranscribe
            )

            val submitResult = tingwuPipeline.submit(request)
            val jobId = when (submitResult) {
                is Result.Success -> submitResult.data
                is Result.Error -> throw Exception("Tingwu submission failed", submitResult.throwable)
            }
            mutateAndSave { current ->
                current.map {
                    if (it.id == audioId) {
                        it.copy(activeJobId = jobId, lastErrorMessage = null)
                    } else {
                        it
                    }
                }
            }
            observeTranscription(audioId, jobId)
        } catch (e: Exception) {
            mutateAndSave { current ->
                current.map {
                    if (it.id == audioId) {
                        it.copy(
                            status = TranscriptionStatus.PENDING,
                            progress = 0f,
                            activeJobId = null,
                            lastErrorMessage = e.message ?: "转写失败，请稍后重试。"
                        )
                    } else {
                        it
                    }
                }
            }
            throw e
        }
    }

    fun deleteAudio(audioId: String) {
        val target = _audioFiles.value.find { it.id == audioId } ?: return
        observationJobs.remove(audioId)?.cancel()
        _audioFiles.update { current -> current.filterNot { it.id == audioId } }
        saveToDiskAsync()

        resolveStoredAudioFile(audioId)?.delete()
        artifactFile(audioId).delete()

        if (target.source == AudioSource.SMARTBADGE) {
            repositoryScope.launch {
                runCatching { connectivityBridge.deleteRecording(target.filename) }
            }
        }
    }

    fun toggleStar(audioId: String) {
        _audioFiles.update { current ->
            current.map { if (it.id == audioId) it.copy(isStarred = !it.isStarred) else it }
        }
        saveToDiskAsync()
    }

    fun getAudio(audioId: String): AudioFile? = _audioFiles.value.find { it.id == audioId }

    suspend fun getArtifacts(audioId: String): TingwuJobArtifacts? = withContext(ioDispatcher) {
        val file = artifactFile(audioId)
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString<TingwuJobArtifacts>(file.readText()) }
            .getOrElse {
                android.util.Log.e("SimAudioRepository", "load artifacts failed for $audioId", it)
                null
            }
    }

    fun bindSession(audioId: String, sessionId: String) {
        _audioFiles.update { current ->
            current.map { if (it.id == audioId) it.copy(boundSessionId = sessionId) else it }
        }
        saveToDiskAsync()
    }

    fun clearBoundSession(audioId: String) {
        _audioFiles.update { current ->
            current.map { if (it.id == audioId) it.copy(boundSessionId = null) else it }
        }
        saveToDiskAsync()
    }

    fun getBoundSessionId(audioId: String): String? = getAudio(audioId)?.boundSessionId

    fun getAudioFilesSnapshot(): List<AudioFile> = _audioFiles.value

    private fun backfillSeedInventory() {
        runCatching {
            val existingIds = _audioFiles.value.map { it.id }.toSet()
            val updatedFiles = _audioFiles.value.toMutableList()

            seedDefinitions.forEach { seed ->
                val seedFile = storedAudioFile(seed.id, "mp3")
                if (!seedFile.exists()) {
                    context.assets.open(seed.assetName).use { input ->
                        seedFile.outputStream().use { output -> input.copyTo(output) }
                    }
                }

                if (seed.id !in existingIds) {
                    updatedFiles += AudioFile(
                        id = seed.id,
                        filename = seed.filename,
                        timeDisplay = "内置样本",
                        source = AudioSource.PHONE,
                        status = TranscriptionStatus.PENDING,
                        isStarred = seed.isStarred
                    )
                }
            }

            if (updatedFiles != _audioFiles.value) {
                _audioFiles.value = updatedFiles
                metadataFile.writeText(json.encodeToString(updatedFiles))
            } else if (!metadataFile.exists()) {
                metadataFile.writeText(json.encodeToString(_audioFiles.value))
            }
        }.onFailure {
            android.util.Log.e("SimAudioRepository", "seed inventory failed", it)
        }
    }

    private fun resumeTrackedJobs() {
        _audioFiles.value
            .filter { it.status == TranscriptionStatus.TRANSCRIBING && !it.activeJobId.isNullOrBlank() }
            .forEach { audio ->
                observeTranscription(audio.id, audio.activeJobId!!)
            }
    }

    private fun observeTranscription(audioId: String, jobId: String) {
        val existingJob = observationJobs[audioId]
        if (existingJob?.isActive == true) return

        observationJobs[audioId] = repositoryScope.launch {
            tingwuPipeline.observeJob(jobId).collectLatest { state ->
                when (state) {
                    TingwuJobState.Idle -> Unit
                    is TingwuJobState.InProgress -> {
                        mutateAndSave { current ->
                            current.map {
                                if (it.id == audioId) {
                                    it.copy(
                                        status = TranscriptionStatus.TRANSCRIBING,
                                        progress = state.progressPercent / 100f,
                                        activeJobId = jobId,
                                        lastErrorMessage = null
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                    }

                    is TingwuJobState.Completed -> {
                        val artifacts = state.artifacts ?: TingwuJobArtifacts(
                            transcriptMarkdown = state.transcriptMarkdown
                        )
                        artifactFile(audioId).writeText(json.encodeToString(artifacts))
                        val summary = artifacts.smartSummary?.summary ?: artifacts.transcriptMarkdown?.take(120)

                        mutateAndSave { current ->
                            current.map {
                                if (it.id == audioId) {
                                    it.copy(
                                        status = TranscriptionStatus.TRANSCRIBED,
                                        summary = summary,
                                        progress = 1.0f,
                                        activeJobId = null,
                                        lastErrorMessage = null
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                        observationJobs.remove(audioId)
                    }

                    is TingwuJobState.Failed -> {
                        mutateAndSave { current ->
                            current.map {
                                if (it.id == audioId) {
                                    it.copy(
                                        status = TranscriptionStatus.PENDING,
                                        progress = 0f,
                                        activeJobId = null,
                                        lastErrorMessage = state.reason
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                        observationJobs.remove(audioId)
                    }
                }
            }
        }
    }

    private data class SimSeedDefinition(
        val id: String,
        val assetName: String,
        val filename: String,
        val isStarred: Boolean = false
    )

    private suspend fun performBadgeSyncLocked(): Int {
        val listResult = connectivityBridge.listRecordings()
        val badgeFiles = when (listResult) {
            is Result.Success -> listResult.data
            is Result.Error -> {
                val reason = listResult.throwable.message ?: "unknown"
                emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                    detail = "listRecordings failed: $reason"
                )
                throw Exception(buildSimBadgeSyncListFailureMessage(reason))
            }
        }

        val newFilesToDownload = selectNewSimBadgeFilenames(
            badgeFilenames = badgeFiles,
            existingBadgeFilenames = existingSimBadgeFilenames(_audioFiles.value)
        )

        var importedCount = 0
        var failedDownloadCount = 0
        var firstDownloadFailureReason: String? = null
        for (filename in newFilesToDownload) {
            when (val downloadResult = connectivityBridge.downloadRecording(filename)) {
                is WavDownloadResult.Success -> {
                    val newId = UUID.randomUUID().toString()
                    val destFile = storedAudioFile(newId, "wav")
                    downloadResult.localFile.copyTo(destFile, overwrite = true)
                    downloadResult.localFile.delete()

                    val newFile = AudioFile(
                        id = newId,
                        filename = filename,
                        timeDisplay = "Just now",
                        source = AudioSource.SMARTBADGE,
                        status = TranscriptionStatus.PENDING,
                        isStarred = false
                    )
                    mutateAndSave { current -> current + newFile }
                    importedCount += 1
                }

                is WavDownloadResult.Error -> {
                    failedDownloadCount += 1
                    if (firstDownloadFailureReason == null) {
                        firstDownloadFailureReason = downloadResult.message
                    }
                    emitSimAudioSyncFailureWhileConnectivityUnavailableTelemetry(
                        detail = "downloadRecording failed filename=$filename reason=${downloadResult.message}"
                    )
                    android.util.Log.e("SimAudioRepository", "syncFromDevice failed: ${downloadResult.message}")
                }
            }
        }

        if (newFilesToDownload.isNotEmpty() && importedCount == 0 && failedDownloadCount > 0) {
            throw Exception(buildSimBadgeSyncDownloadFailureMessage(firstDownloadFailureReason))
        }

        return importedCount
    }

    private suspend fun mutateAndSave(mutate: (List<AudioFile>) -> List<AudioFile>) {
        fileMutex.withLock {
            val newList = mutate(_audioFiles.value)
            _audioFiles.value = newList
            withContext(ioDispatcher) {
                metadataFile.writeText(json.encodeToString(newList))
            }
        }
    }

    private fun saveToDiskAsync() {
        repositoryScope.launch {
            runCatching {
                fileMutex.withLock {
                    metadataFile.parentFile?.mkdirs()
                    metadataFile.writeText(json.encodeToString(_audioFiles.value))
                }
            }.onFailure {
                android.util.Log.e("SimAudioRepository", "save metadata failed", it)
            }
        }
    }

    private fun loadFromDisk() {
        if (!metadataFile.exists()) return
        runCatching {
            val contents = metadataFile.readText()
            if (contents.isNotBlank()) {
                _audioFiles.value = recoverOrphanedSimTranscriptions(
                    json.decodeFromString(contents)
                )
            }
        }.onFailure {
            android.util.Log.e("SimAudioRepository", "load metadata failed", it)
        }
    }

    private fun storedAudioFile(audioId: String, extension: String): File {
        return File(context.filesDir, simStoredAudioFilename(audioId, extension))
    }

    private fun ensureDebugAudioFileExists(audioId: String, extension: String = "mp3") {
        val file = storedAudioFile(audioId, extension)
        if (file.exists()) return
        context.assets.open(DEBUG_SCENARIO_ASSET_NAME).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private fun artifactFile(audioId: String): File {
        return File(context.filesDir, simArtifactFilename(audioId))
    }

    private fun resolveStoredAudioFile(audioId: String): File? {
        val candidates = listOf("wav", "mp3", "m4a", "aac", "ogg")
            .map { storedAudioFile(audioId, it) }
        return candidates.firstOrNull { it.exists() }
    }

    private fun guessExtensionFromUri(uriString: String): String {
        val lower = uriString.lowercase()
        return when {
            lower.endsWith(".mp3") -> "mp3"
            lower.endsWith(".m4a") -> "m4a"
            lower.endsWith(".aac") -> "aac"
            lower.endsWith(".ogg") -> "ogg"
            else -> "wav"
        }
    }
}
