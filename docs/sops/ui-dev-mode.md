# UI Dev Mode

> Purpose: Define the controlled UI development model for designers, developers, and agents.
>
> Goal: Allow free visual exploration without losing consistency, professionalism, or implementation control.
>
> Status: Active SOP
>
> Last Updated: 2026-03-28

---

## Why This Mode Exists

This repo treats UI as a surface layer connected to product behavior through explicit contracts.

The system should support both:

- visual freedom at the prototype stage
- strict consistency at the production stage

UI work must not depend on repeated codebase spelunking just to rediscover how the product should look and feel.

Instead, UI work should follow a stable document chain:

1. surface contract
2. visual identity guide
3. UI element registry
4. prototype-first workflow
5. approved production transplant
6. UI tracker

---

## Battle-Tested Conclusion

The most successful UI delivery pattern in this repo is now explicit:

1. use a strong UI design agent to explore and build the prototype quickly, using agent-browser / browser assistance for visual reference and comparison
2. use Codex to do the surgical production transplant and controlled iteration
3. use the Surface Contract and UI Element Registry to keep the production result governable

Important interpretation:

- the prototype is the primary engine for visual invention
- Codex is the primary engine for repo-safe transplant, refinement, and verification
- the contract and registry are not the main source of aesthetic creativity
- the contract and registry are the control layer that keeps the shipped UI from drifting into ownership leaks, fake seams, or accidental behavior changes

Simple law:

- prototype decides what great should look like
- Codex decides how to land it safely in production
- contract and registry decide whether the landing remains structurally legal

Authority split for active UI tracking:

- prototype = alignment source and review checkpoint set
- accepted implementation = current delivered reality
- UI tracker = progress memory, drift memory, and implementation-ahead logging

Rule:

- the prototype remains highly prescriptive, but not perfectly final forever
- after real implementation and polish, accepted code/design may lawfully move ahead of the prototype
- when that happens, the tracker must record the new accepted state instead of pretending the prototype is still fully exhaustive

Default runtime rule:

- Codex does not do first-pass prototyping by default
- Codex may prepare the brief, review criteria, or transplant plan, but the UI design agent owns prototype authorship unless the user explicitly overrides that rule

---

## Core Mental Model

Use this model when discussing UI work:

- skeleton = domain truth, business rules, persistent data
- muscles = state machines, ViewModels, intents, orchestration
- skin = layout, styling, motion, visual hierarchy, presentation

The skin may evolve freely, but it must stay attached to the same behavioral body.

That means:

- visual form may change
- UX invariants may not break
- business logic may not leak into raw UI styling decisions
- production implementation must still obey the real state and intent contracts

Add one more practical lens:

- prototype work is a creative search problem
- production transplant is a precision engineering problem

Treating both as the same kind of work caused avoidable confusion in older UI flows.

---

## Working Names

Some existing files still use older naming. For this workflow, use the intuitive names below when thinking and communicating:

| Working Name | Current Repo File | Role |
|--------------|-------------------|------|
| Surface Contract | `docs/specs/prism-ui-ux-contract.md` | UI boundary, state/intent decoupling, feature-spec index |
| Visual Identity Guide | `docs/specs/style-guide.md` | Global visual law, aesthetic standards, token intent |
| UI Element Registry | `docs/specs/ui_element_registry.md` | Component behavior, states, layers, invariants |
| Prototype-First UI Workflow | `docs/sops/ui-building.md` | Prototype, approval, transplant sequence |
| UI Tracker | `docs/plans/ui-tracker.md` | UI-specific execution tracking, approval state, visual drift |

Rule:

- prefer intuitive names in discussion
- preserve existing file paths unless a deliberate doc migration is approved

---

## When To Use UI Dev Mode

Use this mode when:

- designing a new screen
- redesigning a major surface
- creating a new visual pattern
- refining interaction polish
- building a web prototype before Android implementation
- transplanting an approved prototype into Compose

Do not use this mode for small copy-only edits or tiny visual bug fixes unless they affect shared patterns.

---

## Which SOP To Invoke

For actual interface-building work, invoke:

- `docs/sops/ui-building.md`

Treat this file as the governing model behind that SOP.

Simple rule:

- `ui-building.md` = the execution SOP
- `ui-dev-mode.md` = the background law and explanation

Developers normally should not need to invoke both separately.

---

