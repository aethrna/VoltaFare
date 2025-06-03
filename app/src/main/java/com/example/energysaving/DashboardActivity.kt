package com.example.energysaving

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream

class DashboardActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvDisplayTitle: TextView
    private lateinit var xpProgressBar: ProgressBar // NEW
    private lateinit var tvLevelProgress: TextView // NEW
    private lateinit var tvDetailDisplayName: TextView
    private lateinit var tvDetailDisplayAchievements: TextView
    private lateinit var tvDetailEmail: TextView
    private lateinit var tvDetailDisplayTitle: TextView
    private lateinit var btnLogout: Button
    private lateinit var dbHelper: DBHelper

    private lateinit var recyclerViewAchievements: RecyclerView
    private lateinit var achievementAdapter: AchievementAdapter

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        dbHelper = DBHelper(this)

        // Initialize UI elements
        profileImage = findViewById(R.id.profileImage)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvDisplayTitle = findViewById(R.id.tvDisplayTitle)
        xpProgressBar = findViewById(R.id.xpProgressBar) // NEW
        tvLevelProgress = findViewById(R.id.tvLevelProgress) // NEW
        tvDetailDisplayName = findViewById(R.id.tvDetailDisplayName)
        tvDetailDisplayAchievements = findViewById(R.id.tvDetailDisplayAchievements)
        tvDetailEmail = findViewById(R.id.tvDetailEmail)
        tvDetailDisplayTitle = findViewById(R.id.tvDetailDisplayTitle)
        btnLogout = findViewById(R.id.btnLogout)

        recyclerViewAchievements = findViewById(R.id.recyclerViewAchievements)
        recyclerViewAchievements.layoutManager = LinearLayoutManager(this)

        // Set up click listeners for "Edit" buttons
        findViewById<Button>(R.id.btnEditDisplayName).setOnClickListener { showEditDialog("Display Name", tvDetailDisplayName.text.toString(), DBHelper.COLUMN_DISPLAY_NAME) }
        findViewById<Button>(R.id.btnEditDisplayTitle).setOnClickListener { showEditDialog("Display Title", tvDetailDisplayTitle.text.toString(), DBHelper.COLUMN_DISPLAY_TITLE) }
        findViewById<Button>(R.id.btnEditDisplayAchievements).setOnClickListener {
            Toast.makeText(this, "Edit achievements coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Profile picture change listener
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Logout button listener
        btnLogout.setOnClickListener {
            performLogout()
        }

        // Handle back button if added to layout
        findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        loadUserProfile()
        loadShowcasedAchievements()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
        loadShowcasedAchievements()
    }

    private fun loadUserProfile() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)

        if (currentUserId != null) {
            val userProfile = dbHelper.getUserProfile(currentUserId)
            val currentXp = userProfile[DBHelper.COLUMN_XP]?.toIntOrNull() ?: 0 // Get XP
            val currentLevel = userProfile[DBHelper.COLUMN_LEVEL]?.toIntOrNull() ?: 1 // Get Level

            tvDisplayName.text = userProfile[DBHelper.COLUMN_DISPLAY_NAME]
            tvDisplayTitle.text = userProfile[DBHelper.COLUMN_DISPLAY_TITLE]

            tvDetailDisplayName.text = userProfile[DBHelper.COLUMN_DISPLAY_NAME]
            tvDetailEmail.text = userProfile[DBHelper.COLUMN_EMAIL]
            tvDetailDisplayTitle.text = userProfile[DBHelper.COLUMN_DISPLAY_TITLE]

            // Calculate progress for XP bar
            val xpNeededForNextLevel = calculateXpForLevel(currentLevel + 1)
            val xpForCurrentLevel = calculateXpForLevel(currentLevel)
            val xpProgressInCurrentLevel = currentXp - xpForCurrentLevel // XP accumulated in current level
            val xpNeededInCurrentLevel = xpNeededForNextLevel - xpForCurrentLevel // Total XP span for current level

            xpProgressBar.max = xpNeededInCurrentLevel // Max value for the progress bar
            xpProgressBar.progress = xpProgressInCurrentLevel // Current progress within this level

            tvLevelProgress.text = "Level $currentLevel ($currentXp / $xpNeededForNextLevel XP to next level)" // Display text


            // Load profile image
            val imageUriString = userProfile[DBHelper.COLUMN_PROFILE_IMAGE_URI]
            if (!imageUriString.isNullOrBlank()) {
                try {
                    val imageUri = Uri.parse(imageUriString)
                    val imageStream: InputStream? = contentResolver.openInputStream(imageUri)
                    val selectedImage = BitmapFactory.decodeStream(imageStream)
                    profileImage.setImageBitmap(selectedImage)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load profile image: ${e.message}", Toast.LENGTH_SHORT).show()
                    profileImage.setImageResource(R.mipmap.ic_launcher_round)
                }
            } else {
                profileImage.setImageResource(R.mipmap.ic_launcher_round)
            }

        } else {
            Toast.makeText(this, "User session not found. Please log in again.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // Define XP progression logic (e.g., exponential)
    private fun calculateXpForLevel(level: Int): Int {
        return when (level) {
            1 -> 0
            2 -> 100 // XP needed to reach level 2 (from 0 to 99)
            3 -> 250 // XP needed to reach level 3 (from 100 to 249)
            4 -> 450
            5 -> 700
            else -> {
                // Example: simple exponential growth
                (50 * (level - 1) * (level - 1)) + (50 * (level - 1)) + 100 // Adjust formula as needed
            }
        }
    }


    private fun loadShowcasedAchievements() {
        val achievements = listOf(
            Achievement("The Lorax", "Consistent top-tier performance", 85, R.mipmap.ic_launcher_round), // Replace with actual icons
            Achievement("Eco Curious", "Try and experience every feature in VoltaFare", 67, R.mipmap.ic_launcher_round),
            Achievement("Plugged In", "Log your first Device", 93, R.mipmap.ic_launcher_round)
        )
        achievementAdapter = AchievementAdapter(achievements)
        recyclerViewAchievements.adapter = achievementAdapter
    }

    private fun showEditDialog(title: String, currentValue: String, dbColumnName: String) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Edit $title")

        val input = EditText(this)
        input.setText(currentValue)
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val newValue = input.text.toString().trim()
            if (newValue.isNotEmpty()) {
                val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                val currentUserId = prefs.getString("currentUserId", null)
                if (currentUserId != null) {
                    if (dbHelper.updateUserProfileField(currentUserId, dbColumnName, newValue)) {
                        Toast.makeText(this, "$title updated successfully!", Toast.LENGTH_SHORT).show()
                        loadUserProfile()
                    } else {
                        Toast.makeText(this, "Failed to update $title.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "$title cannot be empty.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun performLogout() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        prefs.edit {
            putBoolean("isLoggedIn", false)
            putBoolean("userWantsToStayLoggedIn", false)
            remove("email")
            remove("currentUserId")
            apply()
        }

        val intent = Intent(this, RLog::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("This method has been deprecated...")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let { uri ->
                val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                val currentUserId = prefs.getString("currentUserId", null)
                if (currentUserId != null) {
                    if (dbHelper.updateUserProfileField(currentUserId, DBHelper.COLUMN_PROFILE_IMAGE_URI, uri.toString())) {
                        val imageStream: InputStream? = contentResolver.openInputStream(uri)
                        val selectedImage = BitmapFactory.decodeStream(imageStream)
                        profileImage.setImageBitmap(selectedImage)
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to save profile picture.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User not identified, cannot save picture.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}