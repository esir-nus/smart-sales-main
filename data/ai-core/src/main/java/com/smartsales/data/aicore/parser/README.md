# LLM Parser Module

**Spec Reference:** Orchestrator-V1.md §3.1.3

## Purpose

Parses `MachineArtifact` from AI Chatter output and writes structured updates to Metadata Hub.

## Key Components

- `LlmParser.kt` - Main parser logic

## Constraints

- Must NOT modify transcript truth (§2.2)
- Can only update M2/M2B/M3 layers
- Must emit Trace events on failure
