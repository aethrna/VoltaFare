package com.example.energysaving

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper // Import Looper for Handler
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit // For SharedPreferences KTX

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper // Assuming this is your user DB helper

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DBHelper(this)

        val usernameEditText = findViewById<EditText>(R.id.registerUsername)
        val passwordEditText = findViewById<EditText>(R.id.registerPassword)
        val confirmEditText = findViewById<EditText>(R.id.registerConfirmPassword)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val messageText = findViewById<TextView>(R.id.registerMessage)
        val btLogin = findViewById<TextView>(R.id.backToLogin)

        btLogin.setOnClickListener {
            // Consider finishing RegisterActivity if you go back to Login to prevent stack buildup
            // startActivity(Intent(this, LoginActivity::class.java))
            // finish()
            // Or, if StartActivity is your login/register hub:
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim() // Assuming username is email
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                messageText.text = "All fields are required."
            } else if (password != confirmPassword) {
                messageText.text = "Passwords do not match."
            } else if (dbHelper.checkEmailExists(username)) { // Assuming checkEmailExists is for username/email
                messageText.text = "Username already exists."
            } else {
                val success = dbHelper.registerUser(username, password, "VoltaUser") // Pass username as initial display name
                if (success) {
                    // *** IMPORTANT: Clear old session data and reset isNewUser flag ***
                    val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    prefs.edit {
                        putBoolean("isNewUser", true)
                        putBoolean("isLoggedIn", false)
                        putBoolean("userWantsToStayLoggedIn", false)
                        remove("email")
                        remove("currentUserId") // <<< --- ADD THIS LINE
                        apply()
                    }

                    messageText.setTextColor(getColor(android.R.color.holo_green_dark))
                    messageText.text = "Registration successful! Please login." // Updated message
                    Handler(Looper.getMainLooper()).postDelayed({ // Use Looper.getMainLooper()
                        // Go to StartActivity, so user has to log in with new credentials
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish() // Clear this activity and its parent stack if any
                    }, 2000) // Increased delay slightly for message visibility
                } else {
                    messageText.text = "Registration failed. Try again."
                }
            }
        }
    }
}