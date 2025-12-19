# XFyun 审计与修复记录（ghost debugging 后）

日期：2025-12-19

目的：在不假设“根因一定是代码 bug”的前提下，基于证据梳理 XFyun upload/getResult 现状，并修复近期调试阶段可能引入的高风险/误导性行为（例如过度强制参数、观测不足导致误诊）。

## 1) 当前 /v2/upload 实际发送的 query 参数（仅 keys）

说明：以下为代码层可控的 query keys（不包含 signature/密钥材料，签名在 header）。

- 必定包含：
  - `appId`
  - `accessKeyId`
  - `dateTime`
  - `signatureRandom`
  - `fileSize`
  - `fileName`
  - `ts`
  - `roleType`
  - `roleNum`
  - `audioMode`
  - `resultType`
  - `eng_smoothproc`
  - `eng_colloqproc`
  - `eng_vad_mdn`
  - `language`
- 可能包含（非空才会出现）：
  - `featureIds`（声纹辅助分离）
  - `pd`（领域个性化）
  - `duration`

## 2) language 的派生与兜底逻辑（精确行为）

目标：避免服务端返回 `code=100020: language verify fail, cause: language[cn] does not support`。

- 输入来源（用于证据记录）：
  - `requestedLanguage`：上层调用显式传入
  - `AiParaSettings.transcription.xfyun.upload.language`：运行时设置
- 取值优先级：
  - 优先使用 `requestedLanguage`（非空）
  - 否则使用 settings 中的 `upload.language`
- 兜底/修正（conservative sanitizer）：
  - 空值 → `autodialect`
  - 旧值 `cn/zh/zh_cn/zh-cn/chinese` → `autodialect`
  - 其它值：原样透传（避免把“配额/未开通/鉴权”等问题误诊成 language 问题）
- 重要：`language` 必须显式发送（省略会被服务端默认成 `cn`，仍可能失败）。

## 3) Retry / fallback 行为

- 不存在 “omit language” 之类的 retry。
- 目前仅保留“参数签名兼容策略”的尝试序列（不同 encodeKeys/策略组合），用于兼容签名细节差异；不涉及语言切换。

## 4) AiParaSettings 默认值（与 fail-soft 目标对齐）

- `xfyun.upload.language` 默认：`autodialect`
- `xfyun.upload.roleType` 默认：`1`（普通说话人分离）
- `xfyun.voiceprint.enabled` 默认：`false`
- `xfyun.postXfyun.enabled` 默认：`false`

## 5) 错误处理与观测（避免误诊）

upload/getResult 解析均会读取响应 JSON 的 `code`/`descInfo`；即使 HTTP 200，也会在 `code != 000000` 时判定为失败并记录。

新增/强化的 trace 字段（可在 HUD “复制摘要”中看到）：

- upload language 证据：
  - `uploadLanguageRequested`
  - `uploadLanguageFromSettings`
  - `uploadLanguageResolved`
  - `uploadLanguageSent`
- upload URL 证据：
  - `uploadUrlHost`
  - `uploadUrlPath`
- upload 业务失败证据与分类：
  - `uploadBusinessCode`
  - `uploadBusinessDescInfo`
  - `uploadFailureCategory`（LANGUAGE / QUOTA_OR_ENTITLEMENT / AUTH / UNKNOWN）
  - `uploadFailureHint`（安全、简短的下一步建议）

## 6) 临时调试痕迹确认

- 已移除在工作目录写入调试文件的代码（例如把请求/响应写入本地文件的“agent log”逻辑）。
- HUD/复制文本不包含任何密钥/签名材料，不包含声纹注册的 base64 音频数据，不包含 XFyun 原始 HTTP 响应体（raw dump 仅以文件路径形式暴露）。

