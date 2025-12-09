package com.smartsales.feature.chat.home

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenBindings.kt
// 模块：:feature:chat
// 说明：HomeScreen 所需依赖的 Hilt 绑定与默认实现
// 作者：创建于 2025-11-21

import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService as DataAiChatService
import com.smartsales.data.aicore.AiChatStreamEvent
import com.smartsales.feature.chat.core.AiChatService
import com.smartsales.feature.chat.core.AudioContextSummary
import com.smartsales.feature.chat.core.ChatRequest
import com.smartsales.feature.chat.core.ChatRole
import com.smartsales.feature.chat.core.ChatStreamEvent
import com.smartsales.feature.chat.core.DefaultQuickSkillCatalog
import com.smartsales.feature.chat.core.QuickSkillCatalog
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestrator
import com.smartsales.feature.chat.home.orchestrator.HomeOrchestratorImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/** 简单仓库实现：暂不持久化历史记录。 */
@Singleton
class InMemoryHomeSessionRepository @Inject constructor() : AiSessionRepository {
    override suspend fun loadOlderMessages(currentTopMessageId: String?): List<ChatMessageUi> = emptyList()
}

/** 将 data 层的聊天服务转换为 Home 所需的 streaming 接口。 */
@Singleton
class DelegatingHomeAiChatService @Inject constructor(
    private val delegate: DataAiChatService
) : AiChatService {
    override fun streamChat(request: ChatRequest): Flow<ChatStreamEvent> = flow {
        val dataRequest = AiChatRequest(
            prompt = buildPromptWithHistory(request),
            skillTags = request.quickSkillId?.let { setOf(it) } ?: emptySet(),
            transcriptMarkdown = request.audioContextSummary?.toMarkdown()
        )
        var lastContent = ""
        delegate.streamMessage(dataRequest).collect { event ->
            when (event) {
                is AiChatStreamEvent.Chunk -> {
                    val content = event.content
                    val delta = extractDelta(content, lastContent)
                    if (delta.isNotEmpty()) {
                        emit(ChatStreamEvent.Delta(delta))
                    }
                    lastContent = content
                }
                is AiChatStreamEvent.Completed -> {
                    lastContent = ""
                    emit(ChatStreamEvent.Completed(event.response.displayText))
                }
                is AiChatStreamEvent.Error -> {
                    lastContent = ""
                    emit(ChatStreamEvent.Error(event.throwable))
                }
            }
        }
    }

    /**
     * 使用最长公共前缀（LCP）算法提取增量内容
     * 更健壮地处理 DashScope 返回的累积内容
     */
    private fun extractDelta(newContent: String, lastContent: String): String {
        if (lastContent.isEmpty()) {
            return newContent
        }
        
        // 情况1：新内容以旧内容开头（最常见的情况）
        if (newContent.startsWith(lastContent)) {
            return newContent.substring(lastContent.length)
        }
        
        // 情况2：新内容比旧内容短，可能是重置
        if (newContent.length < lastContent.length) {
            return newContent
        }
        
        // 情况3：使用最长公共前缀算法找到匹配的前缀
        val lcp = findLongestCommonPrefix(newContent, lastContent)
        if (lcp.length >= lastContent.length * 0.8) {
            // 如果公共前缀足够长（>= 80%），认为匹配成功
            return newContent.substring(lcp.length)
        }
        
        // 情况4：尝试在新内容中查找旧内容的位置
        val index = newContent.indexOf(lastContent)
        if (index >= 0) {
            return newContent.substring(index + lastContent.length)
        }
        
        // 情况5：尝试查找旧内容的子串（处理格式变化）
        // 如果旧内容足够长，尝试查找其大部分内容
        if (lastContent.length > 20) {
            val searchKey = lastContent.substring(0, lastContent.length / 2)
            val foundIndex = newContent.indexOf(searchKey)
            if (foundIndex >= 0 && foundIndex < newContent.length / 2) {
                // 如果找到的位置在内容前半部分，可能是累积
                val remaining = newContent.substring(foundIndex + searchKey.length)
                // 检查剩余部分是否包含旧内容的后续部分
                val nextKey = lastContent.substring(searchKey.length)
                if (remaining.startsWith(nextKey)) {
                    return remaining.substring(nextKey.length)
                }
            }
        }
        
        // 情况6：无法确定增量，返回全部内容（可能是全新的内容）
        return newContent
    }
    
    /**
     * 找到两个字符串的最长公共前缀
     */
    private fun findLongestCommonPrefix(str1: String, str2: String): String {
        val minLength = minOf(str1.length, str2.length)
        var commonLength = 0
        
        for (i in 0 until minLength) {
            if (str1[i] == str2[i]) {
                commonLength++
            } else {
                break
            }
        }
        
        return str1.substring(0, commonLength)
    }

    private fun buildPromptWithHistory(request: ChatRequest): String {
        val builder = StringBuilder()
        if (request.history.isNotEmpty()) {
            val historyText = request.history.joinToString(separator = "\n") { item ->
                val role = when (item.role) {
                    ChatRole.USER -> "用户"
                    ChatRole.ASSISTANT -> "助手"
                }
                "$role：${item.content}"
            }
            builder.appendLine("历史对话：")
            builder.appendLine(historyText)
            builder.appendLine()
        }
        builder.append("最新问题：${request.userMessage}")
        // 根据模式追加提示：GENERAL_CHAT 与 SMART_ANALYSIS 各自定制
        if (request.quickSkillId == null) {
            // GENERAL 首条回复：控制在 2-4 句，末尾可选单个 JSON 对象；明确禁止多次重复和多轮 JSON 草稿。
            builder.appendLine()
            builder.appendLine()
            if (request.isFirstAssistantReply) {
                builder.appendLine("你是销售助手，服务对象是【销售人员本人】，不是终端客户。你的任务是帮助销售理解当前机会、识别信息缺口、规划下一步行动，并在需要时提供少量示例话术，而不是代替销售直接跟客户说话。")
                builder.appendLine()
                builder.appendLine("""默认情况下，请按以下原则生成首条回复（除非用户明确要求"写一封完整邮件""写详细话术脚本"等长文本）：""")
                builder.appendLine()
                builder.appendLine("1. **判断输入是否适合做销售分析**")
                builder.appendLine()
                builder.appendLine("   * 如果用户输入只是打招呼或无意义内容（例如 `hello`、`hi`、表情、`123123`、看不出任何销售场景的一串字母/数字），")
                builder.appendLine("     → 不要尝试做销售分析，也不要输出 JSON。")
                builder.appendLine("     → 只需友好回应，并引导销售粘贴与客户的真实沟通内容（如聊天记录、电话纪要、邮件、会议纪要等）。")
                builder.appendLine()
                builder.appendLine("2. **信息明显不足但能看出销售场景时**")
                builder.appendLine()
                builder.appendLine("""   * 如果只能看出一些零散关键词（例如"罗总 大规模采购 机械臂"），但缺少关键信息，""")
                builder.appendLine("     → 首先明确说明：当前信息不足，不适合直接给出完整方案。")
                builder.appendLine("     → 帮销售列出 2–4 个**需要补充的关键信息点**（例如项目背景、数量/预算、时间节点、决策链等）。")
                builder.appendLine("""     → 最后可以给出 **一段澄清用的示例话术**，用"你可以先这样问客户：'…'"的形式，帮助销售向客户把情况问清楚。""")
                builder.appendLine("     → 如果你输出 JSON，必须保守填写：")
                builder.appendLine()
                builder.appendLine("""     * 无法确定的字段留空或使用约定占位值（如 `main_person` 用"未知客户"、`location` 用"未知"），""")
                builder.appendLine("     * 不要编造具体数量、预算、地点或客户意图。")
                builder.appendLine()
                builder.appendLine("3. **信息相对完整时**")
                builder.appendLine()
                builder.appendLine("   * 如果用户已经提供了较完整的对话/纪要/需求描述，")
                builder.appendLine("     → 先用 1 段简洁文字，总结这次沟通/机会的核心情况（面向销售本人）。")
                builder.appendLine("     → 再用 2–4 句话或条目，指出关键要点：例如客户关注点、机会点、风险点、建议的下一步动作。")
                builder.appendLine("""     → 最后可以给出 1–2 句示例话术，用"你可以对客户这样说：'…'"的形式，帮助销售落地沟通。""")
                builder.appendLine("""   * 回答时始终直接跟"你（销售）"说话，不要假装自己就是销售在给客户发消息。""")
                builder.appendLine()
                builder.appendLine("4. **重复与啰嗦控制**")
                builder.appendLine()
                builder.appendLine("   * 避免无意义重复：不要一遍遍重复同一句话。")
                builder.appendLine("""   * 在分析部分，不要过多重复客户称呼（例如"罗总"整个回复中出现不超过 2 次为宜）。""")
                builder.appendLine("   * 示例话术中也避免多次重复客户称呼；自然提及 1 次即可。")
                builder.appendLine()
                builder.appendLine("5. **可选 JSON 元数据（仅在信息足够时输出）**")
                builder.appendLine()
                builder.appendLine("   * 只有当你已经从文本中提炼出较清晰的客户与场景信息时，才在回复末尾额外输出一个**可选 JSON 对象**。")
                builder.appendLine("   * 如果不确定 JSON 是否正确或字段是否匹配，请**完全不要输出 JSON**。")
                builder.appendLine("   * JSON 规则：")
                builder.appendLine()
                builder.appendLine("     1）JSON 最多输出 **一个** 对象；")
                builder.appendLine("     2）JSON 必须放在**最后一行**，前面是自然语言回复，中间用换行分隔；")
                builder.appendLine("     3）JSON 行之后**不再添加任何文字或说明**；")
                builder.appendLine("""     4）不要输出中途尝试的"半成品 JSON"或多个版本的 JSON；""")
                builder.appendLine("     5）JSON 推荐字段包括：")
                builder.appendLine()
                builder.appendLine("""     * `main_person`：主要客户称呼（例如"罗总（客户）"，未知时用"未知客户"）；""")
                builder.appendLine("""     * `short_summary`：一句话概述本次沟通或机会（信息不足时可以写"信息不足，需要补充细节"）；""")
                builder.appendLine("     * `summary_title_6chars`：不超过 6 个汉字的短标题，实在不合适可以留空；")
                builder.appendLine("""     * `location`：城市或地区名称，未知时用"未知"；""")
                builder.appendLine("     * `highlights`：关键信息/亮点列表；")
                builder.appendLine("     * `actionable_tips`：建议行动列表；")
                builder.appendLine("     * `core_insight`：一句核心洞察；")
                builder.appendLine("     * `sharp_line`：一句可直接对客户使用的精炼话术。")
                builder.appendLine()
                builder.appendLine("请严格遵守以上规则：")
                builder.appendLine()
                builder.appendLine("* 对于问候或无关内容，不做分析、不输出 JSON；")
                builder.appendLine("* 对于信息不足的销售线索，先暴露信息缺口、提问题，再给澄清话术；")
                builder.appendLine("* 对于信息充足的销售内容，先做结构化分析，再给少量落地话术；")
                builder.appendLine("* 如输出 JSON，必须只在最后一行输出**一个**干净的 JSON 对象，并避免任何中途草稿或重复。")
                builder.appendLine()
                builder.appendLine("【全局硬性规则，优先级最高】")
                builder.appendLine()
                builder.appendLine("无论上文任何说明，你必须同时满足以下约束：")
                builder.appendLine()
                builder.appendLine("中文自然语言部分总长度不超过 160 个汉字，不要写长篇大段。")
                builder.appendLine()
                builder.appendLine("禁止重复完全相同的句子或段落；如果你发现自己在重复同一句话或同一段提示，立刻结束回答。")
                builder.appendLine()
                builder.appendLine("""关于"当前信息不足""需要补充以下关键信息"这类提示语，每种只允许出现一次，不要在不同段落反复出现。""")
                builder.appendLine()
                builder.appendLine("不要使用 Markdown 标题、粗体、代码块或复杂列表符号，直接输出普通中文句子；如需列要点，只用「1、2、3」这类简单编号。")
                builder.appendLine()
                builder.appendLine("不要根据想象补充用户没有提供的具体细节（例如具体数量、预算金额、地点、内部角色姓名），信息不足时就明确说明信息不足。")
                builder.appendLine()
                builder.appendLine("如果你输出 JSON：")
                builder.appendLine("　　- JSON 必须只在最后一行输出一个完整对象；")
                builder.appendLine("　　- JSON 行之后严禁再输出任何文字；")
                builder.appendLine("　　- 如果你不确定 JSON 是否正确或字段是否合理，请完全不要输出 JSON。")
                builder.appendLine()
                builder.appendLine("如果用户输入只是打招呼或无意义内容（如 hello、一串数字/字母），只做友好回应 + 引导粘贴真实对话，并且不要输出 JSON。")
            } else {
                builder.appendLine("请按下面格式回复（总长不超过 200 字）：")
                builder.appendLine("1) 用 1-2 句话复述用户问题；如信息不足，直接说明需要更多细节")
                builder.appendLine("2) 用 2-3 条要点给出核心洞察")
                builder.appendLine("3) 用 2-4 条可执行建议")
                builder.appendLine("4) 如需深度拆解，可提醒使用「智能分析」获取完整版并可导出 PDF/CSV")
                builder.appendLine()
                builder.append("重要：不要重复前面的内容，每个编号只写一次，不要累积重复。")
            }
        } else if (request.quickSkillId == "SMART_ANALYSIS") {
            // SMART_ANALYSIS 专用：LLM 只负责产出结构化 JSON 元数据，不负责排版
            builder.appendLine()
            builder.appendLine()
            builder.appendLine("你是 SmartSales 的销售分析助手。只能输出一个 JSON 对象，不要输出 Markdown、解释或多版本草稿。")
            builder.appendLine("JSON 字段：")
            builder.appendLine("- main_person: string，本次会话最重要的客户/外部联系人称呼，未知用 \"未知客户\"，不要写你自己或内部同事/老板。")
            builder.appendLine("- short_summary: string，2–3 句中文总结。")
            builder.appendLine("- summary_title_6chars: string，≤6 个汉字的标题。")
            builder.appendLine("- location: string，可为空。")
            builder.appendLine("- highlights: string[]，2–4 条关键要点。")
            builder.appendLine("- actionable_tips: string[]，2–4 条可执行的后续行动建议。")
            builder.appendLine("- core_insight: string，一句核心洞察。")
            builder.appendLine("- sharp_line: string，一句精炼话术（≤20 汉字）。")
            builder.appendLine("- stage: string，可选，DISCOVERY/NEGOTIATION/PROPOSAL/CLOSING/POST_SALE/UNKNOWN。")
            builder.appendLine("- risk_level: string，可选，LOW/MEDIUM/HIGH/UNKNOWN。")
            builder.appendLine("示例（仅用于格式，按需增删字段）：")
            builder.appendLine("{")
            builder.appendLine("  \"main_person\": \"王总\",")
            builder.appendLine("  \"short_summary\": \"客户关注报价与交付周期\",")
            builder.appendLine("  \"summary_title_6chars\": \"报价跟进\",")
            builder.appendLine("  \"location\": \"上海\",")
            builder.appendLine("  \"highlights\": [\"预算紧张\", \"交付周期要求短\"],")
            builder.appendLine("  \"actionable_tips\": [\"提供分期方案\", \"明确交付里程碑\"],")
            builder.appendLine("  \"core_insight\": \"客户看重价格确定性\",")
            builder.appendLine("  \"sharp_line\": \"价格透明、交付准时\"")
            builder.appendLine("}")
            builder.appendLine("不要输出示例外的任何文字或格式。")
        }
        return builder.toString()
    }

    private fun AudioContextSummary.toMarkdown(): String = buildString {
        appendLine("### 音频上下文")
        appendLine("- 已准备片段：$readyClipCount 条")
        appendLine("- 待上传片段：$pendingClipCount 条")
        appendLine("- 含转写：${if (hasTranscripts) "是" else "否"}")
        note?.takeIf { it.isNotBlank() }?.let {
            appendLine("- 备注：$it")
        }
    }.trim()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeScreenBindingsModule {
    @Binds
    @Singleton
    abstract fun bindHomeAiChatService(impl: DelegatingHomeAiChatService): AiChatService

    @Binds
    @Singleton
    abstract fun bindHomeSessionRepository(impl: InMemoryHomeSessionRepository): AiSessionRepository

    @Binds
    @Singleton
    abstract fun bindHomeOrchestrator(impl: HomeOrchestratorImpl): HomeOrchestrator
}

@Module
@InstallIn(SingletonComponent::class)
object HomeScreenProvidesModule {
    @Provides
    @Singleton
    fun provideQuickSkillCatalog(): QuickSkillCatalog = DefaultQuickSkillCatalog()
}
