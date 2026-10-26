package com.himii.guga.settings

import android.content.Context

class UserProfileRepository(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCE_FILE_NAME,
        Context.MODE_PRIVATE
    )

    fun loadSettings(): UserProfileSettings {
        return UserProfileSettings(
            temperaturePreference = sharedPreferences.getString(
                KEY_TEMPERATURE_PREFERENCE,
                UserProfileSettings.PREFERENCE_NORMAL
            ) ?: UserProfileSettings.PREFERENCE_NORMAL
        )
    }

    fun saveSettings(settings: UserProfileSettings) {
        sharedPreferences.edit()
            .putString(KEY_TEMPERATURE_PREFERENCE, settings.temperaturePreference)
            .apply()
    }

    companion object {
        private const val PREFERENCE_FILE_NAME = "guga_user_profile_store"
        private const val KEY_TEMPERATURE_PREFERENCE = "temperature_preference"
    }
}
