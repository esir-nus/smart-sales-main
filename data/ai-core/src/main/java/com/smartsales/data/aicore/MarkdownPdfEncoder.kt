package com.smartsales.data.aicore

import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/MarkdownPdfEncoder.kt
// 模块：:data:ai-core
// 说明：根据运行环境生成可读PDF（Android使用PdfDocument，JVM使用轻量写入器）
// 作者：创建于 2025-11-16
internal object MarkdownPdfEncoder {
    private val ORDERED_BULLET = Regex("^\\d+\\.\\s+.*")
    private const val PDF_HEADER = "%PDF-1.4\n"
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 64f
    private const val FONT_SIZE = 14f
    private const val LINE_GAP = 6f

    fun encode(markdown: String): ByteArray {
        val displayLines = extractDisplayLines(markdown)
        return if (isAndroidRuntime()) {
            AndroidPdfRenderer.render(displayLines)
        } else {
            BasicPdfRenderer.render(displayLines)
        }
    }

    private fun isAndroidRuntime(): Boolean {
        val runtimeName = System.getProperty("java.runtime.name") ?: return false
        return runtimeName.contains("Android", ignoreCase = true)
    }

    private fun extractDisplayLines(markdown: String): List<String> {
        return markdown
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split('\n')
            .map(::stripMarkdownPrefix)
            .map { if (it.isBlank()) " " else it }
            .ifEmpty { listOf(" ") }
    }

    private fun stripMarkdownPrefix(rawLine: String): String {
        val trimmed = rawLine.trimStart()
        if (trimmed.isEmpty()) return ""
        return when {
            trimmed.startsWith("#") -> trimmed.trimStart('#', ' ', '\t')
            trimmed.startsWith("- ") -> trimmed.drop(2).trimStart()
            trimmed.startsWith("* ") -> trimmed.drop(2).trimStart()
            ORDERED_BULLET.matches(trimmed) -> {
                val index = trimmed.indexOf('.')
                trimmed.substring(index + 1).trimStart()
            }
            trimmed.startsWith("> ") -> trimmed.drop(2).trimStart()
            else -> trimmed
        }
    }

    private object AndroidPdfRenderer {
        fun render(lines: List<String>): ByteArray {
            val doc = PdfDocument()
            val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                textSize = FONT_SIZE
                typeface = Typeface.SANS_SERIF
            }
            val fm = paint.fontMetrics
            val lineHeight = (fm.bottom - fm.top) + LINE_GAP
            val maxBaseline = PAGE_HEIGHT - MARGIN
            var baseline = MARGIN - fm.top
            var pageIndex = 1
            var page = newPage(doc, pageIndex)
            var canvas = page.canvas
            val textWidth = PAGE_WIDTH - MARGIN * 2

            lines.forEach { line ->
                val segments = wrapLine(line, paint, textWidth)
                val effectiveSegments = if (segments.isEmpty()) listOf(" ") else segments
                effectiveSegments.forEach { chunk ->
                    if (baseline > maxBaseline) {
                        doc.finishPage(page)
                        pageIndex += 1
                        page = newPage(doc, pageIndex)
                        canvas = page.canvas
                        baseline = MARGIN - fm.top
                    }
                    canvas.drawText(chunk, MARGIN, baseline, paint)
                    baseline += lineHeight
                }
            }

            doc.finishPage(page)
            val output = ByteArrayOutputStream()
            doc.writeTo(output)
            doc.close()
            return output.toByteArray()
        }

        private fun wrapLine(text: String, paint: TextPaint, maxWidth: Float): List<String> {
            if (text.isEmpty()) return emptyList()
            val chunks = mutableListOf<String>()
            var start = 0
            while (start < text.length) {
                val count = paint.breakText(text, start, text.length, true, maxWidth, null)
                if (count <= 0) break
                chunks.add(text.substring(start, start + count))
                start += count
            }
            return chunks
        }

        private fun newPage(doc: PdfDocument, index: Int): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), index).create()
            return doc.startPage(pageInfo)
        }
    }

    private object BasicPdfRenderer {
        fun render(lines: List<String>): ByteArray {
            val sanitized = lines.map(::escapeForPdf)
            val contentStream = buildContentStream(sanitized)
            return buildPdfDocument(contentStream)
        }

        private fun escapeForPdf(line: String): String {
            if (line.isEmpty()) return " "
            val builder = StringBuilder(line.length)
            for (ch in line) {
                val normalized = when {
                    ch == '\\' || ch == '(' || ch == ')' -> "\\$ch"
                    ch == '\t' -> "    "
                    ch.code in 32..126 -> ch.toString()
                    else -> "?"
                }
                builder.append(normalized)
            }
            return builder.toString()
        }

        private fun buildContentStream(lines: List<String>): ByteArray {
            val builder = StringBuilder()
            builder.append("BT\n")
                .append("/F1 12 Tf\n")
                .append("18 TL\n")
                .append("1 0 0 1 ${MARGIN.toInt()} ${(PAGE_HEIGHT - MARGIN).toInt()} Tm\n")
            lines.forEachIndexed { index, line ->
                if (index > 0) {
                    builder.append("T*\n")
                }
                builder.append("(").append(line).append(") Tj\n")
            }
            builder.append("ET\n")
            return builder.toString().toByteArray(StandardCharsets.UTF_8)
        }

        private fun buildPdfDocument(content: ByteArray): ByteArray {
            val output = ByteArrayOutputStream()
            val offsets = mutableListOf<Int>()

            fun write(text: String) {
                output.write(text.toByteArray(StandardCharsets.US_ASCII))
            }

            fun markOffset() {
                offsets.add(output.size())
            }

            write(PDF_HEADER)

            markOffset()
            write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

            markOffset()
            write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n")

            markOffset()
            write(
                "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_WIDTH $PAGE_HEIGHT] " +
                    "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
            )

            markOffset()
            write("4 0 obj\n<< /Length ${content.size} >>\nstream\n")
            output.write(content)
            write("endstream\nendobj\n")

            markOffset()
            write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n")

            val xrefOffset = output.size()
            write("xref\n0 6\n0000000000 65535 f \n")
            offsets.forEach {
                write(String.format(Locale.US, "%010d 00000 n \n", it))
            }
            write("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n")
            write(xrefOffset.toString())
            write("\n%%EOF")

            return output.toByteArray()
        }
    }
}
