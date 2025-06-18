package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceCategoryAdapter(
    private val deviceCategories: List<String>,
    private val onCategorySelected: (String) -> Unit
) : RecyclerView.Adapter<DeviceCategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceCategoryName: TextView = itemView.findViewById(R.id.tvDeviceCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = deviceCategories[position]
        holder.tvDeviceCategoryName.text = category
        holder.itemView.setOnClickListener {
            onCategorySelected(category)
        }
    }

    override fun getItemCount(): Int = deviceCategories.size
}