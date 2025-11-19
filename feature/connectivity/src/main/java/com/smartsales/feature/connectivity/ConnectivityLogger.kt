package com.smartsales.feature.connectivity

import android.util.Log

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/ConnectivityLogger.kt
// 说明：提供安全的日志包装，JVM 单测环境下自动降级到 println
internal object ConnectivityLogger {
    private const val TAG = "SmartSalesConn"

    fun d(message: String) = log(Log.DEBUG, message, null)
    fun i(message: String) = log(Log.INFO, message, null)
    fun w(message: String, throwable: Throwable? = null) = log(Log.WARN, message, throwable)

    fun tag(): String = TAG

    private fun log(priority: Int, message: String, throwable: Throwable?) {
        try {
            when (priority) {
                Log.INFO -> Log.i(TAG, message, throwable)
                Log.WARN -> Log.w(TAG, message, throwable)
                else -> Log.d(TAG, message, throwable)
            }
        } catch (_: Throwable) {
            val level = when (priority) {
                Log.INFO -> "I"
                Log.WARN -> "W"
                else -> "D"
            }
            if (throwable != null) {
                println("$level/$TAG: $message\n${throwable.message}")
            } else {
                println("$level/$TAG: $message")
            }
        }
    }
}
