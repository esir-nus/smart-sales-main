package com.smartsales.core.pipeline

import kotlinx.coroutines.flow.Flow

/**
 * Executes the conversational pipeline: extracts intent, disambiguates entities,
 * assembles the context, and either dispatches a tool or streams a response.
 */
interface UnifiedPipeline {
    suspend fun processInput(input: PipelineInput): Flow<PipelineResult>
}
