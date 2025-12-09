---

# Orchestrator–MetaHub V4.1 (Spec)

> **Goal**
>
> * Eliminate SMART_ANALYSIS template leakage / verbose / duplicated output.
> * Make the boundaries crystal clear:
>
>   * **LLM only produces JSON**
>   * **Orchestrator formats user-facing text**
>   * **UI/ViewModel handles streaming & placeholders**
>
> V3 stays as historical reference; this document **overrides V3 wherever they conflict about SMART_ANALYSIS**.

---

## 0. Roles & Responsibilities

### LLM

* Acts as a **metadata engine**, not a layout engine:

  * **GENERAL chat**: returns natural-language text; may optionally append a JSON metadata block at the end.
  * **SMART_ANALYSIS**: returns **a single JSON object only** – no Markdown, no explanation, no examples.

### Orchestrator (Home / Transcript layers)

* Responsible for:

  * Building LLM requests (prompt + history).
  * Parsing JSON returned by the LLM.
  * Constructing and upserting `SessionMetadata` into MetaHub.
  * For SMART_ANALYSIS **only**: locally assembling the Markdown text to show in the chat bubble based on `SessionMetadata`.
* Does **not** decide UI layout; it only decides “what this assistant message’s text should be”.

### UI / ViewModel

* Responsible for:

  * Sending requests (text / quickSkill / audio-triggered analysis).
  * Controlling whether tokens are streamed into the UI:

    * GENERAL → streamed.
    * SMART_ANALYSIS → **not** streamed; only final text.
  * Showing local placeholders (“Analyzing…”) and replacing them on completion.
  * Rendering the final text from the Orchestrator.
* Does **not** parse complex JSON templates and **never** shows raw JSON to the user.

---

## 1. Two Main Pipelines

### 1.1 GENERAL Chat Pipeline

#### (1) Prompt rules

For GENERAL chat first replies:

* It’s allowed to **optionally** request a JSON metadata block, but must follow:

* ✅ **Lightweight guidance only**:

  * 保持简短的自然语言约束（短段落即可，避免长篇），可附带 **一个精简的 JSON 示例 + 简短字段说明**，提示可选的元数据格式。
  * 示例意图：在对话回答后，如能识别元数据，可追加一个小型 JSON；不要把 JSON 说明写成大纲或多页模板。

* ✅ **JSON instructions must be short**:

  * Only describe field names and meanings.
  * Do **not** embed a full JSON skeleton or multi-line boilerplate beyond a compact example.

* ❌ **Forbidden**:

  * Long outline / scaffold like:

    * “First output ‘Conversation Summary’, then ‘Customer Persona’, then ‘Needs & Pain Points’…”
  * Stepwise “fill-in-the-template” instructions that naturally cause:

    * `## Section`
    * `## Section` + more detail
    * `## Section` + even more detail…
  * Pasting a full JSON template (with all fields and placeholder text) into the prompt for the model to “copy & fill”.
  * 在 GENERAL prompt 中加入类似 SMART 的多分节分析骨架（例如完整的「客户画像 / 需求 / 机会与风险 / 建议与行动 / 核心洞察」模板）。

> Design principle:
> The first GENERAL assistant reply should **look like a human answer**,
> and the JSON is just a **bonus machine-readable summary**, not the driver of the UI structure.

#### (2) Request construction

* `HomeScreenViewModel.sendMessageInternal`:

  * `quickSkillId = null`.
  * Builds `ChatRequest` with:

    * history
    * `isFirstAssistantReply` flag.

#### (3) Orchestrator behavior

* `HomeOrchestratorImpl.streamChat(request)` for GENERAL:

  * Acts as a **transparent pass-through**:

    * Does **not** rewrite text.
  * May tag internal source (e.g. `GENERAL_FIRST_REPLY`), but doesn’t change the message content.

#### (4) UI / ViewModel streaming behavior

