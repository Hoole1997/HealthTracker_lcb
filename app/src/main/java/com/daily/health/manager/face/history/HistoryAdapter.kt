package com.daily.health.manager.face.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daily.health.manager.R
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.TrItemCholHistoryRecordBinding
import com.daily.health.manager.databinding.TrItemHistoryRecordBinding
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible

/**
 * 历史记录适配器
 * 支持多种健康记录类型的统一显示
 */
class HistoryAdapter : ListAdapter<HistoryRecordItem, RecyclerView.ViewHolder>(
    HistoryDiffCallback()
) {

    companion object {
        private const val VIEW_TYPE_SIMPLE = 1      // 血糖/血压/心率/BMI
        private const val VIEW_TYPE_CHOLESTEROL = 2 // 胆固醇
    }

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

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).getRecordType()) {
            HistoryRecordItem.RecordType.CHOLESTEROL -> VIEW_TYPE_CHOLESTEROL
            HistoryRecordItem.RecordType.BLOOD_SUGAR,
            HistoryRecordItem.RecordType.BLOOD_PRESSURE,
            HistoryRecordItem.RecordType.HEART_RATE,
            HistoryRecordItem.RecordType.BMI_RECORD -> VIEW_TYPE_SIMPLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_SIMPLE -> {
                val binding = TrItemHistoryRecordBinding.inflate(inflater, parent, false)
                SimpleHistoryViewHolder(binding)
            }
            VIEW_TYPE_CHOLESTEROL -> {
                val binding = TrItemCholHistoryRecordBinding.inflate(inflater, parent, false)
                CholesterolViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is SimpleHistoryViewHolder -> holder.bind(item)
            is CholesterolViewHolder -> holder.bind(item as CholesterolHistoryItem)
        }
    }

    inner class SimpleHistoryViewHolder(private val binding: TrItemHistoryRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: HistoryRecordItem) {
            with(binding) {
                // 设置主要数值
                tvValue1.text = item.getPrimaryValue()

                // 设置次要数值（血压有舒张压，其他类型没有）
                val secondaryValue = item.getSecondaryValue()
                if (secondaryValue != null) {
                    tvValue2.text = secondaryValue
                    tvValue2.visible()
                } else {
                    tvValue2.gone()
                }

                // 设置状态描述（血压显示心率，血糖显示状态，心率和BMI不显示）
                val status = item.getStatus(tvStatus.context)
                if (status != null) {
                    if (secondaryValue != null) {
                        // 血压：显示 "Pulse: xxx"
                        tvStatus.text = "${tvStatus.context.getString(R.string.tr_pulse)}:$status"
                    } else {
                        if(item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_SUGAR){
                            // 血糖：显示 "Status: xxx"
                            tvStatus.text = "${tvStatus.context.getString(R.string.tr_status)}:$status"
                        }else{
                            // BMI：直接显示身高体重
                            tvStatus.text = status
                        }

                    }
                    tvStatus.visible()
                } else {
                    // 心率：不显示状态
                    tvStatus.gone()
                }

                // 设置单位
                tvUnit.text = item.getUnit()

                // 设置等级
                tvLeve.text = item.getLevel(tvLeve.context)

                // 设置左侧颜色标记
                vRangeFlag.backgroundTintList = ContextCompat.getColorStateList(
                    vRangeFlag.context,
                    item.getLeveColorRes()
                )

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
     * 胆固醇记录 ViewHolder
     * 使用 item_chol_history_record.xml 布局
     */
    inner class CholesterolViewHolder(
        private val binding: TrItemCholHistoryRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CholesterolHistoryItem) {
            with(binding) {
                // 设置三大核心指标（大字体显示）
                tvHdlValue.text = item.getHdlValue()
                tvLdlValue.text = item.getLdlValue()
                tvTgValue.text = item.getTgValue()

                // 设置三个计算指标（小字体显示）
                tvNonHdlValue.text = item.getNonHdlValue()
                tvTcHdlValue.text = item.getTcHdlRatioValue()
                tvLdlHdlValue.text = item.getLdlHdlRatioValue()

                // 设置等级显示（根据风险等级设置文字颜色）
                tvLeve.text = item.getLevel(tvLeve.context)
                tvLeve.setTextColor(
                    ContextCompat.getColor(tvLeve.context, item.getLeveColorRes())
                )

                // 设置记录时间
                tvRecordTime.text = DateTimeUtils.formatDateTime(item.getRecordTime())

                // 设置点击事件
                root.setOnClickListener {
                    itemClickListener?.onItemClick(item, adapterPosition)
                }

                // 设置删除按钮
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
            // 基础字段比较
            val baseEquals = oldItem.getPrimaryValue() == newItem.getPrimaryValue() &&
                             oldItem.getSecondaryValue() == newItem.getSecondaryValue() &&
                             oldItem.getRecordTime() == newItem.getRecordTime() &&
                             oldItem.getLeveColorRes() == newItem.getLeveColorRes()

            if (!baseEquals) return false

            // 胆固醇记录需要额外比较 6 个指标
            if (oldItem is CholesterolHistoryItem && newItem is CholesterolHistoryItem) {
                return oldItem.getHdlValue() == newItem.getHdlValue() &&
                       oldItem.getLdlValue() == newItem.getLdlValue() &&
                       oldItem.getTgValue() == newItem.getTgValue() &&
                       oldItem.getNonHdlValue() == newItem.getNonHdlValue() &&
                       oldItem.getTcHdlRatioValue() == newItem.getTcHdlRatioValue() &&
                       oldItem.getLdlHdlRatioValue() == newItem.getLdlHdlRatioValue()
            }

            return true
        }
    }
}
