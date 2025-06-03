// In app/src/main/java/com/example/energysaving/IndividualDeviceAdapter.kt
package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class IndividualDeviceAdapter(
    private var devices: List<Device>,
    private val onDeviceStateChanged: (Device, Boolean) -> Unit
) : RecyclerView.Adapter<IndividualDeviceAdapter.DeviceViewHolder>() {

    // This list is kept for potential future use (e.g., different icons per type)
    // but is not strictly needed for the updated name display logic.
    // Ensure it matches predefined types in DeviceTypeActivity.kt (excluding "Custom").
    private val actualPredefinedDeviceTypes = listOf("Light", "Fan", "TV", "Fridge", "AC")

    private val expandedItemPositions = mutableSetOf<Int>()
    private val electricityPricePerKwh: Double = 1444.70 // Example, make configurable

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_individual_device, parent, false) //
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val isExpanded = expandedItemPositions.contains(position)
        holder.bind(device, isExpanded)

        holder.itemView.setOnClickListener {
            android.util.Log.d("AdapterClick", "Item clicked at position: $position in IndividualDeviceAdapter")
            if (expandedItemPositions.contains(position)) {
                expandedItemPositions.remove(position)
            } else {
                expandedItemPositions.add(position)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<Device>) {
        this.devices = newDevices
        expandedItemPositions.clear()
        notifyDataSetChanged()
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvIndividualDeviceName) //
        private val detailsTextView: TextView = itemView.findViewById(R.id.tvIndividualDeviceDetails) //
        private val stateSwitch: SwitchCompat = itemView.findViewById(R.id.switchDeviceState) //
        private val layoutExtraDetails: LinearLayout = itemView.findViewById(R.id.layoutDeviceExtraDetails)
        private val tvUsageWeek: TextView = itemView.findViewById(R.id.tvDeviceUsageWeek)
        private val tvCost: TextView = itemView.findViewById(R.id.tvDeviceCost)

        fun bind(device: Device, isExpanded: Boolean) {
            // MODIFIED LOGIC FOR DISPLAYED NAME:
            // Prioritize device.description if it's not blank, as this typically holds the more specific name or detail.
            // Otherwise, fall back to device.name.
            val displayedName = device.description.ifBlank { device.name }

            nameTextView.text = displayedName

            // The detailsTextView can show the original type (device.name) if it's different from displayedName,
            // or simply watt/hours. Let's show watt/hours for now.
            // If you want to show the "type" as well:
            // if (displayedName != device.name && actualPredefinedDeviceTypes.contains(device.name)) {
            //    detailsTextView.text = "${device.name} - ${device.wattUsage}W, ${device.dailyHours} hours/day"
            // } else {
            detailsTextView.text = "${device.wattUsage}W, ${device.dailyHoursGoal} hours/day"
            // }


            stateSwitch.setOnCheckedChangeListener(null)
            stateSwitch.isChecked = device.isOn
            stateSwitch.setOnCheckedChangeListener { _, isChecked ->
                onDeviceStateChanged(device, isChecked)
            }

            if (isExpanded) {
                var currentSessionSecondsForDisplay = 0L
                if (device.isOn && device.lastTurnOnTimestampMillis > 0) {
                    // Calculate how long it's been ON in the current session for display purposes
                    val currentMillis = System.currentTimeMillis()
                    if (currentMillis > device.lastTurnOnTimestampMillis) {
                        currentSessionSecondsForDisplay = (currentMillis - device.lastTurnOnTimestampMillis) / 1000
                    }
                }
                // Total seconds used today for display includes already logged + current running session
                val displayTotalSecondsToday = device.timeUsedTodaySeconds + currentSessionSecondsForDisplay

                val displayUsedHoursToday = displayTotalSecondsToday / 3600.0
                val kwhToday = (device.wattUsage / 1000.0) * displayUsedHoursToday // kWh = (W/1000) * h
                val costToday = kwhToday * electricityPricePerKwh // electricityPricePerKwh from adapter

                // Update your TextViews in item_individual_device.xml
                // Example: Assuming tvDeviceUsageWeek and tvDeviceCost are your TextView IDs
                tvUsageWeek.text = String.format(Locale.US, "%.1f H / %.1f H Goal", displayUsedHoursToday, device.dailyHoursGoal)

                val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID")) // For Indonesia
                rupiahFormat.maximumFractionDigits = 0 // No decimals for Rp
                tvCost.text = rupiahFormat.format(costToday)
                // If you have a separate label for "Cost Today:", set it. Otherwise, include in tvCost.text.

                layoutExtraDetails.visibility = View.VISIBLE
            } else {
                layoutExtraDetails.visibility = View.GONE
            }
        }
    }
}