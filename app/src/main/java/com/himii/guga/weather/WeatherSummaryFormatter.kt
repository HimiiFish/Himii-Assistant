package com.himii.guga.weather

import java.util.Locale

object WeatherSummaryFormatter {

    fun formatReadableSummary(weatherSnapshot: WeatherSnapshot): String {
        val lines = mutableListOf<String>()
        lines.add("地点：${weatherSnapshot.locationDisplayName}")

        weatherSnapshot.temperatureCelsius?.let {
            lines.add(String.format(Locale.getDefault(), "当前气温：%.1f℃", it))
        }
        weatherSnapshot.apparentTemperatureCelsius?.let {
            lines.add(String.format(Locale.getDefault(), "体感温度：%.1f℃", it))
        }
        if (weatherSnapshot.dailyLowTemperatureCelsius != null &&
            weatherSnapshot.dailyHighTemperatureCelsius != null
        ) {
            lines.add(
                String.format(
                    Locale.getDefault(),
                    "今日气温：%.1f℃ ~ %.1f℃",
                    weatherSnapshot.dailyLowTemperatureCelsius,
                    weatherSnapshot.dailyHighTemperatureCelsius
                )
            )
        }
        weatherSnapshot.precipitationProbabilityPercent?.let {
            lines.add("降水概率：$it%")
        }
        weatherSnapshot.relativeHumidityPercent?.let {
            lines.add("相对湿度：$it%")
        }
        weatherSnapshot.windSpeedMetersPerSecond?.let {
            lines.add(String.format(Locale.getDefault(), "风速：%.1f m/s", it))
        }
        weatherSnapshot.weatherCode?.let {
            lines.add("天气代码：$it（${describeWeatherCode(it)}）")
        }
        weatherSnapshot.usAirQualityIndex?.let {
            lines.add("空气质量（US AQI）：$it（${describeAirQualityIndex(it)}）")
        }
        weatherSnapshot.particulateMatter25?.let {
            lines.add(String.format(Locale.getDefault(), "PM2.5：%.1f μg/m³", it))
        }

        lines.add("")
        lines.add("参考建议：")
        lines.add(buildUmbrellaHint(weatherSnapshot))
        lines.add(buildMaskHint(weatherSnapshot))
        lines.add(buildClothingHint(weatherSnapshot))

        return lines.joinToString("\n")
    }

    private fun buildUmbrellaHint(weatherSnapshot: WeatherSnapshot): String {
        val probability = weatherSnapshot.precipitationProbabilityPercent ?: return "带伞：数据不足"
        return when {
            probability >= 60 -> "带伞：建议带伞（降水概率较高）"
            probability >= 30 -> "带伞：可备折叠伞"
            else -> "带伞：暂不必带伞"
        }
    }

    private fun buildMaskHint(weatherSnapshot: WeatherSnapshot): String {
        val airQualityIndex = weatherSnapshot.usAirQualityIndex ?: return "口罩：空气数据不足"
        return when {
            airQualityIndex >= 151 -> "口罩：建议戴口罩（空气较差）"
            airQualityIndex >= 101 -> "口罩：敏感人群建议戴口罩"
            else -> "口罩：一般不必戴口罩"
        }
    }

    private fun buildClothingHint(weatherSnapshot: WeatherSnapshot): String {
        val referenceTemperature = weatherSnapshot.apparentTemperatureCelsius
            ?: weatherSnapshot.temperatureCelsius
            ?: return "穿衣：气温数据不足"
        return when {
            referenceTemperature <= 5 -> "穿衣：厚外套 / 保暖层"
            referenceTemperature <= 12 -> "穿衣：夹克或薄羽绒"
            referenceTemperature <= 20 -> "穿衣：长袖 + 薄外套"
            referenceTemperature <= 27 -> "穿衣：薄长袖或短袖"
            else -> "穿衣：透气短袖，注意防晒"
        }
    }

    private fun describeWeatherCode(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "晴"
            1, 2, 3 -> "多云"
            45, 48 -> "雾"
            51, 53, 55, 56, 57 -> "毛毛雨"
            61, 63, 65, 66, 67 -> "雨"
            71, 73, 75, 77 -> "雪"
            80, 81, 82 -> "阵雨"
            85, 86 -> "阵雪"
            95, 96, 99 -> "雷暴"
            else -> "其他"
        }
    }

    private fun describeAirQualityIndex(airQualityIndex: Int): String {
        return when {
            airQualityIndex <= 50 -> "优"
            airQualityIndex <= 100 -> "良"
            airQualityIndex <= 150 -> "轻度污染"
            airQualityIndex <= 200 -> "中度污染"
            airQualityIndex <= 300 -> "重度污染"
            else -> "严重污染"
        }
    }
}
