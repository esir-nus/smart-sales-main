package com.smartsales.prism.domain.parser

import com.smartsales.prism.domain.pipeline.EntityRef

/**
 * 结构化的语义解析结果
 */
sealed class ParseResult {
    /** 
     * 高确信度：解析成功且实体匹配明确
     * @param resolvedEntityIds 匹配到的明确的实体 ID 列表
     * @param temporalIntent 提取的时间意图（如 "明天", "下周一"）
     * @param rawParsedJson 原始的 JSON，主要用于自动生成会话标题
     */
    data class Success(
        val resolvedEntityIds: List<String>,
        val temporalIntent: String?,
        val rawParsedJson: String
    ) : ParseResult()

    /** 
     * 低确信度：遇到模糊匹配或未找到该实体，需要中止流程并询问用户
     * @param ambiguousName 触发疑问的名字（如 "孙攻"）
     * @param suggestedMatches 在别名库中猜到的可能项（供用户选择）
     * @param clarificationPrompt 用于展示的追问引导语
     */
    data class NeedsClarification(
        val ambiguousName: String,
        val suggestedMatches: List<EntityRef>,
        val clarificationPrompt: String
    ) : ParseResult()

    /**
     * User is explicitly declaring or updating an entity's CRM profile
     * (e.g., answering a clarification prompt: "He is the CEO of MoShengTai")
     */
    data class EntityDeclaration(
        val name: String,
        val company: String?,
        val jobTitle: String?,
        val aliases: List<String>,
        val notes: String?
    ) : ParseResult()
}

/**
 * Step 1: 语义网关（Turbo Router）
 * 在各种模式（Coach/Analyst/Scheduler）构建 Context 之前调用。
 * 负责从用户原始输入中提取意图，并通过 AliasIndex 进行消除歧义。
 */
interface InputParserService {
    suspend fun parseIntent(rawInput: String): ParseResult
}
