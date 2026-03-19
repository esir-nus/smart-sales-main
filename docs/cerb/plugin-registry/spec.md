# Plugin Registry

> **OS Layer**: App Infrastructure / Hardware Abstraction
> **State**: PARTIAL

---

## Overview

The `plugin-registry` is responsible for formally managing the "Hands" of the AI assistant. While the Orchestrator acts as the brain (System II) that decides *what* to do, the Plugin Registry manages the actual executable Kotlin workflows (Plugins).

Currently, tools are hardcoded in a `FakeToolRegistry` with a synchronous interface. This spec defines the path to a real, extensible Plugin SDK where tools can be dynamically registered, executed asynchronously, and interact with the UI without tightly coupling the core Orchestrator to specific features (like Tingwu or CRM Export).

**Key Principles**:
1. **Separation of Concerns**: The Orchestrator knows *about* tools via `getAllTools()`, but does not know *how* they work.
2. **Safe Execution Boundary**: Plugins must execute purely in Kotlin space. The LLM only maps intents to `toolId`s; it does not write executable code.
3. **Capability Gateway First**: Plugins should consume approved runtime capabilities, not directly wire themselves into RAM loaders, repositories, or UI state machines.
4. **Async Yield Contract**: Real tools take time. The registry may still yield asynchronous `Flow<UiState>` during the transitional shell, but capability calls and permissions must be owned by the OS runtime.

The routing law beneath these principles is now:

- the main orchestrator owns the outer user loop
- the LLM chooses a stable semantic `toolId` plus bounded parameters
- each plugin runs a bounded sub-pipeline inside that entry lane
- completion yields a bounded result back to the OS and returns control to the main loop

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Interface Contract** | ✅ SHIPPED | Define `PluginRegistry` spec and interface. Document current fake debt. |
| **2** | **Plugin SDK Definition** | ✅ SHIPPED | Create `PrismPlugin` interface and standard `PluginRequest` / `PluginResponse` DTOs. |
| **3** | **Dynamic Registry** | ✅ SHIPPED | Implement `RealToolRegistry`. Use Dagger/Hilt Multibinding (`@IntoSet`) to inject all registered `PrismPlugin` implementations dynamically. |
| **4** | **Async Execution Wiring** | ✅ SHIPPED | Refactor `executeTool` to return or emit asynchronous UI states to prevent thread blocking during long-running tasks. |
| **5** | **Runtime Gateway Foundation** | 🚧 IN PROGRESS | Replace silent/no-op gateways with real runtime capability access, enforce `requiredPermissions`, and deliver the first bounded read-only capability bundle (`READ_SESSION_HISTORY` + progress signaling). |

---

## T3 Narrow Capability Foundation

Wave 21 `T3` does **not** attempt a full plugin rewrite.

It delivers the first real runtime capability gateway with these explicit boundaries:

- **Allowed in T3**
  - bounded session/history read through OS-owned session memory
  - bounded progress signaling through the plugin gateway
  - runtime enforcement of declared plugin permissions
  - real valve coverage for dispatch -> capability call -> yield/failure

- **Deferred to T4+**
  - plugin-caused SSD writes
  - generic write-result contracts
  - broad "query anything" SDK surfaces
  - repo-wide plugin result re-architecture

The first gateway bundle should stay intentionally narrow:

1. `READ_SESSION_HISTORY`
2. progress signaling

History append is not part of the active T3 gateway surface and remains deferred to a later write-capability wave.

Delivered runtime behavior at this stage:

- non-voice plugin dispatch remains confirmation-gated in the outer loop
- the user-visible commit phrase remains `"确认执行"`
- approved voice plugin dispatch may auto-commit into the plugin lane
- both paths must still yield bounded execution evidence back to the OS shell rather than disappearing into background-only work

---

## T4 Semantic Entry IDs

The plugin lane should no longer be modeled as anonymous vault slots such as `tool01`.

The outer loop should route into stable semantic entry IDs that match real user scenarios in this Android app:

| `toolId` | Primary User Intent | Typical `ruleId` Variants | Notes |
|------|----------------------|---------------------------|-------|
| `artifact.generate` | Generate a shareable business artifact such as a PDF report | `executive_report`, `account_brief`, `meeting_summary` | Artifact lane: draft -> mint -> preview/share |
| `audio.analyze` | Analyze recorded or uploaded audio | `meeting_analysis`, `call_insights`, `speaker_summary` | Audio intelligence lane, usually read-heavy |
| `crm.sheet.generate` | Generate a CRM-facing summary sheet or worksheet | `account_sheet`, `opportunity_sheet`, `followup_sheet` | Structured business worksheet lane |
| `simulation.talk` | Run a roleplay or talk simulation | `sales_roleplay`, `objection_handling`, `meeting_rehearsal` | Interactive simulation lane |

Design law:

- `toolId` chooses the plugin lane
- `ruleId` chooses the behavior inside that lane
- internal capability choreography remains inside the plugin lane and is **not** part of the LLM-facing routing surface

---

## Initial Real Plugin Matrix

These are the first real plugin families the capability framework should optimize for.

### 1. `artifact.generate`

Purpose:

- generate a bounded markdown artifact from approved context
- hand the draft to the OS-owned artifact mint/export path
- return preview/share metadata such as `assetId`

Expected capability bundle:

- `READ_ARTIFACT_CONTEXT`
- `ACTION_GENERATE_DRAFT`
- `WRITE_REQUEST_MINT_ARTIFACT`
- `ACTION_EXPORT_ARTIFACT`

### 2. `audio.analyze`

Purpose:

- analyze an audio-derived session or transcript
- produce bounded insights, highlights, and next-step summaries
- optionally hand off a later artifact request instead of exporting directly inside the plugin

Expected capability bundle:

- `READ_AUDIO_CONTEXT`
- `READ_SESSION_CONTEXT`
- `ACTION_GENERATE_ANALYSIS`

### 3. `crm.sheet.generate`

Purpose:

- synthesize a structured CRM sheet from account/opportunity/session context
- keep the output bounded and reusable
- support later write-request or artifact mint handoff without direct plugin persistence

Expected capability bundle:

- `READ_CRM_CONTEXT`
- `READ_SESSION_CONTEXT`
- `ACTION_GENERATE_SHEET`
- `WRITE_REQUEST_MINT_ARTIFACT`

### 4. `simulation.talk`

Purpose:

- run a bounded talk simulation or rehearsal
- return scripted prompts, objections, scoring, or coaching notes
- remain inside the main orchestration loop rather than becoming an independent app

Expected capability bundle:

- `READ_SIMULATION_CONTEXT`
- `ACTION_RUN_SIMULATION`
- `ACTION_SCORE_SIMULATION`

---

## Capability Framework Direction

The T4 capability framework should expose reusable plugin-facing capacities while keeping owner-specific wiring behind the API surface.

The preferred taxonomy remains:

- `READ`
- `ACTION`
- `WRITE_REQUEST`

But the first real lanes above show that the framework must also support:

- semantic tool entry IDs for routing
- `ruleId` specialization within one plugin family
- bounded result contracts that can return read-only output, artifact mint requests, or later write requests

The first proved write-capability path should come from one of these real lanes rather than from a synthetic demo plugin.
