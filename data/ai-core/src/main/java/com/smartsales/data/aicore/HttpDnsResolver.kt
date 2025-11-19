package com.smartsales.data.aicore

import java.net.InetAddress

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/HttpDnsResolver.kt
// 模块：:data:ai-core
// 说明：抽象 HTTPDNS 解析能力，便于接入阿里云移动解析 SDK
// 作者：创建于 2025-11-18
interface HttpDnsResolver {
    fun lookup(hostname: String): List<InetAddress>
}
