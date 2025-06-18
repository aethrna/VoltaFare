package com.example.energysaving

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class AchievementListAdapter(private var achievements: List<Achievement>) :
    RecyclerView.Adapter<AchievementListAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.achievementIcon)
        val title: TextView = itemView.findViewById(R.id.achievementTitle)
        val statusIcon: ImageView = itemView.findViewById(R.id.achievementStatusIcon)
        val card: MaterialCardView = itemView.findViewById(R.id.achievementCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement_list, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        holder.icon.setImageResource(achievement.iconResId)
        holder.title.text = achievement.title

        if (achievement.isUnlocked) {
            holder.statusIcon.setImageResource(R.drawable.baseline_check_box_24)
            holder.card.setCardBackgroundColor(Color.WHITE)
            holder.title.setTextColor(Color.BLACK)
            holder.icon.alpha = 1.0f
            holder.title.alpha = 1.0f
        } else {
            holder.statusIcon.setImageResource(R.drawable.locked)
            holder.card.setCardBackgroundColor(Color.parseColor("#E0E0E0"))
            holder.title.setTextColor(Color.DKGRAY)
            holder.icon.alpha = 0.5f
            holder.title.alpha = 0.5f
        }
    }

    override fun getItemCount(): Int = achievements.size

    fun updateAchievements(newAchievements: List<Achievement>) {
        achievements = newAchievements
        notifyDataSetChanged()
    }
}