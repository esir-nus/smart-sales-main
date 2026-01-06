# Orchestrator–MetaHub V4 Verification Audit

> **Date**: 2025-12-10  
> **Scope**: Overall Orchestrator–MetaHub V4 behavior across GENERAL chat, SMART_ANALYSIS, and Tingwu  
> **Mode**: Read-only analysis (no code changes)

---

## 1. Restate the scope

This audit covers:
- End-to-end SMART_ANALYSIS behavior (text + audio triggered): prompt construction, LLM output, Orchestrator parsing/Markdown generation, ViewModel streaming/placeholder handling
- GENERAL chat first-reply metadata parsing and prompt hygiene: lightweight guidance requirement vs current scaffold
- Orchestrator–MetaHub V4 responsibility boundary alignment: LLM/Orchestrator/ViewModel/MetaHub layer separation

---

## 2. V4 rules (from docs)

### LLM Responsibilities
- **GENERAL chat**: Returns natural-language text; may optionally append a JSON metadata block at the end
- **SMART_ANALYSIS**: Returns **a single top-level JSON object only** – no Markdown, no explanation, no examples

### Orchestrator Responsibilities
- Building LLM requests (prompt + history)
- Parsing JSON returned by the LLM
- Constructing and upserting `SessionMetadata` into MetaHub
- For SMART_ANALYSIS **only**: locally assembling the Markdown text to show in the chat bubble based on `SessionMetadata`
- Does **not** decide UI layout; it only decides "what this assistant message's text should be"

### ViewModel / UI Responsibilities
- Controlling whether tokens are streamed into the UI:
  - GENERAL → streamed
  - SMART_ANALYSIS → **not** streamed; only final text
- Showing local placeholders ("Analyzing…") and replacing them on completion
- Rendering the final text from the Orchestrator
- Does **not** parse complex JSON templates and **never** shows raw JSON to the user

### Prompt Rules
- **GENERAL first reply**: Lightweight guidance only (保持简短的自然语言约束，短段落即可，避免长篇), may include **one compact JSON example + short field descriptions**, **forbidden** long outline/scaffold or multi-section analysis templates
- **SMART_ANALYSIS**: Must ensure output is exactly one top-level JSON object, no extra natural language/Markdown

### MetaHub Write Rules
- **GENERAL first reply**: If JSON parsing succeeds and at least one field has a value, call `upsertSession` (non-empty overwrite). Responsibility: ViewModel extracts JSON and writes to MetaHub
- **SMART_ANALYSIS**: If JSON completely fails, do not write anything; if partially succeeds, write available fields and update `latestMajorAnalysis*`. Responsibility: Orchestrator parses JSON and writes to MetaHub

### Error Handling
- SMART JSON completely fails: Do nothing in MetaHub, UI shows simple failure message, **do not** show JSON or half-baked Markdown
- SMART JSON partially succeeds: Upsert only non-empty fields, render only sections that have content
- GENERAL JSON fails: UI still shows full natural-language answer, MetaHub not updated

---

## 3. Findings by rule

### LLM Output Format

- ✅ **OK – SMART_ANALYSIS prompt requests JSON-only** (`HomeScreenBindings.kt` lines 184-211): Prompt explicitly states "只能输出一个 JSON 对象，不要输出 Markdown、解释或多版本草稿" and includes "不要输出示例外的任何文字或格式"

- ⚠️ **MISMATCH – GENERAL first-reply prompt still contains scaffold** (`HomeScreenBindings.kt` lines 161-174): While the prompt says "不要套用长篇报告或多段 Markdown 模板", it still includes a **multi-line JSON example** (9 lines) with all fields and placeholder text. This violates V4 requirement: "保持简短的自然语言约束（短段落即可，避免长篇），可附带 **一个精简的 JSON 示例 + 简短字段说明**". The current example is too verbose and detailed.

### Orchestrator Behavior

- ✅ **OK – SMART_ANALYSIS JSON parsing**: `HomeOrchestratorImpl.parseSmartAnalysisPayload` extracts last JSON object, uses "last value wins" strategy (lines 122-192)

- ✅ **OK – SMART_ANALYSIS Markdown generation**: `HomeOrchestratorImpl.buildSmartAnalysisMarkdown` generates Markdown locally based on parsed data, does not depend on LLM output (lines 270-329)

- ✅ **OK – MetaHub writes for SMART_ANALYSIS**: `HomeOrchestratorImpl.buildMergedMetadata` uses non-empty overwrite strategy, updates `latestMajorAnalysis*` fields correctly (lines 86-113)

- ✅ **OK – GENERAL pass-through**: `HomeOrchestratorImpl.streamChat` acts as transparent pass-through for GENERAL (lines 53-65, `shouldParseMetadata` returns false for GENERAL)

- ✅ **OK – GENERAL metadata parsing ownership**: V4 doc explicitly states ViewModel owns GENERAL metadata parsing (`handleGeneralChatMetadata`), which matches current implementation (`HomeScreenViewModel.kt` lines 1221-1224)

