// File: app-core/src/main/java/com/smartsales/prism/ui/settings/VoiceVolumePreferenceStore.kt
// Module: :app-core
// Summary: 徽章语音音量偏好持久化存储，保留用户最近一次选择的音量 (0..100)。
// Author: created on 2026-04-20

package com.smartsales.prism.ui.settings

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class VoiceVolumePreferenceStore internal constructor(
    private val prefs: SharedPreferences
) {
    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private val _desiredVolume = MutableStateFlow(readDesiredVolume(prefs))
    val desiredVolume: StateFlow<Int> = _desiredVolume.asStateFlow()

    private val _lastAppliedVolume = MutableStateFlow(readLastAppliedVolume(prefs))
    val lastAppliedVolume: StateFlow<Int?> = _lastAppliedVolume.asStateFlow()

    fun setDesiredVolume(level: Int) {
        val clamped = level.coerceIn(0, 100)
        prefs.edit().putInt(KEY_DESIRED_VOLUME, clamped).apply()
        _desiredVolume.value = clamped
    }

    fun markAppliedVolume(level: Int) {
        val clamped = level.coerceIn(0, 100)
        prefs.edit().putInt(KEY_LAST_APPLIED_VOLUME, clamped).apply()
        _lastAppliedVolume.value = clamped
    }

    companion object {
        private const val PREFS_NAME = "prism_badge_voice_preferences"
        private const val KEY_DESIRED_VOLUME = "voice_volume"
        private const val KEY_LAST_APPLIED_VOLUME = "voice_volume_last_applied"
        private const val DEFAULT_VOLUME = 50

        private fun readDesiredVolume(prefs: SharedPreferences): Int {
            return prefs.getInt(KEY_DESIRED_VOLUME, DEFAULT_VOLUME).coerceIn(0, 100)
        }

        private fun readLastAppliedVolume(prefs: SharedPreferences): Int? {
            if (!prefs.contains(KEY_LAST_APPLIED_VOLUME)) return null
            return prefs.getInt(KEY_LAST_APPLIED_VOLUME, DEFAULT_VOLUME).coerceIn(0, 100)
        }
    }
}
