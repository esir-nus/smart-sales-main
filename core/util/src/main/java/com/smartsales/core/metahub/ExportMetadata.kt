package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/ExportMetadata.kt
// 模块：:core:util
// 说明：定义导出相关元数据，记录最近一次导出信息
// 作者：创建于 2025-12-04

/**
 * 导出元数据，仅存文件名与时间戳，不含导出正文内容。
 */
data class ExportMetadata(
    val sessionId: String,
    val lastPdfFileName: String?,
    val lastPdfGeneratedAt: Long?,
    val lastCsvFileName: String?,
    val lastCsvGeneratedAt: Long?
)
