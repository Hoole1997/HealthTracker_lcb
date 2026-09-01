package com.daily.health.manager.presentation.medication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.daily.health.manager.databinding.TrItemRemindMedsTimeBinding
import com.healthtracker.framework.ext.click

/** 只展示给药时间并转发点击位置，不参与提醒时间计算和闹钟调度。 */
class MedicationTimeAdapter(
    private val onTimeClick: (Int) -> Unit
) : RecyclerView.Adapter<MedicationTimeAdapter.TimeViewHolder>() {

    private var timeList = listOf<String>()

    fun submitReminderTimes(newTimes: List<String>) {
        timeList = newTimes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val binding = TrItemRemindMedsTimeBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(timeList[position], position)
    }

    override fun getItemCount(): Int = timeList.size

    inner class TimeViewHolder(
        private val binding: TrItemRemindMedsTimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(time: String, position: Int) {
            binding.tvTime.text = time

            binding.root.click {
                onTimeClick(position)
            }
        }
    }
}
