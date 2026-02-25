package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.pipeline.ChatTurn
import com.smartsales.prism.domain.session.SessionTitleGenerator
import com.smartsales.prism.domain.session.TitleResult
import javax.inject.Inject

/**
 * 假标题生成器 — 用于测试和预览
 */
class FakeSessionTitleGenerator @Inject constructor() : SessionTitleGenerator {
    
    var stubResult = TitleResult("王总", "Q4预算审查")
    
    override suspend fun generateTitle(history: List<ChatTurn>): TitleResult {
        return stubResult
    }
}
