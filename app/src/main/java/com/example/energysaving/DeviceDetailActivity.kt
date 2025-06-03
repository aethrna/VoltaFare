package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DevDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_detail_activity)

        dbHelper = DevDBHelper(this)

        // The type selected in DeviceTypeActivity (e.g., "Lights", "Fan")
        val selectedDeviceType = intent.getStringExtra("device_type")

        val etSpecificDeviceName = findViewById<EditText>(R.id.etDeviceName) // This is where you type "Garden Lights"
        val etAdditionalDescription = findViewById<EditText>(R.id.etDeviceDescription) // This field is now for extra notes or can be removed if not needed for specific description
        val etWatt = findViewById<EditText>(R.id.etWattUsage)
        val etHours = findViewById<EditText>(R.id.etDailyHours)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitDevice)

        // Optionally, if you want to pre-fill the specific device name field with the type for "Custom" type
        // or ensure the user sees the type they selected:
        if (selectedDeviceType != null && selectedDeviceType != "Custom") {
            // You could set a hint here or disable etSpecificDeviceName if it should strictly be a description
            // For now, it's user input for specific name.
            // If you want to clarify the UI, consider changing hints or adding a TextView for the selected type.
        }


        btnSubmit.setOnClickListener {
            // This will be "Garden Lights" (the specific name)
            val specificDeviceName = etSpecificDeviceName.text.toString().trim()
            // This will be "Lights" (the category/type)
            // It's crucial to get this from the intent, not from user input fields in this activity,
            // unless you have a "Custom" type flow that handles it differently.
            val deviceCategoryType = selectedDeviceType?.trim() ?: ""

            // This field can be used for truly additional notes beyond the specific name
            val additionalDescription = etAdditionalDescription.text.toString().trim()


            val watt = etWatt.text.toString().toDoubleOrNull()
            val hoursGoal = etHours.text.toString().toDoubleOrNull()

            // Validate that the category type is available and essential fields are filled
            if (deviceCategoryType.isBlank() || specificDeviceName.isBlank() || watt == null || hoursGoal == null) {
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
                // Pass the category/type to the 'name' parameter in the Device object
                deviceCategoryType,
                // Pass the specific user-inputted name to the 'description' parameter in the Device object
                specificDeviceName,
                watt,
                hoursGoal,
                true,
                currentUserId
            )

            if (success) {
                Toast.makeText(this, "Device added", Toast.LENGTH_SHORT).show()
                finish() // Go back to the previous activity
            } else {
                Toast.makeText(this, "Failed to add device", Toast.LENGTH_SHORT).show()
            }
        }
    }
}