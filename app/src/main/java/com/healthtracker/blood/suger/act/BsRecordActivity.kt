package com.healthtracker.blood.suger.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.CollectionUtils.collect
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityBsRecordBinding
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.enum.BloodSugarUnit
import com.healthtracker.blood.suger.ui.viewmodel.BsRecordViewModel
import com.healthtracker.blood.suger.ui.weight.BloodSugarRulerView
import com.healthtracker.blood.suger.util.BloodSugarScaleHelper
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@AndroidEntryPoint
class BsRecordActivity: BaseMVVMActivity<BsRecordViewModel, ActivityBsRecordBinding>() {

    companion object {
        private const val TAG = "BsRecordActivity"
        private const val EXTRA_RECORD_ID = "extra_record_id"
        // 启动编辑模式
        fun start(context: Context, recordId: Long? = null) {
            context.startActivity<BsRecordActivity>(EXTRA_RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() = ActivityBsRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BsRecordViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 获取传入的记录ID（如果有）
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        val editRecordId = if (recordId == -1L) null else recordId

        // 初始化ViewModel
        mViewModel.initializeWithRecord(editRecordId)

        with(mViewBind) {
            btnBack.click {
                finish()
            }

            setupRulerView()
            setupUnitSwitcher()
            setupRangeView()
            setupStatusSelector()
            setupSaveButton()
            observeViewModel()
        }
    }

    private fun setupRulerView() {
        with(mViewBind) {
            rulerView.setOnChooseResultListener(object : BloodSugarRulerView.OnChooseResultListener {
                override fun onEndResult(result: String) {
                    try {
                        "onEndResult result = $result".logd(TAG)
                        mViewModel.updateValue(result.toFloat())
                        rangeView.updateValue(result.toFloat())
                    } catch (e: NumberFormatException) {
                        // 处理转换异常
                    }
                }

                override fun onScrollResult(result: String) {
                    try {
                        "onScrollResult result = $result".logd(TAG)
                        val value = result.toFloat()
                        val currentUnit = mViewModel.currentUnit.value
                        tvSelectValue.text = BloodSugarUnit.formatValue(value, currentUnit)

                    } catch (e: NumberFormatException) {
                        // 处理转换异常
                    }
                }
            })
        }
    }

    private fun setupUnitSwitcher() {
        with(mViewBind) {
            rgUnit.setOnCheckedChangeListener { _, checkedId ->
                val newUnit = when (checkedId) {
                    rbMgdl.id -> BloodSugarUnit.MG_DL
                    rbMmol.id -> BloodSugarUnit.MMOL_L
                    else -> return@setOnCheckedChangeListener
                }

                if (newUnit != mViewModel.currentUnit.value) {
                    mViewModel.switchUnit(newUnit)
                }
            }
        }
    }

    private fun setupSaveButton() {
        mViewBind.btnSave.click {
            lifecycleScope.launch {
                val success = mViewModel.saveRecord()
                if (success) {
                    finish()
                } else {
                    // 显示保存失败提示
                    // TODO: 添加Toast显示
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            mViewModel.currentValue.debounce(50L).distinctUntilChanged().collect { value ->
                updateDisplayValues()
                updateRangeView()
                // 更新刻度尺位置
                mViewBind.rulerView.scrollToScale(value)
            }
        }

        lifecycleScope.launch {
            mViewModel.currentUnit.collect { unit ->
                configureRulerForUnit(unit)
                updateUnitRadioButtons(unit)
                updateDisplayValues()
            }
        }

        lifecycleScope.launch {
            mViewModel.currentStatus.collect { status ->
                mViewBind.tvStatus.text = getStatusDisplayText(status.statusType)
                updateRangeView()
            }
        }

        lifecycleScope.launch {
            mViewModel.isLoading.collect { isLoading ->
                mViewBind.btnSave.isEnabled = !isLoading
                mViewBind.btnSave.text = if (isLoading) {
                    getString(R.string.saving)
                } else {
                    getString(R.string.save)
                }
            }
        }
    }

    private fun updateUnitRadioButtons(unit: BloodSugarUnit) {
        mViewBind.rgUnit.check(
            when (unit) {
                BloodSugarUnit.MG_DL -> mViewBind.rbMgdl.id
                BloodSugarUnit.MMOL_L -> mViewBind.rbMmol.id
            }
        )
    }

    private fun configureRulerForUnit(unit: BloodSugarUnit) {
        BloodSugarScaleHelper.configureRulerForUnit(mViewBind.rulerView, unit)
    }

    private fun setupRangeView() {
        // 范围视图将通过observeViewModel自动更新
    }

    private fun setupStatusSelector() {
        with(mViewBind) {
            // 设置状态选择点击事件
            clStatu.click {
                // TODO: 显示状态选择弹窗
                // 这里暂时模拟切换到不同状态进行测试
                val statuses = BloodSugarStatus.values()
                val currentIndex = statuses.indexOf(mViewModel.currentStatus.value)
                val nextIndex = (currentIndex + 1) % statuses.size
                val newStatus = statuses[nextIndex]

                mViewModel.updateStatus(newStatus)
            }
        }
    }


    private fun updateDisplayValues() {
        val currentValue = mViewModel.currentValue.value
        val currentUnit = mViewModel.currentUnit.value
        mViewBind.tvSelectValue.text = BloodSugarUnit.formatValue(currentValue, currentUnit)
    }

    private fun updateRangeView() {
        mViewBind.rangeView.setCurrentState(
            mViewModel.currentValue.value,
            mViewModel.currentUnit.value,
            mViewModel.currentStatus.value
        )
    }

    private fun getStatusDisplayText(statusType: Int): String {
        val stringRes = getStatusStringRes(statusType)
        return getString(stringRes)
    }

    private fun getStatusStringRes(statusType: Int): Int {
        return when (statusType) {
            0 -> R.string.blood_sugar_status_default
            1 -> R.string.blood_sugar_status_fasting
            2 -> R.string.blood_sugar_status_before_meal
            3 -> R.string.blood_sugar_status_bedtime
            4 -> R.string.blood_sugar_status_after_exercise
            5 -> R.string.blood_sugar_status_one_hour_after_meal
            6 -> R.string.blood_sugar_status_before_exercise
            7 -> R.string.blood_sugar_status_two_hours_after_meal
            else -> R.string.blood_sugar_status_default
        }
    }
}