package com.smartsales.feature.connectivity.badge

/**
 * Badge connectivity states.
 *
 * State machine:
 * UNKNOWN → PAIRED (BLE connected) → OFFLINE (0.0.0.0) or CONNECTED (valid IP)
 */
enum class BadgeState {
    /** App just launched, no connection info yet */
    UNKNOWN,

    /** BLE connected, network status not yet queried */
    PAIRED,

    /** BLE connected, badge has no WiFi (returned 0.0.0.0) */
    OFFLINE,

    /** BLE connected, badge on same WiFi, HTTP reachable */
    CONNECTED
}

/**
 * Comprehensive badge status snapshot.
 * Designed for future expansion (battery, firmware, signal quality).
 */
data class BadgeStatus(
    val state: BadgeState,
    val ipAddress: String? = null,
    val wifiName: String? = null,
    val lastCheckMs: Long = 0L,
    val bleConnected: Boolean = false,
    val consecutiveFailures: Int = 0
) {
    companion object {
        val UNKNOWN = BadgeStatus(state = BadgeState.UNKNOWN)
    }

    /** Returns true if badge is reachable for file transfers */
    val isTransferReady: Boolean
        get() = state == BadgeState.CONNECTED && !ipAddress.isNullOrBlank()

    /** Returns true if badge needs WiFi provisioning */
    val needsWifiSetup: Boolean
        get() = state == BadgeState.OFFLINE || (state == BadgeState.PAIRED && ipAddress.isNullOrBlank())
}
