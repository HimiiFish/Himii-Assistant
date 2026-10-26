package com.himii.guga.agent

import android.util.Log
import com.himii.guga.agent.tools.GetAirQualityTool
import com.himii.guga.agent.tools.GetUserProfileTool
import com.himii.guga.agent.tools.GetWeatherForecastTool
import com.himii.guga.location.HomeLocationRepository
import com.himii.guga.settings.LlmConnectionSettings
import com.himii.guga.settings.UserProfileRepository
import com.himii.guga.weather.OpenMeteoWeatherClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class MorningBriefingAgentResult(
    val guideText: String,
    val usedToolNames: List<String>,
    val stepCount: Int
)

class MorningBriefingAgent(
    private val connectionSettings: LlmConnectionSettings,
    homeLocationRepository: HomeLocationRepository,
    userProfileRepository: UserProfileRepository,
    openMeteoWeatherClient: OpenMeteoWeatherClient = OpenMeteoWeatherClient()
) {

    private val tools: List<AgentTool> = listOf(
        GetWeatherForecastTool(homeLocationRepository, openMeteoWeatherClient),
        GetAirQualityTool(homeLocationRepository, openMeteoWeatherClient),
        GetUserProfileTool(homeLocationRepository, userProfileRepository)
    )

    private val toolsByName = tools.associateBy { it.definition.toolName }

    suspend fun generateMorningBriefing(): MorningBriefingAgentResult {
        if (!connectionSettings.isConfigured()) {
            throw IllegalStateException("请先在「模型设置」中填写 Base URL、API Key 与模型名")
        }

        return withContext(Dispatchers.IO) {
            withTimeout(OVERALL_TIMEOUT_MILLIS) {
                runReactLoop()
            }
        }
    }

    private fun runReactLoop(): MorningBriefingAgentResult {
        val chatClient = OpenAiCompatibleChatClient(connectionSettings)
        val messages = mutableListOf(
            ChatMessage(role = "system", content = SYSTEM_PROMPT),
            ChatMessage(
                role = "user",
                content = "请根据工具返回的真实数据，生成今天的出门指南。务必先调用工具取数，再给结论。"
            )
        )

        val usedToolNames = mutableListOf<String>()
        var stepCount = 0

        while (stepCount < MAXIMUM_STEP_COUNT) {
            stepCount += 1
            Log.i(LOG_TAG, "Agent step=$stepCount")

            val completion = chatClient.completeWithTools(
                messages = messages,
                toolDefinitions = tools.map { it.definition }
            )
            val assistantMessage = completion.assistantMessage
            messages.add(assistantMessage)

            val toolCalls = assistantMessage.toolCallsJson
            if (toolCalls == null || toolCalls.length() == 0) {
                val finalText = assistantMessage.content?.trim().orEmpty()
                if (finalText.isBlank()) {
                    throw IllegalStateException("模型未返回出门指南正文")
                }
                return MorningBriefingAgentResult(
                    guideText = finalText,
                    usedToolNames = usedToolNames.distinct(),
                    stepCount = stepCount
                )
            }

            for (index in 0 until toolCalls.length()) {
                val toolCall = toolCalls.getJSONObject(index)
                val toolCallIdentifier = toolCall.getString("id")
                val functionObject = toolCall.getJSONObject("function")
                val toolName = functionObject.getString("name")
                val argumentsRaw = functionObject.optString("arguments", "{}")
                val argumentsJson = runCatching { JSONObject(argumentsRaw) }
                    .getOrElse { JSONObject() }

                val tool = toolsByName[toolName]
                val toolResultText = if (tool == null) {
                    JSONObject().put("error", "未知工具：$toolName").toString()
                } else {
                    usedToolNames.add(toolName)
                    runCatching { tool.execute(argumentsJson) }
                        .getOrElse { error ->
                            JSONObject()
                                .put("error", error.message ?: "工具执行失败")
                                .toString()
                        }
                }

                messages.add(
                    ChatMessage(
                        role = "tool",
                        content = toolResultText,
                        toolCallIdentifier = toolCallIdentifier,
                        name = toolName
                    )
                )
            }
        }

        throw IllegalStateException("Agent 超过最大步数 $MAXIMUM_STEP_COUNT，已中止")
    }

    companion object {
        private const val LOG_TAG = "GugaMorningAgent"
        private const val MAXIMUM_STEP_COUNT = 6
        private const val OVERALL_TIMEOUT_MILLIS = 90_000L

        private val SYSTEM_PROMPT = """
你是「咕嘎」，二次元风格的晨间出门小助手。语气可爱、简洁，但事实必须准确。

硬性规则：
1. 温度、降水、空气质量等数字只能来自工具返回，禁止编造。
2. 生成指南前，至少调用 get_weather_forecast 与 get_air_quality；需要了解怕冷/怕热时调用 get_user_profile。
3. 最终回复使用中文，包含：今日天气简述、是否带伞、是否戴口罩、穿衣建议。可加一两句角色口癖，但不要太长。
4. 不要输出 JSON，不要解释你调用了哪些工具的内部细节。
5. 可以使用轻量 Markdown 便于阅读（如 **加粗**、## 小标题、- 列表）；不要输出代码块或复杂表格。
        """.trimIndent()
    }
}
