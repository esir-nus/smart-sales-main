> ⚠️ **ARCHIVED** - This file is historical reference only. Current spec: `docs/Orchestrator-V1.md`

# T-Task: Engineering Trace (Orchestrator)

Current spec: `docs/Orchestrator-V1.md` (CURRENT)  
Purpose: track active workstreams and preserve archived traces.  
Note: This file is CURRENT. Historical traces (e.g., V7) are preserved under Archived Traces and frozen.

---

## 1) V1 Workstreams (CURRENT)

### WS-V1-1 — Disector plan + batch policy
- 10min target + 10s pre-roll overlap
- Absolute timeline grounding and deterministic plan persistence

### WS-V1-2 — Tingwu batch orchestration
- maxInflight=10 queue + retry scaffolding
- Batch completion can be out-of-order; publish is prefix-only

### WS-V1-3 — MemoryCenter interface + SessionMemory schema
- LLM outputs are optional; schema must be stable
- Speaker mapping evolves by batchIndex

### WS-V1-4 — Publisher deterministic rendering
- Absolute time anchoring + overlap range filtering
- Transcript + analysis views, no suspicious-gap polishing

---

## 2) Task List (V1)

Legend: TODO / DOING / DONE / BLOCKED

### T1-001 Orchestrator-V1 spec + doc sync
- Status: DONE
- Evidence:
  - `docs/Orchestrator-V1.md`

---

## Archived Traces

### V7 Dev Trace (Orchestrator–MetaHub) [ARCHIVED]

# T-Task: V7 Dev Trace (Orchestrator–MetaHub) [ARCHIVED]

Doc version: 1.0  
Target spec: `docs/archived/Orchestrator-MetadataHub-V7.md` (ARCHIVED)  
Replacement: `docs/Orchestrator-V1.md`  
Purpose: Track V7 engineering workstreams, decisions, and verification evidence.

> 说明：V7 已弃用并归档。本文件冻结，仅供历史参考；新任务以 V1 规范为准。

---

## 0) Scope & Outcomes (Expected Results Under Control)

V7 targets:

- Tingwu + OSS as default transcription lane; XFyun lane present but disabled by default.
- M1/M2/M3/M4 model documented and enforceable:
  - M1: UserMetadata (static JSON from onboarding/user-center)
  - M2: ConversationDerivedState (dynamic session state; provenance + uiSignals reserved)
  - M3: SessionState + RenamingMetadata (accepted vs candidate; export gating)
  - M4: External Knowledge & Style placeholder (API + pack versioning + HUD trace)
- HUD has 3 copy/paste sections:
  1) Effective run snapshot
  2) Raw transcription output
  3) Preprocessed snapshot

---

## 1) Workstreams

### WS1 — Docs & Contracts
- V7 spec doc (Orchestrator–MetaHub)
- V7 schema (`docs/archived/metahub-schema-v7.json`)
- api-contracts + ux-contract alignment
- AGENTS.md updated to set V7 as CURRENT
- Archived banners added to V6/V5/V4

### WS2 — Provider Lanes (Integration)
- Tingwu + OSS lane (default) verification
- XFyun lane (disabled by default) wiring audit + guardrails
- Third-party integration registry (docs/source-repo.json + schema) kept 3rd-party only

### WS3 — Multi-agent Pipeline (Design -> Implementation)
- Agent1 (Memory Keeper + Smart Analysis) patch model for M2
- Agent2 (Polisher) bounded edits model
- Pseudo-streaming batch-by-batch contract

### WS4 — Debug/HUD Observability
- Snapshot composition rules (3 sections)
- Redaction rules: no secrets, no raw HTTP bodies, copy-only JSON blocks

### WS5 — Export (Gated by Smart Analysis)
- Export gating model
- Export naming via RenamingMetadata

### WS6 — External Knowledge & Style (M4 placeholder)
- Pack versioning fields, retrieval trace, consent/feature flags (design only)

---

## 2) Task List (Status Tracking)

Legend: TODO / DOING / DONE / BLOCKED

### T7-001 Doc sync to V7 (spec + contracts + schema)
- Status: DONE
- Evidence:
  - `docs/archived/Orchestrator-MetadataHub-V7.md`
  - `docs/archived/metahub-schema-v7.json`
  - `docs/api-contracts.md`
  - `docs/ux-contract.md`
  - `docs/AGENTS.md`

### T7-002 Archive banners (V6/V5/V4)
- Status: DONE
- Evidence:
  - banner inserted at top of archived docs

