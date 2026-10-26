package com.himii.guga.tts

import com.himii.guga.text.ReadablePlainTextFormatter
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * OpenAI 兼容：POST {base}/v1/audio/speech
 */
class OpenAiCompatibleTtsProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val modelName: String,
    private val voiceName: String
) : TtsProvider {

    override fun synthesizeToAudioBytes(text: String): ByteArray {
        val sanitizedText = ReadablePlainTextFormatter.toSpeechPlainText(text)
        if (sanitizedText.isBlank()) {
            throw IllegalStateException("播报文本为空")
        }

        val endpoint = buildSpeechEndpoint(baseUrl)
        val requestBody = JSONObject()
            .put("model", modelName)
            .put("input", sanitizedText)
            .put("voice", voiceName)
            .put("response_format", "mp3")

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "audio/mpeg, application/json")
        }

        try {
            connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                writer.write(requestBody.toString())
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream
                    ?.bufferedReader(StandardCharsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                throw IllegalStateException("兼容 TTS 失败 HTTP $responseCode：$errorBody")
            }
            return connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }

    private fun buildSpeechEndpoint(rawBaseUrl: String): String {
        val normalized = rawBaseUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/v1")) {
            "$normalized/audio/speech"
        } else {
            "$normalized/v1/audio/speech"
        }
    }
}
