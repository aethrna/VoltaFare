package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class RLog : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val isNewUser = prefs.getBoolean("isNewUser", true)

        val nextActivity = if (isLoggedIn) {
            if (isNewUser) DeviceTypeActivity::class.java else MainActivity::class.java
        } else {
            StartActivity::class.java
        }

        startActivity(Intent(this, nextActivity))
        finish()
    }
}

