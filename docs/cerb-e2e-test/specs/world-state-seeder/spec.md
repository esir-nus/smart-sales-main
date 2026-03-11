# WorldStateSeeder Spec

> **OS Layer**: L2 Testing Environment (RAM + SSD Integration)
> **State**: SHIPPED

**Reference**: The exact Chaos Seed data payload is stored in [`dataset.md`](dataset.md).

## Execution Scenarios

### 1. The Fragmented Aliases
- **Inject**: A sequence of unlinked interactions mentioning the same company via different colloquial aliases (e.g., "字节", "字节跳动", "ByteDance").
- **State**: The `FakeEntityRepository` starts empty and progressively aggregates the aliases using the `EntityWriter`.
- **Expect**: The `EntityRegistry` (via reads) correctly resolves all three aliases to a single unified `EntityEntry` with the canonical name.

### 2. The Overlapping Noise
- **Inject**: Authentic Chinese B2B noise where a contact name is common (e.g., "张伟"), but associated with two distinct companies in different sessions.
- **State**: The context assembler encounters overlapping clues.
- **Expect**: The seeder proves that the disambiguation loop can successfully partition the entities based on temporal/company context without merging them improperly.

### 3. The Contextual Assembly
- **Inject**: A rich profile containing previous meeting notes (Memory), typical reading times (Habits), and upcoming follow-ups (Scheduler).
- **State**: Full multi-domain data is seeded.
- **Expect**: The `ContextBuilder` can successfully assemble a `ContextDepth.FULL` snapshot that includes all dimensions without hitting token overflow or missing critical context boundaries.

### 4. Chaos Seed Injection
- **Inject**: The definitive `dataset.md` JSON payload directly into the Repositories, bypassing `EntityWriter` logic to forcibly set historical timestamps. Contains a 6-month sporadic history and a 1-week dense history for multiple entities.
- **State**: Complex legacy context and active deal negotiations are established natively in SSD.
- **Expect**: The graph natively reflects the semantic evolution and exact metadata required for precise temporal extraction testing in upper layers.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Mock Eviction Structure | ✅ SHIPPED | Setup tear-down, fakes injected |
| **2** | Scenario Writing | ✅ SHIPPED | All Data Generation Branches written |
