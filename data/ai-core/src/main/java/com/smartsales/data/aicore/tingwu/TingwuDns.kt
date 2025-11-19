package com.smartsales.data.aicore.tingwu

import com.smartsales.data.aicore.AiCoreLogger
import com.smartsales.data.aicore.HttpDnsResolver
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import okhttp3.Dns

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuDns.kt
// 模块：:data:ai-core
// 说明：包装 HTTPDNS 解析逻辑，兼容官方防劫持建议
// 作者：创建于 2025-11-18
class TingwuDns(
    private val resolver: HttpDnsResolver
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        try {
            val result = resolver.lookup(hostname)
            if (result.isEmpty()) {
                throw UnknownHostException("HTTPDNS empty result for $hostname")
            }
            return result
        } catch (io: IOException) {
            AiCoreLogger.e(TAG, "HTTPDNS 解析失败：${io.message}")
            throw UnknownHostException("HTTPDNS IO failure for $hostname").apply { initCause(io) }
        } catch (error: Throwable) {
            throw UnknownHostException("HTTPDNS unexpected error for $hostname").apply { initCause(error) }
        }
    }

    companion object {
        private const val TAG = "AiCore/HttpDns"
    }
}
