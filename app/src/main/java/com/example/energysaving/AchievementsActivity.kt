package com.example.energysaving

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AchievementsActivity : BaseActivity() {

    private val TAG = "AchievementLogic"

    override val activeIndicator: Int
        get() = R.id.navItemAchievements

    private lateinit var devDbHelper: DevDBHelper
    private lateinit var userDbHelper: DBHelper
    private lateinit var achievementAdapter: AchievementListAdapter
    private lateinit var bountyAdapter: BountyAdapter
    private lateinit var currentUserId: String
    private lateinit var profileImage: ImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvDisplayTitle: TextView
    private lateinit var xpProgressBar: ProgressBar


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

        initHeaderViews()
        loadUserProfileHeader()
        setupRecyclerViews()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfileHeader()
        loadAchievements()
        loadBounties()
        checkAndResetDailyBounties()
        checkAndUnlockAchievements()
    }

    private fun checkAndUnlockAchievements() {
        val allAchievements = devDbHelper.getAllAchievementsForUser(currentUserId)
        val currentUserProfile = userDbHelper.getUserProfile(currentUserId)
        val currentUserXp = currentUserProfile[DBHelper.COLUMN_XP]?.toIntOrNull() ?: 0

        if (allAchievements.isEmpty()){
            return
        }

        for (achievement in allAchievements) {
            if (!achievement.isUnlocked) {
                var newProgress = achievement.progressCurrent
                var shouldUnlock = false

                when (achievement.defId) {
                    "first_device_ach" -> {
                        val deviceCount = devDbHelper.getAllDevicesForUser(currentUserId).size
                        if (deviceCount >= 1) {
                            newProgress = 1
                            shouldUnlock = true
                        }
                    }
                    "eco_curious_ach" -> {
                        newProgress = 1
                        shouldUnlock = true
                    }
                    "watt_a_legend_ach" -> {
                        if (currentUserXp >= achievement.progressTarget) {
                            newProgress = achievement.progressTarget
                            shouldUnlock = true
                        } else {
                            newProgress = currentUserXp
                        }
                    }
                }
                if (shouldUnlock) {
                    val unlockedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val success = devDbHelper.updateAchievementStatus(currentUserId, achievement.defId, true, newProgress, unlockedDate)
                    if(success){
                        Toast.makeText(this, "Achievement Unlocked: ${achievement.title}!", Toast.LENGTH_LONG).show()
                    }
                }
                else if (newProgress > achievement.progressCurrent) {
                    val success = devDbHelper.updateAchievementStatus(currentUserId, achievement.defId, false, newProgress, "")
                }
            }
        }
        loadAchievements()
    }

    private fun initHeaderViews() {
        profileImage = findViewById(R.id.profileImage)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvDisplayTitle = findViewById(R.id.tvDisplayTitle)
        xpProgressBar = findViewById(R.id.xpProgressBar)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadUserProfileHeader() {
        val userProfile = userDbHelper.getUserProfile(currentUserId)
        val currentXp = userProfile[DBHelper.COLUMN_XP]?.toIntOrNull() ?: 0
        val currentLevel = userProfile[DBHelper.COLUMN_LEVEL]?.toIntOrNull() ?: 1

        tvDisplayName.text = userProfile[DBHelper.COLUMN_DISPLAY_NAME]
        tvDisplayTitle.text = userProfile[DBHelper.COLUMN_DISPLAY_TITLE]

        val imageUriString = userProfile[DBHelper.COLUMN_PROFILE_IMAGE_URI]
        if (!imageUriString.isNullOrBlank()) {
            try {
                profileImage.setImageURI(Uri.parse(imageUriString))
            } catch (e: Exception) {
                profileImage.setImageResource(R.mipmap.ic_launcher_round)
            }
        }

        val xpNeededForNextLevel = calculateXpForLevel(currentLevel + 1)
        val xpForCurrentLevel = calculateXpForLevel(currentLevel)
        xpProgressBar.max = xpNeededForNextLevel - xpForCurrentLevel
        xpProgressBar.progress = currentXp - xpForCurrentLevel
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
            if (bounty.defId == "turn_off_lights_bounty" && !bounty.isCompleted) {
                completeBounty(bounty.defId)
            }
        }
        recyclerViewBounties.adapter = bountyAdapter
    }

    private fun setupRecyclerViews() {
        val recyclerViewAchievements = findViewById<RecyclerView>(R.id.recyclerViewAchievementsList)
        recyclerViewAchievements.layoutManager = LinearLayoutManager(this)
        achievementAdapter = AchievementListAdapter(emptyList())
        recyclerViewAchievements.adapter = achievementAdapter

        val recyclerViewBounties = findViewById<RecyclerView>(R.id.recyclerViewBountiesList)
        recyclerViewBounties.layoutManager = LinearLayoutManager(this)
        bountyAdapter = BountyAdapter(emptyList()) { bounty ->
            if (!bounty.isCompleted) {
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
            if (bounty.lastResetDate.isNotEmpty() && bounty.lastResetDate != todayDate) {
                devDbHelper.updateBountyStatus(currentUserId, bounty.defId, false, 0, todayDate)
                Log.d("AchievementsActivity", "Resetting daily bounty: ${bounty.title}")
            }
        }
        loadBounties()
    }
    fun completeBounty(bountyDefId: String, progressIncrement: Int = 1) {
        val bounty = devDbHelper.getAllBountiesForUser(currentUserId).find { it.defId == bountyDefId }

        if (bounty != null && !bounty.isCompleted) {
            var newProgress = bounty.progressCurrent + progressIncrement
            var isCompleted = false

            if (newProgress >= bounty.progressTarget) {
                newProgress = bounty.progressTarget
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
                loadBounties()
                checkAndUnlockAchievements()
            }
        }
    }
    private fun awardXpToUser(xpAmount: Int) {
        val currentUserProfile = userDbHelper.getUserProfile(currentUserId)
        var currentXp = currentUserProfile[DBHelper.COLUMN_XP]?.toIntOrNull() ?: 0
        var currentLevel = currentUserProfile[DBHelper.COLUMN_LEVEL]?.toIntOrNull() ?: 1

        currentXp += xpAmount
        while (currentXp >= calculateXpForLevel(currentLevel + 1) && calculateXpForLevel(currentLevel + 1) != 0) {
            currentLevel++
            Toast.makeText(this, "Congratulations! You reached Level $currentLevel!", Toast.LENGTH_LONG).show()
        }

        userDbHelper.updateUserXPAndLevel(currentUserId, currentXp, currentLevel)
    }
    private fun calculateXpForLevel(level: Int): Int {
        return when (level) {
            1 -> 0
            2 -> 5
            3 -> 15
            4 -> 30
            5 -> 50
            else -> {
                50 + (level - 5) * 20
            }
        }
    }
}