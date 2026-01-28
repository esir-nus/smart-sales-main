package com.smartsales.prism.ui.drawers

/**
 * Audio Drawer State Models
 * @see prism-ui-ux-contract.md §1.8
 */

data class AudioItemState(
    val id: String,
    val filename: String,
    val timeDisplay: String, // e.g., "1 day", "10:15", "14:30"
    val source: AudioSource,
    val status: AudioStatus,
    val isStarred: Boolean = false,
    val summary: String? = null,
    val progress: Float? = null // 0.0 - 1.0 (valid only for TRANSCRIBING)
)

enum class AudioStatus {
    TRANSCRIBED, // Finished, shows summary
    TRANSCRIBING, // In progress, shows progress bar
    PENDING      // Not started, shows swipe prompt
}

enum class AudioSource {
    SMARTBADGE, // Cloud icon
    PHONE       // Phone/Mobile icon
}
