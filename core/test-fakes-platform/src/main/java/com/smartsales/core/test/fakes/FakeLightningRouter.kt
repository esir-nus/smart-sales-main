package com.smartsales.core.test.fakes

import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.pipeline.LightningRouter
import com.smartsales.core.pipeline.QueryQuality
import com.smartsales.core.pipeline.RouterResult

class FakeLightningRouter : LightningRouter {
    
    // Controlled queue for evaluation results
    private val resultQueue = mutableListOf<RouterResult?>()
    val evaluatedContexts = mutableListOf<EnhancedContext>()
    
    fun enqueueResult(result: RouterResult?) {
        resultQueue.add(result)
    }

    override suspend fun evaluateIntent(context: EnhancedContext): RouterResult? {
        evaluatedContexts.add(context)
        return if (resultQueue.isNotEmpty()) {
            resultQueue.removeAt(0)
        } else {
            // Default safe fallback
            RouterResult(QueryQuality.NOISE, false, "...")
        }
    }
}
