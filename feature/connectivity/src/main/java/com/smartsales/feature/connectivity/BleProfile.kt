package com.smartsales.feature.connectivity

import java.util.Locale
import java.util.UUID

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/BleProfile.kt
// 模块：:feature:connectivity
// 说明：统一管理厂商 BLE 服务与特征 UUID，同时支持多种 profile
// 作者：创建于 2025-11-18，重构于 2025-11-19 支持多 profile
data class BleProfileConfig(
    val id: String,
    val displayName: String,
    val nameKeywords: List<String>,
    val scanServiceUuids: List<UUID> = emptyList()
) {
    fun matches(deviceName: String?, advertisedUuids: Collection<UUID>): Boolean {
        val normalizedName = deviceName.orEmpty().lowercase(Locale.US)
        val keywordHit = nameKeywords.any { keyword ->
            normalizedName.contains(keyword.lowercase(Locale.US))
        }
        val serviceHit = scanServiceUuids.any { advertisedUuids.contains(it) }
        val hasFilters = nameKeywords.isNotEmpty() || scanServiceUuids.isNotEmpty()
        return if (!hasFilters) {
            true
        } else {
            keywordHit || serviceHit
        }
    }
}
