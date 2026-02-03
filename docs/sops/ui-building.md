# SOP: UI Building (Feature UI Lifecycle)

> **Purpose**: Build production UI from wireframe to shippable Android code.
> **Trigger**: User provides a demand (e.g., "build home page", "history drawer").

---

## 🛑 Cardinal Rules

### 1. Zero-Contamination Principle
- **Wireframe** shows WHAT elements exist, NOT how they look.
- Sizes, gaps, radii, colors come from `style-guide.md` and `design-tokens.json`.
- Test app UI is for **structure discovery only** — never copy its visuals.

### 1a. Zero-Spec-Contamination (Literal Reading Trap)
- **Spec ASCII diagrams** define STRUCTURE and HIERARCHY only.
- **Emojis in specs** (📌, 📅, 🗓️) are for **documentation readability** — NOT for visual implementation.
- Real UI uses **icons from the design system** (e.g., Material Icons, custom SVGs), never emoji Unicode.
- **Do NOT copy spec formatting** as UI styling. "📌 置顶" in spec → `PinIcon + "置顶"` in code.

### 2. Explicit Checkpoints
Every phase transition requires **user's explicit declaration**:
- **"Brief Approved"** → Start Prototype
- **"Prototype Passed"** → Start Transplant
- **"Ship It"** → Complete

### 3. Registry Compliance
Android implementation MUST reference `ui_element_registry.md` for:
- Z-Map layer assignment
- Interaction triggers/animations
- Invariants

---

## 📋 Phase 0: Intake

**Trigger**: User demand + optional wireframe screenshot.

**Agent Actions**:
1. Identify target component(s) from demand.
2. Read relevant spec sections:
   - `style-guide.md` — Visual tokens
   - `ui_element_registry.md` — Behavior contract
   - `prism-ui-ux-contract.md` — UX flows (if applicable)
3. Ask clarifying questions if scope is ambiguous.

**Output**: Confirmation of understanding.

---

## 📝 Phase 1: Design Brief (`/12-ui-director`)

**Purpose**: Define WHAT to build with explicit guardrails.

**Agent Actions**:
1. Create **Design Brief** artifact with:
   - User Goal
   - In-Scope / Out-of-Scope elements
   - Visual Tokens to apply (from `style-guide.md`)
   - Functional Invariants (from registry)
   - Acceptance Criteria (visual)
2. **STOP** and request approval.

**Checkpoint**: User must say **"Brief Approved"**.

---

## 🌐 Phase 2: Web Prototype (`/13-web-prototype`)

**Purpose**: Create a high-fidelity visual reference in browser.

**Agent Actions**:
1. Build prototype in `prototypes/prism-web-v1/`.
2. Follow `style-guide.md` tokens exactly.
3. Apply Zero-Chrome rules (no scrollbars, no focus rings).
4. Screenshot and present to user.
5. **STOP** and request approval.

**Checkpoint**: User must say **"Prototype Passed"**.

**Iteration**: If user provides feedback, revise and re-present.

---

## 📲 Phase 3: Android Transplant

**Purpose**: Implement the approved prototype in Kotlin/Compose.

### 3A. Gap Analysis (Light)
- Compare prototype CSS/JS with current Android code.
- List visual discrepancies (NOT code prescriptions).
- Propose token/registry updates if needed.
- For deep audits, use `/14-ui-transplant`.

### 3B. Implementation
- Write Compose code in `app-prism/`.
- Use tokens from `Color.kt`, `Type.kt`, `Theme.kt`.
- Respect registry Z-Map and invariants.
- Verify build passes.

### 3C. Fidelity Check
- Screenshot Android device.
- Compare against prototype screenshot.
- If gaps exist, iterate.

**Checkpoint**: User must say **"Ship It"** or provide feedback for iteration.

---

## 🔄 Iteration Protocol

| Scenario | Action |
|----------|--------|
| Minor gap (color off) | Fix inline, re-screenshot |
| Structural gap (layout wrong) | Return to 3A, re-analyze |
| Spec conflict discovered | Escalate to `/01-senior-reviewr` |

---

## 📊 Workflow Diagram

```
Phase 0: INTAKE → Phase 1: BRIEF → 🛑 "Brief Approved"
                                          ↓
                 Phase 2: PROTOTYPE → 🛑 "Prototype Passed"
                                          ↓
                 Phase 3: TRANSPLANT → 🔄 Iterate → 🛑 "Ship It"
```

---

## 📚 Reference Documents

| Document | Purpose |
|----------|---------|
| [`style-guide.md`](file:///home/cslh-frank/main_app/docs/specs/style-guide.md) | Visual tokens |
| [`design-tokens.json`](file:///home/cslh-frank/main_app/docs/design/design-tokens.json) | Master tokens |
| [`ui_element_registry.md`](file:///home/cslh-frank/main_app/docs/specs/ui_element_registry.md) | Behavior contracts |

---

## ✅ Completion Criteria

A UI feature is **DONE** when:
1. [ ] Design Brief approved
2. [ ] Web Prototype passed
3. [ ] Android implementation matches prototype
4. [ ] Gradle build passes
5. [ ] User declares **"Ship It"**
