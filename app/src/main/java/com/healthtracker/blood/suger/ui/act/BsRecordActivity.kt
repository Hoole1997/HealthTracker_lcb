package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.databinding.ActivityBsRecordBinding
import com.healthtracker.blood.suger.enum.getStatusStringRes
import com.healthtracker.blood.suger.ui.dialog.LabelDialog
import com.healthtracker.blood.suger.ui.dialog.StatusSelectDialog
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
import java.util.Calendar

@OptIn(FlowPreview::class)
@AndroidEntryPoint
class BsRecordActivity: BaseMVVMActivity<BsRecordViewModel, ActivityBsRecordBinding>() {


    private val healthTags = mutableListOf<HealthTag>()
    private val addTagIds = mutableListOf<Long>()

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

            // 设置DateTimeSelectionView的标签点击监听
            dateTimeSelectionView.setOnLabelClickListener {
                val addTags = if(addTagIds.isEmpty()) null else {
                    val tempTags = mutableListOf<HealthTag>()
                    for(id in addTagIds){
                        healthTags.find { it.id == id }?.let {
                            tempTags.add(it)
                        }
                    }
                    tempTags
                }
                LabelDialog.show(supportFragmentManager,healthTags,addTags){
                    mViewModel.updateTags(it)
                }
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
                        tvSelectValue.text = BsUnit.formatValue(value, currentUnit)

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
                    rbMgdl.id -> BsUnit.MG_DL
                    rbMmol.id -> BsUnit.MMOL_L
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
                // 保存前先获取DateTimeSelectionView的时间并更新到ViewModel
                val selectedDateTime = mViewBind.dateTimeSelectionView.getDateTimePicker().getDateTime()
                val currentTime = Calendar.getInstance()
                val selectedCalendar = selectedDateTime.toCalendar()
                // 保留当前时间的秒和毫秒
                selectedCalendar.set(Calendar.SECOND, currentTime.get(Calendar.SECOND))
                selectedCalendar.set(Calendar.MILLISECOND, currentTime.get(Calendar.MILLISECOND))
                //需要减1s避免立即关闭时出现0s前的情况
                selectedCalendar.add(Calendar.SECOND, -1)
                val selectedDate = selectedCalendar.time
                mViewModel.updateRecordTime(selectedDate)

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
        mViewModel.currentValue.debounce(50L).distinctUntilChanged().collectLifecycle { value ->
            try {
                updateDisplayValues()
                updateRangeView()
                mViewBind.rulerView.setScaleImmediately(value)
            } catch (e: Exception) {
                // 记录错误或显示用户友好的错误信息
            }
        }

        mViewModel.currentUnit.collectLatestLifecycle { unit ->
            configureRulerForUnit(unit)
            updateUnitRadioButtons(unit)
            updateDisplayValues()
            // 单位切换后，立即设置当前值位置（无动画）
            mViewBind.rulerView.setScaleImmediately(mViewModel.currentValue.value)
        }

        mViewModel.currentStatus.collectLatestLifecycle { status ->
            mViewBind.tvStatus.text = getStatusDisplayText(status.statusType)
            updateRangeView()
        }

        mViewModel.isLoading.collectLifecycle { isLoading ->
            mViewBind.btnSave.isEnabled = !isLoading
            mViewBind.btnSave.text = if (isLoading) {
                getString(R.string.saving)
            } else {
                getString(R.string.save)
            }
        }

        mViewModel.recordTime.collectLatestLifecycle { recordTime ->
            // 将Date转换为DateTimePicker需要的参数
            val calendar = Calendar.getInstance()
            calendar.time = recordTime
            if(!isDestroyed && !isFinishing){
                mViewBind.dateTimeSelectionView.getDateTimePicker().initView(
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH) + 1,
                    day = calendar.get(Calendar.DAY_OF_MONTH),
                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                    minute = calendar.get(Calendar.MINUTE)
                )
            }
        }

        lifecycleScope.launch {
            mViewModel.getHealthTags().collectLatestLifecycle {
                healthTags.clear()
                healthTags.addAll(it)
            }
        }

        mViewModel.healthTags.collectLatestLifecycle { tagIds ->
            addTagIds.clear()
            addTagIds.addAll(tagIds)
        }
    }

    private fun updateUnitRadioButtons(unit: BsUnit) {
        mViewBind.rgUnit.check(
            when (unit) {
                BsUnit.MG_DL -> mViewBind.rbMgdl.id
                BsUnit.MMOL_L -> mViewBind.rbMmol.id
            }
        )
    }

    private fun configureRulerForUnit(unit: BsUnit) {
        BloodSugarScaleHelper.configureRulerForUnit(mViewBind.rulerView, unit)
    }

    private fun setupRangeView() {
        // 范围视图将通过observeViewModel自动更新
    }

    private fun setupStatusSelector() {
        with(mViewBind) {
            // 设置状态选择点击事件
            clStatu.click {
                StatusSelectDialog.show(supportFragmentManager,mViewModel.currentStatus.value){
                    mViewModel.updateStatus(it)
                }

            }
        }
    }


    private fun updateDisplayValues() {
        val currentValue = mViewModel.currentValue.value
        val currentUnit = mViewModel.currentUnit.value
        mViewBind.tvSelectValue.text = BsUnit.formatValue(currentValue, currentUnit)
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


}