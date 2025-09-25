package com.healthtracker.blood.suger.ui.widget

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.framework.ext.click
import java.util.Calendar
import java.util.Date

/**
 * 周枚举类，用于管理周字符串数组的索引值和名称
 */
enum class WeekDay(
    val calendarValue: Int,
    val englishName: String,
    val englishAbbr: String
) {
    SUNDAY(Calendar.SUNDAY, "Sunday", "Sun"),
    MONDAY(Calendar.MONDAY, "Monday", "Mon"),
    TUESDAY(Calendar.TUESDAY, "Tuesday", "Tue"),
    WEDNESDAY(Calendar.WEDNESDAY, "Wednesday", "Wed"),
    THURSDAY(Calendar.THURSDAY, "Thursday", "Thu"),
    FRIDAY(Calendar.FRIDAY, "Friday", "Fri"),
    SATURDAY(Calendar.SATURDAY, "Saturday", "Sat");

    companion object {
        /**
         * 根据Calendar的DAY_OF_WEEK值获取对应的WeekDay枚举
         */
        fun fromCalendarValue(calendarValue: Int): WeekDay {
            return values().find { it.calendarValue == calendarValue } ?: SUNDAY
        }

        /**
         * 获取按照指定起始日排序的周枚举数组
         * @param startOnMonday 是否从周一开始
         * @return 排序后的周枚举数组
         */
        fun getOrderedWeekDays(startOnMonday: Boolean): Array<WeekDay> {
            return if (startOnMonday) {
                arrayOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
            } else {
                arrayOf(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
            }
        }

        /**
         * 根据Calendar的DAY_OF_WEEK值和起始日设置获取在自定义数组中的索引
         * @param calendarValue Calendar的DAY_OF_WEEK值
         * @param startOnMonday 是否从周一开始
         * @return 在自定义数组中的索引
         */
        fun getCustomArrayIndex(calendarValue: Int, startOnMonday: Boolean): Int {
            val weekDay = fromCalendarValue(calendarValue)
            val orderedWeekDays = getOrderedWeekDays(startOnMonday)
            return orderedWeekDays.indexOf(weekDay).takeIf { it >= 0 } ?: 0
        }
    }
}

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
    
    // 数据集大小控制属性
    private var previousWeeksCount: Int = -1  // -1 表示无限制，0 表示仅当前周
    private var nextWeeksCount: Int = -1      // -1 表示无限制，0 表示仅当前周
    private var disablePastDates: Boolean = false  // 是否禁用过去的日期选择

    // 分离的回调函数
    private var onDateSelectedListener: ((date: Date) -> Unit)? = null
    private var onWeekChangedListener: ((isCurrentWeek: Boolean) -> Unit)? = null

    // 状态
    private var today: Date = DateTimeUtils.now()
    private var selectedDate: Date = today
    private var currentWeekMonday: Date = getWeekStart(today)

    // ViewPager2 适配器
    private val weekAdapter = WeekAdapter()
    
    // 触摸事件处理相关变量
    private var initialX = 0f
    private var initialY = 0f
    private var isHorizontalScroll = false
    private var hasIntercepted = false

    init {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.WeeklyDateSelector)
        try {
            selectedBackgroundColor = typedArray.getColor(R.styleable.WeeklyDateSelector_selectedBackgroundColor, selectedBackgroundColor)
            selectedTextColor = typedArray.getColor(R.styleable.WeeklyDateSelector_selectedTextColor, selectedTextColor)
            unselectedTextColor = typedArray.getColor(R.styleable.WeeklyDateSelector_unselectedTextColor, unselectedTextColor)
            selectedWeekTextColor = typedArray.getColor(R.styleable.WeeklyDateSelector_selectedWeekTextColor, selectedBackgroundColor)
            
            weekStartOnMonday = typedArray.getBoolean(R.styleable.WeeklyDateSelector_weekStartOnMonday, true)
            disablePastDates = typedArray.getBoolean(R.styleable.WeeklyDateSelector_disablePastDates, false)
            
            horizontalPadding = typedArray.getDimensionPixelSize(R.styleable.WeeklyDateSelector_horizontalPadding, 0)
            
            val customWeekdayNamesResId = typedArray.getResourceId(R.styleable.WeeklyDateSelector_customWeekdayNames, 0)
            if (customWeekdayNamesResId != 0) {
                customWeekdayNames = resources.getStringArray(customWeekdayNamesResId)
            }
            
            previousWeeksCount = typedArray.getInt(R.styleable.WeeklyDateSelector_previousWeeksCount, -1)
            nextWeeksCount = typedArray.getInt(R.styleable.WeeklyDateSelector_nextWeeksCount, -1)
        } finally {
            typedArray.recycle()
        }

        setupViews()
        setupPager()
        notifyDateSelected(selectedDate)
    }

    fun setDefaultSelectedDate(defaultDate: Date = DateTimeUtils.now()) {
        // 检查是否为重复设置同一日期
        val isDateChanged = !DateTimeUtils.isSameDay(selectedDate, defaultDate)
        
        selectedDate = defaultDate
        currentWeekMonday = getWeekStart(selectedDate)
        
        // 重新设置ViewPager位置
        setupPager()
        
        // 仅在日期发生变更时通知外部监听器
        if (isDateChanged) {
            notifyDateSelected(selectedDate)
        }
    }

    private fun setupViews() {
        // 基本视图设置
    }

    private fun setupPager() {
        viewPager.adapter = weekAdapter
        
        // 根据数据集控制属性设置初始位置
        val initialPosition = when {
            previousWeeksCount >= 0 && nextWeeksCount >= 0 -> {
                // 两个属性都有限制：当前周位置为 previousWeeksCount
                previousWeeksCount
            }
            previousWeeksCount >= 0 -> {
                // 只限制前面周数：当前周位置为 previousWeeksCount
                previousWeeksCount
            }
            nextWeeksCount >= 0 -> {
                // 只限制后面周数：当前周位置为 1000
                1000
            }
            else -> {
                // 无限制：使用原来的逻辑
                5000
            }
        }
        
        viewPager.setCurrentItem(initialPosition, false)
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                
                // 根据数据集控制属性计算周偏移量
                val weekOffset = when {
                    previousWeeksCount >= 0 && nextWeeksCount >= 0 -> {
                        position - previousWeeksCount
                    }
                    previousWeeksCount >= 0 -> {
                        position - previousWeeksCount
                    }
                    nextWeeksCount >= 0 -> {
                        position - 1000
                    }
                    else -> {
                        position - 5000
                    }
                }
                
                val baseWeekStart = getWeekStart(today)
                
                // 根据数据集大小控制属性进行导航限制检查
                if (previousWeeksCount >= 0 && weekOffset < -previousWeeksCount) {
                    // 超出向前周数限制
                    val limitPosition = when {
                        previousWeeksCount >= 0 && nextWeeksCount >= 0 -> 0
                        previousWeeksCount >= 0 -> 0
                        else -> 5000 - previousWeeksCount
                    }
                    viewPager.setCurrentItem(limitPosition, false)
                    return
                }
                
                if (nextWeeksCount >= 0 && weekOffset > nextWeeksCount) {
                    // 超出向后周数限制
                    val limitPosition = when {
                        previousWeeksCount >= 0 && nextWeeksCount >= 0 -> previousWeeksCount + nextWeeksCount
                        nextWeeksCount >= 0 -> 1000 + nextWeeksCount
                        else -> 5000 + nextWeeksCount
                    }
                    viewPager.setCurrentItem(limitPosition, false)
                    return
                }
                
                currentWeekMonday = addDays(baseWeekStart, weekOffset * 7)
                weekAdapter.notifyDataSetChanged()
                
                // 触发周视图翻页回调，通知当前显示的周是否为系统当前周
                notifyWeekChanged()
            }
        })
    }

    private inner class WeekAdapter : RecyclerView.Adapter<WeekViewHolder>() {
        override fun getItemCount(): Int {
            // 根据数据集大小控制属性计算总的周数
            return when {
                previousWeeksCount >= 0 && nextWeeksCount >= 0 -> {
                    // 两个属性都有限制：前面周数 + 当前周 + 后面周数
                    previousWeeksCount + 1 + nextWeeksCount
                }
                previousWeeksCount >= 0 -> {
                    // 只限制前面周数：使用较大的数量但有限制
                    previousWeeksCount + 1 + 1000
                }
                nextWeeksCount >= 0 -> {
                    // 只限制后面周数：使用较大的数量但有限制
                    1000 + 1 + nextWeeksCount
                }
                else -> {
                    // 无限制：使用足够大的数量
                    10000
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekViewHolder {
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                // ViewPager2要求子视图必须填满整个ViewPager2的宽度和高度
                layoutParams = ViewGroup.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
                // 将horizontalPadding应用到父容器上
                setPadding(horizontalPadding, 0, horizontalPadding, 0)
            }
            return WeekViewHolder(container)
        }

        override fun onBindViewHolder(holder: WeekViewHolder, position: Int) {
            // 根据数据集控制属性计算周偏移量
            val weekOffset = when {
                previousWeeksCount >= 0 && nextWeeksCount >= 0 -> {
                    // 两个属性都有限制：position - previousWeeksCount 得到相对于当前周的偏移
                    position - previousWeeksCount
                }
                previousWeeksCount >= 0 -> {
                    // 只限制前面周数：position - previousWeeksCount 得到相对于当前周的偏移
                    position - previousWeeksCount
                }
                nextWeeksCount >= 0 -> {
                    // 只限制后面周数：position - 1000 得到相对于当前周的偏移
                    position - 1000
                }
                else -> {
                    // 无限制：使用原来的逻辑
                    position - 5000
                }
            }
            
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
                val isPastDate = disablePastDates && dayDate.before(today) && !DateTimeUtils.isSameDay(dayDate, today)
                
                if (isSelected) {
                    // 背景只设置在日期数字上
                    tvDayNumber.setBackgroundResource(R.drawable.bg_week_day_selected)
                    tvDayNumber.setTextColor(selectedTextColor)
                    tvWeekName.setTextColor(selectedWeekTextColor)
                } else if (isPastDate) {
                    // 过去日期的禁用样式
                    tvDayNumber.setBackgroundResource(R.drawable.bg_week_day_normal)
                    tvDayNumber.setTextColor(unselectedTextColor)
                    tvWeekName.setTextColor(unselectedTextColor)
                    tvDayNumber.alpha = 0.3f  // 设置透明度
                    tvWeekName.alpha = 0.3f
                } else {
                    // 背景只设置在日期数字上
                    tvDayNumber.setBackgroundResource(R.drawable.bg_week_day_normal)
                    tvDayNumber.setTextColor(unselectedTextColor)
                    tvWeekName.setTextColor(unselectedTextColor)
                    tvDayNumber.alpha = 1.0f  // 确保正常日期的透明度为1
                    tvWeekName.alpha = 1.0f
                }
                
                // 设置点击事件
                dayView.click {
                    // 检查是否禁用过去日期
                    if (disablePastDates && dayDate.before(today) && !DateTimeUtils.isSameDay(dayDate, today)) {
                        // 如果禁用过去日期且当前日期是过去的日期，则不响应点击
                        return@click
                    }
                    
                    // 检查是否为重复点击同一日期
                    val isDateChanged = !DateTimeUtils.isSameDay(selectedDate, dayDate)
                    
                    // 更新选中日期
                    selectedDate = dayDate
                    
                    // 仅在日期发生变更时通知外部监听器
                    if (isDateChanged) {
                        notifyDateSelected(selectedDate)
                    }
                    
                    // 无论是否变更都需要刷新UI以更新选中状态
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

    /**
     * 通知日期选择事件
     * 仅在用户选择日期时触发，不包含周切换信息
     */
    private fun notifyDateSelected(date: Date) {
        onDateSelectedListener?.invoke(date)
    }

    /**
     * 通知周切换事件
     * 在周视图页面切换时触发，提供是否为当前周的信息
     */
    private fun notifyWeekChanged() {
        val isCurrentWeek = isCurrentWeekDisplayed()
        onWeekChangedListener?.invoke(isCurrentWeek)
    }

    /**
     * 判断当前显示的周是否为系统当前周
     * @return true 如果当前显示的周包含今天，false 否则
     */
    private fun isCurrentWeekDisplayed(): Boolean {
        val todayWeekStart = getWeekStart(today)
        return DateTimeUtils.isSameDay(currentWeekMonday, todayWeekStart)
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

    /**
     * 获取日期对应的周名称缩写
     * 优先使用自定义周名称数组，否则返回英文缩写
     * @param date 日期
     * @return 周名称缩写
     */
    private fun getWeekAbbr(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // 使用安全的null检查和let操作符，避免非空断言
        return customWeekdayNames?.let { weekNames ->
            if (weekNames.size >= 7) {
                // 使用自定义周名称数组
                val index = WeekDay.getCustomArrayIndex(dayOfWeek, weekStartOnMonday)
                weekNames[index]
            } else {
                // 自定义数组长度不足，回退到默认英文缩写
                val weekDay = WeekDay.fromCalendarValue(dayOfWeek)
                weekDay.englishAbbr
            }
        } ?: run {
            // customWeekdayNames为null时，默认返回英文缩写
            val weekDay = WeekDay.fromCalendarValue(dayOfWeek)
            weekDay.englishAbbr
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
        // 检查是否为重复设置同一日期
        val isDateChanged = !DateTimeUtils.isSameDay(selectedDate, date)
        
        selectedDate = date
        currentWeekMonday = getWeekStart(date)
        weekAdapter.notifyDataSetChanged()
        
        // 仅在日期发生变更时通知外部监听器
        if (isDateChanged) {
            notifyDateSelected(selectedDate)
        }
    }

    fun resetToToday() {
        val newToday = DateTimeUtils.now()
        val newSelectedDate = newToday
        
        // 检查是否为重复设置同一日期
        val isDateChanged = !DateTimeUtils.isSameDay(selectedDate, newSelectedDate)
        
        today = newToday
        selectedDate = newSelectedDate
        currentWeekMonday = getWeekStart(today)
        viewPager.setCurrentItem(5000, false)
        weekAdapter.notifyDataSetChanged()
        
        // 仅在日期发生变更时通知外部监听器
        if (isDateChanged) {
            notifyDateSelected(selectedDate)
        }
    }

    fun nextWeek() {
        // 根据新的数据集控制属性检查是否可以向前翻页
        if (nextWeeksCount < 0) {
            // 无限制，可以翻页
            viewPager.setCurrentItem(viewPager.currentItem + 1, true)
        } else {
            // 有限制，需要检查当前位置
            val currentPosition = viewPager.currentItem
            val maxPosition = when {
                previousWeeksCount >= 0 && nextWeeksCount >= 0 -> previousWeeksCount + nextWeeksCount
                nextWeeksCount >= 0 -> 1000 + nextWeeksCount
                else -> currentPosition + 1 // 不应该到达这里
            }
            if (currentPosition < maxPosition) {
                viewPager.setCurrentItem(currentPosition + 1, true)
            }
        }
    }

    fun prevWeek() {
        // 根据新的数据集控制属性检查是否可以向后翻页
        if (previousWeeksCount < 0) {
            // 无限制，可以翻页
            viewPager.setCurrentItem(viewPager.currentItem - 1, true)
        } else {
            // 有限制，需要检查当前位置
            val currentPosition = viewPager.currentItem
            val minPosition = 0
            if (currentPosition > minPosition) {
                viewPager.setCurrentItem(currentPosition - 1, true)
            }
        }
    }

    /**
     * 设置日期选择回调
     * 当用户选择日期时触发
     * @param listener 日期选择回调函数，参数为选中的日期
     */
    fun setOnDateSelectedListener(listener: (date: Date) -> Unit) {
        onDateSelectedListener = listener
    }

    /**
     * 设置周切换回调
     * 当周视图页面切换时触发
     * @param listener 周切换回调函数，参数为是否为当前周
     */
    fun setOnWeekChangedListener(listener: (isCurrentWeek: Boolean) -> Unit) {
        onWeekChangedListener = listener
    }

    // 新的API方法，用于设置数据集大小控制属性
    fun setPreviousWeeksCount(count: Int) {
        this.previousWeeksCount = count
        weekAdapter.notifyDataSetChanged()
        // 重新设置当前位置以适应新的数据集大小
        setupPager()
    }

    fun setNextWeeksCount(count: Int) {
        this.nextWeeksCount = count
        weekAdapter.notifyDataSetChanged()
        // 重新设置当前位置以适应新的数据集大小
        setupPager()
    }

    // 获取当前设置的数据集大小控制属性
    fun getPreviousWeeksCount(): Int = previousWeeksCount
    fun getNextWeeksCount(): Int = nextWeeksCount
    
    /**
     * 重写触摸事件拦截，处理边界滑动时阻止事件传递给父ViewPager
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
                isHorizontalScroll = false
                hasIntercepted = false
                // 让子View先处理事件
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = Math.abs(ev.x - initialX)
                val deltaY = Math.abs(ev.y - initialY)
                
                // 判断是否为水平滑动
                if (!isHorizontalScroll && deltaX > deltaY && deltaX > 20) {
                    isHorizontalScroll = true
                }
                
                // 如果是水平滑动且到达边界，拦截事件
                if (isHorizontalScroll && isAtBoundary(ev.x - initialX)) {
                    hasIntercepted = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (hasIntercepted) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                isHorizontalScroll = false
                hasIntercepted = false
            }
        }
        return super.onInterceptTouchEvent(ev)
    }
    
    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (hasIntercepted) {
                    // 如果已经拦截了事件，阻止进一步的滑动
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (hasIntercepted) {
                    parent?.requestDisallowInterceptTouchEvent(false)
                    hasIntercepted = false
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    /**
     * 判断是否到达滑动边界
     * @param deltaX 水平滑动距离，正值表示向右滑动，负值表示向左滑动
     * @return true 如果到达边界且不能继续滑动
     */
    private fun isAtBoundary(deltaX: Float): Boolean {
        val currentPosition = viewPager.currentItem
        
        // 计算边界位置
        val isAtFirstPage = when {
            previousWeeksCount >= 0 && nextWeeksCount >= 0 -> currentPosition <= 0
            previousWeeksCount >= 0 -> currentPosition <= 0
            nextWeeksCount >= 0 -> currentPosition <= 1000 - nextWeeksCount
            else -> currentPosition <= 0
        }
        
        val isAtLastPage = when {
            previousWeeksCount >= 0 && nextWeeksCount >= 0 -> currentPosition >= previousWeeksCount + nextWeeksCount
            previousWeeksCount >= 0 -> currentPosition >= previousWeeksCount + 1000
            nextWeeksCount >= 0 -> currentPosition >= 1000 + nextWeeksCount
            else -> currentPosition >= weekAdapter.itemCount - 1
        }
        
        // 向右滑动（deltaX > 0）且在第一页，或向左滑动（deltaX < 0）且在最后一页
        return (deltaX > 0 && isAtFirstPage) || (deltaX < 0 && isAtLastPage)
    }
}