### ViewModel Streaming Behavior

- ✅ **OK – SMART_ANALYSIS Delta ignoring**: `HomeScreenViewModel.kt` line 1211 `if (!isSmartAnalysis)` ensures Delta events are not rendered for SMART_ANALYSIS

- ✅ **OK – SMART_ANALYSIS placeholder insertion**: Lines 1200-1204 immediately insert local placeholder `SMART_PLACEHOLDER_TEXT` for SMART_ANALYSIS

- ✅ **OK – SMART_ANALYSIS Completed replacement**: Lines 1256-1258 replace placeholder with Orchestrator's final text (from `event.fullText` which is Orchestrator-generated Markdown)

- ✅ **OK – GENERAL streaming**: Lines 1211-1215 properly stream tokens for GENERAL requests

- ✅ **OK – JSON not exposed**: ViewModel does not pass raw JSON to UI; for SMART_ANALYSIS, it receives Orchestrator-generated Markdown via `ChatStreamEvent.Completed`

### MetaHub Write Timing

- ✅ **OK – SMART_ANALYSIS writes**: `HomeOrchestratorImpl` parses and writes to MetaHub in Completed event (lines 56-59)

- ✅ **OK – GENERAL first-reply writes**: ViewModel's `handleGeneralChatMetadata` handles GENERAL metadata parsing and MetaHub writes (line 1224), which matches V4 specification

### Error Handling

- ✅ **OK – SMART failure handling**: `HomeOrchestratorImpl` returns `SMART_ANALYSIS_FAILURE_MESSAGE` when JSON parsing fails (lines 71-75), ViewModel replaces placeholder with failure text (lines 1233-1245)

- ✅ **OK – Partial success handling**: `buildMergedMetadata` uses non-empty overwrite strategy, only updates fields that have values (lines 101-112)

---

## 4. Likely causes of current visible issues

Based on V4 doc lines 454-458 "实现现状提示":

**Symptom**: GENERAL first-reply may still show verbose JSON examples or template-like structure
- **Likely cause**: `HomeScreenBindings.kt` lines 161-174 prompt still contains a 9-line JSON example with all fields and placeholder text, violating V4 "lightweight guidance (短段落即可，避免长篇)" requirement
- **Layer**: Prompt layer (`HomeScreenBindings.buildPromptWithHistory`)

**Note**: The V4 doc implementation status (line 457) explicitly states: "GENERAL 首条回复仍有历史 scaffold，后续应按 V4"轻量提示"收敛。" This confirms the mismatch is known and needs to be addressed.

---

## 5. Audit summary

### Statistics
- **Total rules checked**: 14
- **OK**: 13
- **MISMATCH**: 1
- **UNKNOWN**: 0

### Key Findings

#### Architecture Layer (Responsibility Ownership)
- ✅ **All responsibility boundaries are correctly aligned**: SMART_ANALYSIS handled by Orchestrator, GENERAL handled by ViewModel (as per V4 spec)

#### Prompt Layer
- ⚠️ **GENERAL first-reply prompt still contains scaffold**: While simplified from previous versions, still contains multi-line JSON example (9 lines) that violates V4 "lightweight guidance" requirement. The prompt text itself says "不要套用长篇报告"，but the JSON example is still too verbose.

#### Implementation Layer (Already Aligned)
- ✅ **SMART_ANALYSIS streaming control**: ViewModel correctly ignores Deltas, inserts placeholder, replaces with Orchestrator Markdown
- ✅ **SMART_ANALYSIS JSON parsing**: Orchestrator correctly extracts last JSON, uses "last value wins"
- ✅ **SMART_ANALYSIS Markdown generation**: Orchestrator generates locally, does not depend on LLM
- ✅ **MetaHub writes**: Both SMART_ANALYSIS and GENERAL paths correctly write, use non-empty overwrite strategy
- ✅ **Error handling**: Failure cases handled gracefully, no JSON leakage to UI

### Priority Fix Recommendations

1. **High priority**: Fix GENERAL prompt scaffold
   - **Location**: `HomeScreenBindings.kt` lines 161-174
   - **Goal**: Simplify multi-line JSON example to a compact single-line example + short field descriptions, matching V4 "lightweight guidance" requirement
   - **Type**: Prompt-level fix

### Overall Assessment

**SMART_ANALYSIS path is fully V4-aligned**: LLM only produces JSON, Orchestrator generates Markdown, ViewModel correctly controls streaming and placeholders. **GENERAL path is mostly aligned** except for prompt hygiene: the prompt still contains verbose JSON example that should be simplified to match V4 lightweight guidance requirement.

---

## Next Steps

1. Simplify GENERAL first-reply prompt in `HomeScreenBindings.kt` to match V4 lightweight guidance (compact JSON example + short field descriptions)
2. Verify prompt changes do not break existing GENERAL metadata extraction logic in ViewModel

---

**Note**: This audit is analysis-only. No code changes were made. Document sync and Codex implementation prompts will be separate steps.

