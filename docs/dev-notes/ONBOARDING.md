# SmartSales Android Project - Onboarding Guide

欢迎加入项目！本指南将帮助你快速上手。

## 📋 前置要求

### 必需软件
- **JDK 17** (必须，项目已固定版本)
- **Android Studio** (Hedgehog 2023.1.1 或更新版本)
- **Git** (已配置 SSH 密钥访问 GitHub)
- **adb** (用于设备测试，Android SDK 自带)

### 操作系统支持

本指南支持 **Linux**、**macOS** 和 **Windows** 操作系统。

#### macOS 用户特别说明

**命令行工具**：
- macOS 默认使用 **Terminal**（bash/zsh）
- 推荐使用 **Homebrew** 管理软件包（JDK、Git 等）
- 本指南中的命令在 macOS Terminal 中可直接运行

**macOS 路径配置**：
```bash
# 在 ~/.zshrc 或 ~/.bash_profile 中添加环境变量（永久）
export ANDROID_HOME="$HOME/Library/Android/sdk"
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$ANDROID_HOME/platform-tools:$PATH"

# 使配置生效
source ~/.zshrc  # 如果使用 zsh
# 或
source ~/.bash_profile  # 如果使用 bash
```

**macOS 安装 JDK 17**：
```bash
# 使用 Homebrew 安装
brew install openjdk@17

# 链接到系统路径
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# 验证安装
java -version
```

**macOS 安装 Android SDK**：
- 通过 **Android Studio** 安装（推荐）
- 路径通常为：`~/Library/Android/sdk`
- 确保在 Android Studio 中安装了 Android SDK Platform-Tools

**macOS 权限设置**：
- 首次运行 `adb` 可能需要授予终端完全磁盘访问权限
- 系统设置 → 隐私与安全性 → 完全磁盘访问权限 → 添加 Terminal

**macOS 特定注意事项**：
- 如果遇到 "Permission denied" 错误，可能需要：`chmod +x gradlew`
- M1/M2 Mac 用户：确保使用 ARM64 版本的 JDK 和 Android SDK
- 如果使用 Rosetta 2，某些工具可能需要额外配置

#### Windows 用户特别说明

**命令行工具选择**：
- 推荐使用 **Git Bash**（随 Git 安装）或 **PowerShell**
- 本指南中的 `bash` 命令在 Git Bash 中可直接运行
- 在 PowerShell 中，部分命令语法可能需要调整（例如：`$env:ANDROID_HOME` 而不是 `$ANDROID_HOME`）

**Windows 路径配置**：
```powershell
# PowerShell 中设置环境变量（临时）
$env:ANDROID_HOME = "C:\Users\YourName\AppData\Local\Android\Sdk"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# 或使用 Git Bash（语法与 Linux 相同）
export ANDROID_HOME="/c/Users/YourName/AppData/Local/Android/Sdk"
export JAVA_HOME="/c/Program Files/Java/jdk-17"
```

**Windows 路径格式**：
- Windows 路径：`C:\Users\Name\AppData\Local\Android\Sdk`
- Git Bash 路径：`/c/Users/Name/AppData/Local/Android/Sdk`
- `gradle.properties` 中使用 Windows 路径：`C:\\Users\\Name\\...`（双反斜杠）

**adb 命令**：
- Windows 中 adb 位于：`%ANDROID_HOME%\platform-tools\adb.exe`
- 确保 `%ANDROID_HOME%\platform-tools` 已添加到 PATH 环境变量

### 验证环境

**Linux/macOS**:
```bash
# 检查 JDK 版本
java -version  # 应该显示 17.x.x

# 检查 Android SDK
echo $ANDROID_HOME  # 应该指向 Android SDK 路径

# 检查 adb
adb version  # 应该显示 adb 版本
```

**Windows (Git Bash)**:
```bash
# 检查 JDK 版本
java -version  # 应该显示 17.x.x

# 检查 Android SDK
echo $ANDROID_HOME  # 应该指向 Android SDK 路径

# 检查 adb
adb version  # 应该显示 adb 版本
```

**Windows (PowerShell)**:
```powershell
# 检查 JDK 版本
java -version  # 应该显示 17.x.x

# 检查 Android SDK
$env:ANDROID_HOME  # 应该指向 Android SDK 路径

# 检查 adb
adb version  # 应该显示 adb 版本
```

---

## 🚀 第一步：克隆并探索项目（15 分钟）

