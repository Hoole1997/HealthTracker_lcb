package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import eightbitlab.com.blurview.RenderScriptBlur
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityBpDetailBinding
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog
import com.healthtracker.blood.suger.ui.viewmodel.BpDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.showToast
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
            btnDelete.clickWithDuration {
                showDeleteConfirm()
            }

            btnEdit.clickWithDuration {
                mViewModel.bloodPressureRecord.value?.let {
                    BpRecordActivity.start(this@BpDetailActivity,it.id)
                }
            }

            // 初始化毛玻璃模糊效果
            setupBlurEffect()
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
            // 设置等级描述文案
            tvLeveDes.text = Html.fromHtml(String.format(rangeDes[idx],record.systolicPressure,record.diastolicPressure))
        }
    }

    override fun getStatusBarColor() = R.color.c5

    private fun showDeleteConfirm() {
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

    /**
     * 设置毛玻璃模糊效果
     *
     * 配置说明：
     * - blurRadius: 模糊半径 (10-25 推荐, 默认 20)
     * - overlayColor: 叠加颜色 (#40FFFFFF = 25% 白色)
     * - blurAutoUpdate: 自动更新模糊效果
     */
    private fun setupBlurEffect() {
        try {
            with(mViewBind){
                blurView.apply {
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                    clipToOutline = true
                    setupWith(window.decorView as ViewGroup)
                        .setFrameClearDrawable(window.decorView.background)
                        .setBlurRadius(5f)
                }
            }
        } catch (e: Exception) {
            e.toString().loge()
        }
    }
}