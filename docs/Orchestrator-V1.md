<!-- 文件路径: docs/Orchestrator-V1.md -->
<!-- 文件作用: Orchestrator-V1 架构规范（唯一权威） -->
<!-- 说明: V7 已弃用并归档；本文件为当前实现与协作的唯一来源 -->

# Orchestrator-V1 架构规范（源真源 / CURRENT）

版本：1.0.0  
状态：CURRENT（现行规范）  
替代：`docs/archived/Orchestrator-MetadataHub-V7.md`（ARCHIVED）

---

## 0) 目标与非目标

### 目标
- 以**最佳推理与最佳用户结果/分析**为首要目标，而非“完美逐字稿”。
- 端到端**可复现、可恢复、可观测**：确定性、幂等、可回放。
- 允许任务乱序完成，但**用户看到的顺序必须正确**。
- 轻量实现，避免引入重型分布式系统。

### 非目标（必须显式拒绝）
- **不做**“可疑间隙/suspicious gap”式转写润色或伪纠错。
- **不做**逐行时间戳精修（V1 只支持章节级时间）。
- **不依赖**任何 chain-of-thought 或隐藏推理。
- **不承诺**跨供应商翻译/预测等高级能力。

---

## 1) 系统总览（四大模块）

1. **Disector（确定性拆分器，非 LLM）**
   - 负责根据录音时长生成**绝对时间轴批次**（DisectorPlan）。
   - 输出稳定、可持久化，且可在重启后复现。

2. **Tingwu（ASR 集成）**
   - 每个批次对应一个 Tingwu Job。
   - 输出三个支柱：**transcription / summary / chapters**。
   - 允许批次乱序完成，但写入必须幂等。

3. **MemoryCenter（LLM 侧）**
   - 将 Tingwu 的 summary + chapters 整理为结构化 SessionMemory。
   - **必须按 batchIndex 顺序处理**，不按完成顺序。
   - 产出 speaker mapping 等给 Publisher 使用。

4. **Publisher（确定性渲染器，非 LLM）**
   - 产出两类结果：**Transcript 视图**与**Result/Analysis 视图**。
   - 对重叠区做**确定性去重**（V1 使用区间过滤）。

---

## 2) Disector 规则（批次拆分与微重叠）

### 2.1 触发规则
- 若录音时长 `<= 20 分钟`：不拆分，单批次直送 Tingwu。
- 若 `> 20 分钟`：进入拆分流程。

### 2.2 批次长度公式（必须按此实现）
- `TEN_MIN_MS = 600_000`
- `full = floor(totalMs / TEN_MIN_MS)`
- `remMs = totalMs % TEN_MIN_MS`
- 先生成 `full` 个 **10 分钟**批次（宏观时间轴）。
- 余数规则：
  - 若 `remMs < 7 分钟`：合并进最后一个批次（最后批次长度 = 10min + rem）。
  - 若 `remMs >= 7 分钟`：新增一个余数批次（长度 = rem）。

### 2.3 示例（必须一致）
- 21 分钟 → (10, 11)
- 26 分钟 → (10, 16)  （余 6 < 7，合并）
- 27 分钟 → (10, 10, 7)
- 36 分钟 → (10, 10, 16)
- 37 分钟 → (10, 10, 10, 7)
- 39 分钟 → (10, 10, 10, 9)

### 2.4 微重叠策略（pre-roll only，确定性）
- `overlapMs = 10_000`
- **宏观窗口**（权威时间轴）：`absStartMs..absEndMs`，半开区间：`abs ∈ [absStartMs, absEndMs)`
- **捕获范围**（实际提交给 Tingwu 的音频）：
  - 首批次：`captureStartMs = absStartMs`
  - 其余批次：`captureStartMs = max(0, absStartMs - overlapMs)`
  - **所有批次**：`captureEndMs = absEndMs`（无 post-roll）
- 意图与约束：
  - 仅为**后续批次**提供预热；不为前一批次增加后缀重叠。
  - **禁止**出现“前后各 10s”的 20s 重叠。

---

## 3) Tingwu 集成规则

### 3.1 Job 生成与提交
- 每个批次一个 Tingwu Job。
- **按 batchIndex 顺序**提交，最大并发 `maxInflight = 10`。

