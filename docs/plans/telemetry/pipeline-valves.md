# Pipeline Valve Telemetry Status

> **Purpose**: This document tracks the implementation status of `PipelineValve` checkpoints across the Data-Oriented OS.
> **Mental Model**: This is the GPS route of a "Passenger" (Data Payload) traveling from the Hardware Edge, down the OS Highway to the Brain, out to the Plugin Towns, and finally resting in the SSD Database.

## Phase 0: The Edge (Hardware & Ingestion)
*Data entering the system from the physical world.*

| Valve Identifier | OS Function | Payload Metric | Status |
|------------------|-------------|----------------|--------|
| `[HARDWARE_AUDIO_RECEIVED]` | Microphone/Agent Central | Byte Size | 🔲 PENDING |
| `[STT_TRANSCRIPT_DECODED]` | Tingwu/ASR Service | String Length | 🔲 PENDING |

## Phase 1: The Gatekeeper (Layer 3 Routing)
*The OS deciding if the input is noise, clarification, or a real task.*

| Valve Identifier | OS Function | Payload Metric | Status |
|------------------|-------------|----------------|--------|
| `[INPUT_RECEIVED]` | Highway Entry / Orchestrator | String Length | ✅ SHIPPED |
| `[ROUTER_DECISION]` | Lightning Router Short-circuit | Confidence/Target | ✅ SHIPPED |

## Phase 2: Context Assembly (The Sync Loop)
*The OS gathering reality before waking up the Brain.*

| Valve Identifier | OS Function | Payload Metric | Status |
|------------------|-------------|----------------|--------|
| `[ALIAS_RESOLUTION]` | Entity Disambiguation | Entity Count | ✅ SHIPPED |
| `[SSD_GRAPH_FETCHED]` | Database query for context | Node Count | 🔲 PENDING |
| `[LIVING_RAM_ASSEMBLED]`| Final payload handed to LLM | Token/Turn Count | ✅ SHIPPED |

## Phase 3: The Brain (LLM Execution)
*The non-deterministic barrier.*

| Valve Identifier | OS Function | Payload Metric | Status |
|------------------|-------------|----------------|--------|
| `[LLM_BRAIN_EMISSION]` | Raw string out of AI | String Length | ✅ SHIPPED |
| `[LINTER_DECODED]` | Typed Kotlin `data class` | Field/Task Count | ✅ SHIPPED |

## Phase 4: System III (The Towns / Plugins)
*First-party plugins functioning as extended arms of the OS. They inherit the right to use OS-level telemetry.*

| Valve Identifier | OS Function | Payload Metric | Status |
|------------------|-------------|----------------|--------|
| `[PLUGIN_DISPATCH_RECEIVED]` | Plugin boundary entry (e.g. Scheduler) | Parameter Count | 🔲 PENDING |
| `[PLUGIN_INTERNAL_ROUTING]` | Plugin decides sub-task (e.g. ASR, Web) | Target string | 🔲 PENDING |
| `[PLUGIN_EXTERNAL_CALL]` | Plugin talks to 3rd party API | Payload Size | 🔲 PENDING |
| `[PLUGIN_YIELDED_TO_OS]` | Plugin returns control upstream | Data Variant | 🔲 PENDING |

## Phase 5: The SSD (Layer 2 Persistence)
*The Async Loop saving state to disk.*

| Valve Identifier | OS Function | Payload Metric | Status |
|------------------|-------------|----------------|--------|
| `[DB_WRITE_EXECUTED]` | E.g., Room EntityWriter | Diff Size / Entity ID | 🔲 PENDING |

## Phase 6: The Skin (Layer 1 Presentation)
*The state bouncing back up to the user's eyes.*

| Valve Identifier | OS Function | Payload Metric | Status |
|------------------|-------------|----------------|--------|
| `[UI_STATE_EMITTED]` | ViewModel emitting to Compose | Data Class Variant | 🔲 PENDING |
