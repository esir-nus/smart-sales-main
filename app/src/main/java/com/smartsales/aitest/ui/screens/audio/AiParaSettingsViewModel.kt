// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/audio/AiParaSettingsViewModel.kt
// 模块：:app
// 说明：测试壳使用的 AiParaSettings 运行时开关 ViewModel（内存态）
// 作者：创建于 2025-12-16
package com.smartsales.aitest.ui.screens.audio

import androidx.lifecycle.ViewModel
import com.smartsales.aitest.audio.TranscriptionProvider
import com.smartsales.data.aicore.params.AiParaSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AiParaSettingsViewModel @Inject constructor(
    private val repository: AiParaSettingsRepository,
) : ViewModel() {

    val settings: StateFlow<com.smartsales.data.aicore.params.AiParaSettingsSnapshot> = repository.flow

    fun setTranscriptionProvider(provider: TranscriptionProvider) {
        repository.update { it.copy(transcriptionProvider = provider.name) }
    }

    fun setXfyunEngSmoothproc(enabled: Boolean) {
        repository.update { it.copy(xfyunEngSmoothproc = enabled) }
    }

    fun setXfyunRoleType(value: Int) {
        repository.update { it.copy(xfyunRoleType = value) }
    }

    fun setXfyunRoleNum(value: Int) {
        repository.update { it.copy(xfyunRoleNum = value) }
    }
}

