# Scheduler Linter Spec

> **OS Layer**: RAM Application
> **State**: SPEC_ONLY

## 1. Responsibility
The FastTrack Parser (SchedulerLinter) is responsible for taking raw text intents and synchronously extracting precise semantic DTOs (`CreateTasksParams`, `RescheduleTaskParams`, `CreateInspirationParams`) for sub-second optimistic Path A execution.

## 2. Core Behaviors
- **One Currency Output**: Always emits strongly-typed JSON mappable to the Kotlin DTOs.
- **Fail Fast**: If the user's intent is vague or requires multi-turn dialogue, it may emit a `NoMatch` or leave `startTimeIso` empty (which the Mutation Module will handle by placing it in Purgatory).
- **No Database Reads**: It does not read `ScheduleBoard`. It purely parses the semantic sentence structurally. All conflict evaluation happens downstream in the Mutation Module.

## 3. Intricacies & Organic UX Constraints
- **Organic UX**: Users will issue timeless intent ("I want to learn guitar"). The parser must seamlessly route this to `CreateInspirationParams` rather than trying to force an ISO time constraint.
- **Data Reality**: NLP models hallucinate timestamps. The DTOs must strictly enforce ISO-8601 formatting to prevent parsing crashes downstream.

## Wave Plan

| Wave | Focus | Status | Deliverables |
|------|-------|--------|--------------|
| **17** | Path A Data Contracts | ✅ SHIPPED | Interface + RealImpl mapping to exact PRD DTOs |
