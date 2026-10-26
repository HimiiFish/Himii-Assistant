package com.himii.guga.agent.tools

import com.himii.guga.agent.AgentTool
import com.himii.guga.agent.AgentToolDefinition
import com.himii.guga.location.HomeLocationRepository
import com.himii.guga.weather.OpenMeteoWeatherClient
import org.json.JSONObject

class GetWeatherForecastTool(
    private val homeLocationRepository: HomeLocationRepository,
    private val openMeteoWeatherClient: OpenMeteoWeatherClient
) : AgentTool {

    override val definition = AgentToolDefinition(
        toolName = "get_weather_forecast",
        description = "获取常住地今日天气：气温、体感、降水概率、湿度、风速、天气现象与今日高低温。",
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
            .put("temperature_celsius", snapshot.temperatureCelsius)
            .put("apparent_temperature_celsius", snapshot.apparentTemperatureCelsius)
            .put("relative_humidity_percent", snapshot.relativeHumidityPercent)
            .put("precipitation_probability_percent", snapshot.precipitationProbabilityPercent)
            .put("wind_speed_meters_per_second", snapshot.windSpeedMetersPerSecond)
            .put("weather_code", snapshot.weatherCode)
            .put("daily_high_temperature_celsius", snapshot.dailyHighTemperatureCelsius)
            .put("daily_low_temperature_celsius", snapshot.dailyLowTemperatureCelsius)
            .toString()
    }
}
