# SmartSales Android Project - Onboarding Guide

欢迎加入项目！本指南将帮助你快速上手。

## 📋 前置要求

### 必需软件
- **JDK 17** (必须，项目已固定版本)
- **Android Studio** (Hedgehog 2023.1.1 或更新版本)
- **Git** (已配置 SSH 密钥访问 GitHub)
- **adb** (用于设备测试，Android SDK 自带)

### 验证环境
```bash
# 检查 JDK 版本
java -version  # 应该显示 17.x.x

# 检查 Android SDK
echo $ANDROID_HOME  # 应该指向 Android SDK 路径

# 检查 adb
adb version  # 应该显示 adb 版本
```

---

## 🚀 第一步：克隆并探索项目（15 分钟）

### 1.1 克隆仓库
```bash
git clone <repository-url>
cd main_app
```

### 1.2 查看项目结构
```bash
# 查看主要模块
ls -la

# 查看关键文件
cat README.md
cat AGENTS.md
```

**关键目录说明**：
- `app/` - 主应用入口（Compose shell）
- `feature/` - 功能模块（chat, media, connectivity, usercenter）
- `data/ai-core/` - AI 核心服务（DashScope, Tingwu, OSS）
- `core/` - 共享工具（util, test）
- `docs/` - **重要！** 项目文档
- `plans/` - 开发计划
- `workflows/` - 工作流程

**主要功能模块**：
- `feature/chat` - 聊天功能（Home 屏幕、ChatHistory、快速技能）
- `feature/media` - 媒体功能（AudioFiles、DeviceManager、播放控制）
- `feature/connectivity` - 设备连接（BLE/WiFi 配网、DeviceSetup 步骤化界面）
- `feature/usercenter` - 用户中心（用户资料、设置）

---

## ⚙️ 第二步：配置项目（20-30 分钟）

### 2.1 配置 JDK 路径（如果需要）

如果 `gradle.properties` 中的 JDK 路径与你的不同：

```bash
# 编辑 gradle.properties
# 修改这一行指向你的 JDK 17 路径：
org.gradle.java.home=/path/to/your/jdk-17
```

### 2.2 创建 local.properties 文件

**重要：API keys 配置**

在项目根目录创建 `local.properties`（如果不存在）：

```bash
# 复制模板（如果存在）
cp local.properties.example local.properties

# 或者手动创建
touch local.properties
```

**必需配置项**：

```properties
# Android SDK 路径（必需）
sdk.dir=/path/to/Android/Sdk

# DashScope API Key（AI 聊天功能）
DASHSCOPE_API_KEY=your_dashscope_key_here
DASHSCOPE_MODEL=qwen-plus

# Tingwu API Key（音频转写功能）
TINGWU_APP_KEY=your_tingwu_app_key
TINGWU_BASE_URL=https://tingwu.cn-beijing.aliyuncs.com
ALIBABA_CLOUD_ACCESS_KEY_ID=your_access_key_id
ALIBABA_CLOUD_ACCESS_KEY_SECRET=your_access_key_secret

# OSS 配置（媒体同步功能）
OSS_ACCESS_KEY_ID=your_oss_key_id
OSS_ACCESS_KEY_SECRET=your_oss_secret
OSS_BUCKET_NAME=your_bucket_name
OSS_ENDPOINT=https://oss-cn-beijing.aliyuncs.com
```

**⚠️ 安全提示**：
- `local.properties` 已在 `.gitignore` 中，不会被提交
- **永远不要**提交 API keys 到 Git
- 如果没有 keys，部分功能会使用 Fake 实现（可正常运行但无真实 AI 响应）

### 2.3 首次构建验证

```bash
# 清理并同步 Gradle
./gradlew clean

# 尝试构建（首次会下载依赖，可能需要 5-10 分钟）
./gradlew :app:assembleDebug

# 如果成功，你会看到：
# BUILD SUCCESSFUL
```

