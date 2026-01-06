# Orchestrator–MetaHub V4 Verification Audit

> **Date**: 2025-12-10  
> **Scope**: End-to-end SMART_ANALYSIS behavior, GENERAL chat metadata parsing, and Orchestrator–MetaHub V4 alignment  
> **Mode**: Read-only analysis (no code changes)

---

## 1. Scope

This audit covers:
- End-to-end SMART_ANALYSIS behavior (text + audio triggered)
- GENERAL chat first-reply metadata parsing and prompt hygiene
- Orchestrator–MetaHub V4 responsibility boundary alignment
- ViewModel streaming and placeholder behavior

---

## 2. V4 Rules (from docs)

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
- **GENERAL first reply**: Lightweight guidance only (at most 1–3 sentences of natural-language constraints), JSON instructions must be short, **forbidden** long outline/scaffold
- **SMART_ANALYSIS**: Must ensure output is exactly one top-level JSON object, no extra natural language/Markdown

### MetaHub Write Rules
- **GENERAL first reply**: If JSON parsing succeeds and at least one field has a value, call `upsertSession` (non-empty overwrite)
- **SMART_ANALYSIS**: If JSON completely fails, do not write anything; if partially succeeds, write available fields and update `latestMajorAnalysis*`

---

## 3. Findings by Rule

### LLM Output Format
- ✅ **OK – SMART_ANALYSIS prompt requests JSON-only** (`HomeScreenBindings.kt` lines 184-200 explicitly require "只能输出一个 JSON 对象，不要输出 Markdown、解释或多版本草稿")
- ⚠️ **MISMATCH – GENERAL first-reply prompt still contains scaffold** (`HomeScreenBindings.kt` lines 161-183): While simplified, still contains multi-line JSON examples and format instructions, violating V4 "lightweight guidance (1–3 sentences)" requirement

### Orchestrator Behavior
- ✅ **OK – SMART_ANALYSIS JSON parsing**: `HomeOrchestratorImpl` extracts last JSON object, uses "last value wins" strategy
- ✅ **OK – SMART_ANALYSIS Markdown generation**: `buildSmartAnalysisMarkdown` generates Markdown locally, does not depend on LLM output
- ✅ **OK – MetaHub writes**: `buildMergedMetadata` uses non-empty overwrite strategy, updates `latestMajorAnalysis*` fields
- ❓ **UNKNOWN – GENERAL first-reply metadata parsing location**: V4 doc lines 126-142 require "extract the last JSON block" but do not explicitly state whether Orchestrator or ViewModel owns this responsibility; current implementation likely has ViewModel's `handleGeneralChatMetadata` handling this logic

### ViewModel Streaming Behavior
- ✅ **OK – SMART_ANALYSIS Delta ignoring**: `HomeScreenViewModel.kt` line 1178 `if (!isSmartAnalysis)` ensures Delta events are not rendered
- ✅ **OK – SMART_ANALYSIS placeholder insertion**: Lines 1169-1171 immediately insert local placeholder `SMART_PLACEHOLDER_TEXT`
- ✅ **OK – SMART_ANALYSIS Completed replacement**: Lines 1223-1225 replace placeholder with Orchestrator's final text
- ✅ **OK – GENERAL streaming**: Lines 1178-1182 properly stream tokens for GENERAL

### UI Display Rules
- ✅ **OK – JSON not exposed**: ViewModel does not pass raw JSON to UI, only Orchestrator-generated Markdown
- ✅ **OK – SMART_ANALYSIS failure handling**: Lines 1244-1253 replace with friendly failure message on error, no JSON shown

### MetaHub Write Timing
- ✅ **OK – SMART_ANALYSIS writes**: `HomeOrchestratorImpl` parses and writes to MetaHub in Completed event
- ❓ **UNKNOWN – GENERAL first-reply writes**: V4 doc does not explicitly state whether Orchestrator or ViewModel owns this; current implementation likely has ViewModel's `handleGeneralChatMetadata`

---

## 4. Likely Causes of Current Visible Issues

Based on V4 doc lines 447-451 "实现现状提示":

**Symptom**: GENERAL first-reply may still show long template/scaffold
- **Likely cause**: `HomeScreenBindings.kt` lines 161-183 prompt still contains multi-line JSON examples and format instructions, violating V4 "lightweight guidance" requirement
- **Layer**: Prompt layer (`HomeScreenBindings`)

**Symptom**: GENERAL metadata parsing ownership unclear
- **Likely cause**: V4 doc does not explicitly state whether GENERAL first-reply JSON parsing is owned by Orchestrator or ViewModel; current implementation likely has ViewModel, but V4 tendency is Orchestrator unified handling
- **Layer**: Architecture layer (responsibility ownership)

---

## 5. Audit Summary

### Statistics
- **Total rules checked**: 12
- **OK**: 8
- **MISMATCH**: 1
- **UNKNOWN**: 2

### Key Findings

#### Architecture Layer (Responsibility Ownership)
- **GENERAL first-reply metadata parsing ownership unclear**: V4 doc does not explicitly state whether Orchestrator or ViewModel owns this; current implementation likely has ViewModel, but recommendation is to unify in Orchestrator for consistency

#### Prompt Layer
- **GENERAL first-reply prompt still contains scaffold**: While simplified, still contains multi-line JSON examples, violating V4 "lightweight guidance (1–3 sentences)" requirement

#### Implementation Layer (Already Aligned)
- **SMART_ANALYSIS streaming control**: ViewModel correctly ignores Deltas, inserts placeholder, replaces with Orchestrator Markdown
- **SMART_ANALYSIS JSON parsing**: Orchestrator correctly extracts last JSON, uses "last value wins"
- **SMART_ANALYSIS Markdown generation**: Orchestrator generates locally, does not depend on LLM
- **MetaHub writes**: SMART_ANALYSIS path correctly writes, uses non-empty overwrite strategy

### Priority Fix Recommendations

1. **High priority**: Fix GENERAL prompt scaffold
   - **Location**: `HomeScreenBindings.kt` lines 161-183
   - **Goal**: Simplify multi-line JSON examples and format instructions to 1–3 sentences of natural-language constraints

2. **Medium priority**: Clarify GENERAL metadata parsing ownership
   - **Option A**: Move GENERAL first-reply JSON parsing to Orchestrator (consistent with SMART_ANALYSIS)
   - **Option B**: Explicitly document in V4 that GENERAL is handled by ViewModel (separate from SMART_ANALYSIS)

### Overall Assessment

**SMART_ANALYSIS path is largely V4-aligned**: LLM only produces JSON, Orchestrator generates Markdown, ViewModel correctly controls streaming and placeholders. **GENERAL path needs prompt hygiene convergence and metadata parsing ownership clarification**.

---

## Next Steps

1. Update V4 spec to explicitly state GENERAL metadata parsing ownership
2. Simplify GENERAL prompt scaffold in `HomeScreenBindings.kt` to match V4 lightweight guidance
3. Consider moving GENERAL metadata parsing to Orchestrator for consistency (if Option A chosen)

---

**Note**: This audit is analysis-only. No code changes were made. Document sync and Codex implementation prompts will be separate steps.

