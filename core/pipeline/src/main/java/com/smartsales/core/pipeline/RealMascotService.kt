package com.smartsales.core.pipeline

import android.util.Log
import com.smartsales.core.llm.ModelRegistry

import com.smartsales.prism.domain.model.Mode
import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.context.ModeMetadata
import com.smartsales.prism.domain.repository.UserProfileRepository
import com.smartsales.prism.domain.system.SystemEvent
import com.smartsales.prism.domain.system.SystemEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealMascotService @Inject constructor(
    private val eventBus: SystemEventBus,
    private val executor: Executor,
    private val userProfileRepository: UserProfileRepository,
    private val promptCompiler: PromptCompiler
) : MascotService {

    private val TAG = "RealMascotService"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val _state = MutableStateFlow<MascotState>(MascotState.Hidden)
    override val state: StateFlow<MascotState> = _state.asStateFlow()

    // Latch to prevent spamming the LLM
    private var hasPromptedForIdle = false
    private var isObserving = false

    override fun startObserving() {
        if (isObserving) return
        isObserving = true
        Log.d(TAG, "Mascot started observing system EventBus.")
        
        scope.launch {
            eventBus.events.collect { event ->
                Log.d(TAG, "Mascot caught event: ${event.javaClass.simpleName}")
                when (event) {
                    is SystemEvent.AppIdle -> {
                        if (!hasPromptedForIdle) {
                            hasPromptedForIdle = true
                            scope.launch { generateProactivePrompt() }
                        }
                    }
                }
            }
        }
    }

    override suspend fun interact(input: MascotInteraction): MascotResponse {
        Log.d(TAG, "Received interaction: $input")
        
        // Any interaction resets the idle latch so next time the user goes idle, Mascot can prompt again.
        hasPromptedForIdle = false
        
        return when (input) {
            is MascotInteraction.Tap -> {
                _state.value = MascotState.Hidden
                MascotResponse.Ignore
            }
            is MascotInteraction.Text -> {
                // Future UI integration for back-and-forth chatter
                MascotResponse.Speak("收到：${input.content}")
            }
        }
    }

    private suspend fun generateProactivePrompt() {
        Log.d(TAG, "Generating proactive prompt using EXTRACTOR...")
        
        val userName = userProfileRepository.profile.value.displayName
        val hour = java.time.LocalTime.now().hour
        val timeGreeting = when {
            hour < 5 -> "夜深了"
            hour < 12 -> "早上好"
            hour < 18 -> "下午好"
            else -> "晚上好"
        }
        
        val systemPrompt = """
            你是一个友好、简短、活泼的系统吉祥物助手。
            用户当前状态为空闲（没有在操作应用）。
            请生成一句简短的寒暄或提示，例如："工作告一段落了？要不要看看今天的复盘？"、"有点安静哦，需要帮忙吗？"、"喝口水休息一下吧！"。
            语气应该轻松愉快，不要像严肃的AI助理。不要总是问问题。
            最多一行，不要超过20个字。不要提供实际的业务分析。
            当前用户名字是：$userName
            当前时间线是：$timeGreeting
        """.trimIndent()

        val dummyContext = EnhancedContext(
            userText = "生成一句闲置状态下的关怀语。",
            modeMetadata = ModeMetadata(Mode.ANALYST, "mascot-ephemeral-session", 0),
            sessionHistory = emptyList(),
            systemPromptOverride = systemPrompt
        )

        try {
            val prompt = promptCompiler.compile(dummyContext)
            val result = executor.execute(ModelRegistry.EXTRACTOR, prompt)
            if (result is ExecutorResult.Success) {
                val message = result.content
                Log.d(TAG, "Generated prompt: $message")
                _state.value = MascotState.Active(message, "happy")
                
                // Auto-hide the mascot toast after 8 seconds
                scope.launch {
                    delay(8000)
                    if (_state.value is MascotState.Active) {
                        _state.value = MascotState.Hidden
                        Log.d(TAG, "Mascot auto-hid after 8s")
                    }
                }
            } else {
                Log.w(TAG, "Failed to generate prompt: ${(result as ExecutorResult.Failure).error}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating prompt", e)
        }
    }
}
