# API Contracts for SmartSales

This document is the single source of truth for every API/protocol that the Android app touches. Each entry below records three things and future edits must keep the same structure:

1. **Contract** – what the HTTP/BLE interface does and the payloads or signals involved.  
2. **Android binding** – which Kotlin classes talk to the interface and how.  
3. **Code location** – module plus file path so anyone can open the implementation quickly.

## Quick Start (for clients)
- DashScope：`POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation`，Header `Authorization: Bearer <DASHSCOPE_API_KEY>`，超时约 10s+，重试 300ms 递增。
- Tingwu：`https://<region>.aliyuncs.com/openapi/tingwu/v2/`，需 ROA 签名头（`x-acs-*`，`x-tingwu-app-key`）；浏览器需走后端代理，勿暴露密钥。
- OSS：上传/预签名仅在后端执行，前端请调用后端获取上传/下载 URL，不要在浏览器暴露 AccessKey。
- 设备媒体服务器：`http://<host>:<port>`（BLE 查询或手输），接口 `/files`、`/upload`、`/apply/{filename}`、`/delete/{filename}`，超时 10–15s。

## Cloud AI & Export Interfaces

### DashScope Text Generation (HTTP)
- **Contract (HTTP)**:
  - Endpoint `POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation`. Body matches `DashscopeRequest { apiKey, model, temperature(default 0.3), messages[] }`, where `messages` is the ordered conversation (`system` prompt assembled from skill tags, `user` prompt containing the question, optional Tingwu markdown, and attachment manifest).
  - Response payload is `DashscopeCompletion { displayText }`. Android expands it into `AiChatResponse { displayText, structuredMarkdown, references[] }` by appending “输入摘要/纪要” metadata to the markdown body.
  - Streaming mode uses DashScope’s SDK callback. `DashscopeStreamEvent.Chunk` delivers incremental text, `Completed` flushes the accumulator, and `Failed` contains the reason plus the original throwable.
  - Error policy: all failures map to `AiCoreException(source=DASH_SCOPE, reason=MISSING_CREDENTIALS|NETWORK|TIMEOUT|REMOTE|UNKNOWN)`. `AiCoreConfig.dashscopeMaxRetries` controls retry count with a 300 ms incremental backoff, and `dashscopeRequestTimeoutMillis` protects synchronous calls via `withTimeout`.
  - Positive run: valid credentials + prompt → `DashscopeCompletion.displayText="您好，以下是建议..."`, final markdown contains “## 输入摘要”.  
    Negative run: blank `apiKey` triggers `AiCoreException(source=DASH_SCOPE, reason=MISSING_CREDENTIALS)` before the HTTP call happens.
  - Example request:
    ```json
    POST /api/v1/services/aigc/text-generation/generation
    Authorization: Bearer <DASHSCOPE_API_KEY>
    {
      "model": "qwen-max",
      "messages": [
        {"role": "system", "content": "你是智能销售助手..."},
        {"role": "user", "content": "请总结会议要点"}
      ],
      "temperature": 0.3
    }
    ```
  - Example response:
    ```json
    { "output": { "text": "您好，以下是建议..." } }
    ```
- **Android binding (Kotlin)**:
  - `DashscopeAiChatService` implements `AiChatService`, builds requests, handles retries/markdown, and exposes both unary and streaming flows.
  - `DefaultDashscopeClient` wraps the DashScope SDK (`Generation.call` / `streamCall`) and emits `DashscopeStreamEvent`.
  - `DashscopeCredentialsProvider` (default implementation reads from `local.properties`) supplies `apiKey` + `model`.
