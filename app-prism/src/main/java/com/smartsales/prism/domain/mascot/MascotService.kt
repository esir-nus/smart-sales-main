package com.smartsales.prism.domain.mascot

import kotlinx.coroutines.flow.StateFlow

sealed class MascotInteraction {
    data class Text(val content: String) : MascotInteraction()
    object Tap : MascotInteraction()
}

sealed class MascotResponse {
    data class Speak(val text: String, val emotion: String = "neutral") : MascotResponse()
    data class Suggestion(val quickReplies: List<String>) : MascotResponse()
    object Ignore : MascotResponse()
}

sealed class MascotState {
    object Hidden : MascotState()
    data class Active(val message: String, val emotion: String) : MascotState()
}

interface MascotService {
    fun startObserving()
    suspend fun interact(input: MascotInteraction): MascotResponse
    val state: StateFlow<MascotState>
}
