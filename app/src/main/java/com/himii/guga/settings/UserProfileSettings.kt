package com.himii.guga.settings

/**
 * 给 Agent 的 get_user_profile 用。地点来自常住地仓库，这里只存主观偏好。
 */
data class UserProfileSettings(
    val temperaturePreference: String
) {
    companion object {
        const val PREFERENCE_NORMAL = "正常"
        const val PREFERENCE_FEELS_COLD_EASILY = "怕冷"
        const val PREFERENCE_FEELS_HOT_EASILY = "怕热"
    }
}
