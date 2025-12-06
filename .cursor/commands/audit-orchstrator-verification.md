About **how to use that big audit prompt**, think of it *not* as something you paste back into me, but as a **manual checklist + script for you** to run locally.

Here’s how to use it effectively:

---

## 1. Save it in your repo

1. Create a file like:

   * `docs/plans/T-orch-V3-Final-Verification.md`
2. Paste the whole audit prompt into that file.

This turns it into an *official checklist* for Orchestrator + MetaHub changes.

---

## 2. When should you use it?

Use it **every time** you:

* Touch:

  * `HomeOrchestratorImpl`
  * `RealTranscriptOrchestrator`
  * `RealTingwuCoordinator`
  * `ExportOrchestrator`
  * `SessionMetadata` / `TranscriptMetadata` / `InMemoryMetaHub`
  * `HomeScreenViewModel` streaming / export / audio upload
* Or you think you might have impacted:

  * SMART_ANALYSIS behavior
  * export behavior
  * Tingwu transcript → Chat wiring
  * speaker-role inference

Basically: any “orchestrator / MetaHub / transcript / export” change → run this checklist.

---

## 3. How to actually run it (step-by-step)

### Step 1 – Run the test commands from the prompt

In your project root, run these **exact commands** in a terminal:

```bash
# MetaHub / merge semantics
./gradlew :core:util:test \
  --tests "com.smartsales.core.metahub.InMemoryMetaHubTest" \
  --tests "com.smartsales.core.metahub.SessionMetadataMergeTest" \
  --tests "com.smartsales.core.metahub.TranscriptMetadataMergeTest"

# Orchestrator + Tingwu
./gradlew :data:ai-core:testDebugUnitTest \
  --tests "com.smartsales.data.aicore.RealTranscriptOrchestratorTest" \
  --tests "com.smartsales.data.aicore.RealTingwuCoordinatorTest" \
  --tests "com.smartsales.data.aicore.ExportOrchestratorContractTest"

# Home export, streaming dedup, transcription binding
./gradlew :feature:chat:testDebugUnitTest \
  --tests "com.smartsales.feature.chat.home.HomeExportActionsTest" \
  --tests "com.smartsales.feature.chat.home.HomeStreamingDedupTest" \
  --tests "com.smartsales.feature.chat.home.HomeTranscriptionTest"
```

**How to use the results:**

* If everything is **green** → good, go to Step 2.
* If something fails:

  * Fix the code or tests.
  * Re-run the same command(s) until they pass.
  * Only then go to Step 2.

You don’t need Codex for this — this is just your normal local Gradle.

---

### Step 2 – Walk through the checklist sections (A–F)

Open the markdown file with the audit prompt and go section by section:

* **A. LLM boundaries**
  In IDE, search for `AiChatService` and confirm:

  * Only `HomeOrchestratorImpl` and `RealTranscriptOrchestrator` depend on it.
  * `RealExportOrchestrator`, `RealTingwuCoordinator`, `HomeScreenViewModel`, `MetaHub` are LLM-free.

* **B. MetaHub models & merge semantics**
  Open `SessionMetadata.kt`, `TranscriptMetadata.kt`, `InMemoryMetaHub.kt`:

  * Ensure `mergeWith` matches the rules (non-null override, tag union, CRM merge, confidence clamp).
  * Confirm `upsertSession`/`upsertTranscript` both use `mergeWith`.

* **C. HomeOrchestrator + HomeScreenViewModel**

  * Confirm JSON is only parsed in `HomeOrchestratorImpl`.
  * In `HomeScreenViewModel`, check that `metaHub` is only used to **read** (`getSession`), not parse JSON.

* **D. SMART_ANALYSIS & export**

  * Check `exportMarkdown` in the VM:

    * Enough content → analysis allowed.
    * Too short content → only hint, no analysis request.
  * `onAnalysisCompleted` writes `latestMajorAnalysis*` into MetaHub.
  * `ExportOrchestrator`:

    * `exportPdf(sessionId, markdown)`
    * `exportCsv(sessionId)` from CRM rows only, no LLM.

* **E. TranscriptOrchestrator + Tingwu**

  * Verify the parameter list of `TranscriptMetadataRequest` calls in `RealTingwuCoordinator`.
  * Confirm confidence-threshold behavior and fail-soft behavior (no crashes).

* **F. Audio upload & session binding**

  * Home upload reuses current `sessionId` → Tingwu → back into same chat.
  * Audio Sync screen is the only place allowed to spin up a **new** session for a transcript.

For each subsection A–F, mentally mark it as ✅ / ⚠️ / ❌.
If you hit ⚠️ or ❌, fix the code, go back to **Step 1**, and loop again.

---

## 4. How to “close” a T-orch task using this

Once:

1. All three Gradle commands are ✅ green, and
2. All checklist sections A–F are truly satisfied,

you can write in your commit / PR / internal doc something like:

> **T-orch-V3 guardrails reverified**
>
> * All orchestrator + MetaHub tests passing
> * LLM boundaries intact (only HomeOrchestratorImpl & RealTranscriptOrchestrator hit AiChatService)
> * MetaHub merge semantics verified via Session/Transcript tests
> * Home streaming dedup & export behavior tested
> * Tingwu transcript → Chat sessionId & speaker roles checked

That’s how this prompt is meant to be used:
👉 **as a repeatable manual “final boss” checklist for orchestrator/MetaHub work**