## 📋 Pipeline Valve Audit Report

### 1. Code vs Tracker Sync
- [x] List of valves found in code but missing from tracker (SYNCED): 
  - None. Code exactly matches the `pipeline-valves.md` tracker (I just patched a missing `[ALIAS_RESOLUTION]` implementation during the scan).
- [x] List of valves in tracker/docs but missing from code (RED FLAG): 
  - 🔴 `[SSD_GRAPH_FETCHED]` is defined in the architectural Master Guide, but is currently **missing from both the codebase and the `pipeline-valves.md` tracker!**

### 2. Effectiveness Evaluation
**🔴 Hard No (Useless Valves)**:
- `[ROUTER_DECISION]` in `IntentOrchestrator`: The `payloadSize` is hardcoded to `0` or `1`. This is lazy. It should ideally track the node count of the minimal context, or the confidence score of the routing decision, not just an arbitrary boolean integer.

**🟢 Good Calls (Strong Telemetry)**:
- `[LIVING_RAM_ASSEMBLED]`: Excellent. Payload size accurately groups `sessionHistory.size + entityContext.size`, and the dump contains Mode, Entities, and Turns. Proper GPS checkpoint.
- `[LINTER_DECODED]`: Excellent. Evaluates the actual parsed sizes of `profileMutations` and `tasks` from the Kotlin `data class`, enforcing the "One Currency" concept mathematically.
- `[ALIAS_RESOLUTION]`: Good. Tracks `resolvedEntities.size` proving we secured the IDs.

### 3. Missing Junctions (The Blind Spots)
Based on the architecture map and current implementation, we urgently need valves at:
1. `[SSD_GRAPH_FETCHED]` in `ContextBuilder.kt`: The master guide mandates tracking when SSD Data is retrieved, but we missed implementing it during Context Assembly. This hides database fetch latency and payload drops.
2. `[PLUGIN_DISPATCH_RECEIVED]` in the new `SchedulerToolPlugin` (Wave 16): As we decouple Scheduler, the generic plugin must acknowledge **exactly what it received** from the UnifiedPipeline to prove the contract holds across modules.
3. `[DB_WRITE_EXECUTED]` in `EntityWriter.kt`: We need an anchor right before the `Room` transaction to prove the mutation actually hit the physical disk.

### 4. Next Actions
Run `/feature-dev-planner` for **Wave 16 T1 (Plugin Extraction)** to build the new Scheduler Plugin and embed the missing `[PLUGIN_DISPATCH_RECEIVED]` valve during construction, while also patching `[SSD_GRAPH_FETCHED]`.
