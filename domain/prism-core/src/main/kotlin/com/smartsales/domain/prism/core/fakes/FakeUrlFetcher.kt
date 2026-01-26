package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.tools.UrlContent
import com.smartsales.domain.prism.core.tools.UrlFetcher

class FakeUrlFetcher : UrlFetcher {
    
    override suspend fun fetch(url: String): UrlContent {
        return UrlContent(
            url = url,
            title = "模拟网页标题",
            content = "这是从 $url 抓取的模拟网页内容。包含各种产品信息和公司介绍。"
        )
    }
}
