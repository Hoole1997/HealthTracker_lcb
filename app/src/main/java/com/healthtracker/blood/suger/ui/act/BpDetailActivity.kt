package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.HtActivityBpDetailBinding
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.viewmodel.BpDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.widget.ExpertAdviceView
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.startActivity
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject

class BpDetailActivity: BaseInterActivity<BpDetailViewModel, HtActivityBpDetailBinding>() {

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()
    private var chartManager: HealthLineChartManager? = null

    companion object{
        private const val RECORD_ID = "record_id"
        // 启动详情页面
        fun start(context: Context, recordId: Long) {
            context.startActivity<BpDetailActivity>(RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() = HtActivityBpDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BpDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 创建chartManager时传入lifecycleOwner，自动绑定生命周期
        chartManager = chartManagerFactory.create(mViewBind.chartView, this)


        
        with(mViewBind){
            btnBack.clickWithDuration {
                handleBackPress()
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
                    showReword()
                }

                override fun onGetTipClicked() {
                    // TODO: 点击获取提示，显示广告
                    showReword()

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
        collectLatest(mViewModel.bloodPressureRecord) { record ->
            record?.let { updateUI(it) }
        }

        collectLatest(mViewModel.error) { error ->
            error?.let {
                // TODO: 显示错误信息
            }
        }

        collectLatest(mViewModel.isLoading) { isLoading ->
            // TODO: 显示/隐藏加载状态
        }

        collectLatest(mViewModel.chartUiState) { state ->
            chartManager?.render(state)
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




    override fun hideMask() {
        mViewBind.expertAdviceView.setMaskVisible(false)
    }

    override fun handleBackPress(): Boolean {
        // 停止倒计时，防止插页广告关闭后激励广告被重新触发
        mViewBind.expertAdviceView.stopCountdown()
        return super.handleBackPress()
    }
}
