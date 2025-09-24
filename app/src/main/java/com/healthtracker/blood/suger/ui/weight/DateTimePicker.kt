package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.healthtracker.blood.suger.R
import com.healthtracker.framework.ext.dp2px
import com.healthtracker.framework.util.FontUtils
import com.peppa.widget.picker.NumberPickerView
import java.util.Calendar
import java.util.Locale
import com.healthtracker.blood.suger.data.utils.DateTimeUtils

class DateTimePicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle), NumberPickerView.OnValueChangeListener {

    private var valueChangeListener: NumberPickerView.OnValueChangeListener? = null
    private var enableTouch = true

    private val yearPicker: NumberPickerView
    private val monthPicker: NumberPickerView
    private val dayPicker: NumberPickerView
    private val hourPicker: NumberPickerView
    private val minutePicker: NumberPickerView

    init {
        val inflater = LayoutInflater.from(context).inflate(R.layout.layout_datetime_picker, this)
        yearPicker = inflater.findViewById(R.id.yearPicker)
        monthPicker = inflater.findViewById(R.id.monthPicker)
        dayPicker = inflater.findViewById(R.id.dayPicker)
        hourPicker = inflater.findViewById(R.id.hourPicker)
        minutePicker = inflater.findViewById(R.id.minutePicker)

        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.leftMargin = context.dp2px(15)
        layoutParams.rightMargin = context.dp2px(15)
        layoutParams.topMargin = context.dp2px(15)
        layoutParams.bottomMargin = context.dp2px(15)
        setLayoutParams(layoutParams)

        val tfRegular = FontUtils.getInstance().robotoRegular
        val tfBold = FontUtils.getInstance().robotoBold

        yearPicker.setContentNormalTextTypeface(tfRegular)
        yearPicker.setContentSelectedTextTypeface(tfBold)
        monthPicker.setContentNormalTextTypeface(tfRegular)
        monthPicker.setContentSelectedTextTypeface(tfBold)
        dayPicker.setContentNormalTextTypeface(tfRegular)
        dayPicker.setContentSelectedTextTypeface(tfBold)
        hourPicker.setContentNormalTextTypeface(tfRegular)
        hourPicker.setContentSelectedTextTypeface(tfBold)
        minutePicker.setContentNormalTextTypeface(tfRegular)
        minutePicker.setContentSelectedTextTypeface(tfBold)

        initView()

    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return if (enableTouch) {
            super.dispatchTouchEvent(ev)
        } else {
            true
        }
    }

    fun initView(
        year: Int = -1,
        month: Int = -1,
        day: Int = -1,
        hour: Int = -1,
        minute: Int = -1
    ) {
        // 只调用一次获取当前时间的所有组件
        val currentDateTime = DateTimeUtils.extractDateComponents(DateTimeUtils.now())
        val actualYear = if (year == -1) currentDateTime.year else year
        val actualMonth = if (month == -1) currentDateTime.month else month
        val actualDay = if (day == -1) currentDateTime.day else day
        val actualHour = if (hour == -1) currentDateTime.hour else hour
        val actualMinute = if (minute == -1) currentDateTime.minute else minute
        setupYearPicker(actualYear)
        setupMonthPicker(actualMonth)
        setupDayPicker(actualYear, actualMonth, actualDay)
        setupHourPicker(actualHour)
        setupMinutePicker(actualMinute)
    }

    private fun setupYearPicker(currentYear: Int) {
        val startYear = currentYear - 9
        val years = Array(10) { i -> (startYear + i).toString() }
        yearPicker.displayedValues = years
        // 先设置min/max值，再设置displayedValues
        yearPicker.minValue = 0
        yearPicker.maxValue = 9

        yearPicker.value = 9// 默认选中最后一个（当前年）
        yearPicker.setOnValueChangedListener(this)
    }

    private fun setupMonthPicker(currentMonth: Int) {
        val months = Array(12) { i -> String.format(Locale.ENGLISH,"%02d", i + 1) }
        monthPicker.displayedValues = months
        // 先设置min/max值，再设置displayedValues
        monthPicker.minValue = 0
        monthPicker.maxValue = 11

        monthPicker.value = currentMonth - 1
        monthPicker.setOnValueChangedListener(this)
    }