**如果构建失败**：
- 检查网络连接（首次需要下载依赖）
- 检查 JDK 17 路径配置
- 查看错误信息，常见问题见下方"故障排除"

---

## 📚 第三步：阅读关键文档（30-45 分钟）

**按以下顺序阅读**：

### 3.1 必须阅读（核心理解）
1. **`docs/role-contract.md`** ⭐⭐⭐
   - 理解多代理工作流程（Operator/Orchestrator/Codex）
   - 了解代码提交规范

2. **`README.md`** ⭐⭐⭐
   - 项目概述和模块说明
   - 构建命令
   - Tingwu base URL 配置说明

3. **`docs/current-state.md`** ⭐⭐⭐
   - 当前系统状态
   - 模块成熟度（T0-T3）
   - 已知风险和限制

4. **`AGENTS.md`** ⭐⭐
   - 代码风格规范
   - 提交格式要求

### 3.2 参考阅读（按需）
5. **`docs/progress-log.md`** - 项目历史变更
6. **`api-contracts.md`** - API 契约文档
7. **`plans/dev_plan.md`** - 开发计划（了解未来方向）

---

## 🏗️ 第四步：理解项目架构（30 分钟）

### 4.1 模块依赖关系

```
:app (主入口)
  ├── :feature:chat (聊天功能)
  │     └── :data:ai-core (AI 核心)
  ├── :feature:media (媒体功能)
  │     └── :data:ai-core
  ├── :feature:connectivity (设备连接)
  │     └── :core:util
  ├── :feature:usercenter (用户中心)
  │     └── :core:util
  └── :core:util (共享工具)
```

### 4.2 技术栈概览

- **UI**: Jetpack Compose + Material 3
- **DI**: Hilt (Dagger)
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **异步**: Kotlin Coroutines + Flow
- **测试**: JUnit 4 + kotlinx-coroutines-test

### 4.3 关键概念

1. **Fake vs Real 实现**：
   - 模块提供 Fake（测试用）和 Real（生产用）两种实现
   - 通过 `AiCoreConfig` 切换
   - 没有 API keys 时自动使用 Fake

2. **T0-T3 层级**：
   - T0: 基础骨架
   - T1: 功能完整 + 测试覆盖
   - T2: 生产就绪
   - T3: 优化完成

---

## 🧪 第五步：运行和测试（20 分钟）

### 5.1 运行应用

```bash
# 连接到设备或启动模拟器
adb devices

# 安装并运行
./gradlew :app:installDebug
adb shell am start com.smartsales.aitest/.AiFeatureTestActivity
```

### 5.2 运行测试

```bash
# 单元测试
./gradlew testDebugUnitTest

# 运行特定模块的测试
./gradlew :feature:connectivity:testDebugUnitTest
./gradlew :feature:chat:testDebugUnitTest
./gradlew :feature:media:testDebugUnitTest

# Android UI 测试（需要设备）
./gradlew :app:connectedDebugAndroidTest

# 导航 Compose UI 冒烟测试
./gradlew :app:assembleDebug :app:connectedDebugAndroidTest
```

### 5.3 查看日志

```bash
# 过滤项目日志
adb logcat | grep -E "SmartSales|AiFeatureTest"

# 或使用标签过滤
adb logcat tag:SmartSalesChat:* *:S
```

---

## 🎯 第六步：开始你的第一个任务

### 6.1 了解工作流程

根据 `docs/role-contract.md`：
1. **Operator** (你) - 执行实际代码工作
2. **Orchestrator** - 创建规范和任务
3. **Codex** - 实现代码（在明确指导下）

### 6.2 推荐的第一个任务

**建议从以下开始**：

1. **修复一个小 bug** 或
2. **添加一个简单的 UI 测试**（参考 `NavigationSmokeTest.kt`）或
3. **完善某个模块的文档** 或
4. **为现有功能添加单元测试**（当前测试覆盖率约 21.5%，目标是 60-80%）

### 6.3 提交代码前检查清单

