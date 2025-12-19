// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/xfyun/XfyunAsrApiUploadParamsTest.kt
// 模块：:data:ai-core
// 说明：锁定 /v2/upload 参数构造规则（包含 eng_smoothproc 且参与签名）
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.xfyun

import com.smartsales.data.aicore.debug.XfyunTraceStore
import com.smartsales.data.aicore.params.AiParaSettingsProvider
import com.smartsales.data.aicore.params.AiParaSettingsSnapshot
import com.smartsales.data.aicore.params.XfyunUploadSettings
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class XfyunAsrApiUploadParamsTest {

    @Test
    fun buildUploadParams_containsDocUploadParams() {
        val dumpDir = Files.createTempDirectory("xfyun_raw_dump").toFile().apply { deleteOnExit() }
        val temp = File.createTempFile("xfyun", ".wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        val api = XfyunAsrApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
            traceStore = XfyunTraceStore(),
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            },
            rawResponseDumper = XfyunRawResponseDumper(
                directoryProvider = object : XfyunRawDumpDirectoryProvider {
                    override fun directory(): File = dumpDir
                }
            ),
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            XfyunUploadSettings::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            java.util.List::class.java,
            String::class.java,
            Long::class.javaObjectType,
            String::class.java,
            String::class.java,
            String::class.java,
            XfyunParamStrategy::class.java,
        ).apply {
            isAccessible = true
        }

        val credentials = XfyunCredentials(
            appId = "app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            baseUrl = "https://example.com",
        )

        val uploadSettings = XfyunUploadSettings(
            language = "autominor",
            pd = "finance",
            roleType = 1,
            roleNum = 2,
            engSmoothProc = true,
            engColloqProc = true,
            engVadMdn = 2,
            audioMode = "fileStream",
        )

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            "autominor",
            uploadSettings,
            1,
            2,
            emptyList<String>(),
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        // 锁定：/v2/upload 的关键参数构造稳定；language 按产品约束固定为 autodialect
        assertEquals("autominor", params["language"])
        assertEquals("finance", params["pd"])
        assertEquals("1", params["roleType"])
        assertEquals("2", params["roleNum"])
        assertEquals("fileStream", params["audioMode"])
        assertEquals("true", params["eng_smoothproc"])
        assertEquals("true", params["eng_colloqproc"])
        assertEquals("2", params["eng_vad_mdn"])
        assertEquals("transfer", params["resultType"])
    }

    @Test
    fun buildUploadParams_includesLanguageAutodialect_whenNull() {
        val dumpDir = Files.createTempDirectory("xfyun_raw_dump").toFile().apply { deleteOnExit() }
        val temp = File.createTempFile("xfyun", ".wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        val api = XfyunAsrApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
            traceStore = XfyunTraceStore(),
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            },
            rawResponseDumper = XfyunRawResponseDumper(
                directoryProvider = object : XfyunRawDumpDirectoryProvider {
                    override fun directory(): File = dumpDir
                }
            ),
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            XfyunUploadSettings::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            java.util.List::class.java,
            String::class.java,
            Long::class.javaObjectType,
            String::class.java,
            String::class.java,
            String::class.java,
            XfyunParamStrategy::class.java,
        ).apply {
            isAccessible = true
        }

        val credentials = XfyunCredentials(
            appId = "app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            baseUrl = "https://example.com",
        )

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            null,
            XfyunUploadSettings(),
            1,
            0,
            emptyList<String>(),
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        assertEquals("autodialect", params["language"])
    }

    @Test
    fun normalizeLanguage_sanitizesToSupportedValues() {
        val dumpDir = Files.createTempDirectory("xfyun_raw_dump").toFile().apply { deleteOnExit() }
        val api = XfyunAsrApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
            traceStore = XfyunTraceStore(),
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            },
            rawResponseDumper = XfyunRawResponseDumper(
                directoryProvider = object : XfyunRawDumpDirectoryProvider {
                    override fun directory(): File = dumpDir
                }
            ),
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "normalizeLanguage",
            String::class.java,
        ).apply { isAccessible = true }

        assertEquals("autominor", method.invoke(api, "autominor") as String)
        assertEquals("autodialect", method.invoke(api, "zh-CN") as String)
        assertEquals("en", method.invoke(api, "en") as String)
    }

    @Test
    fun buildUploadParams_passthroughsUnknownLanguage_whenNotLegacy() {
        val dumpDir = Files.createTempDirectory("xfyun_raw_dump").toFile().apply { deleteOnExit() }
        val temp = File.createTempFile("xfyun", ".wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        val api = XfyunAsrApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
            traceStore = XfyunTraceStore(),
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            },
            rawResponseDumper = XfyunRawResponseDumper(
                directoryProvider = object : XfyunRawDumpDirectoryProvider {
                    override fun directory(): File = dumpDir
                }
            ),
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            XfyunUploadSettings::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            java.util.List::class.java,
            String::class.java,
            Long::class.javaObjectType,
            String::class.java,
            String::class.java,
            String::class.java,
            XfyunParamStrategy::class.java,
        ).apply {
            isAccessible = true
        }

        val credentials = XfyunCredentials(
            appId = "app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            baseUrl = "https://example.com",
        )

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            "en",
            XfyunUploadSettings(language = "en"),
            1,
            0,
            emptyList<String>(),
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        assertEquals("en", params["language"])
    }

    @Test
    fun buildUploadParams_sanitizesCnLanguage_toAutodialect() {
        val dumpDir = Files.createTempDirectory("xfyun_raw_dump").toFile().apply { deleteOnExit() }
        val temp = File.createTempFile("xfyun", ".wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        val api = XfyunAsrApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
            traceStore = XfyunTraceStore(),
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            },
            rawResponseDumper = XfyunRawResponseDumper(
                directoryProvider = object : XfyunRawDumpDirectoryProvider {
                    override fun directory(): File = dumpDir
                }
            ),
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            XfyunUploadSettings::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            java.util.List::class.java,
            String::class.java,
            Long::class.javaObjectType,
            String::class.java,
            String::class.java,
            String::class.java,
            XfyunParamStrategy::class.java,
        ).apply {
            isAccessible = true
        }

        val credentials = XfyunCredentials(
            appId = "app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            baseUrl = "https://example.com",
        )

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            "cn",
            XfyunUploadSettings(language = "cn"),
            1,
            0,
            emptyList<String>(),
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        assertEquals("autodialect", params["language"])
    }

    @Test
    fun buildUploadParams_includesVoiceprintFeatureIds_whenProvided() {
        val dumpDir = Files.createTempDirectory("xfyun_raw_dump").toFile().apply { deleteOnExit() }
        val temp = File.createTempFile("xfyun", ".wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        val api = XfyunAsrApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
            traceStore = XfyunTraceStore(),
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            },
            rawResponseDumper = XfyunRawResponseDumper(
                directoryProvider = object : XfyunRawDumpDirectoryProvider {
                    override fun directory(): File = dumpDir
                }
            ),
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            XfyunUploadSettings::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            java.util.List::class.java,
            String::class.java,
            Long::class.javaObjectType,
            String::class.java,
            String::class.java,
            String::class.java,
            XfyunParamStrategy::class.java,
        ).apply {
            isAccessible = true
        }

        val credentials = XfyunCredentials(
            appId = "app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            baseUrl = "https://example.com",
        )

        val uploadSettings = XfyunUploadSettings(
            roleType = 0,
            roleNum = 0,
        )

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            "autodialect",
            uploadSettings,
            3,
            0,
            listOf("vp_a", "  ", "vp_b"),
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        assertEquals("3", params["roleType"])
        assertEquals("0", params["roleNum"])
        assertEquals("vp_a,vp_b", params["featureIds"])
    }

    @Test
    fun buildUploadParams_omitsBlankPd() {
        val dumpDir = Files.createTempDirectory("xfyun_raw_dump").toFile().apply { deleteOnExit() }
        val temp = File.createTempFile("xfyun", ".wav").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
            deleteOnExit()
        }
        val api = XfyunAsrApi(
            httpClient = XfyunHttpClient(),
            configProvider = XfyunConfigProvider(
                object : AiParaSettingsProvider {
                    override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
                }
            ),
            traceStore = XfyunTraceStore(),
            aiParaSettingsProvider = object : AiParaSettingsProvider {
                override fun snapshot(): AiParaSettingsSnapshot = AiParaSettingsSnapshot()
            },
            rawResponseDumper = XfyunRawResponseDumper(
                directoryProvider = object : XfyunRawDumpDirectoryProvider {
                    override fun directory(): File = dumpDir
                }
            ),
        )

        val method = XfyunAsrApi::class.java.getDeclaredMethod(
            "buildUploadParams",
            XfyunCredentials::class.java,
            File::class.java,
            String::class.java,
            XfyunUploadSettings::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            java.util.List::class.java,
            String::class.java,
            Long::class.javaObjectType,
            String::class.java,
            String::class.java,
            String::class.java,
            XfyunParamStrategy::class.java,
        ).apply {
            isAccessible = true
        }

        val credentials = XfyunCredentials(
            appId = "app",
            accessKeyId = "id",
            accessKeySecret = "secret",
            baseUrl = "https://example.com",
        )

        val uploadSettings = XfyunUploadSettings(pd = "   ")

        @Suppress("UNCHECKED_CAST")
        val params = method.invoke(
            api,
            credentials,
            temp,
            "autodialect",
            uploadSettings,
            1,
            0,
            emptyList<String>(),
            "transfer",
            null,
            "rand16",
            "2025-12-16T00:00:00+0800",
            "1234567890",
            XfyunParamStrategy.DOC_FIRST,
        ) as Map<String, String>

        // 重要：空 pd 不能出现在最终 URL 中，否则会导致签名/URL 不一致。
        assertFalse(params.containsKey("pd"))
    }
}
