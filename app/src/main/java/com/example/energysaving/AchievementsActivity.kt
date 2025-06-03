package com.example.energysaving

import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log // For debugging

class AchievementsActivity : AppCompatActivity() {

    private lateinit var devDbHelper: DevDBHelper
    private lateinit var userDbHelper: DBHelper // To update user XP and Level
    private lateinit var achievementAdapter: AchievementListAdapter
    private lateinit var bountyAdapter: BountyAdapter
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        devDbHelper = DevDBHelper(this)
        userDbHelper = DBHelper(this)

        val prefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        currentUserId = prefs.getString("currentUserId", null)
            ?: run {
                Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupAchievementsRecyclerView()
        setupBountiesRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadAchievements()
        loadBounties()
        checkAndResetDailyBounties() // Ensure daily bounties reset on resume
        checkAndUnlockAchievements() // Check for new achievements
    }

    private fun setupAchievementsRecyclerView() {
        val recyclerViewAchievements = findViewById<RecyclerView>(R.id.recyclerViewAchievementsList)
        recyclerViewAchievements.layoutManager = LinearLayoutManager(this)
        achievementAdapter = AchievementListAdapter(emptyList())
        recyclerViewAchievements.adapter = achievementAdapter
    }

    private fun setupBountiesRecyclerView() {
        val recyclerViewBounties = findViewById<RecyclerView>(R.id.recyclerViewBountiesList)
        recyclerViewBounties.layoutManager = LinearLayoutManager(this)
        bountyAdapter = BountyAdapter(emptyList()) { bounty ->
            // Handle bounty click (e.g., mark as complete manually if applicable, or show details)
            // For bounties that require user action in this activity, this is where you'd trigger it.
            // Most bounties will be completed through other activities (e.g., turning off devices).
            Toast.makeText(this, "Bounty: ${bounty.title} clicked!", Toast.LENGTH_SHORT).show()
            // Example: For "Turn off lights", if user manually clicks, it marks it complete
            // This is a placeholder for actual game logic.
            if (bounty.defId == "turn_off_lights_bounty" && !bounty.isCompleted) {
                completeBounty(bounty.defId)
            }
        }
        recyclerViewBounties.adapter = bountyAdapter
    }

    private fun loadAchievements() {
        val achievements = devDbHelper.getAllAchievementsForUser(currentUserId)
        if (achievements.isEmpty()) {
            devDbHelper.initializeAchievementsForUser(currentUserId)
            achievementAdapter.updateAchievements(devDbHelper.getAllAchievementsForUser(currentUserId))
        } else {
            achievementAdapter.updateAchievements(achievements)
        }
    }

    private fun loadBounties() {
        val bounties = devDbHelper.getAllBountiesForUser(currentUserId)
        if (bounties.isEmpty()) {
            devDbHelper.initializeBountiesForUser(currentUserId)
            bountyAdapter.updateBounties(devDbHelper.getAllBountiesForUser(currentUserId))
        } else {
            bountyAdapter.updateBounties(bounties)
        }
    }

    private fun checkAndResetDailyBounties() {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val allBounties = devDbHelper.getAllBountiesForUser(currentUserId)

        for (bounty in allBounties) {
            // Check if bounty is daily and if its last reset date is not today
            // Note: The bounty initialization data (reset_daily: true/false) is not directly stored in the DB,
            // so for a robust solution, you'd need a static definition of bounties in code
            // or in a separate "bounty_definitions" table.
            // For now, we'll assume bounties initialized with a lastResetDate mean they are resettable.
            if (bounty.lastResetDate.isNotEmpty() && bounty.lastResetDate != todayDate) {
                devDbHelper.updateBountyStatus(currentUserId, bounty.defId, false, 0, todayDate)
                Log.d("AchievementsActivity", "Resetting daily bounty: ${bounty.title}")
            }
        }
        loadBounties() // Reload bounties after potential resets
    }

