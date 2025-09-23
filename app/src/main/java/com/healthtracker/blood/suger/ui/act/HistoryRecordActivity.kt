package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
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
import com.healthtracker.blood.suger.ui.history.HistoryAdapter
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.blood.suger.ui.history.BloodSugarHistoryItem
import com.healthtracker.blood.suger.ui.history.BloodPressureHistoryItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistoryRecordActivity: BaseMVVMActivity<HistoryViewModel, ActivityHistoryRecordBinding>() {

    // 历史记录适配器
    private lateinit var historyAdapter: HistoryAdapter

    companion object{
        private const val TAG = "HistoryRecordActivity"
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

        // 初始化RecyclerView和适配器
        initRecyclerView()
        
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
                        StatusSelectDialog.show(supportFragmentManager, currentStatus) {
                            mViewModel.setBloodSugarStatusFilter(it)
                        }
                    }
                }
            } else {
                tvFilterStatu.gone()
            }
            
            btnAddRecord.clickWithDuration {
                if(isBloodSugar){
                    BsRecordActivity.start(this@HistoryRecordActivity)
                }else{
                    BpRecordActivity.start(this@HistoryRecordActivity)
                }
            }
        }

        // 观察ViewModel状态变化
        observeViewModel()
    }
    
    /**
     * 初始化RecyclerView和适配器
     */
    private fun initRecyclerView() {
        historyAdapter = HistoryAdapter()
        
        // 设置适配器事件监听
        historyAdapter.setOnItemClickListener(object : HistoryAdapter.OnItemClickListener {
            override fun onItemClick(item: HistoryRecordItem, position: Int) {
                // 处理记录项点击事件
                handleItemClick(item)
            }
            
            override fun onDeleteClick(item: HistoryRecordItem, position: Int) {
                // 处理删除按钮点击事件
                handleDeleteClick(item)
            }
        })
        
        // 设置RecyclerView
        with(mViewBind.rvHistory) {
            layoutManager = LinearLayoutManager(this@HistoryRecordActivity)
            adapter = historyAdapter
        }
    }
    
    /**
     * 处理记录项点击事件
     */
    private fun handleItemClick(item: HistoryRecordItem) {
        when (item.getRecordType()) {
            HistoryRecordItem.RecordType.BLOOD_SUGAR -> {
                BsDetailActivity.start(this,item.getId())
            }
            HistoryRecordItem.RecordType.BLOOD_PRESSURE -> {
                BpRecordActivity.start(this,item.getId())
            }
        }
    }

    /**
     * 处理删除按钮点击事件
     */
    private fun handleDeleteClick(item: HistoryRecordItem) =
        showDeleteConfirm(
            item.getId(),
            item.getRecordType() == HistoryRecordItem.RecordType.BLOOD_SUGAR
        )

    private fun showDeleteConfirm(recordId: Long, isBs: Boolean = true) {
        ConfirmDialog(
            title = getString(R.string.delete_record_remind_title),
            message = getString(R.string.delete_record_remind),
            leftText = getString(R.string.cancel),
            rightText = getString(R.string.confirm),
            onDialogListener = object : DialogListener {
                override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                    super.onItemClick(dialogFragment, which)
                    if (which == R.id.btn_ok) {
                        if (isBs) {
                            mViewModel.deleteBsRecord(recordId)
                        } else {
                            mViewModel.deleteBpRecord(recordId)
                        }
                    }
                }
            }
        ).show(supportFragmentManager)
    }


    /**
     * 观察ViewModel状态变化
     */
    private fun observeViewModel() {
        // 观察日期范围文本
        mViewModel.dateRangeText.collectLatestLifecycle { dateRangeText ->
            mViewBind.tvFilterDateRange.text = dateRangeText
        }

        // 观察血糖状态筛选
        mViewModel.selectedBloodSugarStatus.collectLatestLifecycle { status ->
            updateStatusDisplay(status)
        }

        // 观察加载状态
        mViewModel.isLoading.collectLatestLifecycle { isLoading ->
            updateLoadingState(isLoading)
        }

        // 观察错误信息
        mViewModel.errorMessage.collectLatestLifecycle { errorMessage ->
            updateErrorState(errorMessage)
        }

        // 观察历史记录类型和数据
        mViewModel.isBloodSugarHistory.collectLatestLifecycle { isBloodSugar ->
            if (isBloodSugar) {
                // 观察血糖记录
                observeBloodSugarRecords()
            } else {
                // 观察血压记录
                observeBloodPressureRecords()
            }
        }

        // 设置重试按钮点击事件
        mViewBind.btnRetry.click {
            mViewModel.clearError()
            mViewModel.loadHistoryRecords()
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
     * 更新加载状态
     */
    private fun updateLoadingState(isLoading: Boolean) {
        if (isLoading) {
            mViewBind.progressLoading.visible()
            mViewBind.rvHistory.gone()
            mViewBind.layoutError.gone()
            mViewBind.tvEmpty.gone()
        } else {
            mViewBind.progressLoading.gone()
        }
    }

    /**
     * 更新错误状态
     */
    private fun updateErrorState(errorMessage: String?) {
        if (errorMessage != null) {
            mViewBind.layoutError.visible()
            mViewBind.tvErrorMessage.text = errorMessage
            mViewBind.rvHistory.gone()
            mViewBind.tvEmpty.gone()
        } else {
            mViewBind.layoutError.gone()
        }
    }

    /**
     * 观察血糖记录数据
     */
    private fun observeBloodSugarRecords() {
        mViewModel.bloodSugarRecords.collectLatestLifecycle { records ->
            "load bs complete".logd(TAG)
            updateRecordsList(records.isNotEmpty())
            
            // 转换为HistoryRecordItem并更新适配器
            val historyItems = records.map { BloodSugarHistoryItem(it) }
            historyAdapter.submitList(historyItems)
            
            if (records.isEmpty() && !mViewModel.isLoading.value && mViewModel.errorMessage.value == null) {
                mViewBind.tvEmpty.visible()
            } else {
                mViewBind.tvEmpty.gone()
            }
        }
    }

    /**
     * 观察血压记录数据
     */
    private fun observeBloodPressureRecords() {
        mViewModel.bloodPressureRecords.collectLatestLifecycle { records ->
            updateRecordsList(records.isNotEmpty())
            
            // 转换为HistoryRecordItem并更新适配器
            val historyItems = records.map { BloodPressureHistoryItem(it) }
            historyAdapter.submitList(historyItems)
            
            if (records.isEmpty() && !mViewModel.isLoading.value && mViewModel.errorMessage.value == null) {
                mViewBind.tvEmpty.visible()
            } else {
                mViewBind.tvEmpty.gone()
            }
        }
    }

    /**
     * 更新记录列表显示状态
     */
    private fun updateRecordsList(hasData: Boolean) {
        if (hasData) {
            mViewBind.rvHistory.visible()
            mViewBind.tvEmpty.gone()
        } else {
            mViewBind.rvHistory.gone()
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