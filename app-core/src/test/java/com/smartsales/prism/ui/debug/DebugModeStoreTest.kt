package com.smartsales.prism.ui.debug

import com.smartsales.prism.ui.theme.InMemorySharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugModeStoreTest {

    @Test
    fun `debug mode is off by default`() {
        val store = DebugModeStore(InMemorySharedPreferences())

        assertEquals(false, store.enabled.value)
    }

    @Test
    fun `setEnabled persists enabled value for recreation`() {
        val prefs = InMemorySharedPreferences()
        val store = DebugModeStore(prefs)

        store.setEnabled(true)

        assertEquals(true, store.enabled.value)
        assertEquals(true, DebugModeStore(prefs).enabled.value)
    }
}
