# Executive Report Plugin Concept

> **Drafting Protocol**: This document strictly defines the mental model, user experience, and architectural boundaries. Premature concrete interfaces and database schemas are intentionally deferred.

---

## 1. The Mental Model

If the Core Pipeline is the "Highway" and the Artifact Library is the "Warehouse", this plugin is a specialized **Factory** located in one of the Towns off the highway.

| Characteristic | Description |
|---|---|
| **Nature** | Heavy generative LLM worker (System III Plugin). |
| **Metaphor** | The Analyst writing a report at a desk, printing it, and handing it to the user. |
| **Lifecycle** | Ephemeral. It spins up, generates the report, mints the Artifact, and absolutely terminates. |

---

## 2. The User Flow & Experience

Describe the end-to-end journey from the user's perspective.

### Stage A: Trigger & Creation
1. **Invocation**: Frank says, "Generate an executive summary of the Huawei account."
2. **Routing**: The `LightningRouter` identifies this as a `[Plugin]Request` (target: Executive Report) rather than basic chat. It packages the target parameters (e.g., `targetAccount = Huawei`).
3. **The Work**: The Plugin executes. It queries the `MemoryRepository` / CRM database for all recent Huawei interactions. It feeds this raw context to the LLM to synthesize a professional Markdown document.
4. **Handoff**: The Plugin takes the LLM's Markdown output and explicitly hands it to the **Artifact Engine** (System IV). The Engine returns a minted `AssetId`.

### Stage B: Interaction & UI Response
1. **Acknowledgement**: The Plugin replies to the Core Pipeline: *"I have generated the Huawei report."*
2. **Context Binding**: The Agent's reply includes the explicit `AssetId`.
3. **UI Illumination**: Because the text stream emitted an `AssetId`, the frontend `AgentViewModel` illuminates the "Artifact Box" (⚗️) for this session, allowing Frank to immediately tap it, preview the Markdown, and share it as a PDF.

---

## 3. System Boundaries & Cross-Module Connections

Map the expected blast radius when this concept becomes real code.

### A. The Storage Layer 
- The Plugin itself **owns no persistent database state**. 
- It *reads* from `:data:memory` (or CRM).
- It *writes* to `:data:artifact` via a strictly defined interface.

### B. The Awareness Layer
- The Plugin must be registered in the `PluginGateway`.
- It requires an asynchronous execution allowance (it will take 5-10 seconds to generate). The Core Pipeline must not block waiting for it. It should emit a "Generating report..." loading state to the UI.

### C. The Presentation Layer
- The Plugin returns no custom UI. It solely returns a standardized `PluginResult.Success(assetId, message)` which the existing UI architecture already knows how to render.

---

## 4. Anti-Hallucination & Anti-Illusion Proofing

Define the rigid constraints to ensure the LLM cannot lie or invent state.

1. **Information Asymmetry Guarantee**: The LLM parsing the prompt only extracts parameters (`targetAccount`). The deterministic Kotlin codebase performs the actual DB query to get the Huawei facts.
2. **GroundTruth Generation**: The LLM that generates the Markdown report is wrapped in a strict system prompt: *"You are an executive analyst. Generate a report based STRICTLY on the provided JSON facts. Do not invent metrics or meetings."*
3. **Mint Verification Lock**: The Plugin must NEVER return a "Success" text response to the user unless the `Artifact Engine` successfully returned a valid, non-null `AssetId` confirmed on the physical SSD. "Fake it till you make it" is banned here.
