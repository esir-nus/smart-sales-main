---
description: Read interface-map.md before any cross-module code change
trigger: always_on
---

# Cross-Module Check

Before modifying code that touches more than one cerb module:

1. **Read** `docs/cerb/interface-map.md`
2. **Verify** the interaction you're about to create/change is declared in the ownership table
3. **If not declared**: Stop. Flag as new edge. Update the map before writing code.

## Quick Test

> "Am I storing Module B's data on Module A's model?"

If yes → **STOP**. Query B's interface at runtime instead.

## When This Applies

- Adding a field that references another module's domain (e.g., `entityId` on a Task)
- Creating a new import path between modules
- Changing which module owns a piece of data

## When This Does NOT Apply

- Changes within a single module
- UI-only changes (ViewModel → Composable)
- Test code
