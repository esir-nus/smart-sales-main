# Wave 7 Final Audit Boundaries

> **Target**: Physical Device (L3) & Simulated Pipeline Engine (L2)
> **Level**: L2 Simulated & L3 Real Device

## Dependency Management (Anti-Illusion)
- **Allowed Reals**: 
  - `RealUnifiedPipeline` (The core async/sync engine)
  - `RealLightningRouter` (Gatekeeper)
  - `RealContextBuilder` (OS RAM Assembly)
  - `RealIntentOrchestrator` (The conductor)
  - Room Daos (`MemoryDao`, `UserHabitDao`, `EntityDao`)
  - DashScope AI clients (For L3 Testing)
- **Required Fakes**: 
  - `FakeAudioRepository` (For text-injected flows)
  - `FakeTimeProvider` (For deterministic scheduling tests)
  - `L2WorldStateSeeder` (To inject `dataset.md` chaos context)
- **Forbidden Mocks**: 
  - 🛑 **ABSOLUTELY NO `mockito`**. 
  - Do not mock the database.
  - Do not mock the LLM pipeline response outputs.
  - Do not mock the IntentOrchestrator logic.
