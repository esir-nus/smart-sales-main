# 元数据中心结构分析报告

> **分析日期**: 2025-01-XX  
> **目的**: 为设计“元数据中心”（metadata hub）提供代码库结构参考  
> **范围**: 仅分析，不修改代码

---

## 1. 高层架构映射（元数据视角）

### 1.1 主要功能子系统

#### **聊天/会话管理子系统** (`feature/chat`)

**核心职责**: 管理聊天会话、消息历史、会话摘要、标题生成

**关键数据模型**:
- `AiSessionEntity` (`feature/chat/src/main/java/com/smartsales/feature/chat/persistence/AiSessionEntity.kt`)
  - `id: String` (session_id)
  - `title: String`
  - `preview: String` (lastMessagePreview)
  - `updatedAtMillis: Long`
  - `pinned: Boolean`
- `AiSessionSummary` (`feature/chat/src/main/java/com/smartsales/feature/chat/AiSessionRepository.kt`)
  - 与 `AiSessionEntity` 字段相同，用于内存表示
- `ChatMessageEntity` (`feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatMessageEntity.kt`)
  - `id: String`
  - `sessionId: String`
  - `role: String` (USER/ASSISTANT)
  - `content: String`
  - `timestampMillis: Long`
- `ChatMessageUi` (`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`)
  - `id: String`
  - `role: ChatMessageRole` (USER/ASSISTANT)
  - `content: String`
  - `timestampMillis: Long`
  - `isStreaming: Boolean`
  - `hasError: Boolean`

**关键仓库/服务**:
- `AiSessionRepository` (`feature/chat/src/main/java/com/smartsales/feature/chat/AiSessionRepository.kt`)
  - `InMemoryAiSessionRepository` - 内存实现，使用 `MutableStateFlow`
  - `RoomAiSessionRepository` - Room 持久化实现（通过 `AiSessionDao`）
- `ChatHistoryRepository` (`feature/chat/src/main/java/com/smartsales/feature/chat/history/ChatHistoryRepository.kt`)
  - `RoomChatHistoryRepository` - 使用 `ChatHistoryDao` 持久化消息
- `SessionTitleGenerator` (`feature/chat/src/main/java/com/smartsales/feature/chat/title/SessionTitleGenerator.kt`)
  - 基于首轮对话的启发式标题生成（格式：`MM/dd_主要对象_简短场景`）
  - 提取：主要人名（通过敬称/公司名模式）、场景关键词

**状态持久化**:
- `AiSessionEntity` → Room 数据库 (`AiSessionDatabase`)
- `ChatMessageEntity` → Room 数据库 (`ChatHistoryDatabase`)
- `AiSessionSummary` → 内存 (`InMemoryAiSessionRepository`)

**元数据生成点**:
- `HomeScreenViewModel.maybeGenerateSessionTitle()` - 首条助手回复后触发
- `HomeScreenViewModel.updateSessionSummary()` - 每次消息更新时更新 preview
- `HomeScreenViewModel.generateExportFileName()` - 导出时生成文件名（日期+人名+摘要）

---

#### **转写/Tingwu/说话人分离子系统** (`data/ai-core`)

**核心职责**: 提交 Tingwu 转写任务、轮询状态、解析结果、生成 markdown、说话人标签映射

**关键数据模型**:
- `TingwuTranscriptSegment` (`data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuApi.kt`)
  - `id: Int?`
  - `start: Double?` (秒)
  - `end: Double?` (秒)
  - `text: String?`
  - `speaker: String?` (原始 speaker ID，如 "spk_1")
- `TingwuSpeaker` (`data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuApi.kt`)
  - `id: String`
  - `name: String?` (Tingwu 返回的可选名称)
  - `totalTime: Double?`
- `TingwuDiarizedSegment` (`data/ai-core/src/main/java/com/smartsales/data/aicore/TingwuCoordinator.kt`)
  - `speakerId: String?`
  - `speakerIndex: Int` (1-based，按出现顺序)
  - `start: Double`
  - `end: Double`
  - `text: String`
- `TingwuJobArtifacts` (`data/ai-core/src/main/java/com/smartsales/data/aicore/TingwuCoordinator.kt`)
  - `speakerLabels: Map<String, String>` - speakerId → 显示名称（如 "spk_1" → "客户"）
  - `diarizedSegments: List<TingwuDiarizedSegment>?`
  - `transcriptionUrl: String?`
  - `autoChaptersUrl: String?`
  - `smartSummary: String?`

**关键服务**:
- `RealTingwuCoordinator` (`data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`)
  - `submit()` - 创建转写任务
  - `observeJob()` - 观察任务状态流
  - `buildSpeakerLabels()` - 构建 speakerId → 显示名称映射
  - `buildMarkdown()` - 将转写结果转换为 markdown（含说话人标签）

