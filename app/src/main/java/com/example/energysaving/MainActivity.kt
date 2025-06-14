package com.example.energysaving

import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineDataSet
import androidx.core.graphics.toColorInt
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DevDBHelper
    private lateinit var userDbHelper: DBHelper // NEW: Add a reference to your user DB helper
    private lateinit var greetingText: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var weeklyEnergyChart: LineChart


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DevDBHelper(this)
        userDbHelper = DBHelper(this) // NEW: Initialize your user DB helper

        greetingText = findViewById(R.id.tvGreeting)
        imgProfile = findViewById(R.id.imgProfile)

        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userEmail = prefs.getString("email", "User") // Get email to fetch profile
        val currentUserId = prefs.getString("currentUserId", null)

        // NEW: Fetch display name from DBHelper
        var displayName = userEmail
        if (currentUserId != null) {
            val userProfile = userDbHelper.getUserProfile(currentUserId)
            displayName = userProfile[DBHelper.COLUMN_DISPLAY_NAME] ?: userEmail
        }

        greetingText.text = "Hi there, $displayName" // Use display name for greeting
        val dateTextView = findViewById<TextView>(R.id.dateTextView)

        val currentDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date())
        dateTextView.text = currentDate

        imgProfile.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

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
        weeklyEnergyChart = findViewById(R.id.weeklyEnergyChart)
        setupWeeklyEnergyChart()
    }

    override fun onResume() {
        super.onResume()
        setupRecyclerView()
        setupWeeklyEnergyChart() // Refresh chart data on resume

        // Also update the greeting text on resume to reflect any changes made in DashboardActivity
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userEmail = prefs.getString("email", "User")
        val currentUserId = prefs.getString("currentUserId", null)
        var displayName = userEmail
        if (currentUserId != null) {
            val userProfile = userDbHelper.getUserProfile(currentUserId)
            displayName = userProfile[DBHelper.COLUMN_DISPLAY_NAME] ?: userEmail
        }
        greetingText.text = "Hi there, $displayName"
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDevices)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)

        var devicesToDisplay: List<Device> = emptyList()

        if (currentUserId != null) {
            devicesToDisplay = dbHelper.getAllDevicesForUser(currentUserId)
        } else {
            Toast.makeText(this, "Error: User not identified. Cannot load devices.", Toast.LENGTH_LONG).show()
        }

        val groupedDevices = groupDevices(devicesToDisplay)
        val adapter = DeviceCardAdapter(groupedDevices)
        recyclerView.adapter = adapter
    }

    private fun groupDevices(devices: List<Device>): List<DeviceGroup> {
        val groupedDevices = mutableListOf<DeviceGroup>()
        val typeGroups = devices.groupBy { it.name }

        for ((type, devicesOfType) in typeGroups) {
            // Calculate total energy for devices that are ON in this group based on their daily goal
            // For dashboard cards, this is an estimate of what they would consume if ON for their daily goal.
            val totalEnergy = devicesOfType
                .filter { it.isOn }
                .sumOf { (it.wattUsage / 1000) * it.dailyHoursGoal }

            groupedDevices.add(DeviceGroup(type, devicesOfType, totalEnergy * 7)) // Multiply by 7 for weekly estimate
        }
        return groupedDevices
    }

    private fun setupWeeklyEnergyChart() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)

        if (currentUserId == null) {
            Toast.makeText(this, "Cannot load chart: User not identified.", Toast.LENGTH_SHORT).show()
            weeklyEnergyChart.clear()
            weeklyEnergyChart.invalidate()
            return
        }

        // --- FETCH REAL DATA FOR THE GRAPH ---
        val weeklyEnergyData = dbHelper.getWeeklyEnergyConsumption(currentUserId)

        // Get the dates and KWh values in the correct order for the X-axis
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault()) // For "MON", "TUE" labels

        val displayDates = mutableListOf<String>()
        val dailyKwhValues = mutableListOf<Float>()

        // Iterate from 6 days ago up to today to ensure correct ordering for the chart
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i) // Go back i days
            val dateStr = sdf.format(calendar.time) // "yyyy-MM-dd"
            val dayLabel = dayOfWeekFormat.format(calendar.time) // "MON", "TUE"

            displayDates.add(dayLabel)
            dailyKwhValues.add((weeklyEnergyData[dateStr] ?: 0.0).toFloat()) // Get data, default to 0.0 if no entry
        }

        val entries = ArrayList<Entry>()
        dailyKwhValues.forEachIndexed { index, kwh ->
            entries.add(Entry(index.toFloat(), kwh))
        }

        val dataSet = LineDataSet(entries, "Weekly Energy (kWh)")
        dataSet.color = "#A5C0C8".toColorInt()
        dataSet.valueTextColor = android.graphics.Color.DKGRAY
        dataSet.setCircleColor("#6E8B94".toColorInt())
        dataSet.circleRadius = 4f
        dataSet.lineWidth = 2.5f
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = "#D0E0E3".toColorInt()
        dataSet.fillAlpha = 100


        val lineData = LineData(dataSet)
        weeklyEnergyChart.data = lineData

        val xAxis = weeklyEnergyChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(displayDates)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.textColor = android.graphics.Color.DKGRAY


        val yAxisLeft = weeklyEnergyChart.axisLeft
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.textColor = android.graphics.Color.DKGRAY
        yAxisLeft.axisMinimum = 0f


        weeklyEnergyChart.axisRight.isEnabled = false
        weeklyEnergyChart.description.isEnabled = false
        weeklyEnergyChart.legend.isEnabled = false

        weeklyEnergyChart.animateX(1000)
        weeklyEnergyChart.invalidate()
    }
}