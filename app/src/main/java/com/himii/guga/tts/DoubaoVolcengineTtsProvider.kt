package com.himii.guga.tts

import android.util.Base64
import android.util.Log
import com.himii.guga.text.ReadablePlainTextFormatter
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * 豆包 / 火山语音合成。
 * - 若填写了 App Id + Access Token：走 V1 一次性合成（语音合成控制台常见）
 * - 否则走 V3 HTTP Chunked（新版 API Key）
 */
class DoubaoVolcengineTtsProvider(
    private val apiKey: String,
    private val resourceIdentifier: String,
    private val speakerIdentifier: String,
    private val appIdentifier: String = "",
    private val accessToken: String = ""
) : TtsProvider {

    override fun synthesizeToAudioBytes(text: String): ByteArray {
        val sanitizedText = ReadablePlainTextFormatter.toSpeechPlainText(text)
        if (sanitizedText.isBlank()) {
            throw IllegalStateException("播报文本为空")
        }

        return if (appIdentifier.isNotBlank() && accessToken.isNotBlank()) {
            synthesizeWithV1(sanitizedText)
        } else if (apiKey.isNotBlank()) {
            synthesizeWithV3(sanitizedText)
        } else {
            throw IllegalStateException(
                "请填写「App Id + Access Token」（V1）或「API Key」（V3）至少一套"
            )
        }
    }

    /**
     * V1 非流式：https://openspeech.bytedance.com/api/v1/tts
     * 成功 code 通常为 3000。
     */
    private fun synthesizeWithV1(text: String): ByteArray {
        val requestBody = JSONObject()
            .put(
                "app",
                JSONObject()
                    .put("appid", appIdentifier)
                    .put("token", accessToken)
                    .put("cluster", "volcano_tts")
            )
            .put("user", JSONObject().put("uid", "guga-android"))
            .put(
                "audio",
                JSONObject()
                    .put("voice_type", speakerIdentifier)
                    .put("encoding", "mp3")
                    .put("speed_ratio", 1.0)
            )
            .put(
                "request",
                JSONObject()
                    .put("reqid", UUID.randomUUID().toString())
                    .put("text", text)
                    .put("operation", "query")
            )

        val connection = (URL(V1_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            // 注意：Bearer 与 token 之间是分号
            setRequestProperty("Authorization", "Bearer;$accessToken")
        }

        try {
            connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                writer.write(requestBody.toString())
            }
            val httpCode = connection.responseCode
            val body = readBody(connection, httpCode)
            Log.i(LOG_TAG, "V1 HTTP $httpCode bodyPreview=${body.take(240)}")

            val jsonObject = runCatching { JSONObject(body) }.getOrElse {
                throw IllegalStateException(
                    "豆包 V1 响应不是 JSON（HTTP $httpCode）：${body.take(200)}"
                )
            }
            val code = jsonObject.optInt("code", Int.MIN_VALUE)
            val message = jsonObject.optString("message")
            // 3000 = Success；部分网关也可能用 0
            if (code != 3000 && code != 0) {
                throw IllegalStateException(
                    "豆包 V1 TTS 失败 code=$code：$message；请检查 AppId/Token/音色。原文：${body.take(180)}"
                )
            }
            val data = jsonObject.optString("data")
            if (data.isBlank() || data == "null") {
                throw IllegalStateException("豆包 V1 成功但无音频 data，原文：${body.take(180)}")
            }
            return Base64.decode(data, Base64.DEFAULT)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * V3 流式：https://openspeech.bytedance.com/api/v3/tts/unidirectional
     */
    private fun synthesizeWithV3(text: String): ByteArray {
        val requestBody = JSONObject()
            .put("user", JSONObject().put("uid", "guga-android"))
            .put(
                "req_params",
                JSONObject()
                    .put("text", text)
                    .put("speaker", speakerIdentifier)
                    .put(
                        "audio_params",
                        JSONObject()
                            .put("format", "mp3")
                            .put("sample_rate", 24000)
                            // 略降语速，减少合成端「赶字」带来的顿挫感
                            .put("speech_rate", 0)
                    )
            )

        val connection = (URL(V3_ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Api-Key", apiKey)
            setRequestProperty("X-Api-Resource-Id", resourceIdentifier)
            setRequestProperty("X-Api-Request-Id", UUID.randomUUID().toString())
            setRequestProperty("Accept", "application/json, text/event-stream")
        }

        try {
            connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                writer.write(requestBody.toString())
            }
            val httpCode = connection.responseCode
            val body = readBody(connection, httpCode)
            Log.i(LOG_TAG, "V3 HTTP $httpCode bodyPreview=${body.take(240)}")

            if (httpCode !in 200..299) {
                throw IllegalStateException(
                    "豆包 V3 HTTP $httpCode：${body.take(220)}。若你只有 AppId/Token，请改填 V1 字段。"
                )
            }

            val audioBuffer = ByteArrayOutputStream()
            val jsonChunks = extractJsonObjects(body)
            if (jsonChunks.isEmpty()) {
                throw IllegalStateException(
                    "豆包 V3 未解析到 JSON 块：${body.take(220)}"
                )
            }

            var sawTerminalSuccess = false
            jsonChunks.forEach { jsonObject ->
                if (!jsonObject.has("code")) {
                    // 可能是网关包装或字幕片段，忽略无 code 的块
                    return@forEach
                }
                val code = jsonObject.getInt("code")
                when (code) {
                    0 -> {
                        val data = jsonObject.opt("data")
                        if (data is String && data.isNotBlank() && data != "null") {
                            audioBuffer.write(Base64.decode(data, Base64.DEFAULT))
                        }
                    }
                    20000000 -> sawTerminalSuccess = true
                    else -> {
                        val message = jsonObject.optString("message", "未知错误")
                        throw IllegalStateException(
                            "豆包 V3 TTS 失败 code=$code：$message；原文：${jsonObject.toString().take(180)}"
                        )
                    }
                }
            }

            if (audioBuffer.size() == 0) {
                throw IllegalStateException(
                    "豆包 V3 未返回音频。terminal=$sawTerminalSuccess；请检查 Resource Id / 音色权限。预览：${body.take(220)}"
                )
            }
            return audioBuffer.toByteArray()
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection, httpCode: Int): String {
        val stream = if (httpCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
    }

    /**
     * 兼容：换行 JSON、SSE data:、以及粘连的多个 JSON 对象。
     */
    private fun extractJsonObjects(rawBody: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        val lines = rawBody.split('\n')
        lines.forEach { line ->
            var trimmed = line.trim()
            if (trimmed.isEmpty()) {
                return@forEach
            }
            if (trimmed.startsWith("data:")) {
                trimmed = trimmed.removePrefix("data:").trim()
            }
            if (trimmed == "[DONE]") {
                return@forEach
            }
            runCatching { JSONObject(trimmed) }.onSuccess { results.add(it) }
        }
        if (results.isNotEmpty()) {
            return results
        }

        // 整包是一个 JSON
        runCatching { JSONObject(rawBody.trim()) }.onSuccess {
            return listOf(it)
        }

        // 粘连 JSON：{...}{...}
        var index = 0
        val characters = rawBody.toCharArray()
        while (index < characters.size) {
            while (index < characters.size && characters[index] != '{') {
                index += 1
            }
            if (index >= characters.size) {
                break
            }
            var depth = 0
            val start = index
            while (index < characters.size) {
                when (characters[index]) {
                    '{' -> depth += 1
                    '}' -> {
                        depth -= 1
                        if (depth == 0) {
                            val slice = rawBody.substring(start, index + 1)
                            runCatching { JSONObject(slice) }.onSuccess { results.add(it) }
                            index += 1
                            break
                        }
                    }
                }
                index += 1
            }
        }
        return results
    }

    companion object {
        private const val LOG_TAG = "GugaDoubaoTts"
        private const val V1_ENDPOINT = "https://openspeech.bytedance.com/api/v1/tts"
        private const val V3_ENDPOINT =
            "https://openspeech.bytedance.com/api/v3/tts/unidirectional"
    }
}
