package com.example.energysaving

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecommendationsActivity : AppCompatActivity() {

    private lateinit var devDbHelper: DevDBHelper
    private lateinit var tvRecommendations: TextView
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recommendations) // You'll need to create this layout

        devDbHelper = DevDBHelper(this)
        tvRecommendations = findViewById(R.id.tvRecommendations) // Assuming you add this TextView to your layout

        val prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        currentUserId = prefs.getString("currentUserId", null)
            ?: run {
                tvRecommendations.text = "Error: User not logged in."
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
                // Initialize the GenerativeModel (replace with your actual API key)
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash", // Or "gemini-1.5-flash", "gemini-1.5-pro"
                    apiKey = "AIzaSyDyAF8bPkCG3W2Ms6Ugq0u0-5lzuEyYuE8" // NEVER HARDCODE IN PRODUCTION APPS! Use a secure method.
                )

                val response = generativeModel.generateContent(prompt)

                withContext(Dispatchers.Main) {
                    if (response.text != null) {
                        tvRecommendations.text = response.text
                    } else {
                        tvRecommendations.text = "Failed to get recommendations."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvRecommendations.text = "Error: ${e.message}"
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
                "Please be concise and provide actionable steps:\n\n" +
                deviceData.toString() +
                consumptionData.toString() +
                "\nEnergy saving tips:"
    }
}