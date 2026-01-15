---
description: Enter Feature Building Mode - implement new features with spec-first, evidence-based approach
---

# Feature Building Mode

A **mindset** for implementing new features with senior engineering discipline.

---

## When to Enter

- Implementing new V1 spec modules
- Adding major capabilities (not bugfixes or refactors)
- M1+ backlog feature items

---

## Mode Behavior

### Required Reading (Mode Entry)

| Priority | Document | Purpose |
|----------|----------|---------|
| 1 | [`docs/Orchestrator-V1.md`](file:///home/cslh-frank/main_app/docs/specs/Orchestrator-V1.md) | What to build (spec) |
| 2 | [`docs/tracker.md`](file:///home/cslh-frank/main_app/docs/plans/tracker.md) | Where it goes (structure) |
| 3 | JSON schemas in `docs/` | Data contracts |
| 4 | [`docs/api-contracts.md`](file:///home/cslh-frank/main_app/docs/specs/api-contracts.md) | Interface patterns |

**SoT Hierarchy**: V1 Spec > JSON Schema > api-contracts > existing code

### Entry Acknowledgment

When entering this mode, **acknowledge mode + feature target**:
> 🔨 **Feature Building Mode: ON** — [Feature/Module Name]  
> Required docs: ✅ Read / ⏳ Reading

### Persistence
- Mode stays **ON** until user explicitly exits
- Completing a feature does **NOT** auto-exit the mode
- User may continue to the next feature within the same mode

### Exit
Only exit when user explicitly requests:
- "exit mode"
- "end feature mode"
- "done building"

---

## Core Pillars

### 1. Senior Engineer Mindset

> Ship working software. Perfect is the enemy of done.

- **Pragmatic > Perfect** — Build what works, polish later
- **No Over-Engineering** — One implementation, one pattern
- **Vibe Coding Ready** — Code an AI can read and modify without 10-file context

| ❌ Don't | ✅ Do |
|----------|------|
| "Let me add abstraction layer for future" | "Ship now, abstract if needed later" |
| "This needs 5 interfaces" | "One interface if any, start concrete" |
| "Let me handle 12 edge cases" | "Happy path first, edge cases in follow-up" |

### 2. Follow Spec

> Build what spec says. Not what seems cool.

- V1 spec section defines behavior
- If spec is unclear → ask user, don't assume
- If spec is wrong → flag it, propose change, get approval before diverging

### 3. Evidence-Based Planning & Execution

> Audit before building. Grep before assuming.

**Before starting:**
```bash
# Check if similar code exists
grep -rn "FeatureName" feature/

# Check where it should go
view_file docs/tracker.md
```

**Before claiming done:**
```bash
# Build passes
./gradlew :app:assembleDebug

# Tests pass
./gradlew :feature:chat:testDebugUnitTest
```

### 4. Schema-Aware (Adaptive)

> Trust JSON schemas but recognize reality.

```
Schema = Contract (what SHOULD be)
Code = Reality (what IS)
```

**When they conflict:**
1. Reality wins for immediate shipping
2. File schema debt ticket: `// TODO(schema): [description]`
3. Don't block on schema disagreements

---

## Workflow

```
1. Define objective → what feature, which spec section?
2. Audit codebase → find related modules, patterns to follow
3. Plan → implementation plan artifact, verify assumptions
4. Build → Interface first, then Impl, then Fake
5. Wire → connect layers via Hilt, test data flows
6. Test → build + tests pass (using Fakes)
7. Lattice Check → /17-lattice-review
8. Ship → commit, push, update tracker.md
```

**Helper workflows:**
- `/01-senior-review` — Sanity check decisions
- `/06-audit` — Evidence-based code analysis
- `/17-lattice-review` — Verify Lattice compliance before ship

---

## Lattice Architecture Checkpoint

> **Every new box MUST have: Interface + Impl + Fake + Hilt binding**

### Box Creation Order

```
1. Interface → Define contract (what it does)
2. Fake → Test double (for consumer tests)
3. Impl → Real implementation
4. Hilt → @Binds or @Provides
5. Tests → Test with Fakes
```

### Pre-Ship Lattice Checklist

Before marking feature complete, verify:

```bash
# Interface exists
grep -rn "interface [BoxName]" feature/

# Fake exists
grep -rn "class Fake[BoxName]" feature/

# Hilt binding exists
grep -rn "@Binds.*[BoxName]\|@Provides.*[BoxName]" app/

# Zero Android imports in domain
grep -rn "import android\." feature/chat/src/main/java/com/smartsales/domain/
```

| Check | Pass | Fail |
|-------|------|------|
| Interface exists | ✅ | ❌ BLOCKER |
| Fake exists | ✅ | ❌ BLOCKER |
| Hilt binding | ✅ | ❌ BLOCKER |
| Zero Android in domain | ✅ | ❌ BLOCKER |
| Tests use Fakes | ✅ | ⚠️ Log debt |

### When to Skip Lattice

Only skip if:
- Pure utility function (no state, no I/O)
- UI-only change (Composables, ViewModels)
- Quick bugfix (not touching architecture)

---

## Exit Criteria

Leave this mode when:
- [ ] Feature works end-to-end
- [ ] Build passes
- [ ] Tests added for happy path (using Fakes)
- [ ] **Lattice compliant** (ran `/17-lattice-review`)
- [ ] Docs updated (tracker.md)
- [ ] **Metrics summary produced**

---

## Feature Building Metrics Summary (Required)

**Produce this when exiting Feature Building Mode:**

```markdown
### [Feature Name] — Build Metrics

| Metric | Value |
|--------|-------|
| Files added | X |
| Lines added | +XX |
| Boxes added | X (Interface+Fake+Impl) |
| Tests added | X |
| Lattice Score | XX/100 |
| Docs updated | X files |

### Files Created/Modified
| File | Change |
|------|--------|
| `NewFeature.kt` | Created - core logic |
| `FeatureCoordinator.kt` | +interface, +impl |

### Exit Criteria
- [x] Feature works
- [x] Build passes
- [x] Tests added
- [x] Docs updated
```

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| "Let me add this abstraction just in case" | "Ship concrete, abstract when needed" |
| "I'll handle all edge cases now" | "Happy path first, edge cases follow" |
| "The spec says X but I think Y is better" | "Flag concern, get approval, then diverge" |
| "I remember how this works" | "grep to verify, then implement" |
| "Let me build the whole module at once" | "One layer at a time: Interface → Fake → Impl → Hilt" |
| "I'll add tests later" | "Fake first, then test, then ship" |
| "Box doesn't need interface" | "Every box with I/O or state needs Interface+Fake" |
| "Skip Lattice check, it's a small change" | "Run /17-lattice-review before every ship" |
