# Wave 20 T5: Plugin Gateway / Capability SDK Audit

Date: 2026-03-17
Wave: 20
Task: T5
Constitution: [`docs/specs/Architecture.md`](../specs/Architecture.md)
Flow Target: [`docs/core-flow/system-plugin-gateway-flow.md`](../core-flow/system-plugin-gateway-flow.md)

## Scope

Audit the delivered plugin lane against the architecture constitution and the system plugin core flow:

- dispatch and registry behavior
- plugin boundary model
- capability SDK reality
- permission model and gateway usage
- plugin-caused write handoff
- plugin telemetry / GPS coverage

This is an evidence-first audit. It records current code reality and drift. It does not treat the new Plugin SDK model as already delivered just because the docs now define it.

## Files Inspected

- [`docs/core-flow/system-plugin-gateway-flow.md`](../core-flow/system-plugin-gateway-flow.md)
- [`docs/specs/Architecture.md`](../specs/Architecture.md)
- [`docs/cerb/plugin-registry/spec.md`](../cerb/plugin-registry/spec.md)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt)
- [`core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt)
- [`app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt)
- [`app-core/src/main/java/com/smartsales/prism/data/real/plugins/TestingPdfPlugin.kt`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/TestingPdfPlugin.kt)
- [`app-core/src/main/java/com/smartsales/prism/data/real/plugins/TestingEmailPlugin.kt`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/TestingEmailPlugin.kt)
- [`app-core/src/main/java/com/smartsales/prism/data/real/plugins/ExportCsvPlugin.kt`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/ExportCsvPlugin.kt)
- [`core/pipeline/src/test/java/com/smartsales/core/pipeline/EchoPluginTest.kt`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/EchoPluginTest.kt)
- [`core/test-fakes-platform/src/main/java/com/smartsales/core/test/fakes/FakeToolRegistry.kt`](../../core/test-fakes-platform/src/main/java/com/smartsales/core/test/fakes/FakeToolRegistry.kt)

## Constitution Clauses Reused

