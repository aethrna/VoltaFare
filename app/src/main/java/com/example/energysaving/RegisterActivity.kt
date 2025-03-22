package com.example.energysaving

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        dbHelper = DBHelper(this)

        val usernameEditText = findViewById<EditText>(R.id.registerUsername)
        val passwordEditText = findViewById<EditText>(R.id.registerPassword)
        val confirmEditText = findViewById<EditText>(R.id.registerConfirmPassword)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val messageText = findViewById<TextView>(R.id.registerMessage)

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
                    messageText.text = "Registration successful! Go back to login."
                } else {
                    messageText.text = "Registration failed. Try again."
                }
            }
        }
    }
}
