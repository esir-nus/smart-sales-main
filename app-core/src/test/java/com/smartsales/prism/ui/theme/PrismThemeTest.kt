package com.smartsales.prism.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrismThemeTest {

    @Test
    fun `system mode follows light system theme`() {
        assertFalse(
            resolvePrismDarkTheme(
                themeMode = PrismThemeMode.SYSTEM,
                systemDarkTheme = false
            )
        )
    }

    @Test
    fun `system mode follows dark system theme`() {
        assertTrue(
            resolvePrismDarkTheme(
                themeMode = PrismThemeMode.SYSTEM,
                systemDarkTheme = true
            )
        )
    }

    @Test
    fun `light mode always resolves light`() {
        assertFalse(
            resolvePrismDarkTheme(
                themeMode = PrismThemeMode.LIGHT,
                systemDarkTheme = true
            )
        )
    }

    @Test
    fun `dark mode always resolves dark`() {
        assertTrue(
            resolvePrismDarkTheme(
                themeMode = PrismThemeMode.DARK,
                systemDarkTheme = false
            )
        )
    }
}
