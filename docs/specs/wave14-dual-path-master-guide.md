# Wave 14 Master Guide: Dual-Path Scheduler Architecture

> **Preamble**: This is the North Star architectural guide for Wave 14. It dictates the "Town and Highway" mental model for handling unstructured audio inputs via the DEV Audio Hook, decoupling optimistic UI response from heavyweight CRM disambiguation.

## 1. The Core Problem
The previous pipeline forced all audio input through the `RealUnifiedPipeline`. This pipeline performs heavy Context Assembly (ETL), SQLite lookups, and Entity Disambiguation. This resulted in:
- Unacceptable latency (Voice UX requires <500ms feedback).
- Ghosting (If the CRM disambiguation failed due to ambiguity, the UI card was never created).
- Single Point of Failure (A hiccup in the LLM or DB killed the user's base intention: saving a note).

## 2. The Architectural North Star

We are pivoting to a **Dual-Path (Forked) Architecture** synchronized by a `unifiedID`.

### Path A: The Town (Optimistic UI)
- **Role**: Fast-track execution. Fire-and-forget.
- **Component**: A streamlined, lightweight parser (or pure NLP).
- **Action**: Immediately creates a `ScheduledTask` in the local DB. Uses `ScheduleBoard` for hardcoded conflict checks.
- **Goal**: Render the UI Card instantly so the user can confidently walk away.

### Path B: The Highway (Deep CRM Analysis)
- **Role**: Asynchronous enrichment.
- **Component**: The full `RealUnifiedPipeline` (ETL + Disambiguation + LLM).
- **Action**: Background processes the audio text via LLM. Disambiguates names like "Li Zong" to actual CRM User IDs. Submits an `EntityWriter` event to update the `ScheduledTask` created by Path A.
- **Goal**: Preserve data integrity and provide "Juicy" intelligence without blocking the UI.

## 3. The Synchronization Key: `unifiedID`
To link the Town and the Highway without race conditions, the ASR (Transcription) layer orchestrator will generate a unique `unifiedID`. 
- Path A uses `unifiedID` as the Primary Key for the immediate `ScheduledTask`.
- Path B carries `unifiedID` through its intent extraction. When it resolves the CRM entities, the emitted mutations target the `ScheduledTask` where `id == unifiedID`.

## 4. Human UX Intricacy: The Unfolded Card
Pure systems fail when humans do unexpected things. When Path B encounters an unresolvable ambiguity (e.g., 3 clients named "Li Zong"):
- It does NOT crash or silently discard the task.
- It maps the "Clarification Required" state to the `unifiedID`.
- **The UX Resolution**: When the user taps the Task Card in the UI (expanding it), the card acts as a mini-chat window to resolve the ambiguity. We treat the card itself as the temporal anchor for that specific conversation thread.

## 5. Execution Rule
- **No God Tasks**: Implement this via strict "Cerb Shards". Implement the ID generation first, then Path A, then Path B. Do not intermingle UI teardowns with deep pipeline re-wiring. Complete one Shard perfectly before starting the next.
