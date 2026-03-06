---
description: Audit interface-map.md against the codebase to detect drift, architecture violations, and skeleton implementations
---

# 🕵️ Interface Map Audit Workflow

> **Purpose**: The `interface-map.md` is the central nervous system of Cerb. However, code rapidly outpaces static documentation. This workflow actively detects when implementations drift from their spec, exposes "fake" implementations masquerading as shipped code, and ensures architectural invariants (like clean domain layers) are respected.

**Trigger**: `/07-interface-map-audit`

## Rules of Engagement
- **The Code is the Reality**: If the code and the map disagree, the code is treated as the truth (unless it violates a core Prism architectural rule). The map must be updated to reflect the code.
- **Evidence-Based Only**: Do not guess interface signatures. You MUST use tools to view the actual Kotlin files.
- **No Scope Creep**: This is an audit, not a refactoring session. Find the problems, document them, and fix the map. Do not attempt to rewrite the drifting Kotlin code unless explicitly requested.

---

## Execution Steps

**1. Create Audit Artifacts**
Create a `task.md` outlining the modules to audit (grouped by layers 1-5 from `interface-map.md`), and an `audit-report.md` to hold your findings.

**2. Phase 1: Environment & Skeleton Detection**
- **Fake Masquerades**: Check the dependency injection bindings (e.g., `PrismModule.kt`). Look for modules marked as `✅ Shipped` in the interface map that are actually bound to `Fake` implementations.
- **Domain Contamination**: Search `app-prism/src/main/java/**/domain/` for forbidden imports including `android.*`, `android.app.*`, `feature.*`, and `com.smartsales.legacy.*`.

**3. Phase 2: Layer-by-Layer Signature Parity**
For **each** module listed in `interface-map.md`:
- Locate the primary interface file in the codebase.
- Compare it precisely against the "Key Interface" column.
- **Check for**:
    - **Wrappers**: Does the spec say `Result<T>` but the code returns `T`?
    - **Async Mismatch**: Does the spec say `Flow<T>` but the code uses a `suspend` function?
    - **Argument Explosion**: Does the spec use a single payload object (e.g., `ContextRequest`) while the code takes exploded arguments?
    - **Return Type Drift**: Are the returned domain objects identical?
- **Document**: In your `audit-report.md`, explicitly list the Spec Signature vs. Actual Code Signature, and declare the verdict (Aligned or Drift).

**4. Phase 3: Remediation & Sync**
- **Map Update**: Update `interface-map.md` to reflect the *actual* Kotlin signatures found in Phase 2.
- **Status Downgrades**: If a module was a Fake masquerade, downgrade its status in the map (e.g., from `✅` to `📐`).
- **Tech Debt**: Update `docs/plans/tracker.md` to log any severe architectural violations (e.g., Android imports in the domain layer) under the Tech Debt section.
- **Notify**: Inform the user with a summary of the worst offenders and request review of the updated documents.
