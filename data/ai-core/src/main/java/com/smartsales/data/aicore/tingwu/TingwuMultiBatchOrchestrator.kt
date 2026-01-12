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
import com.smartsales.data.aicore.util.AudioSlicer
import com.smartsales.data.aicore.util.SliceOutcome
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates multi-batch transcription logic (Slice -> Upload -> Submit Loop).
 *
 * This logic is extracted from TingwuRunner to prevent "God Class" growth.
 * It manages the sequential execution of batches as per Orchestrator-V1.
 */
@Singleton
class TingwuMultiBatchOrchestrator @Inject constructor(
    private val audioSlicer: AudioSlicer,
    private val ossUploadClient: OssUploadClient,
    private val stitcher: MultiBatchStitcher,
    private val pipelineTracer: com.smartsales.data.aicore.debug.PipelineTracer
) {

    /**
     * Executes the multi-batch loop.
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
        val batchResults = mutableListOf<Pair<com.smartsales.data.aicore.disector.DisectorBatch, List<com.smartsales.data.aicore.DiarizedSegment>>>()
        
        // Sequential Loop
        for ((index, batch) in plan.batches.withIndex()) {
            val progressBase = (index * 100) / totalBatches
            onProgress(progressBase)
            
            pipelineTracer.emit(
                stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
                status = "BATCH_STARTED",
                message = "batch=${index + 1}/$totalBatches batchId=${batch.batchAssetId}"
            )
            com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Batch ${index + 1}/$totalBatches started: batchId=${batch.batchAssetId} range=[${batch.captureStartMs}-${batch.captureEndMs}ms]")

            // 1. Slice
            val sliceOutcome = audioSlicer.sliceAudio(
                source = sourceFile,
                requestedCaptureStartMs = batch.captureStartMs,
                captureEndMs = batch.captureEndMs,
                windowKey = batch.batchAssetId
            )
            
            val sliceFile = when (sliceOutcome) {
                is SliceOutcome.Success -> sliceOutcome.result.sliceFile
                is SliceOutcome.Failure -> return@coroutineScope Result.Error(
                    AiCoreException(
                        source = AiCoreErrorSource.TINGWU,
                        reason = AiCoreErrorReason.IO,
                        message = "Audio slicing failed for batch ${batch.batchIndex}: ${sliceOutcome.error.reasonCode}"
                    )
                )
            }

            // 2. Upload
            val ossKey = "${plan.audioAssetId}/${batch.batchAssetId}.m4a"
            val uploadResult = ossUploadClient.uploadAudio(
                OssUploadRequest(file = sliceFile, objectKey = ossKey)
            )
            val ossData = when (uploadResult) {
                is Result.Success -> uploadResult.data
                is Result.Error -> return@coroutineScope Result.Error(uploadResult.throwable)
            }

            // 3. Submit
            val batchRequest = originalRequest.copy(
                ossObjectKey = ossData.objectKey,
                fileUrl = ossData.presignedUrl,
                audioFilePath = null, // Clear local path for batch
                durationMs = null // Let Tingwu detect slice duration
            )
            
            val submission = submitSingleBatch(batchRequest)
            val jobId = when (submission) {
                is Result.Success -> submission.data
                is Result.Error -> return@coroutineScope Result.Error(submission.throwable)
            }

            // 4. Wait for completion and collect segments (Option C)
            val finalState = waitForJob(jobId)
            when (finalState) {
                is TingwuJobState.Failed -> {
                    com.smartsales.data.aicore.AiCoreLogger.e(TAG, "Batch ${index + 1}/$totalBatches FAILED: jobId=$jobId error=${finalState.error.message}")
                    return@coroutineScope Result.Error(finalState.error)
                }
                is TingwuJobState.Completed -> {
                    val segments = finalState.artifacts?.recordingOriginDiarizedSegments.orEmpty()
                    batchResults.add(batch to segments)
                    pipelineTracer.emit(
                        stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
                        status = "BATCH_COMPLETED",
                        message = "batch=${index + 1}/$totalBatches jobId=$jobId segments=${segments.size}"
                    )
                    com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Batch ${index + 1}/$totalBatches completed: jobId=$jobId segments=${segments.size}")
                }
                else -> {
                    com.smartsales.data.aicore.AiCoreLogger.w(TAG, "Batch ${index + 1}/$totalBatches unexpected state after waitForJob: $finalState")
                }
            }
            
            // Cleanup transient slice
            runCatching { sliceFile.delete() }
        }
        
        onProgress(100)
        
        // 5. Stitch all segments (Option C)
        val stitchedSegments = stitcher.stitchSegments(batchResults)
        
        // Return success with parent ID; surface stitched segments via callback
        val parentJobId = "multi_${plan.disectorPlanId}"
        
        // Invoke callback safely (don't let callback failure poison the loop)
        runCatching { onComplete(parentJobId, stitchedSegments) }
            .onFailure { com.smartsales.data.aicore.AiCoreLogger.w(TAG, "onComplete callback failed: ${it.message}") }
        
        pipelineTracer.emit(
            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
            status = "MULTI_BATCH_COMPLETE",
            message = "planId=${plan.disectorPlanId} batches=$totalBatches stitchedSegments=${stitchedSegments.size}"
        )
        com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Multi-batch complete: planId=${plan.disectorPlanId} batches=$totalBatches stitchedSegments=${stitchedSegments.size}")
        Result.Success(parentJobId)
    }

    companion object {
        private const val TAG = "MultiBatchOrchestrator"
    }
}
