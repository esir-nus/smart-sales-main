package com.smartsales.prism.data.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prism-native onboarding completion gate
 * 
 * Persists onboarding-complete flag to SharedPreferences.
 * Survives app restarts and process death.
 */
@Singleton
class OnboardingGate @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        return prefs.getBoolean(KEY_COMPLETED, false)
    }
    
    private fun writeFlag(value: Boolean) {
        prefs.edit().putBoolean(KEY_COMPLETED, value).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "prism_onboarding_gate"
        private const val KEY_COMPLETED = "completed"
    }
}
