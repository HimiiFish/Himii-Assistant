package com.himii.guga.agent.tools

import com.himii.guga.agent.AgentTool
import com.himii.guga.agent.AgentToolDefinition
import com.himii.guga.location.HomeLocationRepository
import com.himii.guga.weather.OpenMeteoWeatherClient
import org.json.JSONObject

class GetAirQualityTool(
    private val homeLocationRepository: HomeLocationRepository,
    private val openMeteoWeatherClient: OpenMeteoWeatherClient
) : AgentTool {

    override val definition = AgentToolDefinition(
        toolName = "get_air_quality",
        description = "获取常住地空气质量：US AQI 与 PM2.5，用于判断是否建议戴口罩。",
        parameterSchemaJson = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject())
            .put("additionalProperties", false)
    )

    override fun execute(argumentsJson: JSONObject): String {
        val homeLocation = homeLocationRepository.loadHomeLocation()
            ?: return JSONObject()
                .put("error", "尚未设置常住地")
                .toString()
        val snapshot = openMeteoWeatherClient.fetchWeatherSnapshot(homeLocation)
        return JSONObject()
            .put("location_display_name", snapshot.locationDisplayName)
            .put("us_air_quality_index", snapshot.usAirQualityIndex)
            .put("particulate_matter_25", snapshot.particulateMatter25)
            .toString()
    }
}
