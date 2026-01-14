// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/store/FileBasedTingwuJobStore.kt
// Module: :data:ai-core
// Summary: File-based implementation of TingwuJobStore (V1 spec Appendix D.2)
// Author: created on 2026-01-14

package com.smartsales.data.aicore.tingwu.store

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.smartsales.data.aicore.AiCoreLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * File-based implementation of [TingwuJobStore].
 * 
 * Storage: Single JSON manifest file with atomic writes.
 * Thread-safety: Mutex for concurrent access.
 * 
 * @param storageDir Directory for manifest and batch artifacts (provided by app)
 */
@Singleton
class FileBasedTingwuJobStore @Inject constructor(
    @Named("TingwuStoreDir") private val storageDir: File
) : TingwuJobStore {

    private val mutex = Mutex()
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val manifestFile: File get() = File(storageDir, MANIFEST_FILENAME)
    private val tempFile: File get() = File(storageDir, MANIFEST_TEMP_FILENAME)

    init {
        // Ensure storage directory exists
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }

    override suspend fun saveJob(job: PersistedJob) = mutex.withLock {
        val manifest = loadManifestInternal()
        val updatedJobs = manifest.jobs.toMutableList()
        val existingIndex = updatedJobs.indexOfFirst { it.jobId == job.jobId }
        if (existingIndex >= 0) {
            updatedJobs[existingIndex] = job
        } else {
            updatedJobs.add(job)
        }
        writeManifestAtomic(manifest.copy(jobs = updatedJobs))
        AiCoreLogger.d(TAG, "Saved job: ${job.jobId} status=${job.status}")
    }

    override suspend fun loadJob(jobId: String): PersistedJob? = mutex.withLock {
        loadManifestInternal().jobs.find { it.jobId == jobId }
    }

    override suspend fun loadAll(): List<PersistedJob> = mutex.withLock {
        loadManifestInternal().jobs
    }

    override suspend fun updateBatchStatus(
        jobId: String,
        batchIndex: Int,
        status: BatchStatus,
        tingwuJobId: String?,
        artifactPath: String?,
        diarizedSegmentsCount: Int?,
        error: String?
    ) = mutex.withLock {
        val manifest = loadManifestInternal()
        val job = manifest.jobs.find { it.jobId == jobId } ?: return@withLock
        
        val updatedBatches = job.batches.toMutableList()
        val existingIndex = updatedBatches.indexOfFirst { it.batchIndex == batchIndex }
        
        val updatedBatch = if (existingIndex >= 0) {
            val existing = updatedBatches[existingIndex]
            existing.copy(
                status = status,
                tingwuJobId = tingwuJobId ?: existing.tingwuJobId,
                artifactPath = artifactPath ?: existing.artifactPath,
                diarizedSegmentsCount = diarizedSegmentsCount ?: existing.diarizedSegmentsCount,
                error = error,
                attemptCount = if (status == BatchStatus.FAILED) existing.attemptCount + 1 else existing.attemptCount
            )
        } else {
            PersistedBatch(
                batchIndex = batchIndex,
                status = status,
                tingwuJobId = tingwuJobId,
                artifactPath = artifactPath,
                diarizedSegmentsCount = diarizedSegmentsCount,
                error = error
            )
        }
        
        if (existingIndex >= 0) {
            updatedBatches[existingIndex] = updatedBatch
        } else {
            updatedBatches.add(updatedBatch)
        }
        
        // Compute job-level status from batch statuses
        val jobStatus = computeJobStatus(updatedBatches)
        val updatedJob = job.copy(
            batches = updatedBatches.sortedBy { it.batchIndex },
            status = jobStatus,
            updatedAtMs = System.currentTimeMillis()
        )
        
        saveJobInternal(manifest, updatedJob)
        AiCoreLogger.d(TAG, "Updated batch: job=$jobId batch=$batchIndex status=$status")
    }

    override suspend fun completeJob(jobId: String, status: JobStatus) = mutex.withLock {
        val manifest = loadManifestInternal()
        val job = manifest.jobs.find { it.jobId == jobId } ?: return@withLock
        val updatedJob = job.copy(
            status = status,
            updatedAtMs = System.currentTimeMillis()
        )
        saveJobInternal(manifest, updatedJob)
        AiCoreLogger.d(TAG, "Completed job: $jobId status=$status")
    }

    override suspend fun getRetryableJobs(): List<PersistedJob> = mutex.withLock {
        loadManifestInternal().jobs.filter { job ->
            job.status == JobStatus.PARTIAL_FAILURE ||
            job.status == JobStatus.FAILED ||
            job.batches.any { it.status == BatchStatus.FAILED || it.status == BatchStatus.PENDING }
        }
    }

    override suspend fun clearAll() = mutex.withLock {
        writeManifestAtomic(TingwuJobManifest())
        AiCoreLogger.d(TAG, "Cleared all jobs")
    }

    override suspend fun clearOlderThan(ageMs: Long) = mutex.withLock {
        val cutoff = System.currentTimeMillis() - ageMs
        val manifest = loadManifestInternal()
        val filtered = manifest.jobs.filter { job ->
            // Keep non-completed or recently updated
            job.status != JobStatus.SUCCEEDED || job.updatedAtMs > cutoff
        }
        writeManifestAtomic(manifest.copy(jobs = filtered))
        val removed = manifest.jobs.size - filtered.size
        AiCoreLogger.d(TAG, "Cleared $removed jobs older than ${ageMs}ms")
    }

    // --- Internal helpers ---

    private fun loadManifestInternal(): TingwuJobManifest {
        if (!manifestFile.exists()) {
            return TingwuJobManifest()
        }
        return try {
            val json = manifestFile.readText()
            gson.fromJson(json, TingwuJobManifest::class.java) ?: TingwuJobManifest()
        } catch (e: Exception) {
            AiCoreLogger.e(TAG, "Failed to load manifest, returning empty", e)
            TingwuJobManifest()
        }
    }

    private fun saveJobInternal(manifest: TingwuJobManifest, job: PersistedJob) {
        val updatedJobs = manifest.jobs.toMutableList()
        val existingIndex = updatedJobs.indexOfFirst { it.jobId == job.jobId }
        if (existingIndex >= 0) {
            updatedJobs[existingIndex] = job
        } else {
            updatedJobs.add(job)
        }
        writeManifestAtomic(manifest.copy(jobs = updatedJobs))
    }

    private fun writeManifestAtomic(manifest: TingwuJobManifest) {
        val json = gson.toJson(manifest)
        // Atomic write: write to temp, then rename
        tempFile.writeText(json)
        tempFile.renameTo(manifestFile)
    }

    private fun computeJobStatus(batches: List<PersistedBatch>): JobStatus {
        if (batches.isEmpty()) return JobStatus.PENDING
        val allSucceeded = batches.all { it.status == BatchStatus.SUCCEEDED }
        val anyFailed = batches.any { it.status == BatchStatus.FAILED }
        val anyPending = batches.any { it.status == BatchStatus.PENDING }
        val anyRunning = batches.any { 
            it.status == BatchStatus.RUNNING || 
            it.status == BatchStatus.SUBMITTED ||
            it.status == BatchStatus.SLICING ||
            it.status == BatchStatus.UPLOADING
        }
        return when {
            allSucceeded -> JobStatus.SUCCEEDED
            anyFailed && batches.any { it.status == BatchStatus.SUCCEEDED } -> JobStatus.PARTIAL_FAILURE
            anyFailed -> JobStatus.FAILED
            anyRunning -> JobStatus.IN_PROGRESS
            anyPending -> JobStatus.PENDING
            else -> JobStatus.IN_PROGRESS
        }
    }

    companion object {
        private const val TAG = "TingwuJobStore"
        private const val MANIFEST_FILENAME = "tingwu_jobs_manifest.json"
        private const val MANIFEST_TEMP_FILENAME = "tingwu_jobs_manifest.tmp"
    }
}
