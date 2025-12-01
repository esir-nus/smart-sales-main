Here’s a first cut of **media.md** that matches how we’ve been treating `connectivity.md` and plugs into the DeviceManager work you’re doing.

---

# media.md

> **Scope:** Management of remote media files on BT311 (and similar devices), including:
>
> * HTTP contract with the gadget media server
> * DeviceManager screen & ViewModel
> * Basic integration points with Home exports & AudioFiles

This document defines the **contract** for the media module. New work under `feature/media/**` and the DeviceManager section of the app **must** follow this document.

---

## 1. Goals

1. **Single responsibility:**

   * Connectivity module decides *if* the device is connected.
   * Media module decides *what files exist* and *how we interact with them*.
2. **Predictable UX:**

   * “已连接 BT311” always means: we have a valid HTTP base URL and can *attempt* file operations.
   * File-related failures (list/apply/delete/upload) show **file errors**, not generic connectivity errors.
3. **Stable backend contract:**

   * Media client must adhere to the gadget’s `wifi.py` API contract.
   * Relative URLs and JSON shape must be handled robustly.
4. **Testable:**

   * DeviceManagerViewModel has unit tests for list/preview/apply/delete/upload flows.
   * MediaServerClient has contract tests against the `wifi.py` schema (via fakes).

---

## 2. System architecture

### 2.1 Layers

* **Connectivity core (`feature/connectivity`)**

  * Handles BLE, Wi-Fi, HTTP health and session persistence.
  * Exposes `DeviceConnectionState` and a `mediaBaseUrl` (or equivalent) in the session.

* **Media core (`feature/media`)**

  * **MediaServerClient** / **DeviceMediaGateway**:

    * Thin HTTP client over BT311’s media API.
    * Responsible for building URLs, calling `/files`, `/apply`, `/delete`, `/upload`, and parsing JSON.
  * **Media models**:

    * Data classes for the file list, individual file metadata, and any status.

* **DeviceManager UI (`feature/media/devicemanager`)**

  * `DeviceManagerViewModel`:

    * Observes `DeviceConnectionState`.
    * Talks to MediaServerClient.
    * Owns file list state, preview state, and errors.
  * `DeviceManagerScreen`:

    * Renders connection card, file list, preview panel, and actions (refresh, upload, apply, delete).
    * Uses UI state only; no direct HTTP.

* **Home / Audio integration**

  * Home export flows (smart analysis / generate CSV / generate PDF) may produce files that are:

    * Stored locally, and/or
    * Uploaded via MediaServerClient `/upload` so they appear in DeviceManager.
  * AudioFiles screen may show a derived view of the same logical files (or a subset) but still uses media core.

### 2.2 Ownership boundaries

* Only **media core** calls the gadget’s HTTP server.
* Only **DeviceManagerViewModel** orchestrates file flows for DeviceManager.
* The rest of the app gets everything via:

  * `DeviceConnectionState` (connectivity),
  * Media gateway/client APIs (files),
  * DeviceManager UI state (rendering).

---

## 3. HTTP contract with BT311 media server

The gadget media server is implemented by `wifi.py`. The media module must conform to its API:

> **Base URL:** Provided by connectivity as `http://<host>:8000`
> (host is typically the gadget’s IP on the local network).

### 3.1 Health

* **Endpoint:** `GET /` (or `/index.html`)
* **Semantics:**

  * Responds with HTTP `200` and a small JSON payload such as `{ "status": "ok" }`.
  * Used by connectivity’s `HttpEndpointChecker` to decide if the server is alive.
* Media layer typically does **not** call health directly; it trusts connectivity’s health check.

### 3.2 List files

* **Endpoint:** `GET /files`

* **Response JSON (contract):**

  ```json
  {
    "count": 3,
    "files": [
      {
        "name": "report-2025-11-29.pdf",
        "size": 123456,
        "created_at": "2025-11-29T10:23:00Z",
        "mediaUrl": "/media/report-2025-11-29.pdf",
        "downloadUrl": "/download/report-2025-11-29.pdf"
      }
    ]
  }
  ```

  Notes:

  * `mediaUrl` and `downloadUrl` are **relative paths**; the client must prepend the base URL.
  * Additional fields (e.g. `type`, `description`) may be present; parsing must be forward-compatible.

* **MediaServerClient responsibilities:**

  * Build request to `GET "$baseUrl/files"`.
  * Parse `count` and `files[]` robustly:

    * If `count` mismatches `files.size`, trust `files.size`.
    * Treat missing optional fields as `null` / default.
  * Normalize URLs:

    * `absoluteMediaUrl = baseUrl.trim('/') + mediaUrl`
    * `absoluteDownloadUrl = baseUrl.trim('/') + downloadUrl`

* **DeviceManagerViewModel:**

  * On success:

    * Updates its `files` list in UiState.
    * `isLoadingFiles = false`, `fileLoadError = null`.
  * On error (HTTP errors, parse errors, timeouts):

    * Keeps `DeviceConnectionState` as **Connected**.
    * Sets `fileLoadError` with a user-friendly text (e.g. “无法获取文件列表，请稍后重试”).
    * Does **not** downgrade connection to “未连接”.

