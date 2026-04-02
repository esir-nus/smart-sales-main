package com.smartsales.prism.data.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared production onboarding completion gate.
 *
 * Keeps the final unified root on one product truth while still honoring
 * completion signals written by the former split-era roots.
 */
@Singleton
class BaseRuntimeOnboardingGate @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _completedFlow = MutableStateFlow(readFlag())

    val completedFlow: StateFlow<Boolean> = _completedFlow.asStateFlow()

    fun markCompleted() {
        writeFlag(true)
        _completedFlow.value = true
    }

    fun reset() {
        writeFlag(false)
        _completedFlow.value = false
    }

    private fun readFlag(): Boolean {
        return allPrefs().any { prefs -> prefs.getBoolean(KEY_COMPLETED, false) }
    }

    private fun writeFlag(value: Boolean) {
        allPrefs().forEach { prefs ->
            prefs.edit().putBoolean(KEY_COMPLETED, value).apply()
        }
    }

    private fun allPrefs() = PREFS_NAMES.map { name ->
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_COMPLETED = "completed"
        private val PREFS_NAMES = listOf(
            "base_runtime_onboarding_gate",
            "sim_onboarding_gate",
            "prism_onboarding_gate"
        )
    }
}
