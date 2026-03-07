# Echo Test Plugin (Gateway Proof of Concept)

> **OS Layer**: App Infrastructure / Plugin Ecosystem
> **State**: SHIPPED

---

## Overview

The `echo-test` plugin is a pure architectural proving ground. It exists solely to validate the `PluginGateway` and `CoreModulePermission` mental model in the `app-prism` codebase before we build real, heavy plugins like PDF Export or Negotiation Simulation. 

**Goal**: Prove that a plugin can be completely decoupled from DAOs, execute asynchronously, and gracefully fail if it lacks permissions, all while being routed by the System II Brain.

---

## The Contract

### 1. Schema (What the LLM sees)
```json
{
  "toolId": "ECHO_TEST",
  "name": "Echo Test Tool",
  "description": "A diagnostic tool to test the Gateway socket. Echos the input back after reading session history.",
  "parameters": {
    "message": "The string to echo back"
  }
}
```

### 2. Required Permissions
- `READ_SESSION_HISTORY`: To prove the plugin can read data via the Gateway.

---

## Execution Constraints (The Test Rules)

1. **Isolation**: The `EchoPlugin` class must NOT have any dependencies in its constructor other than standard Kotlin util injects. It cannot inject `SessionHistoryRepository` or `MemoryRepository`.
2. **Gateway Usage**: It must receive a `PluginGateway` via its `execute()` function.
3. **Async Emission**: It must emit a `UiState` indicating it has started, followed by a simulated network delay (`delay(500)`), followed by the final `UiState` containing the echoed message + the last line of session history.
4. **Security Check**: If the plugin does not declare `READ_SESSION_HISTORY` in its manifest, calling `gateway.getSessionHistory()` must throw a `SecurityException` or return an error state.

---

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | **Gateway Core SDK** | ✅ | Define `PluginGateway` and `CoreModulePermission` in `:core:pipeline`. Update `PrismPlugin`. |
| **2** | **Test Implementation** | ✅ | Create `EchoPlugin`, `FakePluginGateway`, and `EchoPluginTest` in the test sources to prove the architecture. |
