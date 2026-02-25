package com.smartsales.prism.ui.modes.analyst

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.analyst.AnalystPipeline
import com.smartsales.prism.domain.analyst.AnalystResponse
import com.smartsales.prism.domain.pipeline.ChatTurn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalystViewModel @Inject constructor(
    private val pipeline: AnalystPipeline
) : ViewModel() {

    private val TAG = "AnalystVM"

    val analystState = pipeline.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = pipeline.state.value
    )
    
    // Simple local history for the simulator
    private val _chatHistory = MutableStateFlow<List<ChatTurn>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()
    
    // Only holds the latest response for rendering
    private val _latestResponse = MutableStateFlow<AnalystResponse?>(null)
    val latestResponse = _latestResponse.asStateFlow()

    fun submitInput(input: String) {
        if (input.isBlank()) return
        Log.d(TAG, "submitInput: $input")
        
        // Add user msg to local history
        val updatedHistory = _chatHistory.value.toMutableList()
        updatedHistory.add(ChatTurn(role = "user", content = input))
        _chatHistory.value = updatedHistory

        viewModelScope.launch {
            val response = pipeline.handleInput(input, _chatHistory.value)
            Log.d(TAG, "Received Response: ${response::class.simpleName}")
            
            _latestResponse.value = response
            
            // If it's a chat response, append to history
            if (response is AnalystResponse.Chat) {
                val newHistory = _chatHistory.value.toMutableList()
                newHistory.add(ChatTurn(role = "assistant", content = response.content))
                _chatHistory.value = newHistory
            }
        }
    }
}
