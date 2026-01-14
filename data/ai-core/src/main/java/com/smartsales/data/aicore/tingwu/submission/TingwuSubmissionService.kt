// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/submission/TingwuSubmissionService.kt
// Module: :data:ai-core
// Summary: Interface and DTOs for Tingwu task submission (Lattice Box)
// Author: created on 2026-01-14

package com.smartsales.data.aicore.tingwu.submission

import com.smartsales.core.util.Result

/**
 * TingwuSubmissionService: Lattice box for Tingwu API task creation.
 * 
 * Extracts the createTranscriptionTask API call from TingwuRunner.
 * Service owns settings resolution internally.
 */
interface TingwuSubmissionService {
    /**
     * Submit a transcription task to Tingwu API.
     * 
     * @param input Minimal submission parameters
     * @return taskId and requestId on success
     */
    suspend fun submit(input: SubmissionInput): Result<SubmissionOutput>
}

/**
 * Minimal input for task submission.
 * Service looks up credentials and settings internally.
 */
data class SubmissionInput(
    val fileUrl: String,
    val taskKey: String,
    val sourceLanguage: String,
    val diarizationEnabled: Boolean = true,
    val customPromptName: String? = null,
    val customPromptText: String? = null
)

/**
 * Output from successful task submission.
 */
data class SubmissionOutput(
    val taskId: String,
    val requestId: String
)

/**
 * Fake implementation for testing.
 */
class FakeTingwuSubmissionService : TingwuSubmissionService {
    
    /** Override to return a specific output. */
    var stubOutput: SubmissionOutput? = null
    
    /** Override to force an error. */
    var stubError: Throwable? = null
    
    /** Track all submit calls for verification. */
    val calls = mutableListOf<SubmissionInput>()
    
    override suspend fun submit(input: SubmissionInput): Result<SubmissionOutput> {
        calls.add(input)
        return stubError?.let { Result.Error(it) }
            ?: Result.Success(stubOutput ?: SubmissionOutput(
                taskId = "fake_task_${input.taskKey}",
                requestId = "fake_req_${System.currentTimeMillis()}"
            ))
    }
    
    /** Reset state between tests. */
    fun reset() {
        stubOutput = null
        stubError = null
        calls.clear()
    }
}
