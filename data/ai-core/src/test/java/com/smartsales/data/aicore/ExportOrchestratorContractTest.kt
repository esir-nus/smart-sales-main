package com.smartsales.data.aicore

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/ExportOrchestratorContractTest.kt
// 模块：:data:ai-core
// 说明：守护 ExportOrchestrator 对外签名与依赖边界，避免重新引入非法接口
// 作者：创建于 2025-12-06

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportOrchestratorContractTest {

    @Test
    fun `export orchestrator exposes only pdf and csv`() {
        val declared = ExportOrchestrator::class.java.declaredMethods
            .filterNot { it.isSynthetic }
            .map { it.name }
            .toSet()
        val expected = setOf("exportPdf", "exportCsv")
        assertTrue(declared.containsAll(expected))
        assertTrue(declared.all { it in expected })
        assertFalse(declared.any { it.contains("exportMarkdown", ignoreCase = true) })
    }

    @Test
    fun `real export orchestrator has no ai chat dependency`() {
        val paramTypes = RealExportOrchestrator::class.java.constructors
            .flatMap { it.parameterTypes.toList() }
        assertFalse(paramTypes.any { it.name.contains("AiChatService") || it.name.contains("Dashscope", ignoreCase = true) })
    }

    @Test
    fun `real tingwu coordinator has no ai chat dependency`() {
        val paramTypes = RealTingwuCoordinator::class.java.constructors
            .flatMap { it.parameterTypes.toList() }
        assertFalse(paramTypes.any { it.name.contains("AiChatService") || it.name.contains("Dashscope", ignoreCase = true) })
    }
}
