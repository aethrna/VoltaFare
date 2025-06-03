package com.example.energysaving

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// IMPORTANT: Increment your database version (e.g., from 4 to 5)
class DevDBHelper(context: Context) : SQLiteOpenHelper(context, "DeviceDB", null, 5) { // Version updated to 5

    // All constants for table and column names MUST be inside the companion object
    companion object {
        const val TABLE_DEVICES = "devices"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_WATT_USAGE = "watt_usage"
        const val COLUMN_IS_ON = "is_on"
        const val COLUMN_USER_ID = "user_id"

        const val COLUMN_DAILY_HOURS_GOAL = "daily_hours_goal"
        const val COLUMN_TIME_USED_TODAY_SECONDS = "time_used_today_seconds"
        const val COLUMN_LAST_RESET_DATE = "last_reset_date"
        const val COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS = "last_turn_on_timestamp_millis"
        const val COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY = "goal_exceeded_alert_sent_today"

        // New Table and Columns for Daily Energy Logs
        const val TABLE_DAILY_ENERGY = "daily_energy_logs"
        const val COLUMN_LOG_ID = "log_id"
        const val COLUMN_LOG_DATE = "log_date" // YYYY-MM-DD
        const val COLUMN_TOTAL_KWH = "total_kwh"

        // NEW: Table and Columns for Achievements
        const val TABLE_ACHIEVEMENTS = "user_achievements"
        const val COLUMN_ACHIEVEMENT_ID = "achievement_id" // Unique ID for this specific achievement record (auto)
        const val COLUMN_ACH_DEF_ID = "ach_def_id" // Reference to a static achievement definition (e.g., "first_device_ach")
        const val COLUMN_ACH_TITLE = "title"
        const val COLUMN_ACH_DESCRIPTION = "description"
        const val COLUMN_ACH_ICON_RES_NAME = "icon_res_name" // Storing drawable name, not ID
        const val COLUMN_ACH_UNLOCKED = "unlocked" // Boolean (INTEGER 0 or 1)
        const val COLUMN_ACH_PROGRESS_CURRENT = "progress_current" // For achievements with progress
        const val COLUMN_ACH_PROGRESS_TARGET = "progress_target" // For achievements with progress
        const val COLUMN_ACH_UNLOCKED_DATE = "unlocked_date" // YYYY-MM-DD

        // NEW: Table and Columns for Bounties
        const val TABLE_BOUNTIES = "user_bounties"
        const val COLUMN_BOUNTY_ID = "bounty_id" // Unique ID for this specific bounty record (auto)
        const val COLUMN_BOUNTY_DEF_ID = "bounty_def_id" // Reference to a static bounty definition (e.g., "turn_off_lights")
        const val COLUMN_BOUNTY_TITLE = "title"
        const val COLUMN_BOUNTY_DESCRIPTION = "description"
        const val COLUMN_BOUNTY_XP_REWARD = "xp_reward"
        const val COLUMN_BOUNTY_ICON_RES_NAME = "icon_res_name"
        const val COLUMN_BOUNTY_COMPLETED = "completed" // Boolean (INTEGER 0 or 1)
        const val COLUMN_BOUNTY_PROGRESS_CURRENT = "progress_current"
        const val COLUMN_BOUNTY_PROGRESS_TARGET = "progress_target"
        const val COLUMN_BOUNTY_LAST_RESET_DATE = "last_reset_date" // For daily/weekly bounties
    }

    private val applicationContext: Context = context // Store context to get resources by name

