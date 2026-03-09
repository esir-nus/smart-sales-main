package com.smartsales.core.test.fakes

import com.smartsales.core.pipeline.MascotInteraction
import com.smartsales.core.pipeline.MascotResponse
import com.smartsales.core.pipeline.MascotService
import com.smartsales.core.pipeline.MascotState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeMascotService : MascotService {
    
    private val _state = MutableStateFlow<MascotState>(MascotState.Hidden)
    override val state: StateFlow<MascotState> = _state
    
    val interactions = mutableListOf<MascotInteraction>()
    var nextResponse: MascotResponse = MascotResponse.Ignore

    override fun startObserving() {
        // No-op for fake
    }

    override suspend fun interact(input: MascotInteraction): MascotResponse {
        interactions.add(input)
        return nextResponse
    }
}
