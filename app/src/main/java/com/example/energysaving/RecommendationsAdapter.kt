package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecommendationAdapter(private var recommendations: List<String>) :
    RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder>() {

    class RecommendationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recommendationTextView: TextView = itemView.findViewById(R.id.tvRecommendationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommendations, parent, false)
        return RecommendationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        val cleanedText = recommendations[position].replaceFirst(Regex("^\\d+\\.\\s*|^-\\s*"), "")
        holder.recommendationTextView.text = cleanedText
    }

    override fun getItemCount() = recommendations.size
}