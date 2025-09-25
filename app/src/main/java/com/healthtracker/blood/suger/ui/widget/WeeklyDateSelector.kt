package com.healthtracker.blood.suger.ui.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import java.util.*

/**
 * WeeklyDateSelector - 简化版本
 * 基本的周日期选择器，支持左右翻页、选中日期、基本的滑动限制
 */
class WeeklyDateSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val inflater = LayoutInflater.from(context)
    private val root: View = inflater.inflate(R.layout.layout_weekly_date_selector, this, true)
    private val viewPager: ViewPager2 = root.findViewById(R.id.viewPager)

    // 自定义属性
    private var selectedBackgroundColor: Int = Color.parseColor("#05BA7B")
    private var unselectedTextColor: Int = Color.parseColor("#666666")
    private var selectedTextColor: Int = Color.WHITE
    private var selectedWeekTextColor: Int = selectedBackgroundColor
    private var weekStartOnMonday: Boolean = true
    private var horizontalPadding: Int = 0
    private var customWeekdayNames: Array<String>? = null
    
    // 基本的滑动限制属性
    private var maxPreviousWeeks: Int = -1  // -1 表示无限制
    private var maxNextWeeks: Int = -1      // -1 表示无限制
    private var restrictPastWeekNavigation: Boolean = false
    private var disablePastDates: Boolean = false  // 是否禁用过去的日期选择

    private var onDateChangeListener: ((date: Date, isToday: Boolean) -> Unit)? = null

    // 状态
    private var today: Date = DateTimeUtils.now()
    private var selectedDate: Date = today
    private var currentWeekMonday: Date = getWeekStart(today)

    // ViewPager2 适配器
    private val weekAdapter = WeekAdapter()

    init {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.WeeklyDateSelector)
        try {
            selectedBackgroundColor = typedArray.getColor(R.styleable.WeeklyDateSelector_selectedBackgroundColor, selectedBackgroundColor)
            selectedTextColor = typedArray.getColor(R.styleable.WeeklyDateSelector_selectedTextColor, selectedTextColor)
            unselectedTextColor = typedArray.getColor(R.styleable.WeeklyDateSelector_unselectedTextColor, unselectedTextColor)
            selectedWeekTextColor = typedArray.getColor(R.styleable.WeeklyDateSelector_selectedWeekTextColor, selectedBackgroundColor)
            
            weekStartOnMonday = typedArray.getBoolean(R.styleable.WeeklyDateSelector_weekStartOnMonday, true)
            restrictPastWeekNavigation = typedArray.getBoolean(R.styleable.WeeklyDateSelector_restrictPastWeekNavigation, false)
            disablePastDates = typedArray.getBoolean(R.styleable.WeeklyDateSelector_disablePastDates, false)
            
            horizontalPadding = typedArray.getDimensionPixelSize(R.styleable.WeeklyDateSelector_horizontalPadding, 0)
            
            val customWeekdayNamesResId = typedArray.getResourceId(R.styleable.WeeklyDateSelector_customWeekdayNames, 0)
            if (customWeekdayNamesResId != 0) {
                customWeekdayNames = resources.getStringArray(customWeekdayNamesResId)
            }
            
            maxPreviousWeeks = typedArray.getInt(R.styleable.WeeklyDateSelector_maxPreviousWeeks, -1)
            maxNextWeeks = typedArray.getInt(R.styleable.WeeklyDateSelector_maxNextWeeks, -1)
        } finally {
            typedArray.recycle()
        }

        setupViews()
        setupPager()
        notifySelection(selectedDate)
    }

    fun setDefaultSelectedDate(defaultDate: Date?) {
        if (defaultDate != null) {
            selectedDate = defaultDate
            currentWeekMonday = getWeekStart(selectedDate)
            
            // 重新设置ViewPager位置
            setupPager()
            notifySelection(selectedDate)
        }
    }

    private fun setupViews() {
        // 基本视图设置
    }

    private fun setupPager() {
        viewPager.adapter = weekAdapter
        viewPager.setCurrentItem(5000, false) // 设置到中间位置
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                val weekOffset = position - 5000
                val baseWeekStart = getWeekStart(today)
                
                // 简单的导航限制检查
                if (restrictPastWeekNavigation && weekOffset < 0) {
                    // 不允许进入过去的周
                    viewPager.setCurrentItem(5000, false)
                    return
                }
                
                if (maxPreviousWeeks > 0 && weekOffset < -maxPreviousWeeks) {
                    // 超出最大过去周数限制
                    viewPager.setCurrentItem(5000 - maxPreviousWeeks, false)
                    return
                }
                
                if (maxNextWeeks > 0 && weekOffset > maxNextWeeks) {
                    // 超出最大未来周数限制
                    viewPager.setCurrentItem(5000 + maxNextWeeks, false)
                    return
                }
                
                currentWeekMonday = addDays(baseWeekStart, weekOffset * 7)
                weekAdapter.notifyDataSetChanged()
            }
        })
    }

    private inner class WeekAdapter : RecyclerView.Adapter<WeekViewHolder>() {
        override fun getItemCount(): Int = 10000 // 足够大的数量

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                // ViewPager2要求子视图必须填满整个ViewPager2的宽度和高度
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // 将horizontalPadding应用到父容器上
                setPadding(horizontalPadding, 0, horizontalPadding, 0)
            }
            return WeekViewHolder(container)
        }

        override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
            val weekOffset = position - 5000
            val baseWeekStart = getWeekStart(today)
            val weekStartDate = addDays(baseWeekStart, weekOffset * 7)
            holder.bind(weekStartDate)
        }
    }

    private inner class WeekViewHolder(private val container: LinearLayout) : RecyclerView.ViewHolder(container) {

        fun bind(weekStartDate: Date) {
            container.removeAllViews()
            
            for (i in 0..6) {
                val dayDate = addDays(weekStartDate, i)
                val dayView = inflater.inflate(R.layout.item_week_day, null)
                
                val tvDayNumber: TextView = dayView.findViewById(R.id.tvDayNumber)
                val tvWeekName: TextView = dayView.findViewById(R.id.tvWeekName)
                
                // 设置日期数字
                val calendar = Calendar.getInstance()
                calendar.time = dayDate
                tvDayNumber.text = calendar.get(Calendar.DAY_OF_MONTH).toString()
                
                // 设置星期名称
                tvWeekName.text = getWeekAbbr(dayDate)
                
                // 检查是否为选中日期
                val isSelected = DateTimeUtils.isSameDay(dayDate, selectedDate)
                val isToday = DateTimeUtils.isSameDay(dayDate, today)
                val isPastDate = disablePastDates && dayDate.before(today)
                
                if (isSelected) {
                    // 背景只设置在日期数字上
                    tvDayNumber.setBackgroundResource(R.drawable.bg_week_day_selected)
                    tvDayNumber.setTextColor(selectedTextColor)
                    tvWeekName.setTextColor(selectedWeekTextColor)
                } else if (isPastDate) {
                    // 过去日期的禁用样式
                    tvDayNumber.setBackgroundResource(R.drawable.bg_week_day_normal)
                    tvDayNumber.setTextColor(Color.parseColor("#CCCCCC"))  // 浅灰色表示禁用
                    tvWeekName.setTextColor(Color.parseColor("#CCCCCC"))
                    tvDayNumber.alpha = 0.5f  // 设置透明度
                    tvWeekName.alpha = 0.5f
                } else {
                    // 背景只设置在日期数字上
                    tvDayNumber.setBackgroundResource(R.drawable.bg_week_day_normal)
                    tvDayNumber.setTextColor(unselectedTextColor)
                    tvWeekName.setTextColor(unselectedTextColor)
                    tvDayNumber.alpha = 1.0f  // 确保正常日期的透明度为1
                    tvWeekName.alpha = 1.0f
                }
                
                // 设置点击事件
                dayView.setOnClickListener {
                    // 检查是否禁用过去日期
                    if (disablePastDates && dayDate.before(today)) {
                        // 如果禁用过去日期且当前日期是过去的日期，则不响应点击
                        return@setOnClickListener
                    }
                    
                    selectedDate = dayDate
                    notifySelection(selectedDate)
                    weekAdapter.notifyDataSetChanged()
                }
                
                // 设置布局参数
                val layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                // 移除margin设置，因为padding已经应用到父容器上
                dayView.layoutParams = layoutParams
                
                container.addView(dayView)
            }
        }
    }

    private fun notifySelection(date: Date) {
        val isToday = DateTimeUtils.isSameDay(date, today)
        onDateChangeListener?.invoke(date, isToday)
    }

    private fun getWeekStart(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToSubtract = if (weekStartOnMonday) {
            if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        } else {
            dayOfWeek - Calendar.SUNDAY
        }
        
        calendar.add(Calendar.DAY_OF_MONTH, -daysToSubtract)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.time
    }

    private fun getWeekAbbr(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        return if (customWeekdayNames != null && customWeekdayNames!!.size >= 7) {
            val index = if (weekStartOnMonday) {
                when (dayOfWeek) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
            } else {
                dayOfWeek - 1
            }
            customWeekdayNames!![index]
        } else {
            // 默认星期缩写
            when (dayOfWeek) {
                Calendar.SUNDAY -> "日"
                Calendar.MONDAY -> "一"
                Calendar.TUESDAY -> "二"
                Calendar.WEDNESDAY -> "三"
                Calendar.THURSDAY -> "四"
                Calendar.FRIDAY -> "五"
                Calendar.SATURDAY -> "六"
                else -> ""
            }
        }
    }

    private fun addDays(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.time
    }

    // 公共API方法
    fun setSelectedDate(date: Date) {
        selectedDate = date
        currentWeekMonday = getWeekStart(date)
        weekAdapter.notifyDataSetChanged()
        notifySelection(selectedDate)
    }

    fun resetToToday() {
        today = DateTimeUtils.now()
        selectedDate = today
        currentWeekMonday = getWeekStart(today)
        viewPager.setCurrentItem(5000, false)
        weekAdapter.notifyDataSetChanged()
        notifySelection(selectedDate)
    }

    fun nextWeek() {
        val nextWeekOffset = 1
        if (maxNextWeeks <= 0 || nextWeekOffset <= maxNextWeeks) {
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        }
    }

    fun prevWeek() {
        val prevWeekOffset = -1
        if (!restrictPastWeekNavigation && (maxPreviousWeeks <= 0 || -prevWeekOffset <= maxPreviousWeeks)) {
            viewPager.setCurrentItem(viewPager.currentItem - 1, true)
        }
    }

    fun setOnDateChangeListener(listener: (date: Date, isToday: Boolean) -> Unit) {
        this.onDateChangeListener = listener
    }

    fun setMaxPreviousWeeks(maxWeeks: Int) {
        this.maxPreviousWeeks = maxWeeks
    }

    fun setMaxNextWeeks(maxWeeks: Int) {
        this.maxNextWeeks = maxWeeks
    }

    fun setRestrictPastWeekNavigation(restrict: Boolean) {
        this.restrictPastWeekNavigation = restrict
    }
}