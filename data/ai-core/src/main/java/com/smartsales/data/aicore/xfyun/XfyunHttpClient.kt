// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunHttpClient.kt
// 模块：:data:ai-core
// 说明：提供讯飞 ASR 的 OkHttpClient（支持文件流上传）
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Singleton
class XfyunHttpClient @Inject constructor() {

    val client: OkHttpClient = OkHttpClient.Builder()
        // 上传文件可能较大，写超时放宽；轮询读取则保持较小超时，避免卡死。
        .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private companion object {
        private const val CONNECT_TIMEOUT_MS = 20_000L
        private const val READ_TIMEOUT_MS = 30_000L
        private const val WRITE_TIMEOUT_MS = 120_000L
    }
}

