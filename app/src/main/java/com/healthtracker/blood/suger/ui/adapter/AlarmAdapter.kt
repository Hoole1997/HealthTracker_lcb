package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.databinding.ItemAlarmBinding

/**
 * 闹钟列表适配器
 * 用于血压和血糖闹钟列表的数据绑定
 */
class AlarmAdapter(
    private val onSwitchChanged: (AlarmRecord, Boolean) -> Unit
) : ListAdapter<AlarmRecord, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = ItemAlarmBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmViewHolder(private val binding: ItemAlarmBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: AlarmRecord) {
            // 绑定时间显示
            binding.tvTime.text = alarm.getFormattedTime()
            
            // 绑定开关状态
            binding.stAlarm.isChecked = alarm.isEnabled
            
            // 设置开关监听器
            binding.stAlarm.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChanged(alarm, isChecked)
            }
        }
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class AlarmDiffCallback : DiffUtil.ItemCallback<AlarmRecord>() {
        override fun areItemsTheSame(oldItem: AlarmRecord, newItem: AlarmRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AlarmRecord, newItem: AlarmRecord): Boolean {
            return oldItem == newItem
        }
    }
}