- System III plugins and capability SDK role: [`Architecture.md:83`](../specs/Architecture.md#L83)
- Plugin SDK / Capability Gateway section: [`Architecture.md:94`](../specs/Architecture.md#L94) through [`Architecture.md:116`](../specs/Architecture.md#L116)
- bounded async plugin workflows: [`Architecture.md:292`](../specs/Architecture.md#L292)
- plugin dispatch / routing valves: [`Architecture.md:324`](../specs/Architecture.md#L324) through [`Architecture.md:325`](../specs/Architecture.md#L325)

## Findings

### 1. The delivered plugin lane is a dynamic registry and async UI-yield path, not yet a real capability SDK

The architecture now says plugins should be built against approved capability APIs rather than direct cross-module wiring. The current code is not there yet.

What is delivered:

- a dynamic plugin registry via Hilt-injected `Set<PrismPlugin>` in [`RealToolRegistry.kt:10`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt#L10) through [`RealToolRegistry.kt:12`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt#L12)
- async execution returning `Flow<UiState>` via [`ToolRegistry.kt:38`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L38) through [`ToolRegistry.kt:45`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L45)
- plugin metadata and dispatch contracts via [`ToolRegistry.kt:70`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L70) through [`ToolRegistry.kt:75`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L75)

What is not delivered:

- no actual capability SDK layer
- no narrow approved APIs such as the new constitutional examples
- no code surface that groups approved plugin capabilities behind a gateway service

Current plugins are thin self-contained flows that directly emit UI states, not consumers of a stable SDK.

Assessment:

- constitution alignment: medium
- delivered behavior: dynamic registry yes, capability SDK no

### 2. The runtime plugin gateway is mostly a stub, not a meaningful bounded capability surface

`PluginGateway` only exposes:

- `getSessionHistory(turns)`
- `appendToHistory(message)`
- `emitProgress(message)`

as defined in [`ToolRegistry.kt:64`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L64) through [`ToolRegistry.kt:68`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L68).

But live callers do not provide a real implementation. In the actual runtime:

- `IntentOrchestrator` creates a `silentGateway` whose methods return empty strings or no-op in [`IntentOrchestrator.kt:263`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L263) through [`IntentOrchestrator.kt:267`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/IntentOrchestrator.kt#L267)
- `AgentViewModel` does the same in [`AgentViewModel.kt:266`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L266) through [`AgentViewModel.kt:269`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L269) and [`AgentViewModel.kt:309`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L309) through [`AgentViewModel.kt:312`](../../app-core/src/main/java/com/smartsales/prism/ui/AgentViewModel.kt#L312)

So even the small gateway surface that exists is not actually connected to session memory, history writeback, or progress infrastructure in production code.

Assessment:

- constitution alignment: low
- delivered behavior: structural stub
- required follow-up: implement a real runtime gateway before expanding plugin responsibilities

### 3. The permission model exists on paper and in tests, but is not enforced by the real runtime

The permission system is declared in [`ToolRegistry.kt:48`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L48) through [`ToolRegistry.kt:58`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L58), and `PrismPlugin` declares `requiredPermissions` in [`ToolRegistry.kt:70`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L70) through [`ToolRegistry.kt:73`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L73).

There is a real permission-enforced gateway, but only in tests. `EchoPluginTest` uses `FakePluginGateway` to throw `SecurityException` if permissions are missing in [`EchoPluginTest.kt:15`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/EchoPluginTest.kt#L15) through [`EchoPluginTest.kt:39`](../../core/pipeline/src/test/java/com/smartsales/core/pipeline/EchoPluginTest.kt#L39).

In real code:

- `RealToolRegistry` does not enforce permissions
- runtime gateways do not inspect `requiredPermissions`
- current real plugins all declare `emptySet()` permissions

So the permission model is currently more of a design affordance than an enforced boundary.

Assessment:

- constitution alignment: low-to-medium
- delivered behavior: test-only

### 4. Plugin outputs currently yield UI states directly, not typed or bounded domain result contracts

The current plugin contract returns `Flow<UiState>` directly from `PrismPlugin.execute(...)` in [`ToolRegistry.kt:74`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L74) through [`ToolRegistry.kt:75`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/ToolRegistry.kt#L75). That means plugins speak in presentation-layer terms.

This is visible in the real plugins:

- `TestingPdfPlugin` emits `UiState.ExecutingTool` and `UiState.Response` in [`TestingPdfPlugin.kt:32`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/TestingPdfPlugin.kt#L32) through [`TestingPdfPlugin.kt:39`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/TestingPdfPlugin.kt#L39)
- `TestingEmailPlugin` does the same in [`TestingEmailPlugin.kt:32`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/TestingEmailPlugin.kt#L32) through [`TestingEmailPlugin.kt:38`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/TestingEmailPlugin.kt#L38)
- `ExportCsvPlugin` also directly emits `UiState` in [`ExportCsvPlugin.kt:24`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/ExportCsvPlugin.kt#L24) through [`ExportCsvPlugin.kt:35`](../../app-core/src/main/java/com/smartsales/prism/data/real/plugins/ExportCsvPlugin.kt#L35)

This was acceptable for the older plugin-registry spec, which explicitly targeted async UI states in [`plugin-registry/spec.md:17`](../cerb/plugin-registry/spec.md#L17), but it does not yet match the newer architecture rule that plugins should re-enter the OS through approved result contracts and not own UI state directly.

Assessment:

- constitution alignment: medium-low
- delivered behavior: legacy async UI contract
- required follow-up: decide whether to keep `Flow<UiState>` as a transitional shell or introduce typed plugin result contracts

### 5. Plugin-caused write handoff into the typed mutation lane is not delivered

The new constitution says plugin-caused SSD mutations must re-enter through the typed mutation / central writer path. I found no real implementation of that handoff.

What exists today:

- `RealUnifiedPipeline` emits generic `ToolDispatch` from task mutations in [`RealUnifiedPipeline.kt:267`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L267) through [`RealUnifiedPipeline.kt:293`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealUnifiedPipeline.kt#L293)
- `IntentOrchestrator` or `AgentViewModel` then runs the plugin directly via `toolRegistry.executeTool(...)`

What does not exist:

- no plugin result that hands back a typed mutation command
- no plugin pathway that routes into `RealUnifiedPipeline` or `EntityWriter` as a typed mutation handoff
- no evidence of plugin-caused writes using the central writer rule

So this part of the plugin core flow is still wholly aspirational.

Assessment:

- constitution alignment: low
- delivered behavior: absent

### 6. Telemetry is only partially delivered for the plugin lane

What exists:

- dispatch received in `RealToolRegistry` via `PLUGIN_DISPATCH_RECEIVED` in [`RealToolRegistry.kt:20`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt#L20) through [`RealToolRegistry.kt:25`](../../core/pipeline/src/main/java/com/smartsales/core/pipeline/RealToolRegistry.kt#L25)
- internal routing and yielded-to-OS in `TestingPdfPlugin` and `TestingEmailPlugin`

What does not exist:

- no `PLUGIN_CAPABILITY_CALL` usage anywhere in code
- no `PLUGIN_EXTERNAL_CALL` usage anywhere in code
- `ExportCsvPlugin` emits no valve checkpoints at all
- no telemetry layer proving capability SDK calls, because no capability SDK exists yet

Assessment:

- constitution alignment: low-to-medium
- delivered behavior: partial and inconsistent

### 7. The live lane is better described as “thin plugin registry with async UI tools” than “SDK-backed bounded plugin OS”

This is the simplest accurate summary.

The old plugin-registry spec itself still says the system is on the path to a real SDK in [`plugin-registry/spec.md:12`](../cerb/plugin-registry/spec.md#L12) through [`plugin-registry/spec.md:18`](../cerb/plugin-registry/spec.md#L18). The delivered code reached:

- dynamic registry
- async execution
- plugin metadata

But it did not yet reach:

- real capability SDK
- real runtime permission enforcement
- real gateway-backed history/progress integration
- typed mutation re-entry for plugin writes

## Positive Alignments

1. The plugin lane is real as a separate registry-based execution path.
2. Dispatch is explicit through `ToolRegistry`.
3. The registry is dynamically composed rather than hardcoded.
4. Async execution exists and can yield progress/result states back to the OS/UI.

## Evaluation

- registry / dispatch fidelity: high
- bounded workflow fidelity: medium
- capability SDK fidelity: low
- permission / boundary enforcement fidelity: low
- plugin-caused write handoff fidelity: low
- telemetry fidelity: low-to-medium
- overall constitutional fidelity: medium-low

Overall evaluation:

The plugin lane exists, but it is still in an earlier architectural generation than the new constitution. It behaves like:

- a dynamic tool registry
- async plugin UI execution
- partial telemetry

not yet like:

- a capability-SDK-backed bounded plugin OS
- with enforced permissions
- with real gateway services
- and with typed mutation re-entry for writes

## Derived Fix Tasks

1. Create a real runtime `PluginGateway` implementation backed by actual session-memory/history/progress services instead of empty lambdas.
2. Introduce the first narrow capability APIs from real use cases and make plugins call those instead of ad-hoc inline logic.
3. Decide the transition plan from `Flow<UiState>` plugins to approved typed or bounded plugin result contracts.
4. Implement runtime permission enforcement based on `requiredPermissions`.
5. Define and implement the plugin-caused write handoff into the typed mutation / central writer lane.
6. Add missing plugin telemetry:
   - `PLUGIN_CAPABILITY_CALL`
   - `PLUGIN_EXTERNAL_CALL`
   - consistent `PLUGIN_INTERNAL_ROUTING` / `PLUGIN_YIELDED_TO_OS`
7. Add lane-level tests for:
   - permission enforcement in production-style registry path
   - gateway-backed history/progress access
   - typed mutation re-entry from plugin results

## Conclusion

The plugin lane is delivered as a real registry, but not yet as the new Plugin SDK / Capability Gateway architecture.

So the main T5 conclusion is:

- **registry path**: real
- **SDK/gateway path**: mostly aspirational
- **write handoff and permission enforcement**: not delivered yet
