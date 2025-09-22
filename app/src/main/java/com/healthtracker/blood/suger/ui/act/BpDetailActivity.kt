package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.databinding.ActivityBpDetailBinding
import com.healthtracker.blood.suger.ui.viewmodel.BpDetailViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
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
            // 更新血压状态视图
            bpStatusView.updateBloodPressure(record.systolicPressure, record.diastolicPressure)

            // 获取血压等级描述
            val category = record.getBloodPressureCategoryEnum()
            val levelDescription = when(category.position) {
                in 0.0f..0.2f -> resources.getStringArray(R.array.bp_level_expert_advice)[0] // 低血压建议
                in 0.21f..0.4f -> resources.getStringArray(R.array.bp_level_expert_advice)[1] // 正常建议
                in 0.41f..0.6f -> resources.getStringArray(R.array.bp_level_expert_advice)[2] // 偏高建议
                in 0.61f..0.8f -> resources.getStringArray(R.array.bp_level_expert_advice)[3] // 1级高血压建议
                in 0.81f..0.95f -> resources.getStringArray(R.array.bp_level_expert_advice)[4] // 2级高血压建议
                else -> resources.getStringArray(R.array.bp_level_expert_advice)[5] // 高血压危象建议
            }

            // 设置等级描述文案
            tvLeveDes.text = levelDescription
        }
    }
}