    private fun setupDayPicker(year: Int, month: Int, currentDay: Int) {
        val daysInMonth = getDaysInMonth(year, month)
        val days = Array(daysInMonth) { i -> String.format(Locale.ENGLISH,"%02d", i + 1) }
        val dayIndex = if (currentDay <= daysInMonth) currentDay - 1 else daysInMonth - 1
        if(dayPicker.displayedValues == null){
            dayPicker.displayedValues = days
        }else{
            dayPicker.refreshByNewDisplayedValues(days)
        }

        // 先设置min/max值，再设置displayedValues
        dayPicker.minValue = 0
        dayPicker.maxValue = daysInMonth - 1

        dayPicker.value = dayIndex
        dayPicker.setOnValueChangedListener(this)
    }

    private fun setupHourPicker(currentHour: Int) {
        val hours = Array(24) { i -> String.format(Locale.ENGLISH,"%02d", i) }
        hourPicker.displayedValues = hours
        // 先设置min/max值，再设置displayedValues
        hourPicker.minValue = 0
        hourPicker.maxValue = 23

        hourPicker.value = currentHour
        hourPicker.setOnValueChangedListener(this)
    }

    private fun setupMinutePicker(currentMinute: Int) {
        val minutes = Array(60) { i -> String.format(Locale.ENGLISH,"%02d", i) }
        minutePicker.displayedValues = minutes
        // 先设置min/max值，再设置displayedValues
        minutePicker.minValue = 0
        minutePicker.maxValue = 59

        minutePicker.value = currentMinute
        minutePicker.setOnValueChangedListener(this)
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    override fun onValueChange(picker: NumberPickerView?, oldVal: Int, newVal: Int) {
        when (picker) {
            yearPicker, monthPicker -> {
                // 年份或月份变化时，重新设置日期选择器
                val selectedYear = getSelectedYear()
                val selectedMonth = getSelectedMonth()
                val currentDay = getSelectedDay()
                if(getDaysInMonth(selectedYear,selectedMonth).toString() != dayPicker.displayedValues[dayPicker.displayedValues.size - 1]){
                    setupDayPicker(selectedYear,selectedMonth,currentDay)
                }
            }
        }
        valueChangeListener?.onValueChange(picker, oldVal, newVal)
    }

    fun hideDivider() {
        yearPicker.setDividerColor(Color.TRANSPARENT)
        monthPicker.setDividerColor(Color.TRANSPARENT)
        dayPicker.setDividerColor(Color.TRANSPARENT)
        hourPicker.setDividerColor(Color.TRANSPARENT)
        minutePicker.setDividerColor(Color.TRANSPARENT)
    }

    fun setOnValueChangeListener(listener: NumberPickerView.OnValueChangeListener?) {
        valueChangeListener = listener
    }

    private fun getSelectedYear(): Int {
        return yearPicker.contentByCurrValue.toInt()
    }

    private fun getSelectedMonth(): Int {
        return monthPicker.contentByCurrValue.toInt()
    }

    private fun getSelectedDay(): Int {
        return dayPicker.contentByCurrValue.toInt()
    }

    private fun getSelectedHour(): Int {
        return hourPicker.contentByCurrValue.toInt()
    }

    private fun getSelectedMinute(): Int {
        return minutePicker.contentByCurrValue.toInt()
    }

    fun getDateTime(): DateTimeData {
        return DateTimeData(
            year = getSelectedYear(),
            month = getSelectedMonth(),
            day = getSelectedDay(),
            hour = getSelectedHour(),
            minute = getSelectedMinute()
        )
    }

    data class DateTimeData(
        var year: Int,
        var month: Int,
        var day: Int,
        var hour: Int,
        var minute: Int
    ) {
        fun toCalendar(): Calendar {
            val calendar = Calendar.getInstance()
            calendar.set(year, month - 1, day, hour, minute, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar
        }
    }
}