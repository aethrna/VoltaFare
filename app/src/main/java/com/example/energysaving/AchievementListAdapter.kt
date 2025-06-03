package com.example.energysaving

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AchievementListAdapter(private var achievements: List<Achievement>) :
    RecyclerView.Adapter<AchievementListAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.achievementIcon)
        val title: TextView = itemView.findViewById(R.id.achievementTitle)
        val description: TextView = itemView.findViewById(R.id.achievementDescription)
        val progressBar: ProgressBar = itemView.findViewById(R.id.achievementProgressBar)
        val progressText: TextView = itemView.findViewById(R.id.achievementProgressText)
        val statusIcon: ImageView = itemView.findViewById(R.id.achievementStatusIcon)

        fun bind(achievement: Achievement) {
            icon.setImageResource(achievement.iconResId)
            title.text = achievement.title
            description.text = achievement.description

            if (achievement.progressTarget > 0) { // Check if it's a progress-based achievement
                progressBar.max = achievement.progressTarget
                progressBar.progress = achievement.progressCurrent
                progressBar.visibility = View.VISIBLE
                progressText.text = "Progress: ${achievement.progressCurrent}/${achievement.progressTarget}"
                progressText.visibility = View.VISIBLE
            } else { // Simple achievement (e.g., 1 step to unlock)
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
            }

            if (achievement.isUnlocked) {
                statusIcon.setImageResource(R.drawable.baseline_check_box_24) // Green checkmark
                statusIcon.setColorFilter(Color.parseColor("#4CAF50")) // Green color
                // You might also grey out the text/icon if unlocked and not showing progress
            } else {
                statusIcon.setImageResource(R.drawable.locked) // Lock icon
                statusIcon.setColorFilter(Color.parseColor("#757575")) // Grey color
            }

            // Visually disable unachieved items (optional, but good for UX)
            val alpha = if (achievement.isUnlocked) 1.0f else 0.5f
            itemView.alpha = alpha
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement_list, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(achievements[position])
    }

    override fun getItemCount(): Int = achievements.size

    fun updateAchievements(newAchievements: List<Achievement>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }
}