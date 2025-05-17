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

        val usernameEditText = findViewById<EditText>(R.id.etEmail)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val signIn = findViewById<Button>(R.id.btnSignIn)
        val goToRegister = findViewById<TextView>(R.id.goToRegister)
        val messageText = findViewById<TextView>(R.id.messageText)
        val rememberMeCheckbox = findViewById<CheckBox>(R.id.checkRemember)

        goToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        signIn.setOnClickListener {
            val email = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                messageText.text = "Please enter both email and password."
                messageText.setTextColor(getColor(android.R.color.holo_red_dark))
            } else {
                val isValid = dbHelper.checkUser(email, password)
                if (isValid) {
                    // Save remember me state
                    val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putBoolean("isLoggedIn", rememberMeCheckbox.isChecked)
                        putBoolean("isNewUser", true)  // Mark as new user
                        putString("email", email) // Save user email for profile
                        apply()
                    }


                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    messageText.text = "Invalid email or password"
                    messageText.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
        }
    }
}
