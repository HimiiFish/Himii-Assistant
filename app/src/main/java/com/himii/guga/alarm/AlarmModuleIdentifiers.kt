package com.himii.guga.alarm

/**
 * App 内置的闹钟功能模块标识。用户在设闹钟时勾选；目录随发版扩展。
 */
object AlarmModuleIdentifiers {
    const val WEATHER = "weather"

    fun isKnownModuleIdentifier(moduleIdentifier: String): Boolean {
        return moduleIdentifier == WEATHER
    }
}
