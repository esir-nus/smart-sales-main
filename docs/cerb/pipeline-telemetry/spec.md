# Pipeline Telemetry Spec

> **Cerb-compliant spec** — Standardized pipeline dataflow observability.
> **OS Layer**: RAM Application (Cross-cutting concern)
> **State**: SPEC_ONLY

---

## Overview

The `pipeline-telemetry` module provides a standardized way to trace data drops, mutations, and API requests across the entire Prism architecture. 

It satisfies two critical constraints:
1. **Lattice Rule Compliance**: Pure `domain/` layer code cannot import `android.util.Log`. This module acts as the abstraction interface.
2. **Industrial Observability**: Standardizes adb logcat output with strictly typed prefixes (e.g., `PrismFlow|Parser`) so developers can type `adb logcat -s "PrismFlow|*:D"` to monitor the precise chronological data flow without system noise.

**Vibe Coding Principle**: Keep it stupidly simple. Do not build huge payload databases or remote metrics shippers here. We are purely wrapping `Log.d` into a domain-safe API.

---

## Logging Guidelines (For AI Agents & Developers)

| DO | DON'T |
|----|-------|
| Emphasize boundary operations (Data passed between modules) | Log granular `for` loop internal traces |
| Log size and shape of payloads (e.g. `Sent 1500 entities to LLM`) | Dump massive 2000-entity JSON arrays into Logcat |
| Use the exact predefined `PipelinePhase` enums | Invent new random tags like `AgentDebug` or `TempCheck` |

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Contract | ✅ SHIPPED | `PipelineTelemetry`, `PipelinePhase`, `FakePipelineTelemetry`, `RealPipelineTelemetry`. |
| **2** | Seeding Dev Logs | ✅ SHIPPED | Inject the logger into `IntentOrchestrator`, `InputParserService`, `ContextBuilder`, and `CoachPipeline`. |

---

## OS Layer Adherence
- Acts purely as a passthrough to the Android system logging daemon within the App layer execution flow.
- Contains no state, no RAM overhead, and does not interact with SSD storage.
