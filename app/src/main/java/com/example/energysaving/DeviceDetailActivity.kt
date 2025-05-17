package com.example.energysaving

import android.content.Intent // For redirecting to login if needed
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit // For SharedPreferences KTX extensions (optional, but clean)

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: DevDBHelper // Corrected to DevDBHelper, assuming this is your device helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_detail_activity)

        // Ensure you are using DevDBHelper for device operations
        dbHelper = DevDBHelper(this)

        val etName = findViewById<EditText>(R.id.etDeviceName)
        val etDescription = findViewById<EditText>(R.id.etDeviceDescription)
        val etWatt = findViewById<EditText>(R.id.etWattUsage)
        val etHours = findViewById<EditText>(R.id.etDailyHours)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitDevice)

        // You might also want to get the 'selectedDeviceType' passed from DeviceTypeActivity
        // val selectedDeviceType = intent.getStringExtra("device_type")
        // And potentially pre-fill etName if selectedDeviceType is not "Custom", e.g.:
        // if (selectedDeviceType != null && !selectedDeviceType.equals("Custom", ignoreCase = true)) {
        //     etName.setText(selectedDeviceType)
        //     etName.isEnabled = false // Or just use selectedDeviceType as the 'name' parameter for insertDevice
        // and etName for 'description' in that case.
        // My previous example for DeviceDetailActivity had more logic for this.
        // }


        btnSubmit.setOnClickListener {
            val name = etName.text.toString().trim()
            // The 'desc' field could be the user-given name if 'name' is a fixed type like "Fan",
            // or it could be an additional description if 'name' is already a custom user input.
            // For simplicity, I'll assume 'name' is what the user types for category/main name,
            // and 'desc' is the specific description. Adjust as per your intent.
            val desc = etDescription.text.toString().trim()
            val watt = etWatt.text.toString().toDoubleOrNull()
            val hours = etHours.text.toString().toDoubleOrNull()

            // Adjust validation as needed. For example, if 'name' comes from 'selectedDeviceType',
            // it might not need to be checked for blank here if 'selectedDeviceType' is guaranteed.
            if (name.isBlank() || watt == null || hours == null) {
                // Made 'desc' optional in this validation, adjust if it's mandatory
                Toast.makeText(this, "Name, Wattage, and Hours must be filled correctly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // --- Get currentUserId from SharedPreferences ---
            val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val currentUserId = prefs.getString("currentUserId", null)

            if (currentUserId == null) {
                Toast.makeText(this, "Error: User session not found. Please log in again.", Toast.LENGTH_LONG).show()
                // Optionally redirect to LoginActivity
                // val loginIntent = Intent(this, LoginActivity::class.java)
                // loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                // startActivity(loginIntent)
                // finish()
                return@setOnClickListener
            }

            // Call insertDevice with all required parameters, including currentUserId
            // The 'isOn' parameter has a default value of 'true' in the method signature,
            // so you can omit it if you want it to default to true, or pass it explicitly.
            val success = dbHelper.insertDevice(
                name,
                desc,
                watt,
                hours,
                true, // Explicitly setting isOn to true (or use your desired default/logic)
                currentUserId // Pass the fetched userId
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