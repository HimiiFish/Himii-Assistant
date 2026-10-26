package com.himii.guga.agent

import com.himii.guga.settings.LlmConnectionSettings
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class ChatMessage(
    val role: String,
    val content: String? = null,
    val toolCallIdentifier: String? = null,
    val toolCallsJson: JSONArray? = null,
    val name: String? = null
)

data class ChatCompletionResult(
    val assistantMessage: ChatMessage,
    val finishReason: String?
)

class OpenAiCompatibleChatClient(
    private val connectionSettings: LlmConnectionSettings
) {

    fun completeWithTools(
        messages: List<ChatMessage>,
        toolDefinitions: List<AgentToolDefinition>
    ): ChatCompletionResult {
        val endpoint = buildChatCompletionsEndpoint(connectionSettings.baseUrl)
        val requestBody = JSONObject()
            .put("model", connectionSettings.modelName)
            .put("messages", messagesToJsonArray(messages))
            .put("tools", toolsToJsonArray(toolDefinitions))
            .put("tool_choice", "auto")

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${connectionSettings.apiKey}")
            setRequestProperty("Accept", "application/json")
        }

        try {
            connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                writer.write(requestBody.toString())
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                ?: ""
            if (responseCode !in 200..299) {
                throw IllegalStateException("LLM 请求失败 HTTP $responseCode：$responseBody")
            }

            val root = JSONObject(responseBody)
            val choices = root.optJSONArray("choices")
                ?: throw IllegalStateException("LLM 响应缺少 choices")
            if (choices.length() == 0) {
                throw IllegalStateException("LLM 响应 choices 为空")
            }
            val choice = choices.getJSONObject(0)
            val message = choice.getJSONObject("message")
            val toolCalls = message.optJSONArray("tool_calls")
            val content = if (message.isNull("content")) null else message.optString("content")

            return ChatCompletionResult(
                assistantMessage = ChatMessage(
                    role = "assistant",
                    content = content,
                    toolCallsJson = if (toolCalls != null && toolCalls.length() > 0) toolCalls else null
                ),
                finishReason = if (choice.has("finish_reason") && !choice.isNull("finish_reason")) {
                    choice.getString("finish_reason")
                } else {
                    null
                }
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun buildChatCompletionsEndpoint(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/v1")) {
            "$normalized/chat/completions"
        } else {
            "$normalized/v1/chat/completions"
        }
    }

    private fun messagesToJsonArray(messages: List<ChatMessage>): JSONArray {
        val jsonArray = JSONArray()
        messages.forEach { message ->
            val jsonObject = JSONObject().put("role", message.role)
            when (message.role) {
                "tool" -> {
                    jsonObject.put("tool_call_id", message.toolCallIdentifier)
                    jsonObject.put("content", message.content ?: "")
                    message.name?.let { jsonObject.put("name", it) }
                }
                else -> {
                    if (message.content != null) {
                        jsonObject.put("content", message.content)
                    } else if (message.toolCallsJson == null) {
                        jsonObject.put("content", "")
                    }
                    message.toolCallsJson?.let { jsonObject.put("tool_calls", it) }
                }
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray
    }

    private fun toolsToJsonArray(toolDefinitions: List<AgentToolDefinition>): JSONArray {
        val jsonArray = JSONArray()
        toolDefinitions.forEach { toolDefinition ->
            jsonArray.put(
                JSONObject()
                    .put("type", "function")
                    .put(
                        "function",
                        JSONObject()
                            .put("name", toolDefinition.toolName)
                            .put("description", toolDefinition.description)
                            .put("parameters", toolDefinition.parameterSchemaJson)
                    )
            )
        }
        return jsonArray
    }
}
