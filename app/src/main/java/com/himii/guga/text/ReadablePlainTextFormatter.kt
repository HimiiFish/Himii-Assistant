package com.himii.guga.text

/**
 * 把 Markdown 收成适合语音播报的纯文本（界面展示请用 LightweightMarkdownText 渲染）。
 */
object ReadablePlainTextFormatter {

    fun toSpeechPlainText(rawText: String, maximumCharacterCount: Int = 450): String {
        var result = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("__([^_]+)__"), "$1")
            .replace(Regex("(?<!\\w)\\*([^*]+)\\*(?!\\w)"), "$1")
            .replace(Regex("(?<!\\w)_([^_]+)_(?!\\w)"), "$1")
            .replace(Regex("\\*\\*"), "")
            .replace(Regex("(?m)^#{1,6}\\s*"), "")
            .replace(Regex("`{1,3}"), "")
            .replace(Regex("(?m)^\\s*[-*•]\\s+"), "")
            .replace(Regex("(?m)^\\s*\\d+\\.\\s+"), "")
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
            .replace(Regex("[\\t\\x0B\\f]+"), " ")
            .replace(Regex("\\n{2,}"), "。")
            .replace(Regex("\\n"), "，")
            .replace(Regex("[ ]{2,}"), " ")
            .trim()
        if (result.length > maximumCharacterCount) {
            result = result.take(maximumCharacterCount) + "。后文省略。"
        }
        return result
    }
}