## `polishing ui` Invocation Contract

When the user invokes `polishing ui`, use this default interpretation unless the user explicitly overrides it:

1. the user is asking for **one page / surface at a time**
2. the current deployed UI is the implementation baseline to inspect
3. the user will provide the prototype counterpart against that deployed UI
4. the prototype HTML is part of the visual source package and should be treated as sufficient alignment context for planning
5. the task is a **surgical high-fidelity prototype alignment** pass, not an invitation to widen into a broad redesign

Default planning posture for `polishing ui`:

- compare current deployed UI against the provided prototype target
- preserve the real product structure and behavior unless the user explicitly asks for structural change
- identify the minimum production write scope needed to close the highest-value fidelity gaps
- plan around transplant, tightening, and visual drift removal rather than fresh aesthetic invention
- update `docs/plans/ui-tracker.md` for the owning page / surface so the active polish scope and gate stay explicit

Simple law:

- one `polishing ui` invocation = one page / surface polishing track
- the provided deployed UI + prototype reference + prototype HTML is the normal working packet
- default output = surgical polish plan for prototype-faithful landing

---

## Status-Bar-Aware Transplant Rule

For prototype-to-Compose transplant, treat the prototype status bar as a visual placeholder only.

- prototype may reserve `44px` at the top of the emulator frame
- Compose must use real `WindowInsets.statusBars` / `WindowInsets.navigationBars`
- do not hardcode safe-area heights in Compose
- do not hide inset ownership at the host root when the actual surface is an independent drawer, sheet, or monolith
- default top-reaching surfaces use the blank-band rule from the style guide
- exception surfaces may preserve spec-owned top-monolith/header alignment, but they still must become status-bar-aware at the real Android layer

---

## Required Reading Order

Before doing major UI work, read in this order:

1. `docs/plans/ui-tracker.md`
2. `docs/specs/prism-ui-ux-contract.md`
3. `docs/specs/style-guide.md`
4. `docs/specs/ui_element_registry.md`
5. feature-specific spec in `docs/cerb-ui/**` or `docs/cerb/**`
6. `docs/sops/ui-building.md`
7. `docs/plans/tracker.md` when the UI task is part of a larger product/feature wave

If a `docs/core-flow/**` doc exists for the feature, read it before treating lower docs as final.

---

## Tracking Model

Use this tracking hierarchy in `docs/plans/ui-tracker.md`:

1. **Family**
   - the prototype/source container or campaign summary
2. **Page / Surface**
   - the default implementation-tracking unit
3. **Core View when needed**
   - add this layer only when the prototype page exposes meaningful review states that materially affect alignment, transplant, or polish

Examples:

- `sim-shell-family` = family
- `scheduler_drawer` = page/surface
- `Open (Normal Collapsed)` / `Calendar Unfolded` / `Expanded: Details` = core views

Rule:

- family rows summarize direction
- page/surface rows log the main implementation track
- core-view rows log state-specific polish, drift, or implementation-ahead changes when the prototype provides those checkpoints

---

## The Workflow

### Phase 1: Frame the Surface

Understand:

- the feature goal
- the real user task
- the state and intent boundary
- the UX invariants that must survive any redesign

Output:

- concise UI brief
- list of locked constraints
- list of flexible visual areas

### Phase 2: Prototype First

Create a web prototype before Android implementation for major UI work.

The prototype should:

- follow the Visual Identity Guide
- reuse existing registry patterns where possible
- explore layout and visual direction freely
- avoid violating platform or product reality

Rule:

- do not treat prototype code as production code
- use agent-browser / browser assistance during prototype work for visual reference gathering, comparison against approved screenshots or parent prototypes, and screenshot-review support

Battle-tested role split:

- a UI-focused design agent owns this phase by default
- this phase should optimize for visual quality, speed of exploration, and screenshot reviewability
- Codex should not silently take over prototype authorship just because it can edit files in the repo
- do not force the prototype phase to think like Compose implementation too early

### Phase 3: Screenshot Critique

After the prototype is rendered, review it from screenshots.

Check:

- visual consistency
- hierarchy clarity
- spacing rhythm
- component fidelity
- professionalism
- accessibility and contrast risks
- drift from the Visual Identity Guide
- drift from the UI Element Registry

This is a critique step, not vague “learning.”

### Phase 4: Human Approval

