package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.LeveStatusViewBinding
import com.healthtracker.blood.suger.ui.act.BpRecordActivity
import com.healthtracker.blood.suger.ui.dialog.BpLeveDialog
import com.healthtracker.framework.ext.clickWithDuration
import androidx.core.content.withStyledAttributes

/**
 * 通用等级状态视图（对齐 BloodPressureStatusView 的展示与交互）
 *
 * 能力：
 * - 展示圆点（颜色随当前等级）与状态文案、范围短文案
 * - 在“添加记录”场景（lsvAddRecord=true）时，范围文案可点击，弹出等级说明弹窗（沿用 BpLeveDialog）
 * - 保留 setLevels(List<LevelItem>) / setCurrentLevel(index) 的最小API
 */
class LeveStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LeveStatusViewBinding =
        LeveStatusViewBinding.inflate(LayoutInflater.from(context), this, true)

    /** 是否为“添加记录”场景（默认 false） */
    private var isAddRecordScene: Boolean = false

    /** 等级列表与当前索引（0-based） */
    private var levels: List<LevelItem> = emptyList()
    private var currentIndex: Int = 0

    // 不再耦合具体分类，进度条仅使用颜色数组与索引

    init {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.LeveStatusView) {
                isAddRecordScene = getBoolean(R.styleable.LeveStatusView_lsvAddRecord, false)
            }
        }
        // 初始化点击行为（仅在添加记录场景有效）
        setupRangeClickIfNeeded()
        refreshUI()
    }

    /** 设置等级列表（空列表将重置索引并隐藏组件） */
    fun setLevels(levels: List<LevelItem>) {
        this.levels = levels
        if (levels.isEmpty()) {
            currentIndex = 0
        } else if (currentIndex !in 0 until levels.size) {
            currentIndex = 0
        }
        refreshUI()
    }

    /** 设置当前索引（越界保护） */
    fun setCurrentLevel(index: Int) {
        currentIndex = if (levels.isEmpty()) 0 else index.coerceIn(0, levels.size - 1)
        refreshUI()
    }

    /** 暴露场景与数据 */
    fun isAddRecordScene(): Boolean = isAddRecordScene
    fun getLevels(): List<LevelItem> = levels
    fun getCurrentIndex(): Int = currentIndex

    /** 刷新UI展示（对齐 BloodPressureStatusView 的视觉与交互） */
    private fun refreshUI() {
        if (levels.isEmpty()) {
            visibility = View.GONE
            return
        }
        visibility = View.VISIBLE

        val item = levels[currentIndex]
        // 圆点颜色
        updateStatusDot(item.colorInt)
        // 状态文本
        binding.statusText.text = item.name
        binding.statusText.setTextColor(item.colorInt)
        // 范围短文案
        binding.rangeText.text = item.rangeDesc

        // 根据场景控制：圆点可见、说明弹窗点击
        binding.statusDot.visibility = if (isAddRecordScene) View.VISIBLE else View.GONE
        setupRangeClickIfNeeded()

        // 配置并更新通用等级进度条（颜色数组 + 索引）
        val colors = levels.map { it.colorInt }.toIntArray()
        binding.levelBar.setColors(colors)
        binding.levelBar.setIndicatorIndex(currentIndex)
    }

    /** 设置范围文本点击（添加记录场景下弹出说明） */
    private fun setupRangeClickIfNeeded() {
        binding.rangeText.isClickable = isAddRecordScene
        binding.rangeText.isEnabled = isAddRecordScene
        if (isAddRecordScene) {
            binding.rangeText.clickWithDuration {
                // 仅在血压记录页触发说明弹窗，避免泛用场景耦合异常
                (context as? BpRecordActivity)?.let { BpLeveDialog.show(it.supportFragmentManager) }
            }
        } else {
            binding.rangeText.setOnClickListener(null)
        }
    }

    /** 更新圆点背景为指定颜色 */
    private fun updateStatusDot(colorInt: Int) {
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(colorInt)
        }
        binding.statusDot.background = dot
    }
}

/**
 * 等级项数据契约
 */
data class LevelItem(
    val name: String,
    val rangeDesc: String,
    val colorInt: Int
)