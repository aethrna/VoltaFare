package com.example.energysaving

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

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

        btLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                messageText.text = "All fields are required."
            } else if (password != confirmPassword) {
                messageText.text = "Passwords do not match."
            } else if (dbHelper.checkUsernameExists(username)) {
                messageText.text = "Username already exists."
            } else {
                val success = dbHelper.registerUser(username, password)
                if (success) {
                    messageText.setTextColor(getColor(android.R.color.holo_green_dark))
                    messageText.text = "Registration successful! Redirecting to login..."
                    Handler(mainLooper).postDelayed({
                        val intent = Intent(this, StartActivity::class.java)
                        startActivity(intent)
                        finish()
                    }, 1500)
                } else {
                    messageText.text = "Registration failed. Try again."
                }
            }
        }
    }
}
