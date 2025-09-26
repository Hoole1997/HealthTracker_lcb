package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.databinding.ItemMedsRemindBinding
import com.healthtracker.blood.suger.ui.model.MedsReminderItem
import com.healthtracker.blood.suger.ui.model.ReminderStatus
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible

/**
 * 药物提醒列表适配器
 */
class MedsReminderAdapter(
    private val onItemClick: (MedsReminderItem) -> Unit = {},
    private val onMoreClick: (MedsReminderItem) -> Unit = {}
) : ListAdapter<MedsReminderItem, MedsReminderAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedsRemindBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemMedsRemindBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // 设置点击监听
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.ivMore.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMoreClick(getItem(position))
                }
            }
        }

        fun bind(item: MedsReminderItem) {
            with(binding) {
                // 设置时间
                tvTime.text = item.time

                // 设置药物名称
                tvName.text = item.medicineName

                // 设置备注
                if (item.notes.isNotBlank()) {
                    tvNotes.text = item.notes
                    tvNotes.visibility = android.view.View.VISIBLE
                } else {
                    tvNotes.visibility = android.view.View.GONE
                }
                // 根据状态设置图标和背景
                if(item.isTaken()){
                    ivTake.visible()
                }else{
                    ivTake.gone()
                }
            }
        }
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class DiffCallback : DiffUtil.ItemCallback<MedsReminderItem>() {
        override fun areItemsTheSame(
            oldItem: MedsReminderItem,
            newItem: MedsReminderItem
        ): Boolean {
            return oldItem.reminderId == newItem.reminderId &&
                   oldItem.reminderDateTime == newItem.reminderDateTime
        }

        override fun areContentsTheSame(
            oldItem: MedsReminderItem,
            newItem: MedsReminderItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}