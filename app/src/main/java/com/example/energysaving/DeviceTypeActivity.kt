package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class DeviceTypeActivity : AppCompatActivity() {

    private val deviceTypes = listOf("Fan", "TV", "Fridge", "AC", "Custom")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_type_activity)

        val listView = findViewById<ListView>(R.id.deviceTypeList)
        val finishBtn = findViewById<Button>(R.id.btnFinish)

        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceTypes)

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedType = deviceTypes[position]
            val intent = Intent(this, DeviceDetailActivity::class.java)
            intent.putExtra("device_type", selectedType)
            startActivity(intent)
        }

        finishBtn.setOnClickListener {
            // Mark user as not new
            val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            prefs.edit { putBoolean("isNewUser", false) }

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
