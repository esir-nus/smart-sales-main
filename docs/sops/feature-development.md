# Feature Development SOP

> **Purpose**: A rigorous, repeatable process for developing features that align with spec, clean architecture, and E2E flow integrity.

---

## When to Use

- Implementing new features (not bugfixes or refactors)
- Adding major capabilities to existing modules
- M1+ backlog items

---

## Phase 1: UNDERSTAND (Read Specs in Order)

**Goal**: Load context before writing code.

| Order | File | Purpose |
|-------|------|---------|
| 1 | `docs/specs/GLOSSARY.md` | Terms, no-synonyms rule (Rule #1) |
| 2 | `docs/specs/prism-ui-ux-contract.md` | UI INDEX — find your module/flow |
| 3 | `docs/sops/ui-dev-mode.md` | UI development model, prototype-first workflow, approval gates |
| 4 | Feature module spec (e.g., `HomeScreen.md`) | Component layout, gestures |
| 5 | Feature flow spec (e.g., `SchedulerFlows.md`) | User journey, states |

**Checkpoint**: Can you explain the feature in one sentence?

---

## Phase 2: VALIDATE REQUIREMENTS

**Goal**: Map the E2E flow before coding.

### 2.1 Map Trinity Layers

| Layer | SOT Document | Question |
|-------|--------------|----------|
| **Brain** | `Architecture.md` §relevant | What's the core logic? |
| **Soul** | `prism-ui-ux-contract.md` | What's the UX intent? |
| **Body** | Module/Component specs | What's the layout? |

### 2.2 E2E Flow Sketch

Draw the user journey on paper or ASCII:

```
User taps → [Component] → [State change] → [UI update] → User sees
```

### 2.3 Clarify Ambiguities

If spec is unclear → **ASK** before assuming.
If spec seems wrong → **FLAG** and propose change.

### 2.4 Small Slice Simplicity Law

- for small bounded features, implement the smallest state machine that fulfills the core flow
- do not add speculative fallback branches, extra host/runtime splits, or hidden “safety” behavior unless the spec or reproduced evidence requires them
- every added safeguard should point to either a literal spec rule or a reproduced failure path

**Checkpoint**: E2E flow mapped, Trinity layers identified.

---

## Phase 3: IMPLEMENT (Lattice Order)

**Goal**: Build with clean architecture.

### 3.1 Box Creation Order

```
1. Interface → Define contract (what it does)
2. Fake → Test double (for consumer tests)
3. Impl → Real implementation
4. Hilt → @Binds or @Provides
5. Tests → Test with Fakes
```

### 3.2 Evidence-Based Coding

Before writing:
```bash
# Check if similar code exists
grep -rn "FeatureName" app-core/

# Check patterns to follow
view_file docs/plans/tracker.md
```

### 3.3 Prism Clean Environment Rule

> **Prism code must NOT import legacy code.**

| ✅ Allowed in domain/ | ❌ Forbidden in domain/ |
|-----------------------|-------------------------|
| `kotlinx.*` | `android.*` |
| `javax.inject.*` | `feature.*` (legacy) |
| `dagger.hilt.*` | `com.smartsales.legacy.*` |

**Checkpoint**: Interface + Fake + Impl + Hilt binding complete.

---

## Phase 4: VERIFY (Audit Gates)

**Goal**: Ensure quality before ship.

### Required Audits

| Audit | Workflow | When |
|-------|----------|------|
| **Prism Verification** | `/cerb-check` | After impl — all 3 gates (Registry → Contract → Architecture) |
| **Agent Visibility** | `/agent-visibility` | If feature touches agent activity |

### Build Verification

```bash
# Build passes
./gradlew :app:assembleDebug

# Tests pass
./gradlew testDebugUnitTest
```

**Checkpoint**: All audits pass, build green.

---

## Phase 5: SHIP

**Goal**: Document and deliver.

### 5.1 Update Tracker

- Update `docs/plans/tracker.md` with feature status
- Move item from 🔲 to ✅

### 5.2 Exit Checklist

```markdown
- [x] Feature works E2E
- [x] /cerb-check passes all 3 gates
- [x] Build + tests green
- [x] Tracker updated
```

---

## Quick Reference

### What Ships a Great Feature

| Pillar | Checkpoint |
|--------|------------|
| **Spec Aligned** | `/cerb-check` Gate 2 passes |
| **Flow Complete** | E2E journey works |
| **Arch Clean** | `/cerb-check` Gate 3 passes |
| **Visible** | Agent feels smart |
| **Tested** | Build + unit tests green |
| **Documented** | tracker updated |

### Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|------|
| Read one spec and start coding | Follow 5-phase order |
| Skip E2E flow mapping | Sketch flow before coding |
| Import legacy code | Rewrite from Prism spec |
| Skip audits "to save time" | Run all 3 audit gates |
| Invent speculative fallback/safety branches | Prove the need first, then add the smallest guard |
| Forget to update tracker | Update tracker as final step |
