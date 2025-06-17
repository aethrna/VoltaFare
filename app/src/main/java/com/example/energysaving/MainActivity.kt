package com.example.energysaving

import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.os.Bundle
import android.view.View // Import View
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
import com.google.android.material.floatingactionbutton.FloatingActionButton // Import FAB

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DevDBHelper
    private lateinit var userDbHelper: DBHelper
    private lateinit var greetingText: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var weeklyEnergyChart: LineChart

    // New properties for navigation items
    private lateinit var navItemAchievements: LinearLayout
    private lateinit var navItemRecommendations: LinearLayout
    private lateinit var navItemHome: LinearLayout
    private lateinit var navItemProfile: LinearLayout
    private lateinit var fabAddDevice: FloatingActionButton

    private lateinit var indicatorAchievements: View
    private lateinit var indicatorRecommendations: View
    private lateinit var indicatorHome: View
    private lateinit var indicatorProfile: View

    private var currentSelectedIndicator: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DevDBHelper(this)
        userDbHelper = DBHelper(this)

        greetingText = findViewById(R.id.tvGreeting)
        imgProfile = findViewById(R.id.imgProfile)

        // Initialize new navigation items
        navItemAchievements = findViewById(R.id.navItemAchievements)
        navItemRecommendations = findViewById(R.id.navItemRecommendations)
        navItemHome = findViewById(R.id.navItemHome)
        navItemProfile = findViewById(R.id.navItemProfile)
        fabAddDevice = findViewById(R.id.fabAddDevice)

        indicatorAchievements = findViewById(R.id.indicatorAchievements)
        indicatorRecommendations = findViewById(R.id.indicatorRecommendations)
        indicatorHome = findViewById(R.id.indicatorHome)
        indicatorProfile = findViewById(R.id.indicatorProfile)

        // Get user profile data and update greeting
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userEmail = prefs.getString("email", "User")
        val currentUserId = prefs.getString("currentUserId", null)

        var displayName = userEmail
        if (currentUserId != null) {
            val userProfile = userDbHelper.getUserProfile(currentUserId)
            displayName = userProfile[DBHelper.COLUMN_DISPLAY_NAME] ?: userEmail
        }
        greetingText.text = "Hi there, $displayName"

        // Set current date
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        val currentDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date())
        dateTextView.text = currentDate

        // Profile image click listener
        imgProfile.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        // Set initial selected state for navigation (e.g., Home)
        selectNavItem(indicatorHome)

        // Set OnClickListeners for new navigation items
        navItemAchievements.setOnClickListener {
            selectNavItem(indicatorAchievements)
            startActivity(Intent(this, AchievementsActivity::class.java))
        }
        navItemRecommendations.setOnClickListener {
            selectNavItem(indicatorRecommendations)
            startActivity(Intent(this, RecommendationsActivity::class.java))
        }
        navItemHome.setOnClickListener {
            selectNavItem(indicatorHome)
            Toast.makeText(this, "You're already on Home", Toast.LENGTH_SHORT).show()
        }
        navItemProfile.setOnClickListener {
            selectNavItem(indicatorProfile)
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        fabAddDevice.setOnClickListener {
            startActivity(Intent(this, DeviceTypeActivity::class.java))
        }

        // Setup RecyclerView for devices
        setupRecyclerView()

        // Setup Weekly Energy Chart
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

        // Re-select the correct tab if returning from another activity
        // This is a simple re-selection. For persistent selection across app kills,
        // you'd save the state in SharedPreferences.
        // For basic navigation, selecting "Home" on resume is fine.
        selectNavItem(indicatorHome)
    }

    // Function to handle selection state of navigation items
    private fun selectNavItem(selectedIndicator: View) {
        // Reset all indicators to transparent
        indicatorAchievements.setBackgroundColor(resources.getColor(android.R.color.transparent, theme))
        indicatorRecommendations.setBackgroundColor(resources.getColor(android.R.color.transparent, theme))
        indicatorHome.setBackgroundColor(resources.getColor(android.R.color.transparent, theme))
        indicatorProfile.setBackgroundColor(resources.getColor(android.R.color.transparent, theme))

        // Set the selected indicator's color
        selectedIndicator.setBackgroundColor(resources.getColor(R.color.blue_indicator, theme))
        currentSelectedIndicator = selectedIndicator
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

        val weeklyEnergyData = dbHelper.getWeeklyEnergyConsumption(currentUserId)

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val displayDates = mutableListOf<String>()
        val dailyKwhValues = mutableListOf<Float>()

        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(calendar.time)
            val dayLabel = dayOfWeekFormat.format(calendar.time)

            displayDates.add(dayLabel)
            dailyKwhValues.add((weeklyEnergyData[dateStr] ?: 0.0).toFloat())
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