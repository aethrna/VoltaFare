// In app/src/main/java/com/example/energysaving/DeviceCardAdapter.kt
package com.example.energysaving

import android.content.Intent // Make sure Intent is imported
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val title: TextView = itemView.findViewById(R.id.deviceCardTitle) //
        val energyUsage: TextView = itemView.findViewById(R.id.deviceCardEnergyUsage) //
        // val deviceImage: ImageView = itemView.findViewById(R.id.deviceImage) // If you use R.id.deviceImage
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_card_item, parent, false) //
        return DeviceCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceCardViewHolder, position: Int) {
        val group = deviceGroups[position]
        holder.title.text = group.type
        // Formatting the energy usage a bit more clearly
        holder.energyUsage.text = String.format(Locale.US, "%.2f kWh", group.totalWeeklyKwhOfOnDevices)
        // Potentially set deviceImage based on group.type here if you have different icons

        // Set the click listener for the entire card item
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            // Create an Intent to start your new DeviceListDetailActivity
            val intent = Intent(context, DeviceListDetailActivity::class.java).apply {
                // Pass the type of the clicked group to the new activity
                putExtra(DeviceListDetailActivity.EXTRA_DEVICE_TYPE, group.type)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = deviceGroups.size
}