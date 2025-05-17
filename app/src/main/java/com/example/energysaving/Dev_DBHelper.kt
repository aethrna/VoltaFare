package com.example.energysaving

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Dev_DBHelper(context: Context) : SQLiteOpenHelper(context, "DeviceDB", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE devices(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT, " +
                    "description TEXT, " +
                    "watt_usage REAL, " +
                    "daily_hours REAL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS devices")
        onCreate(db)
    }

    fun insertDevice(name: String, description: String, wattUsage: Double, dailyHours: Double): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", name)
            put("description", description)
            put("watt_usage", wattUsage)
            put("daily_hours", dailyHours)
        }
        val result = db.insert("devices", null, values)
        return result != -1L
    }

    fun getAllDevices(): List<Device> {
        val deviceList = mutableListOf<Device>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM devices", null)

        if (cursor.moveToFirst()) {
            do {
                val device = Device(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    wattUsage = cursor.getDouble(cursor.getColumnIndexOrThrow("watt_usage")),
                    dailyHours = cursor.getDouble(cursor.getColumnIndexOrThrow("daily_hours"))
                )
                deviceList.add(device)
            } while (cursor.moveToNext())
        }

        cursor.close()
        return deviceList
    }
}