    override fun onCreate(db: SQLiteDatabase) {
        // Create the devices table
        db.execSQL(
            "CREATE TABLE ${TABLE_DEVICES}(" +
                    "${COLUMN_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${COLUMN_NAME} TEXT, " +
                    "${COLUMN_DESCRIPTION} TEXT, " +
                    "${COLUMN_WATT_USAGE} REAL, " +
                    "${COLUMN_DAILY_HOURS_GOAL} REAL DEFAULT 0.0, " +
                    "${COLUMN_IS_ON} INTEGER DEFAULT 1, " +
                    "${COLUMN_USER_ID} TEXT NOT NULL, " +
                    "${COLUMN_TIME_USED_TODAY_SECONDS} INTEGER DEFAULT 0, " +
                    "${COLUMN_LAST_RESET_DATE} TEXT DEFAULT '', " +
                    "${COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS} INTEGER DEFAULT 0, " +
                    "${COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY} INTEGER DEFAULT 0)"
        )

        // Create the daily energy logs table
        db.execSQL(
            "CREATE TABLE ${TABLE_DAILY_ENERGY}(" +
                    "${COLUMN_LOG_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${COLUMN_LOG_DATE} TEXT NOT NULL, " + // Date is unique per user
                    "${COLUMN_TOTAL_KWH} REAL DEFAULT 0.0, " +
                    "${COLUMN_USER_ID} TEXT NOT NULL, " +
                    "UNIQUE(${COLUMN_LOG_DATE}, ${COLUMN_USER_ID}))" // Ensure only one entry per user per day
        )

        // NEW: Create Achievements table
        db.execSQL(
            "CREATE TABLE ${TABLE_ACHIEVEMENTS}(" +
                    "${COLUMN_ACHIEVEMENT_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${COLUMN_USER_ID} TEXT NOT NULL, " +
                    "${COLUMN_ACH_DEF_ID} TEXT NOT NULL, " + // e.g., "first_login_ach"
                    "${COLUMN_ACH_TITLE} TEXT, " +
                    "${COLUMN_ACH_DESCRIPTION} TEXT, " +
                    "${COLUMN_ACH_ICON_RES_NAME} TEXT, " +
                    "${COLUMN_ACH_UNLOCKED} INTEGER DEFAULT 0, " +
                    "${COLUMN_ACH_PROGRESS_CURRENT} INTEGER DEFAULT 0, " +
                    "${COLUMN_ACH_PROGRESS_TARGET} INTEGER DEFAULT 0, " +
                    "${COLUMN_ACH_UNLOCKED_DATE} TEXT DEFAULT '', " +
                    "UNIQUE(${COLUMN_USER_ID}, ${COLUMN_ACH_DEF_ID}))" // One achievement per user per definition
        )

        // NEW: Create Bounties table
        db.execSQL(
            "CREATE TABLE ${TABLE_BOUNTIES}(" +
                    "${COLUMN_BOUNTY_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${COLUMN_USER_ID} TEXT NOT NULL, " +
                    "${COLUMN_BOUNTY_DEF_ID} TEXT NOT NULL, " + // e.g., "daily_lights_off"
                    "${COLUMN_BOUNTY_TITLE} TEXT, " +
                    "${COLUMN_BOUNTY_DESCRIPTION} TEXT, " +
                    "${COLUMN_BOUNTY_XP_REWARD} INTEGER DEFAULT 0, " +
                    "${COLUMN_BOUNTY_ICON_RES_NAME} TEXT, " +
                    "${COLUMN_BOUNTY_COMPLETED} INTEGER DEFAULT 0, " +
                    "${COLUMN_BOUNTY_PROGRESS_CURRENT} INTEGER DEFAULT 0, " +
                    "${COLUMN_BOUNTY_PROGRESS_TARGET} INTEGER DEFAULT 0, " +
                    "${COLUMN_BOUNTY_LAST_RESET_DATE} TEXT DEFAULT '', " +
                    "UNIQUE(${COLUMN_USER_ID}, ${COLUMN_BOUNTY_DEF_ID}))"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // (Assuming you previously had these ALTER statements for version 2)
            // No action needed here if your initial DevDBHelper already created these.
        }
        if (oldVersion < 3) {
            // (Assuming you previously had these ALTER statements for version 3)
            // No action needed here if your initial DevDBHelper already created these.
        }
        if (oldVersion < 4) {
            // (Assuming you previously had these ALTER statements for version 4)
            // No action needed here if your initial DevDBHelper already created these.
        }
        if (oldVersion < 5) { // Upgrade to version 5, adding achievements and bounties tables
            db.execSQL("DROP TABLE IF EXISTS ${TABLE_ACHIEVEMENTS}")
            db.execSQL("DROP TABLE IF EXISTS ${TABLE_BOUNTIES}")
            // Re-create them here to ensure new schema is applied
            onCreate(db) // This will recreate all tables if they don't exist
        }
    }

