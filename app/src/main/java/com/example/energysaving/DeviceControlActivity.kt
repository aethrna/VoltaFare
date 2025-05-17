// In DeviceControlActivity.kt
package com.example.energysaving

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeviceControlActivity : AppCompatActivity() {

    private lateinit var dbHelper: DevDBHelper
    private lateinit var individualDeviceAdapter: IndividualDeviceAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvDeviceTypeHeader: TextView
    private var deviceType: String? = null

    companion object {
        const val EXTRA_DEVICE_TYPE = "extra_device_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_control)

        dbHelper = DevDBHelper(this)
        deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE)

        tvDeviceTypeHeader = findViewById(R.id.tvDeviceTypeHeader)
        recyclerView = findViewById(R.id.recyclerViewIndividualDevices)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (deviceType == null) {
            Toast.makeText(this, "Device type not specified.", Toast.LENGTH_LONG).show()
            finish() // Close activity if no type is passed
            return
        }

        tvDeviceTypeHeader.text = "$deviceType Devices"

        setupAdapter()
        loadDevices()
    }

    private fun setupAdapter() {
        individualDeviceAdapter = IndividualDeviceAdapter(emptyList()) { device, isOn ->
            // Update device state in DB
            val success = dbHelper.updateDeviceState(device.id, isOn)
            if (success > 0) {
                // Optionally, update the local list and refresh adapter, or reload all
                // For simplicity here, we can just update the device object's state
                // and the switch is already visually updated.
                // To ensure data consistency if other things depend on this list, reload:
                device.isOn = isOn // Update the state in the current list item
                Toast.makeText(
                    this,
                    "${device.description} turned ${if (isOn) "ON" else "OFF"}",
                    Toast.LENGTH_SHORT
                ).show()

                // The adapter's list will be out of sync with the database if you don't reload
                // or specifically update the item in the adapter's list.
                // The switch is already updated visually by the user.
                // To refresh the entire list from DB:
                // loadDevices()
            } else {
                Toast.makeText(this, "Failed to update state", Toast.LENGTH_SHORT).show()
                // Revert switch state if DB update fails
                // This requires a bit more complex handling in the adapter or here
            }
        }
        recyclerView.adapter = individualDeviceAdapter
    }

    private fun loadDevices() {
        deviceType?.let { type ->
            val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            val currentUserId = prefs.getString("currentUserId", null) // Get String?

            if (currentUserId != null) {
                val devices = dbHelper.getDevicesByTypeForUser(type, currentUserId) // Use new method
                individualDeviceAdapter.updateDevices(devices)
            } else {
                Toast.makeText(
                    this,
                    "Error: User not identified. Cannot load devices.",
                    Toast.LENGTH_LONG
                ).show()
                individualDeviceAdapter.updateDevices(emptyList())
                // Consider finishing activity or showing error state
            }
        }
    }
}