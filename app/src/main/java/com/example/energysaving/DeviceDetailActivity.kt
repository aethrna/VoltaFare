package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import android.util.Log // Keep for now, or remove if no longer needed

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DevDBHelper
    private var selectedDeviceTypeCategory: String? = null // Make it a class member variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_detail_activity)

        dbHelper = DevDBHelper(this)

        // Assign the value from the Intent to the member variable
        selectedDeviceTypeCategory = intent.getStringExtra("device_type")
        Log.d("DeviceDetailDebug", "selectedDeviceTypeCategory (onCreate): $selectedDeviceTypeCategory") // Keep this log for initial check

        val etSpecificDeviceName = findViewById<EditText>(R.id.etDeviceName)
        val etAdditionalDescription = findViewById<EditText>(R.id.etDeviceDescription)
        val etWatt = findViewById<EditText>(R.id.etWattUsage)
        val etDailyHours = findViewById<EditText>(R.id.etDailyHours)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitDevice)

        btnSubmit.setOnClickListener {
            val specificDeviceName = etSpecificDeviceName.text.toString().trim()
            val additionalDescription = etAdditionalDescription.text.toString().trim()
            val watt = etWatt.text.toString().toDoubleOrNull()
            val hoursGoal = etDailyHours.text.toString().toDoubleOrNull()


            if (selectedDeviceTypeCategory.isNullOrBlank() || specificDeviceName.isBlank() || watt == null || hoursGoal == null) {
                Toast.makeText(this, "Device Type, Specific Name, Wattage, and Hours must be filled correctly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val currentUserId = prefs.getString("currentUserId", null)

            if (currentUserId == null) {
                Toast.makeText(this, "Error: User session not found. Please log in again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val success = dbHelper.insertDevice(
                selectedDeviceTypeCategory!!, // Use '!!' because validation ensures it's not null/blank
                specificDeviceName,
                watt,
                hoursGoal,
                true,
                currentUserId
            )

            if (success) {
                Toast.makeText(this, "Device added", Toast.LENGTH_SHORT).show()
                finish() // Go back to the previous activity (DeviceTypeActivity)
            } else {
                Toast.makeText(this, "Failed to add device", Toast.LENGTH_SHORT).show()
            }
        }
    }
}