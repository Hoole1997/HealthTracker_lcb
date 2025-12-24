package com.daily.health.manager.ui.act

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.enums.CholesterolLevel
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.HtActivityCholesterolRecordBinding
import com.daily.health.manager.databinding.HtLayoutCholesterolDetailValueBinding
import com.daily.health.manager.ui.dialog.LevelExplainDialog
import com.daily.health.manager.ui.dialog.SaveCompleteDialog
import com.daily.health.manager.ui.viewmodel.CholesterolRecordViewModel
import com.daily.health.manager.ui.act.HealthDetailActivity.DetailType
import com.daily.health.manager.ui.weight.LeveDataFactory
import com.daily.health.manager.ui.widget.NumberPickerView
import com.daily.health.manager.util.CholesterolMetrics
import com.daily.health.manager.utils.loadNative
import com.daily.health.manager.utils.showInter
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.daily.health.manager.ui.tracker.HealthType
import com.daily.health.manager.ui.tracker.trackAddNewRecord
import com.healthtracker.framework.util.getRobotoBold
import com.healthtracker.framework.util.getRobotoRegular
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import java.util.Calendar
import java.util.Locale

/**
 * 胆固醇记录页面
 */
class CholesterolRecordActivity :
    BaseInterActivity<CholesterolRecordViewModel, HtActivityCholesterolRecordBinding>() {

    companion object {
        private const val EXTRA_RECORD_ID = "extra_record_id"


        private const val MIN = 1
        private const val MAX = 220

        fun start(context: android.content.Context, recordId: Long? = null) {
            val intent = Intent(context, CholesterolRecordActivity::class.java)
            recordId?.let { intent.putExtra(EXTRA_RECORD_ID, it) }
            context.startActivity(intent)
        }
    }

    private lateinit var cholesterolDetailBinding: HtLayoutCholesterolDetailValueBinding

    override fun createViewBinding(): HtActivityCholesterolRecordBinding =
        HtActivityCholesterolRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = CholesterolRecordViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L).let { if (it == -1L) null else it }
        mViewModel.initialize(recordId)
        recordId?.let {
            mViewBind.actionBar.tvTitle.text = getString(R.string.ht_edit_record)
        }
        setupActionBar()
        setupNumberPickers()
        setupLeveStatusView()
        setupDateTimeSection()
        observeViewModel()
        loadNative(mViewBind.adContainer, style = NativeAdStyle.STANDARD)
    }

    private fun setupActionBar() {
        mViewBind.actionBar.btnBack.clickWithDuration { onBackPress() }
        mViewBind.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.CHOLESTEROL)
            lifecycleScope.launch {
                mViewModel.updateRecordTime(mViewBind.dateTimeSelectionView.getSelectDate())
                when (val result = mViewModel.saveRecord()) {
                    is CholesterolRecordViewModel.SaveResult.Created -> {
                        goDetail(result.recordId)
                    }
                    is CholesterolRecordViewModel.SaveResult.Updated -> {
                       goDetail(result.recordId)

                    }
                    is CholesterolRecordViewModel.SaveResult.Failed -> {
                        showToast(result.error)
                    }
                }
            }
        }
    }

    private fun goDetail(recordId:Long){
        SaveCompleteDialog.show(supportFragmentManager){
           showInter {
               HealthDetailActivity.start(this@CholesterolRecordActivity, DetailType.CHOLESTEROL, recordId)
               finish()
           }
        }
    }

    private fun setupNumberPickers() {
        val tfRegular = getRobotoRegular(this)
        val tfBold = getRobotoBold(this)

        fun NumberPickerView.applyTypography() {
            setContentNormalTextTypeface(tfRegular)
            setContentSelectedTextTypeface(tfBold)
        }

        mViewBind.npvHdl.apply {
            applyTypography()
            setupRange()
            setOnValueChangedListener { _, _, _ ->
                val value = contentByCurrValue.toIntOrNull()
                value?.let { mViewModel.updateHdl(it) }
            }
        }

        mViewBind.npvLdl.apply {
            applyTypography()
            setupRange()
            setOnValueChangedListener { _, _, _ ->
                val value = contentByCurrValue.toIntOrNull()
                value?.let { mViewModel.updateLdl(it) }
            }
        }

        mViewBind.npvTg.apply {
            applyTypography()
            setupRange()
            setOnValueChangedListener { _, _, _ ->
                val value = contentByCurrValue.toIntOrNull()
                value?.let { mViewModel.updateTriglyceride(it) }
            }
        }
    }


    private fun setupDateTimeSection() {
        mViewBind.dateTimeSelectionView.apply {
            setLabelVisible(false)
        }
    }

    private fun observeViewModel() {
        collectLatest(mViewModel.metrics) { metrics ->
            updateMetrics(metrics)
        }


        collectLatest(mViewModel.recordTime) { date ->
            val calendar = Calendar.getInstance().apply { time = date }
            mViewBind.dateTimeSelectionView.getDateTimePicker().initView(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE)
            )
        }

        collectLatest(mViewModel.isSaving) { saving ->
            mViewBind.btnSave.isEnabled = !saving
            mViewBind.btnSave.text = if (saving) {
                getString(R.string.ht_saving)
            } else {
                getString(R.string.ht_save)
            }
        }

        collectLatest(mViewModel.isLoading) { loading ->
            mViewBind.btnSave.isEnabled = !loading && !mViewModel.isSaving.value
        }

        collect(mViewModel.hdl){
            mViewBind.npvHdl.value = it - 1
        }

        collect(mViewModel.ldl){
            mViewBind.npvLdl.value = it - 1
        }

        collect(mViewModel.triglyceride){
            mViewBind.npvTg.value = it - 1
        }
    }

    private fun updateMetrics(metrics: CholesterolMetrics) {
        with(mViewBind){
            val totalValue = metrics.totalCholesterol?.let { formatCholesterolValue(it) } ?: "--"
            tvTc.text = getString(R.string.ht_total_cholesterol,totalValue)
        }
        if (::cholesterolDetailBinding.isInitialized) {
            cholesterolDetailBinding.tvNonHdlValue.text =
                metrics.nonHdl?.let { formatCholesterolValue(it) } ?: "--"
            cholesterolDetailBinding.tvTcHdlValue.text =
                metrics.tcHdlRatio?.let { formatRatioValue(it) } ?: "--"
            cholesterolDetailBinding.tvLdlHdlValue.text =
                metrics.ldlHdlRatio?.let { formatRatioValue(it) } ?: "--"
        }
        updateLsvCurrentIndex(metrics.riskLevel)

    }


    private fun NumberPickerView.setupRange() {
        val values = Array(200) { i -> DateTimeUtils.formatTwoDigit(i + 1) }
        displayedValues = values
        minValue = 0
        maxValue = 199
    }

    /**
     * 构建并设置 LeveStatusView 的等级列表与初始索引
     */
    private fun setupLeveStatusView() {
        val levels = LeveDataFactory.Cholesterol.buildItems(this)
        mViewBind.lsvStatus.setLevels(levels)

        cholesterolDetailBinding = HtLayoutCholesterolDetailValueBinding.inflate(layoutInflater)
        mViewBind.lsvStatus.setExtraView(cholesterolDetailBinding.root)

        // 在记录页开启范围说明点击，弹出通用等级说明对话框
        mViewBind.lsvStatus.setExplainClickable(true)
        mViewBind.lsvStatus.setOnExplainClick {
            val items = ArrayList(LeveDataFactory.Cholesterol.buildExplainItems(this))
            LevelExplainDialog.show(
                supportFragmentManager,
                des = getString(R.string.ht_cholesterol_range_des),
                items = items
            )
        }
    }

    /**
     * 根据当前收缩压/舒张压计算分类并更新 LeveStatusView 的 currentIndex
     */
    private fun updateLsvCurrentIndex(riskLevel: CholesterolLevel) {
        val idx = LeveDataFactory.Cholesterol.indexFor(riskLevel)
        mViewBind.lsvStatus.setCurrentLevel(idx)
    }

    private fun formatCholesterolValue(value: Float): String {
        // 使用单个小数位保留计算结果，提升动态计算的可读性
        return String.format(Locale.getDefault(), "%.0f", value)
    }

    private fun formatRatioValue(value: Float): String {
        return String.format(Locale.getDefault(), "%.2f", value)
    }
}