### 1.1 克隆仓库
```bash
git clone <https://github.com/esir-nus/smart-sales-main.git>
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
- `app/` - 主应用入口（Legacy Compose shell - 逐步废弃中）
- `app-core/` - Prism 架构核心，新版单体主工程
- `data/ai-core/` - AI 核心服务（DashScope, Tingwu, OSS）
- `core/` - 共享工具（util, test）
- `docs/` - **重要！** 项目文档
- `plans/` - 开发计划
- `workflows/` - 工作流程

**主要功能域 (现已合并于 app-core)**：
- `chat` - 聊天与核心对话流水线
- `media` - 媒体功能（设备音频记录、播放）
- `connectivity` - 设备连接（BLE/WiFi 配网）
- `usercenter` - 用户中心

---

## ⚙️ 第二步：配置项目（20-30 分钟）

### 2.1 配置 JDK 路径（如果需要）

如果 `gradle.properties` 中的 JDK 路径与你的不同：

**Linux/macOS**:
```bash
# 编辑 gradle.properties
# 修改这一行指向你的 JDK 17 路径：
org.gradle.java.home=/path/to/your/jdk-17
```

**Windows**:
```properties
# 编辑 gradle.properties
# Windows 路径需要使用双反斜杠或正斜杠：
org.gradle.java.home=C:\\Program Files\\Java\\jdk-17
# 或
org.gradle.java.home=C:/Program Files/Java/jdk-17
```

**找到 JDK 路径的方法**：
- **Linux**: `which java` 然后找到 JDK 目录（通常是 `/usr/lib/jvm/java-17-openjdk`）
- **macOS**: 
  ```bash
  # 使用 java_home 工具查找
  /usr/libexec/java_home -v 17
  
  # 或查看已安装的 JDK
  /usr/libexec/java_home -V
  
  # Homebrew 安装的 JDK 通常在
  /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
  # 或
  /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
  ```
- **Windows**: JDK 通常安装在 `C:\Program Files\Java\jdk-17` 或通过 Android Studio 安装的位置

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

**Linux/macOS**:
```properties
# Android SDK 路径（必需）
sdk.dir=/path/to/Android/Sdk
```

**Windows**:
```properties
# Android SDK 路径（必需）
# Windows 路径可以使用正斜杠或反斜杠（双反斜杠）
sdk.dir=C:/Users/YourName/AppData/Local/Android/Sdk
# 或
sdk.dir=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

**查找 Android SDK 路径**：
- **Android Studio**: File → Settings → Appearance & Behavior → System Settings → Android SDK → Android SDK Location
- **默认位置**：
  - **Linux**: `~/Android/Sdk`
  - **macOS**: `~/Library/Android/sdk`（通过 Android Studio 安装）
  - **Windows**: `C:\Users\<Username>\AppData\Local\Android\Sdk`

**macOS 特定配置**：
- 如果使用 Homebrew 安装的 Android SDK，路径可能不同
- 推荐通过 Android Studio 安装，路径更标准
- 在 Android Studio 中：Preferences → Appearance & Behavior → System Settings → Android SDK

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

**Linux/macOS/Git Bash**:
```bash
# 清理并同步 Gradle
./gradlew clean

# 尝试构建（首次会下载依赖，可能需要 5-10 分钟）
./gradlew :app:assembleDebug

# 如果成功，你会看到：
# BUILD SUCCESSFUL
```

**Windows (PowerShell/CMD)**:
```powershell
# 清理并同步 Gradle
.\gradlew.bat clean

# 尝试构建（首次会下载依赖，可能需要 5-10 分钟）
.\gradlew.bat :app:assembleDebug

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
1. **`docs/README.md`** ⭐⭐⭐
   - 文档导航中心，了解文档结构
   - 快速找到所需文档

2. **`docs/AGENTS.md`** ⭐⭐⭐
   - AI 代理行为规范
   - 代码风格和提交规范
   - Purification Mindset（净化心态）

3. **`docs/specs/Orchestrator-V1.md`** ⭐⭐⭐
   - V1 架构规范（当前 SoT）
   - 模块契约和数据流

4. **`docs/plans/tracker.md`** ⭐⭐
   - 架构实现蓝图
   - 当前里程碑状态

### 3.2 参考阅读（按需）
5. **`docs/specs/api-contracts.md`** - API 契约文档
6. **`docs/specs/ux-contract.md`** - UX 契约文档
7. **`docs/specs/prism-ui-ux-contract.md`** - UX 状态追踪

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

**Linux/macOS/Git Bash**:
```bash
# 连接到设备或启动模拟器
adb devices

# 安装并运行
./gradlew :app:installDebug
adb shell am start com.smartsales.aitest/.AiFeatureTestActivity
```

**Windows (PowerShell/CMD)**:
```powershell
# 连接到设备或启动模拟器
adb devices

# 安装并运行
.\gradlew.bat :app:installDebug
adb shell am start com.smartsales.aitest/.AiFeatureTestActivity
```

**注意**：Windows 用户应使用 `gradlew.bat` 而不是 `./gradlew`。

### 5.2 运行测试

**Linux/macOS/Git Bash**:
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

**Windows (PowerShell/CMD)**:
```powershell
# 单元测试
.\gradlew.bat testDebugUnitTest

# 运行特定模块的测试
.\gradlew.bat :feature:connectivity:testDebugUnitTest
.\gradlew.bat :feature:chat:testDebugUnitTest
.\gradlew.bat :feature:media:testDebugUnitTest

