package com.smartsales.feature.connectivity

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/HttpEndpointChecker.kt
// 模块：:feature:connectivity
// 说明：对设备 HTTP BaseUrl 进行健康探测，避免将“BLE 连通”误判为已连接
// 作者：创建于 2025-11-22

import java.net.HttpURLConnection
import java.net.URL

interface HttpEndpointChecker {
    suspend fun isReachable(baseUrl: String): Boolean
}

class DefaultHttpEndpointChecker : HttpEndpointChecker {
    override suspend fun isReachable(baseUrl: String): Boolean {
        return runCatching {
            val url = URL(baseUrl)
            (url.openConnection() as HttpURLConnection).use { conn ->
                conn.requestMethod = "HEAD"
                conn.connectTimeout = 4_000
                conn.readTimeout = 4_000
                conn.connect()
                val code = conn.responseCode
                code in 200..399
            }
        }.getOrElse { false }
    }
}
