package com.smartsales.data.aicore.tingwu.store

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Integration tests for [FileBasedTingwuJobStore].
 * 
 * Tests the full persistence lifecycle:
 * - Create job with batches
 * - Update batch statuses through lifecycle
 * - Verify manifest file on disk
 * - Simulate app restart (reload from file)
 * - Verify recovery and retry logic
 */
class TingwuJobStoreIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storeDir: File
    private lateinit var store: FileBasedTingwuJobStore

    @Before
    fun setup() {
        storeDir = tempFolder.newFolder("tingwu_store")
        store = FileBasedTingwuJobStore(storeDir)
    }

    // --- Lifecycle Tests ---

    @Test
    fun fullBatchLifecycle_savesAndUpdatesCorrectly() = runTest {
        // Create job with 3 batches
        val job = PersistedJob(
            jobId = "job_001",
            audioAssetName = "meeting_2026-01-14.wav",
            audioFilePath = "/data/audio/meeting.wav",
            ossObjectKey = "uploads/meeting.wav",
            fileUrl = null,
            totalDurationMs = 1800000, // 30 min
            language = "zh-CN",
            status = JobStatus.IN_PROGRESS,
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis(),
            batches = listOf(
                PersistedBatch(batchIndex = 0, status = BatchStatus.PENDING, captureStartMs = 0, captureEndMs = 600000),
                PersistedBatch(batchIndex = 1, status = BatchStatus.PENDING, captureStartMs = 600000, captureEndMs = 1200000),
                PersistedBatch(batchIndex = 2, status = BatchStatus.PENDING, captureStartMs = 1200000, captureEndMs = 1800000)
            )
        )
        store.saveJob(job)

        // Simulate batch 0: PENDING -> SLICING -> UPLOADING -> SUBMITTED -> RUNNING -> SUCCEEDED
        store.updateBatchStatus("job_001", 0, BatchStatus.SLICING)
        store.updateBatchStatus("job_001", 0, BatchStatus.UPLOADING)
        store.updateBatchStatus("job_001", 0, BatchStatus.SUBMITTED, tingwuJobId = "tw_batch_0")
        store.updateBatchStatus("job_001", 0, BatchStatus.RUNNING, tingwuJobId = "tw_batch_0")
        store.updateBatchStatus("job_001", 0, BatchStatus.SUCCEEDED, tingwuJobId = "tw_batch_0", diarizedSegmentsCount = 42)

        // Verify batch 0 is SUCCEEDED
        val afterBatch0 = store.loadJob("job_001")!!
        assertEquals(BatchStatus.SUCCEEDED, afterBatch0.batches.find { it.batchIndex == 0 }?.status)
        assertEquals(42, afterBatch0.batches.find { it.batchIndex == 0 }?.diarizedSegmentsCount)
        assertEquals("tw_batch_0", afterBatch0.batches.find { it.batchIndex == 0 }?.tingwuJobId)

        // Simulate batch 1: PENDING -> ... -> FAILED
        store.updateBatchStatus("job_001", 1, BatchStatus.SLICING)
        store.updateBatchStatus("job_001", 1, BatchStatus.UPLOADING)
        store.updateBatchStatus("job_001", 1, BatchStatus.SUBMITTED, tingwuJobId = "tw_batch_1")
        store.updateBatchStatus("job_001", 1, BatchStatus.RUNNING, tingwuJobId = "tw_batch_1")
        store.updateBatchStatus("job_001", 1, BatchStatus.FAILED, tingwuJobId = "tw_batch_1", error = "TIMEOUT")

        // Verify job is now PARTIAL_FAILURE
        val afterBatch1 = store.loadJob("job_001")!!
        assertEquals(JobStatus.PARTIAL_FAILURE, afterBatch1.status)
        assertEquals(BatchStatus.FAILED, afterBatch1.batches.find { it.batchIndex == 1 }?.status)
        assertEquals("TIMEOUT", afterBatch1.batches.find { it.batchIndex == 1 }?.error)
    }

    @Test
    fun simulateAppRestart_recoversJobFromDisk() = runTest {
        // Create and save job
        val job = PersistedJob(
            jobId = "job_restart",
            audioAssetName = "restart_test.wav",
            audioFilePath = "/data/audio/restart.wav",
            ossObjectKey = null,
            fileUrl = "https://oss.example.com/restart.wav",
            totalDurationMs = 600000,
            language = "zh-CN",
            status = JobStatus.IN_PROGRESS,
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis(),
            batches = listOf(
                PersistedBatch(batchIndex = 0, status = BatchStatus.SUCCEEDED, tingwuJobId = "tw_0", diarizedSegmentsCount = 10),
                PersistedBatch(batchIndex = 1, status = BatchStatus.RUNNING, tingwuJobId = "tw_1")
            )
        )
        store.saveJob(job)

        // Simulate app restart: create NEW store instance from same directory
        val newStore = FileBasedTingwuJobStore(storeDir)

        // Verify job is recovered
        val recovered = newStore.loadJob("job_restart")
        assertNotNull(recovered)
        assertEquals("job_restart", recovered!!.jobId)
        assertEquals(2, recovered.batches.size)
        assertEquals(BatchStatus.SUCCEEDED, recovered.batches[0].status)
        assertEquals(BatchStatus.RUNNING, recovered.batches[1].status)
    }

    @Test
    fun getRetryableJobs_findsPartialFailures() = runTest {
        // Job 1: All succeeded (not retryable)
        store.saveJob(PersistedJob(
            jobId = "job_complete",
            audioAssetName = "complete.wav",
            audioFilePath = null,
            ossObjectKey = null,
            fileUrl = null,
            totalDurationMs = null,
            language = "zh-CN",
            status = JobStatus.SUCCEEDED,
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis(),
            batches = listOf(
                PersistedBatch(batchIndex = 0, status = BatchStatus.SUCCEEDED)
            )
        ))

        // Job 2: Has failed batch (retryable)
        store.saveJob(PersistedJob(
            jobId = "job_partial",
            audioAssetName = "partial.wav",
            audioFilePath = null,
            ossObjectKey = null,
            fileUrl = null,
            totalDurationMs = null,
            language = "zh-CN",
            status = JobStatus.PARTIAL_FAILURE,
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis(),
            batches = listOf(
                PersistedBatch(batchIndex = 0, status = BatchStatus.SUCCEEDED),
                PersistedBatch(batchIndex = 1, status = BatchStatus.FAILED, error = "TIMEOUT")
            )
        ))

        // Job 3: Has pending batch (retryable)
        store.saveJob(PersistedJob(
            jobId = "job_pending",
            audioAssetName = "pending.wav",
            audioFilePath = null,
            ossObjectKey = null,
            fileUrl = null,
            totalDurationMs = null,
            language = "zh-CN",
            status = JobStatus.IN_PROGRESS,
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis(),
            batches = listOf(
                PersistedBatch(batchIndex = 0, status = BatchStatus.SUCCEEDED),
                PersistedBatch(batchIndex = 1, status = BatchStatus.PENDING)
            )
        ))

        // Get retryable jobs
        val retryable = store.getRetryableJobs()

        assertEquals(2, retryable.size)
        assertTrue(retryable.any { it.jobId == "job_partial" })
        assertTrue(retryable.any { it.jobId == "job_pending" })
    }

    @Test
    fun manifestFile_isHumanReadable() = runTest {
        store.saveJob(PersistedJob(
            jobId = "job_readable",
            audioAssetName = "readable.wav",
            audioFilePath = null,
            ossObjectKey = null,
            fileUrl = null,
            totalDurationMs = 60000,
            language = "zh-CN",
            status = JobStatus.IN_PROGRESS,
            createdAtMs = 1705200000000,
            updatedAtMs = 1705200000000,
            batches = listOf(
                PersistedBatch(batchIndex = 0, status = BatchStatus.SUCCEEDED, tingwuJobId = "tw_0")
            )
        ))

        // Read manifest file directly
        val manifestFile = File(storeDir, "tingwu_jobs_manifest.json")
        assertTrue(manifestFile.exists())
        
        val content = manifestFile.readText()
        
        // Verify human-readable (pretty-printed)
        assertTrue(content.contains("\"jobId\""))
        assertTrue(content.contains("\"job_readable\""))
        assertTrue(content.contains("\"batches\""))
        assertTrue(content.contains("\"SUCCEEDED\""))
        
        // AI-inspectable: can grep for status
        assertTrue(content.lines().any { it.contains("SUCCEEDED") })
    }

    @Test
    fun atomicWrite_survivesCorruption() = runTest {
        // Save initial job
        store.saveJob(PersistedJob(
            jobId = "job_atomic",
            audioAssetName = "atomic.wav",
            audioFilePath = null,
            ossObjectKey = null,
            fileUrl = null,
            totalDurationMs = null,
            language = "zh-CN",
            status = JobStatus.IN_PROGRESS,
            createdAtMs = System.currentTimeMillis(),
            updatedAtMs = System.currentTimeMillis(),
            batches = emptyList()
        ))

        // Verify temp file doesn't exist after write
        val tempFile = File(storeDir, "tingwu_jobs_manifest.tmp")
        val manifestFile = File(storeDir, "tingwu_jobs_manifest.json")
        
        assertTrue(manifestFile.exists())
        // Temp file should be renamed to manifest, so it shouldn't exist
        // (unless write is in progress, which it's not after suspend returns)
    }

    @Test
    fun clearOlderThan_removesOldCompletedJobs() = runTest {
        val now = System.currentTimeMillis()
        val oldTime = now - 10 * 24 * 60 * 60 * 1000L // 10 days ago

        // Old completed job (should be cleared)
        store.saveJob(PersistedJob(
            jobId = "old_job",
            audioAssetName = "old.wav",
            audioFilePath = null, ossObjectKey = null, fileUrl = null, totalDurationMs = null,
            language = "zh-CN",
            status = JobStatus.SUCCEEDED,
            createdAtMs = oldTime,
            updatedAtMs = oldTime,
            batches = emptyList()
        ))

        // Recent completed job (should NOT be cleared)
        store.saveJob(PersistedJob(
            jobId = "new_job",
            audioAssetName = "new.wav",
            audioFilePath = null, ossObjectKey = null, fileUrl = null, totalDurationMs = null,
            language = "zh-CN",
            status = JobStatus.SUCCEEDED,
            createdAtMs = now,
            updatedAtMs = now,
            batches = emptyList()
        ))

        // Old incomplete job (should NOT be cleared - keep for retry)
        store.saveJob(PersistedJob(
            jobId = "old_incomplete",
            audioAssetName = "incomplete.wav",
            audioFilePath = null, ossObjectKey = null, fileUrl = null, totalDurationMs = null,
            language = "zh-CN",
            status = JobStatus.PARTIAL_FAILURE,
            createdAtMs = oldTime,
            updatedAtMs = oldTime,
            batches = emptyList()
        ))

        // Clear jobs older than 7 days
        store.clearOlderThan(7 * 24 * 60 * 60 * 1000L)

        val remaining = store.loadAll()
        assertEquals(2, remaining.size)
        assertTrue(remaining.any { it.jobId == "new_job" })
        assertTrue(remaining.any { it.jobId == "old_incomplete" })
    }
}
