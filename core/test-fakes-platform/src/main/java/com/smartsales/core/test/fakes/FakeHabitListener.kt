package com.smartsales.core.test.fakes

import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.pipeline.HabitListener
import kotlinx.coroutines.CoroutineScope

class FakeHabitListener : HabitListener {
    var rawInputCaptured: String? = null
    var contextCaptured: EnhancedContext? = null
    var analyzeAsyncCallCount = 0

    override fun analyzeAsync(rawInput: String, context: EnhancedContext, coroutineScope: CoroutineScope) {
        analyzeAsyncCallCount++
        rawInputCaptured = rawInput
        contextCaptured = context
    }
}
