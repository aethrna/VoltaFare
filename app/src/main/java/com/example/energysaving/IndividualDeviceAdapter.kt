package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.NumberFormat
import java.util.*

class IndividualDeviceAdapter(
    private var devices: List<Device>,
    private val onDeviceStateChanged: (Device) -> Unit,
    private val onDeviceDelete: (Device) -> Unit
) : RecyclerView.Adapter<IndividualDeviceAdapter.DeviceViewHolder>() {

    private val expandedItemPositions = mutableSetOf<Int>()
    private val electricityPricePerKwh: Double = 1444.70

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceCard: MaterialCardView = itemView.findViewById(R.id.deviceCard)
        val nameTextView: TextView = itemView.findViewById(R.id.tvIndividualDeviceName)
        val detailsTextView: TextView = itemView.findViewById(R.id.tvIndividualDeviceDetails)
        val deviceIcon: ImageView = itemView.findViewById(R.id.ivDeviceIcon)
        val editButton: ImageButton = itemView.findViewById(R.id.btnEditDevice)
        val moreOptionsButton: ImageButton = itemView.findViewById(R.id.btnMoreOptions)
        val extraDetailsLayout: LinearLayout = itemView.findViewById(R.id.layoutDeviceExtraDetails)
        val usageWeekTextView: TextView = itemView.findViewById(R.id.tvDeviceUsageWeek)
        val costTextView: TextView = itemView.findViewById(R.id.tvDeviceCost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_individual_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.nameTextView.text = device.description.ifBlank { device.name }
        holder.detailsTextView.text = "${device.wattUsage}W, ${device.dailyHoursGoal} hours/day"

        updateIcon(holder, device)
        setupClickListeners(holder, device, position)
        updateExpandedView(holder, device)
    }

    private fun updateIcon(holder: DeviceViewHolder, device: Device) {
        val context = holder.itemView.context
        val (onIconRes, offIconRes) = when (device.name) {
            "Lights" -> Pair(R.drawable.bulbon, R.drawable.bulboff)
            "AC/Heater" -> Pair(R.drawable.tempon, R.drawable.tempoff)
            "Entertainment" -> Pair(R.drawable.enteron, R.drawable.enteroff)
            "Kitchen Appliances" -> Pair(R.drawable.applon, R.drawable.apploff)
            "Power Stations" -> Pair(R.drawable.poweron, R.drawable.poweroff)
            else -> Pair(R.drawable.bulbon, R.drawable.bulboff) // Default icons
        }

        if (device.isOn) {
            holder.deviceIcon.setImageResource(onIconRes)
            holder.deviceIcon.setColorFilter(ContextCompat.getColor(context, R.color.green_on))
        } else {
            holder.deviceIcon.setImageResource(offIconRes)
            holder.deviceIcon.setColorFilter(ContextCompat.getColor(context, R.color.red_off))
        }
    }

    private fun setupClickListeners(holder: DeviceViewHolder, device: Device, position: Int) {
        holder.deviceIcon.setOnClickListener {
            onDeviceStateChanged(device)
        }

        holder.deviceCard.setOnClickListener {
            if (expandedItemPositions.contains(position)) {
                expandedItemPositions.remove(position)
            } else {
                expandedItemPositions.add(position)
            }
            notifyItemChanged(position)
        }

        holder.editButton.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Edit: ${device.description}", Toast.LENGTH_SHORT).show()
        }

        holder.moreOptionsButton.setOnClickListener {
            onDeviceDelete(device)
        }
    }

    private fun updateExpandedView(holder: DeviceViewHolder, device: Device) {
        val isExpanded = expandedItemPositions.contains(holder.adapterPosition)
        if (isExpanded) {
            holder.extraDetailsLayout.visibility = View.VISIBLE

            var currentSessionSeconds = 0L
            if (device.isOn && device.lastTurnOnTimestampMillis > 0) {
                currentSessionSeconds = (System.currentTimeMillis() - device.lastTurnOnTimestampMillis) / 1000
            }
            val totalSecondsToday = device.timeUsedTodaySeconds + currentSessionSeconds
            val usedHoursToday = totalSecondsToday / 3600.0
            val kwhToday = (device.wattUsage / 1000.0) * usedHoursToday
            val costToday = kwhToday * electricityPricePerKwh

            holder.usageWeekTextView.text = String.format(Locale.US, "%.1f H", usedHoursToday)
            val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            rupiahFormat.maximumFractionDigits = 0
            holder.costTextView.text = rupiahFormat.format(costToday)
        } else {
            holder.extraDetailsLayout.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    fun updateDevices(newDevices: List<Device>) {
        devices = newDevices
        expandedItemPositions.clear()
        notifyDataSetChanged()
    }
}