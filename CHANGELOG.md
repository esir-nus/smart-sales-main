# 更新日志

## 最近更新

### 2026-04-25

- **[新增] 音频卡片显示来源徽章标签** — 录音卡片右下角新增来源徽章显示名称（如"Badge 1"），取代原有云图标；来源徽章已移除注册时退化为 MAC 后缀（如"...EE:FF"）；手机录音保持原有手机图标不变。
- **[修复] 手动同步网络片段过滤** — WiFi 状态解析现在只接受 `IP#...` 与 WiFi 名称 `SD#<SSID>` 片段，忽略交错到 BLE 通知流中的 `SD#space#...`、`Bat#...` 与 `Ver#...`，并在 HTTP 预检失败后刷新端点重试，减少手动同步误判。
- **[新增] 徽章任务完成信号改为 Command#end** — 音频管线在 `log#` / `rec#` 处理完成后通过 `ConnectivityBridge` 与 BLE 写入链路发送 `Command#end`，替代旧的 Android-only `commandend#1` 接线。
- **[新增] 徽章 SD 卡空间查询与用户中心展示** — 接入 `SD#space` 查询/回复链路，`ConnectivityViewModel` 暴露存储空间状态，用户中心新增 SD 卡容量行、32GB 比例动画条与点击刷新，并保留固件返回的原始容量文本。
- **[修复] 新增设备注册流程可完成与可退出** — post-onboarding 添加新徽章流程在成功配网后直接关闭回到连接管理，不再要求日程 quick-start；新增配对 overlay 现在显示 `关闭` 并支持 Android back 退出，同时退出前取消配对任务。
- **[修复] 双徽章切换生命周期稳定化** — 真实设备验证 A→B / B→A 切换链路，修复切换时复用旧会话/GATT 的问题，强制按目标徽章 session 重连，并在新增设备扫描中隐藏已注册徽章。
- **[修复] 对话气泡长按复制恢复** — 调整 `CopyableBubble` 的长按处理，修复气泡位于 `LazyColumn` 中时复制手势失效的问题。
- **[新增] SIM 用户中心徽章语音音量控制** — `SimUserCenterDrawer` 接入徽章语音音量滑杆，补齐 SIM 用户中心内的设备控制入口。
- **[维护] 固件 sprint 文档与本地合同忽略规则收口** — 完成 Sprint 06 文档 closeout 清理，并将本地 sprint contract 文档加入忽略规则，减少工作树噪声。

### 2026-04-24

