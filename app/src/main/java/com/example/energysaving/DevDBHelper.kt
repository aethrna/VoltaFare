// File: app/src/main/java/com/example/energysaving/DevDBHelper.kt
package com.example.energysaving

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// IMPORTANT: Increment your database version (e.g., from 2 to 3)
class DevDBHelper(context: Context) : SQLiteOpenHelper(context, "DeviceDB", null, 3) {

    companion object {
        const val TABLE_DEVICES = "devices"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_WATT_USAGE = "watt_usage"
        // const val COLUMN_DAILY_HOURS = "daily_hours" // This was the old column name
        const val COLUMN_IS_ON = "is_on"
        const val COLUMN_USER_ID = "user_id"

        // New Column Names
        const val COLUMN_DAILY_HOURS_GOAL = "daily_hours_goal" // Replaces/renames old daily_hours
        const val COLUMN_TIME_USED_TODAY_SECONDS = "time_used_today_seconds"
        const val COLUMN_LAST_RESET_DATE = "last_reset_date"
        const val COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS = "last_turn_on_timestamp_millis"
        const val COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY = "goal_exceeded_alert_sent_today"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_DEVICES(" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_NAME TEXT, " +
                    "$COLUMN_DESCRIPTION TEXT, " +
                    "$COLUMN_WATT_USAGE REAL, " +
                    "$COLUMN_DAILY_HOURS_GOAL REAL DEFAULT 0.0, " + // New field for goal
                    "$COLUMN_IS_ON INTEGER DEFAULT 1, " +
                    "$COLUMN_USER_ID TEXT NOT NULL, " +
                    // New columns for tracking
                    "$COLUMN_TIME_USED_TODAY_SECONDS INTEGER DEFAULT 0, " +
                    "$COLUMN_LAST_RESET_DATE TEXT DEFAULT '', " +
                    "$COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS INTEGER DEFAULT 0, " +
                    "$COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY INTEGER DEFAULT 0)" // 0 for false, 1 for true
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) { // If upgrading from a version before COLUMN_IS_ON and COLUMN_USER_ID
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DEVICES")
            onCreate(db)
            return // Important to return to avoid further alter statements on a fresh table
        }
        if (oldVersion < 3) { // Upgrading to version 3, adding new tracking columns
            // If your old daily_hours column was meant to be the goal, you might rename it.
            // For this example, we're adding a new specific column for the goal.
            // You might need to handle data migration from an old "daily_hours" column if it existed.
            db.execSQL("ALTER TABLE $TABLE_DEVICES ADD COLUMN $COLUMN_DAILY_HOURS_GOAL REAL DEFAULT 0.0")
            db.execSQL("ALTER TABLE $TABLE_DEVICES ADD COLUMN $COLUMN_TIME_USED_TODAY_SECONDS INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_DEVICES ADD COLUMN $COLUMN_LAST_RESET_DATE TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE $TABLE_DEVICES ADD COLUMN $COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_DEVICES ADD COLUMN $COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY INTEGER DEFAULT 0")
        }
    }

    // Update insertDevice to use dailyHoursGoal and initialize new fields
    fun insertDevice(
        name: String,
        description: String,
        wattUsage: Double,
        dailyHoursGoal: Double, // Parameter name changed to reflect its purpose
        isOn: Boolean = true,
        userId: String
    ): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_WATT_USAGE, wattUsage)
            put(COLUMN_DAILY_HOURS_GOAL, dailyHoursGoal) // Saving to the new/renamed column
            put(COLUMN_IS_ON, if (isOn) 1 else 0)
            put(COLUMN_USER_ID, userId)
            // Initialize new tracking fields
            put(COLUMN_TIME_USED_TODAY_SECONDS, 0L)
            put(COLUMN_LAST_RESET_DATE, getCurrentDateString()) // From Device.kt (or move helper here)
            put(COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS, if (isOn) System.currentTimeMillis() else 0L)
            put(COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY, 0) // 0 for false
        }
        val result = db.insert(TABLE_DEVICES, null, values)
        db.close()
        return result != -1L
    }

    // IMPORTANT: Update your device fetching methods (getAllDevicesForUser, getDevicesByTypeForUser)
    // to read these new columns from the cursor and populate the new fields in the Device objects.
    // Example snippet for fetching one device (adapt for your list fetching methods):
    @SuppressLint("Range") // Suppress lint warning for getColumnIndexOrThrow
    fun getDeviceById(deviceId: Int): Device? { // Example helper method
        val db = this.readableDatabase
        var device: Device? = null
        val cursor = db.query(
            TABLE_DEVICES, null, "$COLUMN_ID = ?", arrayOf(deviceId.toString()),
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
                // Populate new fields
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

    // You'll need similar updates in getAllDevicesForUser and getDevicesByTypeForUser.

    // New method to update all tracking-related data for a device
    fun updateDeviceTrackingData(device: Device): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_ON, if (device.isOn) 1 else 0)
            put(COLUMN_TIME_USED_TODAY_SECONDS, device.timeUsedTodaySeconds)
            put(COLUMN_LAST_RESET_DATE, device.lastResetDate)
            put(COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS, device.lastTurnOnTimestampMillis)
            put(COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY, if (device.goalExceededAlertSentToday) 1 else 0)
            // dailyHoursGoal and wattUsage are typically set during device creation/editing,
            // but if they can change dynamically, include them here too.
        }
        val rowsAffected = db.update(TABLE_DEVICES, values, "$COLUMN_ID = ?", arrayOf(device.id.toString()))
        db.close()
        return rowsAffected
    }
    // Your existing updateDeviceState might just call this or be merged.
}