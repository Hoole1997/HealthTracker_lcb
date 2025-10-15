package com.healthtracker.blood.suger.ui.act

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.CholesterolLevel
import com.healthtracker.blood.suger.databinding.ActivityCholesterolRecordBinding
import com.healthtracker.blood.suger.ui.dialog.LevelExplainDialog
import com.healthtracker.blood.suger.ui.viewmodel.CholesterolRecordViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.util.CholesterolMetrics
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.util.FontUtils
import com.healthtracker.framework.ext.showToast
import com.healthtracker.blood.suger.ui.widget.NumberPickerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * 胆固醇记录页面
 */
@AndroidEntryPoint
class CholesterolRecordActivity :
    BaseMVVMActivity<CholesterolRecordViewModel, ActivityCholesterolRecordBinding>() {

    companion object {
        private const val EXTRA_RECORD_ID = "extra_record_id"

        private const val HDL_MIN = 1
        private const val HDL_MAX = 200

        private const val LDL_MIN = 1
        private const val LDL_MAX = 200

        private const val TG_MIN = 1
        private const val TG_MAX = 200

        fun start(context: android.content.Context, recordId: Long? = null) {
            val intent = Intent(context, CholesterolRecordActivity::class.java)
            recordId?.let { intent.putExtra(EXTRA_RECORD_ID, it) }
            context.startActivity(intent)
        }
    }

    override fun createViewBinding(): ActivityCholesterolRecordBinding =
        ActivityCholesterolRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = CholesterolRecordViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L).let { if (it == -1L) null else it }
        mViewModel.initialize(recordId)
        setupActionBar()
        setupNumberPickers()
        setupLeveStatusView()
        setupDateTimeSection()
        observeViewModel()
    }

    private fun setupActionBar() {
        mViewBind.btnBack.click { finish() }
        mViewBind.btnSave.click {
            lifecycleScope.launch {
                mViewModel.updateRecordTime(mViewBind.dateTimeSelectionView.getSelectDate())
                when (val result = mViewModel.saveRecord()) {
                    is CholesterolRecordViewModel.SaveResult.Created,
                    is CholesterolRecordViewModel.SaveResult.Updated -> finish()
                    is CholesterolRecordViewModel.SaveResult.Failed -> {
                        showToast(result.error)
                    }
                }
            }
        }
    }

    private fun setupNumberPickers() {
        val tfRegular = FontUtils.getInstance().robotoRegular
        val tfBold = FontUtils.getInstance().robotoBold

        fun NumberPickerView.applyTypography() {
            setContentNormalTextTypeface(tfRegular)
            setContentSelectedTextTypeface(tfBold)
        }

        mViewBind.npvHdl.apply {
            applyTypography()
            setupRange(HDL_MIN, HDL_MAX)
            setOnValueChangedListener { _, _, _ ->
                val value = contentByCurrValue.toIntOrNull()
                value?.let { mViewModel.updateHdl(it) }
            }
        }

        mViewBind.npvLdl.apply {
            applyTypography()
            setupRange(LDL_MIN, LDL_MAX)
            setOnValueChangedListener { _, _, _ ->
                val value = contentByCurrValue.toIntOrNull()
                value?.let { mViewModel.updateLdl(it) }
            }
        }

        mViewBind.npvTg.apply {
            applyTypography()
            setupRange(TG_MIN, TG_MAX)
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
                getString(R.string.saving)
            } else {
                getString(R.string.save)
            }
        }

        collectLatest(mViewModel.isLoading) { loading ->
            mViewBind.btnSave.isEnabled = !loading && !mViewModel.isSaving.value
        }
    }

    private fun updateMetrics(metrics: CholesterolMetrics) {
        with(mViewBind){
            val totalValue = metrics.totalCholesterol?.toInt()?.toString() ?: "0"
            tvTc.text = getString(R.string.total_cholesterol,totalValue)
        }
        updateLsvCurrentIndex(metrics.riskLevel)

    }


    private fun NumberPickerView.setupRange(min: Int, max: Int) {
        val values = Array(max - min + 1) { index -> (min + index).toString() }
        displayedValues = values
        minValue = min
        maxValue = max
        value = min
    }

    /**
     * 构建并设置 LeveStatusView 的等级列表与初始索引
     */
    private fun setupLeveStatusView() {
        val levels = LeveDataFactory.Cholesterol.buildItems(this)
        mViewBind.lsvStatus.setLevels(levels)


        // 在记录页开启范围说明点击，弹出通用等级说明对话框
        mViewBind.lsvStatus.setExplainClickable(true)
        mViewBind.lsvStatus.setOnExplainClick {
            val items = ArrayList(LeveDataFactory.Cholesterol.buildExplainItems(this))
            LevelExplainDialog.show(
                supportFragmentManager,
                des = getString(R.string.bp_range_des),
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
}
