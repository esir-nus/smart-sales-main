package com.smartsales.core.test.fakes

import com.smartsales.prism.domain.session.SessionTitleGenerator
import com.smartsales.prism.domain.session.TitleResult

class FakeSessionTitleGenerator : SessionTitleGenerator {
    var nextResult: TitleResult = TitleResult("Fake Client", "Fake Summary")
    
    override fun generateTitle(rawParsedJson: String, resolvedNames: List<String>): TitleResult {
        return nextResult
    }
}
