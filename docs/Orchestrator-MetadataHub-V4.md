
# Orchestrator–MetaHub V4 规范（v4.3.0）

> **状态 / Status**  
> - 当前唯一有效规范（Active spec）。  
> - V3 文档已归档，仅作历史参考，不得作为实现依据。  
>
> **文档版本 / Doc version**  
> - Version: 4.3.0  
> - Last updated: 2025-12-11

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

## 0.x 文档版本与适用范围

本节说明“V4 规范”的版本号含义，避免 Codex / Orchestrator 混淆历史行为。

- **主版本（4）**：表示 Orchestrator–MetaHub V4 代。只要主版本仍为 4，就不回退到 V3 行为。
- **次版本（4.M）**：表示行为/合约级别变更，例如：
  - 新增/调整 SMART_ANALYSIS JSON 字段或语气规范；
  - 重新定义 HomeOrchestrator、MetaHub、ExportOrchestrator 的职责边界；
  - 修改 UI 必须遵守的流转规则（如占位气泡、错误文案）。
- **补丁版本（4.M.m）**：仅文档澄清、示例补充、排版/术语统一，不改变行为。

工程协作约定：

- Orchestrator / Codex 在实现时以**当前文件顶部的版本号**为准，忽略 V3 及更早文档。
- 若实现时发现行为与本规范不一致，优先认定为“实现待修复”，而非“V4 规范只是建议”。
- 历史变更详情参见文末《附录 A：变更记录（仅供追溯，不具备约束力）》。
- **规则**：每次 doc-sync 修改本文件，必须同步更新顶部版本号 + 附录 A 变更记录。

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

### 用户画像（Persona）输入来源

- Persona 来自用户 Profile/Onboarding，仅作为 system prompt 的上下文配置，不写入 `SessionMetadata`：
  - 岗位/职位：销售新人、客户经理、大客户经理、解决方案顾问等。
  - 所属行业：汽车、制造、软件、医疗等。
  - 主要沟通渠道：微信+电话、邮件为主、线下会议为主等。
  - 经验水平：新手、2–5 年、资深。
  - 表达风格偏好：偏正式商务、偏口语。
- 作用：
  - 用于构造 LLM persona/system prompt，影响 GENERAL/SMART 的语气、示例话术风格。
  - **不改变** JSON schema、本地 Orchestrator 逻辑或 MetaHub 字段结构，视为“上下文配置”而非“会话业务元数据”。
- Persona（SalesPersona）来源：
  - 由 Onboarding / 用户中心采集并存储在 Profile；
  - Profile → MetaHub 同步出一份 UserMetadata（用户级画像）供分析使用；
  - Orchestrator 只消费 LLM JSON & SessionMetadata，不写入或修改 Persona/UserMetadata。

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

> **约束**：GENERAL 首条回复的 JSON schema **只描述会话/客户相关信息**，不包含销售用户的 Persona 字段（role / industry / experience 等）。这些 Persona 字段来自 Onboarding / 用户中心，不由 LLM 填写。

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

### SMART_ANALYSIS 人设与视角（Persona & POV）

- 系统人设：**AI 销售助手**，面向销售同学复盘与客户的对话/邮件/会议，不扮演真实销售本人。
- 视角约定：
  - 优先使用**第二人称 + 角色名**：`你作为销售顾问… / 你在本次对话中…`
  - 也可以使用**第三人称**：`销售顾问在本次通话中向客户介绍了…`
  - **禁止**用第一人称“我”来扮演销售本人（例如：`我在电话里向客户介绍了…`，其中“我”指销售）。
- SMART_ANALYSIS 输出的是**对对话的分析报告卡片**，而不是销售本人写的复盘日记，更不是继续和客户聊天。

字段口吻（在 JSON 字段说明中遵守）：

- `short_summary`：1–2 句总结，语气为 AI 帮销售复盘，例如：`你本次与张总主要沟通了…` 或 `销售顾问向客户介绍了…`，不要写成销售第一人称经历。
- `highlights`：亮点要点数组，通常 2–4 条，每条 1 句描述销售做得好的地方，例如：`你及时捕捉到客户关心的…`；不要写成“我向客户展示了…”.
- `actionable_tips`：后续建议数组，通常 2–4 条，每条 1 句，用建议口吻对销售说话，例如：`建议：后续可以安排一次试驾…`。
- `core_insight` / `sharp_line`：保持助理视角或中立描述，避免“我=销售”。

