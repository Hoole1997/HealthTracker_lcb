package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityBmiDetailBinding
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.viewmodel.BmiDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.widget.ExpertAdviceView
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.showToast
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Locale

class BmiDetailActivity: BaseInterActivity<BmiDetailViewModel, ActivityBmiDetailBinding>() {

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()
    private var chartManager: HealthLineChartManager? = null

    companion object {
        private const val TAG = "BmiDetailActivity"
        private const val EXTRA_RECORD_ID = "extra_record_id"

        fun start(context: Context, recordId: Long) {
            val intent = android.content.Intent(context, BmiDetailActivity::class.java)
            intent.putExtra(EXTRA_RECORD_ID, recordId)
            context.startActivity(intent)
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun createViewBinding() = ActivityBmiDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BmiDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // Get record ID from intent
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)

        // Initialize ViewModel with record ID
        mViewModel.initializeWithRecord(recordId)

        chartManager = chartManagerFactory.create(mViewBind.chartView, this)

        setupViews()
        observeData()
    }

    private fun setupViews() {
        with(mViewBind) {
            // Back button
            btnBack.clickWithDuration {
                onBackPress()
            }

            // Edit button
            btnEdit.clickWithDuration {
                mViewModel.bmiRecord.value?.let {
                    BmiRecordActivity.start(this@BmiDetailActivity,it.id)
                }

            }

            // Delete button
            btnDelete.clickWithDuration {
                showDeleteConfirmDialog()
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
        // Observe BMI record data
        this.collectLatest(mViewModel.bmiRecord) { record ->
            if (record != null) {
                "BMI record updated: $record".logd(TAG)
                updateUI()
            }
        }

        // Observe loading state
        this.collectLatest(mViewModel.isLoading) { isLoading ->
            // TODO: Show/hide loading indicator if needed
            "Loading state: $isLoading".logd(TAG)
        }

        // Observe error state
        this.collectLatest(mViewModel.error) { error ->
            if (error != null) {
                showToast(error)
                mViewModel.clearError()
            }
        }

        this.collectLatest(mViewModel.chartUiState) { state ->
            chartManager?.render(state)
        }
    }

    private fun updateUI() {
        with(mViewBind) {
            // Display weight value (converted to preferred unit, without unit label)
            val displayWeight = mViewModel.getDisplayWeight()
            tvWeightValue.text = displayWeight

            // Display height value (converted to preferred unit, without unit label)
            val displayHeight = mViewModel.getDisplayHeight()
            tvHeightValue.text = displayHeight

            // Calculate and display BMI value (always unitless)
            val bmi = mViewModel.calculateBmi()
            tvBmiValue.text = bmi?.let { String.format("%.1f", it) } ?: "--"

            // Display record time
            val recordTime = mViewModel.getRecordTime()
            tvTime.text = recordTime?.let { dateFormat.format(it) } ?: ""

            // Update BMI status view
            updateBmiStatusView(bmi)

            // Display expert advice
            updateExpertAdvice()
        }
    }

    private fun updateBmiStatusView(bmi: Float?) {
        if (bmi == null) return

        with(mViewBind) {
            // Get BMI level items from factory
            val bmiItems = LeveDataFactory.BMI.buildItems(this@BmiDetailActivity)

            // Get current BMI category index
            val bmiCategory = mViewModel.getBmiCategory()
            val currentIndex =  LeveDataFactory.BMI.indexFor(bmi)

            "BMI: $bmi, Category: $bmiCategory, Index: $currentIndex".logd(TAG)

            // Set items and current index
            bpStatusView.setLevels(bmiItems)
            bpStatusView.setCurrentLevel(currentIndex)
        }
    }

    private fun updateExpertAdvice() {
        val bmiCategory = mViewModel.getBmiCategory()
        if (bmiCategory == null) {
            mViewBind.expertAdviceView.setAdviceText("")
            return
        }

        // Get expert advice from resources based on BMI category
        val adviceArray = resources.getStringArray(R.array.bmi_level_expert_advice)
        val categoryIndex = LeveDataFactory.BMI.indexFor(mViewModel.calculateBmi() ?: 0f)

        if (categoryIndex in adviceArray.indices) {
            val adviceText = adviceArray[categoryIndex]
            mViewBind.expertAdviceView.setAdviceText(adviceText)
        } else {
            mViewBind.expertAdviceView.setAdviceText("")
        }

        "Displaying advice for category: $bmiCategory (index: $categoryIndex)".logd(TAG)
    }

    private fun showDeleteConfirmDialog() {
        showDeleteConfirm {
            mViewModel.deleteRecord()
        }
    }

    override fun getStatusBarColor() = R.color.c5

    override fun hideMask() {
        mViewBind.expertAdviceView.setMaskVisible(false)
    }
}
