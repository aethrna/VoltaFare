package com.example.energysaving

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.InputStream

class DashboardActivity : BaseActivity() {

    override val activeIndicator: Int
        get() = R.id.navItemProfile

    private lateinit var profileImage: ImageView
    private lateinit var tvDisplayName: TextView
    private lateinit var tvDisplayTitle: TextView
    private lateinit var xpProgressBar: ProgressBar
    private lateinit var tvDetailDisplayName: TextView
    private lateinit var tvDetailDisplayAchievements: TextView
    private lateinit var tvDetailEmail: TextView
    private lateinit var tvDetailDisplayTitle: TextView
    private lateinit var btnLogout: Button
    private lateinit var dbHelper: DBHelper
    private lateinit var devDbHelper: DevDBHelper
    private lateinit var recyclerViewAchievements: RecyclerView
    private lateinit var achievementAdapter: AchievementAdapter

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_activity)
        initViews()
        setupClickListeners()
        loadUserProfile()
        loadShowcasedAchievements()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
        loadShowcasedAchievements()
    }

    private fun initViews() {
        dbHelper = DBHelper(this)
        devDbHelper = DevDBHelper(this)
        profileImage = findViewById(R.id.profileImage)
        tvDisplayName = findViewById(R.id.tvDisplayName)
        tvDisplayTitle = findViewById(R.id.tvDisplayTitle)
        xpProgressBar = findViewById(R.id.xpProgressBar)
        tvDetailDisplayName = findViewById(R.id.tvDetailDisplayName)
        tvDetailDisplayAchievements = findViewById(R.id.tvDetailDisplayAchievements)
        tvDetailEmail = findViewById(R.id.tvDetailEmail)
        tvDetailDisplayTitle = findViewById(R.id.tvDetailDisplayTitle)
        btnLogout = findViewById(R.id.btnLogout)
        recyclerViewAchievements = findViewById(R.id.recyclerViewAchievements)
        recyclerViewAchievements.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        findViewById<Button>(R.id.btnEditDisplayName).setOnClickListener {
            showEditDialog("Display Name", tvDetailDisplayName.text.toString(), DBHelper.COLUMN_DISPLAY_NAME)
        }
        findViewById<Button>(R.id.btnEditDisplayTitle).setOnClickListener {
            showTitleSelectionDialog()
        }
        findViewById<Button>(R.id.btnEditDisplayAchievements).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }
        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
        btnLogout.setOnClickListener {
            performLogout()
        }
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

            val imageUriString = userProfile[DBHelper.COLUMN_PROFILE_IMAGE_URI]
            if (!imageUriString.isNullOrBlank()) {
                try {
                    val imageUri = Uri.parse(imageUriString)
                    profileImage.setImageURI(imageUri)
                } catch (e: Exception) {
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
            1 -> 0
            2 -> 100
            3 -> 250
            4 -> 500
            else -> 500 + (level - 4) * 500
        }
    }

    private fun loadShowcasedAchievements() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)
        if (currentUserId != null) {
            val unlockedAchievements = devDbHelper.getAllAchievementsForUser(currentUserId).filter { it.isUnlocked }
            achievementAdapter = AchievementAdapter(unlockedAchievements.take(3))
            recyclerViewAchievements.adapter = achievementAdapter
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
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun performLogout() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        prefs.edit {
            clear()
            apply()
        }
        val intent = Intent(this, RLog::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showTitleSelectionDialog() {
        val currentUserId = ""
        val allAchievements = devDbHelper.getAllAchievementsForUser(currentUserId)
        val unlockedAchievements = allAchievements.filter { it.isUnlocked }
        if (unlockedAchievements.isEmpty()) {
            Toast.makeText(this, "No unlocked titles to choose from!", Toast.LENGTH_SHORT).show()
            return
        }

        val unlockedTitles = unlockedAchievements.map { it.title }.toTypedArray()

        android.app.AlertDialog.Builder(this)
            .setTitle("Select a New Title")
            .setItems(unlockedTitles) { dialog, which ->
                val selectedTitle = unlockedTitles[which]
                updateUserDisplayTitle(selectedTitle)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun updateUserDisplayTitle(newTitle: String) {
        val currentUserId = ""
        if (dbHelper.updateUserProfileField(currentUserId, DBHelper.COLUMN_DISPLAY_TITLE, newTitle)) {
            Toast.makeText(this, "Title updated successfully!", Toast.LENGTH_SHORT).show()
            loadUserProfile()
        } else {
            Toast.makeText(this, "Failed to update title.", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("This method has been deprecated in favor of the Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                val currentUserId = prefs.getString("currentUserId", null)
                if (currentUserId != null) {
                    if (dbHelper.updateUserProfileField(currentUserId, DBHelper.COLUMN_PROFILE_IMAGE_URI, uri.toString())) {
                        profileImage.setImageURI(uri)
                        Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}