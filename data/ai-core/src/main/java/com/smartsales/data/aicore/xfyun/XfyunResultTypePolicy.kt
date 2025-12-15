// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunResultTypePolicy.kt
// 模块：:data:ai-core
// 说明：讯飞 resultType 策略：默认仅转写(transfer)，并在 failType=11 时触发安全降级
// 作者：创建于 2025-12-15

package com.smartsales.data.aicore.xfyun

import java.util.Locale

internal object XfyunResultTypePolicy {
    const val TRANSFER_ONLY: String = "transfer"

    fun shouldDowngradeOnFailType11(
        status: Int?,
        failType: Int?,
        resultType: String,
    ): Boolean {
        // 说明：
        // - failType=11 表示账号未开通对应能力（常见于 predict/translate）。
        // - 此处不强绑定 status 的具体语义，只要出现 failType=11 且 resultType 非 transfer 就降级。
        if (failType != 11) return false
        return normalize(resultType) != TRANSFER_ONLY
    }

    private fun normalize(value: String): String =
        value.trim().lowercase(Locale.US)
}