* In `startStreamingResponse` for GENERAL:

  * For each `ChatStreamEvent.Delta`:

    * Append tokens to the current assistant bubble (the “Lego stacking” model).
  * On `ChatStreamEvent.Completed`:

    * Run `sanitizeAssistantOutput` for **light** cleanup:

      * Remove any trailing JSON.
      * Fix simple numbering (1, 2, 3…).
      * Strip obvious echoes of system instructions / prompt templates if they slip through.

#### (5) Metadata parsing

* Only for the **first** GENERAL assistant reply:

  * **Responsibility**: ViewModel extracts JSON and writes to MetaHub (unlike SMART_ANALYSIS which is handled by Orchestrator).
  * Extract the **last** JSON block from the raw assistant text:

    * Last fenced or bare JSON object.
  * Use a tolerant parser to read:

    * `main_person`
    * `short_summary`
    * `summary_title_6chars`
    * `location`
  * If at least one field is valid:

    * Build `SessionMetadata` and `upsert` into MetaHub.
    * If the current title is a placeholder, allow **one-time auto rename**.

* UI continues to display the cleaned natural-language text; **JSON is never shown**.

> **Note**: GENERAL metadata parsing is handled by ViewModel (e.g., `handleGeneralChatMetadata`) rather than Orchestrator. This differs from SMART_ANALYSIS where Orchestrator owns all JSON parsing and MetaHub writes. This separation is intentional: GENERAL is lightweight and optional, while SMART_ANALYSIS requires strict JSON-only output and centralized formatting.
>
> 当前责任分层：
> * SMART_ANALYSIS：LLM 仅出 JSON，由 Home Orchestrator 解析 → 写入 MetaHub → 本地拼 Markdown，UI 只展示 Markdown。
> * GENERAL 首条回复：可选 JSON 尾巴由 Home 层 ViewModel 的元数据 helper（如 `handleGeneralChatMetadata`）解析并写入 MetaHub，UI 不展示 JSON。
> * 如需进一步下沉，可在未来把 GENERAL 首条回复的 JSON 解析迁移到 Orchestrator，但 V4 不强制。

---

### 1.2 SMART_ANALYSIS Pipeline (Text & Audio)

> This is the **big divergence** from V3.
> V4 rules below are the **only authoritative** ones for SMART_ANALYSIS.

#### (1) Prompt rules – JSON-only

For SMART_ANALYSIS, the prompt must ensure:

* The model’s output is:

  * **Exactly one top-level JSON object**
  * **No extra natural language / Markdown** around it.

* JSON fields:

  * Required:

    * `main_person`: string
    * `short_summary`: string
    * `summary_title_6chars`: string
    * `location`: string
    * `highlights`: string[]
    * `actionable_tips`: string[]
    * `core_insight`: string
    * `sharp_line`: string

  * Optional:

    * `stage`: string
    * `risk_level`: string

* Prompt may include **short comments** explaining each field, but must **not**:

  * Output multiple JSON documents.
  * Output extra explanation like “Here is your JSON: …”.
  * Output Markdown headings or formatted sections.

> For the LLM, SMART_ANALYSIS =
> “Take this content and turn it into one structured JSON object. Nothing else.”

#### (2) Request construction

* Text entry (home quick skill):

  * `quickSkillId = "SMART_ANALYSIS"`.

* Audio entry (“analyze this call”):

  * After transcription is ready, send a SMART_ANALYSIS request:

    * Content = transcript + any necessary context (meeting type, roles, etc.).
    * `quickSkillId = "SMART_ANALYSIS"`.
    * `source = SMART_ANALYSIS_AUTO` if auto-triggered.

#### (3) UI / ViewModel streaming behavior (critical)

In `startStreamingResponse`:

