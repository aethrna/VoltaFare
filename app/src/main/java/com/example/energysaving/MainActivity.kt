package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: Dev_DBHelper
    private lateinit var greetingText: TextView
    private lateinit var imgProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = Dev_DBHelper(this)

        greetingText = findViewById(R.id.tvGreeting)
        imgProfile = findViewById(R.id.imgProfile)

        // Set greeting dynamically (optional)
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val username = prefs.getString("username", "User")
        greetingText.text = "Hi there, $username"

        imgProfile.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        // Bottom navigation
        findViewById<Button>(R.id.btnAchievements).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }

        findViewById<Button>(R.id.btnRecommendations).setOnClickListener {
            startActivity(Intent(this, RecommendationsActivity::class.java))
        }

        findViewById<Button>(R.id.btnAddDevice).setOnClickListener {
            startActivity(Intent(this, DeviceTypeActivity::class.java))
        }

        findViewById<Button>(R.id.btnHome).setOnClickListener {
            Toast.makeText(this, "You're already on Home", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnProfile).setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDevices)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val devices = dbHelper.getAllDevices()
        val groupedDevices = groupDevices(devices)

        val adapter = DeviceCardAdapter(groupedDevices)
        recyclerView.adapter = adapter
    }

    private fun groupDevices(devices: List<Device>): List<DeviceGroup> {
        val groupedDevices = mutableListOf<DeviceGroup>()
        val typeGroups = devices.groupBy { if (it.name == "Custom") it.description else it.name }

        for ((type, devicesOfType) in typeGroups) {
            val totalEnergy = devicesOfType.sumOf { it.wattUsage * it.dailyHours * 7 / 1000 }
            groupedDevices.add(DeviceGroup(type, devicesOfType, totalEnergy))
        }
        return groupedDevices
    }
}