**说话人标签生成逻辑** (`RealTingwuCoordinator.buildSpeakerLabels()`):
1. 优先使用 `TingwuSpeaker.name`（如果 Tingwu 返回了名称）
2. 否则使用 `SpeakerDisplayConfig.defaultLabelsByIndex`（默认：`["客户", "销售"]`）
3. 如果都没有，使用 `"发言人 ${speakerIndex}"`

**配置**:
- `SpeakerDisplayConfig` (`data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreConfig.kt`)
  - `defaultLabelsByIndex: List<String> = listOf("客户", "销售")`
  - `hideLabelWhenSingleSpeaker: Boolean = true`

**状态持久化**:
- `TingwuJobState` → 内存 (`MutableStateFlow`，按 jobId 存储)
- 转写结果 markdown → 注入到聊天消息（通过 `HomeScreenViewModel.onTranscriptionRequested()`）

**元数据生成点**:
- `RealTingwuCoordinator.buildSpeakerLabels()` - 生成说话人标签映射
- `RealTingwuCoordinator.buildDiarizedSegments()` - 构建带说话人信息的分段
- 转写完成后，通过 `TranscriptionChatRequest` 传入 `HomeScreenViewModel`，创建新会话或注入现有会话

---

#### **设备管理器子系统** (`feature/media`)

**核心职责**: 管理设备文件列表、上传、应用、删除操作

**关键数据模型**:
- `DeviceFileUi` (`feature/media/src/main/java/com/smartsales/feature/media/devicemanager/DeviceManagerViewModel.kt`)
  - `id: String` (文件名)
  - `displayName: String`
  - `sizeText: String`
  - `mimeType: String`
  - `mediaType: DeviceMediaTab` (Videos/Gifs/Images)
  - `mediaLabel: String`
  - `modifiedAtText: String`
  - `mediaUrl: String`
  - `downloadUrl: String`
  - `isApplied: Boolean`
  - `durationText: String?`
  - `thumbnailUrl: String?`
- `DeviceMediaFile` (`feature/media/src/main/java/com/smartsales/feature/media/devicemanager/DeviceManagerViewModel.kt`)
  - `name: String`
  - `sizeBytes: Long`
  - `mimeType: String`
  - `modifiedAtMillis: Long`
  - `mediaUrl: String`
  - `downloadUrl: String`
  - `durationMillis: Long?`
  - `location: String?`
  - `source: String?`

**关键服务**:
- `DeviceManagerViewModel` (`feature/media/src/main/java/com/smartsales/feature/media/devicemanager/DeviceManagerViewModel.kt`)
  - 管理文件列表、上传、应用、删除
- `DeviceMediaGateway` - 与设备 HTTP API 交互

**状态持久化**:
- 文件列表 → 内存（`cachedFiles: List<DeviceFileUi>`）
- 无持久化数据库（每次刷新从设备拉取）

**元数据生成点**:
- 文件类型推断（通过 MIME type 和文件名）
- `isApplied` 状态（标记文件是否已应用到设备）
- 当前**无**与聊天会话的关联

---

#### **导出子系统** (`data/ai-core`)

**核心职责**: 将 markdown 转换为 PDF/CSV

**关键数据模型**:
- `ExportResult` (`data/ai-core/src/main/java/com/smartsales/data/aicore/ExportManager.kt`)
  - `fileName: String`
  - `mimeType: String`
  - `payload: ByteArray`
  - `localPath: String`
- `ExportFormat` - `PDF` / `CSV`

**关键服务**:
- `ExportManager` (`data/ai-core/src/main/java/com/smartsales/data/aicore/ExportManager.kt`)
  - `exportMarkdown(markdown: String, format: ExportFormat, suggestedFileName: String?)`
- `RealExportManager` - 实现类，使用 `MarkdownPdfEncoder` / `MarkdownCsvEncoder`

**文件名生成逻辑** (`HomeScreenViewModel.generateExportFileName()`):
1. 日期部分：`yyyy年MM月dd日`（通过 `formatDateForFileName()`）
2. 人名部分：优先使用 `userName`，否则 "客户"（通过 `extractPersonNameForFileName()`）
3. 摘要部分：
   - 优先使用 `latestAnalysisMarkdown` 的首行（前 20 字符）
   - 否则使用首条用户消息（前 20 字符）
   - 兜底："对话记录"
4. 最终格式：`${datePart}_${personPart}_${summaryPart}`

**状态持久化**:
- 导出文件 → 本地文件系统 (`context.cacheDir/exports/markdown`)

**元数据生成点**:
- `HomeScreenViewModel.generateExportFileName()` - 生成导出文件名
- 文件名依赖：`userName`（来自 `UserProfileRepository`）、`latestAnalysisMarkdown`、首条用户消息

---

#### **用户配置子系统** (`feature/usercenter`)

**核心职责**: 管理用户个人信息

**关键数据模型**:
- `UserProfile` (`feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserProfile.kt`)
  - `displayName: String?`
  - `email: String?`
  - `isGuest: Boolean`

