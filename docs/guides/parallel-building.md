# Parallel Building Contract

> **Purpose**: Enable frontend and backend to build simultaneously against shared contracts.
>
> **Last Updated**: 2026-01-09

---

## Contract Stack

When building in parallel, these are your sources of truth:

| Layer | Contract File | Who Owns | Stability |
|-------|---------------|----------|-----------|
| **Behavior** | [`Orchestrator-V1.md`](../specs/Orchestrator-V1.md) | Eng | 🔒 Locked |
| **Data Shapes** | [`orchestrator-v1.schema.json`](../specs/orchestrator-v1.schema.json) | Eng | 🔒 Locked |
| **Example Payloads** | [`orchestrator-v1.examples.json`](../specs/orchestrator-v1.examples.json) | Eng | 📝 Evolving |
| **Domain Interfaces** | [`api-contracts.md`](../specs/api-contracts.md) | Eng | 🔒 Locked |
| **UI Behavior** | [`ux-contract.md`](../specs/ux-contract.md) | Product/Eng | 🔒 Locked |
| **UI Presentation** | [`ux-experience.md`](./ux-experience.md) | UX | 📝 Evolving |
| **Providers** | [`source-repo.json`](../specs/source-repo.json) | Eng | 📝 Evolving |

**Precedence**: Orchestrator-V1 > schema > api-contracts > ux-contract > ux-experience > code

---

## Mocking Strategy

### Frontend Mocks Against

| What | Source | Location |
|------|--------|----------|
| Domain coordinator outputs | `api-contracts.md` interface + `examples.json` | `core/test/mock/Fake*.kt` |
| State shapes | `orchestrator-v1.schema.json` | `core/test/mock/payloads/*.json` |
| Provider responses | `source-repo.json` | `core/test/mock/payloads/*.json` |

### Backend Implements Against

| What | Source | Validation |
|------|--------|------------|
| Domain coordinator interfaces | `api-contracts.md` | Compile-time (Kotlin interfaces) |
| Output shapes | `orchestrator-v1.schema.json` | Unit tests + integration tests |
| Provider integration | `source-repo.json` | Contract tests |

### Mock Directory Structure

```
core/test/
├── mock/
│   ├── payloads/
│   │   ├── chat_events.json         # ChatEvent stream examples
│   │   ├── transcription_states.json
│   │   └── tingwu_responses.json    # Raw provider mocks
│   ├── FakeChatCoordinator.kt       # Emit payloads from JSON
│   ├── FakeTranscriptionCoordinator.kt
│   └── MockPayloadLoader.kt         # Load JSON → domain objects
```

---

## Contract Change Process

### When to Use

- Adding/removing fields from domain types
- Changing coordinator interface signatures
- Modifying state machine transitions
- Altering provider response handling

### Process

```
1. PROPOSER creates delta doc
   └── docs/plans/contract-delta-<date>-<topic>.md

2. DELTA doc structure:
   ┌─────────────────────────────────────┐
   │ ## Contract Delta: [Topic]         │
   │ **Date**: YYYY-MM-DD               │
   │ **Proposer**: [Name/Agent]         │
   │                                     │
   │ ### What Changes                    │
   │ - [Field X added to Y]              │
   │ - [Interface method signature]      │
   │                                     │
   │ ### Why                             │
   │ - [Business/technical reason]       │
   │                                     │
   │ ### Migration Impact                │
   │ - Frontend: [what changes]          │
   │ - Backend: [what changes]           │
   │ - Tests: [what changes]             │
   │                                     │
   │ ### Sign-off                        │
   │ - [ ] Frontend reviewed             │
   │ - [ ] Backend reviewed              │
   └─────────────────────────────────────┘

3. AFFECTED parties review and check off

4. UPDATE contracts atomically:
   - Schema + examples + api-contracts in same commit
   - Update mock payloads

5. BOTH sides sync before integration
```

---

## Integration Handoff

### Phase Checklist

| Phase | Frontend | Backend |
|-------|----------|---------|
| **Contract** | ✅ Read contracts | ✅ Read contracts |
| **Build** | Build against `Fake*` | Implement real coordinators |
| **Validate** | UI tests with mocks | Unit + integration tests |
| **Sync** | Swap `Fake*` → real | Verify output matches schema |
| **Integrate** | Run E2E | Contract tests pass |

