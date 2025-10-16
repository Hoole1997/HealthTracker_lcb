package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityBpDetailBinding
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.viewmodel.BpDetailViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BpDetailActivity: BaseMVVMActivity<BpDetailViewModel, ActivityBpDetailBinding>() {

    companion object{
        private const val RECORD_ID = "record_id"
        // 启动详情页面
        fun start(context: Context, recordId: Long) {
            context.startActivity<BpDetailActivity>(RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() = ActivityBpDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BpDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind){
            btnBack.click {
                finish()
            }
            btnDelete.clickWithDuration {
                showDeleteConfirm()
            }

            btnEdit.clickWithDuration {
                mViewModel.bloodPressureRecord.value?.let {
                    BpRecordActivity.start(this@BpDetailActivity,it.id)
                }
            }
        }

        // 观察数据变化
        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            // 观察血压记录数据变化
            mViewModel.bloodPressureRecord.collect { record ->
                record?.let {
                    updateUI(it)
                }
            }
        }

        lifecycleScope.launch {
            // 观察错误状态
            mViewModel.error.collect { error ->
                error?.let {
                    // TODO: 显示错误信息
                }
            }
        }

        lifecycleScope.launch {
            // 观察加载状态
            mViewModel.isLoading.collect { isLoading ->
                // TODO: 显示/隐藏加载状态
            }
        }
    }

    private fun updateUI(record: BloodPressureRecord) {
        with(mViewBind) {
            // 使用通用等级视图：设置等级列表与当前索引
            val levels = LeveDataFactory.BloodPressure.buildItems(this@BpDetailActivity)
            bpStatusView.setLevels(levels)
            val idx = LeveDataFactory.BloodPressure.indexFor(record.systolicPressure, record.diastolicPressure)
            bpStatusView.setCurrentLevel(idx)
            tvSystolicValue.text = record.systolicPressure.toString()
            tvDiastolicValue.text = record.diastolicPressure.toString()
            tvPulseValue.text = record.pulseRate.toString()
            tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)

            // 获取血压等级描述
            val category = record.getBloodPressureCategoryEnum()
            val rangeDes = resources.getStringArray(R.array.bp_level_expert_advice)
            val levelDescription = when(category) {
                BloodPressureCategory.LOW -> rangeDes[0] // 低血压建议
                BloodPressureCategory.NORMAL -> rangeDes[1] // 正常建议
                BloodPressureCategory.ELEVATED -> rangeDes[2] // 偏高建议
                BloodPressureCategory.HIGH_STAGE_1 -> rangeDes[3] // 1级高血压建议
                BloodPressureCategory.HIGH_STAGE_2 -> rangeDes[4] // 2级高血压建议
                BloodPressureCategory.HYPERTENSIVE_CRISIS -> rangeDes[5] // 高血压危象建议
                else -> "UnKnow"
            }

            // 设置等级描述文案
            tvLeveDes.text = Html.fromHtml(String.format(levelDescription,record.systolicPressure,record.diastolicPressure))
        }
    }

    override fun getStatusBarColor() = R.color.c5

    private fun showDeleteConfirm() {
        ConfirmDialog(
            title = getString(R.string.delete_record_remind_title),
            message = getString(R.string.delete_record_remind),
            leftText = getString(R.string.cancel),
            rightText = getString(R.string.confirm),
            onDialogListener = object : DialogListener {
                override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                    super.onItemClick(dialogFragment, which)
                    if (which == R.id.btn_ok) {
                        lifecycleScope.launch {
                            if (mViewModel.deleteRecord()) {
                                finish()
                            } else {
                                showToast(getString(R.string.delete_record_failed))
                            }
                        }
                    }
                }
            }
        ).show(supportFragmentManager)
    }
}