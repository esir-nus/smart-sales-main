package com.smartsales.data.aicore.tingwu.artifact

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuArtifactFetcher.kt
// 模块：:data:ai-core
// 说明：调试/展示用途的工件下载器，限制长度与超时，避免影响主逻辑
// 作者：创建于 2025-12-12
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface TingwuArtifactFetcher {
    fun fetchText(url: String, timeoutMs: Int = 10_000, maxChars: Int = 20_000): String?
}

@Singleton
class RealTingwuArtifactFetcher @Inject constructor() : TingwuArtifactFetcher {
    override fun fetchText(url: String, timeoutMs: Int, maxChars: Int): String? {
        return runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
            }
            connection.inputStream.use { stream ->
                val payload = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (payload.length > maxChars) {
                    payload.take(maxChars)
                } else {
                    payload
                }
            }
        }.getOrNull()
    }
}

/**
 * FakeTingwuArtifactFetcher: Test double for TingwuArtifactFetcher.
 * 
 * Lattice compliance: Every box must have a corresponding Fake.
 */
class FakeTingwuArtifactFetcher : TingwuArtifactFetcher {
    private val responses = mutableMapOf<String, String?>()
    var fetchCount = 0
        private set
    
    fun stubResponse(url: String, response: String?) {
        responses[url] = response
    }
    
    fun stubDefaultResponse(response: String?) {
        responses["*"] = response
    }
    
    override fun fetchText(url: String, timeoutMs: Int, maxChars: Int): String? {
        fetchCount++
        return responses[url] ?: responses["*"]
    }
    
    fun reset() {
        responses.clear()
        fetchCount = 0
    }
}
