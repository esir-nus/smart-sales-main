package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/ArtifactStatus.kt
// Module: :feature:chat
// Summary: Status values for V1 machine artifact extraction.
// Author: created on 2025-12-29

enum class ArtifactStatus {
    VALID,
    INVALID,
    RETRIED,
    FAILED
}
