// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/processor/FakeTranscriptProcessor.kt
// Module: :data:ai-core
// Summary: Fake implementation of TranscriptProcessor for testing (Lattice pattern)
// Author: created on 2026-01-14

package com.smartsales.data.aicore.tingwu.processor

import com.smartsales.data.aicore.DiarizedSegment
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.tingwu.api.TingwuTranscription

/**
 * FakeTranscriptProcessor: Test double for TranscriptProcessor.
 * 
 * Provides stub behavior, call tracking, and reset for test isolation.
 * Callbacks are ignored - returns stubResult directly.
 */
class FakeTranscriptProcessor : TranscriptProcessor {
    
    /** Override to inject specific result. */
    var stubResult: TranscriptResult? = null
    
    /** Override to throw specific error. */
    var stubError: Throwable? = null
    
    /** Track all fetch calls. */
    val calls = mutableListOf<String>()
    
    override suspend fun fetchTranscript(
        jobId: String,
        resultLinks: Map<String, String>?,
        fallbackArtifacts: TingwuJobArtifacts?,
        runEnhancer: suspend (TingwuTranscription?, List<DiarizedSegment>, Map<String, String>, String) -> String,
        composeFinalMarkdown: (String, TingwuJobArtifacts?, Map<String, String>?) -> String
    ): TranscriptResult {
        calls.add(jobId)
        
        stubError?.let { throw it }
        
        return stubResult ?: TranscriptResult(
            markdown = "Fake transcript for $jobId",
            artifacts = fallbackArtifacts,
            chapters = null,
            diarizedSegments = null
        )
    }
    
    /** Reset state between tests. */
    fun reset() {
        stubResult = null
        stubError = null
        calls.clear()
    }
}
