package com.example.energysaving

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

abstract class BaseActivity : AppCompatActivity() {

    abstract val activeIndicator: Int

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setupNavigationBar()
        highlightActiveIndicator()
    }

    private fun setupNavigationBar() {
        val navItemAchievements = findViewById<LinearLayout>(R.id.navItemAchievements)
        val navItemRecommendations = findViewById<LinearLayout>(R.id.navItemRecommendations)
        val navItemHome = findViewById<LinearLayout>(R.id.navItemHome)
        val navItemProfile = findViewById<LinearLayout>(R.id.navItemProfile)
        val fabAddDevice = findViewById<FloatingActionButton>(R.id.fabAddDevice)

        navItemAchievements.setOnClickListener {
            if (this !is AchievementsActivity) {
                startActivity(Intent(this, AchievementsActivity::class.java))
            }
        }
        navItemRecommendations.setOnClickListener {
            if (this !is RecommendationsActivity) {
                startActivity(Intent(this, RecommendationsActivity::class.java))
            }
        }
        navItemHome.setOnClickListener {
            if (this !is MainActivity) {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
        navItemProfile.setOnClickListener {
            if (this !is DashboardActivity) {
                startActivity(Intent(this, DashboardActivity::class.java))
            }
        }
        fabAddDevice.setOnClickListener {
            startActivity(Intent(this, DeviceTypeActivity::class.java))
        }
    }

    private fun highlightActiveIndicator() {
        findViewById<View>(R.id.indicatorAchievements).visibility = if (activeIndicator == R.id.navItemAchievements) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.indicatorRecommendations).visibility = if (activeIndicator == R.id.navItemRecommendations) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.indicatorHome).visibility = if (activeIndicator == R.id.navItemHome) View.VISIBLE else View.INVISIBLE
        findViewById<View>(R.id.indicatorProfile).visibility = if (activeIndicator == R.id.navItemProfile) View.VISIBLE else View.INVISIBLE

        if (activeIndicator == R.id.navItemHome) {
            findViewById<View>(R.id.indicatorHome).setBackgroundResource(R.drawable.glow_indc)
        } else {
            findViewById<View>(R.id.indicatorHome).setBackgroundColor(resources.getColor(android.R.color.transparent, theme))
        }
    }
}