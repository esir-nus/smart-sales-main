package com.smartsales.core.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// 文件路径: core/util/src/main/java/com/smartsales/core/util/DispatcherProvider.kt
// 文件作用: 暴露协程分发器, 便于测试替换
// 文件作者: Codex
// 最近修改: 2025-02-14
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
}

object DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val default: CoroutineDispatcher = Dispatchers.Default
}
