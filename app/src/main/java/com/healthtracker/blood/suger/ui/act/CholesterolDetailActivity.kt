package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityCholesterolDetailBinding
import com.healthtracker.blood.suger.ui.viewmodel.CholesterolDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.ui.widget.ExpertAdviceView
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle

@AndroidEntryPoint
class CholesterolDetailActivity : BaseInterActivity<CholesterolDetailViewModel, ActivityCholesterolDetailBinding>() {

    companion object {
        private const val RECORD_ID = "record_id"

        fun start(context: Context, recordId: Long) {
            context.startActivity<CholesterolDetailActivity>(RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() = ActivityCholesterolDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = CholesterolDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val recordId = intent.getLongExtra(RECORD_ID, -1L)
        if (recordId == -1L) {
            finish()
            return
        }

        mViewModel.initializeWithRecord(recordId)

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
                    // TODO: 倒计时结束，显示广告或解锁内容
                }

                override fun onGetTipClicked() {
                    // TODO: 点击获取提示，显示广告
                }

                override fun onCancelClicked() {
                    // 用户取消倒计时
                }
            })
            loadNative(adContainer, style = NativeAdStyle.CARD)
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
            val adviceArray = resources.getStringArray(R.array.cholesterol_level_expert_advice)
            val adviceIndex = when (cholesterolLevel) {
                com.healthtracker.blood.suger.data.enums.CholesterolLevel.UNKNOWN -> 0
                com.healthtracker.blood.suger.data.enums.CholesterolLevel.NORMAL -> 1
                com.healthtracker.blood.suger.data.enums.CholesterolLevel.NEAR_OPTIMAL -> 2
                com.healthtracker.blood.suger.data.enums.CholesterolLevel.BORDERLINE -> 3
                com.healthtracker.blood.suger.data.enums.CholesterolLevel.HIGH -> 4
                com.healthtracker.blood.suger.data.enums.CholesterolLevel.VERY_HIGH -> 5
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
}
