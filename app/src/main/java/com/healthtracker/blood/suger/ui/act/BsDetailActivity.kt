package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.databinding.ActivityBsDetailBinding
import com.healthtracker.blood.suger.enum.getStatusStringRes
import com.healthtracker.blood.suger.ui.viewmodel.BsDetailViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BsDetailActivity: BaseMVVMActivity<BsDetailViewModel, ActivityBsDetailBinding>() {


    companion object{
        private const val RECORD_ID = "record_id"
        // 启动详情页面
        fun start(context: Context, recordId: Long) {
            context.startActivity<BsDetailActivity>(RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() =  ActivityBsDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BsDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 获取传入的记录ID
        val recordId = intent.getLongExtra(RECORD_ID, -1L)
        if (recordId == -1L) {
            // 没有有效的记录ID，关闭页面
            finish()
            return
        }

        // 初始化ViewModel并加载记录
        mViewModel.initializeWithRecord(recordId)

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
            // 观察血糖记录数据变化
            mViewModel.bloodSugarRecord.collect { record ->
                record?.let {
                    updateUI()
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

    private fun updateUI() {
        with(mViewBind) {
            // 更新血糖状态视图
            val status = mViewModel.getBloodSugarStatus()
            val unit = mViewModel.getDisplayUnit()
            val value = mViewModel.getDisplayValue()

            if (status != null && unit != null && value != null) {
                bsStatusView.updateBloodSugarStatus(value, unit, status)
                val leve = status.getBloodSugarLevel(value,unit)
                val leveDescription = resources.getStringArray(R.array.bs_level_expert_advice)[leve.level]
                // 这里可以根据需要添加更详细的描述文案
                tvLeveDes.text = leveDescription
            }

            // 更新等级描述文案
            status?.let {

            }
        }
    }
}