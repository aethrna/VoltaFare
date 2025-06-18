package com.example.energysaving

data class Bounty(
    val id: Int = 0,
    val defId: String,
    val title: String,
    val description: String,
    val xpReward: Int,
    val iconResId: Int,
    var isCompleted: Boolean = false,
    var progressCurrent: Int = 0,
    val progressTarget: Int = 1,
    var lastResetDate: String = ""
)