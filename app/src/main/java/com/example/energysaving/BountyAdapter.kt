package com.example.energysaving

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BountyAdapter(private var bounties: List<Bounty>, private val onBountyClick: (Bounty) -> Unit) :
    RecyclerView.Adapter<BountyAdapter.BountyViewHolder>() {

    class BountyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.bountyIcon)
        val title: TextView = itemView.findViewById(R.id.bountyTitle)
        val xpReward: TextView = itemView.findViewById(R.id.bountyXpReward)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BountyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bounty_list, parent, false)
        return BountyViewHolder(view)
    }

    override fun onBindViewHolder(holder: BountyViewHolder, position: Int) {
        val bounty = bounties[position]
        holder.icon.setImageResource(bounty.iconResId)
        holder.title.text = bounty.title
        holder.xpReward.text = "${bounty.xpReward}xp"
        holder.itemView.alpha = if (bounty.isCompleted) 0.5f else 1.0f

        holder.itemView.setOnClickListener {
            if (!bounty.isCompleted) {
                onBountyClick(bounty)
            }
        }
    }

    override fun getItemCount(): Int = bounties.size

    fun updateBounties(newBounties: List<Bounty>) {
        bounties = newBounties
        notifyDataSetChanged()
    }
}