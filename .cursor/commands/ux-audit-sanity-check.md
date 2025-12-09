Mode: Orchestrator–MetaHub Pipeline Audit (read-only, no code changes)

You are auditing the Assistant app’s analysis pipelines (GENERAL chat, SMART_ANALYSIS, and Tingwu/transcript flows).

Scope

Focus on how LLM output flows through Orchestrator, MetaHub, and Home UI, specifically:

GENERAL chat first reply: prompt, streaming, JSON metadata, MetaHub, title.

SMART_ANALYSIS (quick skill + audio-triggered): JSON-only behavior, orchestration, formatting, metadata, UI placeholder.

Tingwu / transcript → chat: how transcription, summaries, and smart analysis interact with MetaHub and sessions.

Title / HUD / export: how they consume SessionMetadata and latest analysis markers.

Operation mode: INSPECT and REPORT only.
No file modifications. No refactors. No fix suggestions. You may explain why something is wrong, but do not describe how to change the code.

Sources of truth (read these first)

Treat these docs as authoritative, in this priority for pipeline & behavior:

docs/Orchestrator-MetadataHub-V4.md (or Orchestrator-MetadataHub-V4.1.md) – final word for SMART_ANALYSIS, and high-level rules for GENERAL & Tingwu.

docs/ux-contract.md – what the user should see (no raw JSON, how SMART results look, where titles/HUD/exports get their data).

docs/api-contracts.md – contracts for ExportOrchestrator, MetaHub, etc.

docs/tingwu-doc.md – how Tingwu/transcript flows are supposed to work at a high level.

Orchestrator-MetadataHub-Mvp-V3.md is historical; only use it to understand legacy behavior. Wherever it conflicts with V4 for SMART_ANALYSIS, V4 wins.

Also consult AGENTS.md / role-contract.md only for agent responsibilities and testing discipline, not behavior spec.

Step 1 – Extract pipeline rules from docs

From the docs above, for each pipeline, extract the concrete rules that are currently in force:

GENERAL chat (quickSkillId = null)

Prompt constraints (no long scaffolds; optional small JSON at end only).

Streaming behavior (deltas visible, light cleanup on completion).

First-reply JSON metadata: which fields, how to parse, when to write MetaHub, one-shot title rename.

SMART_ANALYSIS (text + audio-triggered)

LLM output must be JSON-only (single object, no Markdown/explanation).

Orchestrator responsibilities: parse last JSON, build SessionMetadata, set latestMajorAnalysis*, construct human Markdown sections, never send JSON to UI.

UI/ViewModel responsibilities: ignore all SMART deltas, show local “analyzing…” placeholder, replace it only with orchestrator-formatted Markdown on Completed.

Error paths: JSON failure → no MetaHub write, short failure message; partial JSON → only sections with content rendered.

Tingwu / transcript audio flows

How transcripts are built (speaker diarization / renaming), what Tingwu summaries/analysis are for.

Which parts (if any) should feed MetaHub now vs “future plan”.

How “用 AI 分析本次通话” should route into SMART_ANALYSIS (session creation/selection, analysis source markers).

Title / HUD / export integration

Session title lifecycle: placeholder → one-shot auto-title from MetaHub → no further auto-changes after non-placeholder.

HUD fields: which bits of SessionMetadata / debug info should appear.

Export filenames: <Username>_<major person>_<summary6>_<timestamp> and how they depend on MetaHub + profile.

Summarize these as 10–20 bullets under “Pipeline rules (from docs)”, grouped by pipeline (GENERAL / SMART / Tingwu / Titles & Export).
Keep each bullet behavior-focused, not pseudo-code.

Step 2 – Inspect implementation (read-only)

Carefully read these Kotlin files (no edits):

Orchestrator & bindings

feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeOrchestratorImpl.kt

feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenBindings.kt

Home ViewModel & UI

feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt

feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt

MetaHub & session metadata

MetaHub interface + implementation (core/metahub/... – e.g. MetaHub, SessionMetadata, TranscriptMetadata).

Session & history repositories

AiSessionRepository (feature/chat), any Room/in-memory impls.

History / chat history repos that persist messages and summaries.

Exports

data/aicore/ExportOrchestrator.kt and its real implementation.

Audio / Tingwu

Audio library & transcription coordinator (audio files routes).

Tingwu coordinator / transcript pipeline (RealTingwuCoordinator or similar).