### 3.2 重试策略（确定性）
- 默认重试：`2s → 8s → 32s`，`maxAttempts = 3`。
- 429 限流：`60s → 120s → 300s`，`maxAttempts = 3`。
- 非重试：鉴权失败/参数错误类 4xx（除 429）。

### 3.3 时间戳可靠性
- Tingwu 时间戳仅为**相对提示**。
- 绝对时间一律由 DisectorPlan 推导。

---

## 4) MemoryCenter（结构化记忆）

### 4.1 输入与排序
- 输入：每批次 Tingwu 的 `summary + chapters`。
- **必须按 batchIndex 顺序处理**。

### 4.2 输出（SessionMemory）能力
- 统一上下文，为后续 LLM 任务提供稳定输入。
- 输出 speaker mapping（给 Publisher 使用）。
- 支持“未来思考/工作日志”扩展，但 V1 **不依赖** chain-of-thought。
- 结构字段（可选/最小集）：
   - `batchSummaries`：按 batchIndex 的简短批次摘要列表。
   - `highlights` / `decisions` / `nextSteps`：面向分析视图的要点清单。

### 4.3 章节时间校准（与 captureStart 一致）
- 若 Tingwu 提供 `chapter.offsetMs`：
  - `chapterAbsStartMs = batch.captureStartMs + offsetMs`
- 过滤到宏观窗口：
  - 若 `chapterAbsStartMs ∈ [batch.absStartMs, batch.absEndMs)`，保留
  - 否则回退到 `batch.absStartMs`（V1 选择稳定回退）

---

## 5) Publisher（确定性渲染）

### 5.1 输出类型
1) **Transcript 视图**：`MM:SS  Speaker: utterance`
2) **Result/Analysis 视图**：使用 SessionMemory 的结构化结果与章节时间线

### 5.2 绝对时间锚定（必须一致）
- 如果 Tingwu 提供相对段：
  - `absStart = batch.captureStartMs + relativeStartMs`
  - 若有 `relativeEndMs`：`absEnd = batch.captureStartMs + relativeEndMs`

### 5.3 重叠去重（V1 策略，确定性）
- **默认策略仅做区间过滤**（无文本相似度）
- 规则：
  - 若 `(absStart, absEnd)` 已知：发布当且仅当 `absEnd > batch.absStartMs && absStart < batch.absEndMs`
  - 若仅有 `absStart`：发布当且仅当 `absStart ∈ [batch.absStartMs, batch.absEndMs)`
- 保留“去重 hook”接口用于未来扩展，但 V1 不启用文本相似度。

#### 5.3.1 范围过滤示例（计算口径）
- 已知：`batch.absStartMs=600000`，`batch.absEndMs=1200000`，`captureStartMs=590000`
- Tingwu 段：`relStartMs=15000`，`relEndMs=20000`
- 绝对时间：`absStart=605000`，`absEnd=610000`
- 因 `absEnd > 600000` 且 `absStart < 1200000`，段落发布。

### 5.4 当无相对段信息时
- 以**批次块**形式发布（每批次一个 block）。
- 明确接受 pre-roll 区域可能重复的限制。

### 5.5 连续前缀发布（硬性不变量）
- 只发布连续前缀 `b1..bk`。
- 持久化 `publishedPrefixBatchIndex`，只允许单调递增。

---

## 6) 时间与顺序不变量（必须明确）

- 所有发布的时间戳**绝对**且**来源于 DisectorPlan**。
- Tingwu 时间戳仅为相对提示，必须先锚定到 `captureStartMs`。
- 宏观时间窗口为半开区间 `[absStartMs, absEndMs)`，避免交叉归属。
- 排序基元为 `batchIndex`（数值），**禁止用 batchId 字符串排序**。

---

## 7) 数据契约（V1 形状定义）

> 以下为实现级数据结构示意（JSON/Kotlin 等价）。

### 7.1 DisectorPlan
```
DisectorPlan {
  sessionId: String
  version: Int = 1
  totalMs: Long
  overlapMs: Long = 10_000
  batches: List<DisectorBatch>
}

DisectorBatch {
  batchIndex: Int        // 1-based, ordering primitive
  batchId: String        // "b<batchIndex>"
  absStartMs: Long
  absEndMs: Long
  captureStartMs: Long
  captureEndMs: Long
  durationMs: Long
}
```

