package com.smartsales.domain.prism.core

/**
 * 内存中心通知器 — Snackbar 更新通知
 * @see Prism-V1.md §2.2 #7
 */
interface MemoryCenterNotifier {
    /**
     * 通知用户内存更新
     * @param category 更新类别：习惯、客户、日程、资料
     * @param content 更新内容摘要（截断至 20 字符）
     */
    fun notifyUpdate(category: String, content: String)
}