**关键服务**:
- `UserProfileRepository` (`feature/usercenter/src/main/java/com/smartsales/feature/usercenter/data/UserProfileRepository.kt`)
  - `InMemoryUserProfileRepository` - 内存实现，默认值：`displayName = "SmartSales 用户"`

**状态持久化**:
- `UserProfile` → 内存（当前无持久化）

**元数据使用点**:
- `HomeScreenViewModel.deriveUserName()` - 从 `UserProfile` 提取用户名用于导出文件名
- `HomeScreenViewModel.loadUserProfile()` - 加载用户信息到 UI 状态

---

#### **编排器/绑定层** (`feature/chat/home`)

**核心职责**: 连接 ViewModel 与 AI 服务，构建 prompt，处理 streaming

**关键服务**:
- `DelegatingHomeAiChatService` (`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenBindings.kt`)
  - 将 `ChatRequest` 转换为 `AiChatRequest`
  - 构建 prompt（含历史对话、快捷技能标签、转写 markdown）
  - 处理 streaming 事件转换

**Prompt 构建逻辑** (`HomeScreenBindings.buildPromptWithHistory()`):
- 历史对话格式化：`"用户：${content}"` / `"助手：${content}"`
- 首条助手回复时，要求 LLM 输出格式：
  ```
  【客户】<主要客户或联系人名称>
  【场景】<简短的销售场景/任务名>
  ```
- 后续回复要求：复述问题 → 核心洞察 → 可执行建议

**元数据生成点**:
- Prompt 中要求 LLM 输出客户名和场景（但当前未解析回元数据）
- 转写 markdown 作为上下文注入 prompt

---

### 1.2 数据流向总结

**会话级元数据流向**:
```
用户发送消息 → HomeScreenViewModel.sendMessageInternal()
  → updateSessionSummary(userMessage.content)  // 更新 preview
  → maybeGenerateSessionTitle()  // 首条回复后生成标题
  → sessionRepository.updateTitle() / upsert()
```

**转写元数据流向**:
```
音频上传 → RealTingwuCoordinator.submit()
  → 轮询状态 → fetchTranscript()
  → buildSpeakerLabels()  // 生成说话人标签
  → buildMarkdown()  // 生成含说话人标签的 markdown
  → HomeScreenViewModel.onTranscriptionRequested()
  → 创建新会话或注入现有会话
```

**导出元数据流向**:
```
用户点击导出 → HomeScreenViewModel.exportMarkdown()
  → generateExportFileName()  // 使用 userName、latestAnalysisMarkdown、首条消息
  → ExportManager.exportMarkdown(markdown, format, suggestedFileName)
```

---

## 2. 当前“元数据表面”

### 2.1 会话级元数据

**`AiSessionEntity` / `AiSessionSummary`**
- **字段**: `id`, `title`, `preview` (lastMessagePreview), `updatedAtMillis`, `pinned`
- **写入者**:
  - `HomeScreenViewModel.ensureSessionSummary()` - 创建/更新会话摘要
  - `HomeScreenViewModel.updateSessionSummary()` - 更新 preview
  - `HomeScreenViewModel.maybeGenerateSessionTitle()` - 生成标题（通过 `SessionTitleGenerator`）
  - `HomeScreenViewModel.onTranscriptionRequested()` - 转写会话创建时设置标题（格式：`"通话分析 – ${fileName}"`）
- **读取者**:
  - `HomeScreenViewModel.observeSessions()` - 监听会话列表变化
  - `HomeScreenViewModel.applySessionList()` - 映射为 `SessionListItemUi`
  - `ChatHistoryScreen` - 显示会话列表

**缺失的元数据字段**:
- `mainContactName` - 主要联系人名称（当前仅在标题中通过启发式提取）
- `companyName` - 公司名称（当前仅在标题中通过启发式提取）
- `scenario` - 销售场景（当前仅在标题中通过启发式提取）
- `tags` - 标签列表（无）
- `isTranscription` - 是否为转写会话（当前通过标题前缀 `"通话分析 –"` 判断）

---

### 2.2 消息级元数据

**`ChatMessageEntity` / `ChatMessageUi`**
- **字段**: `id`, `sessionId`, `role`, `content`, `timestampMillis`, `isStreaming`, `hasError`
- **写入者**:
  - `HomeScreenViewModel.sendMessageInternal()` - 创建用户/助手消息
  - `HomeScreenViewModel.onTranscriptionRequested()` - 注入转写消息
  - `ChatHistoryRepository.saveMessages()` - 持久化
- **读取者**:
  - `HomeScreenViewModel.loadSession()` - 加载历史消息
  - `HomeScreenViewModel.buildChatRequest()` - 构建 prompt 时使用历史
  - `HomeScreenViewModel.buildTranscriptMarkdown()` - 导出时构建 markdown

**转写消息 vs 普通消息**:
- 当前**无**显式区分字段
- 转写消息通过 `onTranscriptionRequested()` 注入，但 `role` 仍为 `ASSISTANT`
- 转写会话通过 `currentSession.isTranscription` 标记（基于标题前缀）

