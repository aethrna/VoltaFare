// app/src/main/java/com/example/energysaving/AchievementAdapter.kt
package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AchievementAdapter(private val achievements: List<Achievement>) :
    RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.achievementIcon)
        val title: TextView = itemView.findViewById(R.id.achievementTitle)
        val description: TextView = itemView.findViewById(R.id.achievementDescription)
        val progressBar: ProgressBar = itemView.findViewById(R.id.achievementProgressBar)
        val progressText: TextView = itemView.findViewById(R.id.achievementProgressText)

        fun bind(achievement: Achievement) {
            icon.setImageResource(achievement.iconResId)
            title.text = achievement.title
            description.text = achievement.description
            progressBar.progress = achievement.progressPercentage
            progressText.text = "${achievement.progressPercentage}% of User Achieved This"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement_card, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(achievements[position])
    }

    override fun getItemCount(): Int = achievements.size
}