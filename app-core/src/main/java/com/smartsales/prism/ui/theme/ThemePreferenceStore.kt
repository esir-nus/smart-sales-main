package com.smartsales.prism.ui.theme

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PrismThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStoredValue(value: String?): PrismThemeMode {
            return entries.firstOrNull { it.name == value } ?: SYSTEM
        }
    }
}

fun PrismThemeMode.toDisplayLabel(): String = when (this) {
    PrismThemeMode.SYSTEM -> "跟随系统"
    PrismThemeMode.LIGHT -> "浅色"
    PrismThemeMode.DARK -> "深色"
}

@Singleton
class ThemePreferenceStore internal constructor(
    private val prefs: SharedPreferences
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _themeMode = MutableStateFlow(readThemeMode(prefs))
    val themeMode: StateFlow<PrismThemeMode> = _themeMode.asStateFlow()
    private val _hasStoredThemeMode = MutableStateFlow(hasStoredThemeMode(prefs))
    val hasStoredThemeMode: StateFlow<Boolean> = _hasStoredThemeMode.asStateFlow()

    fun setThemeMode(mode: PrismThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
        _hasStoredThemeMode.value = true
    }

    companion object {
        private const val PREFS_NAME = "prism_theme_preferences"
        private const val KEY_THEME_MODE = "theme_mode"

        private fun readThemeMode(prefs: SharedPreferences): PrismThemeMode {
            return PrismThemeMode.fromStoredValue(prefs.getString(KEY_THEME_MODE, null))
        }

        private fun hasStoredThemeMode(prefs: SharedPreferences): Boolean {
            return prefs.contains(KEY_THEME_MODE)
        }
    }
}
