package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : BaseActivity() {

    override val activeIndicator: Int
        get() = R.id.navItemHome

    private lateinit var dbHelper: DevDBHelper
    private lateinit var userDbHelper: DBHelper
    private lateinit var greetingText: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var weeklyEnergyChart: LineChart
    private lateinit var recyclerViewDevices: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dbHelper = DevDBHelper(this)
        userDbHelper = DBHelper(this)
        greetingText = findViewById(R.id.tvGreeting)
        imgProfile = findViewById(R.id.imgProfile)
        weeklyEnergyChart = findViewById(R.id.weeklyEnergyChart)
        recyclerViewDevices = findViewById(R.id.recyclerViewDevices)

        imgProfile.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        updateUserProfile()
        setupRecyclerViewLayout()
        updateRecyclerViewData()
        setupWeeklyEnergyChart()
    }

    override fun onResume() {
        super.onResume()
        updateUserProfile()
        updateRecyclerViewData()
        setupWeeklyEnergyChart()
    }

    private fun setupRecyclerViewLayout() {
        recyclerViewDevices.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        if (recyclerViewDevices.onFlingListener == null) {
            val snapHelper = PagerSnapHelper()
            snapHelper.attachToRecyclerView(recyclerViewDevices)
        }
    }

    private fun updateRecyclerViewData() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)

        val devicesToDisplay: List<Device> = if (currentUserId != null) {
            dbHelper.getAllDevicesForUser(currentUserId)
        } else {
            Toast.makeText(this, "Error: User not identified.", Toast.LENGTH_LONG).show()
            emptyList()
        }
        val groupedDevices = groupDevices(devicesToDisplay)
        val adapter = DeviceCardAdapter(groupedDevices)
        recyclerViewDevices.adapter = adapter
    }

    private fun updateUserProfile() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val userEmail = prefs.getString("email", "User")
        val currentUserId = prefs.getString("currentUserId", null)
        var displayName = userEmail
        if (currentUserId != null) {
            val userProfile = userDbHelper.getUserProfile(currentUserId)
            displayName = userProfile[DBHelper.COLUMN_DISPLAY_NAME] ?: userEmail
        }
        greetingText.text = "Hi there, $displayName"
        val dateTextView = findViewById<TextView>(R.id.dateTextView)
        val currentDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date())
        dateTextView.text = currentDate
    }

    private fun groupDevices(devices: List<Device>): List<DeviceGroup> {
        val groupedDevices = mutableListOf<DeviceGroup>()
        val typeGroups = devices.groupBy { it.name }
        for ((type, devicesOfType) in typeGroups) {
            val totalEnergy = devicesOfType
                .filter { it.isOn }
                .sumOf { (it.wattUsage / 1000) * it.dailyHoursGoal }
            groupedDevices.add(DeviceGroup(type, devicesOfType, totalEnergy * 7))
        }
        return groupedDevices
    }

    private fun setupWeeklyEnergyChart() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null)

        if (currentUserId == null) {
            weeklyEnergyChart.clear()
            weeklyEnergyChart.invalidate()
            return
        }

        val weeklyEnergyData = dbHelper.getWeeklyEnergyConsumption(currentUserId)
        val displayDates = mutableListOf<String>()
        val entries = ArrayList<Entry>()

        for (i in 6 downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time)
                .uppercase(Locale.getDefault())

            displayDates.add(dayLabel)
            val kwhValue = (weeklyEnergyData[dateStr] ?: 0.0).toFloat()
            entries.add(Entry(entries.size.toFloat(), kwhValue))
        }

        val dataSet = LineDataSet(entries, "Weekly Energy")
        dataSet.color = "#163D6A".toColorInt()
        dataSet.lineWidth = 3f
        dataSet.mode = LineDataSet.Mode.LINEAR
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.setDrawFilled(false)

        weeklyEnergyChart.data = LineData(dataSet)

        val xAxis = weeklyEnergyChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(displayDates)
        xAxis.textColor = "#163D6A".toColorInt()
        xAxis.axisLineColor = "#163D6A".toColorInt()
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(true)

        val yAxisLeft = weeklyEnergyChart.axisLeft
        yAxisLeft.textColor = "#163D6A".toColorInt()
        yAxisLeft.setDrawGridLines(false)
        yAxisLeft.setDrawAxisLine(false)
        yAxisLeft.axisMinimum = 0f

        weeklyEnergyChart.axisRight.isEnabled = false
        weeklyEnergyChart.legend.isEnabled = false
        weeklyEnergyChart.description.isEnabled = false
        weeklyEnergyChart.setTouchEnabled(false)
        weeklyEnergyChart.setDrawGridBackground(false)

        weeklyEnergyChart.invalidate()
    }
}