// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/store/TingwuJobStore.kt
// Module: :data:ai-core
// Summary: Interface for job state persistence (V1 spec Appendix D.2)
// Author: created on 2026-01-14

package com.smartsales.data.aicore.tingwu.store

/**
 * Persistence store for Tingwu transcription jobs.
 * 
 * Enables:
 * - Batch-level status tracking
 * - Targeted retry of failed batches
 * - Manual recovery after app restart
 * 
 * Implementation uses file-based JSON manifest.
 */
interface TingwuJobStore {
    
    /**
     * Save or update a job in the manifest.
     */
    suspend fun saveJob(job: PersistedJob)
    
    /**
     * Load a specific job by ID.
     */
    suspend fun loadJob(jobId: String): PersistedJob?
    
    /**
     * Load all jobs from manifest.
     */
    suspend fun loadAll(): List<PersistedJob>
    
    /**
     * Update batch status within a job.
     * Creates the job if it doesn't exist.
     */
    suspend fun updateBatchStatus(
        jobId: String,
        batchIndex: Int,
        status: BatchStatus,
        tingwuJobId: String? = null,
        artifactPath: String? = null,
        diarizedSegmentsCount: Int? = null,
        error: String? = null
    )
    
    /**
     * Mark job as completed with final status.
     */
    suspend fun completeJob(jobId: String, status: JobStatus)
    
    /**
     * Get jobs that need retry (have failed/pending batches).
     */
    suspend fun getRetryableJobs(): List<PersistedJob>
    
    /**
     * Clear all jobs. Used in settings for management.
     */
    suspend fun clearAll()
    
    /**
     * Clear completed jobs older than specified age.
     */
    suspend fun clearOlderThan(ageMs: Long)
}
