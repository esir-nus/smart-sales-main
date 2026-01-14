// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/store/TingwuJobManifest.kt
// Module: :data:ai-core
// Summary: Data classes for job manifest persistence (V1 spec Appendix D.2)
// Author: created on 2026-01-14

package com.smartsales.data.aicore.tingwu.store

import com.google.gson.annotations.SerializedName

/**
 * Root manifest structure persisted to tingwu_jobs_manifest.json.
 * 
 * Schema designed for:
 * - AI agent readability (human-readable JSON)
 * - Batch-level targeted retry
 * - Atomic write safety
 */
data class TingwuJobManifest(
    @SerializedName("schemaVersion") val schemaVersion: Int = 1,
    @SerializedName("jobs") val jobs: List<PersistedJob> = emptyList()
)

/**
 * Persisted state for a single transcription job.
 * Tracks both job-level and batch-level progress.
 */
data class PersistedJob(
    @SerializedName("jobId") val jobId: String,
    @SerializedName("audioAssetName") val audioAssetName: String,
    @SerializedName("audioFilePath") val audioFilePath: String?,
    @SerializedName("ossObjectKey") val ossObjectKey: String?,
    @SerializedName("fileUrl") val fileUrl: String?,
    @SerializedName("totalDurationMs") val totalDurationMs: Long?,
    @SerializedName("language") val language: String = "zh-CN",
    @SerializedName("status") val status: JobStatus,
    @SerializedName("createdAtMs") val createdAtMs: Long,
    @SerializedName("updatedAtMs") val updatedAtMs: Long,
    @SerializedName("batches") val batches: List<PersistedBatch> = emptyList()
)

/**
 * Persisted state for a single batch within a multi-batch job.
 * Enables targeted retry of failed batches only.
 */
data class PersistedBatch(
    @SerializedName("batchIndex") val batchIndex: Int,
    @SerializedName("status") val status: BatchStatus,
    @SerializedName("tingwuJobId") val tingwuJobId: String? = null,
    @SerializedName("artifactPath") val artifactPath: String? = null,
    @SerializedName("diarizedSegmentsCount") val diarizedSegmentsCount: Int? = null,
    @SerializedName("captureStartMs") val captureStartMs: Long? = null,
    @SerializedName("captureEndMs") val captureEndMs: Long? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("attemptCount") val attemptCount: Int = 0
)

/**
 * Job-level status.
 */
enum class JobStatus {
    @SerializedName("PENDING") PENDING,
    @SerializedName("IN_PROGRESS") IN_PROGRESS,
    @SerializedName("SUCCEEDED") SUCCEEDED,
    @SerializedName("PARTIAL_FAILURE") PARTIAL_FAILURE,
    @SerializedName("FAILED") FAILED
}

/**
 * Batch-level status.
 */
enum class BatchStatus {
    @SerializedName("PENDING") PENDING,
    @SerializedName("SLICING") SLICING,
    @SerializedName("UPLOADING") UPLOADING,
    @SerializedName("SUBMITTED") SUBMITTED,
    @SerializedName("RUNNING") RUNNING,
    @SerializedName("SUCCEEDED") SUCCEEDED,
    @SerializedName("FAILED") FAILED
}
