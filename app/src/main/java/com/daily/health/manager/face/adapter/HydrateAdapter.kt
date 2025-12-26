package com.daily.health.manager.face.adapter

import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daily.health.manager.R
import com.daily.health.manager.config.HydrateSettingManager
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.HtItemHydrateQuickAddSectionBinding
import com.daily.health.manager.databinding.HtItemHydrateRecordItemBinding
import com.daily.health.manager.databinding.HtItemHydrateRecordSectionBinding
import com.daily.health.manager.databinding.HtItemLabelBinding
import java.util.Date

/**
 * 喝水页面 RecyclerView 适配器
 * - 仅处理 QuickAddSection 和 RecordSection
 * - TotalSection 已移至布局直接绑定
 */
class HydrateAdapter(
    private val onQuickAddClick: (Int) -> Unit = {},
    private val onRecordDeleteClick: (HydrateRecordItem) -> Unit = {}
) : ListAdapter<HydrateItem, RecyclerView.ViewHolder>(DIFF) {

    companion object {
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
        is HydrateItem.QuickAddSection -> TYPE_QUICK_ADD_SECTION
        is HydrateItem.RecordSection -> TYPE_RECORD_SECTION
        else -> throw IllegalArgumentException("Unsupported item type")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_QUICK_ADD_SECTION -> QuickAddSectionViewHolder(
                HtItemHydrateQuickAddSectionBinding.inflate(inflater, parent, false),
                onQuickAddClick
            )
            TYPE_RECORD_SECTION -> RecordSectionViewHolder(
                HtItemHydrateRecordSectionBinding.inflate(inflater, parent, false),
                onRecordDeleteClick
            )
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is QuickAddSectionViewHolder -> holder.bind(getItem(position) as HydrateItem.QuickAddSection)
            is RecordSectionViewHolder -> holder.bind(getItem(position) as HydrateItem.RecordSection)
        }
    }

    class QuickAddSectionViewHolder(
        private val binding: HtItemHydrateQuickAddSectionBinding,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var adapter: QuickAddAdapter? = null
        private var lastUnitLabel: String? = null

        fun bind(item: HydrateItem.QuickAddSection) {
            if (adapter == null) {
                adapter = QuickAddAdapter(onItemClick)
                binding.rvQuickAdd.layoutManager = LinearLayoutManager(binding.root.context, LinearLayoutManager.HORIZONTAL, false)
                binding.rvQuickAdd.adapter = adapter
                binding.rvQuickAdd.addItemDecoration(EndSpacingItemDecoration(16))
            }
            if (lastUnitLabel != item.unit) {
                // 单位发生变化时，刷新子适配器以使用新的单位显示
                adapter?.notifyDataSetChanged()
                lastUnitLabel = item.unit
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
        private val binding: HtItemHydrateRecordSectionBinding,
        private val onDeleteClick: (HydrateRecordItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private var adapter: RecordAdapter? = null
        private var emptyContainer: ConstraintLayout? = null
        private var lastUnitLabel: String? = null

        fun bind(item: HydrateItem.RecordSection) {
            if (adapter == null) {
                adapter = RecordAdapter(onDeleteClick)
                binding.rvRecords.layoutManager = LinearLayoutManager(binding.root.context)
                binding.rvRecords.adapter = adapter
            }
            if (lastUnitLabel != item.unit) {
                // 单位发生变化时刷新记录列表的展示单位
                adapter?.notifyDataSetChanged()
                lastUnitLabel = item.unit
            }
            if (item.records.isEmpty()) {
                if (emptyContainer == null) {
                    // 容器：约束布局，图片居中于父，文字水平居中且与图片底对齐
                    emptyContainer = ConstraintLayout(binding.root.context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dpToPx(158)
                        ).apply {
                            topMargin = dpToPx(12)
                            bottomMargin = dpToPx(12)
                        }
                        background = GradientDrawable().apply {
                            setColor(Color.WHITE)
                            cornerRadius = dpToPx(8).toFloat()
                        }
                    }

                    // 图片：居中于父
                    val iv = ImageView(binding.root.context).apply {
                        id = View.generateViewId()
                        setImageResource(R.mipmap.ht_ic_empty_hydrate_record)
                        adjustViewBounds = true
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                        contentDescription = binding.root.context.getString(R.string.ht_hydrate_empty_record)
                        layoutParams = ConstraintLayout.LayoutParams(
                            dpToPx(150),
                            dpToPx(120)
                        ).apply {
                            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                            bottomMargin = dpToPx(16)
                        }
                    }

                    val tv = TextView(binding.root.context).apply {
                        id = View.generateViewId()
                        val emptyHint = binding.root.context.getString(R.string.ht_hydrate_empty_record)
                        text = emptyHint
                        setTextColor("#999999".toColorInt())
                        textSize = 14f
                        gravity = Gravity.CENTER
                        setPadding(dpToPx(16), dpToPx(0), dpToPx(16), dpToPx(0))
                        layoutParams = ConstraintLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                            topToBottom = iv.id
                            topMargin = dpToPx(-20)
                        }
                    }

                    emptyContainer?.addView(iv)
                    emptyContainer?.addView(tv)

                    binding.root.addView(emptyContainer)
                }
                emptyContainer?.visibility = View.VISIBLE
                binding.rvRecords.visibility = View.GONE
                adapter?.submitList(emptyList())
            } else {
                emptyContainer?.visibility = View.GONE
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
            val binding = HtItemLabelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return QuickAddViewHolder(binding, onItemClick)
        }

        override fun onBindViewHolder(holder: QuickAddViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class QuickAddViewHolder(
        private val binding: HtItemLabelBinding,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(value: Int) {
            val cupUnit = HydrateSettingManager.getCupUnit()
            val unitText = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) binding.root.context.getString(R.string.ht_fl_oz) else binding.root.context.getString(R.string.ht_unit_ml)
            val displayValue = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) HydrateSettingManager.fromMl(value, HydrateSettingManager.CupUnit.FL_OZ) else value
            binding.tvLabel.text = displayValue.toString()
            binding.tvUnit.text = unitText
            // 点击回传始终使用毫升值以便数据库统一存储
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
            val binding = HtItemHydrateRecordItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return RecordViewHolder(binding, onDeleteClick)
        }

        override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private class RecordViewHolder(
        private val binding: HtItemHydrateRecordItemBinding,
        private val onDeleteClick: (HydrateRecordItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HydrateRecordItem) {
            val cupUnit = HydrateSettingManager.getCupUnit()
            val unitText = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) binding.root.context.getString(R.string.ht_fl_oz) else binding.root.context.getString(R.string.ht_unit_ml)
            val displayAmount = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) HydrateSettingManager.fromMl(item.intakeMl, HydrateSettingManager.CupUnit.FL_OZ) else item.intakeMl
            binding.tvAmount.text = displayAmount.toString()
            binding.tvUnit.text = unitText
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
        val totalIntake: Int,  // 总饮水量数值（ml）
        val targetMl: Int,     // 目标饮水量（ml）
        val unit: String,      // 单位标签，例如 "ML" 或 "fl oz"
        val description: String, // 描述文案
        val cupVolumeMl: Int   // 杯子容积（ml），用于 Drink 按钮每次饮水量
    ) : HydrateItem()

    data class QuickAddSection(
        val values: List<Int>, // 预设的快捷添加毫升数，例如 [100,200,250,...]
        val unit: String       // 当前单位标签，触发子适配器刷新
    ) : HydrateItem()

    data class RecordSection(
        val records: List<HydrateRecordItem>,
        val unit: String       // 当前单位标签，触发子适配器刷新
    ) : HydrateItem()
}

data class HydrateRecordItem(
    val id: Long,
    val intakeMl: Int,
    val date: Date
)