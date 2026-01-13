package com.smartsales.feature.designlab

import androidx.compose.runtime.Composable

/**
 * Registry for Design Lab galleries.
 * This is the single source of truth for what galleries are available.
 */
object LabRegistry {
    data class Gallery(
        val id: String,
        val title: String,
        val description: String,
        val content: @Composable () -> Unit
    )

    private val _galleries = mutableListOf<Gallery>()
    val galleries: List<Gallery> get() = _galleries

    fun register(
        id: String,
        title: String,
        description: String,
        content: @Composable () -> Unit
    ) {
        if (_galleries.none { it.id == id }) {
            _galleries.add(Gallery(id, title, description, content))
        }
    }
    
    // Auto-registration hook (called by Activity)
    fun initialize() {
        // Will populate with actual galleries later
    }
}
