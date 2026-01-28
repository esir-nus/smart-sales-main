package com.smartsales.prism.ui.onboarding

/**
 * The Golden Thread (12 Steps)
 * @see prism-ui-ux-contract.md §1.6
 */
enum class OnboardingStep {
    WELCOME,          // Step 1: Brand Hero
    PERMISSIONS,      // Step 2: Glass Card Priming
    VOICE_HANDSHAKE,  // Step 3: "Getting to know you"
    HARDWARE_WAKE,    // Step 4: Manual Instructions
    SCAN,             // Step 5: Radar Pulse
    DEVICE_FOUND,     // Step 6: Manual Connect
    WIFI_CREDS,       // Step 7: SSID/Pwd
    FIRMWARE_CHECK,   // Step 8: Integrity Check
    DEVICE_NAMING,    // Step 9: Personalization
    ACCOUNT_GATE,     // Step 10: Bind Context
    PROFILE,          // Step 11: Role/Industry
    COMPLETE          // Step 12: Success
}
