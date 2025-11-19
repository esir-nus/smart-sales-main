package com.smartsales.data.aicore.tingwu

import com.smartsales.data.aicore.AiCoreLogger
import okhttp3.Call
import okhttp3.EventListener

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuOkHttpEventListener.kt
// 模块：:data:ai-core
// 说明：用于输出 OkHttp 链路监控日志，便于官方排障
// 作者：创建于 2025-11-18
class TingwuOkHttpEventListener : EventListener() {
    override fun dnsStart(call: Call, domainName: String) {
        AiCoreLogger.d(TAG, "DNS start: $domainName")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
        AiCoreLogger.d(TAG, "DNS end: $domainName -> ${inetAddressList.joinToString { it.hostAddress ?: "-" }}")
    }

    override fun connectStart(call: Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) {
        AiCoreLogger.d(TAG, "Connect start: ${inetSocketAddress.hostName}:${inetSocketAddress.port}")
    }

    override fun connectFailed(
        call: Call,
        inetSocketAddress: java.net.InetSocketAddress,
        proxy: java.net.Proxy,
        protocol: okhttp3.Protocol?,
        ioe: java.io.IOException
    ) {
        AiCoreLogger.e(TAG, "Connect failed: ${inetSocketAddress.hostName}:${inetSocketAddress.port} ${ioe.message}")
    }

    override fun responseHeadersEnd(call: Call, response: okhttp3.Response) {
        AiCoreLogger.d(TAG, "Response received: code=${response.code} url=${response.request.url}")
    }

    override fun callFailed(call: Call, ioe: java.io.IOException) {
        AiCoreLogger.e(TAG, "Call failed：${ioe.message}", ioe)
    }

    companion object {
        private const val TAG = "AiCore/OkHttp"
    }

    class Factory : EventListener.Factory {
        override fun create(call: Call): EventListener = TingwuOkHttpEventListener()
    }
}
