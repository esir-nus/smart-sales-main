package com.smartsales.prism.data.audio

import android.content.Context
import com.smartsales.prism.domain.audio.AudioFile
import com.smartsales.prism.domain.audio.AudioSource
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.tingwu.TingwuJobArtifacts
import com.smartsales.prism.domain.tingwu.TingwuSmartSummary
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal const val DEBUG_FAILURE_SCENARIO_MESSAGE = "调试场景：模拟转写失败，请重试。"

private const val DEBUG_FAILURE_AUDIO_ID = "sim_debug_failure"
private const val DEBUG_MISSING_SECTIONS_AUDIO_ID = "sim_debug_missing_sections"
private const val DEBUG_FALLBACK_AUDIO_ID = "sim_debug_fallback"
private const val DEBUG_SCENARIO_TIME_DISPLAY = "Debug Scenario"
private const val DEBUG_SCENARIO_ASSET_NAME = "sim_wave2_seed.mp3"

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

internal fun simArtifactFilename(audioId: String): String {
    return "sim_${audioId}_artifacts.json"
}

internal fun simArtifactFile(context: Context, audioId: String): File {
    return File(context.filesDir, simArtifactFilename(audioId))
}

internal fun summarizeSimArtifacts(artifacts: TingwuJobArtifacts): String? {
    return artifacts.smartSummary?.summary ?: artifacts.transcriptMarkdown?.take(120)
}

internal class SimAudioRepositoryArtifactSupport(
    private val runtime: SimAudioRepositoryRuntime,
    private val storeSupport: SimAudioRepositoryStoreSupport
) {

    suspend fun getArtifacts(audioId: String): TingwuJobArtifacts? = withContext(runtime.ioDispatcher) {
        val file = simArtifactFile(runtime.context, audioId)
        if (!file.exists()) return@withContext null
        runCatching { runtime.json.decodeFromString<TingwuJobArtifacts>(file.readText()) }
            .getOrElse {
                android.util.Log.e("SimAudioRepository", "load artifacts failed for $audioId", it)
                null
            }
    }

    suspend fun seedDebugFailureScenario() = withContext(runtime.ioDispatcher) {
        ensureDebugAudioFileExists(DEBUG_FAILURE_AUDIO_ID)
        simArtifactFile(runtime.context, DEBUG_FAILURE_AUDIO_ID).delete()
        storeSupport.mutateAndSave { current ->
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

    suspend fun seedDebugMissingSectionsScenario() = withContext(runtime.ioDispatcher) {
        ensureDebugAudioFileExists(DEBUG_MISSING_SECTIONS_AUDIO_ID)
        val artifacts = buildSimDebugMissingSectionsArtifacts()
        simArtifactFile(runtime.context, DEBUG_MISSING_SECTIONS_AUDIO_ID)
            .writeText(runtime.json.encodeToString(artifacts))
        storeSupport.mutateAndSave { current ->
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

    suspend fun seedDebugFallbackScenario() = withContext(runtime.ioDispatcher) {
        ensureDebugAudioFileExists(DEBUG_FALLBACK_AUDIO_ID)
        val artifacts = buildSimDebugFallbackArtifacts()
        simArtifactFile(runtime.context, DEBUG_FALLBACK_AUDIO_ID)
            .writeText(runtime.json.encodeToString(artifacts))
        storeSupport.mutateAndSave { current ->
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

    suspend fun writeArtifacts(audioId: String, artifacts: TingwuJobArtifacts) {
        withContext(runtime.ioDispatcher) {
            simArtifactFile(runtime.context, audioId)
                .writeText(runtime.json.encodeToString(artifacts))
        }
    }

    private fun ensureDebugAudioFileExists(audioId: String, extension: String = "mp3") {
        val file = simStoredAudioFile(runtime.context, audioId, extension)
        if (file.exists()) return
        runtime.context.assets.open(DEBUG_SCENARIO_ASSET_NAME).use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