#### Conciseness & no duplication

- `short_summary`：1–2 句，优先说明“这次沟通主要解决了什么 / 下一步要做什么”，不要复述整段对话。
- `highlights` / `actionable_tips` 等数组字段：每个数组建议 2–3 条，要点最多不超过 5 条；每条尽量 1 句，说清一个点即可。
- 避免在不同字段之间重复同一条信息（例如同一“亮点”不要同时出现在 `highlights` 和 `actionable_tips` 中）。
- 不要输出长篇段落或包含二级小标题的 Markdown 文本；结构由 Orchestrator 在本地拼装。

* Prompt may include **short comments** explaining each field, but must **not**:

  * Output multiple JSON documents.
  * Output extra explanation like “Here is your JSON: …”.
  * Output Markdown headings or formatted sections.

> For the LLM, SMART_ANALYSIS =
> “Take this content and turn it into one structured JSON object. Nothing else.”

> **约束**：SMART_ANALYSIS 的 JSON schema **只描述会话/客户相关信息**，不包含销售用户的 Persona 字段（role / industry / experience 等）。这些 Persona 字段来自 Onboarding / 用户中心，不由 LLM 填写。

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

#### SMART Markdown hygiene（仅 SMART_ANALYSIS）

- SMART 模式下，Orchestrator 负责将单个 JSON 对象转换为**可阅读的分析卡片**，而不是原样把 LLM 的长文塞给 UI。
- 建议（可用简单 heuristics 实现）：
  - 每个分节（需求与痛点 / 机会与风险 / 建议与行动等）默认展示 2–3 条要点，最多不超过 5 条。
  - 对明显重复或高度相似的要点做去重；同一条 insight 不要在多个分节重复出现。
  - 规范编号：若分节使用有序列表，最终渲染为连续的 `1, 2, 3...`；避免 `1 3 4 4`、`11)1)` 等累积痕迹。
  - 若某个分节没有任何有意义内容，则整节隐藏（不渲染空标题）。
- 这些清理逻辑属于 Orchestrator 责任层，不应该放到 ViewModel / UI 中。

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

### 用户级元数据（UserMetadata / SalesUserMeta）

除了会话级的 SessionMetadata 以外，MetaHub 还维护一份**用户级画像**：

- 粒度：按 userId 存储，1 个销售用户对应 1 条记录；
- 字段来源：来自 Onboarding / 用户中心表单，而不是 LLM；
- 代表含义：销售角色画像（SalesPersona），例如：
  - `role`：销售顾问 / 客户经理 / 大客户经理 / ……
  - `industry`：所在行业（汽车 / SaaS / 制造 / ……）
  - `mainChannel`：主要沟通渠道（电话 / 微信+线下 / 邮件 / ……）
  - `experienceLevel`：经验层级（新手 / 有一定经验 / 资深）
  - `stylePreference`：表达风格偏好（偏正式 / 偏口语）

**注意：**

- UserMetadata 是 Profile / Onboarding 的镜像，用于分析与导出；
- LLM 不负责生成或修改 UserMetadata；
- UserMetadata 与 SessionMetadata 分层存储，不混表、不混 schema。

#### 数据来源与职责边界补充

- **UserMetadata（用户级）**
  - 来源：Onboarding / 用户中心 → Profile 服务；
  - 同步：由 Profile → MetaHub 的一条单向同步链路（非 LLM）；
  - 用途：人群分析、分群运营、导出报表中拼接“销售画像”信息。

- **SessionMetadata（会话级）**
  - 来源：LLM JSON（SMART_ANALYSIS / GENERAL_FIRST_REPLY / 将来 Tingwu）；
  - 只描述**本次会话 / 录音对应的客户场景**；
  - 不包含销售本人角色画像字段。

### Write rules

#### GENERAL first reply

