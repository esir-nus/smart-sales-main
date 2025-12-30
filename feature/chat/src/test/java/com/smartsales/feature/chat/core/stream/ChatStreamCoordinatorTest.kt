package com.smartsales.feature.chat.core.stream

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/stream/ChatStreamCoordinatorTest.kt
// Module: :feature:chat
// Summary: Unit tests for ChatStreamCoordinator event routing.
// Author: created on 2025-12-30

import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatStreamEvent
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatStreamCoordinatorTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun `routes delta and completed events`() = runTest(dispatcher) {
        // 验证流事件能按顺序回调到外部处理器
        val coordinator = ChatStreamCoordinator { _ ->
            flowOf(
                ChatStreamEvent.Delta("Hi"),
                ChatStreamEvent.Completed("Hi there"),
            )
        }
        val request = ChatRequest(sessionId = "s1", userMessage = "hello")
        val deltas = mutableListOf<String>()
        var completed: String? = null
        var error: Throwable? = null

        coordinator.start(
            scope = this,
            request = request,
            onDelta = { deltas.add(it) },
            onCompleted = { completed = it },
            onError = { error = it },
        )

        advanceUntilIdle()

        assertEquals(listOf("Hi"), deltas)
        assertEquals("Hi there", completed)
        assertEquals(null, error)
    }

    @Test
    fun `routes error event`() = runTest(dispatcher) {
        // 错误应直接透传给调用方，避免吞掉异常
        val expected = IllegalStateException("boom")
        val coordinator = ChatStreamCoordinator { _ ->
            flowOf(ChatStreamEvent.Error(expected))
        }
        val request = ChatRequest(sessionId = "s1", userMessage = "hello")
        var error: Throwable? = null

        coordinator.start(
            scope = this,
            request = request,
            onDelta = { },
            onCompleted = { },
            onError = { error = it },
        )

        advanceUntilIdle()

        assertSame(expected, error)
    }
}
