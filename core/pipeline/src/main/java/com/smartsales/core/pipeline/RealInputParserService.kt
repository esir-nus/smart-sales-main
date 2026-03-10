package com.smartsales.core.pipeline

import android.util.Log
import com.smartsales.core.util.Result
import com.smartsales.core.llm.Executor
import com.smartsales.core.llm.ExecutorResult
import com.smartsales.core.llm.ModelRegistry
import com.smartsales.prism.domain.memory.EntityRepository
import com.smartsales.prism.domain.memory.EntityType


import com.smartsales.prism.domain.telemetry.PipelinePhase
import com.smartsales.prism.domain.telemetry.PipelineTelemetry
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealInputParserService @Inject constructor(
    private val executor: Executor,
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
1. Identify if the user is declaring or defining a new CRM entity (Person or Company). e.g., "他是XX的CEO", "名字写错了，是王总", "那是新客户比克大魔王公司".
2. If it IS an entity declaration, you MUST extract the info into the `declaration` object.
   - If declaring a Person, `name` is the person's name.
   - If declaring a Company, `name` is the company's name. 
   - The `name` field inside `declaration` is ALWAYS REQUIRED for a declaration.
3. If it is NOT a declaration, extract temporal intent.
4. Match ANY mentioned names against the "name" or "aliases" in the Contact Sheet.
5. If a name matches perfectly or semantically, output its "idx" in `resolved_indices`.
6. If a name is completely unknown, output it in `unknown_names` (DO NOT guess ambiguous matches).

REQUIRED JSON SCHEMA:
{
  "temporal_intent": "String or null",
  "resolved_indices": [1, 2],
  "unknown_names": ["新客户名"],
  "declaration": {
    "name": "String",
    "company": "String or null",
    "job_title": "String or null",
    "aliases": ["String"],
    "notes": "String or null"
  } // Can be null if the user is not defining an entity
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
        val fullPrompt = "${prompt}\n\nUSER INPUT: $rawInput"
        
        val response = when (val result = executor.execute(ModelRegistry.EXTRACTOR, fullPrompt)) {
            is ExecutorResult.Success -> {
                result.content
            }
            is ExecutorResult.Failure -> {
                Log.e("InputParser", "LLM extraction failed: ${result.error}")
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
            return ParseResult.NeedsClarification(
                ambiguousName = "未知意图",
                suggestedMatches = emptyList(),
                clarificationPrompt = "系统未能理解您的语义意图，请您换个说法再试一次。"
            )
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

        // Step 5: Entity Declaration Detection (Must run BEFORE unknown check)
        val declarationObj = jsonObject.optJSONObject("declaration")
        if (declarationObj != null) {
            val decName = declarationObj.optString("name")
            if (decName.isNotBlank() && decName != "null") {
                telemetry.recordEvent(PipelinePhase.ENTITY_RESOLUTION, "Detected Entity Declaration for: $decName")
                val aliasesArray = declarationObj.optJSONArray("aliases")
                val aliasesList = mutableListOf<String>()
                if (aliasesArray != null) {
                    for (i in 0 until aliasesArray.length()) {
                        val alias = aliasesArray.optString(i)
                        if (alias.isNotBlank()) aliasesList.add(alias)
                    }
                }
                return ParseResult.EntityDeclaration(
                    name = decName,
                    company = declarationObj.optString("company").takeIf { it != "null" && it.isNotBlank() },
                    jobTitle = declarationObj.optString("job_title").takeIf { it != "null" && it.isNotBlank() },
                    aliases = aliasesList,
                    notes = declarationObj.optString("notes").takeIf { it != "null" && it.isNotBlank() }
                )
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
