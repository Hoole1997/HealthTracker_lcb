package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.LeveStatusViewBinding
import com.healthtracker.blood.suger.getUserAge
import com.healthtracker.blood.suger.isMale
import com.healthtracker.framework.ext.clickWithDuration

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
    /** 外部控制的说明点击开关与回调（默认跟随 isAddRecordScene）*/
    private var explainClickable: Boolean = false
    private var onExplainClick: (() -> Unit)? = null

    /** 等级列表与当前索引（0-based） */
    private var levels: List<LevelItem> = emptyList()
    private var currentIndex: Int = 0

    // 不再耦合具体分类，进度条仅使用颜色数组与索引

    init {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.LeveStatusView) {
                isAddRecordScene = getBoolean(R.styleable.LeveStatusView_lsvAddRecord, false)
                explainClickable = isAddRecordScene
                // 支持通过自定义属性设置范围文字颜色
                val defaultRangeColor = binding.rangeText.currentTextColor
                val customRangeColor = getColor(
                    R.styleable.LeveStatusView_lsvRangeTextColor,
                    defaultRangeColor
                )
                binding.rangeText.setTextColor(customRangeColor)
            }
        }
        // 初始化点击行为（仅在添加记录场景有效）
        setupRangeClickIfNeeded()
        updateRangeIconVisibility()
        refreshUI()
        setupUserProfile()
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
        updateRangeIconVisibility()

        // 配置并更新通用等级进度条（颜色数组 + 索引）
        val colors = levels.map { it.colorInt }.toIntArray()
        binding.levelBar.setColors(colors)
        binding.levelBar.setIndicatorIndex(currentIndex)
    }

    /** 设置范围文本点击（由外部控制是否可点击与回调） */
    private fun setupRangeClickIfNeeded() {
        val canClick = explainClickable && onExplainClick != null
        binding.rangeText.isClickable = canClick
        binding.rangeText.isEnabled = canClick
        if (canClick) {
            binding.rangeText.clickWithDuration { onExplainClick?.invoke() }
        } else {
            binding.rangeText.setOnClickListener(null)
        }
    }

    /** 根据场景切换范围文本的尾部图标显示 */
    private fun updateRangeIconVisibility() {
        val showIcon = explainClickable && onExplainClick != null
        if (showIcon) {
            binding.rangeText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, R.drawable.ic_blood_detail, 0
            )
        } else {
            binding.rangeText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
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

    /** 外部控制 API：设置说明点击回调 */
    fun setOnExplainClick(listener: (() -> Unit)?) {
        this.onExplainClick = listener
        setupRangeClickIfNeeded()
        updateRangeIconVisibility()
    }

    /** 外部控制 API：设置范围说明是否可点击 */
    fun setExplainClickable(clickable: Boolean) {
        this.explainClickable = clickable
        setupRangeClickIfNeeded()
        updateRangeIconVisibility()
    }

    private fun setupUserProfile(){
        with(binding){
            tvGender.text = context.getString(if(isMale()) R.string.male else R.string.female)
            tvAge.text = context.getString(R.string.temp_age,getUserAge().toString())
        }
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

// 顶层扩展函数已移除，避免访问私有成员导致的编译错误