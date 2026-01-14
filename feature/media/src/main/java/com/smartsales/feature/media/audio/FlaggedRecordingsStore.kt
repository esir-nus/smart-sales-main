package com.smartsales.feature.media.audio

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// 文件：feature/media/src/main/java/com/smartsales/feature/media/audio/FlaggedRecordingsStore.kt
// 模块：:feature:media
// 说明：存储用户标记（星标）的录音 ID
// 作者：创建于 2026-01-14

/**
 * Storage for flagged (starred) recording IDs.
 */
interface FlaggedRecordingsStore {
    /** Get all flagged recording IDs. */
    fun getFlaggedIds(): Set<String>
    
    /** Set or clear the flagged status for a recording. */
    fun setFlagged(id: String, flagged: Boolean)
    
    /** Check if a recording is flagged. */
    fun isFlagged(id: String): Boolean = getFlaggedIds().contains(id)
}

/**
 * SharedPreferences-based implementation.
 */
@Singleton
class SharedPrefsFlaggedRecordingsStore @Inject constructor(
    @ApplicationContext context: Context
) : FlaggedRecordingsStore {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    override fun getFlaggedIds(): Set<String> {
        return prefs.getStringSet(KEY_FLAGGED_IDS, emptySet()) ?: emptySet()
    }
    
    override fun setFlagged(id: String, flagged: Boolean) {
        val current = getFlaggedIds().toMutableSet()
        if (flagged) {
            current.add(id)
        } else {
            current.remove(id)
        }
        prefs.edit().putStringSet(KEY_FLAGGED_IDS, current).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "flagged_recordings"
        private const val KEY_FLAGGED_IDS = "flagged_ids"
    }
}
