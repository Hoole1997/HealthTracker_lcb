package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityBpDetailBinding
import com.healthtracker.blood.suger.ui.viewmodel.BpDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.widget.ExpertAdviceView
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle

@AndroidEntryPoint
class BpDetailActivity: BaseInterActivity<BpDetailViewModel, ActivityBpDetailBinding>() {

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
            btnBack.clickWithDuration {
                onBackPress()
            }
            btnDelete.clickWithDuration {
                showDeleteConfirm()
            }

            btnEdit.clickWithDuration {
                mViewModel.bloodPressureRecord.value?.let {
                    BpRecordActivity.start(this@BpDetailActivity,it.id)
                }
            }

            // 设置专家建议控件监听器
            expertAdviceView.setOnExpertAdviceListener(object : ExpertAdviceView.OnExpertAdviceListener {
                override fun onCountdownFinished() {
                    // TODO: 倒计时结束，显示广告或解锁内容
//                    expertAdviceView.setMaskVisible(false)
                }

                override fun onGetTipClicked() {
                    // TODO: 点击获取提示，显示广告
                    // 示例：显示遮罩并开始倒计时

                }

                override fun onCancelClicked() {
                    // 用户取消倒计时，不需要额外处理
                }
            })
            loadNative(adContainer, style = NativeAdStyle.CARD_5)
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
            val rangeDes = resources.getStringArray(R.array.bp_level_expert_advice)
            // 设置专家建议文案
            val adviceText = String.format(rangeDes[idx], record.systolicPressure, record.diastolicPressure)
            expertAdviceView.setAdviceText(adviceText)
        }
    }

    override fun getStatusBarColor() = R.color.c5

    private fun showDeleteConfirm() {
        showDeleteConfirm {
            mViewModel.deleteRecord()
        }
    }

}