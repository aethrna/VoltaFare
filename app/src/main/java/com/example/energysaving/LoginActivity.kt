package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        dbHelper = DBHelper(this)

        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val messageTextView = findViewById<TextView>(R.id.messageTextView)
        val goToRegister = findViewById<TextView>(R.id.goToRegister)
        goToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                messageTextView.text = "Please enter both username and password."
                messageTextView.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                val isValid = dbHelper.checkUser(username, password)
                if (isValid) {
                    messageTextView.setTextColor(getColor(android.R.color.holo_green_dark))
                    messageTextView.text = "Login successful!"
                    // Optional: Navigate to home screen
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    messageTextView.setTextColor(getColor(android.R.color.holo_red_dark))
                    messageTextView.text = "Invalid username or password."
                }
            }
        }
    }
}
