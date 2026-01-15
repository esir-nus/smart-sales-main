// File: data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/polling/TingwuApiRepository.kt
// Module: :data:ai-core
// Summary: Lattice interface for Tingwu API utilities (validation, polling, error mapping)
// Author: created on 2026-01-15

package com.smartsales.data.aicore.tingwu.polling

import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.AiCoreErrorReason
import com.smartsales.data.aicore.AiCoreErrorSource
import com.smartsales.data.aicore.TingwuCredentials
import com.smartsales.data.aicore.TingwuRequest
import com.smartsales.data.aicore.tingwu.api.TingwuStatusResponse

/**
 * TingwuApiRepository: Lattice interface for Tingwu API utilities.
 * 
 * Responsibility: Validation, polling with retry, error mapping, URL resolution.
 */
interface TingwuApiRepository {
    /** Poll job status with retry policy per V1 spec §8.1. */
    suspend fun pollWithRetry(jobId: String): TingwuStatusResponse
    
    /** Validate credentials, return error or null if valid. */
    fun validateCredentials(credentials: TingwuCredentials): AiCoreException?
    
    /** Resolve file URL from request (direct URL or generate signed URL). */
    suspend fun resolveFileUrl(request: TingwuRequest): Result<String>
    
    /** Build deterministic task key from request. */
    fun buildTaskKey(request: TingwuRequest): String
    
    /** Map language code to Tingwu source language. */
    fun mapSourceLanguage(language: String): String
    
    /** Check if error is retryable (network, 5xx, 429). */
    fun isRetryableError(error: Throwable): Boolean
    
    /** Map throwable to AiCoreException. */
    fun mapError(error: Throwable): AiCoreException
}

/**
 * FakeTingwuApiRepository: Fake implementation for testing.
 */
class FakeTingwuApiRepository : TingwuApiRepository {
    var stubPollResponse: TingwuStatusResponse? = null
    var stubPollError: Throwable? = null
    var stubValidationError: AiCoreException? = null
    var stubFileUrl: Result<String> = Result.Success("https://fake.oss/audio.wav")
    var stubTaskKey: String = "fake_task_key"
    var stubLanguage: String = "cn"
    
    val pollCalls = mutableListOf<String>()
    
    override suspend fun pollWithRetry(jobId: String): TingwuStatusResponse {
        pollCalls.add(jobId)
        stubPollError?.let { throw it }
        return stubPollResponse ?: throw NotImplementedError("Stub pollWithRetry response")
    }
    
    override fun validateCredentials(credentials: TingwuCredentials): AiCoreException? {
        return stubValidationError
    }
    
    override suspend fun resolveFileUrl(request: TingwuRequest): Result<String> {
        return stubFileUrl
    }
    
    override fun buildTaskKey(request: TingwuRequest): String {
        return stubTaskKey
    }
    
    override fun mapSourceLanguage(language: String): String {
        return stubLanguage
    }
    
    override fun isRetryableError(error: Throwable): Boolean = false
    
    override fun mapError(error: Throwable): AiCoreException = AiCoreException(
        source = AiCoreErrorSource.TINGWU,
        reason = AiCoreErrorReason.UNKNOWN,
        message = error.message ?: "Fake error"
    )
    
    fun reset() {
        stubPollResponse = null
        stubPollError = null
        stubValidationError = null
        stubFileUrl = Result.Success("https://fake.oss/audio.wav")
        stubTaskKey = "fake_task_key"
        stubLanguage = "cn"
        pollCalls.clear()
    }
}
