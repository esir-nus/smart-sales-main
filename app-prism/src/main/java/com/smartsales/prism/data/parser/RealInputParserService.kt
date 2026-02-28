package com.smartsales.prism.data.parser

import android.util.Log
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService
import com.smartsales.prism.domain.parser.AliasIndex
import com.smartsales.prism.domain.parser.InputParserService
import com.smartsales.prism.domain.parser.ParseResult
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealInputParserService @Inject constructor(
    private val aiChatService: AiChatService,
    private val aliasIndex: AliasIndex
) : InputParserService {

    companion object {
        private const val SYSTEM_PROMPT = """
You are a semantic extraction gateway. Your ONLY job is to extract time and entity names from the user's input.
Return a clean JSON object without any Markdown formatting or backticks.

REQUIRED JSON SCHEMA:
{
  "temporal_intent": "String or null (e.g., '明天', '下周一')",
  "mentioned_names": ["String", "List of person or company names mentioned in the text"]
}

Rule: Do NOT invent names. If no names are mentioned, return an empty array for mentioned_names.
"""
    }

    override suspend fun parseIntent(rawInput: String): ParseResult {
        // 1. 调用 Qwen Turbo 进行意图抽取
        val request = AiChatRequest(
            prompt = "${SYSTEM_PROMPT.trimIndent()}\n\nUSER INPUT: $rawInput",
            model = "qwen-turbo",
            skillTags = setOf("extraction", "json")
        )

        val response = when (val result = aiChatService.sendMessage(request)) {
            is Result.Success -> result.data.displayText ?: result.data.structuredMarkdown ?: ""
            is Result.Error -> {
                Log.e("InputParser", "LLM extraction failed: ${result.throwable.message}")
                return ParseResult.Success(emptyList(), null, "{}")
            }
        }

        // 2. 解析 JSON
        val cleanJson = response.replace("```json", "").replace("```", "").trim()
        val jsonObject = try {
            JSONObject(cleanJson)
        } catch (e: Exception) {
            Log.e("InputParser", "Failed to parse JSON: $cleanJson")
            return ParseResult.Success(emptyList(), null, cleanJson)
        }

        val temporalIntent = jsonObject.optString("temporal_intent").takeIf { it != "null" && it.isNotBlank() }
        val namesArray = jsonObject.optJSONArray("mentioned_names")
        val names = mutableListOf<String>()
        if (namesArray != null) {
            for (i in 0 until namesArray.length()) {
                val name = namesArray.optString(i)
                if (name.isNotBlank()) names.add(name)
            }
        }

        if (names.isEmpty()) {
            return ParseResult.Success(emptyList(), temporalIntent, cleanJson)
        }

        // 3. 顺序查询 AliasIndex 进行消歧
        return coroutineScope {
            val resolvedIds = mutableListOf<String>()
            
            for (name in names) {
                val matches = aliasIndex.resolveAlias(name)
                
                when {
                    matches.size == 1 -> {
                        // 确信命中
                        resolvedIds.add(matches.first().entityId)
                    }
                    matches.size > 1 -> {
                        // 存在二义性，直接熔断并返回澄清请求
                        val options = matches.joinToString(" / ") { it.displayName }
                        return@coroutineScope ParseResult.NeedsClarification(
                            ambiguousName = name,
                            suggestedMatches = matches,
                            clarificationPrompt = "系统发现 '$name' 指向多个可能的目标（$options），请问您指的是哪一个？"
                        )
                    }
                    else -> {
                        // 未找到实体。拦截 pipeline，向上抛出需要澄清的异常流
                        return@coroutineScope ParseResult.NeedsClarification(
                            ambiguousName = name,
                            suggestedMatches = emptyList(),
                            clarificationPrompt = "系统通讯录中未找到关于 '$name' 的记录，您是想提及新客户还是拼写有误？"
                        )
                    }
                }
            }
            
            ParseResult.Success(
                resolvedEntityIds = resolvedIds.distinct(),
                temporalIntent = temporalIntent,
                rawParsedJson = cleanJson
            )
        }
    }
}