Before production transplant, the human reviewer must confirm the prototype direction.

No major UI redesign should skip this gate.

### Phase 5: Production Transplant

Only after approval:

- map prototype elements to registry elements
- map approved visuals to real Compose structure
- connect the UI to the real state and intent contract
- implement production-safe behavior

Rule:

- transplant approved design intent, not raw prototype hacks

Battle-tested role split:

- Codex should own this phase whenever possible
- Codex should work surgically inside the existing repo boundaries rather than re-inventing the design in Kotlin
- once the prototype is approved, the Kotlin phase should bias toward faithful transplant and micro-iteration, not renewed aesthetic exploration

### Phase 6: Verification

Verify production UI against:

- approved prototype
- Visual Identity Guide
- UI Element Registry
- Surface Contract
- feature behavior spec

---

## Non-Negotiable Rules

### 1. Contract First

The UI reads state and emits intents.

It must not directly depend on:

- database row shapes
- pipeline internals
- LLM internal structures
- storage-layer implementation details

Clarification:

- "contract first" does not mean "invent the UI from the contract"
- the contract defines legal boundaries, ownership, state inputs, and intent outputs
- it is a production control surface, not a substitute for design taste

### 2. Prototype Before Major Android UI Work

For meaningful screen or pattern work:

- prototype first
- approve
- transplant second

Do not jump straight into Compose unless the task is minor.

Battle-tested refinement:

- for medium or major UI work, jumping straight to Compose usually produces slower iteration and weaker polish
- the prototype should absorb the expensive aesthetic trial-and-error before Kotlin starts

### 2a. Current UI Is Not The Aesthetic Teacher

The current app UI often exists partly for testing convenience, state validation, and flow coverage.

Therefore:

- agents may inspect current UI to understand logic, state flow, interaction wiring, and implementation constraints
- agents must not treat current UI as the visual source of truth by default

Do not learn the target aesthetic, polish standard, spacing quality, or motion language from the current production UI unless it is explicitly marked as an approved visual reference.

Visual source-of-truth order:

1. approved prototype screenshots
2. Visual Identity Guide
3. UI Element Registry
4. explicit approved visual references

### 2b. Screenshot-First Structural Wireframe Rule

When the user provides a screenshot for UI work, treat it as a structural wireframe unless the user explicitly says the layout itself should change.

That means the screenshot is usually authoritative for:

- what primary elements must exist
- what each element's product role is
- which controls are required
- the rough layer hierarchy
- the relative left / center / right placement of key controls

That same screenshot is usually not authoritative for:

- polish quality
- color treatment
- corner radius
- opacity
- spacing rhythm

---

## Role Split That Actually Works

| Role | Best Owner | Main Job | Must Not Drift Into |
|------|------------|----------|---------------------|
| Visual exploration | UI design agent | generate and refine the prototype, use browser assistance for visual reference/comparison, tune mood, spacing, hierarchy, and material feel | production wiring decisions |
| Structural approval | human reviewer | approve direction, reject weak visual interpretations, lock layout/tone decisions | low-level Kotlin implementation detail |
| Surgical transplant | Codex | map approved prototype into Compose with minimal behavior drift | fresh visual reinvention without approval or first-pass prototype authorship |
| Contract control | Surface Contract | define legal state and intent boundaries | acting as a visual mood board |
| Pattern control | UI Element Registry | define component invariants, behavior expectations, and layer discipline | acting as the first design sketch |
| Shipment control | UI tracker + acceptance report | make approval, fidelity, drift, and ship state explicit | replacing the actual prototype comparison |

---

## Lessons Learned

### 1. The prototype must lead the aesthetic

The strongest UI outcomes came from letting the prototype become excellent first.

If Kotlin implementation starts while the design is still aesthetically unresolved, the repo pays for that uncertainty multiple times.

### 2. Codex is strongest at precision, not first-pass taste search

Codex performs best when asked to:

- transplant
- tighten
- align
- remove drift
- preserve behavior while improving fidelity

This is different from asking it to invent the full visual language from repo code alone.

Operational default:

- Codex should not be the first-pass prototype author
- if no prototype exists yet, Codex should help frame the brief and review criteria, then wait for the UI design agent's prototype pass

### 3. Contract and registry are useful, but mainly as governance tools

Their highest-value use is:

- protecting behavior boundaries
- preventing ownership drift
- preserving interaction invariants
- keeping shared component semantics stable

