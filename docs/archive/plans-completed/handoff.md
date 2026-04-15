# Objective: Layer 3 Core Pipeline Architecture Assembly

Hello! You are continuing the "Great Assembly" architectural refactoring for the Smart Sales Prism project. 

## Current State
The previous agent successfully completed **Layer 1 Infrastructure Extraction** and **Layer 2 Data Services Modularization (Stages 1, 2, and 2.5)**. 
- All Room DAOs and entities are physically isolated in `:core:database`.
- All Domain Contracts for LTM, STM, and RL are isolated in `:domain:*` modules.
- All Data Repositories for Session, Habit, Memory, and CRM are isolated in `:data:*` modules.
- The `app-core` module has been thoroughly decoupled from pure hardware and persistence concerns. 

## Next Goal
We are now moving on to **Layer 3: Core Pipeline (The Roads & Intersections)**. 
This layer orchestrates the LLM processing and routes intents. It includes:
- `ContextBuilder`
- `InputParser`
- `EntityDisambiguator`
- `LightningRouter`
- `ModelRegistry` & `Executor`
- `PluginRegistry`
- `UnifiedPipeline`

Your goal is to extract these components out of `app-core` and assemble them into cohesive, isolated Gradle modules (e.g., `:core:pipeline`, `:core:llm`, etc.) according to the `interface-map.md` topology.

## Getting Started
1. Start by running the `@[/feature-dev-planner-[tool]]` to structure your approach.
2. Read `docs/plans/tracker.md` (specifically the "The Great Assembly" section at the bottom) to orient yourself.
3. Read `docs/cerb/interface-map.md` (Layer 3 section) to understand the data flow and ownership rules. 
4. Convene an `@[/00-review-conference-[tool]]` with `@[/01-senior-reviewr-[persona]]` and `@[/17-lattice-review]` before writing any code to debate the exact module boundaries for Layer 3, as it is the most tightly coupled layer. Do a thorough architectural dependency audit.
