package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.*

/**
 * Fake MemoryCenterNotifier — No-op 实现，仅打印日志
 */
class FakeMemoryCenterNotifier : MemoryCenterNotifier {
    
    override fun notifyUpdate(category: String, content: String) {
        println("[FakeMemoryCenterNotifier] ${category}已更新：$content")
    }
}
