<!-- 文件路径: feature/connectivity/README.md -->
<!-- 文件作用: 描述设备连接模块接口与状态机 -->
<!-- 文件目的: 统一 BLE 与 Wi-Fi 配网逻辑 -->
<!-- 相关文件: docs/current-state.md, plans/tdd-plan.md, reference-source/legacy/README_DELIVERY.md -->

> ⚠️ 在阅读本模块说明之前，请先阅读 `docs/current-state.md` 了解项目当前快照和全局约束。

# Connectivity Feature 模块

## 模块职责
- 管理 BLE 扫描/配对、Wi-Fi 凭据下发、热点凭据获取以及心跳同步。
- 对外暴露 `DeviceConnectionManager`（状态 `Flow<ConnectionState>` + 指令 `startPairing`, `retry`, `forgetDevice`, `requestHotspotCredentials`）。
- 不执行媒体同步或 AI 调用，只提供连接状态给其它模块消费（媒体/聊天/导出等）。

## 关键接口与依赖
- `ConnectionState`：`Disconnected` / `Pairing` / `WifiProvisioned` / `Syncing` / `Error`。
- `BleSession`：记录设备 ID、信号强度、安全 Token。
- `WifiProvisioner`：负责配网与热点凭据请求；默认实现 `SimulatedWifiProvisioner`。
- 依赖：`projects.core.util`（Result、Dispatchers）、`projects.core.test`（测试 Fake），真实环境需接入 BLE/Wi-Fi SDK。

## 数据与控制流
1. UI/业务调用 `startPairing`，内部创建 `BleSession` 并进入 `Pairing` 状态。
2. `WifiProvisioner` 完成凭据写入 → 输出 `ProvisioningStatus` → 状态更新为 `WifiProvisioned`。
3. 进入心跳循环，定期发出 `Syncing` 状态供媒体/聊天模块判断联网情况，`:app` 的 `AppShellRepository` 也订阅此状态驱动连接横幅/媒体面板。
4. 失败路径：记录 `ConnectivityError`，供 UI 提示并触发 `retry` 或 `forgetDevice`。

## 当前状态（T1）
- 状态机与单测稳定，`DefaultDeviceConnectionManagerTest` 覆盖成功、重试、热点凭据缺失等场景。
- `WifiProvisioner` 仍为模拟实现，未与真实 BLE/Wi-Fi 硬件交互；权限策略也未接入 UI。
- 尚无与 `:feature:media` 的实时联动，UI 仅能通过日志或未来面板查看状态。

### BLE Profile 配置
- Hilt 仍提供 `List<BleProfileConfig>`，但现在只负责 **扫描过滤与 UI 显示**。`AndroidBleScanner` 根据 `nameKeywords` 与可选的 `scanServiceUuids` 判断是否展示某个外围设备，并将 `profileId` 写入 `BlePeripheral`，方便日志排查。
- 连接成功后，`GattBleGateway` 会自动调用 `discoverServices()`，遍历设备实际暴露的 Service/Characteristic 并在运行时构建 `BleGatewayConfig`（优先选择同时具备 WRITE+NOTIFY 或 WRITE+READ 的 GATT 服务），结果会缓存到 `configCache`，不再依赖硬编码 UUID。
- 默认 profile 仍匹配 `BT311 / Nordic UART`。如需额外的名称/UUID 过滤，可通过 `-Pconnectivity.bleProfilesJson='[{"id":"alpha","displayName":"Alpha","nameKeywords":["Alpha"],"scanServiceUuids":["0000ffe1-..."]}]'` 或环境变量 `SMARTSALES_BLE_PROFILES_JSON` 覆盖；JSON 里 **不需要** 再写具体的 characteristic UUID。
- logger 统一使用 `ConnectivityLogger`，JVM 单测环境下也能安全输出。

## 风险与限制
- 没有真实 SDK → 无法验证并发扫描、权限弹窗、Android 12+ BLE 限制。
- 心跳循环依赖协程 Scope，若外部未妥善管理可能导致泄漏。
- 缺少错误记录与 Telemetry，排查困难。

## 下一步动作
- 接入真实 BLE/Wi-Fi SDK，参考 `reference-source` 补充协议说明。
- 在 `:core:test` 添加更丰富的 Fake（延迟、掉线、权限拒绝），并扩展单测覆盖所有负路径。
- 将 `ConnectionState` 注入到 `:feature:media` / `:app` UI，提供配网流程与错误提示。

## 调试与验证
- 单测：`./gradlew :feature:connectivity:test`。
- 真机调试：待接入硬件后，需在 `docs/progress-log.md` 记录每次协议或权限变更，并触发 logging workflow。
