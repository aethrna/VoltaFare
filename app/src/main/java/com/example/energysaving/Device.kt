// In app/src/main/java/com/example/energysaving/Device.kt
package com.example.energysaving

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Device(
    val id: Int,
    val name: String, // This is the type/category (e.g., "Light", "Fan", or "Custom Type")
    val description: String, // Specific name for predefined, or details for custom
    val wattUsage: Double,
    val dailyHoursGoal: Double, // Renamed/repurposed from dailyHours
    var isOn: Boolean = true,
    val userId: String,

    // New fields for tracking
    var timeUsedTodaySeconds: Long = 0L, // Accumulated seconds device was ON today
    var lastResetDate: String = getCurrentDateString(), // Tracks when timeUsedTodaySeconds was last reset (e.g., "YYYY-MM-DD")
    var lastTurnOnTimestampMillis: Long = 0L, // Timestamp when device was last turned ON
    var goalExceededAlertSentToday: Boolean = false // To prevent multiple alerts per day
)

// Helper function to get current date as YYYY-MM-DD string
fun getCurrentDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}