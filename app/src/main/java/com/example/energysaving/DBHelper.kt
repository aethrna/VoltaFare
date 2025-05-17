package com.example.energysaving

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, "UserDB", null, 1) {

    companion object {
        // It's good practice to define table and column names as constants
        const val TABLE_USERS = "users"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        // Add other column names if you have them, e.g., for a user_id if you add one later
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Use constants for table and column names
        db.execSQL("CREATE TABLE $TABLE_USERS($COLUMN_EMAIL TEXT PRIMARY KEY, $COLUMN_PASSWORD TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun registerUser(email: String, password: String): Boolean {
        val db = this.writableDatabase // Open database
        var result = -1L
        try {
            val values = ContentValues().apply {
                put(COLUMN_EMAIL, email)
                put(COLUMN_PASSWORD, password)
            }
            result = db.insert(TABLE_USERS, null, values)
        } finally {
            db.close() // Ensure database is closed even if an error occurs
        }
        return result != -1L
    }

    fun checkUser(email: String, password: String): Boolean {
        val db = this.readableDatabase // Open database
        var exists = false
        var cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL=? AND $COLUMN_PASSWORD=?",
            arrayOf(email, password)
        )
        try {
            exists = cursor.count > 0
        } finally {
            cursor.close() // Close cursor
            db.close()     // Ensure database is closed
        }
        return exists
    }

    fun checkEmailExists(email: String): Boolean {
        val db = this.readableDatabase // Open database
        var exists = false
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL=?",
            arrayOf(email)
        )
        try {
            exists = cursor.count > 0
        } finally {
            cursor.close() // Close cursor
            db.close()     // Ensure database is closed
        }
        return exists
    }

    fun getUserId(emailInput: String): String? {
        val db = this.readableDatabase // Open database
        var userId: String? = null
        val projection = arrayOf(COLUMN_EMAIL)
        val selection = "$COLUMN_EMAIL = ?"
        val selectionArgs = arrayOf(emailInput)

        val cursor = db.query(
            TABLE_USERS,
            projection,
            selection,
            selectionArgs,
            null, null, null
        )
        try {
            if (cursor.moveToFirst()) {
                userId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
            }
        } finally {
            cursor.close() // Close cursor
            db.close()     // Ensure database is closed
        }
        return userId
    }
}