// In app/src/main/java/com/example/energysaving/IndividualDeviceAdapter.kt
package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // Import ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class IndividualDeviceAdapter(
    private var devices: List<Device>,
    private val onDeviceStateChanged: (Device, Boolean) -> Unit,
    private val onDeviceDelete: (Device) -> Unit // NEW: Add delete listener
) : RecyclerView.Adapter<IndividualDeviceAdapter.DeviceViewHolder>() {

    private val expandedItemPositions = mutableSetOf<Int>()
    private val electricityPricePerKwh: Double = 1444.70 // Example, make configurable

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_individual_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val isExpanded = expandedItemPositions.contains(position)
        holder.bind(device, isExpanded, onDeviceStateChanged, onDeviceDelete) // Pass new listener
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<Device>) {
        this.devices = newDevices
        expandedItemPositions.clear()
        notifyDataSetChanged()
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvIndividualDeviceName)
        private val detailsTextView: TextView = itemView.findViewById(R.id.tvIndividualDeviceDetails)
        private val stateSwitch: SwitchCompat = itemView.findViewById(R.id.switchDeviceState)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteDevice) // NEW: Reference delete button
        private val layoutExtraDetails: LinearLayout = itemView.findViewById(R.id.layoutDeviceExtraDetails)
        private val tvUsageWeek: TextView = itemView.findViewById(R.id.tvDeviceUsageWeek)
        private val tvCost: TextView = itemView.findViewById(R.id.tvDeviceCost)

        fun bind(
            device: Device,
            isExpanded: Boolean,
            onDeviceStateChanged: (Device, Boolean) -> Unit,
            onDeviceDelete: (Device) -> Unit // NEW: Receive delete listener
        ) {
            // MODIFIED LOGIC FOR DISPLAYED NAME:
            // Prioritize device.description if it's not blank, as this typically holds the more specific name or detail.
            // Otherwise, fall back to device.name.
            val displayedName = device.description.ifBlank { device.name }

            nameTextView.text = displayedName
            detailsTextView.text = "${device.wattUsage}W, ${device.dailyHoursGoal} hours/day"

            stateSwitch.setOnCheckedChangeListener(null)
            stateSwitch.isChecked = device.isOn
            stateSwitch.setOnCheckedChangeListener { _, isChecked ->
                onDeviceStateChanged(device, isChecked)
            }

            // NEW: Set click listener for delete button
            btnDelete.setOnClickListener {
                onDeviceDelete(device)
            }

            // Existing click listener for expanding/collapsing details
            itemView.setOnClickListener {
                android.util.Log.d("AdapterClick", "Item clicked at position: $adapterPosition in IndividualDeviceAdapter")
                if (expandedItemPositions.contains(adapterPosition)) {
                    expandedItemPositions.remove(adapterPosition)
                } else {
                    expandedItemPositions.add(adapterPosition)
                }
                notifyItemChanged(adapterPosition)
            }

            if (isExpanded) {
                var currentSessionSecondsForDisplay = 0L
                if (device.isOn && device.lastTurnOnTimestampMillis > 0) {
                    val currentMillis = System.currentTimeMillis()
                    if (currentMillis > device.lastTurnOnTimestampMillis) {
                        currentSessionSecondsForDisplay = (currentMillis - device.lastTurnOnTimestampMillis) / 1000
                    }
                }
                val displayTotalSecondsToday = device.timeUsedTodaySeconds + currentSessionSecondsForDisplay

                val displayUsedHoursToday = displayTotalSecondsToday / 3600.0
                val kwhToday = (device.wattUsage / 1000.0) * displayUsedHoursToday
                val costToday = kwhToday * electricityPricePerKwh

                tvUsageWeek.text = String.format(Locale.US, "%.1f H / %.1f H Goal", displayUsedHoursToday, device.dailyHoursGoal)

                val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                rupiahFormat.maximumFractionDigits = 0
                tvCost.text = rupiahFormat.format(costToday)

                layoutExtraDetails.visibility = View.VISIBLE
            } else {
                layoutExtraDetails.visibility = View.GONE
            }
        }
    }
}