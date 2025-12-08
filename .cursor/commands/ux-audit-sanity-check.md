
> **Mode: UX Read-Only Audit (no code changes)**
>
> You are auditing the Android AI assistant app.
>
> ### Scope
>
> * **Screen / flows under audit:**
>   e.g. “Home chat shell, title pipeline, HUD, and history drawer.”
> * **Operation mode:** **INSPECT and REPORT only. No file modifications. No fix suggestions.**
>
> ### Sources of truth (read these first, every time)
>
> Treat these as the **only** authoritative UX sources, with this priority:
>
> 1. `docs/assistant-ux-contract.md` – highest priority for UI/UX structure and behavior.
> 2. `docs/Orchestrator-MetadataHub-Mvp-V3.md` – orchestration + metadata + title pipeline rules.
> 3. `api-contracts.md` – any UI-visible behavior implied by API contracts.
> 4. `style-guide.md` – visual/styling principles only; do **not** override behavior from the UX contract.
>
> Legacy React UI, old UX docs, and archived projects are **not** sources of truth. They may be used only as vague inspiration if explicitly referenced, never to overrule the contract above.
>
> ### Step 1 – Extract UX rules from docs
>
> From the 4 docs above, **for this screen/flow only**, extract the concrete UX rules that are currently in force. For example:
>
> * Top bar composition and behavior (title source, icon roles, HUD entry, new-chat, profile).
> * Layout principles for the main body (what appears, in what order, what is intentionally *not* shown).
> * HUD behavior and shape (how it’s toggled, what it shows, how it’s dismissed, copy behavior).
> * Session title lifecycle (one-time auto rename from metadata / resolver; user ownership afterwards).
> * History drawer structure and mapping to session summaries.
> * Keyboard / gesture guardrails if they are documented.
>
> Summarize these rules as **5–15 bullets** under `“UX rules (from docs)”`.
> Keep it high-level and behavior-focused (no pseudo-code).
>
> ### Step 2 – Inspect implementation (read-only)
>
> Carefully read these Kotlin files (no edits):
>
> * `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt`
> * `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
> * `app/src/main/java/com/smartsales/aitest/AiFeatureTestActivity.kt`
> * Any other composables/activities related to this scope (find them with ripgrep for relevant tags/text from the UX rules).
>
> Use ripgrep (or equivalent) to search for **terms derived from the rules**, such as:
>
> * Titles/labels mentioned in the docs
> * TestTags referenced in the UX contract
> * HUD tags or identifiers
> * Metadata/title pipeline symbols (`SessionMetadata`, title resolver, etc.)
>
> Do not rely on a fixed, hard-coded list of strings; derive your queries from the current docs.
>
> ### Step 3 – Map rules to code
>
> For **each UX rule** you extracted in Step 1 that should be visible in this screen/flow:
>
> * Locate the corresponding implementation (if any) in the inspected files.
> * Decide:
>
>   * **OK** – code clearly implements the rule.
>   * **MISMATCH – …** – code diverges, is missing, or still uses legacy behavior.
>   * **UNKNOWN – …** – not enough evidence in code to confirm or deny.
>
> When you mark **MISMATCH**, always include:
>
> * `file:line` (or a small line range)
> * 1-sentence explanation of the discrepancy.
>
> ### Output format
>
> 1. **Header**
>
>    * Current branch name and HEAD commit hash (from git status / log).
>    * Screen/flow under audit (one line).
> 2. **UX rules (from docs)**
>
>    * 5–15 bullets summarizing the relevant rules you extracted.
> 3. **Findings by rule**
>    For each rule, one bullet:
>
>    * `**OK** – [short rule label] – (file:line) optional note`
>    * `**MISMATCH – [short rule label] – file:line – explanation`
>    * `**UNKNOWN – [short rule label] – why you can’t confirm from code`
> 4. **Likely causes of visible issues (optional)**
>    Only if there are MISMATCHes that directly explain things like:
>
>    * Top bar still showing legacy “AI 助手” instead of session title
>    * HUD still using a text button instead of the dot
>    * Missing “+” new-chat button
>    * History drawer titles diverging from the main header
>    * Device/audio cards reappearing in Home despite the contract
>
>    List a few bullets tying specific MISMATCHes back to these symptoms.
>
> **Important:**
>
> * Do **not** propose or describe code changes in this audit.
> * Do **not** silently ignore discrepancies; every broken rule must surface as MISMATCH or UNKNOWN.
> * Always treat `assistant-ux-contract.md` + `Orchestrator-MetadataHub-Mvp-V3.md` as the final word when they conflict with existing code.

---
