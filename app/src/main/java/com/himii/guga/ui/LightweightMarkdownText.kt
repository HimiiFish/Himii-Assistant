package com.himii.guga.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * 轻量 Markdown 展示：标题、粗体、斜体、列表、段落。
 * 不引入额外依赖；复杂语法会尽量按纯文本可读方式落下。
 */
@Composable
fun LightweightMarkdownText(
    markdownText: String,
    modifier: Modifier = Modifier,
    bodyTextStyle: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val blocks = remember(markdownText) {
        parseMarkdownBlocks(markdownText)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    Text(
                        text = buildInlineAnnotatedString(block.text),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            else -> MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Text(
                        text = buildAnnotatedString {
                            append("• ")
                            append(buildInlineAnnotatedString(block.text))
                        },
                        style = bodyTextStyle,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                is MarkdownBlock.NumberedItem -> {
                    Text(
                        text = buildAnnotatedString {
                            append("${block.number}. ")
                            append(buildInlineAnnotatedString(block.text))
                        },
                        style = bodyTextStyle,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineAnnotatedString(block.text),
                        style = bodyTextStyle,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                MarkdownBlock.BlankLine -> {
                    // 段落间距已由上下 padding 承担
                }
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data object BlankLine : MarkdownBlock()
}

private fun parseMarkdownBlocks(markdownText: String): List<MarkdownBlock> {
    val normalized = markdownText.replace("\r\n", "\n").replace("\r", "\n")
    val lines = normalized.split('\n')
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = StringBuilder()

    fun flushParagraph() {
        val paragraphText = paragraphBuffer.toString().trim()
        if (paragraphText.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphText))
        }
        paragraphBuffer.clear()
    }

    lines.forEach { rawLine ->
        val line = rawLine.trimEnd()
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> {
                flushParagraph()
                blocks.add(MarkdownBlock.BlankLine)
            }

            trimmed.matches(Regex("^#{1,6}\\s+.+")) -> {
                flushParagraph()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                val text = trimmed.drop(level).trim()
                blocks.add(MarkdownBlock.Heading(level = level, text = text))
            }

            trimmed.matches(Regex("^[-*•]\\s+.+")) -> {
                flushParagraph()
                val text = trimmed.replace(Regex("^[-*•]\\s+"), "")
                blocks.add(MarkdownBlock.BulletItem(text = text))
            }

            trimmed.matches(Regex("^\\d+\\.\\s+.+")) -> {
                flushParagraph()
                val number = trimmed.substringBefore('.').toIntOrNull() ?: 1
                val text = trimmed.replace(Regex("^\\d+\\.\\s+"), "")
                blocks.add(MarkdownBlock.NumberedItem(number = number, text = text))
            }

            else -> {
                if (paragraphBuffer.isNotEmpty()) {
                    paragraphBuffer.append(' ')
                }
                paragraphBuffer.append(trimmed)
            }
        }
    }
    flushParagraph()
    return blocks.filterNot { it is MarkdownBlock.BlankLine && blocks.size == 1 }
}

private fun buildInlineAnnotatedString(sourceText: String): AnnotatedString {
    return buildAnnotatedString {
        // 先处理链接 [text](url) → text，再处理粗体/斜体
        val withoutLinks = sourceText.replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        var index = 0
        val characters = withoutLinks
        while (index < characters.length) {
            when {
                characters.startsWith("**", index) -> {
                    val closeIndex = characters.indexOf("**", index + 2)
                    if (closeIndex > index) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stripNestedMarkers(characters.substring(index + 2, closeIndex)))
                        }
                        index = closeIndex + 2
                    } else {
                        append("**")
                        index += 2
                    }
                }

                characters.startsWith("__", index) -> {
                    val closeIndex = characters.indexOf("__", index + 2)
                    if (closeIndex > index) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stripNestedMarkers(characters.substring(index + 2, closeIndex)))
                        }
                        index = closeIndex + 2
                    } else {
                        append("__")
                        index += 2
                    }
                }

                characters[index] == '*' -> {
                    val closeIndex = characters.indexOf('*', index + 1)
                    if (closeIndex > index) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(characters.substring(index + 1, closeIndex))
                        }
                        index = closeIndex + 1
                    } else {
                        append('*')
                        index += 1
                    }
                }

                characters[index] == '`' -> {
                    val closeIndex = characters.indexOf('`', index + 1)
                    if (closeIndex > index) {
                        append(characters.substring(index + 1, closeIndex))
                        index = closeIndex + 1
                    } else {
                        append('`')
                        index += 1
                    }
                }

                else -> {
                    append(characters[index])
                    index += 1
                }
            }
        }
    }
}

private fun stripNestedMarkers(text: String): String {
    return text.replace(Regex("[*_`]"), "")
}
