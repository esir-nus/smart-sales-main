# 讯飞录音文件转写大模型 REST API 文档

> 来源：https://www.xfyun.cn/doc/spark/asr_llm/Ifasr_llm.html  
> 最后更新：2025-12-14

## 1. 接口说明

**注意**：该接口可正式使用。如您需要申请使用，请通过讯飞开放平台相关渠道领取免费额度或开通服务，确保获取有效的API密钥（AppID、APIKey、APISecret）后再调用。

该接口用于将音频文件（如WAV、MP3格式）转换为文本，支持最长音频时长需参考讯飞平台具体限制（示例中音频文件大小对应合理时长），适用于语音内容存档、字幕生成、语音指令识别等场景。

## 2. 请求说明

| 内容 | 说明 |
|------|------|
| **请求协议** | HTTPS（为保障数据安全，强制使用HTTPS） |
| **请求地址** | `https://office-api-ist-dx.iflyaisol.com` |
| **接口鉴权** | 签名机制（基于HMAC-SHA1算法），详情请参照下方接口鉴权 |
| **字符编码** | UTF-8 |
| **响应格式** | 统一采用JSON格式 |
| **开发语言** | 任意（支持发起HTTP POST请求的语言，如Python、Java、JavaScript等） |
| **音频属性** | 采样率：16kHz或8kHz；位长：16bit；声道：单声道（需与音频文件实际属性匹配） |
| **音频格式** | WAV、MP3（其他格式需提前确认平台支持情况） |
| **音频文件大小** | 示例中文件大小为3136940字节（约3MB，对应16kHz、16bit、单声道WAV格式时长约16秒），具体最大限制以讯飞平台文档为准 |

## 3. 接口鉴权

接口通过请求头上传 `signature` 鉴权参数，具体生成方式如下：

### 3.1 签名生成流程

#### 3.1.1 参数处理

1. 排除待签名参数中的 `signature` 字段
2. 对剩余参数按参数名进行自然排序（与Java TreeMap排序规则一致）

#### 3.1.2 构建基础字符串（baseString）

1. 对排序后的每个参数的键（key）和值（value）分别进行URL编码
   - 编码方式：标准URL编码，不保留特殊字符
   - 空值或空字符串不参与签名
2. 编码后的键值对格式：`encoded_key=encoded_value`
3. 所有键值对用 `&` 连接，形成完整baseString

#### 3.1.3 生成签名

1. 使用HMAC-SHA1算法对baseString进行加密
   - 密钥：`access_key_secret`（需UTF-8编码）
   - 待加密内容：baseString（需UTF-8编码）
2. 对加密结果进行Base64编码，得到最终签名（signature）

### 3.2 鉴权示例代码

```java
public static String signature(String accessKeySecret, Map<String, String> queryParam) throws Exception {
    TreeMap<String, String> treeMap = new TreeMap<>(queryParam);
    treeMap.remove("signature");
    
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> entry : treeMap.entrySet()) {
        String value = entry.getValue();
        if (value != null && !value.isEmpty()) {
            String encode = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
            builder.append(entry.getKey()).append("=").append(encode).append("&");
        }
    }
    
    if (builder.length() > 0) {
        builder.deleteCharAt(builder.length() - 1);
    }
    
    String baseString = builder.toString();
    System.out.println("baseString：" + baseString);
    
    Mac mac = Mac.getInstance("HmacSHA1");
    SecretKeySpec keySpec = new SecretKeySpec(accessKeySecret.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8.name());
    mac.init(keySpec);
    byte[] signBytes = mac.doFinal(baseString.getBytes(StandardCharsets.UTF_8));
    
    return Base64.getEncoder().encodeToString(signBytes);
}
```

## 4. 核心接口说明

非实时语音转写流程分为两步：**音频上传**（获取订单ID）和**结果查询**（通过订单ID获取转写文本），需依次调用两个接口。

### 4.1 音频上传接口（/v2/upload）

