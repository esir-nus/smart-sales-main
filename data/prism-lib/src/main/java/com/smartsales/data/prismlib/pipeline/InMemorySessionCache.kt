package com.smartsales.data.prismlib.pipeline

import com.smartsales.domain.prism.core.SessionCache
import com.smartsales.domain.prism.core.SessionCacheSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

/**
 * 内存 Session Cache — 任务内快速上下文
 * @see Prism-V1.md §2.2 #1b
 */
@Singleton
class InMemorySessionCache @Inject constructor() : SessionCache {

    private val cache = ConcurrentHashMap<String, String>()

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
