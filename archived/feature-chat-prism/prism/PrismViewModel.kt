package com.smartsales.feature.chat.prism

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.domain.prism.core.Mode
import com.smartsales.domain.prism.core.ModePublisher
import com.smartsales.domain.prism.core.Orchestrator
import com.smartsales.domain.prism.core.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * Prism ViewModel — 连接 ModePublisher 和 Orchestrator
 * @see Prism-V1.md §2.2
 */
@HiltViewModel
class PrismViewModel @Inject constructor(
    @Named("coach") private val coachPublisher: ModePublisher,
    @Named("analyst") private val analystPublisher: ModePublisher,
    @Named("scheduler") private val schedulePublisher: ModePublisher,
    private val orchestrator: Orchestrator
) : ViewModel() {

    // 当前模式
    val currentMode: StateFlow<Mode> = orchestrator.currentMode

    // 当前 UI 状态（根据模式从对应 Publisher 获取）
    val uiState: StateFlow<UiState>
        get() = when (currentMode.value) {
            Mode.COACH -> coachPublisher.uiState
            Mode.ANALYST -> analystPublisher.uiState
            Mode.SCHEDULER -> schedulePublisher.uiState
        }

    // 输入文本
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    // 发送状态
    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 更新输入文本
     */
    fun onInputChanged(text: String) {
        _inputText.value = text
    }

    /**
     * 切换模式
     */
    fun switchMode(mode: Mode) {
        viewModelScope.launch {
            orchestrator.switchMode(mode)
        }
    }

    /**
     * 发送用户输入
     */
    fun send() {
        val input = _inputText.value.trim()
        if (input.isEmpty() || _isSending.value) return

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null

            try {
                val result = orchestrator.processUserIntent(input)
                // 清空输入
                _inputText.value = ""
                
                // 发布到对应的 Publisher
                val publisher = when (currentMode.value) {
                    Mode.COACH -> coachPublisher
                    Mode.ANALYST -> analystPublisher
                    Mode.SCHEDULER -> schedulePublisher
                }
                publisher.publish(result.executorResult)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "发送失败"
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