* If JSON parsing succeeds and at least one field has a value:

  * Call `upsertSession`:

    * Non-empty new values overwrite old.
    * Empty / null fields keep existing values.
  * If the current title is a placeholder:

    * Allow a **single** auto-rename based on the new metadata.
* 会话的**第一条助手回复（GENERAL）**应尽量在尾部输出一个 JSON 对象，用于命名与摘要；信息不足时可用占位值：
  * `main_person`: `"未知客户"`
  * `short_summary`: `"信息不足，需要补充细节"`
  * `summary_title_6chars`: `"未命名会话"`
  * `location`: `"未知"`
* JSON 仍遵守 V4 约束：最多一个对象，只能在最后一行，之后不再有自然语言，用户看不到；纯问候/噪音场景也可用占位 JSON 触发一次性自动命名。若确实无法判断，可不输出，但对首条回复产品期望**优先尝试占位 JSON**。

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

### 重命名与深度分析的元数据边界

- **重命名/列表摘要元数据**：
  - 主要来自 `GENERAL_FIRST_REPLY` 的 JSON。
  - 提供 `mainPerson`、`shortSummary`、`summaryTitle6Chars`，即使是占位值（未知客户/未命名会话）也可接受。
  - 用途：占位标题 → 一次性自动改名；历史列表 snippet。
- **深度销售分析元数据**：
  - 主要来自 SMART_ANALYSIS、Tingwu 等路径，包含 stage/risk/highlights/actionable_tips 等。
  - 用于分析依据、HUD/调试面板、导出头部信息、后续自动化分析。
- **合并策略**：
  - `GENERAL_FIRST_REPLY` 负责“早期命名 + snippet”，SMART/Tingwu 负责“后续深度分析”。
  - 后者不应无意义覆盖前者命名字段，除非策略明确允许。

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

## 附录 A：变更记录（仅供追溯，非规范）

> ⚠️ **说明（给 Orchestrator / Codex）**  
> - 本附录仅用于追溯“为什么当时这么改”，**不具备规范约束力**。  
> - 在推导当前行为时，请只以正文章节为准；除非用户明确询问历史行为，否则不需要阅读本附录。

### v4.3.0（2025-12-11）

- 扩展 MetaHub 为双切片架构（SessionMetadata + UserMetadata）。UserMetadata 由 Profile/Onboarding 同步而来，用于分析与导出；LLM JSON schema 明确不包含 Persona 字段。

### v4.1.0（2025-12-10）

- **Persona / POV 调整**
  - 明确 SMART_ANALYSIS 人设为「AI 销售助手」，面向销售同学复盘，不扮演真实销售本人。
  - JSON 字段与 Markdown 示例统一使用“你/销售顾问”或第三人称描述，禁止“我=销售”视角。
- **SMART 卡片长度与去重规则**
  - 增加对每个分节的推荐长度（每节 2–3 条要点，最多 5 条）。
  - 明确 Orchestrator 负责去重、编号修复与隐藏空分节，ViewModel 不对 SMART 文本做复杂清理。
- **实现层指引补充**
  - 约定 SMART_ANALYSIS 的 `ChatStreamEvent.Completed.fullText` 必须是已清理好的分析卡，GENERAL 继续使用 `sanitizeAssistantOutput`。
  - 对 GENERAL / SMART 清理职责分别划归 ViewModel / Orchestrator。

### v4.0.0（2025-xx-xx）

- 初版 V4 规范：定义 JSON-only SMART_ANALYSIS、MetaHub SessionMetadata 结构、HomeOrchestrator 责任，以及与 V3 的职责切换。
- 明确 V3 文档归档，仅作历史背景参考。

### v4.2.0（2025-12-10）

- Persona 输入来源：明确来自 Onboarding/Profile，仅用于 system prompt，非 SessionMetadata。
- GENERAL 首条回复：首条助手回复鼓励输出占位 JSON 尾巴（未知客户/未命名会话等），用于一次性自动命名；重申单行单对象约束。
- 元数据边界：区分 `GENERAL_FIRST_REPLY`（早期命名+snippet）与 SMART/Tingwu（深度分析），避免无意义覆盖。

---
