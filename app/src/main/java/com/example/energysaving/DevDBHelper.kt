// File: app/src/main/java/com/example/energysaving/DevDBHelper.kt
package com.example.energysaving

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log // Added for logging
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// IMPORTANT: Increment your database version (e.g., from 3 to 4)
class DevDBHelper(context: Context) : SQLiteOpenHelper(context, "DeviceDB", null, 4) { // Version updated to 4

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

        // New Table and Columns for Daily Energy Logs - THESE MUST BE INSIDE COMPANION OBJECT
        const val TABLE_DAILY_ENERGY = "daily_energy_logs"
        const val COLUMN_LOG_ID = "log_id"
        const val COLUMN_LOG_DATE = "log_date" // YYYY-MM-DD
        const val COLUMN_TOTAL_KWH = "total_kwh"
    }

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

        // Create the new daily energy logs table
        db.execSQL(
            "CREATE TABLE ${TABLE_DAILY_ENERGY}(" +
                    "${COLUMN_LOG_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "${COLUMN_LOG_DATE} TEXT NOT NULL, " + // Date is unique per user
                    "${COLUMN_TOTAL_KWH} REAL DEFAULT 0.0, " +
                    "${COLUMN_USER_ID} TEXT NOT NULL, " +
                    "UNIQUE(${COLUMN_LOG_DATE}, ${COLUMN_USER_ID}))" // Ensure only one entry per user per day
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database schema upgrades
        if (oldVersion < 2) {
            // This part handles upgrades from version 1 to 2, if you had one.
            // If you're starting fresh, this might not be strictly necessary,
            // but it's good practice for existing users.
            db.execSQL("DROP TABLE IF EXISTS ${TABLE_DEVICES}")
            onCreate(db)
            return // Important to return to avoid further alter statements on a fresh table
        }
        if (oldVersion < 3) {
            // Add columns for device tracking (daily_hours_goal, time_used_today_seconds, etc.)
            db.execSQL("ALTER TABLE ${TABLE_DEVICES} ADD COLUMN ${COLUMN_DAILY_HOURS_GOAL} REAL DEFAULT 0.0")
            db.execSQL("ALTER TABLE ${TABLE_DEVICES} ADD COLUMN ${COLUMN_TIME_USED_TODAY_SECONDS} INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE ${TABLE_DEVICES} ADD COLUMN ${COLUMN_LAST_RESET_DATE} TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ${TABLE_DEVICES} ADD COLUMN ${COLUMN_LAST_TURN_ON_TIMESTAMP_MILLIS} INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE ${TABLE_DEVICES} ADD COLUMN ${COLUMN_GOAL_EXCEEDED_ALERT_SENT_TODAY} INTEGER DEFAULT 0")
        }
        if (oldVersion < 4) { // Upgrading to version 4, adding the new daily_energy_logs table
            // Drop and recreate the daily energy table if it exists (for development simplicity)
            db.execSQL("DROP TABLE IF EXISTS ${TABLE_DAILY_ENERGY}")
            // Re-create the new table. Note: onCreate will create *all* tables if they don't exist.
            // If you only want to add the new table, you'd just call its CREATE TABLE statement.
            // For simplicity with `DROP TABLE IF EXISTS`, calling onCreate is fine here.
            db.execSQL(
                "CREATE TABLE ${TABLE_DAILY_ENERGY}(" +
                        "${COLUMN_LOG_ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "${COLUMN_LOG_DATE} TEXT NOT NULL, " +
                        "${COLUMN_TOTAL_KWH} REAL DEFAULT 0.0, " +
                        "${COLUMN_USER_ID} TEXT NOT NULL, " +
                        "UNIQUE(${COLUMN_LOG_DATE}, ${COLUMN_USER_ID}))"
            )
        }
    }

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

        Log.d("DevDBHelper", "Weekly energy query: $query")
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