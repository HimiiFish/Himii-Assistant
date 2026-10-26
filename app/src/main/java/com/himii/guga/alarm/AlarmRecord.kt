package com.himii.guga.alarm

/**
 * 平等多闹钟记录。功能模块挂在单条闹钟上；无「主力闹钟」概念。
 */
data class AlarmRecord(
    val alarmIdentifier: Long,
    val hourOfDay: Int,
    val minuteOfHour: Int,
    val isEnabled: Boolean,
    val repeatsEveryDay: Boolean,
    val displayLabel: String,
    val enabledModuleIdentifiers: List<String>
) {
    fun hasWeatherModule(): Boolean {
        return enabledModuleIdentifiers.contains(AlarmModuleIdentifiers.WEATHER)
    }

    fun hasAnyModule(): Boolean {
        return enabledModuleIdentifiers.isNotEmpty()
    }
}