**缺失的元数据字段**:
- `messageType` - 区分普通消息/转写消息/系统消息
- `sourceJobId` - 如果是转写消息，关联的 Tingwu jobId
- `quickSkillId` - 如果是快捷技能触发，记录技能 ID
- `attachments` - 附件列表（当前无）

---

### 2.3 转写元数据

**`TingwuJobArtifacts`**
- **字段**: `speakerLabels: Map<String, String>`, `diarizedSegments`, `transcriptionUrl`, `autoChaptersUrl`, `smartSummary`
- **写入者**:
  - `RealTingwuCoordinator.fetchTranscript()` - 转写完成后构建
- **读取者**:
  - `RealTingwuCoordinator.buildMarkdown()` - 生成 markdown 时使用说话人标签
  - 当前**未持久化**，仅在内存中传递

**`TingwuDiarizedSegment`**
- **字段**: `speakerId`, `speakerIndex`, `start`, `end`, `text`
- **写入者**:
  - `RealTingwuCoordinator.buildDiarizedSegments()` - 从 `TingwuTranscriptSegment` 转换
- **读取者**:
  - `RealTingwuCoordinator.buildMarkdown()` - 生成分段 markdown

**说话人标签映射** (`speakerLabels: Map<String, String>`):
- 当前映射：`"spk_1" → "客户"`, `"spk_2" → "销售"`（默认配置）
- 如果 Tingwu 返回了 `TingwuSpeaker.name`，优先使用
- 当前**未持久化**，每次转写重新生成

**缺失的元数据字段**:
- 说话人真实姓名（当前只有通用标签）
- 说话人角色推断（客户/销售/其他）
- 转写任务关联的会话 ID（当前通过 `TranscriptionChatRequest.sessionId` 传递，但未持久化）

---

### 2.4 设备/文件元数据

**`DeviceFileUi` / `DeviceMediaFile`**
- **字段**: `id` (文件名), `displayName`, `sizeBytes`, `mimeType`, `mediaType`, `modifiedAtMillis`, `mediaUrl`, `downloadUrl`, `isApplied`, `durationMillis`, `thumbnailUrl`
- **写入者**:
  - `DeviceManagerViewModel.fetchFilesInternal()` - 从设备 HTTP API 拉取
- **读取者**:
  - `DeviceManagerScreen` - 显示文件列表
  - 当前**无**与聊天会话的关联

**缺失的元数据字段**:
- `associatedSessionId` - 关联的聊天会话 ID（如果文件用于转写）
- `uploadedBy` - 上传者（用户信息）
- `tags` - 文件标签
- `description` - 文件描述

---

### 2.5 导出元数据

**导出文件名生成** (`HomeScreenViewModel.generateExportFileName()`):
- **当前逻辑**:
  1. 日期：`yyyy年MM月dd日`
  2. 人名：`userName` 或 "客户"
  3. 摘要：`latestAnalysisMarkdown` 首行 或 首条用户消息 或 "对话记录"
- **格式**: `${datePart}_${personPart}_${summaryPart}`

**缺失的元数据字段**:
- 导出时使用的会话元数据（客户名、公司名、场景）未结构化存储
- 导出历史记录（无）

---

### 2.6 用户配置元数据

**`UserProfile`**
- **字段**: `displayName`, `email`, `isGuest`
- **写入者**:
  - `UserProfileRepository.save()` - 用户编辑
- **读取者**:
  - `HomeScreenViewModel.loadUserProfile()` - 加载到 UI
  - `HomeScreenViewModel.deriveUserName()` - 用于导出文件名

**缺失的元数据字段**:
- `tenant` - 租户/组织
- `locale` - 语言区域
- `role` - 用户角色（销售/经理等）

---

## 3. 与元数据相关的数据流

### 3.1 新聊天会话 → 首条 AI 回复 → 会话标题/摘要

**流程**:
1. 用户发送首条消息 → `HomeScreenViewModel.sendMessageInternal()`
2. 创建用户消息 → `createUserMessage()`
3. 更新会话摘要 → `updateSessionSummary(userMessage.content)`（更新 preview）
4. 构建 prompt → `buildChatRequest()` → `buildPromptWithHistory()`
5. 发送请求 → `aiChatService.streamChat()`
6. 接收首条助手回复 → `ChatStreamEvent.Completed`
7. 生成标题 → `maybeGenerateSessionTitle()`
   - 检查 `isFirstAssistantReply`
   - 调用 `SessionTitleGenerator.deriveSessionTitle()`
   - 提取：日期、主要人名、场景关键词
   - 更新 `sessionRepository.updateTitle()`

**当前问题**:
- 标题生成是**启发式**的（正则匹配），不准确
- 提取的客户名、场景**未单独存储**，只存在标题字符串中
- LLM 在 prompt 中被要求输出 `【客户】` 和 `【场景】`，但**未解析回元数据**