### 7.2 Tingwu 批次产物
```
TingwuBatchArtifacts {
  sessionId: String
  batchIndex: Int
  batchId: String
  jobId: String
  providerMeta: {
    attempt: Int
    status: String
    rawRef?: String
  }
  transcription: TingwuTranscription
  summary: TingwuSummary
  chapters: List<TingwuChapter>
}

TingwuChapter {
  title: String
  offsetMs?: Long
  durationMs?: Long
}
```

### 7.3 SessionMemory（MemoryCenter 输出）
```
SessionMemory {
  sessionId: String
  schemaVersion: Int = 1
  updatedAtMs?: Long
  speakerMap: List<SpeakerMapping>
  chapters: List<MemoryChapter>
  highlights?: List<String>
  decisions?: List<String>
  nextSteps?: List<String>
}

SpeakerMapping {
  speakerKey: String           // S1/S2 or provider speaker id
  displayName: String
  title?: String
  confidence: Float
  effectiveFromBatchId: String // "b<batchIndex>"
}

MemoryChapter {
  title: String
  absStartMs: Long
  absEndMs?: Long
  batchId: String
}
```

### 7.4 PublishedTranscript（Publisher 输出）
```
PublishedTranscript {
  sessionId: String
  version: Int = 1
  publishedPrefixBatchIndex: Int
  items: List<PublishedUtterance>
}

PublishedUtterance {
  absStartMs: Long
  absEndMs?: Long
  speakerKey: String
  speakerDisplay: String
  text: String
  batchId: String
}
```

---

## 7.5) Schemas & JSON artifacts（CURRENT）

以下 JSON 文件为 V1 的**当前权威**数据定义与示例：

- Schema：`docs/orchestrator-v1.schema.json`
- Examples：`docs/orchestrator-v1.examples.json`

说明：
- 关键不变量通过 `$comment` 标注（例如 pre-roll only、half-open 宏观窗口、batchIndex 排序基元）。
- 以本节 JSON 为准进行数据校验与工具集成。

---

## 8) 状态机与执行顺序

### 8.1 批次状态机
`PLANNED → SUBMITTED → RUNNING → SUCCEEDED → (PUBLISHED)`
- 失败路径：`RUNNING → FAILED → RETRYING → RUNNING`（直到 maxAttempts）
- 终止：`FAILED`（不可重试时）

### 8.2 会话级流程
1) 生成 DisectorPlan
2) 批次按 batchIndex 提交 Tingwu（maxInflight=10）
3) 结果乱序回写，但**MemoryCenter 只处理前缀**
4) Publisher 只发布连续前缀
5) 会话完成后可触发“重发/重建”

---

## 9) 失败模式与恢复

- Tingwu 失败：按重试策略执行；超过次数则标记 FAILED，仍允许后续批次继续处理。
- MemoryCenter 失败：记录失败并保持“待处理”状态；可手动或自动重试。
- Publisher 失败：可重复执行（幂等），以 `publishedPrefixBatchIndex` 为唯一推进依据。
- 重启恢复：从持久化状态恢复 DisectorPlan / TingwuJobState / SessionMemory / PublisherState，继续未完成步骤。

---

## 10) 持久化与幂等

### 10.1 必须持久化
- DisectorPlan（批次宏观时间轴 + captureStartMs）
- TingwuJobState（提交/运行/成功/失败 + attempt）
- TingwuBatchArtifacts（transcription/summary/chapters）
- SessionMemory（按 batchIndex 生成）
- PublisherState（publishedPrefixBatchIndex + 可选 checksum）

### 10.2 幂等规则（V1 最小定义）
- 幂等键：`(sessionId, batchIndex, artifactType, version)`
- `DisectorPlan.version = 1`（确定性可重建）
- Tingwu artifacts：按 `(sessionId, batchId)` **upsert**；attempt 记录在 providerMeta
- Publisher：`PublishedTranscript.version = 1`，以 `publishedPrefixBatchIndex` 单调推进

