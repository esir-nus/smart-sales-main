// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreLogger.kt
// 模块：:data:ai-core
// 说明：提供模块内部日志工具，兼容 JVM 单元测试
// 作者：创建于 2025-11-16
package com.smartsales.data.aicore

internal object AiCoreLogger {
    fun v(tag: String, message: String) {
        if (!tryAndroidLog { android.util.Log.v(tag, message) }) {
            println("[$tag][verbose] $message")
        }
    }

    fun d(tag: String, message: String) {
        if (!tryAndroidLog { android.util.Log.d(tag, message) }) {
            println("[$tag][debug] $message")
        }
    }

    fun w(tag: String, message: String) {
        if (!tryAndroidLog { android.util.Log.w(tag, message) }) {
            println("[$tag][warn] $message")
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (!tryAndroidLog { android.util.Log.e(tag, message, throwable) }) {
            println("[$tag][error] $message ${throwable?.message ?: ""}")
        }
    }

    private inline fun tryAndroidLog(block: () -> Unit): Boolean =
        runCatching { block() }
            .onFailure { println("[AiCore][log-fallback] ${it.message}") }
            .isSuccess
}
