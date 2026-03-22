# SOP: UI Building (Feature UI Lifecycle)

> **Purpose**: Build production UI from wireframe to shippable Android code.
> **Trigger**: User provides a demand (e.g., "build home page", "history drawer").
>
> **Read First**: [`ui-dev-mode.md`](./ui-dev-mode.md) defines the overall working model and approval gates for prototype-first UI development.
>
> **Invocation Rule**: This is the primary SOP to invoke when building an interface UI.

> **Default Modern Invocation**: User invokes this SOP and provides one or more screenshots. Unless the user explicitly says otherwise, treat those screenshots as structural wireframes and redesign the visual layer around them.

---

## Cardinal Rules

### 1. Zero-Contamination Principle
- **Wireframe** shows WHAT elements exist, NOT how they look.
- Sizes, gaps, radii, colors come from `style-guide.md` and `design-tokens.json`.
- Test app UI is for **structure discovery only** — never copy its visuals.

### 1a. Zero-Spec-Contamination (Literal Reading Trap)
- **Spec ASCII diagrams** define STRUCTURE and HIERARCHY only.
- Specs and SOPs must use plain professional prose with no emoji.
- Real UI uses **icons from the design system** (e.g., Material Icons, custom SVGs), never emoji Unicode.
- **Do NOT copy spec formatting** as UI styling. A textual "置顶" marker in spec becomes `PinIcon + "置顶"` in code.

### 1b. Current UI Is Logic Reference Only
- The current app UI may exist partly for testing convenience, state validation, and implementation coverage.
- Use current UI to understand logic, state wiring, ownership, and interaction hooks.
- Do **not** use current UI as the aesthetic source of truth unless it is explicitly marked as an approved visual reference.
- Aesthetic truth comes from approved prototypes, the style guide, and the UI element registry.

### 2. Explicit Checkpoints
Every phase transition requires explicit human confirmation, but the agent should keep the ceremony light.

Default checkpoint meanings:

- **design confirmed** → Start production transplant
- **"Ship It"** → Complete

Legacy phrases such as **"Brief Approved"** and **"Prototype Passed"** are still valid, but the default working model should not force the user to speak in ritual language when normal confirmation already makes intent clear.

### 3. Registry Compliance
Android implementation MUST reference `ui_element_registry.md` for:
- Z-Map layer assignment
- Interaction triggers/animations
- Invariants

### 4. Contract and Registry Re-Binding Is Mandatory
Screenshots and prototypes define visual direction, but they do not replace the real implementation boundaries.

Before and during Kotlin/Compose transplant, the agent must re-bind the approved design back to:

- `prism-ui-ux-contract.md` for state/intent boundaries and surface ownership
- `ui_element_registry.md` for component behavior, layer rules, and invariants

Simple law:

- screenshot drives structure
- prototype drives polish
- contract and registry drive production boundaries

Do not transplant visuals into Compose as freeform UI code detached from the contract and registry model.

---

## Phase 0: Intake

**Trigger**: User demand + one or more screenshots, optionally with text annotations.

**Agent Actions**:
1. Identify target component(s) from demand.
2. Treat provided screenshots as structural wireframes unless the user explicitly allows layout change.
3. Extract:
   - locked structure
   - flexible styling areas
   - required controls
   - relative placement rules
   - obvious product-role constraints
2. Read relevant spec sections:
   - `docs/plans/ui-tracker.md` — UI-specific status, approval, and drift tracking
   - `prism-ui-ux-contract.md` — Surface contract and UI boundary
   - `style-guide.md` — Visual identity guide and tokens
   - `ui_element_registry.md` — Element registry and behavior contract
   - `ui-dev-mode.md` — workflow expectations and approval model
4. Ask clarifying questions only when the screenshot and user notes do not provide enough structural truth.

**Output**: Concise confirmation of understanding, including:

- what is locked
- what may be redesigned
- which Surface Contract boundary the work belongs to
- which registry elements or invariants are likely involved
- what the next design step will be

---

## Phase 1: Structure Read and Design Framing

**Purpose**: Define what is fixed and where visual discretion is allowed.

**Agent Actions**:
1. Produce a concise design framing with:
   - User Goal
   - Locked Structure
   - Flexible Styling Areas
   - Surface Contract anchor
   - Registry anchor
   - In-Scope / Out-of-Scope elements
   - Visual Tokens or style direction to apply
   - Functional Invariants
   - Acceptance target for the next visual pass
2. If the user's screenshots and notes are already sufficiently clear, do not force a heavyweight separate brief artifact. A compact framing is enough.

**Checkpoint**: The user confirms the direction or gives corrections.

