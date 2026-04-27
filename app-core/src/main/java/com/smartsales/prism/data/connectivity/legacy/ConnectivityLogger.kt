package com.smartsales.prism.data.connectivity.legacy

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/ConnectivityLogger.kt
// 说明：提供安全的日志包装，JVM 单测环境下自动降级到 println
//       增加 BLE TX/RX 事件流用于调试 HUD
internal object ConnectivityLogger {
    private const val TAG = "SmartSalesConn"

    // BLE traffic events for debug HUD
    private val _bleTrafficEvents = MutableSharedFlow<BleTrafficEvent>(replay = 50, extraBufferCapacity = 50)
    val bleTrafficEvents: SharedFlow<BleTrafficEvent> = _bleTrafficEvents.asSharedFlow()

    fun d(message: String) = log(Log.DEBUG, message, null)
    fun i(message: String) = log(Log.INFO, message, null)
    fun w(message: String, throwable: Throwable? = null) = log(Log.WARN, message, throwable)

    fun tag(): String = TAG

    /** Log TX (app → device) with raw bytes */
    fun tx(label: String, data: ByteArray) {
        val rawHex = data.joinToString(" ") { "%02X".format(it) }
        val rawText = runCatching { data.decodeToString() }.getOrDefault("<binary>")
        val logged = redactTxPayload(label, rawText, rawHex)
        val event = BleTrafficEvent(
            direction = BleTrafficDirection.TX,
            label = label,
            rawHex = logged.hex,
            rawText = logged.text,
            timestampMs = System.currentTimeMillis()
        )
        _bleTrafficEvents.tryEmit(event)
        d("📤 TX [$label]: ${logged.text} (${logged.hex})")
    }

    /** Log RX (device → app) with raw bytes */
    fun rx(label: String, data: ByteArray) {
        val hex = data.joinToString(" ") { "%02X".format(it) }
        val text = runCatching { data.decodeToString() }.getOrDefault("<binary>")
        val event = BleTrafficEvent(
            direction = BleTrafficDirection.RX,
            label = label,
            rawHex = hex,
            rawText = text,
            timestampMs = System.currentTimeMillis()
        )
        _bleTrafficEvents.tryEmit(event)
        d("📥 RX [$label]: $text ($hex)")
    }

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

    private fun redactTxPayload(label: String, text: String, hex: String): LoggedPayload {
        val isPassword = label.equals("Password", ignoreCase = true) || text.startsWith("PD#")
        return if (isPassword) {
            LoggedPayload(text = "PD#****", hex = "<redacted>")
        } else {
            LoggedPayload(text = text, hex = hex)
        }
    }

    private data class LoggedPayload(
        val text: String,
        val hex: String
    )
}

/** Public accessor for BLE traffic events (for debug HUD) */
object BleTrafficObserver {
    val events: SharedFlow<BleTrafficEvent> get() = ConnectivityLogger.bleTrafficEvents
}

enum class BleTrafficDirection { TX, RX }

data class BleTrafficEvent(
    val direction: BleTrafficDirection,
    val label: String,
    val rawHex: String,
    val rawText: String,
    val timestampMs: Long
) {
    fun formatForHud(): String {
        val dir = if (direction == BleTrafficDirection.TX) "→" else "←"
        val time = (timestampMs % 100000).toString().padStart(5, '0')
        return "[$time] $dir $label: $rawText"
    }
    
    fun formatVerbose(): String {
        val dir = if (direction == BleTrafficDirection.TX) "TX →" else "RX ←"
        val time = (timestampMs % 100000).toString().padStart(5, '0')
        return "[$time] $dir $label\n  Text: $rawText\n  Hex:  $rawHex"
    }
}
