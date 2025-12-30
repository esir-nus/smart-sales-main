package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/test/java/com/smartsales/feature/chat/core/publisher/MachineArtifactValidatorTest.kt
// Module: :feature:chat
// Summary: Unit tests for schema-aligned MachineArtifact validation.
// Author: created on 2025-12-30

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MachineArtifactValidatorTest {

    private val validator = MachineArtifactValidator()

    @Test
    fun `valid minimal artifact passes`() {
        val json = """
            {
              "artifactType":"MachineArtifact",
              "schemaVersion":1,
              "mode":"L1",
              "provenance":{
                "chatSessionId":"cs1",
                "turnId":"t1",
                "createdAtMs":0
              }
            }
        """.trimIndent()

        val result = validator.validate(json)

        assertTrue(result.isValid)
        assertNull(result.reason)
    }

    @Test
    fun `missing provenance fails`() {
        val json = """
            {
              "artifactType":"MachineArtifact",
              "schemaVersion":1,
              "mode":"L1"
            }
        """.trimIndent()

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertEquals(MachineArtifactValidator.REASON_MISSING_REQUIRED_FIELD, result.reason)
    }

    @Test
    fun `invalid mode fails with enum mismatch`() {
        val json = """
            {
              "artifactType":"MachineArtifact",
              "schemaVersion":1,
              "mode":"L4",
              "provenance":{
                "chatSessionId":"cs1",
                "turnId":"t1",
                "createdAtMs":0
              }
            }
        """.trimIndent()

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertEquals(MachineArtifactValidator.REASON_ENUM_MISMATCH, result.reason)
    }

    @Test
    fun `provenance unknown key fails`() {
        val json = """
            {
              "artifactType":"MachineArtifact",
              "schemaVersion":1,
              "mode":"L1",
              "provenance":{
                "chatSessionId":"cs1",
                "turnId":"t1",
                "createdAtMs":0,
                "extra":"x"
              }
            }
        """.trimIndent()

        val result = validator.validate(json)

        assertFalse(result.isValid)
        assertEquals(MachineArtifactValidator.REASON_UNKNOWN_KEY_PROVENANCE, result.reason)
    }

    @Test
    fun `top level unknown key is allowed`() {
        val json = """
            {
              "artifactType":"MachineArtifact",
              "schemaVersion":1,
              "mode":"L1",
              "provenance":{
                "chatSessionId":"cs1",
                "turnId":"t1",
                "createdAtMs":0
              },
              "extraTop":"ok"
            }
        """.trimIndent()

        val result = validator.validate(json)

        assertTrue(result.isValid)
        assertNull(result.reason)
    }
}
