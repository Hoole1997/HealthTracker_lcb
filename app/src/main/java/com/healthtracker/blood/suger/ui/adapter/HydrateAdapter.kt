package com.healthtracker.blood.suger.ui.adapter

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.compose.ui.text.intl.Locale
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ItemHydrateQuickAddSectionBinding
import com.healthtracker.blood.suger.databinding.ItemHydrateRecordItemBinding
import com.healthtracker.blood.suger.databinding.ItemHydrateRecordSectionBinding
import com.healthtracker.blood.suger.databinding.ItemHydrateTotalSectionBinding
import com.healthtracker.blood.suger.databinding.ItemLabelBinding
import java.util.Date
import kotlin.math.max

/**
 * 喝水页面 RecyclerView 适配器
 * - 目前仅实现第一个部分：总饮水量与水杯视图
 * - 后续可扩展其他部分为不同的 ViewType
 */
class HydrateAdapter(
    private val onQuickAddClick: (Int) -> Unit = {},
    private val onRecordDeleteClick: (HydrateRecordItem) -> Unit = {},
    private val onDrinkClick: (Int) -> Unit = {}
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

    // 适配器级别维护当前选择的饮水量，避免 ViewHolder 重建后丢失状态
    private var currentDrinkAmountMl: Int = 100

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is HydrateItem.TotalSection -> TYPE_TOTAL_SECTION
        is HydrateItem.QuickAddSection -> TYPE_QUICK_ADD_SECTION
        is HydrateItem.RecordSection -> TYPE_RECORD_SECTION
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TOTAL_SECTION -> TotalSectionViewHolder(
                ItemHydrateTotalSectionBinding.inflate(inflater, parent, false),
                onDrinkClick
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

    inner class TotalSectionViewHolder(
        private val binding: ItemHydrateTotalSectionBinding,
        private val onDrinkClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val drinkTextFormat: String = binding.root.context.getString(R.string.drink_btn_format)

        init {
            binding.drinkMore.setOnClickListener {
                currentDrinkAmountMl += 10
                binding.drinkBtn.text = String.format(drinkTextFormat, currentDrinkAmountMl)
            }
            binding.drinkLess.setOnClickListener {
                currentDrinkAmountMl = max(10, currentDrinkAmountMl - 10)
                binding.drinkBtn.text = String.format(drinkTextFormat, currentDrinkAmountMl)
            }
            binding.drinkBtn.setOnClickListener {
                onDrinkClick(currentDrinkAmountMl)
            }
        }
        fun bind(item: HydrateItem.TotalSection) {
            binding.apply {
                totalWaterIntake.text = item.totalIntake.toString()
                totalWaterUnit.text = item.unit
                totalWaterDesc.text = item.description

                // 根据当前选择的饮水量更新按钮文案
                drinkBtn.text = String.format(drinkTextFormat, currentDrinkAmountMl)

                // 更新水杯视图：以毫升驱动，最小单位 10ml
                // 目标毫升按照 1杯 = 250ml 计算（保持与业务“8杯=2000ml”一致）
                val targetMl = item.maxCups * 250
                waterCupView.setStepMl(10)
                waterCupView.setTargetMl(targetMl)
                waterCupView.setCurrentMl(item.totalIntake)
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
                binding.rvQuickAdd.addItemDecoration(EndSpacingItemDecoration(16))
            }
            adapter?.submitList(item.values)
        }
    }

    private class EndSpacingItemDecoration(private val endDp: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            val lastIndex = (parent.adapter?.itemCount ?: 0) - 1
            if (position != RecyclerView.NO_POSITION && position == lastIndex) {
                outRect.right = (endDp * view.context.resources.displayMetrics.density + 0.5f).toInt()
            }
        }
    }

    class RecordSectionViewHolder(
        private val binding: ItemHydrateRecordSectionBinding,
        private val onDeleteClick: (HydrateRecordItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var adapter: RecordAdapter? = null
        private var emptyView: TextView? = null

        fun bind(item: HydrateItem.RecordSection) {
            if (adapter == null) {
                adapter = RecordAdapter(onDeleteClick)
                binding.rvRecords.layoutManager = LinearLayoutManager(binding.root.context)
                binding.rvRecords.adapter = adapter
            }
            if (item.records.isEmpty()) {
                if (emptyView == null) {
                    emptyView = TextView(binding.root.context).apply {
                        val emptyHint = binding.root.context.getString(R.string.hydrate_empty_record)
                        text = emptyHint
                        setTextColor("#999999".toColorInt())
                        textSize = 14f
                        gravity = Gravity.CENTER
                        setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
                        background = GradientDrawable().apply {
                            setColor(Color.TRANSPARENT)
                            setStroke(dpToPx(1), "#F5F5F5".toColorInt())
                            cornerRadius = dpToPx(8).toFloat()
                        }
                        layoutParams = LinearLayoutCompat.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = dpToPx(12)
                            bottomMargin = dpToPx(24)
                        }
                    }
                    val root = binding.root
                    root.addView(emptyView)
                }
                emptyView?.visibility = View.VISIBLE
                binding.rvRecords.visibility = View.GONE
                adapter?.submitList(emptyList())
            } else {
                emptyView?.visibility = View.GONE
                binding.rvRecords.visibility = View.VISIBLE
                adapter?.submitList(item.records)
            }
        }

        private fun dpToPx(dp: Int): Int {
            val density = binding.root.context.resources.displayMetrics.density
            return (dp * density + 0.5f).toInt()
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
            binding.tvLabel.text = String.format(java.util.Locale.getDefault(), "%d ML", value)
            binding.root.setOnClickListener { onItemClick(value) }
        }
    }

    private class RecordAdapter(
        private val onDeleteClick: (HydrateRecordItem) -> Unit
    ) : ListAdapter<HydrateRecordItem, RecordViewHolder>(object : DiffUtil.ItemCallback<HydrateRecordItem>() {
        override fun areItemsTheSame(oldItem: HydrateRecordItem, newItem: HydrateRecordItem): Boolean {
            return oldItem.id == newItem.id
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
    val id: Long,
    val intakeMl: Int,
    val date: Date
)