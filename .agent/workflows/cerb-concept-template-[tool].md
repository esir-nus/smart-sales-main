# Cerb Concept Scaffolder

> **Purpose**: Create a standardized conceptual specification for unconstrained brainstorming ("The Fifth Pillar").
> **Output**: `docs/to-cerb/[feature-name]/concept.md`
> **Note**: Concepts are ephemeral mental models. They exist to gain user alignment *before* any formal interfaces or database schemas are committed to code. They must be deleted once translated into a formal `docs/cerb/` spec.

---

## Step 1: Define the Mental Model

Create `docs/to-cerb/[feature-name]/concept.md`.

**Rule**: No premature code logic. No `interface.md`. Focus strictly on metaphors, user experience, and anti-illusion bounds.

```markdown
# [Feature Name] Concept

> **Drafting Protocol**: This document strictly defines the mental model, user experience, and architectural boundaries. Premature concrete interfaces and database schemas are intentionally deferred.

---

## 1. The Mental Model

Define the core metaphor and nature of this feature. How does it fit into the OS model (Kernel, RAM, SSD, UI)?

| Characteristic | Description |
|---|---|
| **Nature** | What fundamentally is this? |
| **Metaphor** | What real-world object represents this? |
| **Lifecycle** | Ephemeral (Session-bounded) or Persistent? |

---

## 2. The User Flow & Experience

Describe the end-to-end journey from the user's perspective (Frank).

### Stage A: Trigger & Creation
1. How does the user invoke this feature?
2. What happens in the background?
3. How does the system respond?

### Stage B: Interaction & UI Response
1. What UI components change? 
2. Is the interaction session-bounded?
3. How is Context mapped? (e.g., Sniffer, Injector)

---

## 3. System Boundaries & Cross-Module Connections

Map the expected blast radius when this concept becomes real code.

### A. The Storage Layer (`:data:[module]`)
- Where does the data live? (SSD vs RAM)
- *Constraint*: [Architectural boundaries to respect]

### B. The Awareness Layer (`:core:context` / `:core:pipeline`)
- How does the pipeline route to this?
- *Risk*: [e.g., Token bloat, pipeline blocking]

### C. The Presentation Layer (`:app-prism`)
- What raw UI states are needed?

---

## 4. Anti-Hallucination & Anti-Illusion Proofing

Define the rigid constraints to ensure the LLM cannot lie or invent state.

1. **Information Asymmetry Guarantee**: How do we ensure the LLM only knows about things that actually exist in the native subsystem?
2. **Strict Grounding**: System prompt boundaries.
3. **UI Source of Truth**: How does the UI verify state independently from LLM text generation?
```

---

## Checklist

- [ ] `concept.md` created (Mental Model + User Flow + Boundaries)
- [ ] Anti-Hallucination constraints strictly defined
- [ ] Output presented to User for Senior Review approval

**Done. Await user feedback before proceeding to `/feature-dev-planner` formalization.**