**元数据生成点**:
- `SessionTitleGenerator.deriveSessionTitle()` - 生成标题（但信息丢失在字符串中）
- `HomeScreenViewModel.maybeGenerateSessionTitle()` - 触发标题生成

**元数据持久化点**:
- `sessionRepository.updateTitle()` - 更新标题（但客户名、场景未单独存储）

---

### 3.2 转写任务 → Tingwu 结果 → 聊天/转写消息

**流程**:
1. 用户上传音频 → `HomeScreenViewModel.onAudioFilePicked()`
2. 保存到本地 → `audioStorageRepository.importFromPhone()`
3. 上传到 OSS → `transcriptionCoordinator.uploadAudio()`
4. 提交转写任务 → `transcriptionCoordinator.submitTranscription()`
5. 创建转写会话 → `onTranscriptionRequested(TranscriptionChatRequest)`
   - `sessionId = "session-${UUID.randomUUID()}"`
   - `title = "通话分析 – ${fileName}"`
6. 轮询转写状态 → `RealTingwuCoordinator.startPolling()`
7. 转写完成 → `fetchTranscript()`
   - `buildSpeakerLabels()` - 生成说话人标签映射
   - `buildDiarizedSegments()` - 构建分段
   - `buildMarkdown()` - 生成含说话人标签的 markdown
8. 注入转写消息 → `HomeScreenViewModel.onTranscriptionRequested()`
   - 如果 `transcript` 非空，创建助手消息显示转写内容
   - 如果 `transcript` 为空，显示“正在转写...”占位消息，监听 `transcriptionCoordinator.observeJob()`

**当前问题**:
- 说话人标签映射（`speakerLabels`）**未持久化**，仅在内存中
- 转写任务 ID（`jobId`）**未关联到会话**，无法追溯
- 说话人真实姓名**未存储**（只有通用标签“客户/销售”）
- 转写分段（`diarizedSegments`）**未持久化**，只存在于 markdown 中

**元数据生成点**:
- `RealTingwuCoordinator.buildSpeakerLabels()` - 生成说话人标签映射
- `RealTingwuCoordinator.buildDiarizedSegments()` - 构建分段
- `HomeScreenViewModel.onTranscriptionRequested()` - 创建转写会话

**元数据持久化点**:
- 转写 markdown → 注入为聊天消息（`ChatMessageEntity`），但说话人标签、分段**未单独存储**

**元数据丢失点**:
- `TingwuJobArtifacts.speakerLabels` - 仅在内存中，转写完成后丢失
- `TingwuJobArtifacts.diarizedSegments` - 仅在内存中，转写完成后丢失
- `TingwuJobArtifacts.smartSummary` - 仅在内存中，转写完成后丢失

---

### 3.3 智能分析 / 快捷技能 → 增强元数据

**流程**:
1. 用户选择快捷技能 → `HomeScreenViewModel.onSelectQuickSkill()`
2. 发送消息 → `sendMessageInternal(skillOverride = quickSkillId)`
3. 构建 prompt → `buildChatRequest()` → `buildPromptWithHistory()`
   - `quickSkillId` 映射为 mode（如 `"SMART_ANALYSIS"`）
   - 注入到 `AiChatRequest.skillTags`
4. 接收回复 → `ChatStreamEvent.Completed`
5. 处理完成 → `handleSmartAnalysisCompleted()`（仅 SMART_ANALYSIS）
   - 保存 `latestAnalysisMarkdown = summary`
   - 显示导出提示

**当前问题**:
- 快捷技能 ID（`quickSkillId`）**未存储在消息中**，无法追溯
- 智能分析结果（`latestAnalysisMarkdown`）**未持久化**，仅存在 ViewModel 内存中
- 分析结果中的结构化信息（客户意图、痛点、机会、风险）**未解析为元数据**

**元数据生成点**:
- `HomeScreenViewModel.handleSmartAnalysisCompleted()` - 保存分析结果（但仅内存）
- LLM 输出结构化分析，但**未解析回元数据**

**元数据持久化点**:
- 分析结果作为助手消息内容存储（`ChatMessageEntity.content`），但**未结构化**

---

### 3.4 设备管理器 → 文件应用到会话/设备

**流程**:
1. 用户上传文件 → `DeviceManagerViewModel.onUploadFile()`
2. 上传到设备 → `mediaGateway.uploadFile()`
3. 应用文件 → `DeviceManagerViewModel.onApplyFile()`
4. 标记为已应用 → `updateAppliedFile(fileId, applied = true)`

**当前问题**:
- 文件**无**与聊天会话的关联
- 文件**无**标签或描述
- 文件应用状态（`isApplied`）**未持久化**（每次刷新从设备拉取）

**元数据生成点**:
- `DeviceManagerViewModel.updateAppliedFile()` - 更新应用状态（但仅内存）

**元数据持久化点**:
- **无**（文件列表每次从设备拉取）

---

## 4. 差距、重复和耦合

### 4.1 元数据重复

