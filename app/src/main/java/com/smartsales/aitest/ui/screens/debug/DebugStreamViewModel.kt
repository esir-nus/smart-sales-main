// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/debug/DebugStreamViewModel.kt
// 模块：:app
// 说明：DashScope 流式调试页的状态管理，区分全量前缀与增量输出
// 作者：创建于 2025-12-11
package com.smartsales.aitest.ui.screens.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.data.aicore.DashscopeStreamEvent
import com.smartsales.data.aicore.debug.DashscopeDebugClient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DebugStreamMode(val incrementalOutput: Boolean, val label: String) {
    FullSequence(incrementalOutput = false, label = "全量前缀"),
    Incremental(incrementalOutput = true, label = "增量拼接")
}

data class DebugStreamUiState(
    val prompt: String = "",
    val mode: DebugStreamMode = DebugStreamMode.FullSequence,
    val rawLogs: List<String> = emptyList(),
    val aggregatedText: String = "",
    val isStreaming: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DebugStreamViewModel @Inject constructor(
    private val dashscopeDebugClient: DashscopeDebugClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugStreamUiState())
    val uiState: StateFlow<DebugStreamUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var chunkCounter = 0

    fun updatePrompt(value: String) {
        _uiState.update { it.copy(prompt = value) }
    }

    fun updateMode(mode: DebugStreamMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun startStreaming() {
        val currentPrompt = _uiState.value.prompt.trim()
        if (currentPrompt.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "请输入提示内容") }
            return
        }
        streamJob?.cancel()
        chunkCounter = 0
        val mode = _uiState.value.mode
        _uiState.update {
            it.copy(
                rawLogs = emptyList(),
                aggregatedText = "",
                isStreaming = true,
                errorMessage = null
            )
        }
        streamJob = viewModelScope.launch {
            val aggregated = StringBuilder()
            try {
                dashscopeDebugClient.streamRaw(
                    userPrompt = currentPrompt,
                    incrementalOutput = mode.incrementalOutput
                ).collect { event ->
                    when (event) {
                        is DashscopeStreamEvent.Chunk -> {
                            chunkCounter += 1
                            val logLine = """[Chunk #$chunkCounter] "${event.content}""""
                            // 按模式选择“替换前缀”或“追加增量”，模拟两种 chunk 行为
                            val text = if (mode == DebugStreamMode.FullSequence) {
                                aggregated.clear()
                                aggregated.append(event.content)
                                aggregated.toString()
                            } else {
                                aggregated.append(event.content)
                                aggregated.toString()
                            }
                            appendLog(logLine)
                            _uiState.update { state ->
                                state.copy(aggregatedText = text, errorMessage = null)
                            }
                        }
                        DashscopeStreamEvent.Completed -> {
                            appendLog("""[Completed] finalText="${aggregated}"""")
                            _uiState.update { state ->
                                state.copy(aggregatedText = aggregated.toString())
                            }
                        }
                        is DashscopeStreamEvent.Failed -> {
                            appendLog("[Error] ${event.reason}")
                            _uiState.update { state ->
                                state.copy(
                                    errorMessage = event.reason,
                                    isStreaming = false
                                )
                            }
                        }
                    }
                }
            } catch (cancel: CancellationException) {
                appendLog("[Cancelled]")
            } finally {
                _uiState.update { it.copy(isStreaming = false) }
            }
        }
    }

    private fun appendLog(line: String) {
        _uiState.update { state ->
            state.copy(rawLogs = state.rawLogs + line)
        }
    }
}
