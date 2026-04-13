# Strict Interface Integrity Boundaries

> **Target**: `L2StrictInterfaceIntegrityTest.kt`
> **Level**: L2 Simulated Pipeline

## Dependency Management (Anti-Illusion)
- **Allowed Reals**: `RealUnifiedPipeline`, `RealContextBuilder`, `EntityWriter`
- **Required Fakes**: `FakeInputParserService`, `FakeEntityDisambiguationService`, `FakeExecutor`, `FakeEntityRepository`
- **Forbidden Mocks**: Do NOT use `mockito` for verifying flow emissions. We must natively execute `pipeline.processInput().firstOrNull()` and assert against the exact `PipelineResult` payload type.
