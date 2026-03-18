> **OS Layer**: JVM / Android Test

# Test Infrastructure Spec

## 1. Description
The active shared test infrastructure is split across three modules:

- `:core:test` for lightweight shared test helpers such as `FakeDispatcherProvider`
- `:core:test-fakes-domain` for pure JVM, state-backed domain fakes
- `:core:test-fakes-platform` for Android/platform-facing fakes layered on top of the domain fakes

Together these modules are the current source of truth for reusable test doubles and helpers. The repo does not currently ship a `PrismTestRig`; tests wire the specific fakes they need directly.

Testing infrastructure is treated as a first-class Cerb feature to enforce strict Anti-Drift and Anti-Illusion protocols.

## 2. Core Components
- **`:core:test`**: Shared helpers that do not belong to a fake data owner module.
- **`:core:test-fakes-domain`**: Concrete, state-backed domain fakes (`FakeHistoryRepository`, `FakeEntityRepository`, `FakeMemoryRepository`, `FakeEntityWriter`, etc.).
- **`:core:test-fakes-platform`**: Platform-aware fakes for pipeline and Android-facing seams (`FakeExecutor`, `FakeContextBuilder`, `FakeToolRegistry`, etc.).
- **WorldStateSeeder**: The centralized, deterministic factory for generating B2B "chaos data" (aliases, overlapping context, homophones).
- **`scripts/run-tests.sh`**: Canonical repo entrypoint for the current automated Gradle test targets.
  First-class slices: `all` (curated repo-default), `infra`, `pipeline`, `scheduler`, `l2`.
  Convenience alias: `app`.

## 3. Current Status

| Area | Status | Notes |
|------|--------|-------|
| Shared helper module | ✅ SHIPPED | `:core:test` exists as a lightweight helper module and currently exposes `FakeDispatcherProvider`. |
| State-backed fake coverage | ✅ SHIPPED | Shared domain and platform fakes exist across the split fake modules. |
| Domain/platform isolation | ✅ SHIPPED | The fake stack is split into `:core:test-fakes-domain` and `:core:test-fakes-platform`. |
| Canonical runner | ✅ SHIPPED | `scripts/run-tests.sh` is the current repo-level wrapper for automated Gradle test targets. |
| Runner slice contract | ✅ SHIPPED | `all` is explicitly a curated repo-default slice, while `infra`, `pipeline`, `scheduler`, and `l2` are first-class named verification slices. |
| Infra assertion reality | ✅ SHIPPED | The infra runner now executes real assertions for dispatcher control, state-backed fake read-through, and platform fake collaboration behavior. |
| `PrismTestRig` | ⏸️ DEFERRED | Historical proposal only. It is not part of the current delivered contract and should not be treated as live infrastructure. |
