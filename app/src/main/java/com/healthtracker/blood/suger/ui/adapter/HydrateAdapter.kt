package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.databinding.ItemHydrateQuickAddSectionBinding
import com.healthtracker.blood.suger.databinding.ItemHydrateRecordItemBinding
import com.healthtracker.blood.suger.databinding.ItemHydrateRecordSectionBinding
import com.healthtracker.blood.suger.databinding.ItemHydrateTotalSectionBinding
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ItemLabelBinding
import java.util.Date

/**
 * 喝水页面 RecyclerView 适配器
 * - 目前仅实现第一个部分：总饮水量与水杯视图
 * - 后续可扩展其他部分为不同的 ViewType
 */
class HydrateAdapter(
    private val onQuickAddClick: (Int) -> Unit = {},
    private val onRecordDeleteClick: (HydrateRecordItem) -> Unit = {}
) : ListAdapter<HydrateItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_TOTAL_SECTION = 1
        private const val TYPE_QUICK_ADD_SECTION = 2
        private const val TYPE_RECORD_SECTION = 3

        private val DIFF = object : DiffUtil.ItemCallback<HydrateItem>() {
            override fun areItemsTheSame(oldItem: HydrateItem, newItem: HydrateItem): Boolean {
                return oldItem::class == newItem::class
            }

            override fun areContentsTheSame(oldItem: HydrateItem, newItem: HydrateItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HydrateItem.TotalSection -> TYPE_TOTAL_SECTION
        is HydrateItem.QuickAddSection -> TYPE_QUICK_ADD_SECTION
        is HydrateItem.RecordSection -> TYPE_RECORD_SECTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TOTAL_SECTION -> TotalSectionViewHolder(
                ItemHydrateTotalSectionBinding.inflate(inflater, parent, false)
            )
            TYPE_QUICK_ADD_SECTION -> QuickAddSectionViewHolder(
                ItemHydrateQuickAddSectionBinding.inflate(inflater, parent, false),
                onQuickAddClick
            )
            TYPE_RECORD_SECTION -> RecordSectionViewHolder(
                ItemHydrateRecordSectionBinding.inflate(inflater, parent, false),
                onRecordDeleteClick
            )
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TotalSectionViewHolder -> holder.bind(getItem(position) as HydrateItem.TotalSection)
            is QuickAddSectionViewHolder -> holder.bind(getItem(position) as HydrateItem.QuickAddSection)
            is RecordSectionViewHolder -> holder.bind(getItem(position) as HydrateItem.RecordSection)
        }
    }

    class TotalSectionViewHolder(private val binding: ItemHydrateTotalSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HydrateItem.TotalSection) {
            binding.apply {
                totalWaterIntake.text = item.totalIntake.toString()
                totalWaterUnit.text = item.unit
                totalWaterDesc.text = item.description

                // 更新水杯视图
                waterCupView.setMaxCups(item.maxCups)
                waterCupView.setCurrentCups(item.currentCups)
            }
        }
    }

    class QuickAddSectionViewHolder(
        private val binding: ItemHydrateQuickAddSectionBinding,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var adapter: QuickAddAdapter? = null

        fun bind(item: HydrateItem.QuickAddSection) {
            if (adapter == null) {
                adapter = QuickAddAdapter(onItemClick)
                binding.rvQuickAdd.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
                binding.rvQuickAdd.adapter = adapter
            }
            adapter?.submitList(item.values)
        }
    }

    class RecordSectionViewHolder(
        private val binding: ItemHydrateRecordSectionBinding,
        private val onDeleteClick: (HydrateRecordItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var adapter: RecordAdapter? = null

        fun bind(item: HydrateItem.RecordSection) {
            if (adapter == null) {
                adapter = RecordAdapter(onDeleteClick)
                binding.rvRecords.layoutManager = LinearLayoutManager(binding.root.context)
                binding.rvRecords.adapter = adapter
            }
            adapter?.submitList(item.records)
        }
    }

    private class QuickAddAdapter(
        private val onItemClick: (Int) -> Unit
    ) : ListAdapter<Int, QuickAddViewHolder>(object : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean = oldItem == newItem
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickAddViewHolder {
            val binding = ItemLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return QuickAddViewHolder(binding, onItemClick)
        }

        override fun onBindViewHolder(holder: QuickAddViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class QuickAddViewHolder(
        private val binding: ItemLabelBinding,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(value: Int) {
            binding.tvLabel.text = "$value ML"
            binding.root.setOnClickListener { onItemClick(value) }
        }
    }

    private class RecordAdapter(
        private val onDeleteClick: (HydrateRecordItem) -> Unit
    ) : ListAdapter<HydrateRecordItem, RecordViewHolder>(object : DiffUtil.ItemCallback<HydrateRecordItem>() {
        override fun areItemsTheSame(oldItem: HydrateRecordItem, newItem: HydrateRecordItem): Boolean {
            return oldItem.intakeMl == newItem.intakeMl && oldItem.date.time == newItem.date.time
        }

        override fun areContentsTheSame(oldItem: HydrateRecordItem, newItem: HydrateRecordItem): Boolean = oldItem == newItem
    }) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
            val binding = ItemHydrateRecordItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RecordViewHolder(binding, onDeleteClick)
        }

        override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class RecordViewHolder(
        private val binding: ItemHydrateRecordItemBinding,
        private val onDeleteClick: (HydrateRecordItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HydrateRecordItem) {
            binding.tvAmount.text = item.intakeMl.toString()
            binding.tvUnit.text = "ML"
            binding.tvTime.text = DateTimeUtils.formatDateTimeWithSeconds(Date(item.date.time))
            binding.ivDelete.setOnClickListener { onDeleteClick(item) }
        }
    }
}

/**
 * UI 数据模型
 */
sealed class HydrateItem {
    data class TotalSection(
        val totalIntake: Int, // 总饮水量数值
        val unit: String,     // 单位，例如 "ML" 或 "CUPS"
        val description: String, // 描述文案
        val currentCups: Int, // 当前已喝的杯数
        val maxCups: Int      // 最大杯数目标
    ) : HydrateItem()

    data class QuickAddSection(
        val values: List<Int> // 预设的快捷添加毫升数，例如 [100,200,250,...]
    ) : HydrateItem()

    data class RecordSection(
        val records: List<HydrateRecordItem>
    ) : HydrateItem()
}

data class HydrateRecordItem(
    val intakeMl: Int,
    val date: Date
)