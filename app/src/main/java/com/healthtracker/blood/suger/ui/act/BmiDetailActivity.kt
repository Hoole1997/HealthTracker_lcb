package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityBmiDetailBinding
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog
import com.healthtracker.blood.suger.ui.viewmodel.BmiDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.widget.ExpertAdviceView
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class BmiDetailActivity: BaseMVVMActivity<BmiDetailViewModel, ActivityBmiDetailBinding>() {

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

        setupViews()
        observeData()
    }

    private fun setupViews() {
        with(mViewBind) {
            // Back button
            btnBack.click {
                finish()
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
                    // TODO: 倒计时结束，显示广告或解锁内容
                }

                override fun onGetTipClicked() {
                    // TODO: 点击获取提示，显示广告
                }

                override fun onCancelClicked() {
                    // 用户取消倒计时
                }
            })
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

    override fun getStatusBarColor() = R.color.c5
}