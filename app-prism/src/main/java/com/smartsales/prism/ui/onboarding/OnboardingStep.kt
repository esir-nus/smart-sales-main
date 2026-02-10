package com.smartsales.prism.ui.onboarding

/**
 * The Golden Thread (11 Steps)
 * @see prism-ui-ux-contract.md §1.6
 * Note: Permissions are requested inline at ScanStep, not as a separate step.
 */
enum class OnboardingStep {
    WELCOME,          // Step 1: Brand Hero
    VOICE_HANDSHAKE,  // Step 2: "Getting to know you"
    HARDWARE_WAKE,    // Step 3: Manual Instructions
    SCAN,             // Step 4: Radar Pulse (requests BLE permissions here)
    DEVICE_FOUND,     // Step 5: Manual Connect
    BLE_CONNECTING,   // Step 5.5: 5s cooldown — BLE pairing buffer (makeshift)
    WIFI_CREDS,       // Step 6: SSID/Pwd
    FIRMWARE_CHECK,   // Step 7: Integrity Check
    DEVICE_NAMING,    // Step 8: Personalization
    ACCOUNT_GATE,     // Step 9: Bind Context
    PROFILE,          // Step 10: Role/Industry
    NOTIFICATION_PERMISSION, // Step 10.5: Request POST_NOTIFICATIONS (Android 13+)
    COMPLETE          // Step 11: Success
}
