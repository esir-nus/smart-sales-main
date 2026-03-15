package com.smartsales.core.test.fakes

import com.smartsales.core.context.EnhancedContext
import com.smartsales.core.pipeline.PromptCompiler

/**
 * A fake PromptCompiler that bypasses the complex real string building 
 * and returns a predictable prompt for tests that don't need real formatting.
 */
class FakePromptCompiler : PromptCompiler() {
    
    var compileOutput: String = "Fake Compiled Prompt"
    
    override fun compile(context: EnhancedContext): String {
        return "$compileOutput\nUserText: ${context.userText}"
    }
}
