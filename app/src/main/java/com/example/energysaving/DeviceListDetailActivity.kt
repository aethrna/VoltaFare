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
import androidx.core.app.NotificationManagerCompat // Use Compat version
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat // For getCurrentDateString if not using the one from Device.kt
import java.util.Date            // For getCurrentDateString
import java.util.Locale          // For getCurrentDateString & formatting

class DeviceListDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DevDBHelper //
    private lateinit var individualDeviceAdapter: IndividualDeviceAdapter //
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvDeviceListHeader: TextView
    private var deviceType: String? = null
    private var deviceList: MutableList<Device> = mutableListOf() // Keep a mutable list for easier updates

    companion object {
        const val EXTRA_DEVICE_TYPE = "extra_device_type"
        private const val ALERT_NOTIFICATION_CHANNEL_ID = "DEVICE_GOAL_ALERTS_CHANNEL"
        private const val TAG = "DeviceListDetail"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_list_activity) // Assumes this layout exists

        dbHelper = DevDBHelper(this)
        deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE)

        tvDeviceListHeader = findViewById(R.id.tvDeviceListHeader)
        recyclerView = findViewById(R.id.recyclerViewDeviceList)
        recyclerView.layoutManager = LinearLayoutManager(this)

        createNotificationChannel() // Create notification channel on activity creation

        if (deviceType == null) {
            Toast.makeText(this, "Device type not specified.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        tvDeviceListHeader.text = "$deviceType"
        setupAdapter()
        // loadDevices() // Will be called in onResume for freshness
    }

    override fun onResume() {
        super.onResume()
        loadDevices() // Load or refresh devices when activity resumes
    }

    private fun setupAdapter() {
        individualDeviceAdapter = IndividualDeviceAdapter(deviceList) { deviceFromAdapter, newIsOnState ->
            // Find the actual device object from our local list to ensure we're updating the same instance
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
                val fetchedDevices = dbHelper.getDevicesByTypeForUser(type, currentUserId) // Ensure this fetches all new fields
                deviceList.clear()
                deviceList.addAll(fetchedDevices)
                // Before updating adapter, check and reset daily usage for all loaded devices if needed
                deviceList.forEach { checkAndResetDailyUsage(it, true) } // Pass true to persist if reset happens
                individualDeviceAdapter.updateDevices(deviceList) // Update adapter with the fresh list
            } else {
                Toast.makeText(this, "User not identified.", Toast.LENGTH_LONG).show()
                deviceList.clear()
                individualDeviceAdapter.updateDevices(deviceList)
            }
        }
    }

    private fun localGetCurrentDateString(): String { // To avoid conflict if Device.kt also has one
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // This function modifies the device object directly
    private fun checkAndResetDailyUsage(device: Device, persistChanges: Boolean = false): Boolean {
        val currentDateStr = localGetCurrentDateString()
        var resetOccurred = false
        if (device.lastResetDate != currentDateStr) {
            Log.d(TAG, "Resetting daily usage for device ID: ${device.id}. Old date: ${device.lastResetDate}, New date: $currentDateStr")
            // If device was ON and it's a new day, the timeUsedTodaySeconds should capture usage until midnight.
            // For simplicity now, we just reset. More advanced logic would calculate partial day usage.
            if (device.isOn && device.lastTurnOnTimestampMillis > 0) {
                // Calculate usage from lastTurnOnTimestampMillis up to "end of yesterday"
                // This part can be complex. For now, let's assume a simple reset.
                // A more accurate way would be to calculate usage until midnight yesterday.
                // For now, if it was ON, lastTurnOnTimestampMillis is reset to start of today.
                device.lastTurnOnTimestampMillis = System.currentTimeMillis() // Effectively restart "ON" period for today
            }
            device.timeUsedTodaySeconds = 0L
            device.goalExceededAlertSentToday = false
            device.lastResetDate = currentDateStr
            resetOccurred = true
            if (persistChanges) {
                dbHelper.updateDeviceTrackingData(device) // Persist reset state
            }
        }
        return resetOccurred
    }

    private fun handleDeviceStateChange(device: Device, newIsOnState: Boolean) {
        Log.d(TAG, "Handling state change for ${device.id}. New state: $newIsOnState. Current timeUsed: ${device.timeUsedTodaySeconds}s")
        checkAndResetDailyUsage(device) // Ensure usage is for the current day

        val oldIsOnState = device.isOn
        device.isOn = newIsOnState

        if (newIsOnState) { // Turning ON
            if (!oldIsOnState) { // If it was previously OFF
                device.lastTurnOnTimestampMillis = System.currentTimeMillis()
                Log.d(TAG, "Device ${device.id} turned ON. Timestamp: ${device.lastTurnOnTimestampMillis}")
            }
        } else { // Turning OFF
            if (oldIsOnState && device.lastTurnOnTimestampMillis > 0) { // If it was previously ON
                val durationMillis = System.currentTimeMillis() - device.lastTurnOnTimestampMillis
                if (durationMillis > 0) {
                    val durationSeconds = durationMillis / 1000
                    device.timeUsedTodaySeconds += durationSeconds
                    Log.d(TAG, "Device ${device.id} turned OFF. Duration: ${durationSeconds}s. Total today: ${device.timeUsedTodaySeconds}s")
                }
                // device.lastTurnOnTimestampMillis = 0L // Reset timestamp once usage is logged
            }
        }

        dbHelper.updateDeviceTrackingData(device) // Save changes to DB

        // Notify adapter to re-bind the specific item for UI update (e.g. real-time kWh display)
        val itemIndex = deviceList.indexOfFirst { it.id == device.id }
        if (itemIndex != -1) {
            individualDeviceAdapter.notifyItemChanged(itemIndex)
        }

        if (!newIsOnState) { // Check for alerts only when turning OFF
            checkGoalAndAlert(device)
        }
    }

    private fun checkGoalAndAlert(device: Device) {
        val usedHoursToday = device.timeUsedTodaySeconds / 3600.0
        Log.d(TAG, "Checking goal for ${device.id}. Used: $usedHoursToday H, Goal: ${device.dailyHoursGoal} H, AlertSent: ${device.goalExceededAlertSentToday}")
        if (usedHoursToday > device.dailyHoursGoal && !device.goalExceededAlertSentToday) {
            val deviceDisplayName = device.description.ifBlank { device.name }
            sendUsageAlertNotification(deviceDisplayName, usedHoursToday, device.dailyHoursGoal, device.id)
            device.goalExceededAlertSentToday = true
            dbHelper.updateDeviceTrackingData(device) // Save that alert has been sent
        }
    }

    private fun sendUsageAlertNotification(deviceName: String, usedHours: Double, goalHours: Double, notificationId: Int) {
        val notificationManager = NotificationManagerCompat.from(this)

        val contentText = String.format(Locale.US, "Used for %.1f hours today, exceeding your goal of %.1f hours.", usedHours, goalHours)

        val notification = NotificationCompat.Builder(this, ALERT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Ensure you have this, or use a default icon
            .setContentTitle("VoltaFare Usage Alert: $deviceName")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Check for notification permission if targeting Android 13+ (API 33)
        // For simplicity, this example doesn't include the permission request boilerplate.
        try {
            notificationManager.notify(notificationId, notification) // Use a unique ID for each notification
            Log.i(TAG, "Notification sent for device ID: $notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission might be missing.", e)
            Toast.makeText(this, "Notification permission needed to show alerts.", Toast.LENGTH_LONG).show()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name) + " Usage Alerts" // Example channel name
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
}