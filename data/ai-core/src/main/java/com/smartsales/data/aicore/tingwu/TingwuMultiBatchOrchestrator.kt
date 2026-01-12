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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

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
        com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Starting PARALLEL multi-batch: ${totalBatches} batches")
        
        // ========== PHASE 1: Slice all batches (sequential for file I/O) ==========
        onProgress(5)
        val sliceResults = mutableListOf<Pair<DisectorBatch, File>>()
        
        for ((index, batch) in plan.batches.withIndex()) {
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
                is SliceOutcome.Failure -> return@coroutineScope Result.Error(
                    AiCoreException(
                        source = AiCoreErrorSource.TINGWU,
                        reason = AiCoreErrorReason.IO,
                        message = "Audio slicing failed for batch ${batch.batchIndex}: ${sliceOutcome.error.reasonCode}"
                    )
                )
            }
            sliceResults.add(batch to sliceFile)
        }
        
        onProgress(20)
        com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Slicing complete: ${sliceResults.size} slices")
        
        // ========== PHASE 2: Upload + Submit all batches (PARALLEL) ==========
        pipelineTracer.emit(
            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_UPLOAD,
            status = "PARALLEL_UPLOAD_START",
            message = "batches=$totalBatches"
        )
        
        val submissionJobs = sliceResults.mapIndexed { index, (batch, sliceFile) ->
            async {
                pipelineTracer.emit(
                    stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
                    status = "BATCH_STARTED",
                    message = "batch=${index + 1}/$totalBatches batchId=${batch.batchAssetId}"
                )
                
                // Upload
                val ossKey = "${plan.audioAssetId}/${batch.batchAssetId}.m4a"
                val uploadResult = ossUploadClient.uploadAudio(
                    OssUploadRequest(file = sliceFile, objectKey = ossKey)
                )
                val ossData = when (uploadResult) {
                    is Result.Success -> uploadResult.data
                    is Result.Error -> return@async Triple(batch, null as String?, uploadResult.throwable)
                }
                
                // Submit
                val batchRequest = originalRequest.copy(
                    ossObjectKey = ossData.objectKey,
                    fileUrl = ossData.presignedUrl,
                    audioFilePath = null,
                    durationMs = null
                )
                
                val submission = submitSingleBatch(batchRequest)
                val jobId = when (submission) {
                    is Result.Success -> submission.data
                    is Result.Error -> return@async Triple(batch, null as String?, submission.throwable)
                }
                
                // Cleanup slice file after upload
                runCatching { sliceFile.delete() }
                
                Triple(batch, jobId, null as Throwable?)
            }
        }
        
        val submissionResults = submissionJobs.awaitAll()
        
        // Check for any upload/submit failures
        val firstError = submissionResults.firstOrNull { it.third != null }
        if (firstError != null) {
            return@coroutineScope Result.Error(firstError.third!!)
        }
        
        onProgress(50)
        com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Upload+Submit complete: ${submissionResults.size} jobs submitted")
        
        // ========== PHASE 3: Poll all jobs (PARALLEL) ==========
        pipelineTracer.emit(
            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
            status = "PARALLEL_POLL_START",
            message = "jobs=${submissionResults.size}"
        )
        
        val pollingJobs = submissionResults.map { (batch, jobId, _) ->
            async {
                val finalState = waitForJob(jobId!!)
                batch to finalState
            }
        }
        
        val pollResults = pollingJobs.awaitAll()
        
        onProgress(90)
        
        // ========== PHASE 4: Collect results and stitch ==========
        val batchResults = mutableListOf<Pair<DisectorBatch, List<com.smartsales.data.aicore.DiarizedSegment>>>()
        
        for ((index, result) in pollResults.withIndex()) {
            val (batch, finalState) = result
            when (finalState) {
                is TingwuJobState.Failed -> {
                    com.smartsales.data.aicore.AiCoreLogger.e(TAG, "Batch ${index + 1}/$totalBatches FAILED: ${finalState.error.message}")
                    return@coroutineScope Result.Error(finalState.error)
                }
                is TingwuJobState.Completed -> {
                    val segments = finalState.artifacts?.recordingOriginDiarizedSegments.orEmpty()
                    batchResults.add(batch to segments)
                    pipelineTracer.emit(
                        stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
                        status = "BATCH_COMPLETED",
                        message = "batch=${index + 1}/$totalBatches jobId=${finalState.jobId} segments=${segments.size}"
                    )
                    com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Batch ${index + 1}/$totalBatches completed: segments=${segments.size}")
                }
                else -> {
                    com.smartsales.data.aicore.AiCoreLogger.w(TAG, "Batch ${index + 1}/$totalBatches unexpected state: $finalState")
                }
            }
        }
        
        onProgress(100)
        
        // Stitch all segments
        val stitchedSegments = stitcher.stitchSegments(batchResults)
        
        val parentJobId = "multi_${plan.disectorPlanId}"
        
        // Invoke callback safely
        runCatching { onComplete(parentJobId, stitchedSegments) }
            .onFailure { com.smartsales.data.aicore.AiCoreLogger.w(TAG, "onComplete callback failed: ${it.message}") }
        
        pipelineTracer.emit(
            stage = com.smartsales.data.aicore.debug.PipelineStage.TINGWU_POLL,
            status = "MULTI_BATCH_COMPLETE",
            message = "planId=${plan.disectorPlanId} batches=$totalBatches stitchedSegments=${stitchedSegments.size}"
        )
        com.smartsales.data.aicore.AiCoreLogger.d(TAG, "Multi-batch PARALLEL complete: batches=$totalBatches stitchedSegments=${stitchedSegments.size}")
        Result.Success(parentJobId)
    }

    companion object {
        private const val TAG = "MultiBatchOrchestrator"
    }
}
