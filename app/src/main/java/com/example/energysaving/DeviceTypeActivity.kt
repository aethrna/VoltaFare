package com.example.energysaving

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeviceTypeActivity : AppCompatActivity() {

    private val deviceCategories = listOf("Lights", "AC/Heater", "Power Stations", "Entertainment", "Kitchen Appliances", "Custom")

    private lateinit var collapsibleHeader: LinearLayout
    private lateinit var imgExpandCollapseArrow: ImageView
    private lateinit var deviceCategoryRecyclerView: RecyclerView
    private lateinit var finishBtn: Button

    private var isExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_type_activity)

        collapsibleHeader = findViewById(R.id.collapsibleHeader)
        imgExpandCollapseArrow = findViewById(R.id.imgExpandCollapseArrow)
        deviceCategoryRecyclerView = findViewById(R.id.deviceCategoryRecyclerView)
        finishBtn = findViewById(R.id.btnFinish)


        deviceCategoryRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = DeviceCategoryAdapter(deviceCategories) { selectedCategory ->
            val intent = Intent(this, DeviceDetailActivity::class.java)
            intent.putExtra("device_type", selectedCategory)
            startActivity(intent)
            toggleCollapsibleSection()
        }
        deviceCategoryRecyclerView.adapter = adapter

        imgExpandCollapseArrow.rotation = if (isExpanded) 90f else 0f

        collapsibleHeader.setOnClickListener {
            toggleCollapsibleSection()
        }

        finishBtn.setOnClickListener {
            val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
            prefs.edit { putBoolean("isNewUser", false) }

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun toggleCollapsibleSection() {
        isExpanded = !isExpanded
        if (isExpanded) {
            deviceCategoryRecyclerView.visibility = View.VISIBLE
            imgExpandCollapseArrow.animate().rotation(90f).setDuration(200).start()
        } else {
            deviceCategoryRecyclerView.visibility = View.GONE
            imgExpandCollapseArrow.animate().rotation(0f).setDuration(200).start()
        }
    }
}