### T7-003 Third-party registry refactor (OSS/Tingwu/XFyun/Dashscope only)
- Status: DONE
- Evidence:
  - `docs/source-repo.json`
  - `docs/source-repo.schema.json`

### T7-004 Implementation: MetaHub storage for M2/M3 (patch-based)
- Status: DONE
- Definition of done:
  - M2 writes are patch-based with provenance
  - M3 renaming accepted vs candidate works and is stable after user override
 - Evidence:
  - `core/util/src/main/java/com/smartsales/core/metahub/ConversationDerivedState.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/M2PatchRecord.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/MetaHubM2Helper.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/MetaHub.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/InMemoryMetaHub.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/FileBackedMetaHub.kt`
  - `core/util/src/test/java/com/smartsales/core/metahub/M2PatchMergeTest.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/metahub/FileBackedMetaHubTest.kt`
  - Tests:
    - `./gradlew :core:util:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 7s）
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 14s）
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 22s）
 - Semantics summary:
  - M2 补丁记录为内部派生结构（schema 未定义 patch type），仅用于存储与确定性合并。
  - 合并按补丁追加顺序应用，后写覆盖（仅覆盖补丁中显式字段）。
  - effective.updatedAt 取最后一个补丁 createdAt；version 取补丁数量，读取不产生新时间戳。
  - PatchHistory 与 effectiveM2 落盘由 FileBackedMetaHub 统一持久化（原子写）。
 - 📱 On-device sanity checklist (manual):
  - [ ] 触发至少 1 个 M2 补丁写入（使用现有调试入口/流程）
  - [ ] 强制停止 App 后重启
  - [ ] 验证同一 session 的 m2PatchHistory/effectiveM2 仍可加载（HUD/文件路径确认）

### T7-004A Implementation: MetaHub M3 naming stability (candidate/accepted/effective)
- Status: DONE
- Evidence:
  - `core/util/src/main/java/com/smartsales/core/metahub/RenamingMetadata.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/MetaHubRenamingHelper.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/SessionMetadata.kt`
  - `core/util/src/main/java/com/smartsales/core/metahub/ExportNameResolver.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryViewModel.kt`
  - `core/util/src/test/java/com/smartsales/core/metahub/RenamingMetadataTest.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/ExportNameResolverTest.kt`
  - `feature/chat/src/test/java/com/smartsales/feature/chat/history/ChatHistoryViewModelTest.kt`
  - Tests:
    - `./gradlew :core:util:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 8s）
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 18s）
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 12s）
- Semantics summary:
  - M3 命名直接写入 `SessionMetadata.renaming`，避免平行 SessionState 结构。
  - accepted 优先于 candidate，candidate 更新不会覆盖 accepted（合并与 helper 均遵循）。
  - effective name 解析为 accepted > candidate > fallback。
  - ExportNameResolver 优先使用 M3 命名：先所有 accepted，再 candidate。
  - 用户手动改名时写入 M3 accepted 与对应时间戳（userRenamedAt）。

### T7-005 Implementation: provider lane selection (default Tingwu+OSS; XFyun disabled)
- Status: DONE
- Definition of done:
  - Effective lane selection visible in HUD Section 1
  - XFyun cannot be used unless explicitly enabled and preflight validated
  - Evidence:
    - `data/ai-core/src/main/java/com/smartsales/data/aicore/params/AiParaSettings.kt`（默认 Tingwu；新增 `xfyunEnabled=false` gate）
    - `data/ai-core/src/main/java/com/smartsales/data/aicore/params/TranscriptionLaneSelector.kt`（集中判定 lane + disabled reason）
    - `app/src/main/java/com/smartsales/aitest/audio/SwitchableAudioTranscriptionCoordinator.kt`（未开启 XFyun 时回退 Tingwu）
    - `app/src/main/java/com/smartsales/aitest/ui/screens/audio/AiParaSettingsViewModel.kt`（用户显式选择 XFyun 时开启 gate）
    - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt` + `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt`（HUD Section 1 展示 lane + 禁用原因；Tingwu HUD Raw/Transcript 导出按钮）
    - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt` + `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt`（Tingwu 原始转写落盘；HUD 展示导出入口）
    - Tests:
      - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 8s）
      - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 8s）

### T7-006 Implementation: HUD 3-block copy snapshot
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugSnapshot.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugSnapshotRedactor.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 17s）
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 25s）

