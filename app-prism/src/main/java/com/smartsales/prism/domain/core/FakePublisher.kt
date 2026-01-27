package com.smartsales.prism.domain.core

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Publisher — 直接转换为 UiState
 * Phase 1 占位实现
 */
@Singleton
class FakePublisher @Inject constructor() : Publisher {
    
    override suspend fun publish(result: ExecutorResult, mode: Mode): UiState {
        return when (result) {
            is ExecutorResult.Success -> UiState.Response(result.content)
            is ExecutorResult.Failure -> UiState.Error(
                message = result.error,
                retryable = result.retryable
            )
        }
    }
}