## Phase 2: Screenshot-Guided Design Pass

**Purpose**: Create a polished visual proposal from the supplied structural wireframe.

**Agent Actions**:
1. Use the screenshots as structural source-of-truth unless told otherwise.
2. Preserve:
   - required elements
   - semantic roles
   - interaction meaning
   - major placement contracts already expressed by the screenshots
3. Freely redesign:
   - spacing
   - radius
   - opacity
   - shape language
   - background treatment
   - visual composition
   - polish quality
4. Build the design pass in the most useful medium for fast review, normally the web prototype in `prototypes/prism-web-v1/`.
5. Screenshot the result and present it to the user.
6. Revise from user screenshot feedback until the design direction is confirmed.

**Checkpoint**: Once the user confirms the design direction, begin production transplant.

**Iteration Rule**: User feedback through additional screenshots, markup, or short comments should be treated as the primary refinement loop.

---

## Phase 3: Android Transplant

**Purpose**: Implement the confirmed design in Kotlin/Compose and keep iterating visually until the user says it is ready to ship.

### 3A. Gap Analysis (Light)
- Compare prototype CSS/JS with current Android code.
- List visual discrepancies (NOT code prescriptions).
- Map each major approved element back to:
  - contract-owned state input
  - contract-owned intent output
  - registry-owned behavior and layer rules
- Propose token/registry updates if needed.
- For deep audits, use `/14-ui-transplant`.

### 3B. Implementation
- Write Compose code in `app-core/`.
- Use tokens from `Color.kt`, `Type.kt`, `Theme.kt`.
- Respect registry Z-Map and invariants.
- Keep UI state reading and intent emission aligned with `prism-ui-ux-contract.md`.
- Prefer existing registry-aligned seams and interfaces instead of letting screenshot polish create new ownership leaks.
- Verify build passes.
- Preserve the screenshot-approved structure unless the user explicitly reopens layout decisions.

### 3C. Fidelity Check
- Screenshot Android device.
- Compare against:
  - confirmed prototype screenshots
  - user-provided structural screenshots
  - latest user correction screenshots, if any
- If gaps exist, iterate in Kotlin and re-screenshot.

**Checkpoint**: Keep iterating until the user says **"Ship It"**.

---

## Iteration Protocol

| Scenario | Action |
|----------|--------|
| Minor gap (color off) | Fix inline, re-screenshot |
| Structural gap (layout wrong) | Return to 3A, re-analyze |
| Spec conflict discovered | Escalate to `/01-senior-reviewr` |

During UI iteration, default evidence order is:

1. latest user screenshot correction
2. latest confirmed design screenshot
3. approved prototype
4. style guide and registry

If these conflict, do not guess silently. State the conflict and propose the narrowest correction.

Implementation-boundary rule during iteration:

- visual corrections may change polish freely
- behavioral boundary changes must still be justified through the Surface Contract or UI Element Registry
- do not let screenshot feedback silently introduce data-layer coupling, ownership drift, or ad-hoc interaction rules

---

## Workflow Diagram

```
Phase 0: INTAKE -> Phase 1: STRUCTURE READ
                                 |
                 Phase 2: DESIGN PASS + SCREENSHOT ITERATION
                                 |
                      user confirms design direction
                                 |
                 Phase 3: COMPOSE TRANSPLANT + DEVICE ITERATION
                                 |
                              "Ship It"
```

---

## Reference Documents

| Document | Purpose |
|----------|---------|
| [`docs/plans/ui-tracker.md`](file:///home/cslh-frank/main_app/docs/plans/ui-tracker.md) | Dedicated UI status tracker |
| [`ui-dev-mode.md`](file:///home/cslh-frank/main_app/docs/sops/ui-dev-mode.md) | Instruction doc for the overall UI dev model |
| [`prism-ui-ux-contract.md`](file:///home/cslh-frank/main_app/docs/specs/prism-ui-ux-contract.md) | Surface contract and UI index |
| [`style-guide.md`](file:///home/cslh-frank/main_app/docs/specs/style-guide.md) | Visual tokens |
| [`design-tokens.json`](file:///home/cslh-frank/main_app/docs/design/design-tokens.json) | Master tokens |
| [`ui_element_registry.md`](file:///home/cslh-frank/main_app/docs/specs/ui_element_registry.md) | Behavior contracts |

---

## Completion Criteria

A UI feature is **DONE** when:
1. [ ] Screenshot-driven structure and constraints are understood
2. [ ] Design direction is confirmed by the user
3. [ ] Android implementation matches the confirmed direction
4. [ ] Gradle build passes
5. [ ] User declares **"Ship It"**
