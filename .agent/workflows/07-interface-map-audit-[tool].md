---
description: Perform a comprehensive, evidence-based code audit against the interface-map.md SOT
---

# Interface Map Audit Workflow

> **Purpose**: The `docs/cerb/interface-map.md` is the architectural source of truth for the Cerb OS model. Code naturally drifts ahead of static documentation. This workflow programmatically verifies that the reality of the `app-core` codebase matches the claims in the interface map, detecting false positives, legacy violations, and interface signature drift.

## Phase 1: Preparation & Clean Environment Check
1. Read the `docs/cerb/interface-map.md` completely to understand the current claimed architecture, layers, and module statuses.
2. Initialize an `audit-report.md` artifact to collect findings.
3. Check for obvious architectural violations using exact `grep_search`:
   - Search for `android.*` imports in the `domain` layers (violation of clean architecture).
   - Search for `feature.*` or `legacy.*` imports in the `domain` layers.
   - Record any violations in the audit report.

## Phase 2: False Positive Detection (The "Fake" Masquerade)
1. Read `app-core/src/main/java/com/smartsales/prism/di/PrismModule.kt` (and other related core DI modules).
2. Identify all domain interfaces that are actively bound to a `Fake*` implementation.
3. Cross-reference these fakes with the statuses in `interface-map.md`. 
4. If a module is marked `✅` (Shipped) but is actually bound to a Fake in DI, flag it as a **False Positive (Masquerade)** in the audit report. It must be downgraded.

## Phase 3: Layer-by-Layer Signature Parity Audit
For every layer and module defined in the `interface-map.md` (Infrastructure, Data Services, Core Pipeline, Features, Intelligence):
1. Use `find_by_name` to locate the exact Kotlin `interface` file for the module.
2. Use `view_file` to read the entire Kotlin interface definition.
3. Compare the exact function signatures (method names, arguments, and return types, including `Flow` vs `suspend` usage) with the `Key Interface` column in the map.
4. If the code and the map diverge, record the exact actual Kotlin signature and flag it as **Interface Drift**. 
   - *Note: Code is the ultimate source of truth here. The map must be updated to reflect the code, not the other way around.*

## Phase 4: Reporting & Remediation
1. Compile all findings into the `audit-report.md` artifact. The report must be brutally evidence-based. No assumptions.
2. Use `multi_replace_file_content` to surgically apply the corrections directly to `docs/cerb/interface-map.md`:
   - Downgrade any False Positives from `✅` (Shipped) to `📐` (Interface Only) or `🚧` (Active).
   - Update the `Key Interface` column to exactly match the current Kotlin reality (e.g., from `-> Flow<T>` to `-> T`).
   - Add/remove any modules that were created/deleted but not recorded.
3. Update `docs/plans/tracker.md`:
   - Add a changelog entry in the "Changelog" section detailing the audit execution and high-level findings.
   - Append any identified architectural violations (e.g., Domain layer Android imports) to the **Tech Debt** section.
4. Notify the user with a summary of the drift fixed and provide the paths to the modified documents.