* When `quickSkillId == SMART_ANALYSIS`:

  * **Delta events must not be rendered to the UI**:

    * Do **not** append tokens into any user-visible bubble.
    * Do **not** show any partial JSON or partial scaffolding.
  * Immediately insert a local placeholder message, e.g.:

    * “正在智能分析当前会话内容…” / “Running smart analysis on this conversation…”
  * Wait for `ChatStreamEvent.Completed`; once received, replace the placeholder text with the final Markdown from the Orchestrator.

> Formal requirement:
>
> * When in SMART_ANALYSIS mode, the ViewModel **must ignore all `ChatStreamEvent.Delta` events** for that request.
> * Only the `Completed.text` from the Orchestrator is allowed to be rendered.

#### (4) Orchestrator behavior

In `HomeOrchestratorImpl.streamChat` for SMART_ANALYSIS:

1. Call the LLM (internally may be streaming or not; the Orchestrator hides this from the UI).

2. From the returned text, extract the **last JSON object**:

   * If the prompt was accidentally violated and there is extra text, ignore everything except the last JSON.

3. Parse using strict JSON (e.g. `JSONObject`):

   * For each field, use a **“last value wins”** strategy:

     * If a field appears multiple times, keep the last one.
     * Missing fields are allowed to be null / empty.

4. Build `SessionMetadata`:

   * `mainPerson`
   * `shortSummary`
   * `summaryTitle6Chars`
   * `location`
   * `stage`
   * `riskLevel`
   * `tags` ← union of `highlights + actionable_tips`
   * `crmRows` (still empty list for now)
   * `latestMajorAnalysis*` timestamps, with source depending on:

     * user-triggered vs auto-triggered (SMART_ANALYSIS_USER / SMART_ANALYSIS_AUTO)

5. Upsert into MetaHub via `upsertSession`.

6. Build an internal `SmartAnalysisResult` / `ParsedSmartAnalysis`, then generate the final user-facing Markdown:

   Recommended section structure (configurable):

   1. `## Conversation Summary`

   2. `## Customer Persona & Intent`

   3. `## Needs & Pain Points`

   4. `## Opportunities & Risks` (only if there is relevant content)

   5. `## Recommendations & Next Actions`

      * numbered list 1..n from `actionable_tips`.

   6. `## Core Insight`

   7. `## One-line Pitch / Sharp Line`

   * Only render sections that have content.
   * **Do not** render empty headings or placeholder labels.

7. Return this Markdown as a `ChatStreamEvent.Completed(text = markdown)` to the ViewModel.

#### (5) Final UI behavior

* When the ViewModel receives the SMART_ANALYSIS `Completed`:

  * Replace the placeholder bubble text with the orchestrator-formatted Markdown.
  * Do **not** attempt further JSON parsing on that text (because the JSON was never sent to the UI).
  * Still mark analysis source (`SMART_ANALYSIS_USER` / `SMART_ANALYSIS_AUTO`) via the existing “latest analysis” marker paths.

---

## 2. MetaHub Update Rules

### Entity: `SessionMetadata`

Key fields for these pipelines:

* `sessionId`
* `mainPerson`
* `shortSummary`
* `summaryTitle6Chars`
* `location`
* `stage`
* `riskLevel`
* `tags` (from `highlights` & `actionable_tips`)
* `crmRows` (future)
* `latestMajorAnalysisMessageId`
* `latestMajorAnalysisAt`
* `latestMajorAnalysisSource` ∈ { `SMART_ANALYSIS_USER`, `SMART_ANALYSIS_AUTO`, `GENERAL_FIRST_REPLY`, … }

### Write rules

#### GENERAL first reply

* If JSON parsing succeeds and at least one field has a value:

  * Call `upsertSession`:

    * Non-empty new values overwrite old.
    * Empty / null fields keep existing values.
  * If the current title is a placeholder:

    * Allow a **single** auto-rename based on the new metadata.

#### SMART_ANALYSIS

* If JSON completely fails to parse:

  * **Do not write** anything to MetaHub.
  * **Do not update** title or analysis markers.
  * UI should show a simple failure message (see next section).

