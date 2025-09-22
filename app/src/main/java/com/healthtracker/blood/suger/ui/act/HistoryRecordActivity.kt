package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityHistoryRecordBinding
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.enum.getStatusStringRes
import com.healthtracker.blood.suger.ui.dialog.StatusSelectDialog
import com.healthtracker.blood.suger.ui.viewmodel.HistoryViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryRecordActivity: BaseMVVMActivity<HistoryViewModel, ActivityHistoryRecordBinding>() {


    companion object{
        private const val IS_BS = "IS_BS"
        fun start(context: Context, isBs: Boolean = true){
            context.startActivity<HistoryRecordActivity>(IS_BS to isBs)
        }
    }


    override fun createViewBinding() = ActivityHistoryRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HistoryViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 设置历史记录类型
        val isBloodSugar = intent.getBooleanExtra(IS_BS, true)
        mViewModel.setHistoryType(isBloodSugar)

        // 初始化UI
        with(mViewBind){
            btnBack.click {
                finish()
            }
            tvFilterDateRange.clickWithDuration {
                showTimeRangePick()
            }

            if (isBloodSugar) {
                tvFilterStatu.clickWithDuration {
                    lifecycleScope.launch {
                        val currentStatus = mViewModel.selectedBloodSugarStatus.value
                        StatusSelectDialog.show(supportFragmentManager, currentStatus ?: BloodSugarStatus.DEFAULT) {
                            mViewModel.setBloodSugarStatusFilter(if (it == BloodSugarStatus.DEFAULT) null else it)
                        }
                    }
                }
            } else {
                tvFilterStatu.gone()
            }
        }

        // 观察ViewModel状态变化
        observeViewModel()
    }


    /**
     * 观察ViewModel状态变化
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            mViewModel.dateRangeText.collect { dateRangeText ->
                mViewBind.tvFilterDateRange.text = dateRangeText
            }
        }

        lifecycleScope.launch {
            mViewModel.selectedBloodSugarStatus.collect { status ->
                updateStatusDisplay(status)
            }
        }
    }

    /**
     * 更新状态显示
     */
    private fun updateStatusDisplay(status: BloodSugarStatus?) {
        mViewBind.tvFilterStatu.text =
            if (status == null) getString(R.string.all_types) else {
                getString(getStatusStringRes(status.statusType))
            }
    }



    /**
     * 显示日期范围选择器
     */
    private fun showTimeRangePick(){
        lifecycleScope.launch {
            val startDate = mViewModel.startDate.value
            val endDate = mViewModel.endDate.value

            // 创建日历约束，设置打开时显示结束日期所在的月份
            val calendarConstraints = CalendarConstraints.Builder()
                .setOpenAt(endDate) // 定位到结束日期所在月份
                .build()

            val datePicker = MaterialDatePicker.Builder.dateRangePicker().apply {
                setTitleText("SELECT DATE RANGE")
                // 设置自定义主题
                setTheme(R.style.CustomDatePickerTheme)
                // 设置默认选中的日期范围
                setSelection(androidx.core.util.Pair(startDate, endDate))
                // 设置日历约束
                setCalendarConstraints(calendarConstraints)
            }.build()

            // 设置选择监听器
            datePicker.addOnPositiveButtonClickListener { selection ->
                // 更新ViewModel中的日期范围
                mViewModel.setDateRange(selection.first, selection.second)
            }

            datePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
        }
    }
}