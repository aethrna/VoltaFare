// File: app/src/main/java/com/example/energysaving/DeviceListDetailActivity.kt
package com.example.energysaving

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar // Import Calendar

class DeviceListDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DevDBHelper
    private lateinit var individualDeviceAdapter: IndividualDeviceAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvDeviceListHeader: TextView
    private var deviceType: String? = null
    private var deviceList: MutableList<Device> = mutableListOf()

    companion object {
        const val EXTRA_DEVICE_TYPE = "extra_device_type"
        private const val ALERT_NOTIFICATION_CHANNEL_ID = "DEVICE_GOAL_ALERTS_CHANNEL"
        private const val TAG = "DeviceListDetail"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_list_activity)

        dbHelper = DevDBHelper(this)
        deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE)

        tvDeviceListHeader = findViewById(R.id.tvDeviceListHeader)
        recyclerView = findViewById(R.id.recyclerViewDeviceList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        createNotificationChannel()

        if (deviceType == null) {
            Toast.makeText(this, "Device type not specified.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        tvDeviceListHeader.text = "$deviceType"
        setupAdapter()
    }

    override fun onResume() {
        super.onResume()
        loadDevices()
        // New: Trigger daily aggregation when the activity resumes
        aggregateAndLogDailyEnergy()
    }

    private fun setupAdapter() {
        individualDeviceAdapter = IndividualDeviceAdapter(deviceList) { deviceFromAdapter, newIsOnState ->
            val deviceInList = deviceList.find { it.id == deviceFromAdapter.id }
            if (deviceInList != null) {
                handleDeviceStateChange(deviceInList, newIsOnState)
            } else {
                Log.e(TAG, "Device not found in local list for ID: ${deviceFromAdapter.id}")
            }
        }
        recyclerView.adapter = individualDeviceAdapter
    }

    private fun loadDevices() {
        deviceType?.let { type ->
            val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val currentUserId = prefs.getString("currentUserId", null)
            if (currentUserId != null) {
                val fetchedDevices = dbHelper.getDevicesByTypeForUser(type, currentUserId)
                deviceList.clear()
                deviceList.addAll(fetchedDevices)
                deviceList.forEach { checkAndResetDailyUsage(it, true) }
                individualDeviceAdapter.updateDevices(deviceList)
            } else {
                Toast.makeText(this, "User not identified.", Toast.LENGTH_LONG).show()
                deviceList.clear()
                individualDeviceAdapter.updateDevices(deviceList)
            }
        }
    }

    private fun localGetCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun checkAndResetDailyUsage(device: Device, persistChanges: Boolean = false): Boolean {
        val currentDateStr = localGetCurrentDateString()
        var resetOccurred = false
        if (device.lastResetDate != currentDateStr) {
            Log.d(TAG, "Resetting daily usage for device ID: ${device.id}. Old date: ${device.lastResetDate}, New date: $currentDateStr")

            // If device was ON when day changed, its `timeUsedTodaySeconds` would hold usage up to that point of `lastResetDate`.
            // When loaded on a new day, we reset its `timeUsedTodaySeconds` for the new day's tracking.
            if (device.isOn && device.lastTurnOnTimestampMillis > 0) {
                // For simplified reset, the ongoing session's time will start fresh for the new day.
                device.lastTurnOnTimestampMillis = System.currentTimeMillis()
            }
            device.timeUsedTodaySeconds = 0L
            device.goalExceededAlertSentToday = false
            device.lastResetDate = currentDateStr
            resetOccurred = true
            if (persistChanges) {
                dbHelper.updateDeviceTrackingData(device)
            }
        }
        return resetOccurred
    }

    private fun handleDeviceStateChange(device: Device, newIsOnState: Boolean) {
        Log.d(TAG, "Handling state change for ${device.id}. New state: $newIsOnState. Current timeUsed: ${device.timeUsedTodaySeconds}s")
        checkAndResetDailyUsage(device)

        val oldIsOnState = device.isOn
        device.isOn = newIsOnState

        if (newIsOnState) {
            if (!oldIsOnState) {
                device.lastTurnOnTimestampMillis = System.currentTimeMillis()
                Log.d(TAG, "Device ${device.id} turned ON. Timestamp: ${device.lastTurnOnTimestampMillis}")
            }
        } else {
            if (oldIsOnState && device.lastTurnOnTimestampMillis > 0) {
                val durationMillis = System.currentTimeMillis() - device.lastTurnOnTimestampMillis
                if (durationMillis > 0) {
                    val durationSeconds = durationMillis / 1000
                    device.timeUsedTodaySeconds += durationSeconds
                    Log.d(TAG, "Device ${device.id} turned OFF. Duration: ${durationSeconds}s. Total today: ${device.timeUsedTodaySeconds}s")
                }
            }
        }

        dbHelper.updateDeviceTrackingData(device)

        val itemIndex = deviceList.indexOfFirst { it.id == device.id }
        if (itemIndex != -1) {
            individualDeviceAdapter.notifyItemChanged(itemIndex)
        }

        if (!newIsOnState) { // Check for alerts and aggregate only when turning OFF
            checkGoalAndAlert(device)
            aggregateAndLogDailyEnergy() // Log daily energy when a device is turned off
        }
    }

    private fun checkGoalAndAlert(device: Device) {
        val usedHoursToday = device.timeUsedTodaySeconds / 3600.0
        Log.d(TAG, "Checking goal for ${device.id}. Used: $usedHoursToday H, Goal: ${device.dailyHoursGoal} H, AlertSent: ${device.goalExceededAlertSentToday}")
        if (usedHoursToday > device.dailyHoursGoal && !device.goalExceededAlertSentToday) {
            val deviceDisplayName = device.description.ifBlank { device.name }
            sendUsageAlertNotification(deviceDisplayName, usedHoursToday, device.dailyHoursGoal, device.id)
            device.goalExceededAlertSentToday = true
            dbHelper.updateDeviceTrackingData(device)
        }
    }

    private fun sendUsageAlertNotification(deviceName: String, usedHours: Double, goalHours: Double, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(this)

        val contentText = String.format(Locale.US, "Used for %.1f hours today, exceeding your goal of %.1f hours.", usedHours, goalHours)

        val notification = NotificationCompat.Builder(this, ALERT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("VoltaFare Usage Alert: $deviceName")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(notificationId, notification)
            Log.i(TAG, "Notification sent for device ID: $notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission might be missing.", e)
            Toast.makeText(this, "Notification permission needed to show alerts.", Toast.LENGTH_LONG).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name) + " Usage Alerts"
            val descriptionText = "Channel for device usage goal alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(ALERT_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created.")
        }
    }

    // New: Function to aggregate and log daily energy consumption
    private fun aggregateAndLogDailyEnergy() {
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        val currentUserId = prefs.getString("currentUserId", null) ?: return

        val yesterdayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)) // Yesterday's date
        )
        val todayDateStr = localGetCurrentDateString()

        val allDevices = dbHelper.getAllDevicesForUser(currentUserId)

        var totalKwhYesterday = 0.0
        var totalKwhToday = 0.0

        for (device in allDevices) {
            // Aggregate usage for yesterday
            if (device.lastResetDate == yesterdayDateStr && device.timeUsedTodaySeconds > 0) {
                val hoursUsedYesterday = device.timeUsedTodaySeconds / 3600.0
                totalKwhYesterday += (device.wattUsage / 1000.0) * hoursUsedYesterday
            }

            // Aggregate usage for today (including current active session)
            if (device.lastResetDate == todayDateStr) {
                var currentSessionSeconds = 0L
                if (device.isOn && device.lastTurnOnTimestampMillis > 0) {
                    currentSessionSeconds = (System.currentTimeMillis() - device.lastTurnOnTimestampMillis) / 1000
                }
                val hoursUsedToday = (device.timeUsedTodaySeconds + currentSessionSeconds) / 3600.0
                totalKwhToday += (device.wattUsage / 1000.0) * hoursUsedToday
            }
        }

        // Log yesterday's total (if it's not already logged for that date)
        // Only add or update if totalKwhYesterday is meaningful or if it's not yet recorded for yesterday.
        dbHelper.addOrUpdateDailyEnergy(currentUserId, yesterdayDateStr, totalKwhYesterday)

        // Always update today's total
        dbHelper.addOrUpdateDailyEnergy(currentUserId, todayDateStr, totalKwhToday)

        Log.d(TAG, "Aggregated Daily Energy: Today: ${totalKwhToday} kWh, Yesterday: ${totalKwhYesterday} kWh")
    }
}