* If JSON partially parses:

  * Write the available fields.
  * Leave missing ones as-is (keep previous values).
  * Update `latestMajorAnalysis*` to reflect this analysis’ time & source.

---

## 3. Error & Fallback Behavior

> Goal: it is always better to show a **short “analysis failed/partial” message**
> than to show half-broken JSON / template junk to the user.

### (1) SMART JSON completely fails

Examples:

* Response is not valid JSON at all.
* JSON parses but **all** key fields are null/blank.

Behavior:

* MetaHub:

  * Do nothing (no `SessionMetadata` changes).
* UI:

  * Replace the placeholder message with something like:

    * “Smart analysis failed this time. Please try again later or simplify the input.”
  * Do **not** show any JSON or half-baked Markdown.

### (2) SMART JSON partially succeeds

Examples:

* Only `main_person` and `short_summary` have values; arrays are empty.

Behavior:

* MetaHub:

  * Upsert only non-empty fields; keep old values for others.
* UI Markdown:

  * Render only sections that have content.
  * If `actionable_tips` is empty:

    * Skip the “Recommendations & Next Actions” section entirely instead of showing an empty heading.

### (3) GENERAL JSON fails

* UI still shows the full natural-language answer.
* MetaHub is not updated for that reply.
* No title change.

---

## 4. Tingwu / Transcript Integration (Current & Future)

### Current state

* Tingwu provides:

  * diarized transcript segments.
  * its own summaries / chapters / analyses (audio-oriented).
* Current implementation:

  * Parses Tingwu JSON → builds `DiarizedSegment`.
  * Performs **speaker renaming**:

    * “Speaker 1” → “Mr. Luo (Client)”
    * “Speaker 2” → “Sales Consultant”
  * Does **not** write those summaries directly into `SessionMetadata` (they are just UI content for now).

### V4 Direction (planned, not yet implemented)

* Tingwu outputs (summary/analysis) can become an additional metadata source:

  * e.g. write into a `TranscriptMetadata` entity in MetaHub, or
  * merge selectively into `SessionMetadata` (with clear precedence rules).

* SMART_ANALYSIS remains independent:

  * Always uses the JSON-only LLM → Orchestrator Markdown → MetaHub path.
  * Focuses on **sales-centric** insight and next actions.

> V4 does **not** change Tingwu implementation yet.
> It only documents **separation of responsibilities** and a **future path** for merging.

---

## 5. Compatibility & Rollout

1. **GENERAL** behavior remains as in V3, **with stricter prompt hygiene**:

   * First-reply prompt must not use long scaffolds or templates.
   * Only lightweight JSON guidance is allowed.

2. **SMART_ANALYSIS** must fully adopt V4 behavior:

   * JSON-only output from the LLM.
   * Orchestrator parses JSON and builds Markdown.
   * ViewModel:

     * Ignores all deltas for SMART.
     * Shows a local placeholder.
     * Replaces it with the orchestrator Markdown on completion.

3. Any UI / ViewModel code:

   * MUST NOT show JSON directly to users.
   * MUST NOT append SMART_ANALYSIS deltas into a chat bubble.

4. V3 doc stays in the repo but should be **clearly marked as historical**.
   When in doubt about SMART_ANALYSIS, **this V4.1 spec is the source of truth.**

5. 实现现状提示：

   * 当前 ViewModel 仍在 SMART_ANALYSIS Completed 文本上尝试再解析元数据（若无 JSON 将跳过），MetaHub 入库以 Orchestrator 解析为准。
   * GENERAL 首条回复仍有历史 scaffold，后续应按 V4"轻量提示"收敛。
   * GENERAL 元数据解析职责：ViewModel 负责（`handleGeneralChatMetadata`），与 SMART_ANALYSIS（Orchestrator 负责）分离。此设计是故意的：GENERAL 是轻量且可选的，SMART_ANALYSIS 需要严格的 JSON-only 输出和集中格式化。

---
