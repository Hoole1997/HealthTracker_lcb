package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.icu.text.DateFormat
import android.os.Bundle
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityHistoryRecordBinding
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.enum.getStatusStringRes
import com.healthtracker.blood.suger.ui.dialog.StatusSelectDialog
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.startActivity
import java.text.SimpleDateFormat
import java.util.*

class HistoryRecordActivity: BaseMVVMActivity<BaseViewModel, ActivityHistoryRecordBinding>() {


    companion object{
        private const val RECORD_ID = "RECORD_ID"
        private const val IS_BS = "IS_BS"
        fun start(context: Context,recordId: Long, isBs: Boolean = true){
            context.startActivity<HistoryRecordActivity>(RECORD_ID to recordId,IS_BS to isBs)
        }
    }

    // 保存当前选中的日期范围
    private var currentStartDate: Long = 0
    private var currentEndDate: Long = 0

    private var bsStatus: BloodSugarStatus = BloodSugarStatus.DEFAULT

    // 日期格式化器
    private val dateFormat = DateFormat.getDateInstance()

    override fun createViewBinding() = ActivityHistoryRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 初始化默认日期范围
        initDefaultDateRange()
        with(mViewBind){
            btnBack.click {
                finish()
            }
            tvFilterDateRange.clickWithDuration {
                showTimeRangePick()
            }

            if (intent.getBooleanExtra(IS_BS, true)) {
                tvFilterStatu.clickWithDuration {
                    StatusSelectDialog.show(supportFragmentManager, bsStatus) {
                        bsStatus = it
                        updateStatusType()
                    }
                }
                updateStatusType()
            } else {
                tvFilterStatu.gone()
            }
        }
    }


    private fun updateStatusType(){
        mViewBind.tvFilterStatu.text =
            if (bsStatus == BloodSugarStatus.DEFAULT) getString(R.string.all_types) else {
                getString(getStatusStringRes(bsStatus.statusType))
            }
    }

    /**
     * 初始化默认日期范围（去年今天 ~ 今天）
     */
    private fun initDefaultDateRange() {
        // 先获取本地今天的日期，然后转换为UTC的midnight
        val localCalendar = Calendar.getInstance()
        val year = localCalendar.get(Calendar.YEAR)
        val month = localCalendar.get(Calendar.MONTH)
        val dayOfMonth = localCalendar.get(Calendar.DAY_OF_MONTH)

        // 创建UTC时区的Calendar，设置为今天的midnight (UTC)
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.set(year, month, dayOfMonth, 0, 0, 0)
        utcCalendar.set(Calendar.MILLISECOND, 0)
        currentEndDate = utcCalendar.timeInMillis

        // 计算去年今天的日期
        val startUtcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        startUtcCalendar.set(year - 1, month, dayOfMonth, 0, 0, 0)
        startUtcCalendar.set(Calendar.MILLISECOND, 0)
        currentStartDate = startUtcCalendar.timeInMillis

        // 更新UI显示
        updateDateRangeDisplay()
    }

    /**
     * 更新日期范围显示
     */
    private fun updateDateRangeDisplay() {
        val startDateStr = dateFormat.format(Date(currentStartDate))
        val endDateStr = dateFormat.format(Date(currentEndDate))
        "$startDateStr - $endDateStr".also { mViewBind.tvFilterDateRange.text = it }
    }


    private fun showTimeRangePick(){
        // 创建日历约束，设置打开时显示结束日期所在的月份
        val calendarConstraints = CalendarConstraints.Builder()
            .setOpenAt(currentEndDate) // 定位到结束日期所在月份
            .build()

        val datePicker = MaterialDatePicker.Builder.dateRangePicker().apply {
            // 设置自定义主题
            setTheme(R.style.CustomDatePickerTheme)
            // 设置默认选中的日期范围
            setSelection(androidx.core.util.Pair(currentStartDate, currentEndDate))
            // 设置日历约束
            setCalendarConstraints(calendarConstraints)
        }.build()

        // 设置选择监听器
        datePicker.addOnPositiveButtonClickListener { selection ->
            // 用户确认选择，更新当前日期范围
            currentStartDate = selection.first
            currentEndDate = selection.second

            // 更新UI显示
            updateDateRangeDisplay()
        }

        // 取消操作不做任何处理，保持原有显示

        datePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }
}