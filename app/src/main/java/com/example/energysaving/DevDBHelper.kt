package com.example.energysaving

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DevDBHelper(context: Context) : SQLiteOpenHelper(context, "DeviceDB", null, 2) { // <-- Version incremented to 2

    companion object {
        const val TABLE_DEVICES = "devices"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_WATT_USAGE = "watt_usage"
        const val COLUMN_DAILY_HOURS = "daily_hours"
        const val COLUMN_IS_ON = "is_on"
        const val COLUMN_USER_ID = "user_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_DEVICES(" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$COLUMN_NAME TEXT, " +
                    "$COLUMN_DESCRIPTION TEXT, " +
                    "$COLUMN_WATT_USAGE REAL, " +
                    "$COLUMN_DAILY_HOURS REAL, " +
                    "$COLUMN_IS_ON INTEGER DEFAULT 1, " +
                    "$COLUMN_USER_ID TEXT NOT NULL)" // Added: user_id column, make it NOT NULL
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Simple upgrade: drop and recreate.
        // WARNING: This will delete all existing data if the version changes.
        if (oldVersion < 2) { // Check if upgrading from a version before 'is_on' was added
            db.execSQL("DROP TABLE IF EXISTS $TABLE_DEVICES")
            onCreate(db)
        }
    }

    // Update insertDevice to handle the isOn state (though it defaults in schema)
    fun insertDevice(
        name: String,
        description: String,
        wattUsage: Double,
        dailyHours: Double,
        isOn: Boolean = true,
        userId: String
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_DESCRIPTION, description)
            put(COLUMN_WATT_USAGE, wattUsage)
            put(COLUMN_DAILY_HOURS, dailyHours)
            put(COLUMN_IS_ON, if (isOn) 1 else 0)
            put(COLUMN_USER_ID, userId) // Add userId
        }
        val result = db.insert(TABLE_DEVICES, null, values)
        return result != -1L
    }

    // Update getAllDevices to read the isOn state
    fun getAllDevicesForUser(userId: String): List<Device> {
        val deviceList = mutableListOf<Device>()
        val db = readableDatabase
        val selection = "$COLUMN_USER_ID = ?"
        val selectionArgs = arrayOf(userId)
        val cursor = db.query(
            TABLE_DEVICES,
            null, // All columns
            selection,
            selectionArgs,
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val device = Device(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    wattUsage = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WATT_USAGE)),
                    dailyHours = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DAILY_HOURS)),
                    isOn = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ON)) == 1,
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)) // Populate userId
                )
                deviceList.add(device)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return deviceList
    }

    // New function to update device state
    fun updateDeviceState(deviceId: Int, isOn: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IS_ON, if (isOn) 1 else 0)
        }
        return db.update(TABLE_DEVICES, values, "$COLUMN_ID = ?", arrayOf(deviceId.toString()))
    }

    // New function to get devices by type (name)
    fun getDevicesByTypeForUser(deviceType: String, userId: String): List<Device> {
        val deviceList = mutableListOf<Device>()
        val db = readableDatabase
        // Modify selection to include both device type (name) and userId
        val selection = "$COLUMN_NAME = ? AND $COLUMN_USER_ID = ?"
        val selectionArgs = arrayOf(deviceType, userId)

        val cursor = db.query(
            TABLE_DEVICES,
            null, // All columns
            selection,
            selectionArgs,
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val device = Device(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                    wattUsage = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WATT_USAGE)),
                    dailyHours = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DAILY_HOURS)),
                    isOn = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_ON)) == 1,
                    userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)) // Populate userId
                )
                deviceList.add(device)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return deviceList
    }
}