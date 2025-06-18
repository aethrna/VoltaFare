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

    private lateinit var dbHelper: DBHelper

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

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                messageText.text = "All fields are required."
            } else if (password != confirmPassword) {
                messageText.text = "Passwords do not match."
            } else if (dbHelper.checkEmailExists(username)) {
                messageText.text = "Username already exists."
            } else {
                val success = dbHelper.registerUser(username, password, "VoltaUser")
                if (success) {
                    val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    prefs.edit {
                        putBoolean("isNewUser", true)
                        putBoolean("isLoggedIn", false)
                        putBoolean("userWantsToStayLoggedIn", false)
                        remove("email")
                        remove("currentUserId")
                        apply()
                    }

                    messageText.setTextColor(getColor(android.R.color.holo_green_dark))
                    messageText.text = "Registration successful! Please login."
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish()
                    }, 2000)
                } else {
                    messageText.text = "Registration failed. Try again."
                }
            }
        }
    }
}