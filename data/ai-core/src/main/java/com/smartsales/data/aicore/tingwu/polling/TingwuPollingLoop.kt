// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/polling/TingwuPollingLoop.kt
// Module: :data:ai-core
// Summary: Polling loop extracted from TingwuRunner. Composes with TingwuRunnerRepository.
// Author: created on 2026-01-12

package com.smartsales.data.aicore.tingwu.polling

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.data.aicore.AiCoreConfig
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.tingwu.api.TingwuStatusData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import java.util.Optional
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Pure polling loop for Tingwu job status.
 * 
 * Responsibility: Poll → parse status → emit state
 * Does NOT handle: MetaHub updates, trace recording (caller's responsibility via callback)
 * 
 * V1 spec §8.1: Uses TingwuRunnerRepository.pollWithRetry() for retry policy compliance.
 */
@Singleton
class TingwuPollingLoop @Inject constructor(
    private val repository: TingwuRunnerRepository,
    optionalConfig: Optional<AiCoreConfig>,
    private val dispatchers: DispatcherProvider
) {
    private val config = optionalConfig.orElse(AiCoreConfig())

    /**
     * Poll until terminal state (Completed or Failed).
     * 
     * @param jobId Tingwu job ID
     * @param stateFlow Flow to emit progress/completion/failure states
     * @param onTerminal Callback when terminal state reached (for MetaHub, trace, cleanup)
     */
    suspend fun poll(
        jobId: String,
        stateFlow: MutableStateFlow<TingwuJobState>,
        onTerminal: suspend (TingwuJobState) -> Unit
    ) {
        val pollInterval = max(config.tingwuPollIntervalMillis, 500L)
        val perTaskTimeout = max(config.tingwuPollTimeoutMillis, pollInterval * 2)
        val globalTimeout = config.tingwuGlobalPollTimeoutMillis.takeIf { it > 0 }
        val effectiveTimeout = min(perTaskTimeout, globalTimeout ?: Long.MAX_VALUE)
        val initialDelay = max(config.tingwuInitialPollDelayMillis, 0L)
        
        val start = System.currentTimeMillis()
        
        if (initialDelay > 0) {
            delay(initialDelay)
        }
        
        AiCoreLogger.d(TAG, "Polling started: jobId=$jobId timeout=${effectiveTimeout}ms")
        
        try {
            while (currentCoroutineContext().isActive) {
                // Timeout check
                if (System.currentTimeMillis() - start > effectiveTimeout) {
                    val failed = TingwuJobState.Failed(
                        jobId = jobId,
                        error = AiCoreException(
                            source = AiCoreErrorSource.TINGWU,
                            reason = AiCoreErrorReason.TIMEOUT,
                            message = "Tingwu 轮询超时",
                            suggestion = "可调大 AiCoreConfig.tingwuPollTimeoutMillis"
                        )
                    )
                    stateFlow.value = failed
                    onTerminal(failed)
                    return
                }
                
                // Poll with V1 §8.1 retry policy (delegated to repository)
                val response = try {
                    repository.pollWithRetry(jobId)
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    val mapped = repository.mapError(error)
                    AiCoreLogger.e(TAG, "Polling failed (retries exhausted): ${mapped.message}", mapped)
                    val failed = TingwuJobState.Failed(jobId, mapped)
                    stateFlow.value = failed
                    onTerminal(failed)
                    return
                }
                
                // Parse status
                val data = response.data
                if (data == null) {
                    val failed = TingwuJobState.Failed(
                        jobId = jobId,
                        error = AiCoreException(
                            source = AiCoreErrorSource.TINGWU,
                            reason = AiCoreErrorReason.REMOTE,
                            message = "Tingwu 返回空数据: code=${response.code} message=${response.message}"
                        )
                    )
                    stateFlow.value = failed
                    onTerminal(failed)
                    return
                }
                
                val normalizedStatus = data.taskStatus?.uppercase(Locale.US) ?: "UNKNOWN"
                
                when (normalizedStatus) {
                    "FAILED", "ERROR" -> {
                        val failed = TingwuJobState.Failed(
                            jobId = jobId,
                            error = AiCoreException(
                                source = AiCoreErrorSource.TINGWU,
                                reason = AiCoreErrorReason.REMOTE,
                                message = data.errorMessage ?: "Tingwu 返回失败"
                            ),
                            errorCode = data.errorCode
                        )
                        stateFlow.value = failed
                        onTerminal(failed)
                        return
                    }
                    "SUCCEEDED", "COMPLETED", "FINISHED" -> {
                        AiCoreLogger.d(TAG, "Job completed: jobId=$jobId")
                        // Return completed state with result links for caller to fetch artifacts
                        val completed = TingwuJobState.Completed(
                            jobId = jobId,
                            transcriptMarkdown = "", // Caller will fetch and populate
                            artifacts = data.toArtifacts(),
                            statusLabel = normalizedStatus
                        )
                        stateFlow.value = completed
                        onTerminal(completed)
                        return
                    }
                    else -> {
                        // In progress
                        val progress = inferProgress(data)
                        stateFlow.value = TingwuJobState.InProgress(
                            jobId = jobId,
                            progressPercent = progress,
                            statusLabel = normalizedStatus,
                            artifacts = data.toArtifacts()
                        )
                        delay(pollInterval)
                    }
                }
            }
        } catch (e: CancellationException) {
            AiCoreLogger.d(TAG, "Polling cancelled: jobId=$jobId")
            throw e
        }
    }
    
    private fun inferProgress(status: TingwuStatusData): Int {
        val raw = status.taskProgress
        return when {
            raw != null && raw in 0..100 -> raw
            status.taskStatus?.uppercase(Locale.US) in listOf("RUNNING", "PROCESSING") -> 50
            else -> 25
        }
    }
    
    private fun TingwuStatusData.toArtifacts(): TingwuJobArtifacts? {
        val links = resultLinks ?: return null
        return TingwuJobArtifacts(
            transcriptionUrl = links["Transcription"],
            autoChaptersUrl = links["AutoChapters"],
            customPromptUrl = links["MeetingSummary"] ?: links["CustomPrompt"],
            extraResultUrls = links
        )
    }
    
    companion object {
        private const val TAG = "TingwuPollingLoop"
    }
}
