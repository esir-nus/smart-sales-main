package com.smartsales.aitest.ui.screens.debug

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/debug/TingwuPlaygroundViewModel.kt
// 模块：:app
// 说明：Tingwu 调试页的状态与提交逻辑，仅用于开发环境快速验证 diarization
// 作者：创建于 2025-12-12

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.TingwuCoordinator
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.data.aicore.DiarizedSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TingwuPlaygroundUiState(
    val fileUrl: String = "",
    val diarizationEnabled: Boolean = true,
    val isSubmitting: Boolean = false,
    val statusLabel: String = "待提交",
    val artifacts: TingwuJobArtifacts? = null,
    val transcriptMarkdown: String? = null,
    val diarizedSegments: List<DiarizedSegment> = emptyList(),
    val speakerLabels: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

@HiltViewModel
class TingwuPlaygroundViewModel @Inject constructor(
    private val tingwuCoordinator: TingwuCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(TingwuPlaygroundUiState())
    val uiState: StateFlow<TingwuPlaygroundUiState> = _uiState.asStateFlow()
    private var currentJobId: String? = null

    fun updateFileUrl(url: String) {
        _uiState.value = _uiState.value.copy(fileUrl = url)
    }

    fun updateDiarizationEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(diarizationEnabled = enabled)
    }

    fun submit() {
        val url = _uiState.value.fileUrl.trim()
        if (url.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请填写 FileUrl")
            return
        }
        _uiState.value = _uiState.value.copy(isSubmitting = true, errorMessage = null, statusLabel = "提交中…")
        viewModelScope.launch {
            val request = TingwuRequest(
                audioAssetName = "playground-file",
                language = "zh-CN",
                fileUrl = url,
                diarizationEnabled = _uiState.value.diarizationEnabled
            )
            when (val result = tingwuCoordinator.submit(request)) {
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = result.throwable.message ?: "提交失败",
                        statusLabel = "提交失败"
                    )
                }
                is Result.Success -> {
                    currentJobId = result.data
                    observeJob(result.data)
                }
            }
        }
    }

    private fun observeJob(jobId: String) {
        viewModelScope.launch {
            tingwuCoordinator.observeJob(jobId).collect { state ->
                when (state) {
                    is TingwuJobState.Idle -> {
                        _uiState.value = _uiState.value.copy(statusLabel = "排队中…")
                    }
                    is TingwuJobState.InProgress -> {
                        val label = state.statusLabel ?: "处理中 ${state.progressPercent}%"
                        _uiState.value = _uiState.value.copy(statusLabel = label, isSubmitting = true)
                    }
                    is TingwuJobState.Completed -> {
                        val artifacts = state.artifacts
                        val segments = artifacts?.diarizedSegments.orEmpty()
                        val labels = artifacts?.speakerLabels.orEmpty()
                        _uiState.value = _uiState.value.copy(
                            statusLabel = state.statusLabel ?: "转写完成",
                            isSubmitting = false,
                            artifacts = artifacts,
                            transcriptMarkdown = state.transcriptMarkdown,
                            diarizedSegments = segments,
                            speakerLabels = labels
                        )
                    }
                    is TingwuJobState.Failed -> {
                        val msg = state.reason.ifBlank { "转写失败" }
                        _uiState.value = _uiState.value.copy(
                            statusLabel = "失败",
                            isSubmitting = false,
                            errorMessage = msg
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
