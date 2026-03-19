package com.smartsales.prism.ui.sim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.ui.drawers.AudioItemState
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SimAudioEntry(
    val item: AudioItemState,
    val preview: String
)

/**
 * SIM 音频抽屉数据源。
 * Wave 1 只提供本地壳层样本，避免接入共享存储与会话绑定。
 */
class SimAudioDrawerViewModel : ViewModel() {

    private val _entries = MutableStateFlow(
        listOf(
            SimAudioEntry(
                item = AudioItemState(
                    id = "sim_audio_transcribed",
                    filename = "SIM Wave 1 样本录音",
                    timeDisplay = "08:12",
                    source = AudioSource.SMARTBADGE,
                    status = AudioStatus.TRANSCRIBED,
                    summary = "用于验证抽屉到讨论会话的路由。",
                    isStarred = true
                ),
                preview = "这是一个 Wave 1 壳层样本，用于验证 Audio Drawer -> Ask AI -> Discussion Chat 的独立路由。"
            ),
            SimAudioEntry(
                item = AudioItemState(
                    id = "sim_audio_pending",
                    filename = "待转写样本",
                    timeDisplay = "03:40",
                    source = AudioSource.PHONE,
                    status = AudioStatus.PENDING
                ),
                preview = "该条目用于验证 Pending -> Transcribing -> Transcribed 的占位状态切换。"
            )
        )
    )
    val entries: StateFlow<List<SimAudioEntry>> = _entries.asStateFlow()

    private val _uiEvents = MutableSharedFlow<String>()
    val uiEvents: SharedFlow<String> = _uiEvents.asSharedFlow()

    fun toggleStar(audioId: String) {
        _entries.value = _entries.value.map { entry ->
            if (entry.item.id == audioId) {
                entry.copy(item = entry.item.copy(isStarred = !entry.item.isStarred))
            } else {
                entry
            }
        }
    }

    fun startTranscription(audioId: String) {
        val target = _entries.value.firstOrNull { it.item.id == audioId } ?: return
        if (target.item.status != AudioStatus.PENDING) return

        _entries.value = _entries.value.map { entry ->
            if (entry.item.id == audioId) {
                entry.copy(item = entry.item.copy(status = AudioStatus.TRANSCRIBING, progress = 0.2f))
            } else {
                entry
            }
        }

        viewModelScope.launch {
            _uiEvents.emit("SIM 正在演示转写状态流转")
            delay(500)
            _entries.value = _entries.value.map { entry ->
                if (entry.item.id == audioId) {
                    entry.copy(item = entry.item.copy(status = AudioStatus.TRANSCRIBING, progress = 0.7f))
                } else {
                    entry
                }
            }
            delay(500)
            _entries.value = _entries.value.map { entry ->
                if (entry.item.id == audioId) {
                    entry.copy(
                        item = entry.item.copy(
                            status = AudioStatus.TRANSCRIBED,
                            progress = null,
                            summary = "Wave 1 已完成壳层状态演示，完整 Tingwu 流程将在后续波次接入。"
                        ),
                        preview = "该音频已被提升为可讨论状态。当前仍是 SIM Wave 1 本地样本。"
                    )
                } else {
                    entry
                }
            }
        }
    }

    fun createDiscussion(audioId: String): SimAudioDiscussion? {
        val entry = _entries.value.firstOrNull { it.item.id == audioId } ?: return null
        if (entry.item.status != AudioStatus.TRANSCRIBED) {
            viewModelScope.launch { _uiEvents.emit("仅已转写条目可进入讨论") }
            return null
        }
        return SimAudioDiscussion(
            audioId = audioId,
            title = entry.item.filename,
            summary = entry.item.summary ?: entry.preview
        )
    }
}

data class SimAudioDiscussion(
    val audioId: String,
    val title: String,
    val summary: String
)
