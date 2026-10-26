package com.himii.guga.weather

import com.himii.guga.location.HomeLocationRecord
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class OpenMeteoWeatherClient {

    fun fetchWeatherSnapshot(homeLocationRecord: HomeLocationRecord): WeatherSnapshot {
        val latitude = homeLocationRecord.latitudeDegrees
        val longitude = homeLocationRecord.longitudeDegrees

        val forecastUrl =
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature," +
                "precipitation_probability,weather_code,wind_speed_10m" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
                "&timezone=auto" +
                "&forecast_days=1"

        val airQualityUrl =
            "https://air-quality-api.open-meteo.com/v1/air-quality" +
                "?latitude=$latitude" +
                "&longitude=$longitude" +
                "&current=us_aqi,pm2_5"

        val forecastJson = getJsonObject(forecastUrl)
        val airQualityJson = runCatching { getJsonObject(airQualityUrl) }.getOrNull()

        val current = forecastJson.optJSONObject("current")
        val daily = forecastJson.optJSONObject("daily")
        val airCurrent = airQualityJson?.optJSONObject("current")

        return WeatherSnapshot(
            locationDisplayName = homeLocationRecord.displayName,
            latitudeDegrees = latitude,
            longitudeDegrees = longitude,
            temperatureCelsius = current?.optDoubleOrNull("temperature_2m"),
            apparentTemperatureCelsius = current?.optDoubleOrNull("apparent_temperature"),
            relativeHumidityPercent = current?.optIntOrNull("relative_humidity_2m"),
            precipitationProbabilityPercent = current?.optIntOrNull("precipitation_probability")
                ?: daily?.optJsonArrayFirstInt("precipitation_probability_max"),
            windSpeedMetersPerSecond = current?.optDoubleOrNull("wind_speed_10m"),
            weatherCode = current?.optIntOrNull("weather_code"),
            dailyHighTemperatureCelsius = daily?.optJsonArrayFirstDouble("temperature_2m_max"),
            dailyLowTemperatureCelsius = daily?.optJsonArrayFirstDouble("temperature_2m_min"),
            usAirQualityIndex = airCurrent?.optIntOrNull("us_aqi"),
            particulateMatter25 = airCurrent?.optDoubleOrNull("pm2_5")
        )
    }

    private fun getJsonObject(requestUrl: String): JSONObject {
        val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
        }
        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            if (responseCode !in 200..299) {
                throw IllegalStateException("天气接口失败 HTTP $responseCode：$body")
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) {
            return null
        }
        return optDouble(key)
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) {
            return null
        }
        return optInt(key)
    }

    private fun JSONObject.optJsonArrayFirstDouble(key: String): Double? {
        val jsonArray = optJSONArray(key) ?: return null
        if (jsonArray.length() == 0 || jsonArray.isNull(0)) {
            return null
        }
        return jsonArray.getDouble(0)
    }

    private fun JSONObject.optJsonArrayFirstInt(key: String): Int? {
        val jsonArray = optJSONArray(key) ?: return null
        if (jsonArray.length() == 0 || jsonArray.isNull(0)) {
            return null
        }
        return jsonArray.getInt(0)
    }
}
