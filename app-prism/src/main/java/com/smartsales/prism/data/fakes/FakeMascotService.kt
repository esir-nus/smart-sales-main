package com.smartsales.prism.data.fakes

import android.util.Log
import com.smartsales.prism.domain.mascot.MascotInteraction
import com.smartsales.prism.domain.mascot.MascotResponse
import com.smartsales.prism.domain.mascot.MascotService
import com.smartsales.prism.domain.mascot.MascotState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class FakeMascotService @Inject constructor() : MascotService {

    private val TAG = "FakeMascotService"
    private val _state = MutableStateFlow<MascotState>(MascotState.Hidden)
    override val state: StateFlow<MascotState> = _state.asStateFlow()

    override fun startObserving() {
        Log.d(TAG, "Mascot started observing system EventBus.")
    }

    override suspend fun interact(input: MascotInteraction): MascotResponse {
        Log.d(TAG, "Mascot received interaction: \$input")
        return when (input) {
            is MascotInteraction.Text -> {
                val content = input.content.lowercase()
                when {
                    content.contains("hello") || content.contains("hi") || content.contains("你好") -> {
                        _state.value = MascotState.Active("你好呀！有什么我可以帮你的吗？", "happy")
                        MascotResponse.Speak("你好呀！", "happy")
                    }
                    else -> {
                        _state.value = MascotState.Active("听不懂哦，这是一些噪音吗？", "confused")
                        MascotResponse.Ignore
                    }
                }
            }
            is MascotInteraction.Tap -> {
                _state.value = MascotState.Active("戳我干嘛？", "surprised")
                MascotResponse.Speak("戳我干嘛？", "surprised")
            }
        }
    }
}
