package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityHistoryRecordBinding
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.getStatusStringRes
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog
import com.healthtracker.blood.suger.ui.dialog.StatusSelectDialog
import com.healthtracker.blood.suger.ui.history.BloodPressureHistoryItem
import com.healthtracker.blood.suger.ui.history.BloodSugarHistoryItem
import com.healthtracker.blood.suger.ui.history.HistoryAdapter
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.blood.suger.ui.viewmodel.HistoryViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import dagger.hilt.android.AndroidEntryPoint

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
                    // 移除不必要的lifecycleScope.launch，StatusSelectDialog已经处理了协程
                    val currentStatus = mViewModel.selectedBloodSugarStatus.value
                    StatusSelectDialog.show(supportFragmentManager, currentStatus) {
                        mViewModel.setBloodSugarStatusFilter(it)
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
                BpDetailActivity.start(this,item.getId())
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
        this.collectLatest(mViewModel.dateRangeText) { dateRangeText ->
            mViewBind.tvFilterDateRange.text = dateRangeText
        }

        // 观察血糖状态筛选
        this.collectLatest(mViewModel.selectedBloodSugarStatus) { status ->
            updateStatusDisplay(status)
        }

        // 观察加载状态
        this.collectLatest(mViewModel.isLoading) { isLoading ->
            updateLoadingState(isLoading)
        }

        // 观察错误信息
        this.collectLatest(mViewModel.errorMessage) { errorMessage ->
            updateErrorState(errorMessage)
        }

        // 观察历史记录类型，只设置一次初始类型，避免重复观察
        this.collectLatest(mViewModel.isBloodSugarHistory) { isBloodSugar ->
            // 根据记录类型观察对应的数据流
            observeRecordsBasedOnType(isBloodSugar)
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
     * 只负责加载指示器的显示/隐藏，不干扰其他UI状态
     */
    private fun updateLoadingState(isLoading: Boolean) {
        if (isLoading) {
            mViewBind.progressLoading.visible()
            // 不隐藏其他View，由updateUIWithRecords和updateErrorState统一管理
        } else {
            mViewBind.progressLoading.gone()
            // 不在这里显示其他View，由数据更新方法负责
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
     * 根据记录类型观察相应数据
     */
    private fun observeRecordsBasedOnType(isBloodSugar: Boolean) {
        if (isBloodSugar) {
            observeBloodSugarRecords()
        } else {
            observeBloodPressureRecords()
        }
    }

    /**
     * 观察血糖记录数据
     */
    private fun observeBloodSugarRecords() {
        this.collectLatest(mViewModel.bloodSugarRecords) { records ->
            // 转换为HistoryRecordItem并更新适配器
            val historyItems = records.map { BloodSugarHistoryItem(it) }
            updateUIWithRecords(historyItems, records.isEmpty())
        }
    }

    /**
     * 观察血压记录数据
     */
    private fun observeBloodPressureRecords() {
        this.collectLatest(mViewModel.bloodPressureRecords) { records ->
            // 转换为HistoryRecordItem并更新适配器
            val historyItems = records.map { BloodPressureHistoryItem(it) }
            updateUIWithRecords(historyItems, records.isEmpty())
        }
    }

    /**
     * 统一更新UI状态
     * 此方法负责正常数据状态的UI显示，不处理错误状态
     */
    private fun updateUIWithRecords(historyItems: List<HistoryRecordItem>, isEmpty: Boolean) {
        historyAdapter.submitList(historyItems)

        // 只在非加载且非错误状态下才处理数据显示
        if (!mViewModel.isLoading.value && mViewModel.errorMessage.value == null) {
            if (isEmpty) {
                mViewBind.rvHistory.gone()
                mViewBind.tvEmpty.visible()
            } else {
                mViewBind.rvHistory.visible()
                mViewBind.tvEmpty.gone()
            }
        }
        // 如果正在加载或有错误，不改变数据显示状态，由对应的状态处理方法管理
    }

    // 移除了updateRecordsList方法，统一在updateUIWithRecords中处理



    /**
     * 显示日期范围选择器
     */
    /**
     * 显示日期范围选择器
     */
    private fun showTimeRangePick(){
        // 移除不必要的lifecycleScope.launch，UI操作不需要协程
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