**标题中的信息重复**:
- `AiSessionSummary.title` 包含：日期、客户名、场景（格式：`MM/dd_主要对象_简短场景`）
- 这些信息**未单独存储**，只能通过字符串解析提取
- **问题**: 无法直接查询“所有与客户 X 的会话”或“所有场景为 Y 的会话”

**说话人标签重复**:
- `TingwuJobArtifacts.speakerLabels` 在内存中生成
- markdown 中已包含说话人标签（如 `"客户：你好"`）
- **问题**: 说话人标签映射**未持久化**，无法跨会话使用

**导出文件名生成逻辑重复**:
- `HomeScreenViewModel.generateExportFileName()` 从多个来源提取信息：
  - `userName`（来自 `UserProfileRepository`）
  - `latestAnalysisMarkdown`（内存）
  - 首条用户消息（从 `chatMessages` 提取）
- **问题**: 这些信息**未结构化存储**，每次导出重新计算

---

### 4.2 逻辑分散

**标题生成逻辑分散**:
- `SessionTitleGenerator.deriveSessionTitle()` - 启发式提取
- `HomeScreenViewModel.maybeGenerateSessionTitle()` - 触发逻辑
- `HomeScreenViewModel.onTranscriptionRequested()` - 转写会话标题硬编码（`"通话分析 – ${fileName}"`）
- **问题**: 标题生成逻辑分散在多个地方，难以统一管理

**说话人标签生成逻辑分散**:
- `RealTingwuCoordinator.buildSpeakerLabels()` - 生成映射
- `SpeakerDisplayConfig` - 配置默认标签
- `RealTingwuCoordinator.buildMarkdown()` - 使用标签生成 markdown
- **问题**: 说话人标签生成逻辑在 `RealTingwuCoordinator` 中，但**未持久化**，无法跨转写任务复用

**导出文件名生成逻辑分散**:
- `HomeScreenViewModel.generateExportFileName()` - 生成逻辑
- `HomeScreenViewModel.formatDateForFileName()` - 日期格式化
- `HomeScreenViewModel.extractPersonNameForFileName()` - 人名提取
- `HomeScreenViewModel.generateShortSummaryForFileName()` - 摘要提取
- **问题**: 文件名生成依赖多个数据源，逻辑复杂

---

### 4.3 元数据与原始内容未分离

**转写结果未分离**:
- 转写 markdown（含说话人标签）直接注入为聊天消息内容
- 说话人标签、分段信息**未单独存储**
- **问题**: 无法单独查询“所有说话人为 X 的转写片段”

**智能分析结果未分离**:
- 分析结果作为助手消息内容存储
- 结构化信息（洞察、行动项）**未解析为元数据**
- **问题**: 无法单独查询“所有包含痛点 X 的会话”

**会话标题未分离**:
- 标题包含日期、客户名、场景，但**未单独存储**
- **问题**: 无法直接过滤“所有与客户 X 的会话”

---

### 4.4 元数据隐式编码在文本中

**Prompt 中要求 LLM 输出元数据，但未解析**:
- `HomeScreenBindings.buildPromptWithHistory()` 要求首条回复格式：
  ```
  【客户】<主要客户或联系人名称>
  【场景】<简短的销售场景/任务名>
  ```
- **问题**: LLM 输出后，这些信息**未解析回元数据**，只存在消息内容中

**转写 markdown 中的说话人信息**:
- markdown 格式：`"客户：你好\n销售：欢迎光临"`
- **问题**: 说话人信息**未单独存储**，只能从 markdown 解析

**导出文件名中的信息**:
- 文件名格式：`yyyy年MM月dd日_客户名_摘要`
- **问题**: 这些信息**未结构化存储**，只能从文件名解析

---

### 4.5 具体问题位置

**`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`**:
- `generateExportFileName()` (行 381-418) - 文件名生成逻辑复杂，依赖多个数据源
- `maybeGenerateSessionTitle()` (行 2225-2241) - 标题生成触发逻辑
- `updateSessionSummary()` (行 2213-2223) - 仅更新 preview，未更新其他元数据
- `handleSmartAnalysisCompleted()` (行 1300-1308) - 分析结果仅存内存

**`feature/chat/src/main/java/com/smartsales/feature/chat/title/SessionTitleGenerator.kt`**:
- `deriveSessionTitle()` (行 30-41) - 启发式标题生成，信息丢失在字符串中
- `extractMajorName()` (行 43-57) - 客户名提取（正则匹配，不准确）
- `extractSummary()` (行 59-65) - 场景提取（关键词匹配，不准确）

**`data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`**:
- `buildSpeakerLabels()` (行 909-1027) - 说话人标签生成，但**未持久化**
- `buildDiarizedSegments()` (行 820-900) - 分段构建，但**未持久化**
- `buildMarkdown()` (行 1007-1070) - markdown 生成，说话人信息丢失在文本中

**`feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenBindings.kt`**:
- `buildPromptWithHistory()` (行 139-177) - 要求 LLM 输出元数据格式，但**未解析**

