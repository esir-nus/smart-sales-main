// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunIdFactory.kt
// 模块：:data:ai-core
// 说明：生成讯飞请求所需的随机串等标识
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

import java.util.UUID

internal object XfyunIdFactory {
    fun random16(): String =
        UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(16)
}

