package com.example.energysaving

data class Device(
    val id: Int,
    val name: String,
    val description: String,
    val wattUsage: Double,
    val dailyHours: Double,
    var isOn: Boolean = true,
    val userId: String
)