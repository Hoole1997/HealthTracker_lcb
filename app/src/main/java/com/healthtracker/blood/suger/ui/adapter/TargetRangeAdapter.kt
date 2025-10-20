package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.enums.getStatusStringRes
import com.healthtracker.blood.suger.databinding.ItemTargetRangeBinding
import com.healthtracker.blood.suger.ui.viewmodel.RangeItem
import com.healthtracker.framework.ext.click

/**
 * 血糖目标范围列表适配器
 */
class TargetRangeAdapter(
    private val currentUnit: BsUnit,
    private val onItemClick: (RangeItem) -> Unit
) : ListAdapter<RangeItem, TargetRangeAdapter.ViewHolder>(RangeItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTargetRangeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTargetRangeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RangeItem) {
            with(binding) {
                // 设置状态名称
                tvLeveStatus.text = root.context.getString(getStatusStringRes(item.status.statusType))

                // 格式化范围值显示
                val unitText = if (currentUnit == BsUnit.MG_DL) "mg/dL" else "mmol/L"
                tvLeveLowValue.text = "< ${formatValue(item.ranges.lowHigh)} $unitText"
                tvLeveNormalValue.text = "${formatValue(item.ranges.lowHigh)} ~ ${formatValue(item.ranges.normalHigh)} $unitText"
                tvLevePreValue.text = "${formatValue(item.ranges.normalHigh)} ~ ${formatValue(item.ranges.prediabetesHigh)} $unitText"
                tvLeveDiaValue.text = "≥ ${formatValue(item.ranges.diabetesLow)} $unitText"

                tvLeveLowName.apply {
                    text = context.getString(R.string.blood_sugar_level_low)
                }
                tvLeveNormalName.apply {
                    text = context.getString(R.string.blood_sugar_level_normal)
                }

                tvLevePreName.apply {
                    text = context.getString(R.string.blood_sugar_level_prediabetes)
                }

                tvLeveLowName.apply {
                    text = context.getString(R.string.blood_sugar_level_diabetes)
                }

                // 点击事件
                root.click { onItemClick(item) }
            }
        }

        private fun formatValue(value: Float): String {
            return if (currentUnit == BsUnit.MG_DL) {
                value.toInt().toString()
            } else {
                String.format("%.1f", value)
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