---

## 5. 未来元数据中心的推荐锚点（仅设计输入）

### 5.1 会话级元数据的自然归属

**推荐位置**: `feature/chat` 模块，围绕 `AiSessionRepository` 扩展

**建议模型**:
```kotlin
data class SessionMetadata(
    val sessionId: String,
    // 现有字段
    val title: String,
    val lastMessagePreview: String,
    val updatedAtMillis: Long,
    val pinned: Boolean,
    // 新增结构化字段
    val mainContactName: String?,
    val companyName: String?,
    val scenario: String?,  // 如 "报价跟进"、"会议纪要"
    val tags: List<String>,
    val isTranscription: Boolean,
    val transcriptionJobId: String?,  // 如果是转写会话
    val quickSkillIds: Set<QuickSkillId>,  // 使用的快捷技能
    // 扩展字段（未来）
    val customFields: Map<String, String>
)
```

**建议接口**:
```kotlin
interface SessionMetadataRepository {
    val summaries: Flow<List<SessionMetadata>>
    suspend fun upsert(metadata: SessionMetadata)
    suspend fun findById(sessionId: String): SessionMetadata?
    suspend fun updateField(sessionId: String, field: String, value: Any)
    suspend fun queryByContact(contactName: String): List<SessionMetadata>
    suspend fun queryByScenario(scenario: String): List<SessionMetadata>
    suspend fun queryByTag(tag: String): List<SessionMetadata>
}
```

**迁移策略**:
- 保持 `AiSessionEntity` 向后兼容（添加新字段，默认值）
- 或创建新的 `SessionMetadataEntity`，通过 `sessionId` 关联

---

### 5.2 转写元数据的归属

**推荐位置**: `data/ai-core` 模块，新增 `TranscriptionMetadataRepository`

**建议模型**:
```kotlin
data class TranscriptionMetadata(
    val jobId: String,
    val sessionId: String,
    val fileName: String,
    val speakerLabels: Map<String, String>,  // speakerId -> 显示名称
    val speakerRoles: Map<String, SpeakerRole>,  // speakerId -> 角色（客户/销售/其他）
    val diarizedSegments: List<DiarizedSegmentMetadata>,
    val smartSummary: String?,
    val createdAtMillis: Long,
    val completedAtMillis: Long?
)

data class DiarizedSegmentMetadata(
    val segmentId: String,
    val speakerId: String,
    val speakerIndex: Int,
    val startSeconds: Double,
    val endSeconds: Double,
    val text: String
)
```

**建议接口**:
```kotlin
interface TranscriptionMetadataRepository {
    suspend fun save(metadata: TranscriptionMetadata)
    suspend fun findByJobId(jobId: String): TranscriptionMetadata?
    suspend fun findBySessionId(sessionId: String): List<TranscriptionMetadata>
    suspend fun querySegmentsBySpeaker(speakerId: String): List<DiarizedSegmentMetadata>
}
```

**与会话元数据关联**:
- `TranscriptionMetadata.sessionId` → `SessionMetadata.sessionId`
- 转写完成后，更新 `SessionMetadata.isTranscription = true`, `transcriptionJobId = jobId`

---

### 5.3 消息级元数据的归属

**推荐位置**: `feature/chat` 模块，扩展 `ChatMessageEntity`

**建议字段扩展**:
```kotlin
data class ChatMessageEntity(
    // 现有字段
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val timestampMillis: Long,
    // 新增元数据字段
    val messageType: MessageType,  // NORMAL / TRANSCRIPT / SYSTEM
    val sourceJobId: String?,  // 如果是转写消息
    val quickSkillId: QuickSkillId?,  // 如果是快捷技能触发
    val attachments: List<AttachmentMetadata>?,
    val extractedMetadata: Map<String, String>?  // LLM 提取的结构化信息
)
```

**迁移策略**:
- 添加新字段，默认值（向后兼容）
- 或创建新的 `ChatMessageMetadataEntity`，通过 `messageId` 关联

---

### 5.4 设备文件元数据的归属

**推荐位置**: `feature/media` 模块，新增 `DeviceFileMetadataRepository`

**建议模型**:
```kotlin
data class DeviceFileMetadata(
    val fileId: String,
    val fileName: String,
    val associatedSessionId: String?,  // 关联的会话
    val uploadedBy: String?,  // 用户 ID
    val tags: List<String>,
    val description: String?,
    val isApplied: Boolean,
    val appliedAtMillis: Long?,
    val createdAtMillis: Long
)
```

**建议接口**:
```kotlin
interface DeviceFileMetadataRepository {
    suspend fun save(metadata: DeviceFileMetadata)
    suspend fun findByFileId(fileId: String): DeviceFileMetadata?
    suspend fun findBySessionId(sessionId: String): List<DeviceFileMetadata>
    suspend fun queryByTag(tag: String): List<DeviceFileMetadata>
}
```

---

### 5.5 子系统读写接口