#### 4.1.1 接口功能

上传音频文件至讯飞服务器，服务器接收后返回唯一 `orderId`（订单ID），用于后续查询转写结果。

#### 4.1.2 请求方式

HTTP POST

#### 4.1.3 请求头

| 请求头字段 | 类型 | 是否必传 | 说明 |
|-----------|------|---------|------|
| Content-Type | string | 是 | 固定值：`application/octet-stream`（表示上传二进制音频文件） |
| signature | string | 是 | 基于请求参数计算的签名（签名生成规则见接口鉴权） |

#### 4.1.4 请求参数（URL查询参数）

所有参数需拼接在URL后，且部分参数需参与签名计算（未编码原始值），最终URL参数需做URL编码（特殊字符如 `:` 转 `%3A`、`+` 转 `%2B`）。

| 参数名称 | 类型 | 是否必传 | 取值范围/格式 | 描述 |
|---------|------|---------|--------------|------|
| appId | string | 是 | 讯飞平台分配的唯一应用ID（如37f3f2b5） | 用于标识调用的应用，需从控制台获取 |
| accessKeyId | string | 是 | 讯飞平台分配的访问密钥ID（如a296be0df88f701ec2e4882be7727568） | 用于签名计算的身份标识，需从控制台获取 |
| dateTime | string | 是 | `yyyy-MM-dd'T'HH:mm:ss±HHmm`（如2025-09-08T22:58:29+0800） | 请求发起的本地时间（带时区偏移，东八区为+0800），需严格匹配格式 |
| signatureRandom | string | 是 | 16位大小写字母+数字组合（如moI5WkopgjL1EL5Y） | 随机字符串，用于增加签名随机性，每次请求需重新生成 |
| fileSize | string | 是 | 正整数（单位：字节） | 音频文件的实际大小，需通过代码获取文件属性计算（如3136940） |
| fileName | string | 是 | 音频文件名（如lfasr_涉政.wav） | 需包含文件后缀，用于服务器识别音频格式 |
| duration | string | 否 | 正整数（单位：毫秒） | 音频时长，需与实际音频时长一致 |
| language | string | 是 | 可选范围：`autodialect`、`autominor` | `autodialect`：支持中英 + 202 种方言免切识别<br>`autominor`：支持 37 个语种免切识别（暂需联系人工对接） |
| pd | string | 否 | 领域参数 | 领域个性化参数，优化特定领域识别效果：<br>法律：court、金融：finance、医疗：medical、科技：tech、体育：sport、教育：edu、运营商：isp、政府：gov、游戏：game、电商：ecom、军事：mil、企业：com、生活：life、娱乐：ent、人文历史：culture、汽车：car |
| callbackUrl | String | 否 | 回调地址 | 订单完成时回调该地址通知完成<br>支持get请求，我们会在回调地址中拼接参数<br>`http://{ip}/{port}?xxx&OrderId=xxxx&status=1`<br>参数：orderId 为订单号，status 为订单状态（1-转写识别成功，-1转写识别失败）<br>长度限制512 |
| roleType | Short | 否 | [0、1、3] | 是否开启角色分离<br>0：不开启角色分离<br>1：通用角色分离<br>3：声纹角色分离（需要传递声纹标识）<br>注：该字段只有在开通了角色分离功能的前提下才会生效。不传默认为0 |
| roleNum | Short | 否 | [0,10] | 说话人数，取值范围，默认为 0 进行盲分<br>注：该字段只有在开通了角色分离功能的前提下才会生效，正确传入该参数后角色分离效果会有所提升 |
| featureIds | String | 否 | 已注册的声纹集合(拼接的字符串) | 该字段需要通过[声纹注册接口](https://www.xfyun.cn/doc/spark/asr_llm/voice_print.html)先注册声纹<br>声纹id集合，只有当roleType传3时该值才有效<br>多个用逗号分隔，最大支持64个声纹id<br>示例：`"20250918XX...XXq2Eg,20250919XX...XXo00"` |
| audioMode | String | 否 | 转写音频上传方式 | `fileStream`：文件流（默认）<br>`urlLink`：音频url外链 |
| audioUrl | String | 否 | 音频url外链地址 | 当audioMode为urlLink时该值必传<br>如果url中包含特殊字符，audioUrl需要UrlEncode（不包含签名时需要的UrlEncode）<br>长度限制512 |
| eng_smoothproc | boolean | 否 | 顺滑开关 | `true`：表示开启<br>`false`：表示关闭<br>默认为true |
| eng_colloqproc | boolean | 否 | 口语规整开关 | 口语规整是顺滑的升级版本<br>`true`：表示开启<br>`false`：表示关闭<br>默认为false<br><br>**组合效果说明**：<br>1. 当 eng_smoothproc 为 false，eng_colloqproc 为 false 时只返回原始转写结果<br>2. 当 eng_smoothproc 为 true，eng_colloqproc 为 false 时返回包含顺滑词的结果和原始结果<br>3. 当 eng_smoothproc 为 true，eng_colloqproc 为 true 时返回包含口语规整的结果和原始结果<br>4. 当 eng_smoothproc 为 false，eng_colloqproc 为 true 时返回包含口语规整的结果和原始结果 |
| eng_vad_mdn | int | 否 | 远近场模式 | 1：远场模式<br>2：近场模式<br>默认为1 |

#### 转写结果异步回调

当订单转写流程结束时会回调用户（如果录音文件转写接口upload传了callbackUrl），会把订单号和订单状态返回。

**回调格式**：
```
GET http://ip:port/server/xxx?orderId=DKHJQ202004291620042916580FBC96690001F&status=1
```

**参数说明**：

| 参数名 | 类型 | 是否必传 | 描述 |
|--------|------|---------|------|
| orderId | string | 是 | 转写订单号，用于查询结果 |
| status | string | 是 | 订单状态<br>-1：失败<br>1：成功<br><br>**注意**：<br>1. 成功需要调用getResult接口查询转写结果数据<br>2. 如果任务包含翻译环节且任务在转码、转写环节失败不会进行翻译的回调 |
| resultType | string | 否 | 回调任务类型：<br>为空或不包含该字段时为转写回调<br>转写：transfer<br>翻译：translate（即将开放，敬请期待）<br>质检：predict（即将开放，敬请期待）<br>语音语种识别：analysis |

#### 4.1.5 请求示例

```
https://office-api-ist-dx.iflyaisol.com/v2/upload?accessKeyId=testaccesskeyid&dateTime=2018-04-13T20%3A22%3A53%2B0800&duration=56352&fileName=1%E5%88%86%E9%92%9F%E9%9F%B3%E9%A2%91.mp3&fileSize=397144&language=en&signatureRandom=a0289d60-26b3-4601-ad18-d2179dc95fd2
```

#### 4.1.6 成功响应示例

```json
{
  "descInfo": "success",
  "code": "000000",
  "content": {
    "orderId": "DKHJQ2020042816200428163809B030D800083",
    "taskEstimateTime": 128000
  }
}
```

**响应参数通用说明**：

| 参数名称 | 类型 | 描述 |
|---------|------|------|
| code | int | 错误码：`"000000"`表示请求成功（业务状态需看content中的status）；其他值表示请求失败 |
| descInfo | string | 描述信息：成功时为`"success"`，失败时为具体错误原因（如`"dateTime format must be [yyyy-MM-dd'T'HH:mm:ssZ]"`） |
| content | object | 业务数据：返回orderId（订单唯一标识，用户后续获取转写结果）和 taskEstimateTime（预计转写时长） |

#### 4.1.7 失败响应示例

```json
{
  "code": 100003,
  "descInfo": "dateTime format must be [yyyy-MM-dd'T'HH:mm:ssZ]"
}
```

### 4.2 结果查询接口（/v2/getResult）

#### 4.2.1 接口功能

通过音频上传接口返回的 `orderId`，查询音频转写的进度和最终结果（支持轮询，直到转写完成）。

#### 4.2.2 请求方式

HTTP POST

#### 4.2.3 请求头

| 请求头字段 | 类型 | 是否必传 | 说明 |
|-----------|------|---------|------|
| Content-Type | string | 是 | 固定值：`application/json`（表示请求体为JSON格式） |
| signature | string | 是 | 基于请求参数计算的签名（签名生成规则与上传接口一致，见接口鉴权） |

#### 4.2.4 请求参数（URL查询参数）

所有参数需拼接在URL后，参与签名计算（未编码原始值），最终需做URL编码。

| 参数名称 | 类型 | 是否必传 | 取值范围/格式 | 描述 |
|---------|------|---------|--------------|------|
| accessKeyId | string | 是 | 与上传接口一致的访问密钥ID | 身份标识，用于签名计算 |
| dateTime | string | 是 | 与上传接口格式一致（`yyyy-MM-dd'T'HH:mm:ss±HHmm`） | 查询请求发起的时间，需重新生成（与上传接口的dateTime不同） |
| signatureRandom | string | 是 | 与上传接口一致的16位随机字符串 | 需与上传接口使用相同的随机串，确保请求关联性 |
| orderId | string | 是 | 上传接口返回的订单ID（如8a9f9999-xxxx-xxxx-xxxx-123456789abc） | 关联需查询的音频转写任务 |
| resultType | string | 是 | 固定值：`transfer,predict` | 表示查询转写结果，不可修改 |

#### 4.2.5 请求体

空JSON对象（需传递 `{}`，不可省略）

**请求示例**：
```
https://office-api-ist-dx.iflyaisol.com/v2/getResult?accessKeyId=testaccesskeyid&dateTime=2018-02-27T16%3A54%3A01%2B0800&orderId=DKHJQ20180208100000DD&signatureRandom=2bec2cc6-186a-414c-8c68-5d30a6fb14bb
```

#### 4.2.6 处理中响应示例

```json
{
  "code": "000000",
  "descInfo": "success",
  "content": {
    "orderInfo": {
      "orderId": "DKHJQ20250909161126126fxIy4kSppb6zi46H",
      "failType": 0,
      "status": 3,
      "originalDuration": 98028
    },
    "orderResult": "",
    "taskEstimateTime": 25000
  }
}
```

#### 处理完成的返回结果

```json
{
  "content": {
    "orderResult": "{\"lattice\":[{\"json_1best\":\"{\\\"st\\\":{\\\"pa\\\":\\\"0\\\",\\\"rt\\\":[{\\\"ws\\\":[{\\\"cw\\\":[{\\\"w\\\":\\\"为 \\\",\\\"wp\\\":\\\"n\\\",\\\"wc\\\":\\\"0.7511\\\"}],\\\"wb\\\":22,\\\"we\\\":75}]}],\\\"bg\\\":\\\"88 0\\\",\\\"rl\\\":\\\"1\\\",\\\"ed\\\":\\\"1680\\\"}}\"},{\"json_1best\":\"{\\\"st\\\":{\\\"pa\\\":\\\"0\\\",\\\"rt\\\":[{\\\"ws\\\":[{\\\"cw\\\":[{\\\"w\\\":\\\"喂 \\\",\\\"wp\\\":\\\"s\\\",\\\"wc\\\":\\\"0.9806\\\"}],\\\"wb\\\":19,\\\"we\\\":52},{\\\"cw\\\":[{\\\"w\\\":\\\"你好 \\\",\\\"wp\\\":\\\"n\\\",\\\"wc\\\":\\\"1.0000\\\"}],\\\"wb\\\":53,\\\"we\\\":111},{\\\"cw\\\":[{\\\"w\\\":\\\"｡ \\\",\\\"wp\\\":\\\"p\\\",\\\"wc\\\":\\\"0.0000\\\"}],\\\"wb\\\":111,\\\"we\\\":111}]}],\\\"bg\\\":\\\" 2390\\\",\\\"rl\\\":\\\"1\\\",\\\"ed\\\":\\\"3640\\\"}}\"}]}",
    "orderInfo": {
      "failType": 0,
      "status": 4,
      "orderId": "DKHJQ202003171520031715109E1FF5E50001D",
      "originalDuration": 14000
    }
  },
  "descInfo": "success",
  "code": "000000"
}
```

#### 响应参数说明

##### 返回参数

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| orderResult | String | 否 | 转写结果 |
| orderInfo | Object | 是 | 转写订单信息 |
| taskEstimateTime | Int | 是 | 订单预估耗时，单位毫秒 |
| transResult | List | 否 | 翻译结果，请参考TransResult |
| predictResult | String | 否 | 质检结果，请参考PredictResult |

##### orderInfo 对象

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| failType | - | - | 订单异常状态：<br>0：音频正常执行<br>1：音频上传失败<br>2：音频转码失败<br>3：音频识别失败<br>4：音频时长超限（最大音频时长为5小时）<br>5：音频校验失败（duration对应的值与真实音频时长不符合要求）<br>6：静音文件<br>7：翻译失败<br>8：账号无翻译权限<br>9：转写质检失败<br>10：转写质检未匹配出关键词<br>11：upload接口创建任务时，未开启质检或者翻译能力；备注: resultType=translate，未开启翻译能力；resultType=predict，未开启质检能力<br>12：音频语种分析失败<br>99：其他 |
| status | Int | 是 | 订单流程状态：<br>0：订单已创建<br>3：订单处理中<br>4：订单已完成<br>-1：订单失败 |
| orderId | String | 是 | 订单Id |
| originalDuration | Long | 是 | 原始音频时长，单位毫秒 |
| expireTime | Long | 否 | 订单结果过期时间，单位毫秒<br>备注: 1、查询转写结果和翻译结果时返回订单结果过期时间，质检结果不返回；2、订单转写成功或者翻译成功时返回过期时间字段 |
| language | String | 否 | 开启了语音语种分析能力，resultType传入的为analysis时返回识别的语种 |

##### orderResult文本内容

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| lattice | List | 是 | 做顺滑功能的识别结果 |
| lattice2 | List | 是 | 未做顺滑功能的识别结果，当开启顺滑和后语规整后orderResult才返回lattice2字段（需要开通权限） |
| label | Object | 否 | 转写结果标签信息，用于补充转写结果相关信息，目前开启双通道转写时该对象会返回，标记转写结果角色和声道的对应关系 |

##### Lattice包含的集合对象

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| json_1best | String | 是 | 单个vad的结果的json内容 |

##### json_1best对应的对象

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| st | Object | 是 | 单个句子的结果对象 |

##### st对象

| 参数名 | 类型 | 描述 |
|--------|------|------|
| bg | String | 单个句子的开始时间，单位毫秒 |
| ed | String | 单个句子的结束时间，单位毫秒 |
| rl | String | 分离的角色编号，取值正整数，需开启角色分离的功能才返回对应的分离角色编号 |
| rt | List | 输出词语识别结果集合 |

##### ws对象（词语候选识别结果）

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| wb | Long | 是 | 词语开始的帧数（注一帧10ms），位置是相对bg |
| we | Long | 是 | 词语结束的帧数（注一帧10ms），位置是相对bg |
| cw | List | 否 | 词语候选识别结果集合 |

##### cw包含的对象

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| w | String | 是 | 识别结果 |
| wp | String | 是 | 词语的属性，n：正常词；s：顺滑；p：标点；g：分段（按此标识进行分段） |

##### label 对象

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| rl_track | List | 否 | 双通道模式转写结果中角色和音频轨道对应信息，开启分轨模式该字段会返回 |

##### rl_track对象

| 参数名 | 描述 |
|--------|------|
| rl | 分离的角色编号，取值正整数 |
| track | 音频轨道信息，L：左声道，R：右声道 |

##### TransResult

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| segId | String | 是 | 段落序号 |
| dst | String | 是 | 翻译结果 |
| bg | int | 否 | 开始时间 |
| ed | int | 否 | 结束时间 |
| tags | List | 否 | 标签 |
| roles | List | 否 | 角色 |

##### PredictResult对象

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| keywords | List | 是 | 关键词相关信息，请参考：KeyWord对象 |

##### KeyWord对象

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| word | String | 是 | 质检关键词内容（开启质检后会输出内容） |
| label | String | 是 | 词库标签信息 |
| timeStamp | List | 是 | 质检关键词出现位置时间戳信息，请参考TimeStamp对象 |

##### TimeStamp对象

| 参数名 | 类型 | 必传 | 描述 |
|--------|------|------|------|
| bg | Long | 是 | 词出现的开启位置时间戳 |
| ed | Long | 是 | 词出现的结束位置时间戳 |

## 5. 语种列表

| 语种 | 描述 |
|------|------|
| **autodialect** | 自动识别中英，以及中文下的202种方言：<br>合肥话、芜湖话、皖北话、铜陵话、安庆话、黄山话、滁州话、六安话、池州话、宣城话、粤语、北京话、福州话、闽南语、莆仙话、延平话、宁德话、永安话、兰州话、白银话、天水话、武威话、张掖话、平凉话、酒泉话、庆阳话、定西话、韶关话、潮汕话、客家话、桂南平话、柳州话、桂北平话、桂林话、来宾话、贵阳话、六盘水话、遵义话、安顺话、毕节话、铜仁话、海口话、海南话、儋州话、石家庄话、唐山话、秦皇岛话、邯郸话、邢台话、保定话、张家口话、承德话、沧州话、廊坊话、衡水话、太原话、郑州话、开封话、洛阳话、平顶山话、安阳话、鹤壁话、新乡话、焦作话、濮阳话、许昌话、漯河话、三门峡话、南阳话、商丘话、信阳话、周口话、驻马店话、东北话、武汉话、黄石话、十堰话、宜昌话、襄阳话、鄂州话、荆门话、孝感话、荆州话、黄冈话、咸宁话、随州话、长沙话、湘潭话、衡阳话、邵阳话、岳阳话、常德话、张家界话、益阳话、郴州话、永州话、怀化话、娄底话、延吉话、南京话、无锡话、徐州话、常州话、苏州话、南通话、连云港话、淮安话、盐城话、扬州话、镇江话、泰州话、宿迁话、靖江话、启海话、南昌话、景德镇话、萍乡话、九江话、新余话、鹰潭话、赣州话、吉安话、宜春话、抚州话、上饶话、大连话、丹东话、营口话、朝阳话、晋语、包头话、赤峰话、鄂尔多斯话、银川话、石嘴山话、吴忠话、固原话、中卫话、西宁话、海东话、济南话、青岛话、淄博话、枣庄话、东营话、烟台话、潍坊话、济宁话、泰安话、威海话、日照话、临沂话、德州话、聊城话、滨州话、菏泽话、莱芜话、大同话、阳泉话、长治话、晋城话、朔州话、晋中话、运城话、忻州话、临汾话、吕梁话、汾阳话、西安话、铜川话、宝鸡话、咸阳话、渭南话、延安话、汉中话、榆林话、安康话、商洛话、上海话、四川话、台湾话、天津话、乌鲁木齐话、吐鲁番话、哈密话、云南话、曲靖话、玉溪话、保山话、昭通话、杭州话、宁波话、温州话、嘉兴话、湖州话、绍兴话、金华话、衢州话、舟山话、台州话、丽水话、重庆话 |
| **autominor** | 自动识别37种语种：<br>中文、英文、日语、韩语、俄语、法语、西班牙语、阿拉伯语、德语、泰语、越南语、印地语、葡萄牙语、意大利语、马来语、印尼语、菲律宾语、土耳其语、希腊语、捷克语、乌尔都语、孟加拉语、泰米尔语、乌克兰语、哈萨克语、乌兹别克语、波兰语、蒙语、斯瓦西里语、豪撒语、波斯语、荷兰语、瑞典语、罗马尼亚语、保加利亚语、维语、藏语 |

## 6. 代码示例

- [Python 示例代码](https://openres.xfyun.cn/xfyundoc/2025-11-27/6225ba1e-0354-4be4-93e0-9ef5b0eef6e7/1764252681118/Ifasr_llm.zip)
- [Java 示例代码](https://openres.xfyun.cn/xfyundoc/2025-10-11/b26ecfa2-6582-47a0-9fa1-a4810f559d5c/1760161937694/Ifasr_llm_java.zip)

## 7. 错误码说明

### 错误码对照表

| 错误码 | 描述 |
|--------|------|
| 000000 | 成功 |
| 999999 | 未知异常 |
| 000001 | 参数错误或不完整 |
| 000002 | accessKeyId不存在 |
| 1000000 | 不支持的操作 |
| 100001 | 订单不存在或状态异常 |
| 100002 | 订单音频未上传 |
| 100003 | 参数错误 |
| 100004 | 查询订单错误 |
| 100005 | 查询音频为空 |
| 100006 | 上传音频异常 |
| 100007 | 权限错误 |
| 100008 | 签名异常-请求时间超过限制 |
| 100009 | 签名校验不通过 |
| 100012 | 请求超过频率限制 |
| 100013 | 订单未完成 |
| 100015 | 热词必须是中文 |
| 100016 | 热词超出长度限制 |
| 100017 | 热词超出数量限制 |
| 100018 | 热词分隔符不能连续出现 |
| 100019 | 热词验证失败 |
| 100020 | 语言验证失败 |
| 100021 | 热词上传失败 |
| 100022 | 热词不断重复 |
| 100023 | 热词保存失败 |
| 100024 | 热词为空 |
| 100025 | 热词ID未知 |
| 100026 | 时间格式必须为：yy-MM-dd |
| 100027 | patch ID未知 |
| 100028 | Patch验证失败 |
| 100029 | 文件已存在 |
| 100030 | 未知的文件格式 |
| 100031 | 多候选ID未知 |
| 100032 | 多候选验证失败 |
| 100033 | 无效的角色分离个数，角色分离个数范围：[0-10] |
| 100034 | 更改AccesskeySecret失败 |
| 100037 | 非法的订单号 |
| 100038 | 删除订单验证失败 |
| 100039 | 订单为空 |
| 100040 | 订单个数超出限制 |
| 100041 | 切换通道失败 |
| 100042 | 外链地址无效 |
| 100043 | 通道类型验证失败 |
| 100044 | 通道类型不存在 |

## 最佳实践

1. **轮询策略**：调用上传接口后，使用返回的 `orderId` 定期查询结果（建议每5秒查询一次），直到 `status` 为 4（已完成）或 -1（失败）

2. **错误处理**：
   - 检查 `code` 字段，非 `"000000"` 表示请求失败
   - 检查 `orderInfo.status` 字段，判断订单处理状态
   - 检查 `orderInfo.failType` 字段，了解具体失败原因

3. **签名安全**：
   - 确保 `accessKeySecret` 不泄露
   - 每次请求生成新的 `signatureRandom`
   - 确保时间格式正确，避免签名过期

4. **音频要求**：
   - 推荐转写5分钟以上的音频文件
   - 避免上传大量短音频，易引起任务排队
   - 确保音频格式和属性与参数匹配

5. **回调处理**：
   - 如果使用回调，确保回调地址可访问
   - 回调为GET请求，需正确处理URL参数
   - 收到回调后仍需调用查询接口获取完整结果