# Android UI 测试（需要设备）
.\gradlew.bat :app:connectedDebugAndroidTest

# 导航 Compose UI 冒烟测试
.\gradlew.bat :app:assembleDebug :app:connectedDebugAndroidTest
```

**注意**：Windows 用户应使用 `.\gradlew.bat` 代替 `./gradlew`。

### 5.3 查看日志

**Linux/macOS/Git Bash**:
```bash
# 过滤项目日志
adb logcat | grep -E "SmartSales|AiFeatureTest"

# 或使用标签过滤
adb logcat tag:SmartSalesChat:* *:S
```

**Windows (PowerShell)**:
```powershell
# 过滤项目日志
adb logcat | Select-String -Pattern "SmartSales|AiFeatureTest"

# 或使用标签过滤
adb logcat tag:SmartSalesChat:* *:S
```

**Windows (CMD)**:
```cmd
# 过滤项目日志（需要先安装 findstr 或使用其他工具）
adb logcat | findstr "SmartSales AiFeatureTest"
```

---

## 🎯 第六步：开始你的第一个任务

### 6.1 了解工作流程

根据 `docs/AGENTS.md` 和 `docs/specs/Orchestrator-V1.md`：
1. **PLANNING** - 研究代码库，理解需求，设计方案
2. **EXECUTION** - 编写代码，实现设计
3. **VERIFICATION** - 测试变更，验证正确性

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
**Linux/macOS/Git Bash**:
```bash
# 解决方案：刷新依赖
./gradlew --refresh-dependencies
```

**Windows**:
```powershell
# 解决方案：刷新依赖
.\gradlew.bat --refresh-dependencies
```

### Q2: 构建失败 - JDK 版本错误
**Linux**:
```bash
# 检查并设置正确的 JDK
export JAVA_HOME=/path/to/jdk-17
# 或修改 gradle.properties 中的 org.gradle.java.home
```

**macOS**:
```bash
# 使用 java_home 工具设置（推荐）
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# 或手动设置路径
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
# 或 Homebrew 安装的路径
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

# 或修改 gradle.properties 中的 org.gradle.java.home
```

**macOS 常见问题**：
- 如果安装了多个 JDK 版本，使用 `java_home` 工具自动选择
- M1/M2 Mac 用户确保使用 ARM64 版本的 JDK
- 验证：`java -version` 应该显示 17.x.x

**Windows (PowerShell)**:
```powershell
# 检查并设置正确的 JDK（临时）
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
# 或修改 gradle.properties 中的 org.gradle.java.home
```

**Windows (Git Bash)**:
```bash
# 检查并设置正确的 JDK
export JAVA_HOME="/c/Program Files/Java/jdk-17"
# 或修改 gradle.properties 中的 org.gradle.java.home
```

### Q3: 依赖下载慢或失败
- 项目已配置阿里云镜像，应该较快
- 如果仍慢，检查网络连接
- 本地镜像在 `third_party/maven-repo/`，大部分依赖已缓存

**macOS 特定问题**：
- 如果使用代理，确保在 `~/.gradle/gradle.properties` 中配置代理设置
- 某些企业网络可能需要配置代理才能访问 Maven 仓库
- 如果遇到 SSL 证书问题，可能需要更新 Java 的 CA 证书

### Q4: API keys 缺失怎么办？
- 应用仍可运行，但使用 Fake 实现
- AI 聊天会返回模拟响应
- 不影响大部分开发和测试工作

### Q5: 如何查看某个功能如何工作？
1. 查看 `docs/plans/tracker.md` 了解模块状态和架构
2. 阅读代码中的注释和文件头
3. 查看测试代码了解预期行为
4. 查看 `docs/specs/Orchestrator-V1.md` 了解核心架构契约

### Q7: macOS 上 adb 命令找不到
**解决方案**：
```bash
# 确保 Android SDK platform-tools 在 PATH 中
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"

# 添加到 ~/.zshrc 或 ~/.bash_profile 使其永久生效
echo 'export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"' >> ~/.zshrc
source ~/.zshrc

# 验证
adb version
```

### Q8: macOS 上 gradlew 权限错误
**解决方案**：
```bash
# 添加执行权限
chmod +x gradlew

# 如果仍然有问题，检查文件权限
ls -l gradlew
# 应该显示 -rwxr-xr-x
```

### Q6: 新增了哪些主要功能？
根据最新更新，项目已包含：
- **ChatHistory 屏幕** - 聊天历史记录查看
- **AudioFiles 屏幕** - 音频文件管理
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
- [ ] 阅读了 `docs/AGENTS.md`
- [ ] 阅读了 `docs/plans/tracker.md`
- [ ] 理解了模块结构（chat, media, connectivity, usercenter）
- [ ] 配置了 `local.properties`（或了解如何配置）
- [ ] 了解了项目当前里程碑（M1 Feature Complete）

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

