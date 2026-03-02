# Model Routing Interface

> **Owner**: Core/Prism (Config)
> **Consumers**: Analyst, Scheduler, Pipeline Services, Tool Executors

### ModelRegistry

Object defining available pre-configured models:
- **EXTRACTOR**: `qwen-turbo` (Temp 0.0f) — For deterministic parsing tasks.
- **PLANNER**: `qwen-plus` (Temp 0.5f) — For reading large contexts and strategy generation.
- **EXECUTOR**: `qwen3-max-2026-01-23` (Temp 0.0f) — For strict JSON tool-calling and execution.

```kotlin
object ModelRegistry {
    val EXTRACTOR: LlmProfile
    val PLANNER: LlmProfile
    val EXECUTOR: LlmProfile
}
```

## Data Models

```kotlin
/**
 * Defines the configuration for a specific LLM execution.
 */
data class LlmProfile(
    val modelId: String,
    val temperature: Float = 0.5f,
    val skillTags: Set<String> = emptySet()
)
```

## You Should NOT

- **You should NOT** attempt to dynamically "route" models based on context or build smart routers. Model selection should be explicitly manual based on the module's known responsibility.
- **You should NOT** hardcode model strings (like `"qwen-turbo"`) anywhere in service logic.
- **You should NOT** hardcode temperature values in service logic.
- **You should NOT** use `EXECUTOR` for generic chatting (it forces `temperature = 0.0f` and strict schemas).
- **You should NOT** use `EXTRACTOR` for handling `EnhancedContext` (context window is too small).
