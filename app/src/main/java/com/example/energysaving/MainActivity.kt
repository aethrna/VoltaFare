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
    private lateinit var greetingText: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var weeklyEnergyChart: LineChart


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DevDBHelper(this)

        greetingText = findViewById(R.id.tvGreeting)
        imgProfile = findViewById(R.id.imgProfile)

        // Set greeting dynamically (optional)
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val username = prefs.getString("username", "User")
        greetingText.text = "Hi there, $username"
        val dateTextView = findViewById<TextView>(R.id.dateTextView)

        val currentDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date())
        dateTextView.text = currentDate

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
        weeklyEnergyChart = findViewById(R.id.weeklyEnergyChart)
        setupWeeklyEnergyChart()
    }

    override fun onResume() {
        super.onResume()
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewDevices)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null) // Get stored userId, default to -1 if not found

        var devicesToDisplay: List<Device> = emptyList() // Default to empty list

        if (currentUserId != null) {
            // User ID is valid, fetch their devices
            devicesToDisplay = dbHelper.getAllDevicesForUser(currentUserId) // Use new method
        } else {
            // Handle case where userId is not available (e.g., error, or user not properly logged in)
            Toast.makeText(this, "Error: User not identified. Cannot load devices.", Toast.LENGTH_LONG).show()
            // You might want to navigate to login or show an error state
        }

        val groupedDevices = groupDevices(devicesToDisplay)
        val adapter = DeviceCardAdapter(groupedDevices) // Assuming DeviceCardAdapter is ready
        recyclerView.adapter = adapter
    }

    // In MainActivity.kt
    private fun groupDevices(devices: List<Device>): List<DeviceGroup> {
        val groupedDevices = mutableListOf<DeviceGroup>()
        // Group by name, or by description if name is "Custom"
        val typeGroups = devices.groupBy {
            if (it.name.equals("Custom", ignoreCase = true) && it.description.isNotBlank()) {
                it.description // Use description as type for "Custom" devices
            } else {
                it.name // Use name as type for other devices
            }
        }

        for ((type, devicesOfType) in typeGroups) {
            // Only sum energy for devices that are ON
            val totalEnergy = devicesOfType
                .filter { it.isOn }
                .sumOf { (it.wattUsage / 1000) * it.dailyHours }
            groupedDevices.add(DeviceGroup(type, devicesOfType, totalEnergy))
        }
        return groupedDevices
    }

    private fun setupWeeklyEnergyChart() {
        // --- DATA PREPARATION FOR THE GRAPH ---
        // This is SIMULATED data. In a real app, you'd fetch or calculate this.
        val dailyKwhValues = listOf(
            30f, // Monday
            40f, // Tuesday
            28f, // Wednesday
            35f, // Thursday
            42f, // Friday
            55f, // Saturday
            48f  // Sunday
        )

        // If you wanted to use your current device data (FLAT LINE):
        // val allDevices = dbHelper.getAllDevices()
        // val totalDailyKwh = allDevices.sumOf { (it.wattUsage / 1000) * it.dailyHours }.toFloat()
        // val dailyKwhValues = List(7) { totalDailyKwh } // Same value for all 7 days


        val entries = ArrayList<Entry>()
        dailyKwhValues.forEachIndexed { index, kwh ->
            entries.add(Entry(index.toFloat(), kwh))
        }

        val dataSet = LineDataSet(entries, "Weekly Energy (kWh)")
        // --- STYLING (Refer to previous detailed guide or MPAndroidChart docs) ---
        // Example styling (customize as needed to match your UI image)
        dataSet.color = "#A5C0C8".toColorInt() // A light blue similar to image
        dataSet.valueTextColor = android.graphics.Color.DKGRAY
        dataSet.setCircleColor("#6E8B94".toColorInt()) // Darker circle
        dataSet.circleRadius = 4f
        dataSet.lineWidth = 2.5f
        dataSet.setDrawValues(false) // Don't draw kWh values on each point on the line
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Makes the line smoother like your image
        dataSet.setDrawFilled(true) // To fill the area under the line
        dataSet.fillColor = "#D0E0E3".toColorInt() // Light fill color
        dataSet.fillAlpha = 100


        val lineData = LineData(dataSet)
        weeklyEnergyChart.data = lineData

        // --- X-AXIS CONFIGURATION ---
        val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        val xAxis = weeklyEnergyChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(days)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false) // No vertical grid lines
        xAxis.textColor = android.graphics.Color.DKGRAY


        // --- Y-AXIS CONFIGURATION (LEFT) ---
        val yAxisLeft = weeklyEnergyChart.axisLeft
        yAxisLeft.setDrawGridLines(true) // Horizontal grid lines
        yAxisLeft.textColor = android.graphics.Color.DKGRAY
        yAxisLeft.axisMinimum = 0f // Or slightly below your data's min
        // yAxisLeft.axisMaximum = 65f; // Set a max if needed, or let it auto-calculate


        // --- OTHER CHART CONFIGURATIONS ---
        weeklyEnergyChart.axisRight.isEnabled = false // Disable right Y-axis
        weeklyEnergyChart.description.isEnabled = false // No description label
        weeklyEnergyChart.legend.isEnabled = false // Hide legend if only one dataset

        weeklyEnergyChart.animateX(1000) // Optional: Animate chart drawing
        weeklyEnergyChart.invalidate() // Refresh the chart
    }
}
