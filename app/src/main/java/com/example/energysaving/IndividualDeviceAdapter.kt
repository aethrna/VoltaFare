// In IndividualDeviceAdapter.kt
package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView

class IndividualDeviceAdapter(
    private var devices: List<Device>,
    private val onDeviceStateChanged: (Device, Boolean) -> Unit
) : RecyclerView.Adapter<IndividualDeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_individual_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<Device>) {
        this.devices = newDevices
        notifyDataSetChanged() // Or use DiffUtil for better performance
    }

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tvIndividualDeviceName)
        private val detailsTextView: TextView = itemView.findViewById(R.id.tvIndividualDeviceDetails)
        private val stateSwitch: SwitchCompat = itemView.findViewById(R.id.switchDeviceState)

        fun bind(device: Device) {
            // If device.name is generic (e.g. "Light"), use description. Otherwise, use name.
            nameTextView.text = if (device.name == device.description || device.description.isBlank()) {
                "${device.name} ID: ${device.id}" // Show ID to distinguish same-named items
            } else {
                device.description
            }
            detailsTextView.text = "${device.wattUsage}W, ${device.dailyHours} hours/day"

            stateSwitch.setOnCheckedChangeListener(null) // Important to prevent listener firing during bind
            stateSwitch.isChecked = device.isOn
            stateSwitch.setOnCheckedChangeListener { _, isChecked ->
                onDeviceStateChanged(device, isChecked)
            }
        }
    }
}