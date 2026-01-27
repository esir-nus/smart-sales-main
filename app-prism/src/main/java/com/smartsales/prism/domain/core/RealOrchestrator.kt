package com.smartsales.prism.domain.core

import com.smartsales.data.aicore.DashscopeClient
import com.smartsales.data.aicore.DashscopeCredentialsProvider
import com.smartsales.data.aicore.DashscopeMessage
import com.smartsales.data.aicore.DashscopeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真实 Orchestrator — 调用 DashScope API
 * @see Prism-V1.md §2.2 #2
 */
@Singleton
class RealOrchestrator @Inject constructor(
    private val dashscopeClient: DashscopeClient,
    private val credentialsProvider: DashscopeCredentialsProvider
) : Orchestrator {

    private val _currentMode = MutableStateFlow(Mode.COACH)
    override val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()

    override suspend fun processInput(input: String): UiState {
        return withContext(Dispatchers.IO) {
            try {
                val creds = credentialsProvider.obtain()
                val systemPrompt = getSystemPrompt(_currentMode.value)
                
                val request = DashscopeRequest(
                    apiKey = creds.apiKey,
                    model = creds.model,
                    messages = listOf(
                        DashscopeMessage(role = "system", content = systemPrompt),
                        DashscopeMessage(role = "user", content = input)
                    )
                )
                
                // 使用 generate 同步调用（按 §3.1 Buffered Streaming 设计）
                val response = dashscopeClient.generate(request)
                UiState.Response(response.displayText)
                
            } catch (e: Exception) {
                android.util.Log.e("Prism", "DashScope 调用失败", e)
                UiState.Error(
                    message = e.message ?: "请求失败",
                    retryable = true
                )
            }
        }
    }

    override suspend fun switchMode(newMode: Mode) {
        _currentMode.value = newMode
    }
    
    private fun getSystemPrompt(mode: Mode): String {
        return when (mode) {
            Mode.COACH -> Prompts.COACH_SYSTEM
            Mode.ANALYST -> Prompts.ANALYST_SYSTEM
            Mode.SCHEDULER -> Prompts.SCHEDULER_SYSTEM
        }
    }
}
