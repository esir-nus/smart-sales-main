package com.smartsales.data.prismlib.tools

import com.smartsales.domain.prism.core.tools.UrlContent
import com.smartsales.domain.prism.core.tools.UrlFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp + Jsoup 实现的 URL 内容抓取工具
 * @see Prism-V1.md §2.2 #1 Input Normalization
 */
@Singleton
class OkHttpUrlFetcher @Inject constructor(
    private val okHttpClient: OkHttpClient
) : UrlFetcher {

    override suspend fun fetch(url: String): UrlContent = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (compatible; SmartSales/1.0)")
                .build()

            val response = okHttpClient.newCall(request).execute()
            val html = response.body?.string() ?: ""

            // 使用 Jsoup 解析 HTML
            val document = Jsoup.parse(html)
            val title = document.title().takeIf { it.isNotBlank() }
            
            // 提取正文内容，移除脚本和样式
            document.select("script, style, nav, header, footer, aside").remove()
            val content = document.body()?.text() ?: ""

            UrlContent(
                url = url,
                title = title,
                content = content.take(MAX_CONTENT_LENGTH) // 限制内容长度
            )
        } catch (e: Exception) {
            // 失败时返回空内容
            UrlContent(
                url = url,
                title = null,
                content = "抓取失败: ${e.message}"
            )
        }
    }

    companion object {
        private const val MAX_CONTENT_LENGTH = 10_000 // 最大内容长度
    }
}
