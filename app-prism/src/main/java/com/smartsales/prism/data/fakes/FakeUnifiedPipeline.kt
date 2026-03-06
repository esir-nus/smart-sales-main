package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.unifiedpipeline.PipelineInput
import com.smartsales.prism.domain.unifiedpipeline.PipelineResult
import com.smartsales.prism.domain.unifiedpipeline.UnifiedPipeline
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FakeUnifiedPipeline @Inject constructor() : UnifiedPipeline {
    
    override suspend fun processInput(input: PipelineInput): Flow<PipelineResult> = flow {
        // Quick delay to simulate ETL
        delay(300)

        // Clean mock responses based on intent
        when {
            input.rawText.contains("missing", ignoreCase = true) -> {
                emit(PipelineResult.ClarificationNeeded("I'm not exactly sure which company you mean. Did you mean Acme Corp?"))
            }
            input.rawText.contains("report", ignoreCase = true) -> {
                emit(
                    PipelineResult.ToolDispatch(
                        toolId = "GENERATE_PDF",
                        params = mapOf("urgency" to "high")
                    )
                )
            }
            else -> {
                // Return standard conversational text simulating a built payload success
                emit(PipelineResult.ConversationalReply("Here is the analysis based on your input: ${input.rawText}. Everything looks well structured."))
            }
        }
    }
}