Any orchestration code that triggers SMART_ANALYSIS from an audio transcription.

Tests / fixtures

Orchestrator tests: HomeOrchestratorImplTest (and any SMART/GENERAL tests).

Metadata/title/Export tests that simulate JSON responses.

Use ripgrep (or equivalent) to search for terms derived from the rules, for example:

SMART_ANALYSIS, quickSkillId, SmartAnalysisResult, ParsedSmartAnalysis.

main_person, short_summary, summary_title_6chars, highlights, actionable_tips.

SessionMetadata, latestMajorAnalysisSource, SMART_ANALYSIS_USER, SMART_ANALYSIS_AUTO.

sanitizeAssistantOutput, handleGeneralChatMetadata, handleSmartAnalysisMetadata.

Tingwu, TranscriptMetadata, any transcript-related orchestrator.

Export methods: exportPdf, exportCsv, ExportOrchestrator.

Important:

Don’t rely on my field/class names as exact; if they differ in code, use the real ones.

Do not change any code; your job here is to map behavior.

Step 3 – Map rules to code (per pipeline)

For each pipeline rule you extracted in Step 1 that should be enforced by the code:

Locate the corresponding implementation (if any) in the inspected files.

Decide one of:

OK – code clearly implements the rule.

MISMATCH – … – code diverges, is missing, or still uses legacy behavior.

UNKNOWN – … – not enough evidence in code to confirm or deny.

When you mark MISMATCH, always include:

file:line (or small range)

1–2 sentences explaining the discrepancy in plain behavior terms, e.g.:

“SMART_ANALYSIS deltas are still appended to the assistant bubble (streamed), but V4.1 requires deltas to be ignored and only final Markdown to be shown.”

“The SMART prompt still embeds a long Markdown scaffold and JSON template, which violates the V4.1 JSON-only contract.”

Group your findings under these headings:

GENERAL chat

SMART_ANALYSIS (text)

SMART_ANALYSIS (audio / Tingwu-triggered)

MetaHub & titles/HUD

Exports

Tingwu / transcript integration

Under each heading, list bullets of the form:

**OK** – [short rule label] – (file:line) optional note

**MISMATCH – [short rule label] – file:line – explanation

**UNKNOWN – [short rule label] – why you can’t confirm from code

Do not propose code changes; just describe what the code currently does and how that compares to the spec.

Step 4 – Symptom mapping (optional but important)

If you find MISMATCHes that could explain known issues (like we’ve seen manually), call them out explicitly. For example:

First-round SMART_ANALYSIS replies show verbose template-like text, repeated bullet headings, or partial lines.

Audio “smart analysis” crashes or behaves differently from text-triggered SMART_ANALYSIS.

GENERAL chat first reply still leaks scaffold headings into the answer.

Titles/HUD/exports don’t update after analysis, or update inconsistently.

For each such symptom, add bullets under:

“Likely causes of visible issues”, e.g.:

“SMART_ANALYSIS deltas are still streamed into the bubble (HomeScreenViewModel.kt:…), so the user sees incremental scaffolding lines instead of only the final orchestrator Markdown.”

“Audio-triggered analysis bypasses the SMART orchestrator path and renders raw LLM text (SomeAudioOrchestrator.kt:…), which explains why audio smart analysis still shows JSON or template echoes.”

Keep this section short and focused on cause → symptom links.

Output format

Header

Current branch name and HEAD commit hash (from git status / git log -1 --oneline).

Pipelines under audit (one line), e.g. “GENERAL, SMART_ANALYSIS, Tingwu/transcript, MetaHub, Export”.

Pipeline rules (from docs)

10–20 bullets grouped by pipeline (GENERAL / SMART / Tingwu / Titles & Export) summarizing the relevant rules.

Findings by pipeline

Under each heading (GENERAL / SMART / Tingwu / etc.), bullets in the OK / MISMATCH / UNKNOWN format described above.

Likely causes of visible issues (optional)

Only if there are clear MISMATCHes that explain real UX issues (verbose first round, template leakage, audio analysis crashes, title/HUD desync).

Important:

Do not propose or describe code changes in this audit.

Do not silently ignore discrepancies; every broken rule must surface as MISMATCH or UNKNOWN.

Always treat Orchestrator-MetadataHub-V4.x as the final word for SMART_ANALYSIS behavior when it conflicts with legacy code or older docs.