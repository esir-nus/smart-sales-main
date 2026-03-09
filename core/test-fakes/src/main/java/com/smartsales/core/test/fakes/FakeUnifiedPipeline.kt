package com.smartsales.core.test.fakes

import com.smartsales.core.pipeline.PipelineInput
import com.smartsales.core.pipeline.PipelineResult
import com.smartsales.core.pipeline.UnifiedPipeline
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class FakeUnifiedPipeline : UnifiedPipeline {
    
    val processedInputs = mutableListOf<PipelineInput>()
    var nextResultFlow: Flow<PipelineResult>? = null

    override suspend fun processInput(input: PipelineInput): Flow<PipelineResult> {
        processedInputs.add(input)
        return nextResultFlow ?: emptyFlow()
    }
}
