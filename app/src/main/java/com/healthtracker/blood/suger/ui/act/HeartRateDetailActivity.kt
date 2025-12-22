package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.enums.HeartRateStatus
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.HtActivityHeartRateDetailBinding
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.viewmodel.HeartRateDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.widget.ExpertAdviceView
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.ext.startActivity
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject

class HeartRateDetailActivity :
    BaseInterActivity<HeartRateDetailViewModel, HtActivityHeartRateDetailBinding>() {

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()
    private var chartManager: HealthLineChartManager? = null

    companion object {
        fun start(context: Context, recordId: Long) {
            context.startActivity<HeartRateDetailActivity>(
                HeartRateDetailViewModel.RECORD_ID to recordId
            )
        }
    }

    override fun createViewBinding(): HtActivityHeartRateDetailBinding =
        HtActivityHeartRateDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HeartRateDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 创建chartManager时传入lifecycleOwner，自动绑定生命周期
        chartManager = chartManagerFactory.create(mViewBind.chartView, this)
        with(mViewBind){
            setupActionBar()
            setupStatusView()
        }

        observeViewModel()
    }

    private fun setupActionBar() {
        with(mViewBind) {
            btnBack.clickWithDuration { onBackPress() }
            btnDelete.click {
                showDeleteConfirm()
            }

            btnEdit.click {
                mViewModel.currentRecordId()?.let { id ->
                    HeartRateRecordActivity.start(this@HeartRateDetailActivity, id)
                } ?: showToast(getString(R.string.ht_record_not_ready))
            }

            // 设置专家建议控件监听器
            expertAdviceView.setOnExpertAdviceListener(object : ExpertAdviceView.OnExpertAdviceListener {
                override fun onCountdownFinished() {
                    showReword()
                }

                override fun onGetTipClicked() {
                    showReword()
                }

                override fun onCancelClicked() {
                    // 用户取消倒计时
                }
            })
            loadNative(adContainer, style = NativeAdStyle.CARD_5)
        }
    }

    private fun setupStatusView() {
        val levels = LeveDataFactory.HeartRate.buildItems(this)
        mViewBind.bpmStatusView.apply {
            setLevels(levels)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            collectLatest(mViewModel.record) { record ->
                if (record != null) {
                    updateRecord(record)
                }
            }
        }

        lifecycleScope.launch {
            collectLatest(mViewModel.status) { status ->
                status?.let { updateStatus(it) }
            }
        }

        lifecycleScope.launch {
            collectLatest(mViewModel.isLoading) { loading ->

            }
        }

        lifecycleScope.launch {
            collectLatest(mViewModel.tags) { tags ->
                updateTags(tags.take(2))
            }
        }

        lifecycleScope.launch {
            collectLatest(mViewModel.error) { error ->
                error?.let {
                    showToast(it)
                    mViewModel.clearError()
                }
            }
        }

        lifecycleScope.launch {
            collectLatest(mViewModel.chartUiState) { state ->
                chartManager?.render(state)
            }
        }
    }

    private fun updateRecord(record: HeartRateRecord) {
        mViewBind.tvBpmValue.text = record.heartRateBpm.toString()
        mViewBind.tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)
        val index = LeveDataFactory.HeartRate.indexFor(record.heartRateBpm)
        mViewBind.bpmStatusView.setCurrentLevel(index)
        val desArray = resources.getStringArray(R.array.ht_hr_level_expert_advice)
        mViewBind.expertAdviceView.setAdviceText(desArray[index])
    }

    private fun updateStatus(status: HeartRateStatus) {
        mViewBind.bpmStatusView.setCurrentLevel(
            LeveDataFactory.HeartRate.indexFor(status)
        )
    }

    private fun updateTags(tags: List<HealthTag>) {
        mViewBind.tvTags.text = if (tags.isEmpty()) {
            getString(R.string.ht_heart_rate_no_tags)
        } else {
            tags.joinToString(" · ") { it.name }
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

}
