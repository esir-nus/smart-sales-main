package com.smartsales.prism.domain.analyst

import com.smartsales.prism.domain.pipeline.EnhancedContext

enum class QueryQuality {
    NOISE, SIMPLE_QA, DEEP_ANALYSIS, CRM_TASK, VAGUE
}

/**
 * Phase 0 Router Result
 * Represents the 4-Tier Intent Evaluation Gateway's decision.
 */
data class RouterResult(
    val queryQuality: QueryQuality,
    val infoSufficient: Boolean,
    val response: String,
    val missingEntities: List<String> = emptyList()
)

/**
 * Phase 0 Lightning Router
 * Evaluates the user's intent against MINIMAL RAM context before delegating to Mascot or Analyst.
 */
interface LightningRouter {
    suspend fun evaluateIntent(context: EnhancedContext): RouterResult?
}
