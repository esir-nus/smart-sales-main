package com.smartsales.domain.prism.core.entities

import com.smartsales.domain.prism.core.Mode

/**
 * 会话实体
 */
data class Session(
    val id: String,
    val title: String,
    val mode: Mode,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val messageCount: Int = 0,
    val lastMessagePreview: String? = null
)
