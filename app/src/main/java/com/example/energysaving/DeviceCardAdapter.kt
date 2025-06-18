// In app/src/main/java/com/example/energysaving/DeviceCardAdapter.kt
package com.example.energysaving

import android.content.Intent // Make sure Intent is imported
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
// Import ImageView if you need to access it from device_card_item.xml
// import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

data class DeviceGroup(
    val type: String,
    val allDevicesInGroup: List<Device>,
    val totalWeeklyKwhOfOnDevices: Double
 )

class DeviceCardAdapter(private val deviceGroups: List<DeviceGroup>) :
    RecyclerView.Adapter<DeviceCardAdapter.DeviceCardViewHolder>() {

    class DeviceCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.deviceCardTitle)
        val energyUsage: TextView = itemView.findViewById(R.id.deviceCardEnergyUsage)
        val deviceImage: ImageView = itemView.findViewById(R.id.deviceImage) // Make sure you have this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_card_item, parent, false)
        return DeviceCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceCardViewHolder, position: Int) {
        val group = deviceGroups[position]
        holder.title.text = group.type
        holder.energyUsage.text = String.format(Locale.US, "%.1f", group.totalWeeklyKwhOfOnDevices)

        // ADD THIS LOGIC to set the icon based on the device type
        val iconRes = when (group.type) {
            "Lights" -> R.drawable.lights_ic // Replace with your actual lightbulb icon drawable
            "AC/Heater" -> R.drawable.acheater_ic // Example: you'd need to add this drawable
            "Entertainment" -> R.drawable.entertainment_ic // Example
            "Kitchen Appliances" -> R.drawable.kitchenware_ic // Example
            "Power Stations" -> R.drawable.power_ic // Example
            else -> R.drawable.nav_add // A default icon
        }
        holder.deviceImage.setImageResource(iconRes)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeviceListDetailActivity::class.java).apply {
                putExtra(DeviceListDetailActivity.EXTRA_DEVICE_TYPE, group.type)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = deviceGroups.size
}