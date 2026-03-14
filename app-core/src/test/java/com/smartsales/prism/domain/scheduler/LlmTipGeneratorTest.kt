package com.smartsales.prism.domain.scheduler

import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatResponse
import com.smartsales.data.aicore.AiChatService
import com.smartsales.data.aicore.AiChatStreamEvent
import com.smartsales.core.util.Result
import com.smartsales.prism.data.scheduler.LlmTipGenerator
import com.smartsales.prism.domain.crm.ClientProfileHub
import com.smartsales.prism.domain.crm.FocusedContext
import com.smartsales.prism.domain.crm.QuickContext
import com.smartsales.prism.domain.crm.ProfileActivityState
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.rl.HabitContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * LlmTipGenerator 单元测试
 * 
 * 验证 generate() 公共接口的各种输入场景:
 * - 有效 JSON 数组 → 提示列表
 * - 空数组 → 空列表
 * - Markdown 包裹 → 正确解析
 * - 无效 JSON → 空列表（不崩溃）
 * - 无 keyPersonEntityId → 空列表
 * - LLM 失败 → 空列表
 */
class LlmTipGeneratorTest {

    // ========== Test Doubles ==========

    /** 可控的 AiChatService 桩 — 返回预设响应 */
    private class StubAiChatService(
        private var response: Result<AiChatResponse> = Result.Success(
            AiChatResponse(displayText = "[]", structuredMarkdown = null)
        )
    ) : AiChatService {
        
        var lastRequest: AiChatRequest? = null
        
        fun setResponse(text: String) {
            response = Result.Success(AiChatResponse(displayText = text, structuredMarkdown = null))
        }
        
        fun setError(error: Throwable) {
            response = Result.Error(error)
        }
        
        override suspend fun sendMessage(request: AiChatRequest): Result<AiChatResponse> {
            lastRequest = request
            return response
        }
        
        override fun streamMessage(request: AiChatRequest): Flow<AiChatStreamEvent> = emptyFlow()
    }
    
    /** 最简 ClientProfileHub 桩 — 返回预设 FocusedContext */
    private class StubClientProfileHub : ClientProfileHub {
        private val entities = mutableMapOf<String, EntityEntry>()
        
        fun addEntity(entityId: String, displayName: String) {
            entities[entityId] = EntityEntry(
                entityId = entityId,
                displayName = displayName,
                entityType = EntityType.PERSON,
                aliasesJson = "[\"$displayName\"]",
                attributesJson = "{}",
                metricsHistoryJson = "{}",
                relatedEntitiesJson = "[]",
                decisionLogJson = "[]",
                demeanorJson = "{}",
                createdAt = System.currentTimeMillis(),
                lastUpdatedAt = System.currentTimeMillis()
            )
        }
        
        override suspend fun getQuickContext(entityIds: List<String>): QuickContext {
            return QuickContext(entitySnapshots = emptyMap(), recentActivities = emptyList(), suggestedNextSteps = emptyList())
        }
        
        override suspend fun getFocusedContext(entityId: String): FocusedContext {
            val entity = entities[entityId]
                ?: throw IllegalArgumentException("Entity not found: $entityId")
            return FocusedContext(
                entity = entity,
                relatedContacts = emptyList(),
                relatedDeals = emptyList(),
                activityState = ProfileActivityState(emptyList(), emptyList()),
                habitContext = HabitContext(
                    userHabits = emptyList(),
                    clientHabits = emptyList(),
                    suggestedDefaults = emptyMap()
                )
            )
        }
        
        override suspend fun observeProfileActivityState(entityId: String): kotlinx.coroutines.flow.Flow<ProfileActivityState> = emptyFlow()
    }

    // ========== Helper ==========

    private fun makeTask(
        entityId: String? = "entity-001",
        keyPerson: String? = "张总"
    ) = TimelineItemModel.Task(
        id = "task-001",
        timeDisplay = "14:00",
        title = "部门会议",
        startTime = Instant.parse("2026-02-15T06:00:00Z"),
        keyPerson = keyPerson,
        keyPersonEntityId = entityId
    )

