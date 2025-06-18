package com.example.energysaving

import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecommendationsActivity : BaseActivity() {

    override val activeIndicator: Int
        get() = R.id.navItemRecommendations

    private lateinit var devDbHelper: DevDBHelper
    private lateinit var currentUserId: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecommendationAdapter
    private lateinit var tvHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendations)

        devDbHelper = DevDBHelper(this)
        recyclerView = findViewById(R.id.recyclerViewRecommendations)
        tvHeader = findViewById(R.id.tvRecommendationsHeader)
        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("currentUserId", null)
            ?: run {
                tvHeader.text = "Error: User not logged in."
                return
            }

        generateEnergySavingRecommendations()
    }

    private fun generateEnergySavingRecommendations() {
        CoroutineScope(Dispatchers.IO).launch {
            val allDevices = devDbHelper.getAllDevicesForUser(currentUserId)
            val weeklyEnergyConsumption = devDbHelper.getWeeklyEnergyConsumption(currentUserId)

            val prompt = buildGeminiPrompt(allDevices, weeklyEnergyConsumption)

            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = "GEMINI_API_KEY"
                )

                val response = generativeModel.generateContent(prompt)

                withContext(Dispatchers.Main) {
                    val recommendationsText = response.text
                    if (recommendationsText != null) {
                        val recommendationList = recommendationsText.lines().filter { it.isNotBlank() }
                        adapter = RecommendationAdapter(recommendationList)
                        recyclerView.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                }
            }
        }
    }

    private fun buildGeminiPrompt(devices: List<Device>, weeklyConsumption: Map<String, Double>): String {
        val deviceData = StringBuilder("Here are my registered devices and their current daily usage:\n")
        if (devices.isEmpty()) {
            deviceData.append("No devices registered yet.\n")
        } else {
            devices.forEach { device ->
                val usedHours = device.timeUsedTodaySeconds / 3600.0
                deviceData.append("- ${device.description} (${device.name}): ${device.wattUsage}W, used today for %.1f hours (goal: %.1f hours).\n".format(usedHours, device.dailyHoursGoal))
            }
        }

        val consumptionData = StringBuilder("\nMy weekly energy consumption (kWh) over the last 7 days:\n")
        if (weeklyConsumption.isEmpty()) {
            consumptionData.append("No historical energy data available.\n")
        } else {
            weeklyConsumption.forEach { (date, kwh) ->
                consumptionData.append("- $date: %.2f kWh\n".format(kwh))
            }
        }

        return "Given the following information about my household devices and energy consumption, " +
                "please provide practical and actionable tips to reduce my energy usage and save money. " +
                "Focus on specific device usage patterns and general good practices. " +
                "Provide the output as a simple list separated by newlines, with each tip starting with a number and a period. For example: '1. Unplug chargers.'\n\n" +
                deviceData.toString() +
                consumptionData.toString()
    }
}