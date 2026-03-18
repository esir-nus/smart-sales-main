# Test Infrastructure Interface

> **Owners**: `:core:test`, `:core:test-fakes-domain`, `:core:test-fakes-platform`
> **Consumers**: All `src/test/` source sets across `:core`, `:data`, `:domain`, and `app-core`.

## Public Interface

```kotlin
testImplementation(project(":core:test"))
testImplementation(project(":core:test-fakes-domain"))
testImplementation(project(":core:test-fakes-platform"))
```

Representative shared seams:

```kotlin
// :core:test
FakeDispatcherProvider

// :core:test-fakes-domain
FakeEntityRepository
FakeEntityWriter
FakeHistoryRepository
FakeMemoryRepository
FakeUserHabitRepository
WorldStateSeeder

// :core:test-fakes-platform
FakeContextBuilder
FakeExecutor
FakeInputParserService
FakeLightningRouter
FakeToolRegistry
FakeUnifiedPipeline
```

## Architectural Contract

1. **No Production Leaks**: These modules are strictly deployed via `testImplementation`. Production code (`src/main/`) must NEVER import `:core:test`, `:core:test-fakes-domain`, or `:core:test-fakes-platform`.
2. **Prefer Shared Fakes for Owned Collaborators**: L2-style tests should prefer the shared, state-backed fakes for repo-owned collaborators to avoid "Testing Illusions". Narrow mocks are acceptable only for leaf dependencies where the repo does not yet provide a reusable fake, and they must not replace the core behavior under test.
3. **Canonical Automated Entry**: Use `scripts/run-tests.sh` as the repo-level entrypoint for automated test runs. First-class modes are `all`, `infra`, `pipeline`, `scheduler`, and `l2`. `all` is the curated repo-default slice, not an exhaustive aggregate. L3 device validation remains manual per `docs/cerb-e2e-test/testing-protocol.md`.
