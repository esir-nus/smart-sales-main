package com.smartsales.core.test

import com.smartsales.core.util.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher

// 文件路径: core/test/src/main/java/com/smartsales/core/test/FakeDispatcherProvider.kt
// 文件作用: 在单测中提供自定义的DispatcherProvider
// 文件作者: Codex
// 最近修改: 2025-02-14
class FakeDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}
