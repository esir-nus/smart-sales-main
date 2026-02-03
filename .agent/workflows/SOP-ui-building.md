---
description: UI Building SOP entrance — read SOP, build feature UI through 4-phase process
---

# SOP: UI Building

> **Entrance Workflow**: This workflow routes to the full SOP document.

## Quick Start

When user invokes `/SOP-ui-building`:

1. **Read the full SOP**: [`docs/sops/ui-building.md`](file:///home/cslh-frank/main_app/docs/sops/ui-building.md)
2. **Follow the 4-phase process**:
   - Phase 0: Intake (understand demand)
   - Phase 1: Design Brief (`/12-ui-director`)
   - Phase 2: Web Prototype (`/13-web-prototype`)
   - Phase 3: Android Transplant (iterate until "Ship It")

## 📚 Lessons-Learned Check (Mandatory)

> **At Entry and Phase 3**, check `.agent/rules/lessons-learned.md` for UI-related patterns.

If a lesson **matches** the current work (e.g., Compose patterns, drawer issues):
1. **Tell the user**: "📚 Lesson applied: [TITLE] — avoiding known pitfall"
2. **Proactively apply** the documented pattern
3. **Avoid** repeating past mistakes

**Format**: `📚 Lesson applied: [Title] — [one-line summary]`

---

## Checkpoints (Require Explicit User Approval)

| Checkpoint | User Says | Action |
|------------|-----------|--------|
| Brief Ready | "Brief Approved" | Start Prototype |
| Prototype Ready | "Prototype Passed" | Start Transplant |
| Implementation Ready | "Ship It" | Complete |

## Cardinal Rules

1. **Zero-Contamination**: Wireframe = structure only. Styling from `style-guide.md`.
2. **Registry Compliance**: Android must follow `ui_element_registry.md`.
3. **No Autonomous Phase Transitions**: Wait for explicit user approval.

## Reference

- Full SOP: [`docs/sops/ui-building.md`](file:///home/cslh-frank/main_app/docs/sops/ui-building.md)
- Style Guide: [`docs/specs/style-guide.md`](file:///home/cslh-frank/main_app/docs/specs/style-guide.md)
- Registry: [`docs/specs/ui_element_registry.md`](file:///home/cslh-frank/main_app/docs/specs/ui_element_registry.md)
