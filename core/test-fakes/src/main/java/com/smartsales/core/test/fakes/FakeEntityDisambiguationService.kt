package com.smartsales.core.test.fakes

import com.smartsales.core.pipeline.EntityDisambiguationService
import com.smartsales.core.pipeline.DisambiguationResult
import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import com.smartsales.prism.domain.memory.EntityRef

class FakeEntityDisambiguationService : EntityDisambiguationService {
    var nextResult: DisambiguationResult = DisambiguationResult.PassThrough
    
    override suspend fun process(rawInput: String): DisambiguationResult {
        return nextResult
    }
    
    override fun startDisambiguation(
        originalInput: String, 
        originalMode: Mode, 
        ambiguousName: String, 
        candidates: List<EntityRef>
    ): UiState {
        return UiState.Response("Disambiguation started for $ambiguousName", "")
    }
    
    override fun cancel() {
        // No-op
    }
}