---

## 11) 明确非目标（再次确认）
- 不做 suspicious gap/suspicious boundary 纠错与润色。
- 不输出逐行时间戳细化。
- 不依赖 chain-of-thought。
- 不引入重型基础设施（分布式调度/外部消息队列）。

---

## 12) 迁移 / 改造计划（V7 → V1）

### 12.1 需归档/标记弃用
- 规范文档：
  - `docs/archived/Orchestrator-MetadataHub-V7.md`
  - `docs/archived/metahub-schema-v7.json`
- V7 管线类（需明确标注 legacy）：
  - `com.smartsales.core.metahub.*`
  - `data/ai-core/.../metahub/FileBackedMetaHub.kt`
  - `data/ai-core/.../TranscriptOrchestrator.kt`
  - `data/ai-core/.../ExportOrchestrator.kt`
  - `data/ai-core/.../debug/DebugOrchestrator.kt`（V7 HUD 规则归档）

### 12.2 新增/替换组件
- DisectorPlan 生成器 + 持久化模块
- Tingwu 批次调度器（maxInflight=10）
- MemoryCenter（接口先行，逻辑后续）
- Publisher（确定性渲染 + 前缀发布）

### 12.3 最小滚动发布
- 引入 feature flag：`orchestratorV1Enabled`
- 双轨运行：V7 保持可用，V1 仅覆盖音频新会话
- 先上线 Disector + Tingwu 阶段，再逐步接入 MemoryCenter 与 Publisher

### 12.4 风险与缓解
- **时间锚定错误**：以 `captureStartMs` 锚定并区间过滤，增加单测
- **乱序完成导致错序**：强制前缀发布 + 前缀处理
- **重试风暴**：确定性退避 + 429 特殊延迟

---

## 13) 文档同步计划（执行清单）

- [x] 新建：`docs/Orchestrator-V1.md`（本文件，作为唯一架构源）
- [x] 新建：`docs/orchestrator-v1.schema.json`、`docs/orchestrator-v1.examples.json`
- [x] 更新：`AGENTS.md`、`docs/AGENTS.md`（V1 设为 CURRENT）
- [x] 更新任务追踪：`docs/T-Task.md`（CURRENT，含 V7 归档轨迹）
- [x] 更新：`docs/api-contracts.md`（对齐 V1 模块与事件）
- [x] 更新：`docs/ux-contract.md`（去除 V7 专用条款与可疑间隙润色）
- [x] 更新：`docs/current-state.md`（声明 V1 为现行架构）

**一致性校验关键词**（必须全文替换到一致）：
- `captureStartMs` 是相对时间锚点
- 微重叠为 pre-roll only
- 去重为区间过滤（非文本相似度）
- 排序以 batchIndex 为准
- 章节 offset 以 captureStartMs 计算并做宏观窗口过滤

---

## 14) Codex 实施提示（Phase 1，仅确定性组件）

> 目的：只实现确定性基础，不实现 MemoryCenter LLM 逻辑，不改 UI。

**实现范围**
1. DisectorPlan 生成器 + 持久化
2. Tingwu 批次提交队列（maxInflight=10）+ 重试脚手架
3. Publisher 基础渲染 + 区间过滤去重 hook

**强约束**
- 仅 pre-roll 重叠：`captureStartMs = max(0, absStartMs - overlapMs)`（非首批次）
- 绝对时间锚定：`abs = captureStartMs + relativeStartMs`
- 半开区间过滤：`abs ∈ [absStartMs, absEndMs)`
- 无相对段信息则输出批次级块（接受重复）
- 仅发布连续前缀；`publishedPrefixBatchIndex` 单调推进
- 不做 suspicious gap/polish

**重试规则（确定性）**
- 默认：`2s → 8s → 32s`（maxAttempts=3）
- 429：`60s → 120s → 300s`（maxAttempts=3）

**建议单测**
- DisectorPlan：`remMs == 7min` 边界、示例用例
- captureStartMs 预热逻辑（pre-roll only）
- 发布区间过滤（start-only / start+end）
- 前缀发布推进规则

**测试命令**
- `./gradlew :data:ai-core:testDebugUnitTest --no-daemon`