    // ========== Tests ==========

    @Test
    fun `generate with valid JSON array returns tips`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub().apply { addEntity("entity-001", "张总") }
        val generator = LlmTipGenerator(hub, aiChat)
        
        aiChat.setResponse("""["张总偏好线下沟通", "上次提到预算紧张", "决策周期约2周"]""")
        
        val tips = generator.generate(makeTask())
        
        assertEquals(3, tips.size)
        assertEquals("张总偏好线下沟通", tips[0])
        assertEquals("上次提到预算紧张", tips[1])
        assertEquals("决策周期约2周", tips[2])
    }

    @Test
    fun `generate with empty array returns empty`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub().apply { addEntity("entity-001", "张总") }
        val generator = LlmTipGenerator(hub, aiChat)
        
        aiChat.setResponse("[]")
        
        val tips = generator.generate(makeTask())
        
        assertTrue(tips.isEmpty())
    }

    @Test
    fun `generate with markdown-wrapped JSON strips and parses`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub().apply { addEntity("entity-001", "张总") }
        val generator = LlmTipGenerator(hub, aiChat)
        
        aiChat.setResponse("""```json
["提示A", "提示B"]
```""")
        
        val tips = generator.generate(makeTask())
        
        assertEquals(2, tips.size)
        assertEquals("提示A", tips[0])
    }

    @Test
    fun `generate with invalid JSON returns empty list`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub().apply { addEntity("entity-001", "张总") }
        val generator = LlmTipGenerator(hub, aiChat)
        
        aiChat.setResponse("这不是JSON，只是普通文本")
        
        val tips = generator.generate(makeTask())
        
        assertTrue("Invalid JSON should return empty, got: $tips", tips.isEmpty())
    }

    @Test
    fun `generate without keyPersonEntityId returns empty immediately`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub()
        val generator = LlmTipGenerator(hub, aiChat)
        
        val tips = generator.generate(makeTask(entityId = null))
        
        assertTrue(tips.isEmpty())
        // 不应调用 LLM
        assertNull("Should not call AiChatService when entityId is null", aiChat.lastRequest)
    }

    @Test
    fun `generate when LLM fails returns empty list`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub().apply { addEntity("entity-001", "张总") }
        val generator = LlmTipGenerator(hub, aiChat)
        
        aiChat.setError(RuntimeException("Network timeout"))
        
        val tips = generator.generate(makeTask())
        
        assertTrue("LLM failure should return empty, got: $tips", tips.isEmpty())
    }

    @Test
    fun `generate when entity not found returns empty list`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub() // 不添加任何实体
        val generator = LlmTipGenerator(hub, aiChat)
        
        val tips = generator.generate(makeTask(entityId = "nonexistent"))
        
        assertTrue("Missing entity should return empty, got: $tips", tips.isEmpty())
    }

    @Test
    fun `generate filters blank tips from JSON array`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub().apply { addEntity("entity-001", "张总") }
        val generator = LlmTipGenerator(hub, aiChat)
        
        aiChat.setResponse("""["有效提示", "", "  ", "另一个提示"]""")
        
        val tips = generator.generate(makeTask())
        
        assertEquals(2, tips.size)
        assertEquals("有效提示", tips[0])
        assertEquals("另一个提示", tips[1])
    }

    @Test
    fun `prompt contains task title and key person`() = runTest {
        val aiChat = StubAiChatService()
        val hub = StubClientProfileHub().apply { addEntity("entity-001", "张总") }
        val generator = LlmTipGenerator(hub, aiChat)
        
        aiChat.setResponse("[]")
        generator.generate(makeTask())
        
        val prompt = requireNotNull(aiChat.lastRequest?.prompt) { "No request sent" }
        assertTrue("Prompt should contain task title", prompt.contains("部门会议"))
        assertTrue("Prompt should contain key person", prompt.contains("张总"))
    }
}
