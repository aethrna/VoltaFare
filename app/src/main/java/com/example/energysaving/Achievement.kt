package com.example.energysaving

data class Achievement(
    val id: Int = 0,
    val defId: String,
    val title: String,
    val description: String,
    val iconResId: Int,
    var isUnlocked: Boolean = false,
    var progressCurrent: Int = 0,
    val progressTarget: Int = 1,
    val unlockedDate: String = ""
) {
    val progressPercentage: Int
        get() = if (progressTarget > 0) (progressCurrent * 100 / progressTarget) else 0
}