    // --- NEW: Achievement-related methods ---

    fun insertAchievement(
        userId: String,
        achDefId: String,
        title: String,
        description: String,
        iconResName: String,
        unlocked: Boolean = false,
        progressCurrent: Int = 0,
        progressTarget: Int = 0,
        unlockedDate: String = ""
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_ACH_DEF_ID, achDefId)
            put(COLUMN_ACH_TITLE, title)
            put(COLUMN_ACH_DESCRIPTION, description)
            put(COLUMN_ACH_ICON_RES_NAME, iconResName)
            put(COLUMN_ACH_UNLOCKED, if (unlocked) 1 else 0)
            put(COLUMN_ACH_PROGRESS_CURRENT, progressCurrent)
            put(COLUMN_ACH_PROGRESS_TARGET, progressTarget)
            put(COLUMN_ACH_UNLOCKED_DATE, unlockedDate)
        }
        val result = db.insertWithOnConflict(TABLE_ACHIEVEMENTS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
        return result != -1L
    }

    // Call this upon new user registration to set up default achievements
    fun initializeAchievementsForUser(userId: String) {
        val achievementsToInitialize = listOf(
            mapOf("def_id" to "lorax_ach", "title" to "The Lorax", "description" to "Maintain top-tier energy saving for 7 days.", "icon" to "ic_lorax_ach", "progress_target" to 7),
            mapOf("def_id" to "eco_curious_ach", "title" to "Eco Curious", "description" to "Explore all app features.", "icon" to "ic_eco_curious_ach", "progress_target" to 1), // Example of a 1-step achievement
            mapOf("def_id" to "first_device_ach", "title" to "Plugged In", "description" to "Log your first device.", "icon" to "ic_plugged_in_ach", "progress_target" to 1),
            mapOf("def_id" to "eco_streak_ach", "title" to "Eco-Streak", "description" to "Complete 3 daily bounties in a row.", "icon" to "ic_eco_streak_ach", "progress_target" to 3),
            mapOf("def_id" to "watt_a_legend_ach", "title" to "Watt a Legend", "description" to "Achieve a total of 1000 XP.", "icon" to "ic_watt_a_legend_ach", "progress_target" to 1000),
            mapOf("def_id" to "watt_watcher_ach", "title" to "Watt Watcher", "description" to "Reduce energy usage by 20% in a week.", "icon" to "ic_watt_watcher_ach", "progress_target" to 20)
        )

        for (ach in achievementsToInitialize) {
            insertAchievement(
                userId = userId,
                achDefId = ach["def_id"] as String,
                title = ach["title"] as String,
                description = ach["description"] as String,
                iconResName = ach["icon"] as String,
                progressTarget = ach["progress_target"] as Int
            )
        }
    }


    @SuppressLint("Range")
    fun getAllAchievementsForUser(userId: String): List<Achievement> {
        val achievementList = mutableListOf<Achievement>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_ACHIEVEMENTS,
            null,
            "${COLUMN_USER_ID} = ?",
            arrayOf(userId),
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val iconResId = applicationContext.resources.getIdentifier(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_ICON_RES_NAME)),
                    "drawable", // or "mipmap" depending on where you store icons
                    applicationContext.packageName
                )
                val achievement = Achievement(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACHIEVEMENT_ID)),
                    defId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_DEF_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_DESCRIPTION)),
                    iconResId = iconResId,
                    isUnlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACH_UNLOCKED)) == 1,
                    progressCurrent = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACH_PROGRESS_CURRENT)),
                    progressTarget = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACH_PROGRESS_TARGET)),
                    unlockedDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_UNLOCKED_DATE))
                )
                achievementList.add(achievement)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return achievementList.sortedBy { !it.isUnlocked } // Show unlocked first
    }

    fun updateAchievementStatus(
        userId: String,
        achDefId: String,
        unlocked: Boolean,
        progressCurrent: Int,
        unlockedDate: String? = null // Optional: set if unlocked
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ACH_UNLOCKED, if (unlocked) 1 else 0)
            put(COLUMN_ACH_PROGRESS_CURRENT, progressCurrent)
            if (unlockedDate != null) {
                put(COLUMN_ACH_UNLOCKED_DATE, unlockedDate)
            }
        }
        val rowsAffected = db.update(
            TABLE_ACHIEVEMENTS,
            values,
            "${COLUMN_USER_ID} = ? AND ${COLUMN_ACH_DEF_ID} = ?",
            arrayOf(userId, achDefId)
        )
        db.close()
        return rowsAffected > 0
    }

    @SuppressLint("Range")
    fun getAchievementByDefId(userId: String, achDefId: String): Achievement? {
        val db = this.readableDatabase
        var achievement: Achievement? = null
        val cursor = db.query(
            TABLE_ACHIEVEMENTS,
            null,
            "${COLUMN_USER_ID} = ? AND ${COLUMN_ACH_DEF_ID} = ?",
            arrayOf(userId, achDefId),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            val iconResId = applicationContext.resources.getIdentifier(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_ICON_RES_NAME)),
                "drawable",
                applicationContext.packageName
            )
            achievement = Achievement(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACHIEVEMENT_ID)),
                defId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_DEF_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_TITLE)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_DESCRIPTION)),
                iconResId = iconResId,
                isUnlocked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACH_UNLOCKED)) == 1,
                progressCurrent = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACH_PROGRESS_CURRENT)),
                progressTarget = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACH_PROGRESS_TARGET)),
                unlockedDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ACH_UNLOCKED_DATE))
            )
        }
        cursor.close()
        db.close()
        return achievement
    }


    // --- NEW: Bounty-related methods ---

    fun insertBounty(
        userId: String,
        bountyDefId: String,
        title: String,
        description: String,
        xpReward: Int,
        iconResName: String,
        completed: Boolean = false,
        progressCurrent: Int = 0,
        progressTarget: Int = 1, // Default target is 1 for simple completion
        lastResetDate: String = ""
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_BOUNTY_DEF_ID, bountyDefId)
            put(COLUMN_BOUNTY_TITLE, title)
            put(COLUMN_BOUNTY_DESCRIPTION, description)
            put(COLUMN_BOUNTY_XP_REWARD, xpReward)
            put(COLUMN_BOUNTY_ICON_RES_NAME, iconResName)
            put(COLUMN_BOUNTY_COMPLETED, if (completed) 1 else 0)
            put(COLUMN_BOUNTY_PROGRESS_CURRENT, progressCurrent)
            put(COLUMN_BOUNTY_PROGRESS_TARGET, progressTarget)
            put(COLUMN_BOUNTY_LAST_RESET_DATE, lastResetDate)
        }
        val result = db.insertWithOnConflict(TABLE_BOUNTIES, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        db.close()
        return result != -1L
    }

    // Call this upon new user registration or daily refresh to set up default bounties
    fun initializeBountiesForUser(userId: String) {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val bountiesToInitialize = listOf(
            mapOf("def_id" to "turn_off_lights_bounty", "title" to "Turn Off All Lights", "description" to "Ensure all lights are off when leaving a room.", "xp_reward" to 5, "icon" to "ic_bounty_lights_off", "progress_target" to 1, "reset_daily" to true),
            mapOf("def_id" to "save_5_percent_weekly", "title" to "Save 5% more energy this Week", "description" to "Reduce your total weekly energy consumption by 5% compared to last week.", "xp_reward" to 30, "icon" to "ic_bounty_save_energy", "progress_target" to 5, "reset_daily" to false), // Example of a weekly bounty
            mapOf("def_id" to "hit_energy_goal_2_days", "title" to "Hit your energy goal 2 days in a row", "description" to "Stay below your daily energy goal for two consecutive days.", "xp_reward" to 100, "icon" to "ic_bounty_goal_streak", "progress_target" to 2, "reset_daily" to false)
        )

        for (bounty in bountiesToInitialize) {
            insertBounty(
                userId = userId,
                bountyDefId = bounty["def_id"] as String,
                title = bounty["title"] as String,
                description = bounty["description"] as String,
                xpReward = bounty["xp_reward"] as Int,
                iconResName = bounty["icon"] as String,
                progressTarget = bounty["progress_target"] as Int,
                lastResetDate = if (bounty["reset_daily"] == true) todayDate else ""
            )
        }
    }


    @SuppressLint("Range")
    fun getAllBountiesForUser(userId: String): List<Bounty> {
        val bountyList = mutableListOf<Bounty>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_BOUNTIES,
            null,
            "${COLUMN_USER_ID} = ?",
            arrayOf(userId),
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val iconResId = applicationContext.resources.getIdentifier(
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_ICON_RES_NAME)),
                    "drawable",
                    applicationContext.packageName
                )
                val bounty = Bounty(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_ID)),
                    defId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_DEF_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_TITLE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_DESCRIPTION)),
                    xpReward = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_XP_REWARD)),
                    iconResId = iconResId,
                    isCompleted = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_COMPLETED)) == 1,
                    progressCurrent = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_PROGRESS_CURRENT)),
                    progressTarget = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_PROGRESS_TARGET)),
                    lastResetDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BOUNTY_LAST_RESET_DATE))
                )
                bountyList.add(bounty)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return bountyList.sortedBy { it.isCompleted } // Show incomplete first
    }

    fun updateBountyStatus(
        userId: String,
        bountyDefId: String,
        completed: Boolean,
        progressCurrent: Int,
        lastResetDate: String? = null
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_BOUNTY_COMPLETED, if (completed) 1 else 0)
            put(COLUMN_BOUNTY_PROGRESS_CURRENT, progressCurrent)
            if (lastResetDate != null) {
                put(COLUMN_BOUNTY_LAST_RESET_DATE, lastResetDate)
            }
        }
        val rowsAffected = db.update(
            TABLE_BOUNTIES,
            values,
            "${COLUMN_USER_ID} = ? AND ${COLUMN_BOUNTY_DEF_ID} = ?",
            arrayOf(userId, bountyDefId)
        )
        db.close()
        return rowsAffected > 0
    }

    // Existing methods from DevDBHelper below (getAllDevicesForUser, getDevicesByTypeForUser, etc.)
    // Make sure these are still present and unchanged from your latest DevDBHelper version.

    fun insertDevice(
        name: String, // This is the type/category
        description: String, // This is the specific user-inputted name
        wattUsage: Double,
        dailyHoursGoal: Double,
        isOn: Boolean = true,
        userId: String
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_WATT_USAGE, wattUsage)
            put(COLUMN_DAILY_HOURS_GOAL, dailyHoursGoal)
            put(COLUMN_IS_ON, if (isOn) 1 else 0)
            put(COLUMN_USER_ID, userId)
            put(COLUMN_TIME_USED_TODAY_SECONDS, 0L)
            put(COLUMN_LAST_RESET_DATE, getCurrentDateString())
            put(COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS, if (isOn) System.currentTimeMillis() else 0L)
            put(COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY, 0)
        }
        val result = db.insert(TABLE_DEVICES, null, values)
        db.close()
        return result != -1L
    }

    @SuppressLint("Range")
    fun getDeviceById(deviceId: Int): Device? {
        val db = this.readableDatabase
        var device: Device? = null
        val cursor = db.query(
            TABLE_DEVICES, null, "${COLUMN_ID} = ?", arrayOf(deviceId.toString()),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            device = Device(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                wattUsage = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WATT_USAGE)),
                dailyHoursGoal = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DAILY_HOURS_GOAL)),
                isOn = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ON)) == 1,
                userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                timeUsedTodaySeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIME_USED_TODAY_SECONDS)),
                lastResetDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_RESET_DATE)),
                lastTurnOnTimestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS)),
                goalExceededAlertSentToday = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY)) == 1
            )
        }
        cursor.close()
        db.close()
        return device
    }

    @SuppressLint("Range")
    fun getAllDevicesForUser(userId: String): List<Device> {
        val deviceList = mutableListOf<Device>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_DEVICES,
            null,
            "${COLUMN_USER_ID} = ?",
            arrayOf(userId),
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val device = Device(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    wattUsage = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WATT_USAGE)),
                    dailyHoursGoal = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DAILY_HOURS_GOAL)),
                    isOn = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ON)) == 1,
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    timeUsedTodaySeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIME_USED_TODAY_SECONDS)),
                    lastResetDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_RESET_DATE)),
                    lastTurnOnTimestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS)),
                    goalExceededAlertSentToday = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY)) == 1
                )
                deviceList.add(device)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return deviceList
    }

    @SuppressLint("Range")
    fun getDevicesByTypeForUser(type: String, userId: String): List<Device> {
        val deviceList = mutableListOf<Device>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_DEVICES,
            null,
            "${COLUMN_USER_ID} = ? AND ${COLUMN_NAME} = ?",
            arrayOf(userId, type),
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val device = Device(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    wattUsage = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WATT_USAGE)),
                    dailyHoursGoal = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DAILY_HOURS_GOAL)),
                    isOn = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ON)) == 1,
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    timeUsedTodaySeconds = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIME_USED_TODAY_SECONDS)),
                    lastResetDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_RESET_DATE)),
                    lastTurnOnTimestampMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS)),
                    goalExceededAlertSentToday = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY)) == 1
                )
                deviceList.add(device)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return deviceList
    }

    fun updateDeviceTrackingData(device: Device): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_ON, if (device.isOn) 1 else 0)
            put(COLUMN_TIME_USED_TODAY_SECONDS, device.timeUsedTodaySeconds)
            put(COLUMN_LAST_RESET_DATE, device.lastResetDate)
            put(COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS, device.lastTurnOnTimestampMillis)
            put(COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY, if (device.goalExceededAlertSentToday) 1 else 0)
        }
        val rowsAffected = db.update(TABLE_DEVICES, values, "${COLUMN_ID} = ?", arrayOf(device.id.toString()))
        db.close()
        return rowsAffected
    }

    fun addOrUpdateDailyEnergy(userId: String, date: String, kwh: Double): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LOG_DATE, date)
            put(COLUMN_TOTAL_KWH, kwh)
            put(COLUMN_USER_ID, userId)
        }
        val rowsAffected = db.update(
            TABLE_DAILY_ENERGY,
            values,
            "${COLUMN_LOG_DATE} = ? AND ${COLUMN_USER_ID} = ?",
            arrayOf(date, userId)
        )
        if (rowsAffected == 0) {
            val result = db.insert(TABLE_DAILY_ENERGY, null, values)
            db.close()
            return result != -1L
        }
        db.close()
        return true
    }

    @SuppressLint("Range")
    fun getWeeklyEnergyConsumption(userId: String): Map<String, Double> {
        val dailyData = mutableMapOf<String, Double>()
        val db = this.readableDatabase

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val datesList = mutableListOf<String>()
        for (i in 6 downTo 0) {
            val date = Date(System.currentTimeMillis() - i * 24 * 60 * 60 * 1000)
            val dateStr = sdf.format(date)
            datesList.add(dateStr)
            dailyData[dateStr] = 0.0 // Initialize with 0.0
        }

        val query = "SELECT ${COLUMN_LOG_DATE}, ${COLUMN_TOTAL_KWH} FROM ${TABLE_DAILY_ENERGY} " +
                "WHERE ${COLUMN_USER_ID} = ? AND ${COLUMN_LOG_DATE} IN (${datesList.joinToString { "'$it'" }}) " +
                "ORDER BY ${COLUMN_LOG_DATE} ASC"

        val cursor = db.rawQuery(query, arrayOf(userId))

        if (cursor.moveToFirst()) {
            do {
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOG_DATE))
                val kwh = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_KWH))
                dailyData[date] = kwh
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return dailyData.toSortedMap()
    }
}