### 3.3 Apply / activate a file

* **Endpoint:** `POST /apply/{name}`

* **Semantics:**

  * Tells the gadget to “apply” or activate a given file; concrete meaning depends on file type (e.g. set as active script/config).
  * Returns a simple JSON or text status; treat HTTP `2xx` as success.

* **MediaServerClient:**

  * Encodes `name` safely in the URL path.
  * `POST "$baseUrl/apply/$encodedName"` with an empty body (or as `application/json`/`form` depending on wifi.py; match implementation).

* **DeviceManagerViewModel:**

  * Action `onApplyFile(file)`:

    * Set `isApplying = true` for that file.
    * Call client; on success:

      * Optionally mark that file as “applied” in UI state (badge).
    * On error:

      * Set a file-specific error message (e.g. “应用失败，请重试”).

### 3.4 Delete a file

* **Endpoint:** `POST /delete/{name}`

* **Semantics:**

  * Deletes the file from the gadget’s storage.
  * Returns HTTP `200` or similar on success.

* **DeviceManagerViewModel:**

  * `onDeleteFile(file)`:

    * Optionally ask for confirmation (UI layer).
    * Call client delete; on success:

      * Remove item from `files` list.
    * On error:

      * Show a file-specific error; optional: keep item and mark error.

### 3.5 Upload a file

* **Endpoint:** `POST /upload`

* **Semantics:**

  * Receives a file via multipart/form-data or equivalent.
  * Adds it to the gadget’s storage and makes it visible in `/files`.

* **MediaServerClient:**

  * Build a multipart request from an Android `Uri` / file handle.
  * On success:

    * Either return the new file entry or simply success; DeviceManagerViewModel will then refresh `/files`.

* **DeviceManagerViewModel:**

  * `onUploadFileSelected(uri)`:

    * Set `isUploading = true`.
    * Call client upload; on success:

      * Trigger a file list refresh so the new file appears.
    * On error:

      * Set `uploadError` message.

---

## 4. DeviceManager behavior contract

### 4.1 UiState shape (conceptual)

`DeviceManagerUiState` should capture:

```kotlin
data class DeviceManagerUiState(
    val connectionState: DeviceConnectionState,
    val canStartSetup: Boolean,
    val canRetryConnect: Boolean,

    val baseUrl: String?,           // from connected session
    val isLoadingFiles: Boolean,
    val fileLoadError: String?,
    val files: List<MediaFileUi>,   // mapped from server
    val selectedFileId: String?,
    val isApplying: Boolean,
    val applyError: String?,
    val isDeleting: Boolean,
    val deleteError: String?,
    val isUploading: Boolean,
    val uploadError: String?,

    val previewState: PreviewState  // loading/success/error for current file
)
```

`MediaFileUi` is a UI-friendly representation of server file entries (id, name, size text, createdAt text, applied badge, etc).

### 4.2 Top “设备管理” card

* **When `DeviceConnectionState.Connected(session)`**:

  * Show:

    * Title: “设备管理”.
    * Status badge: “已连接 BT311” (or device name).
    * Subtext: “可预览、刷新和上传文件。”
    * Base URL field (read-only): `session.mediaBaseUrl`.
  * Hide the “设备未连接 / 重试连接” bottom card.

* **When `NeedsSetup`**:

  * Show:

    * Top card: “设备未连接，需要先完成设备配网。”
    * CTA: “开始配网” (calls `onStartSetup`).
  * Bottom card: no “重试连接” button.

* **When `Disconnected` or `Error`**:

  * Top card: “设备未连接 / 请检查网络或设备电源。”
  * Bottom card:

    * “设备未连接 / 重试连接” shown.
    * “重试连接” → `DeviceConnectionManager.forceReconnectNow()` via VM.

### 4.3 File list and empty states

* **Connected + files successfully loaded:**

  * Show section header: “文件列表 (N)”.
  * List items:

    * Name.
    * Optional size & timestamp.
    * Optional applied badge.
  * Selecting an item updates `selectedFileId` and triggers preview.

* **Connected + files loaded but empty:**

  * Header: “文件列表 (0)”.
  * No error; preview panel text: “请选择文件预览” (or “暂无文件，请上传新文件”).
  * Bottom “设备未连接” card is hidden.

* **Connected + `/files` failed:**

  * Keep connection visuals (“已连接 BT311…”).
  * Show a toast or inline message: “无法获取文件列表，请稍后重试” derived from `fileLoadError`.
  * Offer “刷新” to retry loading.
  * Do **not** show “设备未连接” card.

### 4.4 Preview panel

* When a file is selected:

  * `previewState = Loading` → show spinner.
  * On successful load:

    * Render appropriate preview:

      * PDF: embed viewer or open external.
      * CSV: show tabular preview or open external.
      * Other: show generic “可下载/打开” UI.
    * Provide a “下载 / 打开” action using `downloadUrl`.
  * On preview error:

    * `previewState = Error(errorText)`:

      * Show readable message, not “未连接”.

