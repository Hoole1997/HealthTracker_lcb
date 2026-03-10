package com.daily.health.manager.face.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.databinding.TrItemAlarmBinding

/**
 * 闹钟列表适配器
 * 用于血压和血糖闹钟列表的数据绑定
 */
class AlarmAdapter(
    private val onSwitchChanged: (AlarmRecord, Boolean) -> Unit,
    private val onItemClick: ((AlarmRecord) -> Unit)? = null
) : ListAdapter<AlarmRecord, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val binding = TrItemAlarmBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlarmViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlarmViewHolder(private val binding: TrItemAlarmBinding) :
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
            
            // 设置点击监听器
            binding.root.setOnClickListener {
                onItemClick?.invoke(alarm)
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