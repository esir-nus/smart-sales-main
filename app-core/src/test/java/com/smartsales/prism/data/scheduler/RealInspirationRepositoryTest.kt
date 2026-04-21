package com.smartsales.prism.data.scheduler

import android.content.SharedPreferences
import com.smartsales.prism.domain.scheduler.SchedulerTimelineItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RealInspirationRepositoryTest {

    @Test
    fun `insert then getAll emits newest item`() = runTest {
        val prefs = InMemorySharedPreferences()
        val repository = RealInspirationRepository(prefs)

        repository.insert("以后想学吉他")

        val items = repository.getAll().first()

        assertEquals(1, items.size)
        val item = items.first() as SchedulerTimelineItem.Inspiration
        assertEquals("以后想学吉他", item.title)
        assertTrue(item.timeDisplay.isBlank())
    }

    @Test
    fun `json schema round trip survives fresh repository instance`() = runTest {
        val prefs = InMemorySharedPreferences()
        val repository = RealInspirationRepository(prefs)

        val createdId = repository.insert("以后想练口语")
        val initialItems = repository.getAll().first()

        val reloaded = RealInspirationRepository(prefs)
        val reloadedItems = reloaded.getAll().first()

        assertEquals(initialItems, reloadedItems)
        val storedJson = prefs.getString("items", null).orEmpty()
        assertTrue(storedJson.contains("\"id\":\"$createdId\""))
        assertTrue(storedJson.contains("\"text\":\"以后想练口语\""))
        assertTrue(storedJson.contains("\"createdAt\":"))
    }

    @Test
    fun `delete removes by id`() = runTest {
        val prefs = InMemorySharedPreferences()
        val repository = RealInspirationRepository(prefs)

        val firstId = repository.insert("第一条")
        repository.insert("第二条")

        repository.delete(firstId)

        val items = repository.getAll().first()
        assertEquals(listOf("第二条"), items.map { (it as SchedulerTimelineItem.Inspiration).title })
    }

    @Test
    fun `corrupt json recovers to empty list without throwing`() = runTest {
        val prefs = InMemorySharedPreferences().apply {
            edit().putString("items", "{not-json").apply()
        }

        val repository = RealInspirationRepository(prefs)

        assertTrue(repository.getAll().first().isEmpty())
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            return values[key] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            @Suppress("UNCHECKED_CAST")
            return values[key] as? MutableSet<String> ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

        override fun contains(key: String?): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = Editor(values)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
    }

    private class Editor(
        private val values: MutableMap<String, Any?>
    ) : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
            values[key.orEmpty()] = value
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?
        ): SharedPreferences.Editor = apply {
            this.values[key.orEmpty()] = values
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
            values[key.orEmpty()] = value
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
            values[key.orEmpty()] = value
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
            values[key.orEmpty()] = value
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
            values[key.orEmpty()] = value
        }

        override fun remove(key: String?): SharedPreferences.Editor = apply {
            values.remove(key)
        }

        override fun clear(): SharedPreferences.Editor = apply {
            values.clear()
        }

        override fun commit(): Boolean = true

        override fun apply() = Unit
    }
}
