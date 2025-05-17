package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DeviceGroup(
    val title: String,
    val devices: List<Device>,
    val totalEnergyUsage: Double
)

class DeviceCardAdapter(private val deviceGroups: List<DeviceGroup>) :
    RecyclerView.Adapter<DeviceCardAdapter.DeviceCardViewHolder>() {

    class DeviceCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.deviceCardTitle)
        val description: TextView = itemView.findViewById(R.id.deviceCardDescription)
        val energyUsage: TextView = itemView.findViewById(R.id.deviceCardEnergyUsage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_card_item, parent, false)
        return DeviceCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceCardViewHolder, position: Int) {
        val group = deviceGroups[position]
        holder.title.text = group.title
        holder.description.text = "Devices: ${group.devices.size}"
        holder.energyUsage.text = "Weekly Usage: ${group.totalEnergyUsage} kWh"
    }

    override fun getItemCount(): Int = deviceGroups.size
}