Preview fetch should generally use `downloadUrl`, not `mediaUrl`, unless the latter is explicitly for in-device usage.

---

## 5. Flows

### 5.1 Entering DeviceManager when Connected

1. Connectivity state: `DeviceConnectionState.Connected(session)`.
2. DeviceManagerViewModel sees new session:

   * Sets `baseUrl = session.mediaBaseUrl`.
   * `isLoadingFiles = true`.
3. VM calls `MediaServerClient.listFiles(baseUrl)`.
4. Outcomes:

   * **Success, non-empty:**

     * Update `files`.
     * `isLoadingFiles = false`.
   * **Success, empty:**

     * Same, with empty list.
   * **Failure:**

     * `isLoadingFiles = false`.
     * `fileLoadError` set; show file-level error, not connectivity error.

### 5.2 Refresh

1. User taps “刷新”.
2. If connection state is Connected:

   * `isLoadingFiles = true`.
   * Repeat `/files` call as above.
3. If not Connected:

   * `DeviceManagerViewModel` should prefer calling `forceReconnectNow()`; file refresh is blocked until Connected.

### 5.3 Upload

1. User taps “上传新文件”.
2. App opens a file picker and returns `Uri`.
3. VM:

   * `isUploading = true`.
   * Calls `MediaServerClient.upload(baseUrl, uri)`.
   * On success:

     * `isUploading = false`.
     * Triggers file refresh.
   * On error:

     * `isUploading = false`.
     * `uploadError` set.

### 5.4 Apply / Delete

Covered in §3.3 / §3.4; key point is that **connectivity state stays Connected** unless the connectivity module itself reports an error. File operation failures are not reinterpreted as loss of connection.

---

## 6. Error handling & UX mapping

The media module should map errors as:

| Layer        | Example error                               | UX surface                                            |
| ------------ | ------------------------------------------- | ----------------------------------------------------- |
| Connectivity | `EndpointUnreachable`                       | Top card “设备未连接 / 重试连接”                               |
| Media list   | HTTP 500 / JSON parse / timeout on `/files` | Inline/Toast “无法获取文件列表，请稍后重试”; connection stays “已连接” |
| Apply        | HTTP 4xx/5xx on `/apply`                    | File-level error “应用失败，请重试”                           |
| Delete       | HTTP 4xx/5xx on `/delete`                   | File-level error “删除失败，请重试”                           |
| Upload       | HTTP 4xx/5xx on `/upload` or file IO        | Upload error “上传失败，请稍后重试”                             |

**Never** show “设备未连接” solely because `/files`/`/apply`/`/delete`/`/upload` failed; that’s connectivity’s job.

---

## 7. Testing guidelines

### 7.1 Unit tests (media core + DeviceManagerViewModel)

* **MediaServerClient contract tests:**

  * `listFiles`:

    * Normal response with 0, 1, many files.
    * Relative paths → absolute URLs.
    * Missing optional fields.
  * `apply`, `delete`, `upload`:

    * Success responses.
    * Failure paths mapped to custom exceptions / results.

* **DeviceManagerViewModel tests:**

  * Connected + list success → UiState has files, no “未连接”.
  * Connected + list empty → UiState empty list, no error.
  * Connected + list failure → `fileLoadError` set, connection still “已连接”.
  * NeedsSetup → `canStartSetup = true`, bottom retry hidden.
  * Disconnected/Error → `canRetryConnect = true`, bottom retry shown.
  * Upload success/failure paths.
  * Apply/Delete success/failure paths.

### 7.2 Instrumentation tests

* DeviceManagerScreen (connected state):

  * Shows “已连接 BT311 / 媒体服务地址”.
  * When fake client returns files → list rendered with correct count.
  * When fake client returns empty → shows `(0)` without “设备未连接”.
* DeviceManagerScreen (disconnected):

  * Shows bottom “设备未连接 / 重试连接” card.
  * Tapping “重试连接” calls the reconnect path (validated by fake).

---

## 8. Do / Don’t

**Do:**

* Treat `DeviceConnectionState` as the only truth for connectivity.
* Keep file-related errors visually distinct from connection errors.
* Build all media URLs from the `baseUrl` + relative paths.
* Make `DeviceManagerUiState` expressive enough to separate:

  * connection,
  * file loading,
  * preview,
  * file operations.

**Don’t:**

* Don’t call BLE or Wi-Fi APIs from media core.
* Don’t downgrade connection to “未连接” purely because a file operation failed.
* Don’t hardcode absolute URLs; always use the session’s base URL.
* Don’t couple the file list format tightly to current front-end; be tolerant of extra fields.

---

This doc is meant to sit next to `connectivity.md`:

* `connectivity.md` → “Can we talk to BT311 and is HTTP alive?”
* `media.md` → “Now that we can talk to BT311, how do we manage its files?”

We can refine individual sections (especially the exact JSON shape and UiState fields) once we align with the current code and tests, but this should give you a solid contract to design and refactor against.
