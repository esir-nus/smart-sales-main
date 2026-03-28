package com.smartsales.prism

import com.smartsales.prism.ui.theme.PrismThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimMainActivityLogicTest {

    @Test
    fun `shouldRequestSimCalendarPermissions waits until sim onboarding completes`() {
        assertFalse(
            shouldRequestSimCalendarPermissions(
                simOnboardingCompleted = false,
                requestAlreadyAttempted = false
            )
        )
        assertTrue(
            shouldRequestSimCalendarPermissions(
                simOnboardingCompleted = true,
                requestAlreadyAttempted = false
            )
        )
        assertFalse(
            shouldRequestSimCalendarPermissions(
                simOnboardingCompleted = true,
                requestAlreadyAttempted = true
            )
        )
    }

    @Test
    fun `resolveSimDarkTheme defaults fresh SIM startup to dark`() {
        assertTrue(
            resolveSimDarkTheme(
                themeMode = PrismThemeMode.SYSTEM,
                hasStoredThemeMode = false,
                systemDarkTheme = false
            )
        )
    }

    @Test
    fun `resolveSimDarkTheme respects stored light choice`() {
        assertFalse(
            resolveSimDarkTheme(
                themeMode = PrismThemeMode.LIGHT,
                hasStoredThemeMode = true,
                systemDarkTheme = true
            )
        )
    }

    @Test
    fun `resolveSimDarkTheme respects stored dark choice`() {
        assertTrue(
            resolveSimDarkTheme(
                themeMode = PrismThemeMode.DARK,
                hasStoredThemeMode = true,
                systemDarkTheme = false
            )
        )
    }

    @Test
    fun `resolveSimDarkTheme respects stored system choice`() {
        assertFalse(
            resolveSimDarkTheme(
                themeMode = PrismThemeMode.SYSTEM,
                hasStoredThemeMode = true,
                systemDarkTheme = false
            )
        )
        assertTrue(
            resolveSimDarkTheme(
                themeMode = PrismThemeMode.SYSTEM,
                hasStoredThemeMode = true,
                systemDarkTheme = true
            )
        )
    }
}
