package com.smartsales.prism

import com.smartsales.prism.ui.theme.PrismThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityLogicTest {

    @Test
    fun `shouldRequestBaseRuntimeCalendarPermissions waits until onboarding completes`() {
        assertFalse(
            shouldRequestBaseRuntimeCalendarPermissions(
                onboardingCompleted = false,
                requestAlreadyAttempted = false
            )
        )
        assertTrue(
            shouldRequestBaseRuntimeCalendarPermissions(
                onboardingCompleted = true,
                requestAlreadyAttempted = false
            )
        )
        assertFalse(
            shouldRequestBaseRuntimeCalendarPermissions(
                onboardingCompleted = true,
                requestAlreadyAttempted = true
            )
        )
    }

    @Test
    fun `resolveBaseRuntimeDarkTheme defaults fresh startup to dark`() {
        assertTrue(
            resolveBaseRuntimeDarkTheme(
                themeMode = PrismThemeMode.SYSTEM,
                hasStoredThemeMode = false,
                systemDarkTheme = false
            )
        )
    }

    @Test
    fun `resolveBaseRuntimeDarkTheme respects stored light choice`() {
        assertFalse(
            resolveBaseRuntimeDarkTheme(
                themeMode = PrismThemeMode.LIGHT,
                hasStoredThemeMode = true,
                systemDarkTheme = true
            )
        )
    }

    @Test
    fun `resolveBaseRuntimeDarkTheme respects stored dark choice`() {
        assertTrue(
            resolveBaseRuntimeDarkTheme(
                themeMode = PrismThemeMode.DARK,
                hasStoredThemeMode = true,
                systemDarkTheme = false
            )
        )
    }

    @Test
    fun `resolveBaseRuntimeDarkTheme respects stored system choice`() {
        assertFalse(
            resolveBaseRuntimeDarkTheme(
                themeMode = PrismThemeMode.SYSTEM,
                hasStoredThemeMode = true,
                systemDarkTheme = false
            )
        )
        assertTrue(
            resolveBaseRuntimeDarkTheme(
                themeMode = PrismThemeMode.SYSTEM,
                hasStoredThemeMode = true,
                systemDarkTheme = true
            )
        )
    }
}
