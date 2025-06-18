package com.example.energysaving

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.*

class DeviceListDetailActivity : BaseActivity() {

    override val activeIndicator: Int
        get() = 0

    // Class Properties
    private lateinit var dbHelper: DevDBHelper
    private lateinit var userDbHelper: DBHelper
    private lateinit var individualDeviceAdapter: IndividualDeviceAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvDeviceListHeader: TextView
    private lateinit var profileImage: CircleImageView
    private var deviceType: String? = null
    private var deviceList: MutableList<Device> = mutableListOf()
    private lateinit var currentUserId: String

    companion object {
        const val EXTRA_DEVICE_TYPE = "extra_device_type"
        private const val ALERT_NOTIFICATION_CHANNEL_ID = "DEVICE_GOAL_ALERTS_CHANNEL"
        private const val TAG = "DeviceListDetail"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_list_activity)

        dbHelper = DevDBHelper(this)
        userDbHelper = DBHelper(this)
        deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE)

        initViews()
        createNotificationChannel()

        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("currentUserId", null) ?: run {
            Toast.makeText(this, "User not identified.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        tvDeviceListHeader.text = deviceType ?: "Device Details"
        loadProfileImage()
        setupAdapter()
    }

    override fun onResume() {
        super.onResume()
        loadDevices()
        aggregateAndLogDailyEnergy()
    }

    private fun initViews() {
        tvDeviceListHeader = findViewById(R.id.tvDeviceListHeader)
        recyclerView = findViewById(R.id.recyclerViewDeviceList)
        profileImage = findViewById(R.id.profileImage)
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadProfileImage() {
        val userProfile = userDbHelper.getUserProfile(currentUserId)
        val imageUriString = userProfile[DBHelper.COLUMN_PROFILE_IMAGE_URI]
        if (!imageUriString.isNullOrBlank()) {
            try {
                profileImage.setImageURI(Uri.parse(imageUriString))
            } catch (e: Exception) {
                profileImage.setImageResource(R.mipmap.ic_launcher_round)
            }
        } else {
            profileImage.setImageResource(R.mipmap.ic_launcher_round)
        }
    }

    private fun setupAdapter() {
        individualDeviceAdapter = IndividualDeviceAdapter(
            devices = deviceList,
            onDeviceStateChanged = { device ->
                handleDeviceStateChange(device)
            },
            onDeviceDelete = { deviceToDelete ->
                deleteDevice(deviceToDelete)
            }
        )
        recyclerView.adapter = individualDeviceAdapter
    }

    private fun handleDeviceStateChange(device: Device) {
        val newIsOnState = !device.isOn
        Log.d(TAG, "Toggling device ${device.id}. New state: $newIsOnState")
        checkAndResetDailyUsage(device)
        device.isOn = newIsOnState

        if (newIsOnState) {
            device.lastTurnOnTimestampMillis = System.currentTimeMillis()
        } else {
            if (device.lastTurnOnTimestampMillis > 0) {
                val durationMillis = System.currentTimeMillis() - device.lastTurnOnTimestampMillis
                if (durationMillis > 0) {
                    device.timeUsedTodaySeconds += durationMillis / 1000
                }
            }
            checkGoalAndAlert(device)
            aggregateAndLogDailyEnergy()
        }

        dbHelper.updateDeviceTrackingData(device)
        val itemIndex = deviceList.indexOfFirst { it.id == device.id }
        if (itemIndex != -1) {
            individualDeviceAdapter.notifyItemChanged(itemIndex)
        }
    }

    private fun loadDevices() {
        deviceType?.let { type ->
            val fetchedDevices = dbHelper.getDevicesByTypeForUser(type, currentUserId)
            deviceList.clear()
            deviceList.addAll(fetchedDevices)
            // It's important to reset usage *before* updating the adapter
            deviceList.forEach { checkAndResetDailyUsage(it) }
            individualDeviceAdapter.updateDevices(deviceList)

            if (deviceList.isEmpty()) {
                Toast.makeText(this, "No more $type devices.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun deleteDevice(device: Device) {
        val deletedRows = dbHelper.deleteDevice(device.id, currentUserId)
        if (deletedRows > 0) {
            Toast.makeText(this, "${device.description} deleted.", Toast.LENGTH_SHORT).show()
            loadDevices()
        } else {
            Toast.makeText(this, "Failed to delete ${device.description}.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- THIS IS THE CORRECTED FUNCTION ---
    private fun checkAndResetDailyUsage(device: Device): Boolean {
        val currentDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var resetOccurred = false
        if (device.lastResetDate != currentDateStr) {
            Log.d(TAG, "Resetting daily usage for device ID: ${device.id}. Old date: ${device.lastResetDate}, New date: $currentDateStr")
            if (device.isOn) {
                device.lastTurnOnTimestampMillis = System.currentTimeMillis()
            }
            device.timeUsedTodaySeconds = 0L
            device.goalExceededAlertSentToday = false
            device.lastResetDate = currentDateStr
            resetOccurred = true
            // Always persist the changes if a reset happened
            dbHelper.updateDeviceTrackingData(device)
        }
        // This return statement was missing
        return resetOccurred
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
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
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission might be missing.", e)
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
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun aggregateAndLogDailyEnergy() {
        val yesterdayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val allDevices = dbHelper.getAllDevicesForUser(currentUserId)
        var totalKwhYesterday = 0.0
        var totalKwhToday = 0.0
        for (device in allDevices) {
            if (device.lastResetDate == yesterdayDateStr && device.timeUsedTodaySeconds > 0) {
                val hoursUsedYesterday = device.timeUsedTodaySeconds / 3600.0
                totalKwhYesterday += (device.wattUsage / 1000.0) * hoursUsedYesterday
            }
            if (device.lastResetDate == todayDateStr) {
                var currentSessionSeconds = 0L
                if (device.isOn && device.lastTurnOnTimestampMillis > 0) {
                    currentSessionSeconds = (System.currentTimeMillis() - device.lastTurnOnTimestampMillis) / 1000
                }
                val hoursUsedToday = (device.timeUsedTodaySeconds + currentSessionSeconds) / 3600.0
                totalKwhToday += (device.wattUsage / 1000.0) * hoursUsedToday
            }
        }
        dbHelper.addOrUpdateDailyEnergy(currentUserId, yesterdayDateStr, totalKwhYesterday)
        dbHelper.addOrUpdateDailyEnergy(currentUserId, todayDateStr, totalKwhToday)
        Log.d(TAG, "Aggregated Daily Energy: Today: ${totalKwhToday} kWh, Yesterday: ${totalKwhYesterday} kWh")
    }
}