- [ ] 代码遵循 `AGENTS.md` 中的风格规范
- [ ] 所有文件顶部有中文文件头
- [ ] 通过了 lint 检查：`./gradlew lint`
- [ ] 相关测试通过
- [ ] 提交信息符合规范（见下方）

**提交信息格式**（中文）：
```
功能(模块)：简要描述

可选的详细说明。

Test: 测试命令或说明
```

示例：
```
功能(聊天)：在 Home 聊天中添加滚动到最新消息按钮

在 Home 聊天屏幕中实现滚动按钮，带有流畅动画。
Test: ./gradlew :app:connectedDebugAndroidTest

功能：添加 ChatHistory 屏幕，集成数据库和导航 shell

实现聊天历史屏幕，支持查看历史会话。
Test: ./gradlew :feature:chat:testDebugUnitTest
```

---

## ❓ 常见问题（故障排除）

### Q1: 构建失败 - "Plugin not found"
```bash
# 解决方案：刷新依赖
./gradlew --refresh-dependencies
```

### Q2: 构建失败 - JDK 版本错误
```bash
# 检查并设置正确的 JDK
export JAVA_HOME=/path/to/jdk-17
# 或修改 gradle.properties 中的 org.gradle.java.home
```

### Q3: 依赖下载慢或失败
- 项目已配置阿里云镜像，应该较快
- 如果仍慢，检查网络连接
- 本地镜像在 `third_party/maven-repo/`，大部分依赖已缓存

### Q4: API keys 缺失怎么办？
- 应用仍可运行，但使用 Fake 实现
- AI 聊天会返回模拟响应
- 不影响大部分开发和测试工作

### Q5: 如何查看某个功能如何工作？
1. 查看 `docs/current-state.md` 了解模块状态
2. 查看相关模块的 README（如 `feature/chat/README.md`）
3. 阅读代码中的注释和文件头
4. 查看测试代码了解预期行为
5. 查看 `docs/progress-log.md` 了解最近的变更

### Q6: 新增了哪些主要功能？
根据最新更新，项目已包含：
- **ChatHistory 屏幕** - 聊天历史记录查看
- **AudioFiles 屏幕** - 音频文件管理（已迁移到 `feature/media`）
- **DeviceSetup 步骤化界面** - 设备连接配网流程
- **UserCenter 模块** - 用户中心和用户资料
- **导航 Compose UI 冒烟测试** - 验证主要导航路径

---

## 📞 获取帮助

### 文档资源
- 项目文档：`docs/` 目录
- API 契约：`api-contracts.md`
- 开发计划：`plans/dev_plan.md`

### 代码探索技巧
```bash
# 搜索特定功能
grep -r "keyword" --include="*.kt"

# 查找测试文件
find . -name "*Test.kt"

# 查看模块依赖
./gradlew :app:dependencies
```

---

## ✅ 完成清单

完成以下任务后，你已准备好开始工作：

- [ ] 成功构建项目 (`./gradlew :app:assembleDebug`)
- [ ] 应用在设备/模拟器上运行
- [ ] 单元测试通过 (`./gradlew testDebugUnitTest`)
- [ ] UI 测试通过（如可能，`./gradlew :app:connectedDebugAndroidTest`）
- [ ] 阅读了 `docs/role-contract.md`
- [ ] 阅读了 `docs/current-state.md`
- [ ] 理解了模块结构（chat, media, connectivity, usercenter）
- [ ] 配置了 `local.properties`（或了解如何配置）
- [ ] 了解了项目当前成熟度（T0-T1 阶段）

---

## 🎓 下一步学习

完成基础设置后，建议：

1. **深入理解一个模块**：
   - 选择 `:feature:chat` 或 `:feature:connectivity`
   - 阅读代码、测试、README
   - 运行并调试

2. **参与代码审查**：
   - 查看最近的 PR/commits
   - 理解代码变更的原因

3. **开始小任务**：
   - 从简单的 bug fix 或测试开始
   - 逐步熟悉代码库

---

**祝工作顺利！如有问题，查看文档或询问团队成员。**

