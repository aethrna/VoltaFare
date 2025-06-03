package com.example.energysaving

data class Achievement(
    val id: Int = 0, // Database ID, 0 for new
    val defId: String, // Unique identifier for the achievement type (e.g., "first_device_ach")
    val title: String,
    val description: String,
    val iconResId: Int, // Drawable resource ID
    var isUnlocked: Boolean = false,
    var progressCurrent: Int = 0, // Current progress towards the achievement
    val progressTarget: Int = 1, // Target value to unlock achievement
    val unlockedDate: String = "" // Date unlocked (YYYY-MM-DD)
) {
    // Add this computed property
    val progressPercentage: Int
        get() = if (progressTarget > 0) (progressCurrent * 100 / progressTarget) else 0
}