    private fun checkAndUnlockAchievements() {
        val allAchievements = devDbHelper.getAllAchievementsForUser(currentUserId)
        val currentUserProfile = userDbHelper.getUserProfile(currentUserId)
        val currentUserXp = currentUserProfile[DBHelper.COLUMN_XP]?.toIntOrNull() ?: 0

        for (achievement in allAchievements) {
            if (!achievement.isUnlocked) {
                var newProgress = achievement.progressCurrent // Default to current progress
                var shouldUnlock = false

                when (achievement.defId) {
                    "first_device_ach" -> {
                        val deviceCount = devDbHelper.getAllDevicesForUser(currentUserId).size
                        if (deviceCount >= 1 && achievement.progressCurrent < 1) { // Check progressCurrent to avoid re-triggering
                            newProgress = 1
                            shouldUnlock = true
                        }
                    }
                    "eco_curious_ach" -> {
                        // This achievement would require tracking user navigation or feature usage
                        // For demonstration, let's say simply visiting this activity progresses it
                        // You'd need a more robust way to track this
                        if (achievement.progressCurrent == 0) { // Only update if not yet progressed
                            newProgress = 1
                            shouldUnlock = true
                        }
                    }
                    "watt_a_legend_ach" -> {
                        // Progress based on total XP
                        if (currentUserXp >= achievement.progressTarget) {
                            newProgress = achievement.progressTarget
                            shouldUnlock = true
                        } else {
                            newProgress = currentUserXp // Update progress as XP increases
                        }
                    }
                    // Add more achievement logic here based on your defined def_ids
                    // e.g., "lorax_ach" might depend on daily energy consumption data
                    // "eco_streak_ach" might depend on bounty completion streaks
                    // "watt_watcher_ach" might depend on weekly energy reduction calculations
                }

                if (shouldUnlock || (newProgress > achievement.progressCurrent && newProgress <= achievement.progressTarget)) {
                    val unlockedDate = if (shouldUnlock) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else ""
                    devDbHelper.updateAchievementStatus(currentUserId, achievement.defId, shouldUnlock, newProgress, unlockedDate)
                    if (shouldUnlock) {
                        Toast.makeText(this, "Achievement Unlocked: ${achievement.title}!", Toast.LENGTH_LONG).show()
                        // You might also award XP for achievements directly, or just bounties
                    }
                }
            }
        }
        loadAchievements() // Reload achievements after checking
    }


    // This method would be called when a bounty is completed, typically from
    // other activities (e.g., DeviceListDetailActivity when a device is turned off for "Turn Off All Lights")
    // For manual completion in AchievementsActivity, it could be triggered by clicking.
    fun completeBounty(bountyDefId: String, progressIncrement: Int = 1) {
        val bounty = devDbHelper.getAllBountiesForUser(currentUserId).find { it.defId == bountyDefId }

        if (bounty != null && !bounty.isCompleted) {
            var newProgress = bounty.progressCurrent + progressIncrement
            var isCompleted = false

            if (newProgress >= bounty.progressTarget) {
                newProgress = bounty.progressTarget // Cap progress at target
                isCompleted = true
            }

            val updated = devDbHelper.updateBountyStatus(currentUserId, bounty.defId, isCompleted, newProgress)

            if (updated) {
                if (isCompleted) {
                    Toast.makeText(this, "Bounty Completed: ${bounty.title}! +${bounty.xpReward} XP", Toast.LENGTH_LONG).show()
                    awardXpToUser(bounty.xpReward)
                } else {
                    Toast.makeText(this, "${bounty.title} Progress: $newProgress/${bounty.progressTarget}", Toast.LENGTH_SHORT).show()
                }
                loadBounties() // Refresh bounties list
                checkAndUnlockAchievements() // Check if any achievements are unlocked
            }
        }
    }

    private fun awardXpToUser(xpAmount: Int) {
        val currentUserProfile = userDbHelper.getUserProfile(currentUserId)
        var currentXp = currentUserProfile[DBHelper.COLUMN_XP]?.toIntOrNull() ?: 0
        var currentLevel = currentUserProfile[DBHelper.COLUMN_LEVEL]?.toIntOrNull() ?: 1

        currentXp += xpAmount

        // Level up logic (same as in DashboardActivity)
        while (currentXp >= calculateXpForLevel(currentLevel + 1) && calculateXpForLevel(currentLevel + 1) != 0) {
            currentLevel++
            Toast.makeText(this, "Congratulations! You reached Level $currentLevel!", Toast.LENGTH_LONG).show()
        }

        userDbHelper.updateUserXPAndLevel(currentUserId, currentXp, currentLevel)
    }

    // This function needs to be consistent with the one in DashboardActivity
    private fun calculateXpForLevel(level: Int): Int {
        return when (level) {
            1 -> 0 // XP needed to be level 1 (starts at 0)
            2 -> 100 // XP needed to reach level 2 (from 100 to 249 for level 3)
            3 -> 250
            4 -> 450
            5 -> 700
            else -> {
                // Example: simple exponential growth
                (50 * (level - 1) * (level - 1)) + (50 * (level - 1)) + 100 // Adjust formula as needed
            }
        }
    }
}