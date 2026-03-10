package com.daily.health.manager.face.act

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.constants.BodyMetricsDefaults
import com.daily.health.manager.data.entity.HealthTag
import com.daily.health.manager.data.entity.HeartRateRecord
import com.daily.health.manager.data.enums.BmiUnit
import com.daily.health.manager.data.enums.CholesterolLevel
import com.daily.health.manager.data.enums.BsUnit
import com.daily.health.manager.data.enums.TagType
import com.daily.health.manager.data.enums.getStatusStringRes
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.TrActivityBsRecordBinding
import com.daily.health.manager.databinding.TrActivityBmiRecordBinding
import com.daily.health.manager.databinding.TrActivityBpRecordBinding
import com.daily.health.manager.databinding.TrActivityCholesterolRecordBinding
import com.daily.health.manager.databinding.TrActivityHeartRateRecordBinding
import com.daily.health.manager.databinding.TrActivityHealthRecordBinding
import com.daily.health.manager.databinding.TrLayoutCholesterolDetailValueBinding
import com.daily.health.manager.face.dialog.BmiPickerDialog
import com.daily.health.manager.face.dialog.HealthTagDialog
import com.daily.health.manager.face.dialog.LevelExplainDialog
import com.daily.health.manager.face.dialog.SaveCompleteDialog
import com.daily.health.manager.face.dialog.StatusSelectDialog
import com.daily.health.manager.face.tracker.HealthType
import com.daily.health.manager.face.tracker.trackAddNewRecord
import com.daily.health.manager.face.card.HeartRateMeasureEntry
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter
import com.daily.health.manager.face.viewmodel.BsRecordViewModel
import com.daily.health.manager.face.viewmodel.BmiRecordViewModel
import com.daily.health.manager.face.viewmodel.BpRecordViewModel
import com.daily.health.manager.face.viewmodel.CholesterolRecordViewModel
import com.daily.health.manager.face.viewmodel.HeartRateRecordViewModel
import com.daily.health.manager.face.weight.LeveDataFactory
import com.daily.health.manager.face.weight.RulerView
import com.daily.health.manager.face.widget.NumberPickerView
import com.daily.health.manager.util.BloodSugarScaleHelper
import com.daily.health.manager.util.CholesterolMetrics
import com.daily.health.manager.utils.loadNative
import com.daily.health.manager.utils.showInter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.core.context.GlobalContext
import com.healthtracker.framework.util.getRobotoBold
import com.healthtracker.framework.util.getRobotoRegular
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.face.dialog.ReminderSettingsDialogFragment
import com.daily.health.manager.face.compose.ReminderDialogHelper
import java.util.Calendar
import java.util.Locale

class HealthRecordScreen : BaseInterActivity<BaseViewModel, TrActivityHealthRecordBinding>() {

    companion object {
        private const val EXTRA_RECORD_TYPE = "extra_record_type"
        private const val EXTRA_RECORD_ID = "extra_record_id"

        fun start(context: Context, type: RecordType, recordId: Long? = null) {
            val intent = Intent(context, HealthRecordScreen::class.java).apply {
                putExtra(EXTRA_RECORD_TYPE, type.name)
                recordId?.let { putExtra(EXTRA_RECORD_ID, it) }
            }
            context.startActivity(intent)
        }
    }

