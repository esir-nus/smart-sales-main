package com.smartsales.prism.domain.analyst

import com.smartsales.prism.domain.pipeline.EnhancedContext

enum class QueryQuality {
    NOISE, VAGUE, ACTIONABLE
}

/**
 * Wave 2: Phase 1 Consultant Result
 * Represents the intent evaluation of the Analyst mode's first pass.
 */
data class ConsultantResult(
    val queryQuality: QueryQuality,
    val infoSufficient: Boolean,
    val response: String,
    val missingEntities: List<String> = emptyList()
)

/**
 * Analyst Phase 1 Service: Chat & Intent Evaluation
 * Checks if the RAM EnhancedContext is sufficient for the requested analysis.
 */
interface ConsultantService {
    suspend fun evaluateIntent(context: EnhancedContext): ConsultantResult?
}
