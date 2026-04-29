package com.smartsales.prism.ui.debug

import android.content.Context
import android.content.SharedPreferences
import com.smartsales.prism.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DebugModeStore internal constructor(
    private val prefs: SharedPreferences
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _enabled = MutableStateFlow(BuildConfig.DEBUG && prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    fun setEnabled(value: Boolean) {
        val next = BuildConfig.DEBUG && value
        prefs.edit().putBoolean(KEY_ENABLED, next).apply()
        _enabled.value = next
    }

    companion object {
        private const val PREFS_NAME = "prism_debug_mode_preferences"
        private const val KEY_ENABLED = "debug_mode_enabled"
    }
}
