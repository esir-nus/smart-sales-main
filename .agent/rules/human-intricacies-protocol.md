---
trigger: always_on
description: Human Intricacies Protocol - Capturing organic UX and battle-tested realities over pure logic
---

# Human Intricacies Protocol

> **Core Principle**: Pure reasoning builds systems that work in a vacuum. Intricacies build systems that survive contact with the user.

"Intricacies" refers to the human developer's battle-tested understanding and organic UX experience that an agent can rarely deduce through pure logic.

## 1. The Reality of the User
**Agents think**: Users flow linearly from A to B.  
**Humans know**: Users will:
- Have typos, homophones, and use aliases (e.g., search queries)
- Ghost sessions (walk away without formally disconnecting)
- Rage-tap buttons when the network is slow
- Enter a screen with 0 data (Empty State) and need a Call To Action (CTA), not just a blank grid

## 2. When to Apply the Protocol
This protocol must be explicitly evaluated during:
- **Spec Writing** (`/cerb-spec-template`, `/cerb-ui-template`, etc.)
- **Feature Dev Planning** (`/feature-dev-planner`)

## 3. The "Intricacies" Spec Requirement
No Cerb documentation is complete without logging the "dark matter" of the feature.

**Every `spec.md` MUST include:**
```markdown
## 🧠 Intricacies & Organic UX Constraints
- **Organic UX**: (e.g., "Users will spam submit; enforce UI-level debounce.")
- **Data Reality**: (e.g., "SQL is too rigid for human typos here. We need fuzzy matching or an LLM parser.")
- **Failure Gravity**: (e.g., "If this specific API drops due to a tunnel disconnect, do we cache the intent?")
```

## 4. The Action Rule (Agent Behavior)
1. **Translate When Obvious**: When an agent recognizes an Intricacy (e.g., "Network might drop"), it should translate this into standard constraints (e.g., "Implement retry with exponential backoff").
2. **Halt and Discuss**: If an Intricacy requires a structural tradeoff (e.g., "Do we write an entirely new fuzzy-search index or just use an LLM router?"), the agent MUST halt and flag this for **Open Discussion with the Developer** during the planning phase.
3. **No 14-Layer Abstractions**: Do not try to solve human chaos with mathematically "perfect" but over-engineered abstractions. Solve it pragmatically.
