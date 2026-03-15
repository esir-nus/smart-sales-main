package com.smartsales.prism.domain.core

/**
 * Defensive Deserialization Utility (Project Mono)
 * 
 * Safely maps a raw string to an Enum without crashing the application.
 * If the string does not match an Enum constant (due to schema evolution
 * or corrupted DB data), it catches the exception, prints a warning, and
 * returns the provided fallback value.
 */
inline fun <reified T : Enum<T>> safeEnumValueOf(value: String?, fallback: T): T {
    if (value.isNullOrBlank()) return fallback
    
    return try {
        enumValueOf<T>(value)
    } catch (e: Exception) {
        System.err.println("SafeEnum: Failed to deserialize [${T::class.simpleName}] from '$value', falling back to '$fallback'. Reason: ${e.message}")
        fallback
    }
}
