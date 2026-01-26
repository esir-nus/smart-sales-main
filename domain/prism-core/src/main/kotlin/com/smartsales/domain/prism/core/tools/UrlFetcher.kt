package com.smartsales.domain.prism.core.tools

/**
 * URL 内容抓取工具
 * @see Prism-V1.md §2.2 #1
 */
interface UrlFetcher {
    /**
     * 抓取网页内容
     * @return 提取的文本内容
     */
    suspend fun fetch(url: String): UrlContent
}

/**
 * URL 抓取结果
 */
data class UrlContent(
    val url: String,
    val title: String?,
    val content: String,
    val fetchedAt: Long = System.currentTimeMillis()
)
