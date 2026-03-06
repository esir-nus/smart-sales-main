package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.analyst.RouterResult
import com.smartsales.prism.domain.analyst.LightningRouter
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import org.json.JSONObject
import javax.inject.Inject

class RealLightningRouter @Inject constructor(
    private val executor: Executor
) : LightningRouter {

    private val TAG = "RealLightningRouter"

    override suspend fun evaluateIntent(context: EnhancedContext): RouterResult? {
        val result = executor.execute(com.smartsales.prism.domain.config.ModelRegistry.EXTRACTOR, context)

        if (result !is ExecutorResult.Success) {
            Log.e(TAG, "Executor failed during Phase 1")
            return null
        }

        val content = result.content
        val sanitized = content.replace("```json", "").replace("```", "").trim()

        return try {
            val json = JSONObject(sanitized)
            
            val queryQualityStr = json.optString("query_quality", "vague").lowercase()
            val queryQuality = when (queryQualityStr) {
                "noise" -> com.smartsales.prism.domain.analyst.QueryQuality.NOISE
                "greeting" -> com.smartsales.prism.domain.analyst.QueryQuality.GREETING
                "simple_qa" -> com.smartsales.prism.domain.analyst.QueryQuality.SIMPLE_QA
                "deep_analysis" -> com.smartsales.prism.domain.analyst.QueryQuality.DEEP_ANALYSIS
                "crm_task" -> com.smartsales.prism.domain.analyst.QueryQuality.CRM_TASK
                else -> com.smartsales.prism.domain.analyst.QueryQuality.VAGUE
            }
            
            Log.d(TAG, "⚡ Lightning Router Intent: [\$queryQualityStr] -> \$queryQuality")
            
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
            
            if (queryQuality == com.smartsales.prism.domain.analyst.QueryQuality.NOISE || queryQuality == com.smartsales.prism.domain.analyst.QueryQuality.GREETING || queryQuality == com.smartsales.prism.domain.analyst.QueryQuality.VAGUE) {
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
