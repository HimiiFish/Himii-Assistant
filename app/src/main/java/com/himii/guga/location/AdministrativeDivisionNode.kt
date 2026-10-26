package com.himii.guga.location

data class AdministrativeDivisionNode(
    val regionCode: String,
    val regionName: String,
    val regionLevel: String,
    val latitudeDegrees: Double?,
    val longitudeDegrees: Double?,
    val childNodes: List<AdministrativeDivisionNode>
) {
    fun isLeafLikeDistrict(): Boolean {
        return childNodes.isEmpty() ||
            regionLevel == "county" ||
            regionLevel == "district"
    }
}