- **[维护] workflow-renovation 治理收口完成** — 将剩余鸿蒙OS原生分支改动收拢到 `platform/harmony`，为历史分支打上 `archive/*-20260424` 标签，并清理已淘汰的分支与工作树引用。
- **[维护] 固件协议 intake 文档建档** — 新建 `firmware-protocol-intake` 项目与 Bat# 监听、电量 UI 两个 sprint 合同，并同步更新 `docs/specs/esp32-protocol.md` 与接口图，补齐 Bat#、volume#、Ver# 协议文档基线。
- **[新增] 徽章电量推送实时接入 (Bat#)** — 新增 `ConnectivityBridge.batteryNotifications(): Flow<Int>` 并接入 `ConnectivityViewModel.batteryLevel`，替换硬编码占位值；`Bat#<0..100>` 通知现在驱动真实电量读数。
- **[新增] 电量 UI 区分"未收到"与真实读数** — `batteryLevel` 升为 `StateFlow<Int?>`，初始值 `null`；连接面板、历史抽屉和动态岛在首次推送前显示 `--%`，动态岛侧边栏电量芯片隐藏直至收到读数。
- **[修复] 徽章录音文件名 .wav 后缀归一化** — 修复 `log#`/`rec#` 载荷含 `.wav` 后缀时遗漏磁盘前缀的问题；新旧固件两种格式均归一化为 `log_<ts>.wav` / `rec_<ts>.wav`，消除下载 URL 404。
- **[新增] 徽章固件版本查询 (Ver#)** — 实现 `Ver#get` 查询/回复链路；连接建立时自动查询，用户中心支持手动刷新；版本号以 `Ver. <project>.<major>.<minor>.<feature>` 形式展示，未收到响应时显示 `Ver. —`。
- **[维护] 跟踪器目录迁移与文档治理收口** — 将 `docs/plans/` 下的跟踪器迁移至各项目文件夹；完成 develop-protocol-migration 治理收缩、分支墓地清扫及 DTQ-03 遗留文档移交；刷新仪表板与发布检查指引。

### 2026-04-23

- **[维护] Android 版本管理与调度路由治理** — 精简 `app-core/build.gradle.kts` 与 `app/build.gradle.kts`，移除 `AppFlavor` SIM/Full 分支，新增 `ModelRegistry` 与平台治理 CI 检查（`platform-governance-check.yml`）。
- **[修复] ASR 服务单元测试隔离** — 将 `FunAsrService` 转录逻辑提取为可注入的 `FunAsrTranscriptionClient`，单元测试彻底脱离真实网络和 DashScope 凭据。
- **[维护] 发布门控对齐与协议规范体系初始化** — 对齐 pre-renovation 发布门控规则；完成 develop-protocol-migration 规范 bootstrap（接口图、合约模板、分支策略文档）。

### 2026-04-22

- **[新增] 日程管线前台服务与编排层** — 新增 `SchedulerPipelineForegroundService`、`SchedulerPipelineOrchestrator`、`SchedulerPipelineNotifications` 与 `SchedulerPipelineOutcomeStore`，将徽章音频处理链路接入系统前台保活；重构 `RealBadgeAudioPipeline` 初始落地版本。
- **[重构] 徽章音频管线自驱动架构** — `RealBadgeAudioPipeline` 改为 init 协程监听连接通知自驱；移除 `runStages()` / `BadgeAudioPipelineRunOutcome`，以 `processFile()` + `SharedFlow<PipelineEvent>` 替代，前台服务改为收集事件流而非主动调用。
- **[修复] 灵感管线防护与日程 Linter 加固** — 修复 `RealInspirationRepository` 边界处理，加固 `RealUniCExtractionService` 与 `SchedulerLinter` / `FastTrackMutationEngine` 解析稳定性，补充单元测试覆盖。
- **[修复] 徽章端点连接恢复链路** — 新增 `BadgeEndpointRecoveryCoordinator` 与 `IsolationProbeHelper`，重构 `RealConnectivityBridge` 和 `DeviceConnectionManagerConnectionSupport` 断连恢复路径。
- **[新增] 智能对话气泡长按复制** — 新增 `CopyableBubble` 组件与 `ClipboardHelper`，用户气泡和响应气泡均支持长按复制对话内容。
- **[重构] 日程时间线数据流优化** — 合并日程写入 DAO 路径、移除手动刷新触发，并让划线完成项直接进入时间线投影，减少抽屉状态漂移并补齐异步 telemetry 与结构测试覆盖。
- **[修复] 下载前台服务恢复 Lint-safe 生命周期接线** — 补回 `DownloadForegroundService.onStartCommand()` 的 `super` 调用，恢复 Android 安装与 CI Lint 链路稳定性。

### 2026-04-21

- **[新增] 鸿蒙OS 日程 Phase 2C 持久化流程** — 鸿蒙OS 平台 lane 补齐原生日程仓储、任务持久化、重排期与冲突处理流程，并同步更新入口与 hvigor 配置。
- **[维护] 鸿蒙OS 音频与 Tingwu 存储链路补强** — 补强鸿蒙OS文件存储、OSS、Tingwu 服务与运行时配置接线，同时加入 lane proof 脚本和签名台账更新，便于继续独立推进 `platform/harmony`。
- **[新增] 徽章任务创建控制信号** — 日程创建与提醒链路现在会向徽章发送一次 best-effort 控制信号，补齐设备管理、管线契约和回归测试覆盖。
- **[修复] 日程提取与时间边界加固** — 加强日程时间 seam、边界判定和提取测试，降低时间解释偏移导致的日程状态漂移风险。

### 2026-04-20

- **[修复] 日程提交后保留徽章 WAV 文件** — 移除日程提交完成后的徽章端录音删除动作，避免后续下载命中已不存在文件并触发 HTTP 403。
- **[维护] Debug 版本号写入分支与提交信息** — Debug 构建产物现在会带上当前 Git branch 与 commit 标识，便于现场排查安装包来源。

### 2026-04-16

- **[新增] 鸿蒙OS 原生应用骨架落地** — 完成鸿蒙OS原生应用根结构脚手架，为后续独立容器与功能迁移提供落点。
- **[修复] 鸿蒙OS Phase 2A 音频链路类型安全** — 修复 Phase 2A 音频管线中的类型安全问题，减少鸿蒙OS端编译与接线错误。
- **[修复] 徽章下载前台保活恢复** — 恢复徽章录音下载期间的前台保活链路，降低后台阶段被系统中断的风险。
- **[修复] 下载通知前台通道恢复** — 补回下载前台服务所需的通知通道接线，确保下载态提示与服务启动一致。
- **[修复] 音频下载服务恢复 Lint-safe 接线** — 调整下载服务 wiring，使音频下载链路重新满足构建检查并保持运行时行为稳定。
- **[修复] 转录音频抽屉卡片细节优化** — 调整转录音频抽屉卡片的展示细节，提升信息层次与可读性。

### 2026-04-15

- **[维护] Android CI 私有 SDK 依赖补齐** — 将 NUI SDK AAR 与 DashScope 相关 Maven 元数据补入 `third_party/maven-repo`，并调整 `.gitignore` 以允许跟踪 CI 必需的私有构件。
- **[维护] develop 发布前构建链路修复** — 补齐 `TINGWU_BASE_URL` 默认值、移除失效的本地 AAR 引用并改回 Maven 坐标，恢复 Lint 与单元测试在 CI 上的可解析性。
- **[修复] SIM 音频会话补齐上下文历史标记** — 新建音频讨论会话与补挂音频记录时会正确写入 `hasAudioContextHistory`，音频上下文标识不再被隐藏。
- **[修复] SIM 动态岛标题轮播恢复** — 恢复会话标题在动态岛中的高亮注入、`3s` 停留时长，以及重命名后的立即打断轮播能力。
- **[新增] SIM 新增设备引导入口** — SIM 首次引导补充独立的新增设备入口路径，便于在已完成初始设置后继续接入新徽章。
- **[新增] Agent 智能对话界面润色** — 优化智能对话气泡、流式输出与输入区表现，整体聊天体验更接近正式会话界面。
- **[修复] 徽章下载进度条实时更新** — 修复 RealConnectivityBridge 未透传 `onProgress` 的问题，徽章录音下载进度不再停留在 0%。
- **[维护] develop 分支治理切换完成** — 仓库完成从 DTQ 车道系统切换到 `master` / `develop` / `platform/harmony` 分支模型，日常协作路径更清晰。
- **[维护] 清理残留 DTQ 仓库工件** — 移除旧 handoff 目录、lane 检查脚本与过期 CI 校验，活跃仓库仅保留 `docs/archive/dtq-era/` 历史归档。
- **[维护] 发布与更新日志工具链补齐** — 补齐 `/ship`、`/merge`、`/changelog` 协作流程，并新增 CHANGELOG 生成脚本用于导出 HTML 版本。

### 2026-04-14

- **[新增] 徽章重连时的动态岛呼吸提示** — 徽章在重新连接时，动态岛会显示Chrome绿色呼吸动画，让用户清楚地看到连接恢复的过程。
- **[新增] 国产手机蓝牙扫描兼容** — 针对小米、华为、OPPO、Vivo等国产机型，蓝牙配对扫描现在能正确处理定位权限，确保配对成功率。
- **[新增] 配对扫描设备过滤** — 配对时的设备扫描现在支持按设备名称关键词和Service UUID过滤，只显示匹配目标profile的设备，减少干扰。
- **[新增] 徽章连接自动恢复** — 徽章意外断连时会自动启动恢复流程，无需用户手动干预，连接恢复更稳定。
- **[新增] 蓝牙链路健康监测** — 添加了GATT心跳检测机制，能更快发现并处理异常断连，并优化了BLE扫描的设备识别。
- **[新增] Shell自动重连流程** — RuntimeShell在检测到连接断开时会自动重新连接，用户体验更连贯。
- **[新增] 音频抽屉界面精简** — 优化了SIM音频抽屉的布局和同步状态管理，界面更清爽。
- **[发布] 车道分支发布工具** — 上线了/ship和/changelog两个新的Claude Code功能，支持多分支并行开发和版本管理。

### 2026-04-13

- **[发布] 多车道隔离开发体系** — 将代码仓库拆分为6条独立的开发车道，每条车道独立管理，支持多个功能并行开发。
- **[新增] 项目自动化测试** — 新增GitHub Actions工作流，每次代码提交都会自动运行单元测试和代码检查。
- **[修复] 蓝牙连接兼容性优化** — 移除了不必要的网络限制，修复了并发监听的问题，蓝牙握手更可靠。

### 2026-04-09

- **[新增] 徽章录音推送和自动同步** — 徽章现在支持接收录音推送，重新连接后会自动下载同步，动态岛新增同步状态指示。

### 2026-04-04

- **[发布] 徽章后台无声重连** — 徽章在连接中断后会自动在后台重新连接，用户无需手动操作，也不会弹出重连提示。
- **[发布] 鸿蒙OS平台音频处理基础设施** — 为鸿蒙OS平台发布了音频处理的隔离容器架构。

### 2026-04-02

- **[新增] SIM模式动态岛连接状态指示** — SIM Shell现在在动态岛实时显示徽章的连接状态。
- **[新增] SIM模式自动会话命名** — SIM模式下的会话现在会根据内容自动重命名，与标准模式保持一致。
- **[修复] 优化徽章同步期间的WiFi查询** — 抑制了重复的ESP32 WiFi查询，减少BLE通道干扰。
- **[修复] 日程表抽屉手势响应** — 修正了SIM和标准模式下的向右滑动关闭手势映射。

### 2026-03-28

- **[新增] 完整的应用内首次启动引导流程** — 实现了交互式的首次启动流程，包括麦克风权限申请、语音转录确认和AI驱动的欢迎提示。
- **[修复] 实时语音识别服务认证修复** — 修正了FunASR实时认证流程，改为直接调用DashScope API密钥。

### 2026-03-25

- **[修复] 徽章发现设备过滤优化** — 优化了徽章设备发现机制，现在仅显示来自可信CHLE设备族的产品，阻止无关的蓝牙设备干扰。

### 2026-03-22

- **[新增] SIM壳层界面完善** — 完成了SIM模式下的日程表抽屉、用户中心抽屉、历史记录抽屉和空白主页的界面优化和交付。
- **[新增] Tingwu转录中的人物识别** — 完成了关键人物识别和信息提取功能在Tingwu转录中的集成。
