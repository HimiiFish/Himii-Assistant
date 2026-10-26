package com.himii.guga.agent.tools

import com.himii.guga.agent.AgentTool
import com.himii.guga.agent.AgentToolDefinition
import com.himii.guga.location.HomeLocationRepository
import com.himii.guga.settings.UserProfileRepository
import org.json.JSONObject

class GetUserProfileTool(
    private val homeLocationRepository: HomeLocationRepository,
    private val userProfileRepository: UserProfileRepository
) : AgentTool {

    override val definition = AgentToolDefinition(
        toolName = "get_user_profile",
        description = "获取用户画像：常住地行政区名称，以及对冷热的主观偏好（正常/怕冷/怕热）。",
        parameterSchemaJson = JSONObject()
            .put("type", "object")
            .put("properties", JSONObject())
            .put("additionalProperties", false)
    )

    override fun execute(argumentsJson: JSONObject): String {
        val homeLocation = homeLocationRepository.loadHomeLocation()
        val userProfile = userProfileRepository.loadSettings()
        return JSONObject()
            .put("home_location_display_name", homeLocation?.displayName)
            .put("temperature_preference", userProfile.temperaturePreference)
            .put(
                "home_location_configured",
                homeLocation != null
            )
            .toString()
    }
}
