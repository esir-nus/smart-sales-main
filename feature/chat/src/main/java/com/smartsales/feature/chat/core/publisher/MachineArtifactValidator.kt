package com.smartsales.feature.chat.core.publisher

// File: feature/chat/src/main/java/com/smartsales/feature/chat/core/publisher/MachineArtifactValidator.kt
// Module: :feature:chat
// Summary: Deterministic MachineArtifact validation aligned with V1 schema core rules.
// Author: created on 2025-12-30

import org.json.JSONObject

data class ValidationResult(val isValid: Boolean, val reason: String? = null)

class MachineArtifactValidator {

    fun validate(jsonText: String): ValidationResult {
        val obj = runCatching { JSONObject(jsonText) }.getOrElse {
            return ValidationResult(isValid = false, reason = REASON_JSON_PARSE_ERROR)
        }
        return validate(obj)
    }

    internal fun validate(obj: JSONObject): ValidationResult {
        // 按 V1 schema 做确定性校验，避免“能解析就算合法”的风险
        for (field in REQUIRED_TOP_LEVEL_FIELDS) {
            if (!obj.has(field) || obj.isNull(field)) {
                return ValidationResult(isValid = false, reason = REASON_MISSING_REQUIRED_FIELD)
            }
        }

        val artifactType = obj.get("artifactType")
        if (artifactType !is String) {
            return ValidationResult(isValid = false, reason = REASON_TYPE_MISMATCH)
        }
        if (artifactType != "MachineArtifact") {
            return ValidationResult(isValid = false, reason = REASON_CONST_MISMATCH)
        }

        val schemaVersionValue = obj.get("schemaVersion")
        val schemaVersion = asLong(schemaVersionValue) ?: return ValidationResult(
            isValid = false,
            reason = REASON_TYPE_MISMATCH
        )
        if (schemaVersion != 1L) {
            return ValidationResult(isValid = false, reason = REASON_CONST_MISMATCH)
        }

        val modeValue = obj.get("mode")
        if (modeValue !is String) {
            return ValidationResult(isValid = false, reason = REASON_TYPE_MISMATCH)
        }
        if (modeValue !in ALLOWED_MODES) {
            return ValidationResult(isValid = false, reason = REASON_ENUM_MISMATCH)
        }

        val provenanceValue = obj.get("provenance")
        if (provenanceValue !is JSONObject) {
            return ValidationResult(isValid = false, reason = REASON_TYPE_MISMATCH)
        }

        for (field in REQUIRED_PROVENANCE_FIELDS) {
            if (!provenanceValue.has(field) || provenanceValue.isNull(field)) {
                return ValidationResult(isValid = false, reason = REASON_MISSING_REQUIRED_FIELD)
            }
        }

        // provenance 禁止未知字段；顶层允许未知字段（additionalProperties 由 schema 指定）
        val keyIterator = provenanceValue.keys()
        while (keyIterator.hasNext()) {
            val key = keyIterator.next()
            if (key !in REQUIRED_PROVENANCE_FIELDS) {
                return ValidationResult(isValid = false, reason = REASON_UNKNOWN_KEY_PROVENANCE)
            }
        }

        val chatSessionId = provenanceValue.get("chatSessionId")
        if (chatSessionId !is String) {
            return ValidationResult(isValid = false, reason = REASON_TYPE_MISMATCH)
        }
        if (chatSessionId.isBlank()) {
            return ValidationResult(isValid = false, reason = REASON_VALUE_OUT_OF_RANGE)
        }

        val turnId = provenanceValue.get("turnId")
        if (turnId !is String) {
            return ValidationResult(isValid = false, reason = REASON_TYPE_MISMATCH)
        }
        if (turnId.isBlank()) {
            return ValidationResult(isValid = false, reason = REASON_VALUE_OUT_OF_RANGE)
        }

        val createdAtValue = provenanceValue.get("createdAtMs")
        val createdAtMs = asLong(createdAtValue) ?: return ValidationResult(
            isValid = false,
            reason = REASON_TYPE_MISMATCH
        )
        if (createdAtMs < 0L) {
            return ValidationResult(isValid = false, reason = REASON_VALUE_OUT_OF_RANGE)
        }

        return ValidationResult(isValid = true, reason = null)
    }

    private fun asLong(value: Any): Long? {
        if (value !is Number) {
            return null
        }
        val asDouble = value.toDouble()
        val asLong = value.toLong()
        if (asDouble != asLong.toDouble()) {
            return null
        }
        return asLong
    }

    companion object {
        const val REASON_JSON_PARSE_ERROR = "json_parse_error"
        const val REASON_MISSING_REQUIRED_FIELD = "missing_required_field"
        const val REASON_CONST_MISMATCH = "const_mismatch"
        const val REASON_ENUM_MISMATCH = "enum_mismatch"
        const val REASON_TYPE_MISMATCH = "type_mismatch"
        const val REASON_UNKNOWN_KEY_PROVENANCE = "unknown_key_provenance"
        const val REASON_VALUE_OUT_OF_RANGE = "value_out_of_range"

        private val REQUIRED_TOP_LEVEL_FIELDS = setOf(
            "artifactType",
            "schemaVersion",
            "mode",
            "provenance"
        )
        private val REQUIRED_PROVENANCE_FIELDS = setOf(
            "chatSessionId",
            "turnId",
            "createdAtMs"
        )
        private val ALLOWED_MODES = setOf("L1", "L2", "L3")
    }
}
