# Platform Overlay Docs

> **Purpose**: Define where platform-specific delivery constraints live without forking the full product docs tree.
> **Primary Law**: `docs/specs/platform-governance.md`

---

## Directory Contract

- `docs/platforms/android/**` owns Android-specific delivery deltas
- `docs/platforms/harmony/**` owns Harmony-native delivery deltas

Shared product truth stays in:

- `docs/core-flow/**`
- `docs/cerb/**`
- `docs/cerb-ui/**`
- `docs/specs/**`

---

## What Belongs Here

Use platform overlays for:

- permissions
- lifecycle
- notifications / reminders
- background execution
- device integration
- packaging
- native runtime constraints

Do not move product-intent or business-rule truth here unless the user-visible behavior truly diverges by platform.

---

## Split Rule

Default rule:

- shared spec plus platform overlay

Escalate to a platform-specific companion spec only when the platform owns most of the implementation contract or the user-visible behavior diverges enough that overlays would be misleading.
