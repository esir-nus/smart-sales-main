# Cerb Specifications Hub

> **System**: Smart Sales B2B CRM Assistant
> **Status**: Active supporting index
> **Last Updated**: 2026-03-31
> **Structure Rule**: DO NOT physically nest sub-directories under `docs/cerb/`. Keep all feature specs flat to preserve cross-references (`docs/cerb/[feature]/spec.md`). Use this README as the conceptual human-readable map.

This hub maps the physical feature folders to the conceptual architecture outlined in the Product Requirements Document (PRD).

---

## 1. System I: The Mascot
Ephemeral, read-only layer for handling noise, greetings, and system notifications out-of-band. Never initiates DB writes.

- **[Mascot Service](./mascot-service/spec.md)**
- **[Notifications](./notifications/spec.md)**

---

## 2. System II: The Unified Pipeline
The stateful, linear ETL pipeline that handles actual intent analysis, dynamic context assembly, and tool dispatching.
*Replaces legacy Analyst Orchestrator.*

- **[Lightning Router](./lightning-router/spec.md)** (The Phase 0 Gatekeeper)
- **[Unified Pipeline](./unified-pipeline/spec.md)** (The System II Core)
- **[Input Parser](./input-parser/spec.md)**
- **[Model Routing](./model-routing/spec.md)**
- **[Pipeline Telemetry](./pipeline-telemetry/spec.md)**

---

## 3. Context & Memory (OS Layer)
The data structures and persistence mechanisms that the Pipeline reads from and writes to.

- **[Context Builder](./session-context/spec.md)** (RAM / Session Working Set)
- **[Memory Center](./memory-center/spec.md)** (SSD)
- **[Session History](./session-history/spec.md)**
- **[Client Profile Hub](./client-profile-hub/spec.md)**
- **[RL Module (Habits)](./rl-module/spec.md)**
- **[User Habit Storage](./user-habit/spec.md)**

---

## 4. Entity Management & Disambiguation
Strict pipeline for organic CRM entity writing and resolution.

- **[Entity Registry](./entity-registry/spec.md)** (Queries & Reads)
- **[Entity Writer](./entity-writer/spec.md)** (Mutations & Writes)
- **[Entity Disambiguation](./entity-disambiguation/spec.md)** (Clarification Flow)

---

## 5. Hardware & Audio Integration
The pipeline from the BLE badge to the transcription engine.

- **[Connectivity Bridge](./connectivity-bridge/spec.md)**
- **[Device Pairing](./device-pairing/spec.md)**
- **[Badge Audio Pipeline](./badge-audio-pipeline/spec.md)**
- **[Audio Management](./audio-management/spec.md)**
- **[ASR Service](./asr-service/spec.md)**
- **[Tingwu Pipeline](./tingwu-pipeline/spec.md)**
- **[OSS Service](./oss-service/spec.md)**

---

## 6. Features & Workflows
The downstream capabilities executed by the Pipeline.

- **[Scheduler](./scheduler/spec.md)**
- **[Conflict Resolver](./conflict-resolver/spec.md)**
- **[Plugin Registry](./plugin-registry/spec.md)** (Core Tool Infrastructure)

---

## Critical References
- **[Interface Map (`interface-map.md`)](./interface-map.md)**: The strict SOT for data ownership limits and dependencies. Read before defining any new edges!
- **[SmartSales PRD](../../SmartSales_PRD.md)**: Product vision and core mechanics.