    private fun setupBloodPressure(root: View) {
        val binding = TrActivityBpRecordBinding.bind(root)
        val healthTags = mutableListOf<HealthTag>()
        val addTagIds = mutableListOf<Long>()
        var latestSystolic = 120
        var latestDiastolic = 80

        val editRecordId = recordId
        if (editRecordId != null) {
            bpViewModel.loadEditRecord(editRecordId)
            binding.actionBar.tvTitle.text = getString(R.string.tr_edit_record)
        }
        bpViewModel.initializeTags()

        binding.actionBar.btnBack.clickWithDuration {
            handleBackPress()
        }

        val tfRegular = getRobotoRegular(this)
        val tfBold = getRobotoBold(this)
        binding.npvDiastolic.setContentSelectedTextTypeface(tfBold)
        binding.npvSystolic.setContentSelectedTextTypeface(tfBold)
        binding.npvPulse.setContentSelectedTextTypeface(tfBold)
        binding.npvDiastolic.setContentNormalTextTypeface(tfRegular)
        binding.npvSystolic.setContentNormalTextTypeface(tfRegular)
        binding.npvPulse.setContentNormalTextTypeface(tfRegular)

        binding.npvDiastolic.minValue = 20
        binding.npvSystolic.minValue = 20
        binding.npvSystolic.maxValue = 300
        binding.npvDiastolic.maxValue = 300
        binding.npvPulse.minValue = 40
        binding.npvPulse.maxValue = 220

        binding.npvSystolic.setOnValueChangedListener { _, _, _ ->
            val systolic = binding.npvSystolic.contentByCurrValue.toInt()
            bpViewModel.updateSystolicPressure(systolic)
        }
        binding.npvDiastolic.setOnValueChangedListener { _, _, _ ->
            val diastolic = binding.npvDiastolic.contentByCurrValue.toInt()
            bpViewModel.updateDiastolicPressure(diastolic)
        }
        binding.npvPulse.setOnValueChangedListener { _, _, _ ->
            bpViewModel.updatePulseRate(binding.npvPulse.contentByCurrValue.toInt())
        }

        fun updateLsvCurrentIndex() {
            val idx = LeveDataFactory.BloodPressure.indexFor(latestSystolic, latestDiastolic)
            binding.lsvStatusBp.setCurrentLevel(idx)
        }

        val levels = LeveDataFactory.BloodPressure.buildItems(this)
        binding.lsvStatusBp.setLevels(levels)
        latestSystolic = binding.npvSystolic.contentByCurrValue.toInt()
        latestDiastolic = binding.npvDiastolic.contentByCurrValue.toInt()
        updateLsvCurrentIndex()
        binding.lsvStatusBp.setExplainClickable(true)
        binding.lsvStatusBp.setOnExplainClick {
            val items = ArrayList(LeveDataFactory.BloodPressure.buildExplainItems(this))
            LevelExplainDialog.show(
                supportFragmentManager,
                des = getString(R.string.tr_bp_range_des),
                items = items
            )
        }

        binding.dateTimeSelectionView.setOnLabelClickListener {
            val selectedTags = if (addTagIds.isEmpty()) {
                null
            } else {
                val tempTags = mutableListOf<HealthTag>()
                for (id in addTagIds) {
                    healthTags.find { it.id == id }?.let { tempTags.add(it) }
                }
                tempTags
            }
            HealthTagDialog.showBloodPressureDialog(
                supportFragmentManager,
                bpViewModel.getBloodPressureTagsFlow(),
                selectedTags,
                onSave = { selectedTagList ->
                    val tagIds = selectedTagList.map { it.id }
                    addTagIds.clear()
                    addTagIds.addAll(tagIds)
                    bpViewModel.clearSelectedTags()
                    tagIds.forEach { tagId ->
                        bpViewModel.toggleTagSelection(tagId)
                    }
                },
                onDelete = { tag ->
                    bpViewModel.deleteTag(tag)
                },
                onAdd = { tagName ->
                    lifecycleScope.launch {
                        val id = bpViewModel.createCustomTag(tagName)
                        if (id <= 0L) {
                            showToast(getString(R.string.tr_create_label_failed))
                        }
                    }
                }
            )
        }

        binding.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.BLOOD_PRESSURE)
            lifecycleScope.launch {
                bpViewModel.updateRecordTime(binding.dateTimeSelectionView.getSelectDate())
                when (val result = bpViewModel.saveBloodPressureRecord()) {
                    is BpRecordViewModel.SaveRecordResult.Created -> goDetail(RecordType.BLOOD_PRESSURE, result.recordId)
                    is BpRecordViewModel.SaveRecordResult.Updated -> goDetail(RecordType.BLOOD_PRESSURE, result.recordId)
                    is BpRecordViewModel.SaveRecordResult.Failed -> Unit
                }
            }
        }

        collect(bpViewModel.systolicPressure) {
            latestSystolic = it
            updateLsvCurrentIndex()
            if (it == binding.npvSystolic.contentByCurrValue.toInt()) {
                return@collect
            }
            binding.npvSystolic.value = it
        }

        collect(bpViewModel.diastolicPressure) {
            latestDiastolic = it
            updateLsvCurrentIndex()
            if (it == binding.npvDiastolic.contentByCurrValue.toInt()) {
                return@collect
            }
            binding.npvDiastolic.value = it
        }

        collect(bpViewModel.pulseRate) {
            if (it == binding.npvPulse.contentByCurrValue.toInt()) {
                return@collect
            }
            binding.npvPulse.value = it
        }

        collectLatest(bpViewModel.recordTime) { recordTime ->
            val calendar = Calendar.getInstance().apply { time = recordTime }
            if (!isDestroyed && !isFinishing) {
                binding.dateTimeSelectionView.getDateTimePicker().initView(
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH) + 1,
                    day = calendar.get(Calendar.DAY_OF_MONTH),
                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                    minute = calendar.get(Calendar.MINUTE)
                )
            }
        }

        collectLatest(bpViewModel.isLoading) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) {
                getString(R.string.tr_saving)
            } else {
                getString(R.string.tr_save)
            }
        }

        collectLatest(bpViewModel.availableTags) { tags ->
            healthTags.clear()
            healthTags.addAll(tags)
        }

        collectLatest(bpViewModel.selectedTagIds) { tagIds ->
            addTagIds.clear()
            addTagIds.addAll(tagIds)
        }

        loadNative(binding.adContainer, AdPosition.NA_NEW_RECORD_BLOOD_PRESSURE_BOTTOM, style = NativeAdStyle.STANDARD)
    }

    private fun setupBmi(root: View) {
        val binding = TrActivityBmiRecordBinding.bind(root)
        val healthTags = mutableListOf<HealthTag>()
        val addTagIds = mutableListOf<Long>()

        var latestHeightCm = BodyMetricsDefaults.DEFAULT_HEIGHT_CM.toFloat()
        var latestWeightKg = BodyMetricsDefaults.DEFAULT_WEIGHT_KG.toFloat()
        var currentWeightUnit: BmiUnit = BmiUnit.getPreferredWeightUnit()
        var currentHeightUnit: BmiUnit = BmiUnit.getPreferredHeightUnit()

        bmiViewModel.initializeWithRecord(recordId)

        binding.actionBar.btnBack.clickWithDuration { handleBackPress() }
        if (recordId != null) {
            binding.actionBar.tvTitle.text = getString(R.string.tr_edit_record)
        }

        fun getUnitLabelText(isWeight: Boolean, unit: BmiUnit): String {
            val unitName = when {
                isWeight && unit == BmiUnit.METRIC -> getString(R.string.tr_unit_kg)
                isWeight && unit == BmiUnit.IMPERIAL -> getString(R.string.tr_unit_lb)
                !isWeight && unit == BmiUnit.METRIC -> getString(R.string.tr_unit_cm)
                else -> getString(R.string.tr_unit_ft_in)
            }
            return getString(R.string.tr_unit_in_brackets, unitName.lowercase())
        }

        fun updateDisplayValues() {
            val displayWeight = BmiUnit.formatDisplayWeight(latestWeightKg, currentWeightUnit)
            val displayHeight = BmiUnit.formatDisplayHeight(latestHeightCm, currentHeightUnit)
            binding.tvWeightValue.text = displayWeight
            binding.tvHeightValue.text = displayHeight
            binding.tvWeightUnit.text = getUnitLabelText(isWeight = true, unit = currentWeightUnit)
            binding.tvHeightUnit.text = getUnitLabelText(isWeight = false, unit = currentHeightUnit)
        }

        fun updateLsvCurrentIndex() {
            val heightM = latestHeightCm / 100f
            if (heightM > 0f) {
                val bmi = (latestWeightKg / (heightM * heightM))
                val idx = LeveDataFactory.BMI.indexFor(bmi)
                binding.bpStatusView.setCurrentLevel(idx)
            }
        }

        binding.clWeight.clickWithDuration {
            val curDisplay = BmiUnit.toDisplayWeight(latestWeightKg, currentWeightUnit)
            BmiPickerDialog.showWeightPicker(
                supportFragmentManager,
                initialDisplayValue = curDisplay,
                unit = currentWeightUnit
            ) { value, unit ->
                if (currentWeightUnit != unit) {
                    currentWeightUnit = unit
                    bmiViewModel.switchWeightUnit(unit)
                }
                bmiViewModel.updateWeightFromDisplay(value)
            }
        }

        binding.clHeight.clickWithDuration {
            val curDisplay = BmiUnit.toDisplayHeight(latestHeightCm, currentHeightUnit)
            BmiPickerDialog.showHeightPicker(
                supportFragmentManager,
                initialDisplayValue = curDisplay,
                unit = currentHeightUnit
            ) { value, unit ->
                if (currentHeightUnit != unit) {
                    currentHeightUnit = unit
                    bmiViewModel.switchHeightUnit(unit)
                }
                bmiViewModel.updateHeightFromDisplay(value)
            }
        }

        binding.tvWeightUnit.setOnClickListener {
            val newUnit = if (currentWeightUnit == BmiUnit.METRIC) BmiUnit.IMPERIAL else BmiUnit.METRIC
            bmiViewModel.switchWeightUnit(newUnit)
        }
        binding.tvHeightUnit.setOnClickListener {
            val newUnit = if (currentHeightUnit == BmiUnit.METRIC) BmiUnit.IMPERIAL else BmiUnit.METRIC
            bmiViewModel.switchHeightUnit(newUnit)
        }

        binding.dateTimeSelectionView.setOnLabelClickListener {
            val selectedTags = if (addTagIds.isEmpty()) {
                null
            } else {
                val temp = mutableListOf<HealthTag>()
                for (id in addTagIds) {
                    healthTags.find { it.id == id }?.let { temp.add(it) }
                }
                temp
            }
            HealthTagDialog(
                tagType = TagType.BMI,
                tagsFlow = bmiViewModel.loadAvailableHealthTagsFlow(),
                selectedTags = selectedTags,
                onSave = { selectedTagList ->
                    val tagIds = selectedTagList.map { it.id }
                    addTagIds.clear()
                    addTagIds.addAll(tagIds)
                    bmiViewModel.clearSelectedTags()
                    tagIds.forEach { tagId -> bmiViewModel.addTag(tagId) }
                },
                onDelete = { tag ->
                    bmiViewModel.deleteTag(tag)
                },
                onAdd = { tagName ->
                    lifecycleScope.launch {
                        val id = bmiViewModel.createCustomTag(tagName)
                        if (id <= 0L) {
                            showToast(getString(R.string.tr_create_label_failed))
                        }
                    }
                }
            ).show(supportFragmentManager)
        }

        binding.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.BMI)
            lifecycleScope.launch {
                bmiViewModel.updateRecordTime(binding.dateTimeSelectionView.getSelectDate())
                bmiViewModel.saveBmiRecord { result ->
                    when (result) {
                        is BmiRecordViewModel.SaveRecordResult.Created -> goDetail(RecordType.BMI, result.recordId)
                        is BmiRecordViewModel.SaveRecordResult.Updated -> goDetail(RecordType.BMI, result.recordId)
                        is BmiRecordViewModel.SaveRecordResult.Failed -> showToast(result.error)
                    }
                }
            }
        }

        val levels = LeveDataFactory.BMI.buildItems(this)
        binding.bpStatusView.setLevels(levels)
        updateLsvCurrentIndex()
        binding.bpStatusView.setExplainClickable(true)
        binding.bpStatusView.setOnExplainClick {
            val items = ArrayList(LeveDataFactory.BMI.buildExplainItems(this))
            LevelExplainDialog.show(
                supportFragmentManager,
                items = items,
                des = null
            )
        }

        collect(bmiViewModel.heightCm) { height ->
            latestHeightCm = height
            updateDisplayValues()
            updateLsvCurrentIndex()
        }
        collect(bmiViewModel.weightKg) { weight ->
            latestWeightKg = weight
            updateDisplayValues()
            updateLsvCurrentIndex()
        }
        collect(bmiViewModel.weightUnit) { unit ->
            currentWeightUnit = unit
            updateDisplayValues()
        }
        collect(bmiViewModel.heightUnit) { unit ->
            currentHeightUnit = unit
            updateDisplayValues()
        }

        collectLatest(bmiViewModel.recordTime) { recordTime ->
            val calendar = Calendar.getInstance().apply { time = recordTime }
            if (!isDestroyed && !isFinishing) {
                binding.dateTimeSelectionView.getDateTimePicker().initView(
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH) + 1,
                    day = calendar.get(Calendar.DAY_OF_MONTH),
                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                    minute = calendar.get(Calendar.MINUTE)
                )
            }
        }

        collectLatest(bmiViewModel.isLoading) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) {
                getString(R.string.tr_saving)
            } else {
                getString(R.string.tr_save)
            }
        }

        collectLatest(bmiViewModel.availableTags) { tags ->
            healthTags.clear()
            healthTags.addAll(tags)
        }
        collectLatest(bmiViewModel.selectedTagIds) { tagIds ->
            addTagIds.clear()
            addTagIds.addAll(tagIds)
        }

        loadNative(binding.adContainer, AdPosition.NA_NEW_RECORD_BMI_BOTTOM, style = NativeAdStyle.STANDARD)
    }

    private fun setupHeartRate(root: View) {
        val binding = TrActivityHeartRateRecordBinding.bind(root)
        val healthTags = mutableListOf<HealthTag>()
        val addTagIds = mutableListOf<Long>()
        var latestHeartRate = 70

        val recordId = recordId
        heartRateViewModel.initialize(recordId)
        if (recordId != null) {
            binding.actionBar.tvTitle.text = getString(R.string.tr_edit_record)
        }

        binding.cvMeasureEntry.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.cvMeasureEntry.setContent {
            val lastRecord by heartRateViewModel.latestPpgRecord.collectAsState()
            HeartRateMeasureEntry(
                lastBpm = lastRecord?.heartRateBpm,
                lastDate = lastRecord?.recordTime?.time,
                onMeasureClick = {
                    // 直接启动心率测量页面，测量完成后会自动跳转到详情页面
                    HeartRateMeasureScreen.start(this@HealthRecordScreen)
                }
            )
        }

        binding.actionBar.btnBack.clickWithDuration { handleBackPress() }
        binding.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.HEART_RATE)
            lifecycleScope.launch {
                heartRateViewModel.updateRecordTime(binding.dateTimeSelectionView.getSelectDate())
                heartRateViewModel.saveHeartRateRecord { result ->
                    when (result) {
                        is HeartRateRecordViewModel.SaveRecordResult.Created -> goDetail(RecordType.HEART_RATE, result.recordId)
                        is HeartRateRecordViewModel.SaveRecordResult.Updated -> goDetail(RecordType.HEART_RATE, result.recordId)
                        is HeartRateRecordViewModel.SaveRecordResult.Failed -> showToast(result.error)
                    }
                }
            }
        }

        with(binding.npvHeartRate) {
            val tfRegular = getRobotoRegular(this@HealthRecordScreen)
            val tfBold = getRobotoBold(this@HealthRecordScreen)
            setContentNormalTextTypeface(tfRegular)
            setContentSelectedTextTypeface(tfBold)

            val minHeartRate = 40
            val maxHeartRate = 220
            val values = Array(maxHeartRate - minHeartRate + 1) { index ->
                (minHeartRate + index).toString()
            }
            displayedValues = values
            minValue = minHeartRate
            maxValue = maxHeartRate
            value = displayedValues.indexOf((latestHeartRate + minHeartRate).toString())
            setOnValueChangedListener { _, _, _ ->
                val bpm = contentByCurrValue.toIntOrNull() ?: 70
                heartRateViewModel.updateHeartRate(bpm)
            }
        }

        val levels = LeveDataFactory.HeartRate.buildItems(this)
        binding.lsvStatus.setLevels(levels)
        binding.lsvStatus.setOnExplainClick {
            val items = ArrayList(LeveDataFactory.HeartRate.buildExplainItems(this))
            LevelExplainDialog.show(supportFragmentManager, items)
        }

        binding.dateTimeSelectionView.apply {
            setTitleText(getString(R.string.tr_date_time))
            setLabelText(getString(R.string.tr_label))
            setOnLabelClickListener {
                val selected = if (addTagIds.isEmpty()) {
                    null
                } else {
                    healthTags.filter { tag -> addTagIds.contains(tag.id) }
                }
                HealthTagDialog.showHeartRateDialog(
                    fragmentManager = supportFragmentManager,
                    tagsFlow = heartRateViewModel.getHeartRateTagsFlow(),
                    selectedTags = selected,
                    onSave = { selectedTags ->
                        val ids = selectedTags.map { it.id }
                        addTagIds.clear()
                        addTagIds.addAll(ids)
                        heartRateViewModel.clearSelectedTags()
                        ids.forEach { heartRateViewModel.addTag(it) }
                    },
                    onDelete = { tag ->
                        heartRateViewModel.deleteTag(tag)
                    },
                    onAdd = { name ->
                        lifecycleScope.launch {
                            val newId = heartRateViewModel.createCustomTag(name)
                            if (newId <= 0L) {
                                showToast(getString(R.string.tr_create_label_failed))
                            }
                        }
                    }
                )
            }
        }

        fun updateLevelIndex(bpm: Int) {
            val index = LeveDataFactory.HeartRate.indexFor(bpm)
            binding.lsvStatus.setCurrentLevel(index)
        }

        collect(heartRateViewModel.heartRate) { bpm ->
            latestHeartRate = bpm
            val picker = binding.npvHeartRate
            val currentValue = picker.contentByCurrValue.toIntOrNull()
            if (currentValue != bpm) {
                picker.value = bpm
            }
            updateLevelIndex(bpm)
        }

        collectLatest(heartRateViewModel.recordTime) { date ->
            val calendar = Calendar.getInstance().apply { time = date }
            binding.dateTimeSelectionView.getDateTimePicker().initView(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE)
            )
        }

        collectLatest(heartRateViewModel.isLoading) { loading ->
            binding.btnSave.isEnabled = !loading
            binding.btnSave.text = if (loading) {
                getString(R.string.tr_saving)
            } else {
                getString(R.string.tr_save)
            }
        }

        collectLatest(heartRateViewModel.availableTags) { tags ->
            healthTags.clear()
            healthTags.addAll(tags)
        }
        collectLatest(heartRateViewModel.selectedTagIds) { tagIds ->
            addTagIds.clear()
            addTagIds.addAll(tagIds)
        }

        loadNative(binding.adContainer, AdPosition.NA_NEW_RECORD_HEART_RATE_BOTTOM, style = NativeAdStyle.STANDARD)
    }

    private fun setupCholesterol(root: View) {
        val binding = TrActivityCholesterolRecordBinding.bind(root)
        val detailBinding = TrLayoutCholesterolDetailValueBinding.inflate(layoutInflater)

        val recordId = recordId
        cholesterolViewModel.initialize(recordId)
        if (recordId != null) {
            binding.actionBar.tvTitle.text = getString(R.string.tr_edit_record)
        }

        binding.actionBar.btnBack.clickWithDuration { handleBackPress() }
        binding.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.CHOLESTEROL)
            lifecycleScope.launch {
                cholesterolViewModel.updateRecordTime(binding.dateTimeSelectionView.getSelectDate())
                when (val result = cholesterolViewModel.saveRecord()) {
                    is CholesterolRecordViewModel.SaveResult.Created -> goDetail(RecordType.CHOLESTEROL, result.recordId)
                    is CholesterolRecordViewModel.SaveResult.Updated -> goDetail(RecordType.CHOLESTEROL, result.recordId)
                    is CholesterolRecordViewModel.SaveResult.Failed -> showToast(result.error)
                }
            }
        }

        val tfRegular = getRobotoRegular(this)
        val tfBold = getRobotoBold(this)

        fun NumberPickerView.applyTypography() {
            setContentNormalTextTypeface(tfRegular)
            setContentSelectedTextTypeface(tfBold)
        }

        fun NumberPickerView.setupRange() {
            val values = Array(200) { i -> DateTimeUtils.formatTwoDigit(i + 1) }
            displayedValues = values
            minValue = 0
            maxValue = 199
        }

        binding.npvHdl.apply {
            applyTypography()
            setupRange()
            setOnValueChangedListener { _, _, _ ->
                val value = contentByCurrValue.toIntOrNull()
                value?.let { cholesterolViewModel.updateHdl(it) }
            }
        }

        binding.npvLdl.apply {
            applyTypography()
            setupRange()
            setOnValueChangedListener { _, _, _ ->
                val value = contentByCurrValue.toIntOrNull()
                value?.let { cholesterolViewModel.updateLdl(it) }
            }
        }

        binding.npvTg.apply {
            applyTypography()
            setupRange()
            setOnValueChangedListener { _, _, _ ->
                val value = contentByCurrValue.toIntOrNull()
                value?.let { cholesterolViewModel.updateTriglyceride(it) }
            }
        }

        val levels = LeveDataFactory.Cholesterol.buildItems(this)
        binding.lsvStatus.setLevels(levels)
        binding.lsvStatus.setExtraView(detailBinding.root)
        binding.lsvStatus.setExplainClickable(true)
        binding.lsvStatus.setOnExplainClick {
            val items = ArrayList(LeveDataFactory.Cholesterol.buildExplainItems(this))
            LevelExplainDialog.show(
                supportFragmentManager,
                des = getString(R.string.tr_cholesterol_range_des),
                items = items
            )
        }

        binding.dateTimeSelectionView.setLabelVisible(false)

        fun formatCholesterolValue(value: Float): String {
            return NumberFormatter.formatNumber(value.toDouble(), LanguageUtils.getAppLocale(this@HealthRecordScreen), 0)
        }

        fun formatRatioValue(value: Float): String {
            return NumberFormatter.formatNumber(value.toDouble(), LanguageUtils.getAppLocale(this@HealthRecordScreen), 2)
        }

        fun updateLsvCurrentIndex(riskLevel: CholesterolLevel) {
            val idx = LeveDataFactory.Cholesterol.indexFor(riskLevel)
            binding.lsvStatus.setCurrentLevel(idx)
        }

        fun updateMetrics(metrics: CholesterolMetrics) {
            val totalValue = metrics.totalCholesterol?.let { formatCholesterolValue(it) } ?: "--"
            binding.tvTc.text = getString(R.string.tr_total_cholesterol, totalValue)
            detailBinding.tvNonHdlValue.text = metrics.nonHdl?.let { formatCholesterolValue(it) } ?: "--"
            detailBinding.tvTcHdlValue.text = metrics.tcHdlRatio?.let { formatRatioValue(it) } ?: "--"
            detailBinding.tvLdlHdlValue.text = metrics.ldlHdlRatio?.let { formatRatioValue(it) } ?: "--"
            updateLsvCurrentIndex(metrics.riskLevel)
        }

        collectLatest(cholesterolViewModel.metrics) { metrics ->
            updateMetrics(metrics)
        }

        collectLatest(cholesterolViewModel.recordTime) { date ->
            val calendar = Calendar.getInstance().apply { time = date }
            binding.dateTimeSelectionView.getDateTimePicker().initView(
                year = calendar.get(Calendar.YEAR),
                month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH),
                hour = calendar.get(Calendar.HOUR_OF_DAY),
                minute = calendar.get(Calendar.MINUTE)
            )
        }

        collectLatest(cholesterolViewModel.isSaving) { saving ->
            binding.btnSave.isEnabled = !saving
            binding.btnSave.text = if (saving) {
                getString(R.string.tr_saving)
            } else {
                getString(R.string.tr_save)
            }
        }

        collectLatest(cholesterolViewModel.isLoading) { loading ->
            binding.btnSave.isEnabled = !loading && !cholesterolViewModel.isSaving.value
        }

        collect(cholesterolViewModel.hdl) {
            binding.npvHdl.value = it - 1
        }
        collect(cholesterolViewModel.ldl) {
            binding.npvLdl.value = it - 1
        }
        collect(cholesterolViewModel.triglyceride) {
            binding.npvTg.value = it - 1
        }

        loadNative(binding.adContainer, AdPosition.NA_NEW_RECORD_CHOLESTEROL_BOTTOM, style = NativeAdStyle.STANDARD)
    }

    enum class RecordType {
        BLOOD_SUGAR,
        BLOOD_PRESSURE,
        BMI,
        HEART_RATE,
        CHOLESTEROL,
        ;

        fun toHealthType(): HealthType {
            return when (this) {
                BLOOD_SUGAR -> HealthType.BLOOD_SUGAR
                BLOOD_PRESSURE -> HealthType.BLOOD_PRESSURE
                BMI -> HealthType.BMI
                HEART_RATE -> HealthType.HEART_RATE
                CHOLESTEROL -> HealthType.CHOLESTEROL
            }
        }

        fun toLayoutRes(): Int {
            return when (this) {
                BLOOD_SUGAR -> R.layout.tr_activity_bs_record
                BLOOD_PRESSURE -> R.layout.tr_activity_bp_record
                BMI -> R.layout.tr_activity_bmi_record
                HEART_RATE -> R.layout.tr_activity_heart_rate_record
                CHOLESTEROL -> R.layout.tr_activity_cholesterol_record
            }
        }

        companion object {
            fun from(intent: Intent): RecordType? {
                val name = intent.getStringExtra(EXTRA_RECORD_TYPE) ?: return null
                return runCatching { valueOf(name) }.getOrNull()
            }
        }
    }

    private val recordType: RecordType? by lazy { RecordType.from(intent) }

    private val recordId: Long? by lazy {
        intent.getLongExtra(EXTRA_RECORD_ID, -1L).let { if (it == -1L) null else it }
    }

    private val bsViewModel: BsRecordViewModel by lazy { obtainViewModel(BsRecordViewModel::class.java) }
    private val bpViewModel: BpRecordViewModel by lazy { obtainViewModel(BpRecordViewModel::class.java) }
    private val bmiViewModel: BmiRecordViewModel by lazy { obtainViewModel(BmiRecordViewModel::class.java) }
    private val heartRateViewModel: HeartRateRecordViewModel by lazy {
        obtainViewModel(HeartRateRecordViewModel::class.java)
    }
    private val cholesterolViewModel: CholesterolRecordViewModel by lazy {
        obtainViewModel(CholesterolRecordViewModel::class.java)
    }

    private var bsBinding: TrActivityBsRecordBinding? = null
    private val bsHealthTags = mutableListOf<HealthTag>()
    private val bsAddTagIds = mutableListOf<Long>()

    private val targetRangeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateBloodSugarRangeView()
        }
    }


    override fun createViewBinding() = TrActivityHealthRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val type = recordType
        if (type == null) {
            finish()
            return
        }

        mViewBind.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        mViewBind.composeView.setContent {
            HealthRecordHost(type)
        }
    }

    @Composable
    private fun HealthRecordHost(type: RecordType) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                LayoutInflater.from(ctx)
                    .inflate(type.toLayoutRes(), null, false)
                    .also { root ->
                        bindAndSetup(type, root)
                    }
            }
        )
    }

    private fun bindAndSetup(type: RecordType, root: View) {
        when (type) {
            RecordType.BLOOD_SUGAR -> setupBloodSugar(root)
            RecordType.BLOOD_PRESSURE -> setupBloodPressure(root)
            RecordType.BMI -> setupBmi(root)
            RecordType.HEART_RATE -> setupHeartRate(root)
            RecordType.CHOLESTEROL -> setupCholesterol(root)
        }
    }

    private fun setupBloodSugar(root: View) {
        val binding = TrActivityBsRecordBinding.bind(root)
        bsBinding = binding

        bsViewModel.initializeWithRecord(recordId)
        
        // 静默预设血糖测量闹钟（仅首次进入时执行）
        bsViewModel.checkAndPresetBloodSugarAlarms()

        binding.actionBar.btnBack.clickWithDuration {
            handleBackPress()
        }

        if (recordId != null) {
            binding.actionBar.tvTitle.text = getString(R.string.tr_edit_record)
        }

        binding.clRangeTarget.clickWithDuration {
            targetRangeLauncher.launch(Intent(this@HealthRecordScreen, TargetRangeScreen::class.java))
        }

        binding.dateTimeSelectionView.setOnLabelClickListener {
            val addTags = if (bsAddTagIds.isEmpty()) {
                null
            } else {
                val tempTags = mutableListOf<HealthTag>()
                for (id in bsAddTagIds) {
                    bsHealthTags.find { it.id == id }?.let { tempTags.add(it) }
                }
                tempTags
            }
            HealthTagDialog.showBloodSugarDialog(
                supportFragmentManager,
                bsViewModel.getAvailableHealthTags(),
                addTags,
                onSave = { selectedTags ->
                    bsViewModel.updateTags(selectedTags)
                },
                onDelete = { tag ->
                    bsViewModel.deleteTag(tag)
                },
                onAdd = { tagName ->
                    lifecycleScope.launch {
                        val id = bsViewModel.createCustomTag(tagName)
                        if (id <= 0L) {
                            showToast(getString(R.string.tr_create_label_failed))
                        }
                    }
                }
            )
        }

        setupBloodSugarRulerView(binding)
        setupBloodSugarUnitSwitcher(binding)
        setupBloodSugarStatusSelector(binding)
        setupBloodSugarSaveButton(binding)
        observeBloodSugarViewModel(binding)

        loadNative(binding.adContainer, AdPosition.NA_NEW_RECORD_BLOOD_SUGAR_BOTTOM, style = NativeAdStyle.STANDARD)
    }

    private fun setupBloodSugarRulerView(binding: TrActivityBsRecordBinding) {
        binding.rulerView.setOnChooseResultListener(object : RulerView.OnChooseResultListener {
            override fun onEndResult(result: Float) {
                runCatching {
                    bsViewModel.updateValue(result)
                    binding.rangeView.updateValue(result)
                }
            }

            override fun onScrollResult(result: Float) {
                runCatching {
                    val currentUnit = bsViewModel.currentUnit.value
                    binding.tvSelectValue.text = BsUnit.formatValue(result, currentUnit)
                }
            }
        })
    }

    private fun setupBloodSugarUnitSwitcher(binding: TrActivityBsRecordBinding) {
        binding.rgUnit.setOnCheckedChangeListener { _, checkedId ->
            val newUnit = when (checkedId) {
                binding.rbMgdl.id -> BsUnit.MG_DL
                binding.rbMmol.id -> BsUnit.MMOL_L
                else -> return@setOnCheckedChangeListener
            }

            if (newUnit != bsViewModel.currentUnit.value) {
                bsViewModel.switchUnit(newUnit)
            }
        }
    }

    private fun setupBloodSugarStatusSelector(binding: TrActivityBsRecordBinding) {
        binding.clStatu.click {
            StatusSelectDialog.show(supportFragmentManager, bsViewModel.currentStatus.value) {
                it?.run {
                    bsViewModel.updateStatus(this)
                }
            }
        }
    }

    private fun setupBloodSugarSaveButton(binding: TrActivityBsRecordBinding) {
        binding.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.BLOOD_SUGAR)
            lifecycleScope.launch {
                bsViewModel.updateRecordTime(binding.dateTimeSelectionView.getSelectDate())
                val result = bsViewModel.saveRecord()
                when (result) {
                    is BsRecordViewModel.SaveRecordResult.Created -> goDetail(RecordType.BLOOD_SUGAR, result.recordId)
                    is BsRecordViewModel.SaveRecordResult.Updated -> goDetail(RecordType.BLOOD_SUGAR, result.recordId)
                    is BsRecordViewModel.SaveRecordResult.Failed -> Unit
                }
            }
        }
    }

    private fun observeBloodSugarViewModel(binding: TrActivityBsRecordBinding) {
        // 使用 combine 合并 unit 和 value 的 Flow，确保配置和定位的原子性
        // 避免单位切换时两个独立 collector 执行顺序不确定导致的位置计算错误
        lifecycleScope.launch {
            combine(bsViewModel.currentUnit, bsViewModel.currentValue) { unit, value ->
                unit to value
            }.collectLatest { (unit, value) ->
                runCatching {
                    // 1. 先更新 UI 单选按钮状态
                    updateBloodSugarUnitRadioButtons(binding, unit)
                    // 2. 更新显示文本
                    updateBloodSugarDisplayValues(binding)
                    // 3. 更新范围视图
                    updateBloodSugarRangeView(binding)
                    // 4. 配置 RulerView 参数（minScale/maxScale/scaleGap 等）
                    BloodSugarScaleHelper.configureRulerForUnit(binding.rulerView, unit)
                    // 5. 最后设置刻度位置（此时参数已正确）
                    binding.rulerView.setScaleImmediately(value, suppressCallback = true)
                }
            }
        }

        collectLatest(bsViewModel.currentStatus) { status ->
            binding.tvStatus.text = getString(getStatusStringRes(status.statusType))
            updateBloodSugarRangeView(binding)
        }

        collect(bsViewModel.isLoading) { isLoading ->
            binding.btnSave.isEnabled = !isLoading
            binding.btnSave.text = if (isLoading) {
                getString(R.string.tr_saving)
            } else {
                getString(R.string.tr_save)
            }
        }

        collectLatest(bsViewModel.recordTime) { recordTime ->
            val calendar = Calendar.getInstance()
            calendar.time = recordTime
            if (!isDestroyed && !isFinishing) {
                binding.dateTimeSelectionView.getDateTimePicker().initView(
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH) + 1,
                    day = calendar.get(Calendar.DAY_OF_MONTH),
                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                    minute = calendar.get(Calendar.MINUTE)
                )
            }
        }

        lifecycleScope.launch {
            bsViewModel.getAvailableHealthTags().collectLatest { tags ->
                bsHealthTags.clear()
                bsHealthTags.addAll(tags)
            }
        }

        collectLatest(bsViewModel.healthTags) { tagIds ->
            bsAddTagIds.clear()
            bsAddTagIds.addAll(tagIds)
        }
    }

    private fun updateBloodSugarDisplayValues(binding: TrActivityBsRecordBinding) {
        val currentValue = bsViewModel.currentValue.value
        val currentUnit = bsViewModel.currentUnit.value
        binding.tvSelectValue.text = BsUnit.formatValue(currentValue, currentUnit)
    }

    private fun updateBloodSugarRangeView(binding: TrActivityBsRecordBinding) {
        binding.rangeView.setCurrentState(
            bsViewModel.currentValue.value,
            bsViewModel.currentUnit.value,
            bsViewModel.currentStatus.value
        )
    }

    private fun updateBloodSugarUnitRadioButtons(binding: TrActivityBsRecordBinding, unit: BsUnit) {
        binding.rgUnit.check(
            when (unit) {
                BsUnit.MG_DL -> binding.rbMgdl.id
                BsUnit.MMOL_L -> binding.rbMmol.id
            }
        )
    }

    private fun updateBloodSugarRangeView() {
        val binding = bsBinding ?: return
        binding.rangeView.updateStatus(bsViewModel.currentStatus.value)
    }

    private fun goDetail(type: RecordType, recordId: Long) {
        val detailType = when (type) {
            RecordType.BLOOD_SUGAR -> HealthDetailScreen.DetailType.BLOOD_SUGAR
            RecordType.BLOOD_PRESSURE -> HealthDetailScreen.DetailType.BLOOD_PRESSURE
            RecordType.BMI -> HealthDetailScreen.DetailType.BMI
            RecordType.HEART_RATE -> HealthDetailScreen.DetailType.HEART_RATE
            RecordType.CHOLESTEROL -> HealthDetailScreen.DetailType.CHOLESTEROL
        }
        val savePosition = when (type) {
            RecordType.BLOOD_SUGAR -> AdPosition.IV_BLOOD_SUGAR_SAVE
            RecordType.BLOOD_PRESSURE -> AdPosition.IV_BLOOD_PRESSURE_SAVE
            RecordType.BMI -> AdPosition.IV_BMI_SAVE
            RecordType.HEART_RATE -> AdPosition.IV_HEART_RATE_SAVE
            RecordType.CHOLESTEROL -> AdPosition.IV_CHOLESTEROL_SAVE
        }
        
        val alarmType = when (type) {
            RecordType.BLOOD_SUGAR -> AlarmRecord.TYPE_BLOOD_SUGAR
            RecordType.BLOOD_PRESSURE -> AlarmRecord.TYPE_BLOOD_PRESSURE
            RecordType.BMI -> AlarmRecord.TYPE_BMI
            RecordType.HEART_RATE -> AlarmRecord.TYPE_HEART_RATE
            RecordType.CHOLESTEROL -> AlarmRecord.TYPE_CHOLESTEROL
        }

        SaveCompleteDialog.show(supportFragmentManager) {
             if (ReminderDialogHelper.shouldShowReminderDialog(alarmType)) {
                ReminderDialogHelper.markReminderDialogShown(alarmType)
                ReminderSettingsDialogFragment.newInstance(alarmType).apply {
                    // Note: Here we might want to finish the activity after dialog is dismissed
                    // For simplicity, we can let user see the detail after they close or add reminder
                }.show(supportFragmentManager, "reminder")
                
                // For logic continuity, we still open detail in background or after it
                HealthDetailScreen.start(this@HealthRecordScreen, detailType, recordId, isFromSave = true)
                finish()
            } else {
                showInter(savePosition) {
                    HealthDetailScreen.start(this@HealthRecordScreen, detailType, recordId)
                    finish()
                }
            }
        }
    }

    override fun getCurrentHealthType(): HealthType {
        return (recordType ?: return HealthType.OTHER).toHealthType()
    }

    private fun <T : ViewModel> obtainViewModel(modelClass: Class<T>): T {
        val defaultFactory = defaultViewModelProviderFactory
        val koin = runCatching { GlobalContext.get() }.getOrNull()

        val factory = object : ViewModelProvider.Factory {
            override fun <VM : ViewModel> create(clazz: Class<VM>): VM {
                val koinVm = koin?.let { runCatching { it.get<Any>(clazz = clazz.kotlin) }.getOrNull() }
                if (koinVm != null) {
                    @Suppress("UNCHECKED_CAST")
                    return koinVm as VM
                }
                return defaultFactory.create(clazz)
            }

            override fun <VM : ViewModel> create(clazz: Class<VM>, extras: CreationExtras): VM {
                val koinVm = koin?.let {
                    runCatching {
                        val savedStateHandle = extras.createSavedStateHandle()
                        it.get<Any>(
                            clazz = clazz.kotlin,
                            parameters = { org.koin.core.parameter.parametersOf(savedStateHandle) }
                        )
                    }.getOrNull()
                }
                if (koinVm != null) {
                    @Suppress("UNCHECKED_CAST")
                    return koinVm as VM
                }
                return defaultFactory.create(clazz, extras)
            }
        }

        return ViewModelProvider(this, factory)[modelClass]
    }
}
