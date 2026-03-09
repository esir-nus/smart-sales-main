# Anti-Illusion Testing Protocol (防幻觉测试铁律)

> **Root Cause**: AI agents write "hallucinated" tests that optimize for green checkmarks by mocking the happy path, bypassing the actual information gates and physical complexities of the pipeline.

## The Core Principle: Respect the Pipeline Intricacies
You cannot test a pipeline by forcing the output. You must test a pipeline by setting up the specific **Information State** it requires. A "Full Pipeline" test is INVALID if it magically mocks a success result without providing the requisite context (Person, Account, Intent).

## Overhaul Criteria (The "Two Antis")

### 1. Anti-Hallucination (防幻觉)
*Definition*: Tests must not invent successful states out of thin air to bypass complex business logic.
* **Criterion**: Mocks (`thenReturn`) cannot be used to bypass Information Gates. If a pipeline requires a `PersonId` to proceed, the test setup MUST construct a fake context containing a valid `PersonId`. 
* **Criterion**: No assertions of "Success" unless the prerequisite state is explicitly defined in the `Arrange` block.

### 2. Anti-False Positive (防假阳性)
*Definition*: A test must prove that it *will fail* if the underlying logic is removed or bypassed.
* **Criterion**: The RED-First Rule. A test is only valid if it fails when the SUT (System Under Test) skips its core operation.
* **Criterion**: Payload Verification. Tests must assert *what* was passed to downstream services using `verify(...).method(argThat { ... })`. Returning a mocked "Success" from a downstream dependency without verifying the payload it received is a False Positive risk and is strictly banned.

---

## 1. Context-Driven Execution (语境驱动执行法则)

Every pipeline test must explicitly align with one of the three Information States:

| State | Definition | Expected Pipeline Behavior | Test Requirement |
|-------|------------|----------------------------|------------------|
| **No Context** | Chatter, noise, simple greetings. | Handled by **Mascot (System I)**. | Assert `PipelineResult.ConversationalReply` and verify Mascot/EventBus invocation. |
| **Partial Context** | Intent detected, but missing Who/What/Where/When. | Trapped in **Clarification Loop**. | Assert `PipelineResult.ClarificationNeeded` or `DisambiguationIntercepted`. Do NOT mock a success. |
| **Sufficient Context** | All required slot filling (entities/intent) is present. | **Full Pipeline Execution**. | Must provide actual mocked context (Person ID, Account info). Test the final state shift. |

---

## 2. The "Verify or Fail" Rule for Mocks (验证或失败原则)

If you use `mockito.mock()` for a downstream dependency (e.g., `SchedulerLinter`), you are **FORBIDDEN** from simply asserting the final UI state.

**You MUST:**
Use `verify(mock).method(argThat { ... })` to prove the System Under Test fed the correct, expected data structure (e.g., valid JSON, correct object type) to the downstream component.

## 3. Interface Contract Enforcement (契约强制执行)

If a component routes based on an Enum or Sealed Class (e.g., `PipelineInput.intent` using `QueryQuality`), the test file **MUST** contain at least one `@Test` block for **EVERY** branch of that enum. No more testing only the `ANALYST` happy path and ignoring the rest.

## 4. Fakes > Mocks for Real State (Fake 优于 Mock)

For Layer 2/3 state layers (Memory, Habit, Scheduler), dynamic `Mockito.mock()` stubs are banned for CRUD operations.
You **MUST** inject the actual `Fake*Repository` (e.g., `FakeMemoryCenter`, `FakeToolRegistry`). A fake maintains state logic and will crash if you misuse it; a mock will happily lie to you.

---

## Enforcement Checklist for Agents
Before concluding any feature test writing task, an agent must verify:
- [ ] No `thenReturn` mocks are used to bypass a necessary state clarification loop.
- [ ] A negative/partial context test exists for the pipeline.
- [ ] Downstream invocations are strictly `verify()`'d for structural accuracy.
