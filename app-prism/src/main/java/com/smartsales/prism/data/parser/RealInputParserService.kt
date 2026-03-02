package com.smartsales.prism.data.parser

import android.util.Log
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiChatRequest
import com.smartsales.data.aicore.AiChatService
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType
import com.smartsales.prism.domain.parser.InputParserService
import com.smartsales.prism.domain.parser.ParseResult
import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealInputParserService @Inject constructor(
    private val aiChatService: AiChatService,
    private val entityRepository: EntityRepository,
    private val telemetry: PipelineTelemetry
) : InputParserService {

    companion object {
        private const val SYSTEM_PROMPT = """
You are a semantic routing and extraction gateway for a CRM system. 
Your job is to extract time intent and resolve entity names mentioned in the user's input against the provided Contact Sheet.

KNOWN CONTACT SHEET:
%s

INSTRUCTIONS:
1. Extract any temporal intent (e.g., '明天', '下周一').
2. Match ANY mentioned names against the "name" or "aliases" in the Contact Sheet.
3. If a name matches the sheet perfectly or semantically (nicknames, typos), output its "idx" in the `resolved_indices` array.
4. If a name is completely unknown and cannot be mapped to any idx, output the name as a string in the `unknown_names` array.
5. If a name is ambiguous and could map to multiple indices, DO NOT guess. Put the name in `unknown_names`.

REQUIRED JSON SCHEMA:
{
  "temporal_intent": "String or null",
  "resolved_indices": [1, 2],
  "unknown_names": ["新客户名"]
}

Rule: Return ONLY valid JSON without Markdown blocks or backticks.
"""
    }

    override suspend fun parseIntent(rawInput: String): ParseResult {
        // Step 1: Build Payload
        val persons = entityRepository.getByType(EntityType.PERSON)
        val accounts = entityRepository.getByType(EntityType.ACCOUNT)
        // Also fetch CONTACTs as they are functionally persons in CRM context
        val contacts = entityRepository.getByType(EntityType.CONTACT)
        
        val entities = (persons + accounts + contacts).distinctBy { it.entityId }

        val contactSheet = JSONArray()
        val indexToIdMap = mutableMapOf<Int, String>()

        entities.forEachIndexed { idx, entity ->
            val obj = JSONObject()
            obj.put("idx", idx + 1)
            obj.put("id", entity.entityId)
            obj.put("name", entity.displayName)

            val aliases = try {
                JSONArray(entity.aliasesJson)
            } catch (e: Exception) {
                JSONArray()
            }
            obj.put("aliases", aliases)

            contactSheet.put(obj)
            indexToIdMap[idx + 1] = entity.entityId
        }

        telemetry.recordEvent(PipelinePhase.ENTITY_RESOLUTION, "Resolving intent against Contact Sheet (Size: ${contactSheet.length()})")

        // Step 2: System Prompt
        val prompt = String.format(SYSTEM_PROMPT, contactSheet.toString())

        // Step 3: Execution
        val request = AiChatRequest(
            prompt = "${prompt}\n\nUSER INPUT: $rawInput",
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

        // Step 4: Interpretation
        val cleanJson = response.replace("```json", "").replace("```", "").trim()
        val jsonObject = try {
            JSONObject(cleanJson)
        } catch (e: Exception) {
            telemetry.recordError(PipelinePhase.ENTITY_RESOLUTION, "Failed to parse JSON: $cleanJson", e)
            Log.e("InputParser", "Failed to parse JSON: $cleanJson")
            return ParseResult.Success(emptyList(), null, cleanJson)
        }

        val temporalIntent = jsonObject.optString("temporal_intent").takeIf { it != "null" && it.isNotBlank() }
        telemetry.recordEvent(PipelinePhase.ENTITY_RESOLUTION, "Parsed temporal intent: $temporalIntent")
        
        val unknownNamesArray = jsonObject.optJSONArray("unknown_names")
        val unknownNames = mutableListOf<String>()
        val hasUnknown = unknownNamesArray != null && unknownNamesArray.length() > 0
        if (hasUnknown) {
            for (i in 0 until unknownNamesArray!!.length()) {
                val name = unknownNamesArray.optString(i)
                if (name.isNotBlank()) unknownNames.add(name)
            }
        }

        if (unknownNames.isNotEmpty()) {
            val namesStr = unknownNames.joinToString("、")
            return ParseResult.NeedsClarification(
                ambiguousName = namesStr,
                suggestedMatches = emptyList(),
                clarificationPrompt = "系统发现 '$namesStr' 似乎不在通讯录中，您是想提及新客户还是拼写有误？"
            )
        }

        val resolvedIndicesArray = jsonObject.optJSONArray("resolved_indices")
        val resolvedIds = mutableListOf<String>()
        if (resolvedIndicesArray != null) {
            for (i in 0 until resolvedIndicesArray.length()) {
                val idx = resolvedIndicesArray.optInt(i, -1)
                if (idx != -1) {
                    indexToIdMap[idx]?.let { resolvedIds.add(it) }
                }
            }
        }
        
        val distinctResolved = resolvedIds.distinct()
        telemetry.recordEvent(PipelinePhase.ENTITY_RESOLUTION, "Successfully resolved ${distinctResolved.size} entities")

        return ParseResult.Success(
            resolvedEntityIds = distinctResolved,
            temporalIntent = temporalIntent,
            rawParsedJson = cleanJson
        )
    }
}
