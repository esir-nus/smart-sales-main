# Dual-Engine Bridge Spec

> **OS Layer**: L2 Testing Environment (RAM + SSD Integration)

## Execution Scenarios

### 1. The Ideal Path (Multi-Turn Delta)
- **Inject**: Turn 1: "Client X wants 50 widgets". Turn 2: "Actually, make it 100".
- **State**: `FakeEntityRepository` starts empty.
- **Expect**: Turn 1 creates the entity in SSD and `ContextBuilder` puts it in RAM. Turn 2 reads it from RAM, updates it in SSD, and `ContextBuilder` refreshes RAM with the new value.

### 2. The Trap (Context Branch)
- **Inject**: "Change the order to 100" (Without context).
- **State**: `FakeEntityRepository` empty, no prior turn context in `SessionWorkingSet`.
- **Expect**: `ClarificationNeeded` fallback explicitly without crashing the app.

### 3. The Linter Verification
- **Test**: Send hallucinated data from upstream (e.g. invalid entity ID) during Turn 2 (forced via fake executor).
- **Expect**: Test proves data is REJECTED by `verify()` mismatch or schema linter before hitting SSD.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Mock Eviction Structure | 🔲 PLANNED | Setup tear-down, fakes injected (`FakeEntityRepository`, etc) |
| **2** | Scenario Writing | 🔲 PLANNED | All Context Branches written and logging verified |
