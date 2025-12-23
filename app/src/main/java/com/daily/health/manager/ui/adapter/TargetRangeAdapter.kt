package com.daily.health.manager.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daily.health.manager.R
import com.daily.health.manager.data.enums.BsUnit
import com.daily.health.manager.data.enums.getStatusStringRes
import com.daily.health.manager.databinding.HtItemTargetRangeBinding
import com.daily.health.manager.ui.viewmodel.RangeItem
import com.healthtracker.framework.ext.click

/**
 * 血糖目标范围列表适配器
 */
class TargetRangeAdapter(
    private val currentUnit: BsUnit,
    private val onItemClick: (RangeItem) -> Unit
) : ListAdapter<RangeItem, TargetRangeAdapter.ViewHolder>(RangeItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = HtItemTargetRangeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: HtItemTargetRangeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RangeItem) {
            with(binding) {
                // 设置状态名称
                tvLeveStatus.text = root.context.getString(getStatusStringRes(item.status.statusType))

                // 格式化范围值显示
                val unitText = if (currentUnit == BsUnit.MG_DL) "mg/dL" else "mmol/L"
                tvLeveLowValue.text = "< ${BsUnit.formatValue(item.ranges.lowHigh, currentUnit)} $unitText"
                tvLeveNormalValue.text = "${BsUnit.formatValue(item.ranges.lowHigh, currentUnit)} ~ ${BsUnit.formatValue(item.ranges.normalHigh, currentUnit)} $unitText"
                tvLevePreValue.text = "${BsUnit.formatValue(item.ranges.normalHigh, currentUnit)} ~ ${BsUnit.formatValue(item.ranges.prediabetesHigh, currentUnit)} $unitText"
                tvLeveDiaValue.text = "≥ ${BsUnit.formatValue(item.ranges.diabetesLow, currentUnit)} $unitText"

                tvLeveLowName.apply {
                    text = context.getString(R.string.ht_blood_sugar_level_low)
                }
                tvLeveNormalName.apply {
                    text = context.getString(R.string.ht_blood_sugar_level_normal)
                }

                tvLevePreName.apply {
                    text = context.getString(R.string.ht_blood_sugar_level_prediabetes)
                }

                tvLeveLowName.apply {
                    text = context.getString(R.string.ht_blood_sugar_level_diabetes)
                }

                // 点击事件
                root.click { onItemClick(item) }
            }
        }

    }
}

/**
 * DiffUtil 回调用于高效更新列表
 */
class RangeItemDiffCallback : DiffUtil.ItemCallback<RangeItem>() {
    override fun areItemsTheSame(oldItem: RangeItem, newItem: RangeItem): Boolean {
        return oldItem.status.statusType == newItem.status.statusType
    }

    override fun areContentsTheSame(oldItem: RangeItem, newItem: RangeItem): Boolean {
        return oldItem == newItem
    }
}
