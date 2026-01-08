// File: data/ai-core/src/main/java/com/smartsales/data/aicore/NetworkChecker.kt
// Module: :data:ai-core
// Summary: Interface to check network connectivity before making SDK calls
// Author: created on 2026-01-08
package com.smartsales.data.aicore

/**
 * Abstraction for network connectivity check.
 * Used to prevent SDK crashes when network is unavailable.
 */
interface NetworkChecker {
    /**
     * Returns true if network is available, false otherwise.
     */
    fun isNetworkAvailable(): Boolean
}
