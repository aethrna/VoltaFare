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

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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
                    val currentUserIdString: String? = dbHelper.getUserId(email)
                    val wasRememberMeChecked = rememberMeCheckbox.isChecked
                    val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

                    // *** Get User ID ***
                    val currentUserId = dbHelper.getUserId(email) // Call the new method

                    with(sharedPref.edit()) {
                        putBoolean("userWantsToStayLoggedIn", wasRememberMeChecked)
                        if (wasRememberMeChecked) {
                            putBoolean("isLoggedIn", true)
                        } else {
                            putBoolean("isLoggedIn", false)
                        }
                        putString("email", email) // You already have this
                        if (currentUserIdString != null) {
                            putString("currentUserId", currentUserIdString)
                        } else {
                            // Handle case where userId couldn't be found - though login succeeded, this would be odd.
                            // Log an error, perhaps don't proceed.
                            // For now, we'll assume it's always found after successful login.
                        }
                        apply()
                    }

                    val intent = Intent(this, RLog::class.java)
                    intent.putExtra("LOGIN_SUCCESSFUL_THIS_SESSION", true)
                    startActivity(intent)
                    finishAffinity()
                } else {
                    messageText.text = "Invalid email or password"
                    messageText.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
        }
    }
}