### T7-007 Implementation: pseudo-streaming transcript batches
- Status: DONE
- Evidence:
  - `feature/media/src/main/java/com/smartsales/feature/media/audiofiles/AudioTranscriptionCoordinator.kt`（新增批次事件流接口）
  - `feature/media/src/main/java/com/smartsales/feature/media/audiofiles/TranscriptionBatchPlanner.kt`（固定行数批次切分）
  - `app/src/main/java/com/smartsales/aitest/audio/DefaultAudioTranscriptionCoordinator.kt`（Tingwu 批次事件输出）
  - `app/src/main/java/com/smartsales/aitest/audio/XfyunAudioTranscriptionCoordinator.kt`（XFyun 批次事件输出）
  - `app/src/main/java/com/smartsales/aitest/audio/SwitchableAudioTranscriptionCoordinator.kt`（批次流委托）
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`（批次合并 + 完成冻结）
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt`（批次计划记录）
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`（Section 3 批次计划展示）
  - `feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeTranscriptionTest.kt`（批次流单测）
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 8s）
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 11s）
  - On-device sanity checklist (manual):
    - 使用中/长音频发起转写，确认逐字稿气泡至少出现 2 次批次更新。
    - 处理中显示“转写进度”提示，最终完成后停止流式标记。
    - 完成后说话人标签不再变化（冻结）。
    - HUD Section 3 显示批次计划摘要与预览（缺失时为占位提示）。

### T7-008 Implementation: Smart Analysis gating for export
- Status: DONE
- Evidence:
  - `core/util/src/main/java/com/smartsales/core/metahub/ExportNameResolver.kt`（accepted/candidate/fallback 命名解析）
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`（导出 gate 判定 + 早退）
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt`（导出按钮禁用 + 原因提示）
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/ExportOrchestrator.kt`（导出防线 gate + 命名落地）
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`（HUD Section 1 展示 exportGate + resolved name）
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 13s）
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 33s）
  - 📱 On-device sanity checklist (manual):
    - [ ] Smart Analysis 未就绪时导出按钮禁用并提示原因
    - [ ] Smart Analysis 完成后导出按钮自动可用
    - [ ] 导出文件名遵循 accepted > candidate > fallback
    - [ ] HUD Section 1 展示 exportGate 状态 + resolved name

### T7-008.1 Implementation: export chips immediate + auto-analysis auto-export
- Status: DONE
- Evidence:
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt`（导出快捷技能立即动作，不再走选中+发送）
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`（自动分析+自动导出状态机，最后一次点击优先）
  - `feature/chat/src/test/java/com/smartsales/feature/chat/home/HomeExportActionsTest.kt`（未就绪导出触发自动分析+导出）
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 8s）
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 28s）
  - 📱 On-device sanity checklist (manual):
    - [ ] Smart Analysis 未就绪时点击“导出 PDF/CSV”，自动触发智能分析
    - [ ] 分析完成后自动弹出导出分享（无需再次点击）
    - [ ] Smart Analysis 已就绪时点击导出立即生效
    - [ ] Smart Analysis 芯片仍需发送触发（模式不变）
    - [ ] 多次点击导出时以最后一次点击为准

### T7-008.2B Implementation: interrupted audio recovery banner (Home)
- Status: DONE
- Evidence:
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt`（Home 顶部横幅 + 两个动作）
  - Tests:
    - `./gradlew :core:util:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 7s）
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 8s）
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 15s）
  - 📱 On-device sanity checklist (manual):
    - [ ] 进行音频转写并中途强制停止 App，重启后 Home 顶部横幅出现
    - [ ] 点击“知道了”后横幅消失，重启后同一 startedAt 不再出现
    - [ ] 点击“重新上传”打开音频选择流程，选文件后正常进入转写且不发送消息

### T7-009 Placeholder: External Knowledge & Style (M4) API interface
- Status: TODO
- Definition of done:
  - feature flag + consent gates defined
  - HUD Section 1 logs pack versions + traceId (when enabled)

### T7-009 Implementation: Persistent MetaHub (file-backed)
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/FileBackedMetaHub.kt`
  - `app/src/main/java/com/smartsales/aitest/MetaHubModule.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/metahub/FileBackedMetaHubTest.kt`
  - Tests:
    - `./gradlew :core:util:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 14s）
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 15s）
    - `./gradlew :feature:chat:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 34s）
- Notes:
  - 仅持久化 SessionMetadata（含 M3 renaming）；M2 patch 仍未落盘（V7 mismatch，需后续专门任务）
  - Errata: M2 patch persistence statements conflict with T7-004; treat T7-004 evidence/tests as authoritative; this V7 trace is preserved as-is.
- 📱 On-device sanity checklist (manual):
  - [ ] 进入会话并执行智能分析，生成 latestMajorAnalysis*
  - [ ] 手动重命名会话（写入 M3 accepted）
  - [ ] 强制停止 App 后重启
  - [ ] 验证导出 gate 仍为已就绪
  - [ ] 验证 HUD Section 1 显示相同 sessionId + M3 accepted 命名
  - [ ] 验证会话改名仍存在

