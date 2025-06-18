// In app/src/main/java/com/example/energysaving/DeviceCardAdapter.kt
package com.example.energysaving

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        val deviceImage: ImageView = itemView.findViewById(R.id.deviceImage)
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

        val iconRes = when (group.type) {
            "Lights" -> R.drawable.lights_ic
            "AC/Heater" -> R.drawable.acheater_ic
            "Entertainment" -> R.drawable.entertainment_ic
            "Kitchen Appliances" -> R.drawable.kitchenware_ic
            "Power Stations" -> R.drawable.power_ic
            else -> R.drawable.lights_ic
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