- **Code location**:
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeAiChatService.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeClient.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/DashscopeCredentialsProvider.kt`

### Tingwu Offline Transcription (HTTP)
- **Contract (HTTP)**:
  - Base URL `https://<region>.aliyuncs.com/openapi/tingwu/v2/` (overridable via `AiCoreConfig.tingwuBaseUrlOverride`). Requests carry ROA headers: `x-acs-date`, `x-acs-signature-*`, `x-tingwu-app-key`, and optional `x-acs-security-token`.
  - Endpoints:
    1. `PUT /tasks?type=offline` with body `TingwuCreateTaskRequest { AppKey, Input { SourceLanguage, TaskKey, FileUrl }, Parameters.transcription { diarizationEnabled, model } }`.
    2. `GET /tasks/{taskId}?type=offline` returning `TingwuStatusResponse { Data { taskStatus, taskProgress, errorCode, Result } }`.
    3. `GET /tasks/{taskId}/transcription?format=json` returning `TingwuResultResponse { Data { transcription, Result } }`.
  - Lifecycle: `RealTingwuCoordinator.submit()` resolves OSS URLs (via presigned links when only `ossObjectKey` is provided), posts the create call, persists a `TingwuJobState`, and launches a poller. Poll intervals/timeouts are read from `AiCoreConfig` (`tingwuPollIntervalMillis`, `tingwuPollTimeoutMillis`, etc.). When status hits `COMPLETED`, Android either reads inline transcription JSON or dereferences `Result.Transcription` to download markdown text and artifact links.
  - Error policy: HTTP/network exceptions or malformed JSON map to `AiCoreException(source=TINGWU, reason=MISSING_CREDENTIALS|NETWORK|TIMEOUT|REMOTE|IO|UNKNOWN)` with suggestions (e.g., “启用 AiCoreConfig.enableTingwuHttpDns”). Timeouts fire when global or per-task limits lapse. Missing `taskId` or empty `Data` also throw `AiCoreException`.
  - Positive run: `PUT /tasks?type=offline` → `code:0, data:{taskId:"tw-001"}`; poller shows “转写中” and finally `TingwuJobState.Completed` with markdown + artifacts.  
    Negative run: `GET /tasks/{id}` returning `code:500, message:"signature invalid"` becomes `AiCoreException(reason=REMOTE)` prompting ROA signature review.
  - Example create task body:
    ```json
    {
      "AppKey": "<app-key>",
      "Input": {"SourceLanguage": "zh-CN", "TaskKey": "task-123", "FileUrl": "https://oss/xxx.wav"},
      "Parameters": {
        "transcription": {"diarizationEnabled": true, "model": "general"},
        "summarization": {"types": ["Paragraph"]}
      }
    }
    ```
  - Example status response:
    ```json
    {"code":0,"data":{"taskStatus":"COMPLETED","taskProgress":100,"Result":{"Transcription":"..."}}}
    ```
- **Android binding (Kotlin)**:
  - `RealTingwuCoordinator` implements `TingwuCoordinator`, glues job submission, polling, markdown assembly, and artifact hydration.
  - `TingwuApi` (Retrofit interface) plus `TingwuNetworkModule` build the HTTP stack (OkHttp with ROA signing, optional HTTPDNS/logging, configurable base URL).
  - `TingwuAuthInterceptor` injects ROA headers, signs the canonical request, and adds `Authorization: acs <AccessKeyId>:<Signature>`.
- **Code location**:
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuApi.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuAuthInterceptor.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuNetworkModule.kt`

### Aliyun OSS Upload & Signed URLs (HTTP)
- **Contract (HTTP)**:
  - Uploads use the Aliyun Android SDK (`PutObjectRequest(bucket, objectKey, filePath)`) against the configured `OSS_ENDPOINT`. Default object key format: `ai-test/<timestamp>-<filename>`.
  - Successful uploads immediately produce a presigned download URL valid for 900 s via `presignConstrainedObjectURL`. When Tingwu needs read-only access, Android reuses the presigner through `OssSignedUrlProvider`.
  - Credentials are read from `local.properties` (`OSS_ACCESS_KEY_ID`, `OSS_ACCESS_KEY_SECRET`, `OSS_BUCKET_NAME`, `OSS_ENDPOINT`). Missing data short-circuits with `AiCoreException(source=OSS, reason=MISSING_CREDENTIALS)`.
  - Positive run: `PutObject` returns 2xx with `eTag`, presigned URL is stored in `OssUploadResult`.  
    Negative run: invalid bucket name or expired key triggers `ServiceException`, mapped to `AiCoreException(reason=REMOTE)` with `requestId` logged for support.
  - Web client note: do not expose AccessKey in browser; use a backend to upload and presign, returning signed URLs to the client.
- **Android binding (Kotlin)**:
  - `RealOssUploadClient` implements `OssUploadClient`, validates the input file, builds the SDK client, executes uploads, and wraps errors.
  - `RealOssSignedUrlProvider` implements `OssSignedUrlProvider` for presigning existing objects, reusing the same credential provider.
  - `OssCredentialsProvider` centralizes credential loading and normalization so both uploader and presigner stay in sync.
- **Code location**:
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/RealOssUploadClient.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/OssSignedUrlProvider.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/OssUploadClient.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/OssCredentialsProvider.kt`

### Markdown Exporters (Local Service)
- **Contract (local API)**:
  - `ExportManager.exportMarkdown(markdown: String, format: ExportFormat)` converts chat transcripts to `PDF` (via `MarkdownPdfEncoder`) or `CSV` (`MarkdownCsvEncoder`). Output DTO: `ExportResult { fileName, mimeType, payload, localPath }`.
  - Positive run: “Export PDF” button returns `ExportResult(fileName="smart-sales-<timestamp>.pdf", mimeType="application/pdf")`, UI feeds Android sharesheet/storage.  
    Negative run: disk IO failure or encoder error throws `AiCoreException(source=EXPORT, reason=IO)` surfaced back to the Compose UI with a toast suggestion.
