---
description: Enter Parallel Building Mode for simultaneous frontend/backend development
---

# Parallel Building Mode

A **collaboration protocol** for building frontend and backend simultaneously against shared contracts.

---

## When to Enter

- Starting a new feature that touches both UI and domain logic
- Multiple agents/developers working on same feature
- Need to build frontend before backend is ready (or vice versa)

---

## Mode Behavior

### Required Reading (Mode Entry)

Before parallel building, **read these in order**:

| Priority | Document | Purpose |
|----------|----------|---------|
| 1 | [`docs/parallel-building.md`](file:///home/cslh-frank/main_app/docs/guides/parallel-building.md) | Collaboration contract (SoT for this mode) |
| 2 | [`docs/api-contracts.md`](file:///home/cslh-frank/main_app/docs/specs/api-contracts.md) | Domain interfaces to implement/mock |
| 3 | [`docs/orchestrator-v1.schema.json`](file:///home/cslh-frank/main_app/docs/specs/orchestrator-v1.schema.json) | Data shapes |
| 4 | [`docs/ux-experience.md`](file:///home/cslh-frank/main_app/docs/guides/ux-experience.md) | UI states to cover |

### Entry Acknowledgment

When entering this mode, **acknowledge role + reading status**:

```markdown
🔀 **Parallel Building Mode: ON** — [Feature Name]
Role: [FRONTEND | BACKEND | BOTH]
Required docs: ✅ Read / ⏳ Reading
```

### Persistence

- Mode stays **ON** until user explicitly exits
- Completing a task does **NOT** auto-exit
- User may switch roles within the mode

### Exit

Only exit when user explicitly requests:
- "exit mode"
- "end parallel building"
- "integration complete"

---

## Role-Based Checklists

### Frontend Role

```markdown
## Frontend Checklist — [Feature Name]

### Setup
- [ ] Read `api-contracts.md` for coordinator interface
- [ ] Read `ux-experience.md` for all states
- [ ] Identify mock payloads needed

### Build
- [ ] Create/update `Fake*Coordinator` if needed
- [ ] Add mock payloads to `core/test/mock/payloads/`
- [ ] Build UI against fake coordinator
- [ ] Cover ALL states from ux-experience.md

### Validate
- [ ] UI renders correctly with mock data
- [ ] Error states handled
- [ ] Loading states shown
- [ ] Empty states shown

### Visual Evidence
- [ ] Capture screenshots for each state → `docs/verification/[feature]/`
- [ ] Record screen flow if animations involved (save as .webp)

### Pre-Integration
- [ ] Confirm: "Ready to swap Fake* for real impl"
- [ ] Document any contract questions in delta doc
```

### Backend Role

```markdown
## Backend Checklist — [Feature Name]

### Setup
- [ ] Read `api-contracts.md` for interface to implement
- [ ] Read `orchestrator-v1.schema.json` for output shapes
- [ ] Identify integration points

### Build
- [ ] Implement coordinator interface
- [ ] Output matches schema shapes
- [ ] Add unit tests

### Validate
- [ ] Unit tests pass
- [ ] Output matches `examples.json` structure
- [ ] Error handling follows V1 spec

### Pre-Integration
- [ ] Confirm: "Output matches contract shapes"
- [ ] Update `examples.json` if new cases added
- [ ] Document any contract changes in delta doc
```

---

## Contract Delta Workflow

When you need to change a contract during parallel building:

```
1. STOP parallel work
2. CREATE delta doc: docs/plans/contract-delta-<date>-<topic>.md
3. NOTIFY other role
4. WAIT for review/sign-off
5. UPDATE contracts atomically
6. RESUME parallel work
```

Template:
```markdown
## Contract Delta: [Topic]

**Date**: YYYY-MM-DD
**Proposer**: [Agent/Developer]
**Affects**: [FRONTEND | BACKEND | BOTH]

### What Changes
- [Specific change]

### Why
- [Reason]

### Migration Impact
- Frontend: [what to update]
- Backend: [what to update]

### Sign-off
- [ ] Frontend reviewed
- [ ] Backend reviewed
```

---

## Integration Ceremony

When both sides are ready:

```markdown
## Integration Checkpoint — [Feature Name]

### Backend Confirms
- [ ] Coordinator implemented per `api-contracts.md`
- [ ] Outputs match `orchestrator-v1.schema.json`
- [ ] Unit tests pass
- [ ] `examples.json` updated if needed

### Frontend Confirms
- [ ] UI covers all `ux-experience.md` states
- [ ] Works with `Fake*` coordinator
- [ ] Ready to swap to real impl

### Integration
- [ ] Swap `Fake*` → real coordinator
- [ ] E2E test passes
- [ ] No regressions

### Visual Sign-off
- [ ] Screenshots match expected states
- [ ] Evidence saved to `docs/verification/[feature]/`

### Sign-off
- [ ] Build passes: `./gradlew :app:assembleDebug`
- [ ] Tests pass: `./gradlew testDebugUnitTest`
```

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| "Backend isn't ready, I'll wait" | Build against `Fake*` now |
| "I'll just add this field" | Create delta doc first |
| "We agreed in chat" | Document in delta doc |
| "Mock the happy path only" | Mock ALL states |
| Invent mock shapes | Use `examples.json` |
| Change contract mid-build without notice | STOP → delta → sync → resume |

---

## Exit Criteria

Leave this mode when:
- [ ] Feature integrated end-to-end
- [ ] Both roles signed off
- [ ] Build passes
- [ ] Tests pass
- [ ] Any delta docs resolved

---

## Related Workflows

- `/10-feature-build` — Feature building (single-track)
- `/6-audit` — Evidence-based code analysis
- `/1-senior-review` — Sanity check decisions
