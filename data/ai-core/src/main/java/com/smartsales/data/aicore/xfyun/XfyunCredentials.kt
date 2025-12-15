// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunCredentials.kt
// 模块：:data:ai-core
// 说明：定义讯飞 Ifasr_llm REST 所需的凭证与基础配置
// 作者：创建于 2025-12-15
package com.smartsales.data.aicore.xfyun

data class XfyunCredentials(
    val appId: String,
    val accessKeyId: String,
    val accessKeySecret: String,
    val baseUrl: String,
)

