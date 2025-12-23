package com.daily.health.manager.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.HtActivityCholesterolDetailBinding
import com.daily.health.manager.ui.chart.HealthLineChartManager
import com.daily.health.manager.ui.viewmodel.CholesterolDetailViewModel
import com.daily.health.manager.ui.weight.LeveDataFactory
import com.daily.health.manager.ui.widget.ExpertAdviceView
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.ext.startActivity
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject

class CholesterolDetailActivity : BaseInterActivity<CholesterolDetailViewModel, HtActivityCholesterolDetailBinding>() {

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()
    private var chartManager: HealthLineChartManager? = null

    companion object {
        private const val RECORD_ID = "record_id"

        fun start(context: Context, recordId: Long) {
            context.startActivity<CholesterolDetailActivity>(RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() = HtActivityCholesterolDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = CholesterolDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val recordId = intent.getLongExtra(RECORD_ID, -1L)
        if (recordId == -1L) {
            finish()
            return
        }

        mViewModel.initializeWithRecord(recordId)

        chartManager = chartManagerFactory.create(mViewBind.chartView, this)

        setupActionBar()
        observeData()
    }

    private fun setupActionBar() {
        with(mViewBind) {
            btnBack.clickWithDuration {
                onBackPress()
            }

            btnEdit.clickWithDuration {
                val recordId = intent.getLongExtra(RECORD_ID, -1L)
                CholesterolRecordActivity.start(this@CholesterolDetailActivity, recordId)
            }

            btnDelete.clickWithDuration {
                showDeleteConfirm()
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

    private fun observeData() {
        collectLatest(mViewModel.cholesterolRecord) {
            if (it != null) {
                updateUI()
            }
        }

        collectLatest(mViewModel.errorMessage) { error ->
            error?.let {
                showToast(it)
                mViewModel.clearError()
            }
        }

        lifecycleScope.launch {
            mViewModel.isLoading.collect { isLoading ->
                // TODO: 显示/隐藏加载状态
            }
        }

        collectLatest(mViewModel.chartUiState) { state ->
            chartManager?.render(state)
        }
    }

    private fun updateUI() {
        with(mViewBind) {
            // 显示三个指标值（不带单位）
            tvHdlValue.text = mViewModel.getHdlValue()
            tvTcHdlValue.text = mViewModel.getTcHdlRatio()
            tvLdlHdlValue.text = mViewModel.getLdlHdlRatio()

            // 显示时间
            mViewModel.getRecordTime()?.let {
                tvTime.text = DateTimeUtils.formatDateTime(it)
            }

            // 设置状态视图
            val cholesterolLevel = mViewModel.getCholesterolLevel()
            val leveItems = LeveDataFactory.Cholesterol.buildItems(this@CholesterolDetailActivity)
            cholesterolStatusView.setLevels(leveItems)
            val index = LeveDataFactory.Cholesterol.indexFor(cholesterolLevel)
            cholesterolStatusView.setCurrentLevel(index)

            // 显示专家建议
            val adviceArray = resources.getStringArray(R.array.ht_cholesterol_level_expert_advice)
            val adviceIndex = when (cholesterolLevel) {
                com.daily.health.manager.data.enums.CholesterolLevel.UNKNOWN -> 0
                com.daily.health.manager.data.enums.CholesterolLevel.NORMAL -> 1
                com.daily.health.manager.data.enums.CholesterolLevel.NEAR_OPTIMAL -> 2
                com.daily.health.manager.data.enums.CholesterolLevel.BORDERLINE -> 3
                com.daily.health.manager.data.enums.CholesterolLevel.HIGH -> 4
                com.daily.health.manager.data.enums.CholesterolLevel.VERY_HIGH -> 5
            }
            expertAdviceView.setAdviceText(adviceArray[adviceIndex])
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
