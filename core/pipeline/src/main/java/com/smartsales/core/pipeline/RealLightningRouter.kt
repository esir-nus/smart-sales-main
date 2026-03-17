package com.smartsales.core.pipeline



import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult

import org.json.JSONObject
import javax.inject.Inject

class RealLightningRouter @Inject constructor(
    private val executor: Executor,
    private val promptCompiler: PromptCompiler
) : LightningRouter {

    private val TAG = "RealLightningRouter"

    override suspend fun evaluateIntent(context: EnhancedContext): RouterResult? {
        val prompt = promptCompiler.compile(context)
        val result = executor.execute(com.smartsales.core.llm.ModelRegistry.EXTRACTOR, prompt)

        if (result !is ExecutorResult.Success) {
            Log.e(TAG, "Executor failed during Phase 1")
            return null
        }

        val content = result.content
        val sanitized = content.replace("```json", "").replace("```", "").trim()

        return try {
            val json = JSONObject(sanitized)
            
            val queryQualityStr = json.optString("query_quality", "deep_analysis").lowercase()
            val queryQuality = when (queryQualityStr) {
                "noise" -> QueryQuality.NOISE
                "greeting" -> QueryQuality.GREETING
                "simple_qa" -> QueryQuality.SIMPLE_QA
                "deep_analysis" -> QueryQuality.DEEP_ANALYSIS
                "crm_task" -> QueryQuality.CRM_TASK
                "badge_delegation" -> QueryQuality.BADGE_DELEGATION
                else -> QueryQuality.DEEP_ANALYSIS
            }
            
            Log.d(TAG, "⚡ Lightning Router Intent: [$queryQualityStr] -> $queryQuality")
            
            val analysisObj = json.optJSONObject("analysis")
            val infoSufficient = analysisObj?.optBoolean("info_sufficient", false) 
                ?: json.optBoolean("info_sufficient", false)
            
            val response = json.optString("response", "我没完全明白，能再详细说说你想分析的内容吗？")
            
            val missingEntitiesList = mutableListOf<String>()
            val missingArray = json.optJSONArray("missing_entities")
            if (missingArray != null) {
                for (i in 0 until missingArray.length()) {
                    missingEntitiesList.add(missingArray.optString(i))
                }
            }
            
            if (queryQuality == QueryQuality.NOISE || queryQuality == QueryQuality.GREETING || queryQuality == QueryQuality.BADGE_DELEGATION) {
                missingEntitiesList.clear() // Prevent disambiguation loop for rejected/cross-domain intents
            }
            
            RouterResult(
                queryQuality = queryQuality,
                infoSufficient = infoSufficient,
                response = response,
                missingEntities = missingEntitiesList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Router JSON: \$sanitized", e)
            null
        }
    }
}