**聊天/智能分析子系统**:
- **写入**: 创建会话时设置 `mainContactName`, `scenario`（从 LLM 回复解析或用户输入）
- **读取**: 查询会话列表时使用元数据过滤/排序
- **写入**: 智能分析完成后，解析结果提取 `tags`（如 "客户异议"、"价格敏感"）

**转写/Tingwu 子系统**:
- **写入**: 转写完成后，保存 `TranscriptionMetadata`（说话人标签、分段）
- **读取**: 生成 markdown 时使用持久化的说话人标签
- **写入**: 更新 `SessionMetadata.isTranscription = true`, `transcriptionJobId = jobId`

**设备管理器子系统**:
- **写入**: 文件上传/应用时，更新 `DeviceFileMetadata`
- **读取**: 显示文件列表时，显示关联的会话信息
- **写入**: 文件用于转写时，关联 `associatedSessionId`

**导出子系统**:
- **读取**: 生成导出文件名时，使用 `SessionMetadata.mainContactName`, `scenario`
- **读取**: 导出内容时，使用 `TranscriptionMetadata.speakerLabels` 格式化说话人

**用户中心/历史 UI**:
- **读取**: 查询会话列表时，使用 `SessionMetadata.tags`, `mainContactName`, `scenario` 过滤
- **读取**: 显示会话详情时，使用 `TranscriptionMetadata` 显示说话人信息

---

### 5.6 合理边界

**元数据 vs 原始内容**:
- **元数据**: 客户名、公司名、场景、标签、说话人标签、分段时间戳
- **原始内容**: 完整消息文本、完整转写 markdown、完整分析结果
- **原则**: 元数据应该是**可查询、可过滤、可聚合**的结构化信息；原始内容用于**展示和上下文**

**ViewModel vs 元数据服务**:
- **ViewModel**: 负责 UI 状态管理、用户交互处理
- **元数据服务**: 负责元数据的**生成、存储、查询**
- **原则**: ViewModel 调用元数据服务，而不是直接操作数据库

**集中式 vs 分布式**:
- **集中式**: 所有元数据通过统一的 `MetadataHub` 接口访问
- **分布式**: 各子系统维护自己的元数据仓库，但遵循统一的元数据模型
- **推荐**: **混合模式** - 各子系统维护自己的仓库，但通过 `MetadataHub` 提供统一查询接口

---

### 5.7 元数据生成时机

**会话创建时**:
- 用户发送首条消息 → 创建 `SessionMetadata`（初始值）
- LLM 首条回复 → 解析 `【客户】`、`【场景】`，更新 `mainContactName`, `scenario`

**转写完成时**:
- Tingwu 返回结果 → 生成 `TranscriptionMetadata`（说话人标签、分段）
- 更新 `SessionMetadata.isTranscription = true`, `transcriptionJobId = jobId`

**智能分析完成时**:
- LLM 返回分析结果 → 解析结构化信息（洞察、行动项），提取 `tags`
- 更新 `SessionMetadata.tags`, `quickSkillIds`

**导出时**:
- 使用 `SessionMetadata.mainContactName`, `scenario` 生成文件名
- 使用 `TranscriptionMetadata.speakerLabels` 格式化转写内容

---

## 6. 总结

### 6.1 当前状态

- **会话级元数据**: 部分存在（title, preview），但客户名、场景、标签**未结构化存储**
- **转写元数据**: 说话人标签、分段**未持久化**，仅在内存中
- **消息级元数据**: 基本字段存在，但消息类型、快捷技能 ID、附件**未存储**
- **设备文件元数据**: 基本字段存在，但与会话关联、标签**未存储**
- **导出元数据**: 文件名生成逻辑复杂，依赖多个数据源

### 6.2 主要问题

1. **元数据重复**: 信息编码在字符串中（标题、markdown），无法直接查询
2. **逻辑分散**: 标题生成、说话人标签生成、文件名生成逻辑分散在多个地方
3. **未持久化**: 说话人标签、转写分段、智能分析结果仅在内存中
4. **未解析**: LLM 输出的结构化信息（客户名、场景）未解析回元数据
5. **未关联**: 设备文件、转写任务与会话未建立持久化关联

### 6.3 设计建议

1. **创建 `SessionMetadata` 模型**: 扩展 `AiSessionEntity`，添加 `mainContactName`, `companyName`, `scenario`, `tags` 等字段
2. **创建 `TranscriptionMetadataRepository`**: 持久化说话人标签、分段、转写任务关联
3. **扩展 `ChatMessageEntity`**: 添加 `messageType`, `sourceJobId`, `quickSkillId` 等字段
4. **创建 `MetadataHub` 接口**: 提供统一的元数据查询接口，但各子系统维护自己的仓库
5. **解析 LLM 输出**: 从首条回复中解析 `【客户】`、`【场景】`，更新元数据
6. **持久化说话人标签**: 转写完成后，保存 `speakerLabels` 到 `TranscriptionMetadata`

---

**报告结束**

