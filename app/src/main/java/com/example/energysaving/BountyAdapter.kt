package com.example.energysaving

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BountyAdapter(private var bounties: List<Bounty>, private val onBountyClick: (Bounty) -> Unit) :
    RecyclerView.Adapter<BountyAdapter.BountyViewHolder>() {

    class BountyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.bountyIcon)
        val title: TextView = itemView.findViewById(R.id.bountyTitle)
        val description: TextView = itemView.findViewById(R.id.bountyDescription)
        val xpReward: TextView = itemView.findViewById(R.id.bountyXpReward)
        val progressBar: ProgressBar = itemView.findViewById(R.id.bountyProgressBar)
        val progressText: TextView = itemView.findViewById(R.id.bountyProgressText)


        fun bind(bounty: Bounty, onBountyClick: (Bounty) -> Unit) {
            icon.setImageResource(bounty.iconResId)
            title.text = bounty.title
            description.text = bounty.description
            xpReward.text = "${bounty.xpReward}xp"

            if (bounty.progressTarget > 0 && bounty.progressTarget != 1) { // Show progress bar if not a simple 1-step completion
                progressBar.max = bounty.progressTarget
                progressBar.progress = bounty.progressCurrent
                progressBar.visibility = View.VISIBLE
                progressText.text = "Progress: ${bounty.progressCurrent}/${bounty.progressTarget}"
                progressText.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
            }

            // Style completed bounties differently
            if (bounty.isCompleted) {
                itemView.alpha = 0.5f // Grey out
                // Optionally change background color, add a "COMPLETED" text overlay etc.
            } else {
                itemView.alpha = 1.0f
            }

            itemView.setOnClickListener {
                if (!bounty.isCompleted) { // Only clickable if not completed
                    onBountyClick(bounty)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BountyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bounty_list, parent, false)
        return BountyViewHolder(view)
    }

    override fun onBindViewHolder(holder: BountyViewHolder, position: Int) {
        holder.bind(bounties[position], onBountyClick)
    }

    override fun getItemCount(): Int = bounties.size

    fun updateBounties(newBounties: List<Bounty>) {
        bounties = newBounties
        notifyDataSetChanged()
    }
}