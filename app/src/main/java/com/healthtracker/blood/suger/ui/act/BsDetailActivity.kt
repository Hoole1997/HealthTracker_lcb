package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityBsDetailBinding
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.viewmodel.BsDetailViewModel
import com.healthtracker.blood.suger.ui.widget.ExpertAdviceView
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.ext.startActivity
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject

class BsDetailActivity: BaseInterActivity<BsDetailViewModel, ActivityBsDetailBinding>() {

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()
    private var chartManager: HealthLineChartManager? = null


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
            chartManager = chartManagerFactory.create(chartView, this@BsDetailActivity)
            btnBack.clickWithDuration {
                onBackPress()
            }
            btnDelete.clickWithDuration {
                showDeleteConfirm()

            }

            btnEdit.clickWithDuration {
                BsRecordActivity.start(this@BsDetailActivity,recordId)
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

        // 观察数据变化
        observeData()
    }

    private fun observeData() {

        collectLatest(mViewModel.bloodSugarRecord){
            if (it != null) {
                updateUI()
            }
        }

        collectLatest(mViewModel.error){error ->
            error?.let {
                showToast(it)
                mViewModel.clearError()
            }
        }


        lifecycleScope.launch {
            // 观察加载状态
            mViewModel.isLoading.collect { isLoading ->
                // TODO: 显示/隐藏加载状态
            }
        }

        collectLatest(mViewModel.tags){
            updateTags(it.take(2))
        }

        collectLatest(mViewModel.chartUiState) { state ->
            chartManager?.render(state)
        }
    }

    private fun updateUI() {
        with(mViewBind) {
            // 更新血糖状态视图
            val status = mViewModel.getBloodSugarStatus()
            val unit = mViewModel.getDisplayUnit()
            val value = mViewModel.getDisplayValue()

            tvBsValue.text = value.toString()
            tvBsValueUnit.text = unit?.displayName ?: ""
            mViewModel.getRecordTime()?.let {
                tvTime.text = DateTimeUtils.formatDateTime(it)
            }


            if (status != null && unit != null && value != null) {
                // 使用通用 LeveStatusView：设置等级列表与当前索引
                val levels = LeveDataFactory.BloodSugar.buildItems(this@BsDetailActivity, unit, status)
                bsStatusView.setLevels(levels)
                val index = LeveDataFactory.BloodSugar.indexFor(value, unit, status)
                bsStatusView.setCurrentLevel(index)

                val leveDescription = resources.getStringArray(R.array.bs_level_expert_advice)[index]
                expertAdviceView.setAdviceText(leveDescription)
            }

            // 更新等级描述文案
            status?.let {

            }
        }
    }

    private fun updateTags(tags: List<com.healthtracker.blood.suger.data.entity.HealthTag>) {
        mViewBind.tvTags.text = if (tags.isEmpty()) {
            getString(R.string.heart_rate_no_tags)
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
