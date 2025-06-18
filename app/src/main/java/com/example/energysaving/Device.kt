// In app/src/main/java/com/example/energysaving/Device.kt
package com.example.energysaving

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Device(
    val id: Int,
    val name: String,
    val description: String,
    val wattUsage: Double,
    val dailyHoursGoal: Double,
    var isOn: Boolean = true,
    val userId: String,
    var timeUsedTodaySeconds: Long = 0L,
    var lastResetDate: String = getCurrentDateString(),
    var lastTurnOnTimestampMillis: Long = 0L,
    var goalExceededAlertSentToday: Boolean = false
)

fun getCurrentDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}