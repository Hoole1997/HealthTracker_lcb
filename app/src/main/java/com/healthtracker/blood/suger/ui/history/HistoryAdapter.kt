package com.healthtracker.blood.suger.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ItemHistoryRecordBinding
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible
import com.healthtracker.blood.suger.data.utils.DateTimeUtils

/**
 * 历史记录适配器
 * 支持血糖和血压记录的统一显示
 */
class HistoryAdapter : ListAdapter<HistoryRecordItem, HistoryAdapter.HistoryViewHolder>(
    HistoryDiffCallback()
) {
    
    // 事件回调接口
    interface OnItemClickListener {
        /**
         * 记录项点击事件
         * @param item 被点击的记录项
         * @param position 位置
         */
        fun onItemClick(item: HistoryRecordItem, position: Int)
        
        /**
         * 删除按钮点击事件
         * @param item 要删除的记录项
         * @param position 位置
         */
        fun onDeleteClick(item: HistoryRecordItem, position: Int)
    }
    
    private var itemClickListener: OnItemClickListener? = null

    /**
     * 是否展示删除按钮（默认 true，血糖统计页可关闭）
     */
    var showDeleteButton: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    
    /**
     * 设置事件监听器
     */
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.itemClickListener = listener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class HistoryViewHolder(private val binding: ItemHistoryRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: HistoryRecordItem) {
            with(binding) {
                // 设置主要数值
                tvValue1.text = item.getPrimaryValue()
                
                // 设置次要数值（血压有舒张压，血糖没有）
                val secondaryValue = item.getSecondaryValue()
                if (secondaryValue != null) {
                    tvValue2.text = secondaryValue
                    tvValue2.visible()
                    "${tvStatus.context.getString(R.string.pulse)}:${item.getStatus(tvStatus.context)}".also { tvStatus.text = it }

                } else {
                    tvValue2.gone()
                    // 设置状态描述
                    "${tvStatus.context.getString(R.string.status)}:${item.getStatus(tvStatus.context)}".also { tvStatus.text = it }

                }
                
                // 设置单位
                tvUnit.text = item.getUnit()
                
                // 设置等级
                tvLeve.text = item.getLevel(tvLeve.context)

                vRangeFlag.backgroundTintList =  ContextCompat.getColorStateList(vRangeFlag.context, item.getLeveColorRes())
                

                
                // 设置记录时间
                tvRecordTime.text = DateTimeUtils.formatDateTime(item.getRecordTime())
                
                // 设置点击事件
                root.setOnClickListener {
                    itemClickListener?.onItemClick(item, adapterPosition)
                }
                
                // 设置删除按钮点击事件
                if (showDeleteButton) {
                    ivDelete.visible()
                    ivDelete.setOnClickListener {
                        itemClickListener?.onDeleteClick(item, adapterPosition)
                    }
                } else {
                    ivDelete.gone()
                    ivDelete.setOnClickListener(null)
                }
            }
        }
    }
    
    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryRecordItem>() {
        override fun areItemsTheSame(oldItem: HistoryRecordItem, newItem: HistoryRecordItem): Boolean {
            return oldItem.getId() == newItem.getId() && 
                   oldItem.getRecordType() == newItem.getRecordType()
        }
        
        override fun areContentsTheSame(oldItem: HistoryRecordItem, newItem: HistoryRecordItem): Boolean {
            return oldItem.getPrimaryValue() == newItem.getPrimaryValue() &&
                   oldItem.getSecondaryValue() == newItem.getSecondaryValue() &&
                   oldItem.getUnit() == newItem.getUnit() &&
                   oldItem.getLevel(App.INSTANCE) == newItem.getLevel(App.INSTANCE) &&
                   oldItem.getStatus(App.INSTANCE) == newItem.getStatus(App.INSTANCE) &&
                   oldItem.getRecordTime() == newItem.getRecordTime()
        }
    }
}
