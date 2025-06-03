package com.example.energysaving

data class Bounty(
    val id: Int = 0, // Database ID, 0 for new
    val defId: String, // Unique identifier for the bounty type (e.g., "turn_off_lights_bounty")
    val title: String,
    val description: String,
    val xpReward: Int,
    val iconResId: Int, // Drawable resource ID
    var isCompleted: Boolean = false,
    var progressCurrent: Int = 0,
    val progressTarget: Int = 1,
    var lastResetDate: String = "" // For daily/weekly bounties (YYYY-MM-DD)
)