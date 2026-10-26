package com.himii.guga.location

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 从 assets 加载中国省市区树，供设置页逐级选择。
 */
class ChinaAdministrativeDivisionCatalog(context: Context) {

    private val applicationContext = context.applicationContext
    private val provinceNodes: List<AdministrativeDivisionNode> by lazy { loadProvinceNodes() }

    fun listCountries(): List<String> {
        return listOf(COUNTRY_NAME_CHINA)
    }

    fun listProvinces(): List<AdministrativeDivisionNode> {
        return provinceNodes
    }

    /**
     * 直辖市等「省下面直接是区县」时，补一个与省同名的市级节点，方便 UI 统一四级。
     */
    fun listCities(provinceNode: AdministrativeDivisionNode): List<AdministrativeDivisionNode> {
        val prefectureChildren = provinceNode.childNodes.filter {
            it.regionLevel == "prefecture" || it.childNodes.isNotEmpty()
        }
        if (prefectureChildren.isNotEmpty()) {
            return prefectureChildren
        }
        // 省直管区县：虚拟市 = 省名
        return listOf(
            AdministrativeDivisionNode(
                regionCode = provinceNode.regionCode + "_city",
                regionName = provinceNode.regionName,
                regionLevel = "prefecture",
                latitudeDegrees = provinceNode.latitudeDegrees,
                longitudeDegrees = provinceNode.longitudeDegrees,
                childNodes = provinceNode.childNodes
            )
        )
    }

    fun listDistricts(cityNode: AdministrativeDivisionNode): List<AdministrativeDivisionNode> {
        if (cityNode.childNodes.isNotEmpty()) {
            return cityNode.childNodes
        }
        // 无下级时，允许选到市本身
        return listOf(cityNode)
    }

    fun buildHomeLocationRecord(
        countryName: String,
        provinceNode: AdministrativeDivisionNode,
        cityNode: AdministrativeDivisionNode,
        districtNode: AdministrativeDivisionNode
    ): HomeLocationRecord {
        val displayName = listOf(
            countryName,
            provinceNode.regionName,
            cityNode.regionName,
            districtNode.regionName
        ).distinct().joinToString("-")

        val latitudeDegrees = districtNode.latitudeDegrees
            ?: cityNode.latitudeDegrees
            ?: provinceNode.latitudeDegrees
            ?: DEFAULT_LATITUDE_DEGREES
        val longitudeDegrees = districtNode.longitudeDegrees
            ?: cityNode.longitudeDegrees
            ?: provinceNode.longitudeDegrees
            ?: DEFAULT_LONGITUDE_DEGREES

        return HomeLocationRecord(
            displayName = displayName,
            latitudeDegrees = latitudeDegrees,
            longitudeDegrees = longitudeDegrees
        )
    }

    /**
     * 用坐标就近匹配区县，供「使用当前位置」回填树形选择结果。
     */
    fun findNearestHomeLocation(
        countryName: String,
        latitudeDegrees: Double,
        longitudeDegrees: Double
    ): HomeLocationRecord? {
        var bestDistanceMeters = Double.MAX_VALUE
        var bestRecord: HomeLocationRecord? = null

        for (provinceNode in provinceNodes) {
            for (cityNode in listCities(provinceNode)) {
                for (districtNode in listDistricts(cityNode)) {
                    val districtLatitude = districtNode.latitudeDegrees ?: continue
                    val districtLongitude = districtNode.longitudeDegrees ?: continue
                    val distanceMeters = haversineDistanceMeters(
                        latitudeDegrees,
                        longitudeDegrees,
                        districtLatitude,
                        districtLongitude
                    )
                    if (distanceMeters < bestDistanceMeters) {
                        bestDistanceMeters = distanceMeters
                        bestRecord = buildHomeLocationRecord(
                            countryName = countryName,
                            provinceNode = provinceNode,
                            cityNode = cityNode,
                            districtNode = districtNode
                        )
                    }
                }
            }
        }
        // 超过约 80km 认为匹配不可靠
        if (bestDistanceMeters > 80_000.0) {
            return null
        }
        return bestRecord
    }

    private fun loadProvinceNodes(): List<AdministrativeDivisionNode> {
        val jsonText = applicationContext.assets
            .open(ASSET_FILE_NAME)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val jsonArray = JSONArray(jsonText)
        val nodes = mutableListOf<AdministrativeDivisionNode>()
        for (index in 0 until jsonArray.length()) {
            nodes.add(parseNode(jsonArray.getJSONObject(index)))
        }
        return nodes
    }

    private fun parseNode(jsonObject: JSONObject): AdministrativeDivisionNode {
        val centerObject = jsonObject.optJSONObject("center")
        val childArray = jsonObject.optJSONArray("children")
        val childNodes = mutableListOf<AdministrativeDivisionNode>()
        if (childArray != null) {
            for (index in 0 until childArray.length()) {
                childNodes.add(parseNode(childArray.getJSONObject(index)))
            }
        }
        return AdministrativeDivisionNode(
            regionCode = jsonObject.optString("code"),
            regionName = jsonObject.optString("name"),
            regionLevel = jsonObject.optString("level"),
            latitudeDegrees = centerObject?.optDouble("latitude"),
            longitudeDegrees = centerObject?.optDouble("longitude"),
            childNodes = childNodes
        )
    }

    private fun haversineDistanceMeters(
        latitudeOne: Double,
        longitudeOne: Double,
        latitudeTwo: Double,
        longitudeTwo: Double
    ): Double {
        val earthRadiusMeters = 6_371_000.0
        val latitudeDelta = Math.toRadians(latitudeTwo - latitudeOne)
        val longitudeDelta = Math.toRadians(longitudeTwo - longitudeOne)
        val left = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(Math.toRadians(latitudeOne)) * cos(Math.toRadians(latitudeTwo)) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        val centralAngle = 2 * atan2(sqrt(left), sqrt(1 - left))
        return earthRadiusMeters * centralAngle
    }

    companion object {
        const val COUNTRY_NAME_CHINA = "中国"
        private const val ASSET_FILE_NAME = "china_administrative_divisions.json"
        private const val DEFAULT_LATITUDE_DEGREES = 39.9042
        private const val DEFAULT_LONGITUDE_DEGREES = 116.4074
    }
}
