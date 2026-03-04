package com.smartsales.prism.domain.pipeline

/**
 * 决定 Kernel (ContextBuilder) 从 RAM 和 SSD 加载数据的深度
 * @see docs/cerb/session-context/interface.md
 */
enum class ContextDepth {
    MINIMAL,      // 极简加载：仅 UserText + 历史记录 (跳过知识图谱和习惯加载)
    DOCUMENT_QA,  // 基础文档级：包含音频原稿和图像等文档上下文，跳过 CRM 实体加载
    FULL          // 完整状态：包含所有 3-Section 增强（用户习惯、CRM 客户图谱、日程提醒等）
}
