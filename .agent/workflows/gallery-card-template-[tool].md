---
description: Generate consistent gallery cards using locked template pattern
---

# Gallery Card Template

Generate or revise design variant cards for the SmartLab Gallery.

---

## Source of Truth

| Document | Purpose |
|----------|---------|
| `docs/specs/style-guide.md` | Master design spec |
| `docs/design/design-tokens.json` | Token values |

All variants are interpretations of the style-guide.

---

## Variation Scale (1-8)

| Level | Name | Description |
|-------|------|-------------|
| 1 | Faithful | Exact style-guide execution |
| 2 | Refined | Minor polish adjustments |
| 3 | Adapted | Contextual modifications |
| 4 | Enhanced | Added flair within spec |
| 5 | Exploratory | Testing boundaries |
| 6 | Experimental | New interpretations |
| 7 | Divergent | Alternative approaches |
| 8 | Creative | Full innovation |

Specify level when invoking workflow.

---

## Phase 1: Design (Blueprint + Slots)

### Blueprint

```text
┌──────────────────────────────────────┐
│  {element_1}                         │
│  {element_2}                         │
│  ...                                 │
└──────────────────────────────────────┘
```

### Slots

| Slot |
|------|
| `container.background` |
| `container.border` |
| `container.radius` |
| `container.shadow` |
| `container.shadow.spotColor` |
| `container.padding` |
| `title.font` |
| `title.size` |
| `title.weight` |
| `title.case` |
| `content.size` |
| `content.opacity` |
| `accent` |

**Wait for approval before Phase 2.**

---

## Phase 2: Generate

### Injection Targets

| Variant | Container |
|---------|-----------|
| V17 | `.spec-graphic.graphic-v17` |
| V16 | `.spec-graphic.graphic-v16` |
| V15 | `.spec-graphic.graphic-v15` |
| V14 | `.spec-graphic.graphic-v14` |
| V12 | `.spec-graphic.graphic-v12` |
| CH-J | `.spec-graphic.graphic-chj` |

---

## Anti-Patterns

| ❌ Don't | ✅ Do |
|----------|-------|
| Use abstract text | Use real Chinese content |
| Invent structure | Follow blueprint |
| Skip approval | Get Phase 1 approved |
| Freestyle styling | Fill slots from style-guide |

---

## Acceptance

- [ ] Variation level declared
- [ ] Blueprint approved
- [ ] Slots from style-guide
- [ ] Injected to correct container
- [ ] Chinese text content
