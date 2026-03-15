package com.smartsales.core.test.fakes

import com.smartsales.core.pipeline.InputParserService
import com.smartsales.core.pipeline.ParseResult

class FakeInputParserService : InputParserService {
    var nextResult: ParseResult = ParseResult.Success(emptyList(), null, "{}")

    override suspend fun parseIntent(rawInput: String): ParseResult {
        return nextResult
    }
}
