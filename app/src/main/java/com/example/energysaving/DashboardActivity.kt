package com.example.energysaving

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit // Needed for SharedPreferences KTX extensions
import java.io.InputStream

class DashboardActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var btnLogout: Button // Declare the logout button variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        profileImage = findViewById(R.id.profileImage)
        val changePictureBtn = findViewById<Button>(R.id.btnChangePicture)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvBadges = findViewById<TextView>(R.id.tvBadges)
        btnLogout = findViewById(R.id.btnLogout) // Initialize the logout button

        // Load user data from SharedPreferences or database
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val username = prefs.getString("email", "Unknown User") // Using email as username
        tvUserName.text = username

        // TODO: Replace with badge logic
        tvBadges.text = " " // You had a space here, ensure it's intended or replace

        changePictureBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Set OnClickListener for the logout button
        btnLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        // Clear SharedPreferences related to login state
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        prefs.edit {
            putBoolean("isLoggedIn", false)
            putBoolean("userWantsToStayLoggedIn", false)
            remove("email")
            remove("currentUserId") // <<< --- ADD THIS LINE
            apply()
        }

        // Navigate to the RLog activity (or your designated entry/login screen)
        // RLog will then decide to show StartActivity because isLoggedIn is false.
        val intent = Intent(this, RLog::class.java)
        // Clear the activity stack so the user can't go back to DashboardActivity
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finish the DashboardActivity
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let {
                val imageStream: InputStream? = contentResolver.openInputStream(it)
                val selectedImage = BitmapFactory.decodeStream(imageStream)
                profileImage.setImageBitmap(selectedImage)
            }
        }
    }
}