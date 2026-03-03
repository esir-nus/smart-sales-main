package com.smartsales.prism.data.real

import android.util.Log
import com.smartsales.prism.domain.analyst.ConsultantResult
import com.smartsales.prism.domain.analyst.ConsultantService
import com.smartsales.prism.domain.pipeline.EnhancedContext
import com.smartsales.prism.domain.pipeline.Executor
import com.smartsales.prism.domain.pipeline.ExecutorResult
import org.json.JSONObject
import javax.inject.Inject

class RealConsultantService @Inject constructor(
    private val executor: Executor
) : ConsultantService {

    private val TAG = "RealConsultantService"

    override suspend fun evaluateIntent(context: EnhancedContext): ConsultantResult? {
        val result = executor.execute(com.smartsales.prism.domain.config.ModelRegistry.PLANNER, context)

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
                "actionable" -> com.smartsales.prism.domain.analyst.QueryQuality.ACTIONABLE
                else -> com.smartsales.prism.domain.analyst.QueryQuality.VAGUE
            }
            
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
            
            ConsultantResult(
                queryQuality = queryQuality,
                infoSufficient = infoSufficient,
                response = response,
                missingEntities = missingEntitiesList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Consultant JSON: \$sanitized", e)
            null
        }
    }
}
