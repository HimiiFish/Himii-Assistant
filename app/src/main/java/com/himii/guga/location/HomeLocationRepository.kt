package com.himii.guga.location

import android.content.Context

class HomeLocationRepository(context: Context) {

    private val sharedPreferences = context.applicationContext.getSharedPreferences(
        PREFERENCE_FILE_NAME,
        Context.MODE_PRIVATE
    )

    fun loadHomeLocation(): HomeLocationRecord? {
        if (!sharedPreferences.contains(KEY_LATITUDE_DEGREES)) {
            return null
        }
        return HomeLocationRecord(
            displayName = sharedPreferences.getString(KEY_DISPLAY_NAME, "") ?: "",
            latitudeDegrees = Double.fromBits(
                sharedPreferences.getLong(KEY_LATITUDE_DEGREES, 0L)
            ),
            longitudeDegrees = Double.fromBits(
                sharedPreferences.getLong(KEY_LONGITUDE_DEGREES, 0L)
            )
        )
    }

    fun saveHomeLocation(homeLocationRecord: HomeLocationRecord) {
        sharedPreferences.edit()
            .putString(KEY_DISPLAY_NAME, homeLocationRecord.displayName)
            .putLong(KEY_LATITUDE_DEGREES, homeLocationRecord.latitudeDegrees.toRawBits())
            .putLong(KEY_LONGITUDE_DEGREES, homeLocationRecord.longitudeDegrees.toRawBits())
            .apply()
    }

    companion object {
        private const val PREFERENCE_FILE_NAME = "guga_home_location_store"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_LATITUDE_DEGREES = "latitude_degrees_bits"
        private const val KEY_LONGITUDE_DEGREES = "longitude_degrees_bits"
    }
}
