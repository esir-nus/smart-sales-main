package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.*

/**
 * Fake SessionCache — 内存 Map 实现
 */
class FakeSessionCache : SessionCache {
    
    private val cache = mutableMapOf<String, String>()
    
    override fun getSnapshot(): SessionCacheSnapshot {
        return SessionCacheSnapshot(entries = cache.toMap())
    }
    
    override fun update(key: String, value: String) {
        cache[key] = value
    }
    
    override fun clear() {
        cache.clear()
    }
}
