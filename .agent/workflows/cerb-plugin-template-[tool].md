---
description: Create a standardized Cerb specification for Plugins and Tools (Independent Workflows)
---

# Cerb Plugin & Tool Scaffolder

> **Purpose**: Create a standardized specification for independent Plugins, Tools, or Workflows ("The Hands").
> **Output**: `interface.md` + `spec.md` with Invocation Flow and OS Layer capabilities.
> **Note**: Plugins are independent from the dual-engine core. They can be triggered by the Analyst Pipeline (Taskboard) or via direct Tool Course (Lightning Router).

---

## Step 1: Define the Plugin Contract (Interface)

Create `docs/cerb/plugins/[plugin-name]/interface.md`.

**Rule**: Plugins must have a clean, isolated boundary. They do not depend on the core LLM pipelines. They operate on strongly-typed requests and return strongly-typed results. No `android.*` imports in the domain!

```markdown
# [Plugin Name] Interface

> **Owner**: Plugin Registry / [Plugin Name]
> **Invoked By**: Analyst Pipeline OR Direct Tool Course (Lightning Router)

## Public Interface

```kotlin
interface [Plugin]Service {
    val toolId: String // e.g., "GENERATE_PDF", MUST match registry
    val label: String
    val icon: String
    val description: String
    
    /**
     * Executes the plugin workflow.
     * @param request Strongly-typed parameters parsed by LLM/Router
     */
    suspend fun execute(request: [Plugin]Request): [Plugin]Result
}
```

## Data Models

```kotlin
// The exact parameters this tool needs to execute
data class [Plugin]Request(
    val param1: String,
    val param2: Int
)

// The specific domain outcomes of this tool
sealed class [Plugin]Result {
    data class Success(val outputData: String) : [Plugin]Result()
    data class RequiresPlanner(val recommendedPlan: PlanModel) : [Plugin]Result()
    data class RequiresClarification(val question: String) : [Plugin]Result() // Interactive flow
    data class Error(val reason: String, val retryable: Boolean) : [Plugin]Result()
}
```
```

---

## Step 2: Define the Implementation Details (Spec)

Create `docs/cerb/plugins/[plugin-name]/spec.md`.

### 2.1 Capability & Invocation Declaration (Mandatory)

```markdown
> **OS Layer**: [RAM Application | SSD Storage | External API/File Explorer]
> **Invocation Type**: [Direct Execution | Requires Planner Phase | Interactive Flow]
```

### 2.2 Execution Flow

Define how this plugin operates from trigger to result.

```markdown
## Execution Flow

1. **Trigger**: How is this plugin called? What parses the `[Plugin]Request`?
2. **Planner / Interactive Phase (If Applicable)**: Does this plugin need a plan or clarification? If so, what parameters or intermediate UI slots does the user see?
3. **Execution**: What data/interfaces does it mutate or read? (e.g., Reads MemoryCenter, calls external network)
4. **Result Delivery**: How is the `[Plugin]Result` mapped to `UiState`?
```

### 2.3 Wave Plan

```markdown
## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **1** | Core Execution Logic | 🔲 PLANNED | Interface + FakeImpl + RealImpl |
| **2** | Plugin Registry Wiring | 🔲 PLANNED | Register in ToolRegistry |
```

---

## Step 3: Register in Tracker and Interface Map

Update `docs/plans/tracker.md`:
1. Check if a `## Plugins & Tools` section exists. If not, create it.
2. Add new row to tracking table.
3. Set State to `SPEC_ONLY`.

Update `docs/cerb/interface-map.md`:
1. Find the **Delivery Workflow Registry (The TaskBoard Vault)** section.
2. Add the `toolId` to the list of recognized Vault IDs (e.g. `- [PLUGIN_ID] ([Brief Description])`).

---

## Checklist

- [ ] `interface.md` created (Strongly-typed data contract)
- [ ] `spec.md` created (Execution Flow + Invocation Type)
- [ ] Registered in `tracker.md` as `SPEC_ONLY`
- [ ] Registered `toolId` in `interface-map.md` (Delivery Workflow Registry)

**Done. Now verify with `/cerb-check` and then implement the plugin locally.**
