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
    private lateinit var xpProgressBar: ProgressBar
    private lateinit var tvLevelProgress: TextView
    private lateinit var tvDetailDisplayName: TextView
    private lateinit var tvDetailDisplayAchievements: TextView
    private lateinit var tvDetailEmail: TextView
    private lateinit var tvDetailDisplayTitle: TextView
    private lateinit var btnLogout: Button
    private lateinit var dbHelper: DBHelper
    private lateinit var devDbHelper: DevDBHelper // ADD THIS LINE

    private lateinit var recyclerViewAchievements: RecyclerView
    private lateinit var achievementAdapter: AchievementAdapter

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)

        dbHelper = DBHelper(this)
        devDbHelper = DevDBHelper(this) // INITIALIZE DEVDBHELPER HERE

        // Initialize UI elements
        profileImage = findViewById(R.id.profileImage)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvDisplayTitle = findViewById(R.id.tvDisplayTitle)
        xpProgressBar = findViewById(R.id.xpProgressBar)
        tvLevelProgress = findViewById(R.id.tvLevelProgress)
        tvDetailDisplayName = findViewById(R.id.tvDetailDisplayName)
        tvDetailDisplayAchievements = findViewById(R.id.tvDetailDisplayAchievements)
        tvDetailEmail = findViewById(R.id.tvDetailEmail)
        tvDetailDisplayTitle = findViewById(R.id.tvDetailDisplayTitle)
        btnLogout = findViewById(R.id.btnLogout)

        recyclerViewAchievements = findViewById(R.id.recyclerViewAchievements)
        recyclerViewAchievements.layoutManager = LinearLayoutManager(this)

        // Set up click listeners for "Edit" buttons
        findViewById<Button>(R.id.btnEditDisplayName).setOnClickListener { showEditDialog("Display Name", tvDetailDisplayName.text.toString(), DBHelper.COLUMN_DISPLAY_NAME) }
        // MODIFIED: Changed behavior for btnEditDisplayTitle to open achievement selection dialog
        findViewById<Button>(R.id.btnEditDisplayTitle).setOnClickListener {
            showAchievementTitleSelectionDialog()
        }
        findViewById<Button>(R.id.btnEditDisplayAchievements).setOnClickListener {
            // This button might lead to the AchievementsActivity where users can pick which to showcase
            Toast.makeText(this, "Edit showcased achievements coming soon!", Toast.LENGTH_SHORT).show()
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
            val currentXp = userProfile[DBHelper.COLUMN_XP]?.toIntOrNull() ?: 0
            val currentLevel = userProfile[DBHelper.COLUMN_LEVEL]?.toIntOrNull() ?: 1

            tvDisplayName.text = userProfile[DBHelper.COLUMN_DISPLAY_NAME]
            tvDisplayTitle.text = userProfile[DBHelper.COLUMN_DISPLAY_TITLE]

            tvDetailDisplayName.text = userProfile[DBHelper.COLUMN_DISPLAY_NAME]
            tvDetailEmail.text = userProfile[DBHelper.COLUMN_EMAIL]
            tvDetailDisplayTitle.text = userProfile[DBHelper.COLUMN_DISPLAY_TITLE]

            val xpNeededForNextLevel = calculateXpForLevel(currentLevel + 1)
            val xpForCurrentLevel = calculateXpForLevel(currentLevel)
            val xpProgressInCurrentLevel = currentXp - xpForCurrentLevel
            val xpNeededInCurrentLevel = xpNeededForNextLevel - xpForCurrentLevel

            xpProgressBar.max = xpNeededInCurrentLevel
            xpProgressBar.progress = xpProgressInCurrentLevel

            tvLevelProgress.text = "Level $currentLevel ($currentXp / $xpNeededForNextLevel XP to next level)"

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

    private fun calculateXpForLevel(level: Int): Int {
        return when (level) {
            1 -> 0 // Starts at 0 XP
            2 -> 5 // Only 5 XP needed for Level 2 for testing
            3 -> 15 // Example: 15 XP for Level 3
            4 -> 30
            5 -> 50
            else -> {
                // Your exponential or linear formula for higher levels
                50 + (level - 5) * 20 // Example: 20 XP per level after 5
            }
        }
    }

    private fun loadShowcasedAchievements() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)

        if (currentUserId != null) {
            val allAchievements = devDbHelper.getAllAchievementsForUser(currentUserId)
            val unlockedAchievements = allAchievements.filter { it.isUnlocked }

            if (unlockedAchievements.isEmpty()) {
                // Optionally display a message if no achievements are unlocked
                // For example: tvShowcasedAchievementsTitle.text = "No achievements unlocked yet."
                // And hide recyclerViewAchievements or show a placeholder.
                // For now, just set an empty adapter.
                achievementAdapter = AchievementAdapter(emptyList())
            } else {
                // Limit the number of showcased achievements if desired, e.g., .take(3)
                achievementAdapter = AchievementAdapter(unlockedAchievements)
            }
            recyclerViewAchievements.adapter = achievementAdapter
        } else {
            // Handle case where user is not identified, e.g., set empty adapter
            achievementAdapter = AchievementAdapter(emptyList())
            recyclerViewAchievements.adapter = achievementAdapter
            Toast.makeText(this, "Cannot load achievements: User not identified.", Toast.LENGTH_SHORT).show()
        }
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

    /**
     * Shows a dialog allowing the user to select an unlocked achievement title as their display title.
     */
    private fun showAchievementTitleSelectionDialog() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)

        if (currentUserId == null) {
            Toast.makeText(this, "User not identified. Cannot select title.", Toast.LENGTH_SHORT).show()
            return
        }

        val allAchievements = devDbHelper.getAllAchievementsForUser(currentUserId)
        val unlockedAchievements = allAchievements.filter { it.isUnlocked }

        if (unlockedAchievements.isEmpty()) {
            Toast.makeText(this, "No achievements unlocked yet to use as a title!", Toast.LENGTH_LONG).show()
            return
        }

        // Extract just the titles from the unlocked achievements
        val achievementTitles = unlockedAchievements.map { it.title }.toTypedArray()

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select Display Title")
        builder.setItems(achievementTitles) { dialog, which ->
            val selectedTitle = achievementTitles[which]
            updateDisplayTitle(selectedTitle) // Call the helper to update the database and UI
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    /**
     * Updates the user's display title in the database and refreshes the UI.
     */
    private fun updateDisplayTitle(newTitle: String) {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)
        if (currentUserId != null) {
            if (dbHelper.updateUserProfileField(currentUserId, DBHelper.COLUMN_DISPLAY_TITLE, newTitle)) {
                Toast.makeText(this, "Display Title updated successfully!", Toast.LENGTH_SHORT).show()
                loadUserProfile() // Reload to update the displayed title
            } else {
                Toast.makeText(this, "Failed to update Display Title.", Toast.LENGTH_SHORT).show()
            }
        }
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
                // *** IMPORTANT FIX: Take persistent URI permission ***
                val contentResolver = applicationContext.contentResolver
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: SecurityException) {
                    // Handle cases where permission cannot be taken (e.g., system doesn't support it for this URI)
                    Toast.makeText(this, "Failed to get persistent access to image. Please try another image or grant permissions manually.", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                    return // Exit if persistent permission can't be obtained
                }
                // *** End of FIX ***

                val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                val currentUserId = prefs.getString("currentUserId", null)
                if (currentUserId != null) {
                    if (dbHelper.updateUserProfileField(currentUserId, DBHelper.COLUMN_PROFILE_IMAGE_URI, uri.toString())) {
                        try {
                            val imageStream: InputStream? = contentResolver.openInputStream(uri)
                            val selectedImage = BitmapFactory.decodeStream(imageStream)
                            profileImage.setImageBitmap(selectedImage)
                            Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error displaying image: ${e.message}", Toast.LENGTH_LONG).show()
                            profileImage.setImageResource(R.mipmap.ic_launcher_round) // Fallback
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(this, "Failed to save profile picture URI.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "User not identified, cannot save picture.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}