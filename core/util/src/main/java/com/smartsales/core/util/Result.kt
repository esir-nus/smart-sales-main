package com.smartsales.core.util

// 文件路径: core/util/src/main/java/com/smartsales/core/util/Result.kt
// 文件作用: 提供跨模块通用的结果封装
// 文件作者: Codex
// 最近修改: 2025-02-14
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val throwable: Throwable) : Result<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
}
