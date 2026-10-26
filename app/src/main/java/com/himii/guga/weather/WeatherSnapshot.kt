package com.himii.guga.weather

data class WeatherSnapshot(
    val locationDisplayName: String,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val temperatureCelsius: Double?,
    val apparentTemperatureCelsius: Double?,
    val relativeHumidityPercent: Int?,
    val precipitationProbabilityPercent: Int?,
    val windSpeedMetersPerSecond: Double?,
    val weatherCode: Int?,
    val dailyHighTemperatureCelsius: Double?,
    val dailyLowTemperatureCelsius: Double?,
    val usAirQualityIndex: Int?,
    val particulateMatter25: Double?
)
