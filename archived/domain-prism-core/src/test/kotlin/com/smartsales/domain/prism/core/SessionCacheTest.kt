package com.smartsales.domain.prism.core

import com.smartsales.domain.prism.core.fakes.FakeSessionCache
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * SessionCache 接口契约测试
 */
class SessionCacheTest {
    
    private lateinit var cache: SessionCache
    
    @Before
    fun setup() {
        cache = FakeSessionCache()
    }
    
    @Test
    fun `getSnapshot returns empty initially`() {
        val snapshot = cache.getSnapshot()
        
        assertTrue(snapshot.entries.isEmpty())
    }
    
    @Test
    fun `update and getSnapshot round-trips correctly`() {
        cache.update("key1", "value1")
        cache.update("key2", "value2")
        
        val snapshot = cache.getSnapshot()
        
        assertEquals("value1", snapshot.entries["key1"])
        assertEquals("value2", snapshot.entries["key2"])
    }
    
    @Test
    fun `update overwrites existing key`() {
        cache.update("key", "old")
        cache.update("key", "new")
        
        val snapshot = cache.getSnapshot()
        
        assertEquals("new", snapshot.entries["key"])
    }
    
    @Test
    fun `clear removes all entries`() {
        cache.update("key1", "value1")
        cache.update("key2", "value2")
        cache.clear()
        
        val snapshot = cache.getSnapshot()
        
        assertTrue(snapshot.entries.isEmpty())
    }
}
