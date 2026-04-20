# L3 Scheduler Extractor Lane On-Device Validation

Date: 2026-04-17
Branch: `develop`
Device: adb-connected Android device
Evidence class: on-device `adb logcat`

## Scope

This L3 rerun targeted the scheduler parsing lane after the scheduler-only extractor split to `ModelRegistry.SCHEDULER_EXTRACTOR = qwen-plus`.

Checked behaviors:

- exact create with qualified next-week weekday + explicit clock
- supported explicit-time targeted reschedule against an existing visible task
- unsupported delta-only targeted reschedule against the same task

## Sources Used

- `docs/plans/tracker.md`
- `docs/core-flow/scheduler-fast-track-flow.md`
- `docs/core-flow/sim-scheduler-path-a-flow.md`
- `docs/cerb/model-routing/spec.md`
- `docs/cerb/scheduler-path-a-uni-a/interface.md`
- `docs/cerb/scheduler-path-a-uni-b/interface.md`
- `app-core/src/main/java/com/smartsales/prism/ui/sim/SimSchedulerIngressCoordinator.kt`
- `core/pipeline/src/main/java/com/smartsales/core/pipeline/SchedulerIntelligenceRouter.kt`

## Repro Steps

1. Clear device log buffer with `adb logcat -c`
2. Start live capture with `adb logcat -v time -s SimSchedulerIngress:V VALVE_PROTOCOL:I AndroidRuntime:E ActivityManager:I`
3. Speak `下周三早上8点提醒我买机票`
4. Speak `把拿合同改到今天下午4点`
5. Speak `把拿合同的时间推迟1个小时`

## What Was Verified

- scheduler exact create still routes through deterministic `Uni-A`
- explicit-time targeted reschedule still routes through `GLOBAL_RESCHEDULE`
- delta-only reschedule still rejects through the documented unsupported branch
- the observed delta-only failure is a contract rejection, not a scheduler-lane model drift signal

## Key Runtime Findings

### 1. Exact create remains healthy after the scheduler lane split

Observed log lines:

- `route_preflight ... transcript=下周三早上8点提醒我买机票。`
- `route_decision intent=CREATE shape=SINGLE_EXACT owner=UNI_A terminal=true reason=none`
- `create_result kind=CreateTasks routeStage=UNI_A uniM=NOT_MULTI itemCount=1 parseUnresolved=0 downgraded=0`

Interpretation:

- the canonical exact-create phrase still takes the deterministic create branch
- there is no evidence here of scheduler extraction drift or malformed create payload generation

### 2. Supported explicit-time reschedule remains healthy

Observed log lines:

- `route_preflight ... transcript=把拿合同改到今天下午4点。`
- `route_decision intent=RESCHEDULE shape=TARGETED_UPDATE owner=GLOBAL_RESCHEDULE terminal=true reason=none`
- `VALVE_PROTOCOL ... SIM scheduler global shortlist built`
- `VALVE_PROTOCOL ... SIM scheduler global suggestion received`

Interpretation:

- targeted reschedule with an explicit new time point still enters the supported global-reschedule lane
- this proves the on-device failure is not a blanket reschedule parsing failure

### 3. Delta-only reschedule is rejected by contract

Observed log lines:

- `route_preflight ... transcript=把拿合同的时间推迟1个小时。`
- `route_decision intent=RESCHEDULE shape=UNSUPPORTED owner=REJECT terminal=false reason=timeInstruction is a delta-only change ('推迟1个小时'), which is not supported per rule 7; requires explicit new time point`
- `ui_failure branch=reject intent=RESCHEDULE owner=REJECT displayed=SIM 当前仅支持明确目标 + 明确时间改期`

Interpretation:

- the failure happens at the scheduler routing/contract layer
- the utterance is understood as a reschedule request, but intentionally rejected because current product rules require an explicit target time point
- this is not evidence that the scheduler LLM failed to produce plausible structured output

## Verdict

Status: accepted

Reason:

- the scheduler-only extractor lane split did not regress the checked on-device create or supported reschedule paths
- the reproduced failure is the expected safe-fail behavior for delta-only reschedule phrasing under the current scheduler contract
- current evidence does not support freezing or changing the scheduler model as a fix for this specific repro

## Residual Gaps

- this rerun did not exercise a vague create phrase through the scheduler LLM fallback lane
- this rerun did not compare pre-split versus post-split fallback payloads for a known `Uni-B` or `Uni-C` case

Those remain optional follow-up if the team wants direct L3 evidence about the new scheduler-only `qwen-plus` fallback lane itself rather than the current contract diagnosis.
