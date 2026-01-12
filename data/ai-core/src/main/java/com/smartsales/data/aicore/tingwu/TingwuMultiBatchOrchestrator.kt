package com.smartsales.data.aicore.tingwu

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.OssUploadClient
import com.smartsales.data.aicore.OssUploadRequest
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.data.aicore.disector.DisectorPlan
import com.smartsales.data.aicore.disector.DisectorBatch
import com.smartsales.data.aicore.util.AudioSlicer
import com.smartsales.data.aicore.util.SliceOutcome
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicInteger

/**
 * Orchestrates multi-batch transcription logic (Slice -> Upload -> Submit Loop).
 *
 * This logic is extracted from TingwuRunner to prevent "God Class" growth.
 * Batches are processed in PARALLEL for optimal performance.
 */
@Singleton
class TingwuMultiBatchOrchestrator @Inject constructor(
    private val audioSlicer: AudioSlicer,
    private val ossUploadClient: OssUploadClient,
    private val stitcher: MultiBatchStitcher,
    private val pipelineTracer: com.smartsales.data.aicore.debug.PipelineTracer
) {

    /**
     * Executes the multi-batch loop with PARALLEL processing.
     *
     * Phase 1: Slice all batches (sequential - file I/O bound)
     * Phase 2: Upload + Submit all batches (parallel)
     * Phase 3: Poll all jobs (parallel)
     * Phase 4: Stitch results
     *
     * @param submitSingleBatch Callback to submit a single batch request (delegated to TingwuRunner).
     * @param onProgress Callback for overall progress updates (0-100).
     * @param onComplete Callback for completion with stitched result (parentJobId, stitchedSegments).
     */
    suspend fun executeBatchLoop(
        plan: DisectorPlan,
        originalRequest: TingwuRequest,
        sourceFile: File,
        submitSingleBatch: suspend (TingwuRequest) -> Result<String>,
        waitForJob: suspend (String) -> TingwuJobState,
        onProgress: (Int) -> Unit,
        onComplete: (String, List<com.smartsales.data.aicore.DiarizedSegment>) -> Unit = { _, _ -> }
    ): Result<String> = coroutineScope {
        // Validation
        if (!sourceFile.exists()) {
             return@coroutineScope Result.Error(
                AiCoreException(
                    source = AiCoreErrorSource.TINGWU,
                    reason = AiCoreErrorReason.IO,
                    message = "Multi-batch source file not found: ${sourceFile.absolutePath}"
                )
            )
        }

        val totalBatches = plan.batches.size
        com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Starting PARALLEL multi-batch (TRUE PIPELINE): ${totalBatches} batches")
        
        // Progress tracking: 15% Slicing, 30% Upload, 45% Polling (Total 90% + 10% Setup/Finish)
        // We use AtomicInteger to safely update progress from concurrent coroutines
        val progressCounter = AtomicInteger(5) // Start at 5%
        val sliceWeight = 15.0 / totalBatches
        val uploadWeight = 30.0 / totalBatches
        val pollWeight = 45.0 / totalBatches
        
        onProgress(5)

        // Single pipelined job per batch (Slice -> Upload -> Submit -> Poll)
        val batchJobs = plan.batches.mapIndexed { index, batch ->
            async(Dispatchers.Default) {
                // 1. Slice
                pipelineTracer.emit(
                    stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
                    status = "BATCH_SLICING",
                    message = "batch=${index + 1}/$totalBatches batchId=${batch.batchAssetId}"
                )
                
                val sliceOutcome = audioSlicer.sliceAudio(
                    source = sourceFile,
                    requestedCaptureStartMs = batch.captureStartMs,
                    captureEndMs = batch.captureEndMs,
                    windowKey = batch.batchAssetId
                )
                
                val sliceFile = when (sliceOutcome) {
                    is SliceOutcome.Success -> sliceOutcome.result.sliceFile
                    is SliceOutcome.Failure -> return@async Result.Error(
                        AiCoreException(
                            source = AiCoreErrorSource.TINGWU,
                            reason = AiCoreErrorReason.IO,
                            message = "Audio slicing failed for batch ${batch.batchIndex}: ${sliceOutcome.error.reasonCode}"
                        )
                    )
                }
                
                // Update progress after slice
                onProgress(progressCounter.addAndGet(sliceWeight.toInt()))

                // 2. Upload
                pipelineTracer.emit(
                    stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
                    status = "BATCH_STARTED", // Semantically "Batch Processing Started" (Upload+Submit phase)
                    message = "batch=${index + 1}/$totalBatches batchId=${batch.batchAssetId}"
                )

                val ossKey = "${plan.audioAssetId}/${batch.batchAssetId}.m4a"
                val uploadResult = ossUploadClient.uploadAudio(
                    OssUploadRequest(file = sliceFile, objectKey = ossKey)
                )
                
                val ossData = when (uploadResult) {
                    is Result.Success -> uploadResult.data
                    is Result.Error -> return@async Result.Error(uploadResult.throwable)
                }
                
                onProgress(progressCounter.addAndGet(uploadWeight.toInt()))

                // 3. Submit
                // Note: We delete the slice file regardless of submission outcome to prevent leak,
                // but we do it after submission just to be safe (though submission only needs URL).
                // Actually, submission needs URL, file is on OSS. We can delete local file now or later.
                // Let's delete later in finally block or runCatching if we want to be safe.
                // For now, follow previous logic: clean up after usage.
                
                val batchRequest = originalRequest.copy(
                    ossObjectKey = ossData.objectKey,
                    fileUrl = ossData.presignedUrl,
                    audioFilePath = null,
                    durationMs = null
                )
                
                val submission = submitSingleBatch(batchRequest)
                val jobId = when (submission) {
                    is Result.Success -> submission.data
                    is Result.Error -> {
                        sliceFile.delete() // Cleanup
                        return@async Result.Error(submission.throwable)
                    }
                }
                
                // Cleanup local slice execution
                runCatching { sliceFile.delete() }

                // 4. Poll
                val finalState = waitForJob(jobId)
                
                onProgress(progressCounter.addAndGet(pollWeight.toInt()))

                when (finalState) {
                    is TingwuJobState.Completed -> {
                        val segments = finalState.artifacts?.recordingOriginDiarizedSegments.orEmpty()
                        pipelineTracer.emit(
                            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
                            status = "BATCH_COMPLETED",
                            message = "batch=${index + 1}/$totalBatches jobId=${finalState.jobId} segments=${segments.size}"
                        )
                        com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Batch ${index + 1}/$totalBatches completed: segments=${segments.size}")
                        Result.Success(batch to segments)
                    }
                    is TingwuJobState.Failed -> {
                        com.smartsales.data.aicore.AiCoreLogger.e(TAG, "Batch ${index + 1}/$totalBatches FAILED: ${finalState.error.message}")
                        Result.Error(finalState.error)
                    }
                    else -> {
                        // Should be terminal state by now
                        Result.Error(AiCoreException(
                            source = AiCoreErrorSource.TINGWU,
                            reason = AiCoreErrorReason.UNKNOWN,
                            message = "Batch ${index + 1} ended in non-terminal state: $finalState"
                        ))
                    }
                }
            }
        }
        
        // Wait for all pipelines to complete
        val results = batchJobs.awaitAll()
        
        // Check for failures
        val firstError = results.firstOrNull { it is Result.Error }
        if (firstError is Result.Error) {
            return@coroutineScope Result.Error(firstError.throwable)
        }
        
        // Collect successful results
        val successResults = results.mapNotNull { (it as? Result.Success)?.data }
        
        // Sort by batch index to ensure stitching order
        val sortedResults = successResults.sortedBy { it.first.batchIndex }
        
        onProgress(95)
        
        // Stitch
        val stitchedSegments = stitcher.stitchSegments(sortedResults)
        
        val parentJobId = "multi_${plan.disectorPlanId}"
        
        runCatching { onComplete(parentJobId, stitchedSegments) }
            .onFailure { com.smartsales.data.aicore.AiCoreLogger.w(TAG, "onComplete callback failed: ${it.message}") }
            
        pipelineTracer.emit(
            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
            status = "MULTI_BATCH_COMPLETE",
            message = "planId=${plan.disectorPlanId} batches=$totalBatches stitchedSegments=${stitchedSegments.size}"
        )
        onProgress(100)
        
        com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Multi-batch TRUE PIPELINE complete: batches=$totalBatches stitchedSegments=${stitchedSegments.size}")
        Result.Success(parentJobId)
    }

    companion object {
        private const val TAG = "MultiBatchOrchestrator"
    }
}
