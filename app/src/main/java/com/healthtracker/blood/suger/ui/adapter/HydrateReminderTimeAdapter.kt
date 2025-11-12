package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.R

class HydrateReminderTimeAdapter(private val times: List<String>) :
    RecyclerView.Adapter<HydrateReminderTimeAdapter.TimeViewHolder>() {

    private var showDelete: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hydrate_setting_reminder_time, parent, false)
        return TimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        val t = times[position]
        holder.tvTime.text = t
        holder.imgDelete.visibility = if (showDelete) View.VISIBLE else View.GONE
        // 最后一项不显示分割线
        holder.divider.visibility = if (position == times.lastIndex) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = times.size

    fun setDeleteMode(enabled: Boolean) {
        showDelete = enabled
        notifyDataSetChanged()
    }

    class TimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val divider: View = itemView.findViewById(R.id.divider)
        val imgDelete: AppCompatImageView = itemView.findViewById(R.id.imgDelete)
    }
}