### Sync Ceremony

When ready to integrate:

1. Backend confirms: "Output matches `examples.json` shapes"
2. Frontend confirms: "UI renders all states from `ux-experience.md`"
3. **Visual sign-off**: Screenshot/recording evidence for each UI state → save to `docs/verification/[feature]/`
4. Both: Run integration test together
5. Both: Sign off in PR or commit message

---

## Day-to-Day Workflow

### How the Mode Works

Once you enter Parallel Building Mode, you stay in it until explicit exit. Here's the actual rhythm:

```
┌─────────────────────────────────────────────────────────────┐
│ 1. MODE ENTRY                                               │
│    User: "/11-parallel-build mode on"                       │
│    Agent: Reads contracts, acknowledges role                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. FEATURE SELECTION                                        │
│    User: "Build [FeatureName], I'm [FRONTEND/BACKEND/BOTH]" │
│    Agent: Shows role-specific checklist                     │
└─────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┴───────────────────┐
          ▼                                       ▼
┌─────────────────────────┐         ┌─────────────────────────┐
│ FRONTEND TRACK          │         │ BACKEND TRACK           │
│                         │         │                         │
│ 1. Create Fake* if none │         │ 1. Read interface from  │
│ 2. Add mock payloads    │         │    api-contracts.md     │
│    from examples.json   │         │ 2. Implement coordinator│
│ 3. Build UI → all states│         │ 3. Unit test outputs    │
│    from ux-experience   │         │    against schema       │
│ 4. Capture screenshots  │         │ 4. Update examples.json │
│                         │         │    if new shapes        │
└───────────┬─────────────┘         └───────────┬─────────────┘
            │                                   │
            └───────────────┬───────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. CONTRACT DELTA (if needed)                               │
│    Either side: "I need to change [X]"                      │
│    → Both STOP → create delta doc → sign off → resume       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. PRE-INTEGRATION CHECK                                    │
│    Frontend: "All states rendered, screenshots captured"    │
│    Backend: "Outputs match schema, examples updated"        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. INTEGRATION                                              │
│    - Swap Fake* → real coordinator                          │
│    - Run E2E                                                │
│    - Visual sign-off (screenshots match expected)           │
│    - Commit with sign-off message                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. NEXT FEATURE OR EXIT                                     │
│    User: "Next: [AnotherFeature]" → loop back to step 2     │
│    User: "exit mode" → return to normal chat                │
└─────────────────────────────────────────────────────────────┘
```

### Quick Commands While in Mode

| Command | Effect |
|---------|--------|
| "I'm FRONTEND" / "I'm BACKEND" | Switch role, get role-specific checklist |
| "delta: [topic]" | Start contract change process |
| "ready to integrate" | Trigger sync ceremony checklist |
| "switch to [feature]" | Move to different feature (same mode) |
| "exit mode" | End parallel building, return to normal |

### Visual Evidence Directory

```
docs/verification/
├── [feature-name]/
│   ├── idle.png
│   ├── loading.png
│   ├── error.png
│   ├── success.png
│   └── flow.webp (optional screen recording)
```

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| "We synced on Slack" | Use delta doc in `docs/plans/` |
| Backend changes schema without delta | Create delta doc first |
| Frontend invents mock shapes | Use `examples.json` as source |
| "I'll just add this field" | Check if it affects other side |
| Mock happy path only | Mock all states from `ux-experience.md` |
| Wait for backend to test UI | Build against `Fake*` immediately |

---

## Cross-References

| Topic | Document |
|-------|----------|
| V1 behavior spec | [Orchestrator-V1.md](../specs/Orchestrator-V1.md) |
| Schema definitions | [orchestrator-v1.schema.json](../specs/orchestrator-v1.schema.json) |
| API signatures | [api-contracts.md](../specs/api-contracts.md) |
| UI contracts | [ux-contract.md](../specs/ux-contract.md) |
| UX states | [ux-experience.md](./ux-experience.md) |
| Agent rules | [AGENTS.md](./AGENTS.md) |
