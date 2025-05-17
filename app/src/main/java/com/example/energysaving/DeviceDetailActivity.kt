package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class DeviceDetailActivity : AppCompatActivity() {

    private lateinit var dbHelper: Dev_DBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_detail_activity)

        dbHelper = Dev_DBHelper(this)

        val etName = findViewById<EditText>(R.id.etDeviceName)
        val etDescription = findViewById<EditText>(R.id.etDeviceDescription)
        val etWatt = findViewById<EditText>(R.id.etWattUsage)
        val etHours = findViewById<EditText>(R.id.etDailyHours)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitDevice)

        btnSubmit.setOnClickListener {
            val name = etName.text.toString()
            val desc = etDescription.text.toString()
            val watt = etWatt.text.toString().toDoubleOrNull()
            val hours = etHours.text.toString().toDoubleOrNull()

            if (name.isBlank() || desc.isBlank() || watt == null || hours == null) {
                Toast.makeText(this, "Fill in all fields correctly", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val success = dbHelper.insertDevice(name, desc, watt, hours)
            if (success) {
                Toast.makeText(this, "Device added", Toast.LENGTH_SHORT).show()
                finish() // go back to previous (e.g., device type screen or main)
            } else {
                Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
