// File: data/ai-core/src/main/java/com/smartsales/data/aicore/posttingwu/FakePostTingwuTranscriptEnhancer.kt
// Module: :data:ai-core
// Summary: Fake implementation of PostTingwuTranscriptEnhancer for testing (Lattice pattern)
// Author: created on 2026-01-15

package com.smartsales.data.aicore.posttingwu

/**
 * FakePostTingwuTranscriptEnhancer: Test double for PostTingwuTranscriptEnhancer.
 */
class FakePostTingwuTranscriptEnhancer : PostTingwuTranscriptEnhancer {
    
    /** Override to inject specific output. */
    var stubOutput: EnhancerOutput? = null
    
    /** Override to throw specific error. */
    var stubError: Throwable? = null
    
    /** Track all enhance calls. */
    val calls = mutableListOf<String>()
    
    override suspend fun enhance(input: EnhancerInput): EnhancerOutput? {
        calls.add(input.jobId)
        
        stubError?.let { throw it }
        
        return stubOutput
    }
    
    fun reset() {
        stubOutput = null
        stubError = null
        calls.clear()
    }
}