Their lowest-value use is pretending they can replace prototype quality.

So the practical conclusion is:

- yes, they are effective
- but mainly as guardrails, review criteria, and transplant management tools
- not as the main spark for great visual design

### 4. Surgical transplant beats rewrite energy

Once the prototype is approved, the best Android phase is:

- smallest viable write scope
- local token and spacing changes first
- preserve state mapping
- preserve ownership
- iterate from screenshots and device evidence

Large Kotlin redesigns after prototype approval usually create avoidable drift.

### 5. UI tracker plus explicit acceptance closes the loop

The tracker is not ceremony. It prevents hidden state such as:

- "prototype approved but transplant not started"
- "transplant done but fidelity still drifting"
- "looks close" without an acceptance decision

The acceptance note converts taste discussion into documented evidence.

---

## Battle-Tested SOP Summary

Use this operating sequence for future major UI work:

1. frame the surface and read the docs
2. build the prototype with a UI design agent using browser assistance
3. review by screenshots until the direction is explicitly approved
4. lock the structure and tone
5. let Codex perform the Compose transplant surgically
6. use contract and registry as production guardrails during transplant
7. compare device output against the approved prototype
8. record final acceptance in the UI tracker and, when needed, an audit report
- size balance
- proportions
- chrome treatment
- visual composition quality

Default agent behavior:

- preserve semantic structure first
- redesign visual treatment second
- do not reinvent layout when the user's screenshot already expresses the intended product structure

Use this distinction in every screenshot-driven request:

- locked structure = what must stay
- flexible styling = what may be redesigned

### 2c. Agent UI Specialist Discretion

The agent may use UI specialist discretion only inside the flexible styling layer.

This discretion is allowed for:

- improving spacing
- improving proportions
- improving alignment
- simplifying unnecessary containers
- changing shape language
- changing background blending
- improving color hierarchy
- polishing visual weight and composition

This discretion is not allowed to silently change:

- required controls
- control count
- control role
- control placement when the user has already implied a layout contract
- interaction meaning
- feature hierarchy expressed by the screenshot or annotation

Simple law:

- agents may polish aggressively
- agents may not restructure casually

### 2d. Concrete Example: Header Top Bar

If the user provides a top-bar screenshot and says:

- keep hamburger on the left
- keep dynamic island in the middle
- keep new-chat button on the right

then those are locked structural requirements.

The agent must preserve:

- hamburger entry on the left
- dynamic island in the center
- new action on the right

The agent may still redesign:

- whether the side icons sit inside circular pancakes or not
- icon container size
- icon container color
- bar height
- island shape
- island border
- island blending into the background
- square vs rounded vs pill treatment
- fade behavior at the edges
- spacing and overall polish

In that example, the screenshot is a good production wireframe with ugly styling, not a final visual design.

The correct agent interpretation is:

- keep the product skeleton
- upgrade the visual skin

Do not collapse this distinction.
5. current UI code and screenshots for logic understanding only

### 3. No Silent New Design Language

If a task introduces a new recurring pattern, update the relevant guide or registry in the same session.

Examples:

- new card family
- new button style
- new overlay layer
- new motion rule
- new spacing/radius convention

### 4. Registry Beats Improvisation

If a known element already exists in the registry, reuse or extend it deliberately.

Do not create near-duplicates by accident.

### 5. Human Approval Is a Real Gate

Prototype approval is not optional for major UI changes.

### 6. Production Still Obeys Real Contracts

Visual freedom does not allow:

- breaking state semantics
- hiding critical feedback
- removing required error/disabled/loading states
- bypassing existing feature ownership

---

## Expected Deliverables For Major UI Work

For a meaningful UI task, the working set should usually include:

- UI brief
- prototype
- screenshot set
- critique notes
- approval note
- transplant mapping
- production implementation
- verification notes
- UI tracker entry

---

## Done Criteria

A major UI task is not done until:

- the prototype direction is approved
- production code matches the approved direction closely enough
- the implementation still obeys the real state/intent contract
- any new shared pattern is documented
- relevant verification is complete

---

## Relationship To Existing Docs

This SOP explains the dev model.

It does not replace:

- the Surface Contract
- the Visual Identity Guide
- the UI Element Registry
- feature-level UI specs

It tells developers and agents how to use them together in one controlled system.
