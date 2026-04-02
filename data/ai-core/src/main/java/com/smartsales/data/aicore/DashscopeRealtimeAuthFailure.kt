package com.smartsales.data.aicore

enum class DashscopeRealtimeAuthFailureCategory {
    CONFIG_MISSING,
    SDK_AUTH_REJECTED,
    UNKNOWN
}

data class DashscopeRealtimeAuthDiagnostic(
    val category: DashscopeRealtimeAuthFailureCategory,
    val vendorCode: Int? = null,
    val safeMessage: String
)
