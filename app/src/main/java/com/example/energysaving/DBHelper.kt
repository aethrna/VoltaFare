package com.example.energysaving

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.annotation.SuppressLint // Import annotation

class DBHelper(context: Context) : SQLiteOpenHelper(context, "UserDB", null, 3) { // Version updated to 3

    companion object {
        const val TABLE_USERS = "users"
        const val COLUMN_EMAIL = "email"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_DISPLAY_NAME = "display_name"
        const val COLUMN_DISPLAY_TITLE = "display_title"
        const val COLUMN_PROFILE_IMAGE_URI = "profile_image_uri"
        const val COLUMN_XP = "xp"
        const val COLUMN_LEVEL = "level"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE ${TABLE_USERS}(" +
                "${COLUMN_EMAIL} TEXT PRIMARY KEY, " +
                "${COLUMN_PASSWORD} TEXT, " +
                "${COLUMN_DISPLAY_NAME} TEXT DEFAULT '', " +
                "${COLUMN_DISPLAY_TITLE} TEXT DEFAULT '', " +
                "${COLUMN_PROFILE_IMAGE_URI} TEXT DEFAULT '', " +
                "${COLUMN_XP} INTEGER DEFAULT 0, " +
                "${COLUMN_LEVEL} INTEGER DEFAULT 1)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE ${TABLE_USERS} ADD COLUMN ${COLUMN_DISPLAY_NAME} TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ${TABLE_USERS} ADD COLUMN ${COLUMN_DISPLAY_TITLE} TEXT DEFAULT ''")
            db.execSQL("ALTER TABLE ${TABLE_USERS} ADD COLUMN ${COLUMN_PROFILE_IMAGE_URI} TEXT DEFAULT ''")
        }
        if (oldVersion < 3) { // Upgrade to version 3, adding XP and Level columns
            db.execSQL("ALTER TABLE ${TABLE_USERS} ADD COLUMN ${COLUMN_XP} INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE ${TABLE_USERS} ADD COLUMN ${COLUMN_LEVEL} INTEGER DEFAULT 1")
        }
    }

    fun registerUser(email: String, password: String, displayName: String): Boolean {
        val db = this.writableDatabase
        var result = -1L
        try {
            val values = ContentValues().apply {
                put(COLUMN_EMAIL, email)
                put(COLUMN_PASSWORD, password)
                put(COLUMN_DISPLAY_NAME, displayName)
                put(COLUMN_DISPLAY_TITLE, "New Member")
                put(COLUMN_XP, 0)
                put(COLUMN_LEVEL, 1)
            }
            result = db.insert(TABLE_USERS, null, values)
        } finally {
            db.close()
        }
        return result != -1L
    }

    fun checkUser(email: String, password: String): Boolean {
        val db = this.readableDatabase
        var exists = false
        var cursor = db.rawQuery(
            "SELECT * FROM ${TABLE_USERS} WHERE ${COLUMN_EMAIL}=? AND ${COLUMN_PASSWORD}=?",
            arrayOf(email, password)
        )
        try {
            exists = cursor.count > 0
        } finally {
            cursor.close()
            db.close()
        }
        return exists
    }

    fun checkEmailExists(email: String): Boolean {
        val db = this.readableDatabase
        var exists = false
        val cursor = db.rawQuery(
            "SELECT * FROM ${TABLE_USERS} WHERE ${COLUMN_EMAIL}=?",
            arrayOf(email)
        )
        try {
            exists = cursor.count > 0
        } finally {
            cursor.close()
            db.close()
        }
        return exists
    }

    @SuppressLint("Range")
    fun getUserId(emailInput: String): String? {
        val db = this.readableDatabase
        var userId: String? = null
        val projection = arrayOf(COLUMN_EMAIL)
        val selection = "${COLUMN_EMAIL} = ?"
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
            cursor.close()
            db.close()
        }
        return userId
    }

    @SuppressLint("Range")
    fun getUserProfile(email: String): Map<String, String?> {
        val db = this.readableDatabase
        val profile = mutableMapOf<String, String?>()
        val cursor = db.rawQuery(
            "SELECT ${COLUMN_EMAIL}, ${COLUMN_DISPLAY_NAME}, ${COLUMN_DISPLAY_TITLE}, ${COLUMN_PROFILE_IMAGE_URI}, ${COLUMN_XP}, ${COLUMN_LEVEL} FROM ${TABLE_USERS} WHERE ${COLUMN_EMAIL}=?",
            arrayOf(email)
        )
        try {
            if (cursor.moveToFirst()) {
                profile[COLUMN_EMAIL] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL))
                profile[COLUMN_DISPLAY_NAME] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISPLAY_NAME))
                profile[COLUMN_DISPLAY_TITLE] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DISPLAY_TITLE))
                profile[COLUMN_PROFILE_IMAGE_URI] = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE_URI))
                profile[COLUMN_XP] = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_XP)).toString()
                profile[COLUMN_LEVEL] = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEVEL)).toString()
            }
        } finally {
            cursor.close()
            db.close()
        }
        return profile
    }

    fun updateUserProfileField(email: String, fieldName: String, newValue: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(fieldName, newValue)
        }
        val rowsAffected = db.update(
            TABLE_USERS, values, "${COLUMN_EMAIL} = ?", arrayOf(email)
        )
        db.close()
        return rowsAffected > 0
    }

    fun updateUserXPAndLevel(email: String, newXp: Int, newLevel: Int): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_XP, newXp)
            put(COLUMN_LEVEL, newLevel)
        }
        val rowsAffected = db.update(
            TABLE_USERS, values, "${COLUMN_EMAIL} = ?", arrayOf(email)
        )
        db.close()
        return rowsAffected > 0
    }
}