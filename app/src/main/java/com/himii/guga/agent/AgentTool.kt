package com.himii.guga.agent

import org.json.JSONObject

data class AgentToolDefinition(
    val toolName: String,
    val description: String,
    val parameterSchemaJson: JSONObject
)

interface AgentTool {
    val definition: AgentToolDefinition
    fun execute(argumentsJson: JSONObject): String
}
