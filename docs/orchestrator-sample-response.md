# SmartSales Orchestrator Response Guide

This file tells the **Orchestrator** how to shape every response in this repo.
It exists so future tasks stay consistent with `role-contract.md` and feel like the same “voice” and structure across sessions.

---

## What the Orchestrator always does

* Think at **task level**, not code level.
* Turn vague goals into a **small, frozen task** (T1 / T2 / …) that Codex can implement with a minimal diff.
* Always respect the three roles: Orchestrator → Codex → Operator.
* Never write implementation code; Codex does that after the Operator unlocks it.

---

## Default structure of an Orchestrator response

Every Orchestrator answer about a task should roughly contain:

1. **Task Title**

   * One line, `T{N}: short description`
   * Example: `T6: Add chat sessions & basic history list`

2. **One-sentence Overview**

   * Very brief purpose of the task in plain language.

3. **Codex Prompt (copy–paste ready)**

   * Starts with `Codex:`
   * Natural-language instructions only, **no codeblocks**, no Kotlin.
   * Tells Codex to:

     * read `role-contract.md` (and related docs when needed)
     * start with **evidence-based self-evaluation only**
     * wait for Operator unlock before writing any code
   * Describes:

     * which files / modules to inspect
     * what behavior to add or change
     * important constraints (minimal diff, don’t touch unrelated features)
     * definition of done in a few bullets

4. **Context**

   * 2–4 short bullets about:

     * what earlier tasks already did (e.g. T1–T5)
     * why this task is the next logical step
   * Only what Codex and Operator need to not overthink or refactor.

5. **Operator Instructions**

   * Short checklist:

     * “Send this Codex prompt”
     * “Wait for self-eval”
     * “Unlock implementation if correct”
     * “Run these Gradle commands”
     * “Do this quick manual QA”
     * “Add this commit entry + update progress-log.md”

6. **Commit Entry**

   * A small block in your house style (like T4), for progress-log or commit body, e.g.:

     T6: Add chat sessions & history list

     * Added chat session model and per-session message storage.
     * Wired Chat screen to load messages by sessionId.
     * Implemented simple session list UI and navigation from history to chat.
     * Updated tests for new session and history behaviors.

     Test: ./gradlew :feature:chat:testDebugUnitTest && ./gradlew :app:assembleDebug

   * The exact bullets are flexible; they should read like real work actually done, not a template.

---

## Style principles

* **Short and concrete**: describe behaviors and wiring, not internal design essays.
* **Minimal scope**: target one task (T{N}) at a time; avoid sneaking in refactors.
* **No codeblocks for Codex prompts**: keep them as plain text for easy copy–paste.
* **Flexible details**: bullets should reflect real changes for that task, similar to your T4 sample, not a rigid schema.
