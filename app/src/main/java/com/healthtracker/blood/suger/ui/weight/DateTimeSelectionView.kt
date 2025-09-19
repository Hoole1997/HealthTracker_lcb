package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.LayoutDatetimeSelectionBinding
import java.util.*

/**
 * 时间选择复合组件
 * 封装了日期时间选择功能，支持自定义标题和标签按钮
 */
class DateTimeSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutDatetimeSelectionBinding
    private var onDateTimeSelectedListener: OnDateTimeSelectedListener? = null
    private var onLabelClickListener: OnLabelClickListener? = null

    // 自定义属性
    private var titleText: String = ""
    private var labelText: String = ""
    private var showLabel: Boolean = true

    init {
        // 加载布局
        binding = LayoutDatetimeSelectionBinding.inflate(
            LayoutInflater.from(context), this, true
        )
        
        // 解析自定义属性
        initAttributes(attrs)
        
        // 初始化UI
        initViews()
        
        // 设置监听器
        setupListeners()
    }

    /**
     * 解析自定义属性
     */
    private fun initAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.DateTimeSelectionView)
            try {
                titleText = typedArray.getString(R.styleable.DateTimeSelectionView_titleText) 
                    ?: context.getString(R.string.date_time)
                labelText = typedArray.getString(R.styleable.DateTimeSelectionView_labelText) 
                    ?: context.getString(R.string.label)
                showLabel = typedArray.getBoolean(R.styleable.DateTimeSelectionView_showLabel, true)
            } finally {
                typedArray.recycle()
            }
        }
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        // 设置标题文本
        binding.tvDate.text = titleText
        
        // 设置标签文本和可见性
        binding.tvLabel.text = labelText
        binding.tvLabel.visibility = if (showLabel) VISIBLE else GONE
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 标签点击监听
        binding.tvLabel.setOnClickListener {
            onLabelClickListener?.onLabelClick()
        }
        
        // 设置时间变化监听器
        binding.dateTimePicker.setOnValueChangeListener { picker, oldVal, newVal ->
            val dateTime = binding.dateTimePicker.getDateTime()
            onDateTimeSelectedListener?.onDateTimeSelected(dateTime.toCalendar())
        }
    }

    /**
     * 设置标题文本
     */
    fun setTitleText(title: String) {
        titleText = title
        binding.tvDate.text = title
    }

    /**
     * 设置标签文本
     */
    fun setLabelText(label: String) {
        labelText = label
        binding.tvLabel.text = label
    }

    /**
     * 设置标签可见性
     */
    fun setLabelVisible(visible: Boolean) {
        showLabel = visible
        binding.tvLabel.visibility = if (visible) VISIBLE else GONE
    }

    /**
     * 获取DateTimePicker实例
     */
    fun getDateTimePicker(): DateTimePicker {
        return binding.dateTimePicker
    }

    /**
     * 设置当前时间
     */
    fun setCurrentTime(calendar: Calendar) {
        binding.dateTimePicker.initView(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE)
        )
    }

    /**
     * 获取当前选择的时间
     */
    fun getCurrentTime(): Calendar {
        return binding.dateTimePicker.getDateTime().toCalendar()
    }

    /**
     * 设置时间选择监听器
     */
    fun setOnDateTimeSelectedListener(listener: OnDateTimeSelectedListener?) {
        this.onDateTimeSelectedListener = listener
    }

    /**
     * 设置标签点击监听器
     */
    fun setOnLabelClickListener(listener: OnLabelClickListener?) {
        this.onLabelClickListener = listener
    }

    /**
     * 时间选择监听接口
     */
    interface OnDateTimeSelectedListener {
        fun onDateTimeSelected(calendar: Calendar)
    }

    /**
     * 标签点击监听接口
     */
    interface OnLabelClickListener {
        fun onLabelClick()
    }
}