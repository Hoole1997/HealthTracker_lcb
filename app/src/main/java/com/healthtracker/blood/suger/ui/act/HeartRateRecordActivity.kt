package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.databinding.ActivityHeartRateRecordBinding
import com.healthtracker.blood.suger.ui.dialog.HealthTagDialog
import com.healthtracker.blood.suger.ui.dialog.LevelExplainDialog
import com.healthtracker.blood.suger.ui.dialog.SaveCompleteDialog
import com.healthtracker.blood.suger.ui.viewmodel.HeartRateRecordViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.blood.suger.utils.showInter
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.util.FontUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import java.util.ArrayList
import java.util.Calendar
import kotlin.collections.indexOf

/**
 * 心率记录页面
 */
@AndroidEntryPoint
class HeartRateRecordActivity :
    BaseInterActivity<HeartRateRecordViewModel, ActivityHeartRateRecordBinding>() {

    private val healthTags = mutableListOf<HealthTag>()
    private val addTagIds = mutableListOf<Long>()
    private var latestHeartRate: Int = DEFAULT_HEART_RATE

    companion object {
        private const val EXTRA_RECORD_ID = "extra_record_id"
        private const val MIN_HEART_RATE = 40
        private const val MAX_HEART_RATE = 220
        private const val DEFAULT_HEART_RATE = 70

        fun start(context: android.content.Context, recordId: Long? = null) {
            val intent = android.content.Intent(context, HeartRateRecordActivity::class.java)
            recordId?.let { intent.putExtra(EXTRA_RECORD_ID, it) }
            context.startActivity(intent)
        }
    }

    override fun createViewBinding() = ActivityHeartRateRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HeartRateRecordViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L).let { if (it == -1L) null else it }
        mViewModel.initialize(recordId)
        recordId?.let {
            mViewBind.tvTitle.text = getString(R.string.edit_record)
        }
        setupAction()
        setupNumberPicker()
        setupStatusView()
        setupDateTimeView()
        observeViewModel()
        loadNative(mViewBind.adContainer, style = NativeAdStyle.STANDARD)
    }

    private fun setupAction() {
        mViewBind.btnBack.clickWithDuration { onBackPress() }
        mViewBind.btnSave.clickWithDuration {
            lifecycleScope.launch {
                mViewModel.updateRecordTime(mViewBind.dateTimeSelectionView.getSelectDate())
                mViewModel.saveHeartRateRecord { result: HeartRateRecordViewModel.SaveRecordResult ->
                    when (result) {
                        is HeartRateRecordViewModel.SaveRecordResult.Created -> {
                            goDetail(result.recordId)
                        }
                        is HeartRateRecordViewModel.SaveRecordResult.Updated -> {
                            goDetail(result.recordId)
                        }
                        is HeartRateRecordViewModel.SaveRecordResult.Failed -> {
                            showToast(result.error)
                        }
                    }
                }
            }
        }
    }

    private fun goDetail(recordId:Long){
        SaveCompleteDialog.show(supportFragmentManager){
            showInter {
                HeartRateDetailActivity.start(this@HeartRateRecordActivity,recordId)
                finish()
            }
        }
    }

    private fun setupNumberPicker() {
        with(mViewBind.npvHeartRate) {
            val tfRegular = FontUtils.getInstance().robotoRegular
            val tfBold = FontUtils.getInstance().robotoBold
            setContentNormalTextTypeface(tfRegular)
            setContentSelectedTextTypeface(tfBold)

            val values = Array(MAX_HEART_RATE - MIN_HEART_RATE + 1) { index ->
                (MIN_HEART_RATE + index).toString()
            }
            displayedValues = values
            minValue = MIN_HEART_RATE
            maxValue = MAX_HEART_RATE
            value = displayedValues.indexOf((latestHeartRate + MIN_HEART_RATE).toString())
            setOnValueChangedListener { _, _, _ ->
                val bpm = contentByCurrValue.toIntOrNull() ?: DEFAULT_HEART_RATE
                mViewModel.updateHeartRate(bpm)
            }
        }
    }

    private fun setupStatusView() {
        val levels = LeveDataFactory.HeartRate.buildItems(this)
        mViewBind.lsvStatus.setLevels(levels)
        mViewBind.lsvStatus.setOnExplainClick{
            val items = ArrayList(LeveDataFactory.HeartRate.buildExplainItems(this))
            LevelExplainDialog.show(supportFragmentManager, items)
        }
    }

    private fun setupDateTimeView() {
        mViewBind.dateTimeSelectionView.apply {
            setTitleText(getString(R.string.date_time))
            setLabelText(getString(R.string.label))
            setOnLabelClickListener {
                val selected = if (addTagIds.isEmpty()) null else {
                    healthTags.filter { tag -> addTagIds.contains(tag.id) }
                }
                HealthTagDialog.showHeartRateDialog(
                    fragmentManager = supportFragmentManager,
                    tagsFlow = mViewModel.getHeartRateTagsFlow(),
                    selectedTags = selected,
                    onSave = { selectedTags ->
                        val ids = selectedTags.map { it.id }
                        addTagIds.clear()
                        addTagIds.addAll(ids)
                        mViewModel.clearSelectedTags()
                        ids.forEach { mViewModel.addTag(it) }
                    },
                    onDelete = { tag -> mViewModel.deleteTag(tag) },
                    onAdd = { name ->
                        lifecycleScope.launch {
                            val newId = mViewModel.createCustomTag(name)
                            if (newId <= 0L) {
                                showToast(getString(R.string.create_label_failed))
                            }
                        }
                    }
                )
            }
        }
    }

    private fun observeViewModel() {
        collect(mViewModel.heartRate) { bpm ->
            latestHeartRate = bpm
            val picker = mViewBind.npvHeartRate
            val currentValue = picker.contentByCurrValue.toIntOrNull()
            if (currentValue != bpm) {
                picker.value = bpm
            }
            updateLevelIndex(bpm)
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

        collectLatest(mViewModel.isLoading) { loading ->
            mViewBind.btnSave.isEnabled = !loading
            mViewBind.btnSave.text = if (loading) getString(R.string.saving) else getString(R.string.save)
        }

        collectLatest(mViewModel.availableTags) { tags ->
            healthTags.clear()
            healthTags.addAll(tags)
        }

        collectLatest(mViewModel.selectedTagIds) { tagIds ->
            addTagIds.clear()
            addTagIds.addAll(tagIds)
        }
    }


    private fun updateLevelIndex(bpm: Int) {
        val index = LeveDataFactory.HeartRate.indexFor(bpm)
        mViewBind.lsvStatus.setCurrentLevel(index)
    }


}