### T7-010B1 Implementation: Tingwu preprocess → MetaHub M2 patch
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`（转写完成追加 Tingwu 预处理补丁）
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/TingwuPreprocessPatchBuilder.kt`（确定性预览/批次构建）
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/TingwuPreprocessPatchBuilderTest.kt`（补丁写入后预处理快照）
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 29s）
- 📱 On-device sanity checklist (manual):
  - [ ] 完成 Tingwu 转写后，HUD Section 3 显示 MetaHub 预处理预览与批次
  - [ ] 强制停止并重启后，同一会话仍能显示预处理快照

### T7-010B2 Implementation: HUD Section 3 MetaHub preprocess provenance line
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/debug/RealDebugOrchestratorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 25s）
- 📱 On-device sanity checklist (manual):
  - [ ] 完成 Tingwu 转写后，HUD Section 3 显示 preprocess.source 行

### T7-010B3A Tingwu preprocess trace capture (A1/A2)
- A1 Status: DONE（补齐 TingwuTraceStore 字段与排序，尚未接入真实产出点）
- A2 Status: DONE（Tingwu 完成后优先使用 trace 批次/边界）
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/TingwuTraceStore.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/metahub/TingwuPreprocessPatchBuilder.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/RealTingwuCoordinatorTest.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/TingwuPreprocessPatchBuilderTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 1m 43s）
- 📱 On-device sanity checklist (manual):
  - [ ] Tingwu 转写完成后，MetaHub M2 preprocess 使用 trace 批次与可疑边界

### T7-010B3B1 Tingwu suspicious-boundary detector (pure helper)
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuSuspiciousBoundaryDetector.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/tingwu/TingwuSuspiciousBoundaryDetectorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 20s）
- 📱 On-device sanity checklist (manual):
  - [ ] 传入含时间戳与说话人标签的转写文本，探测器返回非空可疑边界

### T7-010B3B2 Tingwu suspicious-boundary wiring (trace → M2)
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/RealTingwuCoordinatorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 27s）
- 📱 On-device sanity checklist (manual):
  - [ ] Tingwu 转写完成后，trace 中 suspiciousBoundaries 非空且 M2 preprocess 使用它们

### T7-010B3C Debug HUD — Tingwu suspiciousBoundaries inspector
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/debug/RealDebugOrchestratorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 20s）
- 📱 On-device sanity checklist (manual):
  - [ ] HUD Section3B 显示 Tingwu suspiciousBoundaries（count/indices/details）

### T7-010B3D Debug HUD hygiene — suppress legacy xfyun.suspiciousBoundaries
- Status: DONE
- Evidence:
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/debug/DebugOrchestrator.kt`
  - `data/ai-core/src/test/java/com/smartsales/data/aicore/debug/RealDebugOrchestratorTest.kt`
  - Tests:
    - `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`（BUILD SUCCESSFUL in 20s）
- 📱 On-device sanity checklist (manual):
  - [ ] Tingwu preprocess 时，Section3 的 xfyun.suspiciousBoundaries 显示 suppressed 文案

---

## 3) Decision Log

- D7-001: Third-party integration registry is limited to OSS/Tingwu/XFyun/Dashscope + placeholders; it must not contain product specs or M1/M2/M3 schema.
- D7-002: XFyun REST parameter tables remain only in `docs/xfyun-asr-rest-api.md` (no duplication).
- D7-003: UI is allowed to read M2 for future playful UX; stabilized uiSignals reserved as future upgrade.

---

## 4) Open Questions / Risks

- R7-001: Tingwu+OSS lane specifics (OSS vendor/API shape) not documented here; must be discovered from code + tests.
- R7-002: M2 stabilization (hysteresis) is not implemented in V7 docs; reserved for future upgrade.
- R7-003: External memory / M4 governance: pack update process and permissions are TBD.

---

## 5) Verification Checklist (when implementation starts)

- [x] 已恢复默认转写提供方为 Tingwu+OSS；XFyun 默认禁用（证据：`app/src/main/java/com/smartsales/aitest/ui/screens/audio/AudioFilesShell.kt`；测试：`./gradlew :data:ai-core:testDebugUnitTest --no-daemon`）
- [ ] HUD 3 sections present and copyable
- [ ] No raw JSON visible in normal UI bubbles
- [ ] Export gated by Smart Analysis
- [ ] M4 placeholder does not change behavior unless enabled
