# DashScope API 参考文档

> **来源**: [阿里云 DashScope API 参考](https://help.aliyun.com/zh/model-studio/dashscope-api-reference)  
> **更新时间**: 2025-12-11  
> **适用范围**: 中国大陆版（北京地域）  
> **文档目的**: 作为开发时的权威参考文档（Source of Truth）

## 目录索引

### 快速导航

- [概述](#概述)
- [功能场景](#功能场景)
- [调用方式](#调用方式)
  - [开始前准备](#开始前准备)
- [请求示例](#请求示例)
  - [Python](#python-请求示例)
  - [Java](#java-请求示例)
  - [HTTP curl](#http-curl-请求示例)
  - [PHP](#php-请求示例)
  - [Node.js](#nodejs-请求示例)
- [请求参数](#请求参数)
  - [HTTP 请求头](#http-请求头)
  - [请求体结构](#请求体结构)
  - [input 对象属性](#input-对象属性)
  - [parameters 对象属性](#parameters-对象属性)
  - [debug 对象属性](#debug-对象属性)
- [响应参数](#响应参数)
  - [响应体结构](#响应体结构)
  - [output 对象属性](#output-对象属性)
  - [doc_references 数组元素属性](#doc_references-数组元素属性)
  - [workflow_message 对象属性](#workflow_message-对象属性)
  - [usage 对象属性](#usage-对象属性)
- [特殊功能说明](#特殊功能说明)
  - [深度思考模型](#深度思考模型)
  - [知识检索文档引用](#知识检索文档引用)
  - [页码引用](#页码引用)
- [错误处理](#错误处理)
- [SDK 版本要求](#sdk-版本要求)
- [安全建议](#安全建议)
- [多轮对话](#多轮对话)
  - [session_id 与 memory_id 的区别](#session_id-与-memory_id-的区别)
- [流式输出](#流式输出)
- [知识库检索](#知识库检索)
- [长期记忆](#长期记忆)
- [文件上传](#文件上传)
- [视觉理解](#视觉理解)
- [传递参数](#传递参数)
- [私网终端节点配置](#私网终端节点配置)
- [常见问题](#常见问题)
  - [Java SDK 依赖问题](#java-sdk-依赖问题)
  - [session_id 与 memory_id 的区别](#session_id-与-memory_id-的区别-1)
- [完整相关链接索引](#完整相关链接索引)

---

## 概述

本文档介绍 DashScope API 调用阿里云百炼应用（**智能体**、**工作流**）的输入与输出参数，并提供 Python 等主流语言在典型场景下的调用示例。

**重要说明**：
- 本文档仅适用于中国大陆版（北京地域）
- 相关指南：
  - [调用智能体应用](https://help.aliyun.com/zh/model-studio/call-agent-application)
  - [调用工作流应用](https://help.aliyun.com/zh/model-studio/call-workflow-application)
- 如需通过 Responses API 调用，请参阅 [Responses API 参考](https://help.aliyun.com/zh/model-studio/responses-api-reference)

## 功能场景

DashScope API 支持以下主要功能场景：

1. **单轮对话** - 单次请求响应
   - 相关文档：[调用智能体应用-单轮对话](https://help.aliyun.com/zh/model-studio/call-single-agent-application#b3be03a1ff21e)
2. **多轮对话** - 通过 session_id 和 messages 启用多轮对话
   - 相关文档：
     - [调用智能体应用-多轮对话](https://help.aliyun.com/zh/model-studio/call-single-agent-application#bb173820c5whx)
     - [调用工作流应用-多轮对话](https://help.aliyun.com/zh/model-studio/invoke-workflow-application#6ca125d59eyc9)
3. **传递参数** - 自定义参数传递
   - 相关文档：[调用智能体应用-传递参数](https://help.aliyun.com/zh/model-studio/call-single-agent-application#de63036b85aj0)
4. **流式输出** - 流式响应处理
   - 相关文档：
     - [调用智能体应用-流式输出](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)
     - [调用工作流应用-流式输出](https://help.aliyun.com/zh/model-studio/invoke-workflow-application#6e644d5a7b3ia)
5. **检索知识库** - 知识库检索功能
   - 相关文档：[调用智能体应用-检索知识库](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)
6. **长期记忆** - 会话记忆管理
   - 相关文档：[长期记忆](https://help.aliyun.com/zh/model-studio/long-term-memory?spm=a2c4g.11186623.help-menu-2400256.d_1_7_0.1684297f7oNrzR&scm=20140722.H_2844738._.OR_help-T_cn~zh-V_1#a3398be1e7c1g)
7. **上传文件** - 文件上传与处理
   - 相关文档：[调用智能体应用-上传文件](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)
8. **视觉理解** - 图像理解与分析
   - 相关文档：[调用智能体应用-视觉理解](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)

## 调用方式

可通过 DashScope SDK 或 HTTP 接口调用阿里云百炼的应用。

- **SDK `base_url` 配置**：`https://dashscope.aliyuncs.com/api/v1`
- **HTTP 请求地址**：`POST https://dashscope.aliyuncs.com/api/v1/apps/{APP_ID}/completion`
  - `APP_ID`：应用ID（必填），从百炼控制台获取

### 请求参数（URL 路径）

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `app_id` | string | 是 | 应用ID，从百炼控制台获取，替换 URL 中的 `{APP_ID}` |

### 开始前准备

1. **获取凭证**
   - **API Key**：前往[密钥管理页面](https://bailian.console.aliyun.com)创建并获取 API Key
   - **应用ID（APP_ID）**：前往[应用管理页面](https://bailian.console.aliyun.com)创建智能体应用，并在应用卡片上复制其 APP_ID
   - 获取 App ID：[获取 App ID 和 Workspace ID](https://help.aliyun.com/zh/model-studio/get-app-id-and-workspace-id)

2. **安装 DashScope SDK**（HTTP 接口调用可跳过此步骤）
   
   **Python**：
   ```bash
   # 使用此命令，将SDK安装到您的Python 3环境中
   python3 -m pip install -U dashscope
   ```
   
   **Java（Maven）**：
   在 `pom.xml` 文件的 `<dependencies>` 部分添加：
   ```xml
   <dependency>
       <groupId>com.alibaba</groupId>
       <artifactId>dashscope-sdk-java</artifactId>
       <!-- 请将 'the-latest-version' 替换为最新版本号 -->
       <!-- 最新版本号：https://mvnrepository.com/artifact/com.alibaba/dashscope-sdk-java -->
       <version>the-latest-version</version>
   </dependency>
   ```
   保存后，IDE 会自动检测变更。如未自动提示，可手动执行 "Reload/Update Maven Project" 或在项目根目录执行：`mvn clean install`
   
   **Java（Gradle）**：
   在 `build.gradle` 文件的 `dependencies` 代码块中添加：
   ```gradle
   dependencies {
       // 请将 'the-latest-version' 替换为最新版本号
       // 最新版本号：https://mvnrepository.com/artifact/com.alibaba/dashscope-sdk-java
       implementation group: 'com.alibaba', name: 'dashscope-sdk-java', version: 'the-latest-version'
   }
   ```
   保存后，IDE 会显示同步图标，或执行：`./gradlew build --refresh-dependencies`
   
   **Node.js**：
   ```bash
   npm install dashscope-node
   ```

3. **配置环境变量（推荐）**
   - 为保障密钥安全并避免在代码中硬编码，建议配置 API Key 到环境变量
   - 环境变量名称：`DASHSCOPE_API_KEY`
   - SDK 将自动从此变量读取

## 请求示例

### Python 请求示例

```python
import os
from http import HTTPStatus
from dashscope import Application

response = Application.call(
    # 若没有配置环境变量，可用百炼API Key将下行替换为：api_key="sk-xxx"。
    # 但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    app_id='APP_ID',  # 替换为实际的应用 ID
    prompt='你是谁？')

if response.status_code != HTTPStatus.OK:
    print(f'request_id={response.request_id}')
    print(f'code={response.status_code}')
    print(f'message={response.message}')
    print(f'请参考文档：https://help.aliyun.com/zh/model-studio/developer-reference/error-code')
else:
    print(response.output.text)
```

### Java 请求示例

```java
// 建议dashscope SDK的版本 >= 2.12.0
import com.alibaba.dashscope.app.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

public class Main {
    public static void appCall()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                // 若没有配置环境变量，可用百炼API Key将下行替换为：.apiKey("sk-xxx")。
                // 但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .appId("APP_ID")
                .prompt("你是谁？")
                .build();
        Application application = new Application();
        ApplicationResult result = application.call(param);
        System.out.printf("text: %s\n",
                result.getOutput().getText());
    }
    
    public static void main(String[] args) {
        try {
            appCall();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.err.println("message："+e.getMessage());
            System.out.println("请参考文档：https://help.aliyun.com/zh/model-studio/developer-reference/error-code");
        }
        System.exit(0);
    }
}
```

### HTTP curl 请求示例

```bash
curl -X POST https://dashscope.aliyuncs.com/api/v1/apps/APP_ID/completion \
--header "Authorization: Bearer $DASHSCOPE_API_KEY" \
--header 'Content-Type: application/json' \
--data '{
    "input": {
        "prompt": "你是谁？"
    },
    "parameters":  {},
    "debug": {}
}'
```

**注意**：将 `APP_ID` 替换为实际的应用 ID。

### PHP 请求示例

```php
<?php
# 若没有配置环境变量，可用百炼API Key将下行替换为：$api_key="sk-xxx"。
# 但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
$api_key = getenv("DASHSCOPE_API_KEY");
$application_id = 'APP_ID'; // 替换为实际的应用 ID
$url = "https://dashscope.aliyuncs.com/api/v1/apps/$application_id/completion";

// 构造请求数据
$data = [
    "input" => [
        'prompt' => '你是谁？'
    ]
];

// 将数据编码为 JSON
$dataString = json_encode($data);

// 检查 json_encode 是否成功
if (json_last_error() !== JSON_ERROR_NONE) {
    die("JSON encoding failed with error: " . json_last_error_msg());
}

// 初始化 cURL 对话
$ch = curl_init($url);

// 设置 cURL 选项
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "POST");
curl_setopt($ch, CURLOPT_POSTFIELDS, $dataString);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Content-Type: application/json',
    'Authorization: Bearer ' . $api_key
]);

// 执行请求
$response = curl_exec($ch);

// 检查 cURL 执行是否成功
if ($response === false) {
    die("cURL Error: " . curl_error($ch));
}

// 获取 HTTP 状态码
$status_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);

// 关闭 cURL 对话
curl_close($ch);

// 解码响应数据
$response_data = json_decode($response, true);

// 处理响应
if ($status_code == 200) {
    if (isset($response_data['output']['text'])) {
        echo "{$response_data['output']['text']}\n";
    } else {
        echo "No text in response.\n";
    }
} else {
    if (isset($response_data['request_id'])) {
        echo "request_id={$response_data['request_id']}\n";
    }
    echo "code={$status_code}\n";
    if (isset($response_data['message'])) {
        echo "message={$response_data['message']}\n";
    } else {
        echo "message=Unknown error\n";
    }
}
?>
```

### Node.js 请求示例

**需安装相关依赖**：
```bash
npm install axios
```

**请求示例**：
```javascript
const axios = require('axios');

async function callDashScope() {
    // 若没有配置环境变量，可用百炼API Key将下行替换为：apiKey='sk-xxx'。
    // 但不建议在生产环境中直接将API Key硬编码到代码中，以减少API Key泄露风险。
    const apiKey = process.env.DASHSCOPE_API_KEY;
    const appId = 'APP_ID';// 替换为实际的应用 ID
    const url = `https://dashscope.aliyuncs.com/api/v1/apps/${appId}/completion`;
    
    const data = {
        input: {
            prompt: "你是谁？"
        },
        parameters: {},
        debug: {}
    };
    
    try {
        const response = await axios.post(url, data, {
            headers: {
                'Authorization': `Bearer ${apiKey}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.status === 200) {
            console.log(`${response.data.output.text}`);
        } else {
            console.log(`request_id=${response.headers['request_id']}`);
            console.log(`code=${response.status}`);
            console.log(`message=${response.data.message}`);
        }
    } catch (error) {
        console.error(`Error calling DashScope: ${error.message}`);
        if (error.response) {
            console.error(`Response status: ${error.response.status}`);
            console.error(`Response data:`, error.response.data);
        }
    }
}

callDashScope();
```

## 请求参数

### HTTP 请求头

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `Authorization` | string | 是 | Bearer Token，格式：`Bearer $DASHSCOPE_API_KEY` |
| `Content-Type` | string | 是 | `application/json` |

### 请求体结构

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `input` | object | 是 | 输入参数对象 |
| `parameters` | object | 否 | 可选参数对象 |
| `debug` | object | 否 | 调试参数对象 |

### input 对象属性

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `prompt` | string | 是（单轮） | 用户输入的提示文本（单轮对话时使用） |
| `session_id` | string | 否 | 会话ID（多轮对话时使用） |
| `messages` | array | 否 | 消息历史数组（多轮对话时使用） |

**多轮对话说明**：
- 通过 `session_id` 和 `messages` 启用多轮对话
- `messages` 数组包含历史对话消息，格式：
  ```json
  [
    {"role": "user", "content": "用户消息"},
    {"role": "assistant", "content": "助手回复"}
  ]
  ```

### parameters 对象属性

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `temperature` | float | 否 | 控制输出的随机性，范围 0.0-2.0 |
| `top_p` | float | 否 | 核采样参数，范围 0.0-1.0 |
| `max_tokens` | integer | 否 | 最大生成 token 数 |
| `has_thoughts` | boolean | 否 | 是否返回思考过程（深度思考模型） |

### debug 对象属性

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| （待补充具体调试参数） | - | - | - |

## 响应参数

### 响应体结构

| 参数 | 类型 | 说明 |
|------|------|------|
| `request_id` | string | 请求的唯一标识符 |
| `output` | object | 输出结果对象 |
| `usage` | object | 本次请求使用的数据信息 |

### output 对象属性

| 参数 | 类型 | 说明 |
|------|------|------|
| `text` | string | 模型生成的文本内容 |
| `thoughts` | string | 模型的思考过程（当 has_thoughts 参数设为 True 时返回） |
| `reasoningContent` | string | 模型的思考过程（深度思考模型） |
| `action_type` | string | 大模型返回的执行步骤类型（如 API、agentRag、reasoning） |
| `action_name` | string | 执行的 action 名称（如知识检索、API插件、思考过程） |
| `action` | string | 执行的步骤 |
| `action_input_stream` | string | 入参的流式结果 |
| `action_input` | string | 插件的输入参数 |
| `observation` | string | 检索或插件的过程 |
| `doc_references` | array | 检索的召回文档中被模型引用的文档信息 |
| `images` | array | 模型引用的图片URL列表 |
| `page_number` | array | 模型引用的文本切片的页码（仅支持在2024年10月25日后创建的知识库） |
| `workflow_message` | object | 包含工作流节点状态和消息的对象 |

### doc_references 数组元素属性

| 参数 | 类型 | 说明 |
|------|------|------|
| `index_id` | string | 模型引用的召回文档索引，如[1] |
| `title` | string | 模型引用的文本切片标题 |
| `doc_id` | string | 模型引用的文档ID |
| `doc_name` | string | 模型引用的文档名 |
| `text` | string | 模型引用的具体文本内容 |
| `biz_id` | string | 模型引用的业务关联标识 |
| `images` | array | 模型引用的图片URL列表 |
| `page_number` | array | 模型引用的文本切片的页码 |

**注意**：`doc_references` 仅在百炼控制台的**智能体应用**内，打开**展示回答来源**开关并**发布**应用后，才可能包含有效信息。

### workflow_message 对象属性

| 参数 | 类型 | 说明 |
|------|------|------|
| `node_status` | string | 当前节点的执行状态，例如 executing（执行中） |
| `node_type` | string | 当前节点的类型，例如 End（结束节点） |
| `node_msg_seq_id` | integer | 节点内消息的序列号 |
| `node_name` | string | 当前节点的名称，例如结束 |
| `message` | object | 包含具体消息内容的对象 |
| `node_is_completed` | boolean | 指示当前节点是否已完成执行（true表示完成，false表示未完成） |
| `node_id` | string | 当前节点的唯一标识ID |

### message 对象属性

| 参数 | 类型 | 说明 |
|------|------|------|
| `content` | string | 消息的具体文本内容 |
| `role` | string | 消息发送者的角色，例如 Assistant |

### usage 对象属性

| 参数 | 类型 | 说明 |
|------|------|------|
| `models` | array | 本次调用的模型信息 |

### models 数组元素属性

| 参数 | 类型 | 说明 |
|------|------|------|
| `model_id` | string | 本次应用调用到的模型 ID |
| `input_tokens` | integer | 用户输入文本转换成Token后的长度 |
| `output_tokens` | integer | 模型生成回复转换为Token后的长度 |

## 特殊功能说明

### 深度思考模型

当在控制台**工作流应用**中选择了[深度思考模型](https://help.aliyun.com/zh/model-studio/deep-thinking#5be853b164zv4)，并成功发布应用后，若在 API 调用时将 `has_thoughts` 参数设为 `True`，则模型的思考过程将在 `reasoningContent` 字段中返回。

### 知识检索文档引用

在百炼控制台的**智能体应用**内，打开**展示回答来源**开关并**发布**应用，`doc_references` 才可能包含有效信息。

### 页码引用

`page_number` 参数仅支持在2024年10月25日后创建的知识库。如需使用该参数：
- Python Dashscope SDK 的版本至少应为 1.20.14
- Java Dashscope SDK 的版本至少应为 2.16.10

## 错误处理

如果调用失败并返回报错信息，请参阅错误信息进行解决。

错误响应格式：
```json
{
    "request_id": "请求ID",
    "code": "错误码",
    "message": "错误消息"
}
```

错误码参考文档：https://help.aliyun.com/zh/model-studio/developer-reference/error-code

## SDK 版本要求

- **Java DashScope SDK**：建议版本 >= 2.12.0
- **Python DashScope SDK**：
  - 使用页码引用功能：版本至少应为 1.20.14
- **Java DashScope SDK**：
  - 使用页码引用功能：版本至少应为 2.16.10

## 安全建议

1. **API Key 管理**：
   - 不建议在生产环境中直接将 API Key 硬编码到代码中
   - 建议使用环境变量存储 API Key
   - 减少 API Key 泄露风险

2. **环境变量配置**：
   - Python: `DASHSCOPE_API_KEY`
   - Java: `DASHSCOPE_API_KEY`
   - Node.js: `DASHSCOPE_API_KEY`

## 多轮对话

### session_id 与 memory_id 的区别

**session_id（多轮对话）**：
- 用于云端托管的多轮对话，自动维护对话上下文
- 有效期：1 小时
- 最大历史轮数：50
- 无需调用者自行维护上下文，但需在下一轮对话中传入上一轮对话的 `session_id`

**memory_id（长期记忆）**：
- 用于创建长期记忆体，存储特定信息
- 需调用 CreateMemory 接口创建，获取 `memoryId`
- 在后续对话中引用特定信息，需传入 `memoryId`

**总结**：两者分别服务于短期对话和长期信息存储。

### Python 多轮对话示例

```python
import os
from http import HTTPStatus
from dashscope import Application

# 第一轮对话
response1 = Application.call(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    app_id='YOUR_APP_ID',
    prompt='你好，我是张三'
)

session_id = response1.output.session_id  # 获取会话ID

# 第二轮对话（使用 session_id）
response2 = Application.call(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    app_id='YOUR_APP_ID',
    session_id=session_id,
    prompt='我刚才说了什么？'
)
```

### HTTP 多轮对话示例

```bash
# 第一轮
curl -X POST https://dashscope.aliyuncs.com/api/v1/apps/YOUR_APP_ID/completion \
--header "Authorization: Bearer $DASHSCOPE_API_KEY" \
--header 'Content-Type: application/json' \
--data '{
    "input": {
        "prompt": "你好，我是张三"
    }
}'

# 第二轮（使用返回的 session_id）
curl -X POST https://dashscope.aliyuncs.com/api/v1/apps/YOUR_APP_ID/completion \
--header "Authorization: Bearer $DASHSCOPE_API_KEY" \
--header 'Content-Type: application/json' \
--data '{
    "input": {
        "session_id": "SESSION_ID_FROM_FIRST_RESPONSE",
        "messages": [
            {"role": "user", "content": "你好，我是张三"},
            {"role": "assistant", "content": "助手回复"},
            {"role": "user", "content": "我刚才说了什么？"}
        ]
    }
}'
```

**相关文档**：
- [调用智能体应用-多轮对话](https://help.aliyun.com/zh/model-studio/call-single-agent-application#bb173820c5whx)
- [调用工作流应用-多轮对话](https://help.aliyun.com/zh/model-studio/invoke-workflow-application#6ca125d59eyc9)

## 流式输出

（待补充流式输出相关文档和示例）

**相关文档**：
- [调用智能体应用-流式输出](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)
- [调用工作流应用-流式输出](https://help.aliyun.com/zh/model-studio/invoke-workflow-application#6e644d5a7b3ia)

## 知识库检索

（待补充知识库检索相关文档和示例）

**相关文档**：
- [调用智能体应用-检索知识库](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)

## 长期记忆

长期记忆（memory_id）用于创建长期记忆体，存储特定信息。

**相关文档**：
- [长期记忆](https://help.aliyun.com/zh/model-studio/long-term-memory?spm=a2c4g.11186623.help-menu-2400256.d_1_7_0.1684297f7oNrzR&scm=20140722.H_2844738._.OR_help-T_cn~zh-V_1#a3398be1e7c1g)

**使用方式**：
1. 调用 CreateMemory 接口创建，获取 `memoryId`
2. 在后续对话中引用特定信息，需传入 `memoryId`

## 文件上传

（待补充文件上传相关文档和示例）

**相关文档**：
- [调用智能体应用-上传文件](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)

## 视觉理解

（待补充视觉理解相关文档和示例）

**相关文档**：
- [调用智能体应用-视觉理解](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)

## 传递参数

（待补充传递参数相关文档和示例）

**相关文档**：
- [调用智能体应用-传递参数](https://help.aliyun.com/zh/model-studio/call-single-agent-application#de63036b85aj0)

## 私网终端节点配置

如果需要在私网环境中调用 DashScope API，可以配置私网终端节点。

### Python 私网终端节点示例

```python
from dashscope import Application

# 配置私网终端节点
application = Application("https://ep-2zei6917b47eed******.dashscope.cn-beijing.privatelink.aliyuncs.com/api/v1/")

response = application.call(
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    app_id='YOUR_APP_ID',
    prompt='你是谁？'
)
```

### Java 私网终端节点示例

```java
// 配置私网终端节点
Application application = new Application("https://ep-2zei6917b47eed******.dashscope.cn-beijing.privatelink.aliyuncs.com/api/v1/");

ApplicationResult result = application.call(param);
```

### HTTP 私网终端节点示例

```bash
curl -X POST https://ep-2zei6917b47eed******.dashscope.cn-beijing.privatelink.aliyuncs.com/api/v1/apps/YOUR_APP_ID/completion \
--header "Authorization: Bearer $DASHSCOPE_API_KEY" \
--header 'Content-Type: application/json' \
--data '{
    "input": {
        "prompt": "你是谁？"
    }
}'
```

## 常见问题

### Java SDK 依赖问题

**问题**：运行 Java 代码示例时，出现类似 "java: 程序包com.alibaba.dashscope.app不存在" 的异常信息。

**解决方案**：

1. **检查导入语句**：确保导入语句中的类名和包名正确

2. **添加依赖库**：
   - **Maven**：确保 DashScope Java SDK 依赖库已添加到 `pom.xml`，且为最新版本
     ```xml
     <!-- https://mvnrepository.com/artifact/com.alibaba/dashscope-sdk-java -->
     <dependency>
         <groupId>com.alibaba</groupId>
         <artifactId>dashscope-sdk-java</artifactId>
         <version>在此处填写最新版本号，例如2.16.4</version>
     </dependency>
     ```
   - **Gradle**：确保依赖已添加到 `build.gradle`
     ```gradle
     // https://mvnrepository.com/artifact/com.alibaba/dashscope-sdk-java
     implementation group: 'com.alibaba', name: 'dashscope-sdk-java', version: '在此处填写最新版本号，例如2.16.4'
     ```
   - 访问 [Maven Repository](https://mvnrepository.com/artifact/com.alibaba/dashscope-sdk-java) 获取最新版本号

3. **升级 SDK**：旧版本的 DashScope Java SDK 可能不包含您尝试使用的功能或类。请确认您所使用的 DashScope Java SDK 是否为最新版，如果当前版本较低，请将其升级至最新版本

4. **重新加载项目**：使更改生效

5. **重新运行代码示例**

### session_id 与 memory_id 的区别

**问题**：多轮对话（session_id）与长期记忆（memory_id）有什么区别？

**答案**：
- **session_id**：用于云端托管的多轮对话，自动维护对话上下文。有效期1小时，最大历史轮数50。无需调用者自行维护上下文，但需在下一轮对话中传入上一轮对话的 `session_id`
- **memory_id**：用于创建长期记忆体，存储特定信息。需调用 CreateMemory 接口创建，获取 `memoryId`。在后续对话中引用特定信息，需传入 `memoryId`

两者分别服务于短期对话和长期信息存储。

## 完整相关链接索引

### 核心文档
- [DashScope API 参考（主文档）](https://help.aliyun.com/zh/model-studio/dashscope-api-reference)
- [获取 App ID 和 Workspace ID](https://help.aliyun.com/zh/model-studio/get-app-id-and-workspace-id)
- [Responses API 参考](https://help.aliyun.com/zh/model-studio/responses-api-reference)
- [应用调用 API 参考](https://help.aliyun.com/zh/model-studio/application-call-api-reference)

### 调用指南 - 智能体应用
- [调用智能体应用（主文档）](https://help.aliyun.com/zh/model-studio/call-single-agent-application)
- [调用智能体应用-单轮对话](https://help.aliyun.com/zh/model-studio/call-single-agent-application#b3be03a1ff21e)
- [调用智能体应用-多轮对话](https://help.aliyun.com/zh/model-studio/call-single-agent-application#bb173820c5whx)
- [调用智能体应用-传递参数](https://help.aliyun.com/zh/model-studio/call-single-agent-application#de63036b85aj0)
- [调用智能体应用-流式输出](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)
- [调用智能体应用-检索知识库](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)
- [调用智能体应用-长期记忆](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)
- [调用智能体应用-上传文件](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)
- [调用智能体应用-视觉理解](https://help.aliyun.com/zh/model-studio/call-single-agent-application#30619780ddy93)

### 调用指南 - 工作流应用
- [调用工作流应用（主文档）](https://help.aliyun.com/zh/model-studio/invoke-workflow-application)
- [调用工作流应用-多轮对话](https://help.aliyun.com/zh/model-studio/invoke-workflow-application#6ca125d59eyc9)
- [调用工作流应用-流式输出](https://help.aliyun.com/zh/model-studio/invoke-workflow-application#6e644d5a7b3ia)

### 功能特性
- [深度思考模型](https://help.aliyun.com/zh/model-studio/deep-thinking#5be853b164zv4)
- [长期记忆](https://help.aliyun.com/zh/model-studio/long-term-memory?spm=a2c4g.11186623.help-menu-2400256.d_1_7_0.1684297f7oNrzR&scm=20140722.H_2844738._.OR_help-T_cn~zh-V_1#a3398be1e7c1g)

### 错误与调试
- [错误码参考](https://help.aliyun.com/zh/model-studio/developer-reference/error-code)

### 控制台与密钥
- [百炼控制台](https://bailian.console.aliyun.com)
- [密钥管理](https://bailian.console.aliyun.com)
- [应用管理](https://bailian.console.aliyun.com)

### 相关功能文档
- [语音合成-CosyVoice/Sambert](https://help.aliyun.com/zh/model-studio/voice-synthesis-cosyvoice-sambert) - 将模型回复的文本信息转成语音
- [智能体应用](https://help.aliyun.com/zh/model-studio/agent-application) - 关于应用的构建和使用
- [Prompt工程](https://help.aliyun.com/zh/model-studio/prompt-engineering) - 关于应用内Prompt辅助工具的使用
- [10分钟给网站添加AI助手](https://help.aliyun.com/zh/model-studio/add-ai-assistant-to-website) - 在前端生产环境下使用

---

**文档来源**: [阿里云 DashScope API 参考](https://help.aliyun.com/zh/model-studio/dashscope-api-reference)  
**最后更新**: 2025-12-11  
**维护说明**: 本文档作为开发时的权威参考，应定期与官方文档同步更新

