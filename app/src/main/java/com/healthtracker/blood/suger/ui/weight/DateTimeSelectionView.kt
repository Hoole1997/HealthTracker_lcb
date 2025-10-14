package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.LayoutDatetimeSelectionBinding
import java.util.*
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.framework.ext.clickWithDuration

/**
 * 时间选择复合组件
 * 封装了日期时间选择功能，支持自定义标题和标签按钮
 */
class DateTimeSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // 加载布局
    private val binding: LayoutDatetimeSelectionBinding = LayoutDatetimeSelectionBinding.inflate(
        LayoutInflater.from(context), this, true
    )


    // 自定义属性
    private var titleText: String = ""
    private var labelText: String = ""
    private var showLabel: Boolean = true

    init {

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
        binding.tvLabel.clickWithDuration {
            onLabelClickListener?.invoke()
        }
        
        // 设置时间变化监听器
        binding.dateTimePicker.setOnValueChangeListener { picker, oldVal, newVal ->
            val dateTime = binding.dateTimePicker.getDateTime()
            onDateTimeSelectedListener?.invoke(dateTime.toCalendar())
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
    fun setOnDateTimeSelectedListener(listener: ((Calendar) -> Unit)?) {
        onDateTimeSelectedListener = listener
    }

    /**
     * 设置标签点击监听器
     */
    fun setOnLabelClickListener(listener: (() -> Unit)?) {
        onLabelClickListener = listener
    }

    // 使用lambda表达式替代接口
    private var onDateTimeSelectedListener: ((Calendar) -> Unit)? = null
    private var onLabelClickListener: (() -> Unit)? = null


    fun getSelectDate(): Date {
        // 获取选择的日期时间
        val selectedDateTime = getDateTimePicker().getDateTime()
        val selectedCalendar = selectedDateTime.toCalendar()
        
        // 使用DateTimeUtils统一处理日期，保持时间准确性
        val finalDateTime = DateTimeUtils.createDate(
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH) + 1, // Calendar月份从0开始，需要+1
            selectedCalendar.get(Calendar.DAY_OF_MONTH),
            selectedCalendar.get(Calendar.HOUR_OF_DAY),
            selectedCalendar.get(Calendar.MINUTE)
        )
        
        return finalDateTime
    }
}