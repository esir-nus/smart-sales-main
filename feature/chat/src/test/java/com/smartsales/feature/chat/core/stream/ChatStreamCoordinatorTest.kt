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

    @Test
    fun `retry then accept uses last attempt`() = runTest(dispatcher) {
        // 验证重试框架按 maxRetries 触发，多次尝试后仅接受一次结果
        var streamCalls = 0
        val coordinator = ChatStreamCoordinator { _ ->
            val attempt = streamCalls
            streamCalls += 1
            flowOf(ChatStreamEvent.Completed("attempt-$attempt"))
        }
        val request = ChatRequest(sessionId = "s1", userMessage = "hello")
        val completed = mutableListOf<String>()
        val retries = mutableListOf<Int>()

        coordinator.start(
            scope = this,
            request = request,
            onDelta = { },
            onCompleted = { completed.add(it) },
            onError = { },
            maxRetries = 2,
            completionEvaluator = { _, attempt ->
                if (attempt < 2) CompletionDecision.Retry else CompletionDecision.Accept
            },
            onRetryStart = { retries.add(it) },
        )

        advanceUntilIdle()

        assertEquals(3, streamCalls)
        assertEquals(listOf(1, 2), retries)
        assertEquals(listOf("attempt-2"), completed)
    }

    @Test
    fun `always retry triggers terminal callback`() = runTest(dispatcher) {
        // 验证达到最大重试次数后进入终止分支
        var streamCalls = 0
        val coordinator = ChatStreamCoordinator { _ ->
            val attempt = streamCalls
            streamCalls += 1
            flowOf(ChatStreamEvent.Completed("attempt-$attempt"))
        }
        val request = ChatRequest(sessionId = "s1", userMessage = "hello")
        var terminal: Pair<Int, String>? = null

        coordinator.start(
            scope = this,
            request = request,
            onDelta = { },
            onCompleted = { },
            onError = { },
            maxRetries = 2,
            completionEvaluator = { _, _ -> CompletionDecision.Retry },
            onTerminal = { text, attempt -> terminal = attempt to text },
        )

        advanceUntilIdle()

        assertEquals(3, streamCalls)
        assertEquals(2 to "attempt-2", terminal)
    }

    @Test
    fun `request provider applies per attempt`() = runTest(dispatcher) {
        // 验证 requestProvider 可以为每次尝试生成不同请求
        val seen = mutableListOf<String>()
        val coordinator = ChatStreamCoordinator { req ->
            seen.add(req.userMessage)
            flowOf(ChatStreamEvent.Completed(req.userMessage))
        }
        val base = ChatRequest(sessionId = "s1", userMessage = "msg-0")
        val completed = mutableListOf<String>()

        coordinator.start(
            scope = this,
            request = base,
            onDelta = { },
            onCompleted = { completed.add(it) },
            onError = { },
            maxRetries = 2,
            completionEvaluator = { _, attempt ->
                if (attempt < 2) CompletionDecision.Retry else CompletionDecision.Accept
            },
            requestProvider = { attempt -> base.copy(userMessage = "msg-$attempt") },
        )

        advanceUntilIdle()

        assertEquals(listOf("msg-0", "msg-1", "msg-2"), seen)
        assertEquals(listOf("msg-2"), completed)
    }
}
