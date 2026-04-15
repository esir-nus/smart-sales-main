# Lightning Fast-Track E2E Boundaries

> **Target**: `RealIntentOrchestratorTest.kt` (To Be Created)
> **Level**: L2 Simulated (Orchestrator Level)

## Dependency Management (Anti-Illusion)
- **Allowed Reals**: `IntentOrchestrator`, `RealLightningRouter` (with simulated LLM responses or injected L2 DashScope), `RealContextBuilder`.
- **Required Fakes**: `FakeMascotService`, `FakeUnifiedPipeline`.
- **Forbidden Mocks**: Do not use `mockito` for evaluating `LightningRouter` conditions or orchestrating the flow.

## Note on Spec Drift
The current `IntentOrchestrator` fails the Literal Spec Alignment for the `SIMPLE_QA` Fast-Track flow. Fixing this is a prerequisite for a complete E2E test suite.
