// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/XfyunResultTypePolicyTest.kt
// 模块：:data:ai-core
// 说明：验证 failType=11 时 resultType 的安全降级策略
// 作者：创建于 2025-12-15

package com.smartsales.data.aicore.xfyun

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XfyunResultTypePolicyTest {

    @Test
    fun `failType 11 triggers downgrade when resultType is not transfer`() {
        assertTrue(
            XfyunResultTypePolicy.shouldDowngradeOnFailType11(
                status = -1,
                failType = 11,
                resultType = "transfer,predict",
            )
        )
        assertTrue(
            XfyunResultTypePolicy.shouldDowngradeOnFailType11(
                status = 0,
                failType = 11,
                resultType = "predict",
            )
        )
    }

    @Test
    fun `transfer only never downgrades`() {
        assertFalse(
            XfyunResultTypePolicy.shouldDowngradeOnFailType11(
                status = -1,
                failType = 11,
                resultType = "transfer",
            )
        )
        assertFalse(
            XfyunResultTypePolicy.shouldDowngradeOnFailType11(
                status = null,
                failType = null,
                resultType = "transfer",
            )
        )
    }
}

