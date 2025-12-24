package com.daily.health.manager.ui.act

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.entity.HealthTag
import com.daily.health.manager.databinding.HtActivityHeartRateRecordBinding
import com.daily.health.manager.ui.dialog.HealthTagDialog
import com.daily.health.manager.ui.dialog.LevelExplainDialog
import com.daily.health.manager.ui.dialog.SaveCompleteDialog
import com.daily.health.manager.ui.viewmodel.HeartRateRecordViewModel
import com.daily.health.manager.ui.act.HealthDetailActivity.DetailType
import com.daily.health.manager.ui.weight.LeveDataFactory
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

/**
 * 心率记录页面
 */
class HeartRateRecordActivity :
    BaseInterActivity<HeartRateRecordViewModel, HtActivityHeartRateRecordBinding>() {

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

    override fun createViewBinding() = HtActivityHeartRateRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HeartRateRecordViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L).let { if (it == -1L) null else it }
        mViewModel.initialize(recordId)
        recordId?.let {
            mViewBind.actionBar.tvTitle.text = getString(R.string.ht_edit_record)
        }
        setupAction()
        setupNumberPicker()
        setupStatusView()
        setupDateTimeView()
        observeViewModel()
        loadNative(mViewBind.adContainer, style = NativeAdStyle.STANDARD)
    }

    private fun setupAction() {
        mViewBind.actionBar.btnBack.clickWithDuration { onBackPress() }
        mViewBind.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.HEART_RATE)
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
                HealthDetailActivity.start(this@HeartRateRecordActivity, DetailType.HEART_RATE, recordId)
                finish()
            }
        }
    }

    private fun setupNumberPicker() {
        with(mViewBind.npvHeartRate) {
            val tfRegular = getRobotoRegular(this@HeartRateRecordActivity)
            val tfBold = getRobotoBold(this@HeartRateRecordActivity)
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
            setTitleText(getString(R.string.ht_date_time))
            setLabelText(getString(R.string.ht_label))
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
                                showToast(getString(R.string.ht_create_label_failed))
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
            mViewBind.btnSave.text = if (loading) getString(R.string.ht_saving) else getString(R.string.ht_save)
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
