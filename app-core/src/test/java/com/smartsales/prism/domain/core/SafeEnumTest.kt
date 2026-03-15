package com.smartsales.prism.domain.core

import org.junit.Assert.assertEquals
import org.junit.Test

enum class TestEnum {
    VALID_A,
    VALID_B,
    UNKNOWN_FALLBACK
}

class SafeEnumTest {

    @Test
    fun `safeEnumValueOf returns exact match when valid`() {
        // Arrange
        val input = "VALID_A"

        // Act
        val result = safeEnumValueOf(input, fallback = TestEnum.UNKNOWN_FALLBACK)

        // Assert
        assertEquals(TestEnum.VALID_A, result)
    }

    @Test
    fun `safeEnumValueOf returns fallback when invalid string`() {
        // Arrange
        val input = "INVALID_OBSOLETE_VALUE"

        // Act
        val result = safeEnumValueOf(input, fallback = TestEnum.UNKNOWN_FALLBACK)

        // Assert
        assertEquals(TestEnum.UNKNOWN_FALLBACK, result)
    }

    @Test
    fun `safeEnumValueOf returns fallback when null`() {
        // Arrange
        val input: String? = null

        // Act
        val result = safeEnumValueOf(input, fallback = TestEnum.UNKNOWN_FALLBACK)

        // Assert
        assertEquals(TestEnum.UNKNOWN_FALLBACK, result)
    }

    @Test
    fun `safeEnumValueOf returns fallback when blank`() {
        // Arrange
        val input = "   "

        // Act
        val result = safeEnumValueOf(input, fallback = TestEnum.UNKNOWN_FALLBACK)

        // Assert
        assertEquals(TestEnum.UNKNOWN_FALLBACK, result)
    }
}
