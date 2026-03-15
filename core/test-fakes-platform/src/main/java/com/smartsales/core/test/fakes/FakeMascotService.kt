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
        
        if (input is MascotInteraction.Text) {
            _state.value = MascotState.Active("收到：${input.content}", "happy")
            // In a real test, we might advance time to see it go hidden, 
            // but for simple fakes, we just leave it active unless a test clears it, 
            // or we launch a minimal scope if needed. We'll simulate it by launching
            // on a GlobalScope or we just let the test assert Active.
            // Actually, we can just leave it Active to allow tests to assert on it. The real one auto-clears.
        } else if (input is MascotInteraction.Tap) {
            _state.value = MascotState.Hidden
        }
        
        return nextResponse
    }
}