- **Android binding (Kotlin)**:
  - `RealExportManager` orchestrates encoding, writes files via `ExportFileStore`, logs status, and wraps any throwable into `AiCoreException`.
  - `AndroidExportFileStore` persists payloads into `context.cacheDir/exports/markdown`, ensuring directories exist before writing.
  - `MarkdownPdfEncoder` & `MarkdownCsvEncoder` hold the pure-conversion logic used by both real and fake managers.
- **Code location**:
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/RealExportManager.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/ExportManager.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/MarkdownPdfEncoder.kt`
  - `:data:ai-core/data/ai-core/src/main/java/com/smartsales/data/aicore/MarkdownCsvEncoder.kt`

## Device Connectivity & Gadget APIs

### BLE Wi-Fi Provisioning Channel (Gatt)
- **Contract (BLE)**:
  - Wi-Fi credentials are written to the device’s credential characteristic as `wifi#connect#<ssid>#<password>` (any `#` in SSID/pass is stripped). Devices acknowledge via JSON (`{ handshake_id, credentials_hash, rejected }`) or a delimited string `wifi#connect#ok|error#<handshakeId|reason>`.
  - Hotspot fetches read the configured hotspot characteristic, expecting JSON `{ "ssid": "...", "password": "..." }`.
  - Network diagnostics send the literal `wifi#address#ip#name` command; responses arrive over the provisioning status characteristic either as JSON `{ ip, device_wifi, phone_wifi }` or as delimited text `wifi#address#<ip>#<deviceWifi>#<phoneWifi>`.
  - Result mapping: success responses become `ProvisioningStatus { wifiSsid, handshakeId, credentialsHash }`. Failures map to `BleGatewayResult.PermissionDenied|Timeout|TransportError|CredentialRejected`, which the app surfaces as `ConnectivityError` states (`ConnectionState.Error`).
  - Positive run: `wifi#connect#ok#handshake123` transitions UI to `ConnectionState.WifiProvisioned` and starts a heartbeat loop.  
    Negative run: `wifi#connect#error#auth_failed` becomes `ProvisioningException.CredentialRejected("auth_failed")`, and the tester page shows an error banner.
- **Android binding (Kotlin)**:
  - `GattBleGateway` handles characteristic discovery, read/write/notify orchestration, payload parsing, and command timeouts.
  - `AndroidBleWifiProvisioner` implements `WifiProvisioner`, translating `BleGatewayResult` into `ProvisioningStatus` or `ProvisioningException`.
  - `DefaultDeviceConnectionManager` coordinates provisioning attempts, auto-retry, and state exposure to UI, while `ConnectivityControlViewModel` feeds Compose UI events (scan, send credentials, query network/hotspot).
- **Code location**:
  - `:feature:connectivity/feature/connectivity/src/main/java/com/smartsales/feature/connectivity/gateway/GattBleGateway.kt`
  - `:feature:connectivity/feature/connectivity/src/main/java/com/smartsales/feature/connectivity/AndroidBleWifiProvisioner.kt`
  - `:feature:connectivity/feature/connectivity/src/main/java/com/smartsales/feature/connectivity/DeviceConnectionManager.kt`
  - `:app/app/src/main/java/com/smartsales/aitest/ConnectivityControlViewModel.kt`

### Device Media Server HTTP API
- **Contract (HTTP)**:
  - Base URL discovered from the BLE network query (or manually entered in the tester page) and normalized to `http://<host>:<port>`. Endpoints:
    - `GET /files` → `{ "files": [ { name, sizeBytes, mimeType, modifiedAtMillis, mediaUrl, downloadUrl } ] }`.
    - `POST /upload` – multipart form field `file` with original filename + MIME type; 2xx means success.
    - `POST /apply/{filename}` – instructs device to activate the uploaded asset.
    - `DELETE /delete/{filename}` – removes the asset.
    - `GET downloadUrl` – raw bytes stream for caching locally.
  - Timeouts: 10 s for reads/deletes/applies, 15 s for uploads/downloads. All responses must be `2xx`; otherwise Android throws `IllegalStateException`.
  - Positive run: `POST /upload` 200 + subsequent `GET /files` shows the asset; `MediaServerPanel` lists it with download/apply/delete controls.  
    Negative run: invalid base URL or HTTP 500 produces `Result.Error` with a descriptive exception surfaced through the tester snack bar.
- **Android binding (Kotlin)**:
  - `MediaServerClient` performs the raw `HttpURLConnection` calls, handles JSON parsing, writes local files, and wraps all results in `Result.Success/Error`.
  - `ConnectivityControlViewModel` and `WifiBleTesterPage/MediaServerPanel` let QA users edit the base URL, trigger uploads/downloads, and display status messages.
  - `MediaServerFile` data class mirrors the response schema so Compose components can render size/type/URLs.
- **Code location**:
  - `:app/app/src/main/java/com/smartsales/aitest/MediaServerClient.kt`
  - `:app/app/src/main/java/com/smartsales/aitest/WifiBleTesterPage.kt`
  - `:app/app/src/main/java/com/smartsales/aitest/ConnectivityControlViewModel.kt`
