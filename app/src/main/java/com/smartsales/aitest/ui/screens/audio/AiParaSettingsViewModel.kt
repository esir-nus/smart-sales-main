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
        repository.update { current ->
            val shouldEnableXfyun = provider == TranscriptionProvider.XFYUN
            current.copy(
                transcription = current.transcription.copy(provider = provider.name)
                    .let { transcription ->
                        // 重要：用户显式选择 XFyun 时才打开开关，避免默认链路被切走。
                        if (shouldEnableXfyun) {
                            transcription.copy(xfyunEnabled = true)
                        } else {
                            transcription
                        }
                    }
            )
        }
    }

    fun setXfyunEngSmoothproc(enabled: Boolean) {
        repository.update { current ->
            val transcription = current.transcription
            val xfyun = transcription.xfyun
            current.copy(
                transcription = transcription.copy(
                    xfyun = xfyun.copy(
                        upload = xfyun.upload.copy(engSmoothProc = enabled)
                    )
                )
            )
        }
    }

    fun setXfyunRoleType(value: Int) {
        repository.update { current ->
            val transcription = current.transcription
            val xfyun = transcription.xfyun
            current.copy(
                transcription = transcription.copy(
                    xfyun = xfyun.copy(
                        upload = xfyun.upload.copy(roleType = value)
                    )
                )
            )
        }
    }

    fun setXfyunRoleNum(value: Int) {
        repository.update { current ->
            val transcription = current.transcription
            val xfyun = transcription.xfyun
            current.copy(
                transcription = transcription.copy(
                    xfyun = xfyun.copy(
                        upload = xfyun.upload.copy(roleNum = value)
                    )
                )
            )
        }
    }
}
